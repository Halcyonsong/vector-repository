package io.github.halcyonsong.chat.stream.service.support.advisor;

import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import io.github.halcyonsong.knowledge.common.config.RagAdvisorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAdvisorSupport {

    private final RagAdvisorFactory ragAdvisorFactory;

    public ChatClientRequestSpec applyKnowledgeBaseAdvisor(ChatClientRequestSpec requestSpec,
                                                           Boolean useKnowledgeBase,
                                                           Boolean allowEmptyContext,
                                                           String knowledgeBaseId,
                                                           Integer topK,
                                                           Double similarityThreshold) {
        if (!Boolean.TRUE.equals(useKnowledgeBase)) {
            log.info("knowledge base disabled");
            return requestSpec;
        }

        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "启用知识库时，knowledgeBaseId 不能为空");
        }

        log.info("knowledge base enabled, allowEmptyContext={}, knowledgeBaseId={}, topK={}, similarityThreshold={}",
                allowEmptyContext, knowledgeBaseId, topK, similarityThreshold);

        return requestSpec.advisors(
                ragAdvisorFactory.create(allowEmptyContext, knowledgeBaseId, topK, similarityThreshold)
        );
    }
}