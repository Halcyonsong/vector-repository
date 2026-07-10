package io.github.halcyonsong.chat.stream.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {

    // 用户的问题
    @NotBlank(message = "question 不能为空")
    private String question;
    // 会话id
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;
    // 是否启用知识库
    private Boolean useKnowledgeBase;
    // 没有检索到知识时，是否允许继续请求大模型
    private Boolean allowEmptyContext;
    // 指定使用哪个知识库
    private String knowledgeBaseId;
    // 检索返回的最大文档数，可为空，默认3
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 20, message = "topK 不能大于 20")
    private Integer topK;
    // 相似度阈值，可为空，默认0.5D
    @DecimalMin(value = "0.0", message = "similarityThreshold 不能小于 0")
    @DecimalMax(value = "1.0", message = "similarityThreshold 不能大于 1")
    private Double similarityThreshold;
    // 可选系统提示词；为空时使用配置文件 app.chat.system-prompt
    private String systemPrompt;
}