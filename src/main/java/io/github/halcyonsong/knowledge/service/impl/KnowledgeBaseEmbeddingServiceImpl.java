package io.github.halcyonsong.knowledge.service.impl;

import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import io.github.halcyonsong.knowledge.constants.KnowledgeMetadataConstants;
import io.github.halcyonsong.knowledge.enums.FileTypeEnum;
import io.github.halcyonsong.knowledge.service.KnowledgeBaseEmbeddingService;
import io.github.halcyonsong.knowledge.service.support.KnowledgeBaseDocumentParser;
import io.github.halcyonsong.knowledge.service.support.KnowledgeBaseStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseEmbeddingServiceImpl implements KnowledgeBaseEmbeddingService {

    private final VectorStore vectorStore;
    private final KnowledgeBaseStore knowledgeBaseStore;
    private final KnowledgeBaseDocumentParser knowledgeBaseDocumentParser;

    @Override
    public Integer uploadDocument(String knowledgeBaseId, MultipartFile file) throws Exception {
        String fileName = validateUploadRequest(knowledgeBaseId, file);
        String fileType = resolveFileType(fileName);

        List<Document> documentList = knowledgeBaseDocumentParser.parse(
                knowledgeBaseId,
                file,
                fileName,
                fileType
        );

        if (documentList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.DOCUMENT_PARSE_ERROR.getCode(), "文档中未解析到可向量化的文本内容");
        }

        vectorStore.add(documentList);
        knowledgeBaseStore.addKnowledgeBase(knowledgeBaseId);

        return documentList.size();
    }

    @Override
    public void deleteKnowledgeBase(String knowledgeBaseId) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "knowledgeBaseId 不能为空");
        }

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        vectorStore.delete(
                filterExpressionBuilder.eq(KnowledgeMetadataConstants.KNOWLEDGE_BASE_ID, knowledgeBaseId).build()
        );

        knowledgeBaseStore.removeKnowledgeBase(knowledgeBaseId);
    }

    @Override
    public List<String> listKnowledgeBases() {
        return knowledgeBaseStore.listKnowledgeBases();
    }

    private String validateUploadRequest(String knowledgeBaseId, MultipartFile file) {
        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "knowledgeBaseId 不能为空");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "文件名不能为空");
        }

        return fileName;
    }

    private String resolveFileType(String fileName) {
        FileTypeEnum fileType = FileTypeEnum.resolve(fileName);
        if (fileType == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "当前仅支持 " + FileTypeEnum.getAllowedExtensions() + " 文件类型");
        }
        return fileType.getTypeCode();
    }
}