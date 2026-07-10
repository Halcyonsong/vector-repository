package io.github.halcyonsong.knowledge.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseEmbeddingService {

    Integer uploadDocument(String knowledgeBaseId, MultipartFile file) throws Exception;

    void deleteKnowledgeBase(String knowledgeBaseId);

    List<String> listKnowledgeBases();

}