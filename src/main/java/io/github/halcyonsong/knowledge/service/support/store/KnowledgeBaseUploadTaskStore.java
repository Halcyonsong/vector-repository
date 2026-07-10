package io.github.halcyonsong.knowledge.service.support.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.halcyonsong.knowledge.pojo.vo.KnowledgeBaseUploadTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseUploadTaskStore {

    private static final String TASK_KEY_PREFIX = "kb:task:";
    private static final Duration TASK_TTL = Duration.ofDays(1);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 缓存任务状态或覆盖更新
    public void saveTask(KnowledgeBaseUploadTaskVO task) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        try {
            String json = objectMapper.writeValueAsString(task);
            stringRedisTemplate.opsForValue().set(buildTaskKey(task.getTaskId()), json, TASK_TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("任务状态序列化失败: " + task.getTaskId(), exception);
        }
    }

    public KnowledgeBaseUploadTaskVO getTask(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        String json = stringRedisTemplate.opsForValue().get(buildTaskKey(taskId));
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, KnowledgeBaseUploadTaskVO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("任务状态反序列化失败: " + taskId, exception);
        }
    }

    public void updateTask(String taskId, Consumer<KnowledgeBaseUploadTaskVO> updater) {
        KnowledgeBaseUploadTaskVO task = getTask(taskId);
        if (task == null) {
            throw new IllegalStateException("任务不存在: " + taskId);
        }

        updater.accept(task);
        saveTask(task);
    }

    private String buildTaskKey(String taskId) {
        return TASK_KEY_PREFIX + taskId;
    }
}