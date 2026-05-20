package com.zmbdp.portal.service.flash.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天客户端配置
 *
 * @author 稚名不带撇
 */
@RefreshScope
@Configuration
public class ChatClientConfig {

    private final ChatProperties properties;

    public ChatClientConfig(ChatProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建聊天客户端 Bean
     * <p>
     * 支持 Nacos 动态刷新配置参数，包括核采样和思考模式开关。
     *
     * @param chatModel  底层聊天模型实例
     * @return 配置好的 ChatClient 实例
     */
    @Bean("chatClient")
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .topP(properties.getTopP()) // 配置核采样参数
                                .enableThinking(properties.getEnableThinking()) // 配置是否启用思考
                                .build()
                ).build();
    }
}