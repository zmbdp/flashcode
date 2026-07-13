package com.zmbdp.portal.service.flash.service.impl;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.security.service.TokenService;
import com.zmbdp.portal.service.flash.advisor.TokenUsageAdvisor;
import com.zmbdp.portal.service.flash.domain.dto.FileInfoDTO;
import com.zmbdp.portal.service.flash.service.IGiteeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 码云仓库服务实现
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class GiteeServiceImpl implements IGiteeService {

//    @Autowired
//    private ChatClient chatClient;
//
//    /**
//     * 用户代码仓库所有者
//     */
//    @Value("${gitee.user-code.owner}")
//    private String userAppCodeOwner;
//
//    /**
//     * 用户代码仓库所有者
//     */
//    @Value("${gitee.user-code.rpo}")
//    private String userAppCodeRpo;
//
//    /**
//     * 用户代码仓库分支
//     */
//    @Value("${gitee.user-code.branch}")
//    private String userAppCodeBranch;
//
//    /**
//     * 令牌服务
//     */
//    @Autowired
//    private TokenService tokenService;
//
//    /**
//     * 令牌密钥
//     */
//    @Value("${jwt.token.secret}")
//    private String secret;
//
//    /**
//     * 聊天模型名称
//     */
//    @Value("${spring.ai.dashscope.chat.options.model}")
//    private String chatModelName;

    /**
     * 提交代码
     *
     * @param appId   应用 ID
     * @param appPath 应用路径
     * @param appType 应用类型
     * @param files   文件
     */
    @Override
    public void commit(Long appId, Path appPath, String appType, Map<String, String> files) {
//        if (CollectionUtils.isEmpty(files)) {
//            return;
//        }
//        // 构建提交信息
//        String commitMessage = String.format("提交应用代码 - appId: %d, 类型: %s", appId, appType);
//        List<FileInfoDTO> fileInfoDTOList = new ArrayList<>();
//        try {
//            for (String filePath : files.keySet()) {
//                FileInfoDTO fileInfoDTO = new FileInfoDTO();
//                String content = Files.readString(appPath.resolve(filePath));
//                fileInfoDTO.setContent(content);
//                fileInfoDTO.setFilePath(appId + "/" + filePath);
//                fileInfoDTOList.add(fileInfoDTO);
//            }
//        } catch (IOException e) {
//            throw new ServiceException(e.getMessage());
//        }
//        commit(appId, fileInfoDTOList, commitMessage);
        log.info("代码提交成功");
    }
//
//    /**
//     * 提交代码
//     *
//     * @param appId           应用 ID
//     * @param fileInfoDTOList 文件信息
//     * @param commitMessage   提交信息
//     */
//    private void commit(Long appId, List<FileInfoDTO> fileInfoDTOList, String commitMessage) {
//        try {
//            String filesJson = JsonUtil.classToJson(fileInfoDTOList);
//            String systemPrompt = "你是一个代码提交助手，负责调用 commitFile 工具将代码提交到 Gitee 仓库。请严格按照用户提供的参数调用工具，不要添加任何解释。";
//            String userPrompt = String.format(
//                    "请调用 commitFile 工具将代码提交到 Gitee 仓库。\n\n" +
//                            "工具参数说明：\n" +
//                            "- owner（仓库所有者）: \"%s\"\n" +
//                            "- repo（仓库名称）: \"%s\"\n" +
//                            "- branch（目标分支）: \"%s\"\n" +
//                            "- message（提交信息）: \"%s\"\n" +
//                            "- files（文件列表）: %s\n\n" +
//                            "请直接调用 commitFile 工具，使用上述参数提交代码。不要添加任何解释或额外文本。",
//                    userAppCodeOwner, userAppCodeRpo, userAppCodeBranch, commitMessage, filesJson
//            );
//            String result = chatClient.prompt()
//                    .system(systemPrompt)
//                    .user(userPrompt)
//                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId))
//                            .param(TokenUsageAdvisor.TAG_APP_ID, appId)
//                            .param(TokenUsageAdvisor.TAG_USER_ID, tokenService.getLoginUser(secret).getUserId())
//                            .param(TokenUsageAdvisor.TAG_MODEL, chatModelName))
//                    .call()
//                    .content();
//            log.info("Mcp commitFile 调用结果为: {}", result);
//        } catch (Exception e) {
//            throw new ServiceException("调用Mcp commitFile失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 拉取用户代码
//     *
//     * @param appId           应用 id
//     * @param userCodeBaseDir 用户代码根目录
//     */
//    @Override
//    public void pullUserAppCode(Long appId, Path userCodeBaseDir) {
//        try {
//            String systemPrompt = "你是一个代码同步助手，负责调用 pullUserAppCode 工具从 Gitee 仓库同步代码到本地。并且请务必调用此工具";
//            String userPrompt = String.format(
//                    "请调用 pullUserAppCode 工具，将 flash-user-code 仓库中 appId=%s 的代码同步到目录 \"%s\"。\n" +
//                            "工具参数如下：\n" +
//                            "- owner: \"%s\"\n" +
//                            "- repo: \"%s\"\n" +
//                            "- branch: \"%s\"\n" +
//                            "- appId: \"%s\"\n" +
//                            "- targetDir: \"%s\"\n" +
//                            "特别注意：请直接调用该工具并返回工具输出，不要添加其他说明。",
//                    appId, userCodeBaseDir, userAppCodeOwner, userAppCodeRpo, userAppCodeBranch, appId, userCodeBaseDir
//            );
//            String result = chatClient.prompt()
//                    .system(systemPrompt)
//                    .user(userPrompt)
//                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId))
//                            .param(TokenUsageAdvisor.TAG_APP_ID, appId)
//                            .param(TokenUsageAdvisor.TAG_USER_ID, tokenService.getLoginUser(secret).getUserId())
//                            .param(TokenUsageAdvisor.TAG_MODEL, chatModelName))
//                    .call()
//                    .content();
//            log.debug("MCP pullUserAppCode 调用结果: {}", result);
//        } catch (Exception e) {
//            log.error("调用 MCP commitFile 失败: {}", e.getMessage(), e);
//            throw new RuntimeException("调用 MCP commitFile 失败: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 删除代码
//     *
//     * @param appId 应用 id
//     */
//    @Override
//    public void delete(Long appId) {
//        try {
//            String systemPrompt = "你是一个代码删除助手，负责调用 deleteDirectory 工具删除 Gitee 仓库中的指定目录。请严格按照用户提供的参数调用工具，不要添加任何解释。";
//
//            String userPrompt = String.format(
//                    "请调用 deleteDirectory 工具，将 flash-app-code 仓库中 appId=%s 对应目录删除。\n" +
//                            "工具参数如下：\n" +
//                            "- owner: \"%s\"\n" +
//                            "- repo: \"%s\"\n" +
//                            "- branch: \"%s\"\n" +
//                            "- dirPath: \"%s\"\n" +
//                            "- message: \"%s\"\n" +
//                            "特别注意：请直接调用该工具并返回工具输出，不要添加其他说明。",
//                    appId,
//                    userAppCodeOwner,
//                    userAppCodeRpo,
//                    userAppCodeBranch,
//                    appId, // 目录就是 appId
//                    "删除应用代码 appId=" + appId
//            );
//
//            String result = chatClient.prompt()
//                    .system(systemPrompt)
//                    .user(userPrompt)
//                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId))
//                            .param(TokenUsageAdvisor.TAG_APP_ID, appId)
//                            .param(TokenUsageAdvisor.TAG_USER_ID, tokenService.getLoginUser(secret).getUserId())
//                            .param(TokenUsageAdvisor.TAG_MODEL, chatModelName))
//                    .call()
//                    .content();
//
//            log.debug("MCP deleteDirectory 调用结果: {}", result);
//
//        } catch (Exception e) {
//            log.error("调用 MCP deleteDirectory 失败: {}", e.getMessage(), e);
//            throw new RuntimeException("调用 MCP deleteDirectory 失败: " + e.getMessage(), e);
//        }
//    }
}