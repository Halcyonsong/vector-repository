package io.github.halcyonsong.knowledge.service;

import io.github.halcyonsong.knowledge.pojo.vo.KnowledgeBaseUploadTaskVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseEmbeddingService {

    KnowledgeBaseUploadTaskVO uploadDocument(String knowledgeBaseId, MultipartFile file) throws Exception;

    KnowledgeBaseUploadTaskVO getUploadTask(String taskId);

    void deleteKnowledgeBase(String knowledgeBaseId);

    List<String> listKnowledgeBases();
}