package io.github.halcyonsong.knowledge.service.support;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseDocumentParser {

    List<Document> parse(String knowledgeBaseId, MultipartFile file, String fileName, String fileType) throws Exception;
}