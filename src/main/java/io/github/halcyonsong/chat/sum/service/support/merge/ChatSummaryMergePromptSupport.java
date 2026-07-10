package io.github.halcyonsong.chat.sum.service.support.merge;

import io.github.halcyonsong.chat.common.config.SummaryMergeProperties;
import io.github.halcyonsong.chat.sum.pojo.vo.ChatSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatSummaryMergePromptSupport {

    private final SummaryMergeProperties summaryMergeProperties;

    public String buildMergeSystemPrompt() {
        if (!StringUtils.hasText(summaryMergeProperties.getSummaryMergePrompt())) {
            throw new IllegalStateException("摘要系统提示词未配置");
        }
        return summaryMergeProperties.getSummaryMergePrompt().trim();
    }

    public String buildMergeUserPrompt(List<ChatSummaryVO> summaryList) {
        if (CollectionUtils.isEmpty(summaryList)) {
            throw new IllegalArgumentException("summaryList 不能为空");
        }

        StringBuilder promptBuilder = new StringBuilder(buildMergeSystemPrompt());

        for (int index = 0; index < summaryList.size(); index++) {
            ChatSummaryVO summary = summaryList.get(index);
            if (summary == null || !StringUtils.hasText(summary.getContent())) {
                continue;
            }

            String positionLabel = index == 0 ? "较早摘要" : (index == summaryList.size() - 1 ? "较新摘要" : "中间摘要");

            promptBuilder.append(positionLabel)
                    .append(index + 1)
                    .append(":\n")
                    .append(summary.getContent().trim())
                    .append("\n\n");
        }

        return promptBuilder.toString();
    }
}