package io.github.halcyonsong.knowledge.common.config;

import io.github.halcyonsong.knowledge.common.constants.KnowledgeMetadataConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagAdvisorFactory {

    private final VectorStore vectorStore;

    public RetrievalAugmentationAdvisor create(Boolean allowEmptyContext,
                                               String knowledgeBaseId,
                                               Integer topK,
                                               Double similarityThreshold) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(
                        filterExpressionBuilder.eq(KnowledgeMetadataConstants.KNOWLEDGE_BASE_ID, knowledgeBaseId).build()
                )
                .build();

        ContextualQueryAugmenter contextualQueryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(Boolean.TRUE.equals(allowEmptyContext))
                .build();


        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(contextualQueryAugmenter)
                .build();
    }
}