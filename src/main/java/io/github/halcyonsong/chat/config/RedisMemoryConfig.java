package io.github.halcyonsong.chat.config;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisMemoryConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;

    @Bean //准备存储仓库
    public RedissonRedisChatMemoryRepository redissonRedisChatMemoryRepository() {
        return RedissonRedisChatMemoryRepository.builder()
                .host(host)
                .port(port)
                .build();
    }


    @Bean
    public MessageWindowChatMemory messageWindowChatMemory(
            RedissonRedisChatMemoryRepository redissonRedisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                // 底层记忆存储仓库
                .chatMemoryRepository(redissonRedisChatMemoryRepository)
                // 单个会话最多保留多少条消息，超过窗口会滚动淘汰旧消息
                .maxMessages(20)
                .build();
    }

}
