package com.zmbdp.portal.service.flash.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.security.service.TokenService;
import com.zmbdp.portal.service.flash.advisor.TokenUsageAdvisor;
import com.zmbdp.portal.service.flash.domain.dto.RequirementsDTO;
import com.zmbdp.portal.service.flash.domain.entity.App;
import com.zmbdp.portal.service.flash.mapper.AppMapper;
import com.zmbdp.portal.service.flash.service.IRequirementsService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 应用需求服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
@RefreshScope
public class RequirementsServiceImpl implements IRequirementsService {

    @Autowired
    private ChatClient chatClient;

    /**
     * 令牌服务
     */
    @Autowired
    private TokenService tokenService;

    /**
     * 令牌密钥
     */
    @Value("${jwt.token.secret}")
    private String secret;

    @Autowired
    private AppMapper appMapper;

    /**
     * 系统提示词
     */
    @Value("${spring.ai.dashscope.chat.options.doc-system-prompt:}")
    private String docSystemPrompt;

    /**
     * 模型名称
     */
    @Value("${spring.ai.dashscope.chat.options.flash.model.doc:qwen-turbo}")
    private String docModelName;

    @Value("${spring.ai.dashscope.chat.options.thinking.doc:true}")
    private boolean docModeThinking;

    // 如果配置中心取不到，用默认值

    /**
     * 初始化系统提示词
     */
    @PostConstruct
    public void initDocSystemPrompt() {
        if (docSystemPrompt == null || docSystemPrompt.isBlank()) {
            docSystemPrompt = String.join("\n",
                    "你是资深产品经理。也是 FlashCode 平台的智能助手小闪，根据用户提供的需求，生成正式且简洁的应用需求文档。请使用 Markdown 严格排版，采用如下结构与编号：",
                    "# 应用需求文档",
                    "## 1. 应用名称",
                    "## 2. 应用描述",
                    "简要说明应用的目标用户和解决的核心痛点，50-100字。",
                    "## 3. 应用核心功能",
                    "核心功能采用 3.1、3.2… 的编号格式分点呈现。",
                    "## 4. 技术约束",
                    "若用户在需求中明确指定了技术栈（如前端框架、构建工具、UI库等），在此章节完整列出用户指定的技术约束，不得遗漏或自行添加。若用户未指定技术栈，此章节可省略。",
                    "规则：",
                    "1. 若用户已明确输入核心功能，完全以用户输入内容为准；",
                    "2. 若用户未提及核心功能，结合需求生成不超过两个极简功能，每个功能均为独立的数据列表管理功能（如列表展示、增删改查、筛选排序），所有功能均独立实现，不依赖任何第三方服务或平台；",
                    "3. 不主动推荐第三方服务；若用户提及具体平台（如微信、支付宝等），将其转化为通用功能模块描述，不出现具体平台名称；",
                    "4. 若用户输入中出现技术栈相关内容（如HTML、Vue3、Spring Boot等），属于技术约束范畴，不在规则3的屏蔽范围内，应如实写入第4章技术约束中；",
                    "5. 若用户输入与应用需求无关或过于模糊，仅回复：\"小闪需要您输入具体的应用需求描述，例如应用的目标用户和核心使用场景。\"",
                    "注意：仅输出符合以上要求的需求文档正文，不要任何附加说明、解释或多余内容。"
            );
        }
    }

    /**
     * 应用需求生成
     *
     * @param input 用户输入
     * @return 应用需求
     */
    @Override
    public RequirementsDTO requirementsGenerate(String input) {
        // 用户 id 获取
        Long userId = tokenService.getLoginUser(secret).getUserId();
        App app = new App();
        app.setUserId(userId);
        appMapper.insert(app);
        String conversationId = String.valueOf(app.getId());
        // =====
//        String appDoc = chatClient.prompt()
//                .system(systemPrompt)
//                .user(input)
//                .options(DashScopeChatOptions.builder()
//                        .model(docModelName)  // 指定模型，把文档模型和代码生成模型分开
//                        .enableThinking(docModeThinking) // 是否开启思考模式
//                        .build())
//                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)
//                        .param(TokenUsageAdvisor.TAG_APP_ID, app.getId())
//                        .param(TokenUsageAdvisor.TAG_USER_ID, userId)
//                        .param(TokenUsageAdvisor.TAG_MODEL, docModelName))
//                .call()
//                .content();

        StringBuilder appDocBuilder = new StringBuilder();
        try {
            chatClient.prompt()
                    .system(docSystemPrompt)
                    .user(input)
                    .options(DashScopeChatOptions.builder()
                            .model(docModelName)  // 指定模型，把文档模型和代码生成模型分开
                            .enableThinking(docModeThinking) // 是否开启思考模式
                            .build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)
                            .param(TokenUsageAdvisor.TAG_APP_ID, app.getId())
                            .param(TokenUsageAdvisor.TAG_USER_ID, userId)
                            .param(TokenUsageAdvisor.TAG_MODEL, docModelName))
                    .stream()
                    .content()
                    .doOnNext(appDocBuilder::append)
                    .blockLast();
        } catch (Exception e) {
            log.error("AI 文档生成失败", e);
            throw new ServiceException("AI 文档生成失败");
        }
        // 拼接完整结果
        String appDoc = appDocBuilder.toString();
        // =====

        //应用名称 + 描述 获取
        Map<String, String> parseAppDocResult = parseAppDoc(appDoc);
        String appName = parseAppDocResult.get("appName");
        String appDesc = parseAppDocResult.get("appDesc");

        app.setAppName(appName);
        app.setAppDesc(appDesc);
        app.setAppDoc(appDoc);
        appMapper.updateById(app);

        RequirementsDTO dto = new RequirementsDTO();
        dto.setAppId(app.getId());
        if (StringUtil.isNotBlank(appDoc)) {
            dto.setContent(appDoc);
        } else {
            log.warn("未能从内容中解析出应用需求文档");
            dto.setContent("解析应用信息失败，请联系管理~~~");
        }
        return dto;
    }

    /**
     * 从 AI 返回的 Markdown 内容中解析应用信息（应用名称和应用描述）
     *
     * @param content AI 返回的 Markdown 格式内容
     * @return 包含应用名称 (appName) 和应用描述 (appDesc) 的 Map
     */
    private Map<String, String> parseAppDoc(String content) {
        Map<String, String> result = new HashMap<>();

        if (content == null || content.isEmpty()) {
            log.warn("解析应用信息失败：内容为空");
            result.put("appName", "");
            result.put("appDesc", "");
            return result;
        }

        // 解析应用名称：匹配 "## 1. 应用名称 " 之后到下一个 "##" 之前的内容
        String appName = parseSection(content, 1, "应用名称");
        result.put("appName", appName);
        log.debug("成功解析应用名称: {}", appName);

        // 解析应用描述：匹配 "## 2. 应用描述 " 之后到下一个 "##" 之前的内容
        String appDesc = parseSection(content, 2, "应用描述");
        result.put("appDesc", appDesc);
        log.debug("成功解析应用描述: {}", appDesc);

        return result;
    }

    /**
     * 从 Markdown 内容中解析指定章节的内容
     *
     * @param content       Markdown 格式内容
     * @param sectionNumber 章节编号
     * @param sectionName   章节名称
     * @return 章节内容
     */
    private String parseSection(String content, int sectionNumber, String sectionName) {
        String regex = String.format("##\\s*%d\\.\\s*%s\\s*[\\r\\n]+([^#]+?)(?=##|$)", sectionNumber, sectionName);
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        log.warn("未能从内容中解析出{}", sectionName);
        return "";
    }
}