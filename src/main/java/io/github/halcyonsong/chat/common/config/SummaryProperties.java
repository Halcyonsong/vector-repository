package io.github.halcyonsong.chat.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.summary")
public class SummaryProperties {

    // 摘要系统提示词
    private String summarySystemPrompt;

    // 触发摘要压缩的最小未压缩历史条数
    private Integer compressThreshold = 10;

}
