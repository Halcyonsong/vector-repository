package io.github.halcyonsong.chat.sum.service.support.summary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.halcyonsong.chat.sum.pojo.vo.ChatSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatSummaryStore {

    private static final String CHAT_SUMMARY_KEY_PREFIX = "chat:summary:";
    private static final String CHAT_SUMMARY_CURSOR_KEY_PREFIX = "chat:summary:cursor:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 追加摘要
    public void appendSummary(String sessionId, ChatSummaryVO summary) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (summary == null) {
            throw new IllegalArgumentException("summary 不能为空");
        }

        try {
            String json = objectMapper.writeValueAsString(summary);
            stringRedisTemplate.opsForList().rightPush(buildSummaryKey(sessionId), json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("摘要序列化失败: " + sessionId, exception);
        }
    }

    // 获取所有摘要
    public List<ChatSummaryVO> listSummaries(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }

        List<String> summaryJsonList = stringRedisTemplate.opsForList().range(buildSummaryKey(sessionId), 0, -1);
        if (summaryJsonList == null || summaryJsonList.isEmpty()) {
            return List.of();
        }

        List<ChatSummaryVO> summaryList = new ArrayList<>(summaryJsonList.size());
        for (String summaryJson : summaryJsonList) {
            try {
                summaryList.add(objectMapper.readValue(summaryJson, ChatSummaryVO.class));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("摘要反序列化失败: " + sessionId, exception);
            }
        }

        return summaryList;
    }

    // 获取压缩游标
    public int getCompressedCursor(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }

        String cursorValue = stringRedisTemplate.opsForValue().get(buildCursorKey(sessionId));
        if (!StringUtils.hasText(cursorValue)) {
            return 0;
        }

        try {
            return Math.max(Integer.parseInt(cursorValue), 0);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("摘要游标格式非法: " + sessionId + ", value=" + cursorValue, exception);
        }
    }

    // 保存压缩游标
    public void saveCompressedCursor(String sessionId, int cursor) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (cursor < 0) {
            throw new IllegalArgumentException("cursor 不能小于 0");
        }

        stringRedisTemplate.opsForValue().set(buildCursorKey(sessionId), String.valueOf(cursor));
    }

    // 清除会话摘要及游标
    public void clearSessionSummary(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }

        stringRedisTemplate.delete(buildSummaryKey(sessionId));
        stringRedisTemplate.delete(buildCursorKey(sessionId));
    }

    // 替换会话摘要
    public void replaceSummaries(String sessionId, List<ChatSummaryVO> summaries) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (summaries == null) {
            throw new IllegalArgumentException("summaries 不能为空");
        }

        stringRedisTemplate.delete(buildSummaryKey(sessionId));
        if (summaries.isEmpty()) {
            return;
        }

        List<String> jsonList = new ArrayList<>(summaries.size());
        for (ChatSummaryVO summary : summaries) {
            try {
                jsonList.add(objectMapper.writeValueAsString(summary));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("摘要序列化失败: " + sessionId, exception);
            }
        }

        stringRedisTemplate.opsForList().rightPushAll(buildSummaryKey(sessionId), jsonList);
    }

    private String buildSummaryKey(String sessionId) {
        return CHAT_SUMMARY_KEY_PREFIX + sessionId;
    }

    private String buildCursorKey(String sessionId) {
        return CHAT_SUMMARY_CURSOR_KEY_PREFIX + sessionId;
    }


}