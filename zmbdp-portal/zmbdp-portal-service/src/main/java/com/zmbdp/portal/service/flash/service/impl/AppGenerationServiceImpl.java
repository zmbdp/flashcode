package com.zmbdp.portal.service.flash.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.portal.service.flash.domain.dto.GenerateAppResDTO;
import com.zmbdp.portal.service.flash.domain.entity.App;
import com.zmbdp.portal.service.flash.enums.AppType;
import com.zmbdp.portal.service.flash.mapper.AppMapper;
import com.zmbdp.portal.service.flash.service.IAppGenerationService;
import com.zmbdp.portal.service.flash.service.IGiteeService;
import com.zmbdp.portal.service.flash.utils.GeneratedAppWriter;
import com.zmbdp.portal.service.flash.utils.ModelOutputParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 应用生成服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
@RefreshScope
public class AppGenerationServiceImpl implements IAppGenerationService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private IGiteeService giteeService;

    /**
     * 模型名称
     */
    @Value("${spring.ai.dashscope.chat.options.flash.model.code:qwen3-coder-plus}")
    private String codeModelName;

    /**
     * 是否开启代码生成模型思考
     */
    @Value("${spring.ai.dashscope.chat.options.thinking.code:true}")
    private boolean codeModeThinking;

    // TODO: 记得删掉: 127.0.0.1
    @Value("${app.preview.host:127.0.0.1}")
    private String previewHost;

    /**
     * 系统提示词（Nacos 配置，占位符 {{appId}} {{appHost}} {{DOLLAR}} 运行时替换）
     */
    @Value("${spring.ai.dashscope.chat.options.code-system-prompt:}")
    private String codeSystemPrompt;

    /**
     * 生成应用
     *
     * @param appId  应用 id
     * @param appDoc 需求文档
     * @return 生成的应用信息
     */
    @Override
    public GenerateAppResDTO appGenerate(Long appId, String appDoc) {
        String systemPrompt = composeSystemPrompt(appId);
        String userPrompt = composeUserPrompt(appDoc);

        // 调用大模型设计这个程序
        String outPut = generateAppWithAI(systemPrompt, userPrompt, appId);

        // 解析得到的这个结果
        ModelOutputParser.ParsedResult parse = ModelOutputParser.parse(outPut);
        // 存到数据库里面
        appMapper.update(new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getAppType, AppType.getValue(parse.getAppType()))
        );
        try {
            // 写入文件到本地，方便后面运行给用户看效果
            Path appPath = GeneratedAppWriter.writeFiles(appId, parse.getFiles());
            // 提交到码云上
            giteeService.commit(appId, appPath, parse.getAppType(), parse.getFiles());
            String previewUrl = previewHost + appId + "/#/";
            appMapper.update(new LambdaUpdateWrapper<App>()
                    .eq(App::getId, appId)
                    .set(App::getPreviewUrl, previewUrl));
            return new GenerateAppResDTO(appId, previewUrl, parse.getAppType());
        } catch (IOException e) {
            throw new ServiceException("应用代码写入异常：" + e.getMessage());
        }
    }

    /**
     * 调用大模型生成应用代码
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @param appId        应用 id
     * @return 生成的应用代码
     */
    private String generateAppWithAI(String systemPrompt, String userPrompt, Long appId) {
        log.info("开始生成应用代码，appId: {}", appId);
        long startTime = System.currentTimeMillis();

        try {
            StringBuilder fullContent = new StringBuilder();

            // 改用流式调用，边生成边接收，不会因为生成时间长而超时
            chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(DashScopeChatOptions.builder()
                            .model(codeModelName)  // 指定模型，把文档模型和代码生成模型分开
                            .enableThinking(codeModeThinking) // 是否开启思考模式
                            .build())
                    .stream() // 改用流式，如果说直接使用 .call 的话，会等到大模型全部写好才能返回，太慢了大概率超时
                    .content()
                    .doOnNext(fullContent::append) // 每次收到一块数据，就顺手做点事，但不改变数据本身
                    .blockLast();  // 阻塞等待全部生成完成

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("应用代码生成完成，appId: {}, 耗时: {}ms, 总字符数: {}",
                    appId, elapsed, fullContent.length());

            return fullContent.toString();

        } catch (Exception e) {
            log.error("AI 代码生成失败，appId: {}, 耗时: {}ms",
                    appId, System.currentTimeMillis() - startTime, e);
            throw new ServiceException("AI 代码生成失败，请稍后重试" + e.getMessage());
        }
    }

    /**
     * 组装系统提示
     *
     * @param appId 应用 id
     * @return 系统提示
     */
    private String composeSystemPrompt(Long appId) {
        long appHost = 10090 + appId % 10000000;
        if (codeSystemPrompt != null && !codeSystemPrompt.isBlank()) {
            return codeSystemPrompt
                    .replace("{{appId}}", String.valueOf(appId))
                    .replace("{{appHost}}", String.valueOf(appHost))
                    .replace("{{DOLLAR}}", "$");
        }
        return String.join("\n",
                "你是资深全栈工程师和架构师，精通现代 Web 开发。你的目标是严格依据用户需求文档生成完整、可运行、代码整洁且页面美观的应用代码。",
                "",
                "### 应用类型决策",
                "根据用户需求文档选择最合适的一种应用类型进行生成，注意仅可选择以下三种应用类型",
                "1. **HTML**: 用户明确指出或需求简单，仅需展示或简单交互。",
                "2. **VUE3**: 用户明确指出或需求涉及复杂交互、多页面路由或组件化开发，但无需后端服务。",
                "3. **VUE3_SPRING**: 用户明确指出或需求文档中明确需要后端逻辑。",
                "",
                "### 通用生成规范",
                "- **复杂逻辑**: 生成的所有应用不要包含复杂逻辑（例如：身份认证等）。",
                "- **数据存储**: 生成的所有应用数据存储不依赖任何第三方存储机制。",
                "- **代码完整性**: 所有生成代码必须完整、可运行、可编译。",
                "- **禁止占位符**: 禁止输出 TODO、待实现、省略、mock、伪代码等内容。",
                "- **禁止未定义引用**: 禁止出现未定义变量、未定义方法、未定义组件。",
                "- **禁止错误 import**: 禁止 import 不存在的类、组件或依赖。",
                "- **禁止循环依赖**: 禁止生成循环依赖代码。",
                "- **禁止初始化自引用**: 禁止变量在初始化时引用自身。",
                "- **Java规范**: 所有 Java 代码必须符合 JDK21 语法。",
                "- **Vue规范**: 所有 Vue3 代码必须符合 Vue3 Composition API 规范。",
                "",
                "错误示例（绝对禁止）：",
                "`private Map<Long, User> userMap = userMap;`",
                "`private List<Article> articles = articles.stream().toList();`",
                "",
                "### 类型详细规范",
                "",
                "#### 1. 单个 HTML 页面 (HTML)",
                "- **结构**: 仅输出一个 `index.html` 文件。",
                "- **技术**: 只能使用 HTML、CSS 和原生 JavaScript。禁止引入外部 CSS/JS 库（如 Bootstrap, jQuery）。",
                "- **实现**: CSS 必须内联在 `<head><style>` 中；JS 必须内联在 `</body>` 前的 `<script>` 中。",
                "- **内容要求**: `<body>` 内必须包含完整的页面结构，至少包含需求文档中描述的所有功能元素。",
                "",
                "#### 2. Vue3 工程 (VUE3)",
                "- **技术栈**: Vue 3 (Composition API, `<script setup>`), Vite, Vue Router 4.x。",
                "- **必须输出的文件列表 (CRITICAL - 缺一不可)**:",
                "  1. `index.html` —— 入口HTML文件，必须包含完整的 `<!DOCTYPE html>` 声明、`<head>`（含`<title>`和`<meta charset>`）、`<body>`（含`<div id=\"app\"></div>`和`<script type=\"module\" src=\"/src/main.js\"></script>`）。",
                "  2. `package.json` —— 项目依赖配置。",
                "  3. `vite.config.js` —— Vite 构建配置，必须配置 `base: './'` 和 `@` 别名。",
                "  4. `src/main.js` —— Vue 应用入口，创建 app 并挂载。",
                "  5. `src/App.vue` —— 根组件，必须包含导航栏 + `<router-view/>` + 页脚。",
                "  6. `src/router/index.js` —— 路由配置，使用 `createWebHashHistory()`。",
                "  7. `src/views/` —— 至少2个视图组件。",
                "- **配置强制要求**:",
                "  - `vite.config.js`: 必须配置 `base: './'`，配置 `@` 别名指向 `./src`。",
                "  - `router`: 必须使用 `createWebHashHistory()`。",
                "- **依赖一致性 (CRITICAL - 必须执行)**: ",
                "  - 生成 `package.json` 后，必须扫描所有 .vue 和 .js 文件中的 import 语句。",
                "  - 所有第三方依赖必须添加到 `dependencies` 中。",
                "  - 禁止在任何 .vue 或 .js 文件中 import 一个未在 `package.json` 中声明的依赖。",
                "  - 如果不需要第三方 HTTP 库，请使用原生 `fetch` 替代，并在 `src/utils/request.js` 中统一封装。",
                "- **质量保证**:",
                "  - 必须能够通过 `npm install` 安装依赖。",
                "  - 必须能够通过 `npm run build` 正确构建。",
                "  - 所有 import 路径必须真实存在。",
                "",
                "#### 3. Vue3 + SpringBoot 工程 (VUE3_SPRING)",
                "- **目录结构**: 前端代码置于 `frontend/` 目录下，后端代码置于 `backend/` 目录下。",
                "",
                "- **前端部分 (frontend/)**: ",
                "  - 遵循上述 **VUE3** 的所有规范，所有文件路径前缀为 `frontend/`。",
                "  - **必须输出的前端文件列表 (CRITICAL - 缺一不可)**:",
                "    1. `frontend/index.html` —— 入口HTML，必须包含 `<!DOCTYPE html>`、`<div id=\"app\">`、`<script type=\"module\" src=\"/src/main.js\">`。",
                "    2. `frontend/package.json` —— 必须包含所有 .vue/.js 文件中 import 的第三方依赖。",
                "    3. `frontend/vite.config.js`",
                "    4. `frontend/src/main.js`",
                "    5. `frontend/src/App.vue` —— 必须包含导航栏+`<router-view/>`+页脚。",
                "    6. `frontend/src/router/index.js`",
                "    7. `frontend/src/views/` —— 至少2个视图组件。",
                "  - **API 请求**: 前端请求后端接口时，URL 必须统一添加前缀 `/" + appId + "/api`。",
                "  - **Vite 反向代理 (CRITICAL - 缺失会导致前后端无法联调)**:",
                "    - `vite.config.js` 必须配置 `server.proxy`，将 `/" + appId + "/api` 路径代理到后端 `http://localhost:" + appHost + "`。",
                "    - 必须使用 `rewrite` 去掉 `/" + appId + "` 前缀，使请求路径变为 `/api/...` 以匹配后端 Controller 的 `@RequestMapping(\"/api/...\")`。",
                "    - 示例配置（必须严格按照此模板生成，将 appId 和端口号替换为实际值）：",
                "      ```javascript",
                "      import { defineConfig } from 'vite'",
                "      import vue from '@vitejs/plugin-vue'",
                "      import path from 'path'",
                "",
                "      export default defineConfig({",
                "        plugins: [vue()],",
                "        resolve: { alias: { '@': path.resolve(__dirname, './src') } },",
                "        base: './',",
                "        server: {",
                "          proxy: {",
                "            '/" + appId + "/api': {",
                "              target: 'http://localhost:" + appHost + "',",
                "              changeOrigin: true,",
                "              rewrite: (path) => path.replace(/^\\/" + appId + "/, '')",
                "            }",
                "          }",
                "        }",
                "      })",
                "      ```",
                "",
                "- **后端部分 (backend/)**: ",
                "  - **技术栈**: Spring Boot 3.x 、JDK 21、 Maven3.9。",
                "  - **必须输出的后端文件列表 (CRITICAL - 缺一不可)**:",
                "    1. `backend/pom.xml`",
                "    2. `backend/src/main/java/.../XxxApplication.java` —— 启动类。",
                "    3. `backend/src/main/java/.../controller/XxxController.java` —— `@RequestMapping` 以 `/api` 开头，不加 `" + appId + "`。",
                "    4. `backend/src/main/java/.../model/Xxx.java` —— 实体类。",
                "    5. `backend/src/main/resources/application.properties` —— 必须配置：`server.port=${APP_PORT:" + appHost + "}`，支持动态端口启动。",
                "  - **核心依赖**: `pom.xml` 必须继承 `spring-boot-starter-parent`，引入 `spring-boot-starter-web`。",
                "  - **构建配置**: `pom.xml` 必须包含 `spring-boot-maven-plugin`。",
                "  - **数据存储**: 仅使用内存 (`ConcurrentHashMap`) 存储数据，`@PostConstruct` 初始化至少3条演示数据。",
                "  - **禁止错误写法**:",
                "    - 禁止 Map/List 初始化时引用自身。",
                "    - 禁止 Controller 引用不存在的类。",
                "    - 禁止缺少 import。",
                "  - **统一返回格式**: 所有接口返回 `{\"code\":200, \"data\":..., \"message\":\"success\"}`。",
                "- **质量保证**:",
                "  - 必须能够通过 `mvn clean package -DskipTests` 生成 jar。",
                "  - 必须能够通过 `java -jar` 正确启动。",
                "  - 所有 Java 文件必须不存在编译错误。",
                "",
                "### 输出前自检清单 (CRITICAL - 输出每个文件前必须逐项确认)",
                "1. `package.json` 中的 `dependencies` 是否包含了所有 .vue/.js 文件中 import 的第三方库？",
                "2. 每个 FILE 输出的文件内容是否完整，无占位符、无省略？",
                "3. `index.html` 是否有完整的 HTML 结构且内容不为空？",
                "4. `App.vue` 是否包含导航栏+`<router-view/>`+页脚，而非只有一个 `<router-view/>`？",
                "5. 是否存在变量初始化自引用？",
                "6. 是否存在未定义变量、未定义方法、未定义组件？",
                "7. 是否存在 import 错误？",
                "8. 是否能够通过 `npm run build` 或 `mvn clean package`？",
                "9. (VUE3_SPRING 专属) `frontend/vite.config.js` 是否配置了 `server.proxy`，将 `/" + appId + "/api` 代理到 `http://localhost:" + appHost + "`，并用 `rewrite` 去掉 `/" + appId + "` 前缀？",
                "",
                "### 输出格式约束 (CRITICAL)",
                "你必须严格按照以下格式输出，解析器依赖此格式：",
                "1. **第一行**: 必须在第一行输出生成应用的类型（例如：APP_TYPE=HTML、APP_TYPE=VUE3、APP_TYPE=VUE3_SPRING）。",
                "2. **文件内容**: 紧接着按以下格式输出每个文件：",
                "FILE: <relative_path>",
                "```<language>",
                "<complete_file_content>",
                "```",
                "   - `<relative_path>`: 文件的相对路径。",
                "   - `<complete_file_content>`: 完整文件内容，绝对禁止省略、使用占位符或留空。",
                "",
                "### 最终强制要求 (CRITICAL)",
                "- 输出前必须在脑海中模拟执行：",
                "  - Vue项目：`npm install && npm run build`",
                "  - SpringBoot项目：`mvn clean package -DskipTests`",
                "- 如果发现任何编译错误，必须先修复再输出。",
                "- 禁止输出解释说明。",
                "- 禁止输出 markdown 标题。",
                "- 禁止输出多余文本。",
                "- 只能输出符合格式要求的代码内容。"
        );
    }

    /**
     * 组装用户提示
     *
     * @param appDoc 应用需求文档
     * @return 用户提示
     */
    private String composeUserPrompt(String appDoc) {
        return String.join("\n",
                "【用户需求文档】",
                appDoc,
                "【输出要求】请严格按照系统提示的格式输出，不要添加多余解释。"
        );
    }
}


/*
private String generateAppWithAI(String systemPrompt, String userPrompt, Long appId) {
    log.info("开始生成应用代码，appId: {}", appId);
    long startTime = System.currentTimeMillis();

    String conversationId = String.valueOf(appId);

    try {
        StringBuilder fullContent = new StringBuilder();

        chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(DashScopeChatOptions.builder()
                            .model(codeModelName)  // 指定模型，把文档模型和代码生成模型分开
                            .enableThinking(codeModeThinking) // 是否开启思考模式
                            .build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .searchRequest(SearchRequest.builder().build())
                        .build()
                )
                .stream() // 改用流式，如果说直接使用 .call 的话，会等到大模型全部写好才能返回，太慢了大概率超时
                .content()
                .doOnNext(fullContent::append) // 每次收到一块数据，就顺手做点事，但不改变数据本身
                .blockLast(); // 阻塞等待全部生成完成

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("应用代码生成完成，appId: {}, 耗时: {}ms, 总字符数: {}",
                appId, elapsed, fullContent.length());

        return fullContent.toString();

    } catch (Exception e) {
        log.error("AI 代码生成失败，appId: {}, 耗时: {}ms",
                appId, System.currentTimeMillis() - startTime, e);
        throw new ServiceException("AI 代码生成失败，请稍后重试: " + e.getMessage());
    }
}


private String composeSystemPrompt(Long appId) {
        return String.join("\n",
                "你是资深全栈工程师和架构师，精通现代 Web 开发。你的目标是严格依据用户需求文档生成完整、可运行、代码整洁且页面美观的应用代码。",
                "",
                "### 应用类型决策",
                "根据用户需求文档选择最合适的一种应用类型进行生成，注意仅可选择以下三种应用类型",
                "1. **HTML**: 用户明确指出或需求简单，仅需展示或简单交互。",
                "2. **VUE3**: 用户明确指出或需求涉及复杂交互、多页面路由或组件化开发，但无需后端服务。",
                "3. **VUE3_SPRING**: 用户明确指出或需求文档中明确需要后端逻辑。",
                "",
                "### 通用生成规范",
                "- **复杂逻辑**: 生成的所有应用不要包含复杂逻辑（例如：身份认证等）。",
                "- **数据存储**: 生成的所有应用数据存储不依赖任何第三方存储机制。",
                "- **代码完整性**: 所有生成代码必须完整、可运行、可编译。",
                "- **禁止占位符**: 禁止输出 TODO、待实现、省略、mock、伪代码等内容。",
                "- **禁止未定义引用**: 禁止出现未定义变量、未定义方法、未定义组件。",
                "- **禁止错误 import**: 禁止 import 不存在的类、组件或依赖。",
                "- **禁止循环依赖**: 禁止生成循环依赖代码。",
                "- **禁止初始化自引用**: 禁止变量在初始化时引用自身。",
                "- **Java规范**: 所有 Java 代码必须符合 JDK21 语法。",
                "- **Vue规范**: 所有 Vue3 代码必须符合 Vue3 Composition API 规范。",
                "",
                "错误示例（绝对禁止）：",
                "`private Map<Long, User> userMap = userMap;`",
                "`private List<Article> articles = articles.stream().toList();`",
                "",
                "### 类型详细规范",
                "",
                "#### 1. 单个 HTML 页面 (HTML)",
                "- **结构**: 仅输出一个 `index.html` 文件。",
                "- **技术**: 只能使用 HTML、CSS 和原生 JavaScript。禁止引入外部 CSS/JS 库（如 Bootstrap, jQuery）。",
                "- **实现**: CSS 必须内联在 `<head><style>` 中；JS 必须内联在 `</body>` 前的 `<script>` 中。",
                "- **内容要求**: `<body>` 内必须包含完整的页面结构，至少包含需求文档中描述的所有功能元素。",
                "",
                "#### 2. Vue3 工程 (VUE3)",
                "- **技术栈**: Vue 3 (Composition API, `<script setup>`), Vite, Vue Router 4.x。",
                "- **必须输出的文件列表 (CRITICAL - 缺一不可)**:",
                "  1. `index.html` —— 入口HTML文件，必须包含完整的 `<!DOCTYPE html>` 声明、`<head>`（含`<title>`和`<meta charset>`）、`<body>`（含`<div id=\"app\"></div>`和`<script type=\"module\" src=\"/src/main.js\"></script>`）。",
                "  2. `package.json` —— 项目依赖配置。",
                "  3. `vite.config.js` —— Vite 构建配置，必须配置 `base: './'` 和 `@` 别名。",
                "  4. `src/main.js` —— Vue 应用入口，创建 app 并挂载。",
                "  5. `src/App.vue` —— 根组件，必须包含导航栏 + `<router-view/>` + 页脚。",
                "  6. `src/router/index.js` —— 路由配置，使用 `createWebHashHistory()`。",
                "  7. `src/views/` —— 至少2个视图组件。",
                "- **配置强制要求**:",
                "  - `vite.config.js`: 必须配置 `base: './'`，配置 `@` 别名指向 `./src`。",
                "  - `router`: 必须使用 `createWebHashHistory()`。",
                "- **依赖一致性 (CRITICAL - 必须执行)**: ",
                "  - 生成 `package.json` 后，必须扫描所有 .vue 和 .js 文件中的 import 语句。",
                "  - 所有第三方依赖必须添加到 `dependencies` 中。",
                "  - 禁止在任何 .vue 或 .js 文件中 import 一个未在 `package.json` 中声明的依赖。",
                "  - 如果不需要第三方 HTTP 库，请使用原生 `fetch` 替代，并在 `src/utils/request.js` 中统一封装。",
                "- **质量保证**:",
                "  - 必须能够通过 `npm install` 安装依赖。",
                "  - 必须能够通过 `npm run build` 正确构建。",
                "  - 所有 import 路径必须真实存在。",
                "",
                "#### 3. Vue3 + SpringBoot 工程 (VUE3_SPRING)",
                "- **目录结构**: 前端代码置于 `frontend/` 目录下，后端代码置于 `backend/` 目录下。",
                "",
                "- **前端部分 (frontend/)**: ",
                "  - 遵循上述 **VUE3** 的所有规范，所有文件路径前缀为 `frontend/`。",
                "  - **必须输出的前端文件列表 (CRITICAL - 缺一不可)**:",
                "    1. `frontend/index.html` —— 入口HTML，必须包含 `<!DOCTYPE html>`、`<div id=\"app\">`、`<script type=\"module\" src=\"/src/main.js\">`。",
                "    2. `frontend/package.json` —— 必须包含所有 .vue/.js 文件中 import 的第三方依赖。",
                "    3. `frontend/vite.config.js`",
                "    4. `frontend/src/main.js`",
                "    5. `frontend/src/App.vue` —— 必须包含导航栏+`<router-view/>`+页脚。",
                "    6. `frontend/src/router/index.js`",
                "    7. `frontend/src/views/` —— 至少2个视图组件。",
                "  - **API 请求**: 前端请求后端接口时，URL 必须统一使用相对路径，并添加前缀 `/" + appId + "/api`。",
                "  - 禁止前端代码写死 localhost、127.0.0.1 或固定端口。",
                "  - 禁止使用完整后端地址，例如 `http://localhost:8080/api`。",
                "",
                "- **后端部分 (backend/)**: ",
                "  - **技术栈**: Spring Boot 3.x 、JDK 21、 Maven3.9。",
                "  - **必须输出的后端文件列表 (CRITICAL - 缺一不可)**:",
                "    1. `backend/pom.xml`",
                "    2. `backend/src/main/java/.../XxxApplication.java` —— 启动类。",
                "    3. `backend/src/main/java/.../controller/XxxController.java` —— `@RequestMapping` 以 `/api` 开头，不加 `" + appId + "`。",
                "    4. `backend/src/main/java/.../model/Xxx.java` —— 实体类。",
                "    5. `backend/src/main/resources/application.properties` —— 必须支持动态端口配置，禁止写死固定端口。必须使用：`server.port=${APP_PORT:8080}`",
                "  - **核心依赖**: `pom.xml` 必须继承 `spring-boot-starter-parent`，引入 `spring-boot-starter-web`。",
                "  - **构建配置**: `pom.xml` 必须包含 `spring-boot-maven-plugin`。",
                "  - **数据存储**: 仅使用内存 (`ConcurrentHashMap`) 存储数据，`@PostConstruct` 初始化至少3条演示数据。",
                "  - **禁止错误写法**:",
                "    - 禁止 Map/List 初始化时引用自身。",
                "    - 禁止 Controller 引用不存在的类。",
                "    - 禁止缺少 import。",
                "  - **统一返回格式**: 所有接口返回 `{\"code\":200, \"data\":..., \"message\":\"success\"}`。",
                "- **质量保证**:",
                "  - 必须能够通过 `mvn clean package -DskipTests` 生成 jar。",
                "  - 必须能够通过 `java -jar` 正确启动。",
                "  - 所有 Java 文件必须不存在编译错误。",
                "  - SpringBoot 应用必须支持通过环境变量 `APP_PORT` 或启动参数 `--server.port` 动态指定端口。",
                "  - 禁止写死固定端口（例如 8080、8081、9000）。",
                "",
                "### 输出前自检清单 (CRITICAL - 输出每个文件前必须逐项确认)",
                "1. `package.json` 中的 `dependencies` 是否包含了所有 .vue/.js 文件中 import 的第三方库？",
                "2. 每个 FILE 输出的文件内容是否完整，无占位符、无省略？",
                "3. `index.html` 是否有完整的 HTML 结构且内容不为空？",
                "4. `App.vue` 是否包含导航栏+`<router-view/>`+页脚，而非只有一个 `<router-view/>`？",
                "5. 是否存在变量初始化自引用？",
                "6. 是否存在未定义变量、未定义方法、未定义组件？",
                "7. 是否存在 import 错误？",
                "8. 是否能够通过 `npm run build` 或 `mvn clean package`？",
                "",
                "### 输出格式约束 (CRITICAL)",
                "你必须严格按照以下格式输出，解析器依赖此格式：",
                "1. **第一行**: 必须在第一行输出生成应用的类型（例如：APP_TYPE=HTML、APP_TYPE=VUE3、APP_TYPE=VUE3_SPRING）。",
                "2. **文件内容**: 紧接着按以下格式输出每个文件：",
                "FILE: <relative_path>",
                "```<language>",
                "<complete_file_content>",
                "```",
                "   - `<relative_path>`: 文件的相对路径。",
                "   - `<complete_file_content>`: 完整文件内容，绝对禁止省略、使用占位符或留空。",
                "",
                "### 最终强制要求 (CRITICAL)",
                "- 输出前必须在脑海中模拟执行：",
                "  - Vue项目：`npm install && npm run build`",
                "  - SpringBoot项目：`mvn clean package -DskipTests`",
                "- 如果发现任何编译错误，必须先修复再输出。",
                "- 禁止输出解释说明。",
                "- 禁止输出 markdown 标题。",
                "- 禁止输出多余文本。",
                "- 只能输出符合格式要求的代码内容。"
        );
    }
*/