package com.zmbdp.portal.service.flash.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天动态配置
 *
 * @author 稚名不带撇
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.dashscope.chat.options")
public class ChatProperties {

    /**
     * 核采样参数（Top-P）<br>
     * 取值范围：0.0 ~ 1.0<br>
     * 值越小，生成结果越确定、保守，适合事实性问答<br>
     * 值越大，生成结果越多样、有创造性，适合创意写作
     */
    private Double topP = 0.6;

    /**
     * 是否启用思考
     */
    private Boolean enableThinking = true;
}