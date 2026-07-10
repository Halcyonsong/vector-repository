package io.github.halcyonsong.chat.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.halcyonsong.chat.pojo.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ChatSessionStore {

    private static final String SESSION_KEY_PREFIX = "chat:session:";
    private static final String SESSION_INDEX_KEY = "chat:session:index";
    private static final long MAX_SESSION_COUNT = 100L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveSession(SessionVO sessionVO) {
        try {
            String json = objectMapper.writeValueAsString(sessionVO);
            stringRedisTemplate.opsForValue().set(buildSessionKey(sessionVO.getSessionId()), json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("会话数据序列化失败: " + sessionVO.getSessionId(), exception);
        }
    }

    public SessionVO getSession(String sessionId) {
        String json = stringRedisTemplate.opsForValue().get(buildSessionKey(sessionId));
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, SessionVO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("会话数据反序列化失败: " + sessionId, exception);
        }
    }

    public Set<String> listSessionIds(long limit) {
        return stringRedisTemplate.opsForZSet()
                .reverseRange(SESSION_INDEX_KEY, 0, limit - 1);
    }

    // 刷新会话索引方法
    public void refreshSessionIndex(String sessionId, LocalDateTime updateTime) {
        // 转换为毫秒级时间戳，作为分数
        long score = updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // 获取有序集合操作对象
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        // 不存在添加，存在更新分数
        zSetOperations.add(SESSION_INDEX_KEY, sessionId, score);
        // 获取总数
        Long size = zSetOperations.zCard(SESSION_INDEX_KEY);
        // 删除最早的范围外的会话
        if (size != null && size > MAX_SESSION_COUNT) {
            zSetOperations.removeRange(SESSION_INDEX_KEY, 0, size - MAX_SESSION_COUNT - 1);
        }
    }

    public boolean deleteSession(String sessionId) {
        Boolean sessionDeleted = stringRedisTemplate.delete(buildSessionKey(sessionId));
        Long indexRemoved = stringRedisTemplate.opsForZSet().remove(SESSION_INDEX_KEY, sessionId);
        return Boolean.TRUE.equals(sessionDeleted) || (indexRemoved != null && indexRemoved > 0);
    }

    private String buildSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}