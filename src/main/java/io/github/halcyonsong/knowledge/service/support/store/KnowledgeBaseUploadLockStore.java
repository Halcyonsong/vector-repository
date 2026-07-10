package io.github.halcyonsong.knowledge.service.support.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseUploadLockStore {

    private static final String UPLOAD_LOCK_KEY_PREFIX = "kb:uploading:";
    private static final Duration LOCK_TTL = Duration.ofHours(2);

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) " +
                            "else " +
                            "return 0 " +
                            "end",
                    Long.class
            );

    private final StringRedisTemplate stringRedisTemplate;

    public boolean tryAcquire(String knowledgeBaseId, String taskId) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new IllegalArgumentException("knowledgeBaseId 不能为空");
        }
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(buildLockKey(knowledgeBaseId), taskId, LOCK_TTL);

        return Boolean.TRUE.equals(success);
    }

    public boolean release(String knowledgeBaseId, String taskId) {
        if (!StringUtils.hasText(knowledgeBaseId) || !StringUtils.hasText(taskId)) {
            return false;
        }

        Long result = stringRedisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                Collections.singletonList(buildLockKey(knowledgeBaseId)),
                taskId
        );

        return Long.valueOf(1L).equals(result);
    }

    public boolean isLocked(String knowledgeBaseId) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            return false;
        }
        Boolean exists = stringRedisTemplate.hasKey(buildLockKey(knowledgeBaseId));
        return Boolean.TRUE.equals(exists);
    }

    private String buildLockKey(String knowledgeBaseId) {
        return UPLOAD_LOCK_KEY_PREFIX + knowledgeBaseId;
    }
}