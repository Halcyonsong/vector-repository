package io.github.halcyonsong.knowledge.service.impl;

import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import io.github.halcyonsong.knowledge.common.constants.KnowledgeMetadataConstants;
import io.github.halcyonsong.knowledge.common.enums.FileTypeEnum;
import io.github.halcyonsong.knowledge.pojo.vo.KnowledgeBaseUploadTaskVO;
import io.github.halcyonsong.knowledge.service.KnowledgeBaseEmbeddingService;
import io.github.halcyonsong.knowledge.service.support.store.KnowledgeBaseStore;
import io.github.halcyonsong.knowledge.service.support.store.KnowledgeBaseUploadLockStore;
import io.github.halcyonsong.knowledge.service.support.upload.KnowledgeBaseUploadAsyncService;
import io.github.halcyonsong.knowledge.service.support.store.KnowledgeBaseUploadTaskStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseEmbeddingServiceImpl implements KnowledgeBaseEmbeddingService {

    private final VectorStore vectorStore;
    private final KnowledgeBaseStore knowledgeBaseStore;
    private final KnowledgeBaseUploadTaskStore knowledgeBaseUploadTaskStore;
    private final KnowledgeBaseUploadAsyncService knowledgeBaseUploadAsyncService;
    private final KnowledgeBaseUploadLockStore knowledgeBaseUploadLockStore;

    @Override
    public KnowledgeBaseUploadTaskVO uploadDocument(String knowledgeBaseId, MultipartFile file) throws Exception {
        String fileName = validateUploadRequest(knowledgeBaseId, file);
        String fileType = resolveFileType(fileName);

        if (knowledgeBaseStore.exists(knowledgeBaseId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "knowledgeBaseId 已存在，请更换后重试");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");

        if (!knowledgeBaseUploadLockStore.tryAcquire(knowledgeBaseId, taskId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该 knowledgeBaseId 正在上传处理中，请稍后重试");
        }

        Path tempFile = null;
        try {
            tempFile = saveTempFile(taskId, fileName, file);

            KnowledgeBaseUploadTaskVO task = KnowledgeBaseUploadTaskVO.builder()
                    .taskId(taskId)
                    .knowledgeBaseId(knowledgeBaseId)
                    .fileName(fileName)
                    .status("pending")
                    .message("上传任务已创建")
                    .errorMessage(null)
                    .totalChunks(0)
                    .processedChunks(0)
                    .batchSize(50)
                    .currentBatch(0)
                    .totalBatches(0)
                    .startTime(LocalDateTime.now())
                    .finishTime(null)
                    .build();

            knowledgeBaseUploadTaskStore.saveTask(task);

            knowledgeBaseUploadAsyncService.processUpload(
                    taskId,
                    knowledgeBaseId,
                    tempFile.toString(),
                    fileName,
                    fileType
            );

            return task;
        } catch (Exception exception) {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
            knowledgeBaseUploadLockStore.release(knowledgeBaseId, taskId);
            throw exception;
        }
    }

    @Override
    public KnowledgeBaseUploadTaskVO getUploadTask(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "taskId 不能为空");
        }

        KnowledgeBaseUploadTaskVO task = knowledgeBaseUploadTaskStore.getTask(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "上传任务不存在: " + taskId);
        }

        return task;
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
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "当前仅支持 " + FileTypeEnum.getAllowedExtensions() + " 文件类型"
            );
        }
        return fileType.getTypeCode();
    }

    private Path saveTempFile(String taskId, String fileName, MultipartFile file) throws Exception {
        Path uploadDir = Path.of(".runtime", "kb-upload").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String safeFileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path tempFile = uploadDir.resolve(taskId + "-" + safeFileName);

        file.transferTo(tempFile);
        return tempFile;
    }


}