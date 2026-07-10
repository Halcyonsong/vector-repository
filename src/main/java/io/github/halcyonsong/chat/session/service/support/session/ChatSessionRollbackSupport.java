package io.github.halcyonsong.chat.session.service.support.session;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import io.github.halcyonsong.chat.common.constants.ChatMemoryConstants;
import io.github.halcyonsong.chat.common.enums.ChatRoleEnum;
import io.github.halcyonsong.chat.session.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.session.service.support.history.ChatHistoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatSessionRollbackSupport {

    private final ChatHistoryStore chatHistoryStore;
    private final RedissonRedisChatMemoryRepository chatMemoryRepository;

    public List<ChatHistoryMessageVO> rollbackLastRound(String sessionId) {
        List<ChatHistoryMessageVO> fullHistory = chatHistoryStore.listAllHistory(sessionId);
        if (CollectionUtils.isEmpty(fullHistory)) {
            return fullHistory;
        }

        int roundEndIndex = findLastRoundEnd(fullHistory);
        if (roundEndIndex < 0) {
            return fullHistory;
        }

        int roundStartIndex = findLastRoundStart(fullHistory, roundEndIndex);
        if (roundStartIndex < 0) {
            return fullHistory;
        }

        List<ChatHistoryMessageVO> remainingHistory = new ArrayList<>(fullHistory);
        remainingHistory.subList(roundStartIndex, roundEndIndex + 1).clear();

        chatHistoryStore.replaceHistory(sessionId, remainingHistory);
        rebuildMemoryWindow(sessionId, remainingHistory);

        return remainingHistory;
    }

    private int findLastRoundEnd(List<ChatHistoryMessageVO> historyList) {
        for (int index = historyList.size() - 1; index >= 0; index--) {
            ChatHistoryMessageVO message = historyList.get(index);
            if (ChatRoleEnum.ASSISTANT.getValue().equals(message.getRole())) {
                return index;
            }
        }

        for (int index = historyList.size() - 1; index >= 0; index--) {
            ChatHistoryMessageVO message = historyList.get(index);
            if (ChatRoleEnum.USER.getValue().equals(message.getRole())) {
                return index;
            }
        }

        return -1;
    }

    private int findLastRoundStart(List<ChatHistoryMessageVO> historyList, int roundEndIndex) {
        for (int index = roundEndIndex; index >= 0; index--) {
            ChatHistoryMessageVO message = historyList.get(index);
            if (ChatRoleEnum.USER.getValue().equals(message.getRole())) {
                return index;
            }
        }
        return -1;
    }

    private void rebuildMemoryWindow(String sessionId, List<ChatHistoryMessageVO> remainingHistory) {
        chatMemoryRepository.deleteByConversationId(sessionId);

        if (CollectionUtils.isEmpty(remainingHistory)) {
            return;
        }

        int startIndex = Math.max(0, remainingHistory.size() - ChatMemoryConstants.MEMORY_MAX_MESSAGES);
        List<ChatHistoryMessageVO> memorySource = remainingHistory.subList(startIndex, remainingHistory.size());

        List<Message> rebuiltMessages = new ArrayList<>(memorySource.size());
        for (ChatHistoryMessageVO historyMessage : memorySource) {
            if (ChatRoleEnum.USER.getValue().equals(historyMessage.getRole())) {
                rebuiltMessages.add(new UserMessage(historyMessage.getContent()));
            } else if (ChatRoleEnum.ASSISTANT.getValue().equals(historyMessage.getRole())) {
                rebuiltMessages.add(new AssistantMessage(historyMessage.getContent()));
            }
        }

        if (!rebuiltMessages.isEmpty()) {
            chatMemoryRepository.saveAll(sessionId, rebuiltMessages);
        }
    }


}