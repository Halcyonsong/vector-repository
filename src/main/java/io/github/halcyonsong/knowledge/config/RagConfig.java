package io.github.halcyonsong.knowledge.config;

import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean // 上下文查询增强器，决定检索到的知识如何呈现
    public ContextualQueryAugmenter contextualQueryAugmenter() {
        return ContextualQueryAugmenter.builder()
                // 没搜到相关知识，也允许将请求继续转发
                .allowEmptyContext(true)
                .build();
    }

}