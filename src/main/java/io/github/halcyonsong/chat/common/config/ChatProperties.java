package io.github.halcyonsong.chat.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    private String chatSystemPrompt;


}