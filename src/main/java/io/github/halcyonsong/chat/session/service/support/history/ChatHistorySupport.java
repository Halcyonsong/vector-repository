package io.github.halcyonsong.chat.session.service.support.history;

import io.github.halcyonsong.chat.common.enums.ChatHistoryStatusEnum;
import io.github.halcyonsong.chat.common.enums.ChatRoleEnum;
import io.github.halcyonsong.chat.session.pojo.vo.ChatHistoryMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatHistorySupport {

    private final ChatHistoryStore chatHistoryStore;

    public void appendUserHistory(String sessionId, String question) {
        chatHistoryStore.appendMessage(
                sessionId,
                ChatHistoryMessageVO.builder()
                        .role(ChatRoleEnum.USER.getValue())
                        .content(question)
                        .status(ChatHistoryStatusEnum.COMPLETED.getValue())
                        .createTime(LocalDateTime.now())
                        .build()
        );
    }

    public void appendAssistantHistory(String sessionId, String content, String status) {
        if (!StringUtils.hasText(content)) {
            return;
        }

        chatHistoryStore.appendMessage(
                sessionId,
                ChatHistoryMessageVO.builder()
                        .role(ChatRoleEnum.ASSISTANT.getValue())
                        .content(content)
                        .status(status)
                        .createTime(LocalDateTime.now())
                        .build()
        );
    }
}