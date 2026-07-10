package io.github.halcyonsong.knowledge.controller;

import io.github.halcyonsong.common.result.Result;
import io.github.halcyonsong.knowledge.service.KnowledgeBaseEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ai/kb")
@RequiredArgsConstructor
public class KnowledgeBaseEmbeddingController {

    private final KnowledgeBaseEmbeddingService knowledgeBaseEmbeddingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Integer> uploadDocument(@RequestParam("knowledgeBaseId") String knowledgeBaseId,
                                          @RequestParam("file") MultipartFile file) throws Exception {
        return Result.success(knowledgeBaseEmbeddingService.uploadDocument(knowledgeBaseId, file));
    }

    @DeleteMapping("/delete")
    public Result<Void> deleteKnowledgeBase(@RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        knowledgeBaseEmbeddingService.deleteKnowledgeBase(knowledgeBaseId);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<String>> listKnowledgeBases() {
        return Result.success(knowledgeBaseEmbeddingService.listKnowledgeBases());
    }


}