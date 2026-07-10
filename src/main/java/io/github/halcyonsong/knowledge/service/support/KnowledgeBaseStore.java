package io.github.halcyonsong.knowledge.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseStore {

    private static final String KNOWLEDGE_BASE_INDEX_KEY = "kb:index";

    private final StringRedisTemplate stringRedisTemplate;

    public void addKnowledgeBase(String knowledgeBaseId) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new IllegalArgumentException("knowledgeBaseId 不能为空");
        }
        stringRedisTemplate.opsForSet().add(KNOWLEDGE_BASE_INDEX_KEY, knowledgeBaseId);
    }

    public void removeKnowledgeBase(String knowledgeBaseId) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(KNOWLEDGE_BASE_INDEX_KEY, knowledgeBaseId);
    }

    public List<String> listKnowledgeBases() {
        Set<String> knowledgeBaseSet = stringRedisTemplate.opsForSet().members(KNOWLEDGE_BASE_INDEX_KEY);
        if (knowledgeBaseSet == null || knowledgeBaseSet.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(knowledgeBaseSet);
    }

    public boolean exists(String knowledgeBaseId) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            return false;
        }
        Boolean member = stringRedisTemplate.opsForSet().isMember(KNOWLEDGE_BASE_INDEX_KEY, knowledgeBaseId);
        return Boolean.TRUE.equals(member);
    }
}