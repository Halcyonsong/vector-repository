package io.github.halcyonsong.knowledge.service.support.parser;

import org.springframework.ai.document.Document;

import java.util.List;

public interface KnowledgeBaseDocumentParser {

    List<Document> parse(String taskId,
                         String knowledgeBaseId,
                         byte[] fileBytes,
                         String fileName,
                         String fileType) throws Exception;
}