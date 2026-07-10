package io.github.halcyonsong.chat.sum.service.support.summary;

import io.github.halcyonsong.chat.common.config.SummaryProperties;
import io.github.halcyonsong.chat.common.enums.ChatRoleEnum;
import io.github.halcyonsong.chat.session.pojo.vo.ChatHistoryMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatSummaryPromptSupport {

    private final SummaryProperties summaryProperties;

    public String buildSummarySystemPrompt() {
        if (!StringUtils.hasText(summaryProperties.getSummarySystemPrompt())) {
            throw new IllegalStateException("摘要系统提示词未配置");
        }
        return summaryProperties.getSummarySystemPrompt().trim();
    }

    public String buildSummaryUserPrompt(List<ChatHistoryMessageVO> historyMessages) {
        if (CollectionUtils.isEmpty(historyMessages)) {
            throw new IllegalArgumentException("historyMessages 不能为空");
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请压缩下面这段历史对话：\n\n");

        for (ChatHistoryMessageVO message : historyMessages) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }

            String roleLabel = ChatRoleEnum.USER.getValue().equals(message.getRole()) ? "用户" : "助手";
            promptBuilder.append(roleLabel)
                    .append(": ")
                    .append(message.getContent().trim())
                    .append("\n\n");
        }

        return promptBuilder.toString();
    }

}