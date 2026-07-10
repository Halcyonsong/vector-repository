package io.github.halcyonsong.chat.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatHistoryStore {

    private static final String CHAT_HISTORY_KEY_PREFIX = "chat:history:";
    private static final int HISTORY_PAGE_SIZE = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 追加对话消息
    public void appendMessage(String sessionId, ChatHistoryMessageVO message) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (message == null) {
            throw new IllegalArgumentException("message 不能为空");
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.opsForList().rightPush(buildHistoryKey(sessionId), json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("历史消息序列化失败: " + sessionId, exception);
        }
    }

    // 删除会话历史
    public void deleteHistory(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        stringRedisTemplate.delete(buildHistoryKey(sessionId));
    }

    // 构建历史会话索引键
    private String buildHistoryKey(String sessionId) {
        return CHAT_HISTORY_KEY_PREFIX + sessionId;
    }

    // 列表查询历史消息
    public ChatHistoryPageVO listHistory(String sessionId, Integer beforeIndex) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }

        Long total = stringRedisTemplate.opsForList().size(buildHistoryKey(sessionId));
        if (total == null || total <= 0) {
            return ChatHistoryPageVO.builder()
                    .records(List.of())
                    .nextCursor(null)
                    .hasMore(false)
                    .total(0L)
                    .build();
        }
        // end 表示查到哪里结束，beforeIndex 表示当前已获取的最早消息索引
        long end;
        if (beforeIndex == null) {
            end = total - 1;
        } else {
            end = Math.min((long) beforeIndex - 1, total - 1);
        }

        if (end < 0) {
            return ChatHistoryPageVO.builder()
                    .records(List.of())
                    .nextCursor(null)
                    .hasMore(false)
                    .total(total)
                    .build();
        }
        // start 表示从哪里开始查
        long start = Math.max(0, end - HISTORY_PAGE_SIZE + 1);

        List<String> messageJsonList = stringRedisTemplate.opsForList()
                .range(buildHistoryKey(sessionId), start, end);

        if (messageJsonList == null || messageJsonList.isEmpty()) {
            return ChatHistoryPageVO.builder()
                    .records(List.of())
                    .nextCursor(null)
                    .hasMore(false)
                    .total(total)
                    .build();
        }

        List<ChatHistoryMessageVO> historyList = new ArrayList<>(messageJsonList.size());
        for (String messageJson : messageJsonList) {
            try {
                ChatHistoryMessageVO message = objectMapper.readValue(messageJson, ChatHistoryMessageVO.class);
                historyList.add(message);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("历史消息反序列化失败: " + sessionId, exception);
            }
        }

        boolean hasMore = start > 0;
        Integer nextCursor = hasMore ? (int) start : null;

        return ChatHistoryPageVO.builder()
                .records(historyList)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .total(total)
                .build();
    }




}