package io.github.halcyonsong.chat.stream.service.support.options;

import io.github.halcyonsong.chat.common.config.ChatProperties;
import io.github.halcyonsong.chat.sum.pojo.vo.ChatSummaryVO;
import io.github.halcyonsong.chat.sum.service.support.summary.ChatSummaryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatRequestOptionsSupport {

    private final ChatProperties chatProperties;
    private final ChatSummaryStore chatSummaryStore;

    public boolean normalizeAllowEmptyContext(Boolean allowEmptyContext) {
        return allowEmptyContext == null || allowEmptyContext;
    }

    public int normalizeTopK(Integer topK) {
        return topK == null ? 3 : topK;
    }

    public double normalizeSimilarityThreshold(Double similarityThreshold) {
        return similarityThreshold == null ? 0.5D : similarityThreshold;
    }

    public String resolveSystemPrompt(String sessionId, String requestSystemPrompt) {
        String basePrompt = StringUtils.hasText(requestSystemPrompt)
                ? requestSystemPrompt.trim()
                : chatProperties.getChatSystemPrompt();

        if (!StringUtils.hasText(sessionId)) {
            return basePrompt;
        }

        List<ChatSummaryVO> summaryList = chatSummaryStore.listSummaries(sessionId);
        if (CollectionUtils.isEmpty(summaryList)) {
            return basePrompt;
        }

        String summaryContext = buildSummaryContext(summaryList);
        if (!StringUtils.hasText(summaryContext)) {
            return basePrompt;
        }

        return basePrompt + "\n\n" + summaryContext;
    }

    private String buildSummaryContext(List<ChatSummaryVO> summaryList) {
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("以下是当前会话已整理的历史摘要，请在回答时继承这些上下文，避免重复收集已确认信息：\n");

        int displayIndex = 1;
        for (ChatSummaryVO summary : summaryList) {
            if (summary == null || !StringUtils.hasText(summary.getContent())) {
                continue;
            }

            summaryBuilder.append(displayIndex++)
                    .append(". ")
                    .append(summary.getContent().trim())
                    .append("\n");
        }

        return displayIndex == 1 ? "" : summaryBuilder.toString().trim();
    }

}