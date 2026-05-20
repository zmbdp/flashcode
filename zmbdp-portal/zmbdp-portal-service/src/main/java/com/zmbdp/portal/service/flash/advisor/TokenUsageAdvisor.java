package com.zmbdp.portal.service.flash.advisor;

import com.alibaba.nacos.common.utils.StringUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TokenUsageAdvisor implements BaseAdvisor {

    public static final String TAG_USER_ID = "userId";
    public static final String TAG_APP_ID = "appId";
    public static final String TAG_MODEL = "model";
    @Value("${spring.ai.context-window.coder:32}")
    private int codeWindow;
    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
//        - Token消耗：
//        - 单个应用Token消耗
//         - 单个用户Token消耗
//        - 某个模型的Token消耗
//      - 上下文消耗：
//        - 单次对话上下文使用占比
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            return chatClientResponse;
        }
        Map<String, Object> context = chatClientResponse.context();
        String model = (String) context.get(TAG_MODEL);
        Long userId = (Long) context.get(TAG_USER_ID);
        Long appId = (Long) context.get(TAG_APP_ID);

        Usage usage = chatResponse.getMetadata().getUsage();
        Integer promptTokens = usage.getPromptTokens(); //输入token
        Integer completionTokens = usage.getCompletionTokens(); //输出token
        Integer totalTokens = usage.getTotalTokens(); //token总数

        double contextUsageRate = calculateContextUsageRate(promptTokens);
        record(userId, appId, model, promptTokens, completionTokens, totalTokens, contextUsageRate);

        return chatClientResponse;
    }

    public void record(Long userId, Long appId, String model,
                       Integer promptTokens, Integer completionTokens,
                       Integer totalTokens, Double contextUsageRate) {

        Tags tags = Tags.of(
                TAG_USER_ID, safeTag(userId),
                TAG_APP_ID, safeTag(appId),
                TAG_MODEL, safeModel(model)
        );

        if (totalTokens > 0) {
            Counter.builder("ai_token_total")
                    .description("LLM total tokens by user/app/model")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(totalTokens);
        }

        if (promptTokens > 0) {
            Counter.builder("ai_token_prompt")
                    .description("LLM prompt tokens by user/app/model")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(promptTokens);
        }

        if (completionTokens > 0) {
            Counter.builder("ai_token_completion")
                    .description("LLM completion tokens by user/app/model")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(completionTokens);
        }

        // 记录上下文使用率
        if (contextUsageRate != null && contextUsageRate >= 0) {
            Gauge.builder("ai_context_usage_rate", () -> contextUsageRate)
                    .description("LLM context window usage rate (0-100) by user/app/model")
                    .tags(tags)
                    .register(meterRegistry);
        }
    }

    private String safeTag(Long value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private String safeModel(String model) {
        return StringUtils.hasText(model) ? model : "unknown";
    }

    public double calculateContextUsageRate(Integer promptTokens) {
        if (promptTokens == null || promptTokens <= 0) {
            return 0.0;
        }
        double rate = (double) promptTokens / codeWindow * 100.0;
        // 限制在 0-100 之间
        return Math.min(100.0, Math.max(0.0, rate));
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}