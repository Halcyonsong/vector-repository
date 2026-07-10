package io.github.halcyonsong.knowledge.service.support.upload;

import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import io.github.halcyonsong.knowledge.common.constants.KnowledgeMetadataConstants;
import io.github.halcyonsong.knowledge.service.support.parser.KnowledgeBaseDocumentParser;
import io.github.halcyonsong.knowledge.service.support.store.KnowledgeBaseStore;
import io.github.halcyonsong.knowledge.service.support.store.KnowledgeBaseUploadLockStore;
import io.github.halcyonsong.knowledge.service.support.store.KnowledgeBaseUploadTaskStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseUploadAsyncService {

    private static final int BATCH_SIZE = 2;

    private final VectorStore vectorStore;
    private final KnowledgeBaseStore knowledgeBaseStore;
    private final KnowledgeBaseDocumentParser knowledgeBaseDocumentParser;
    private final KnowledgeBaseUploadTaskStore knowledgeBaseUploadTaskStore;
    private final KnowledgeBaseUploadLockStore knowledgeBaseUploadLockStore;

    @Async("knowledgeUploadExecutor")
    public void processUpload(String taskId,
                              String knowledgeBaseId,
                              String tempFilePath,
                              String fileName,
                              String fileType) {
        // 读取暂存文件
        Path path = Path.of(tempFilePath);

        try {
            knowledgeBaseUploadTaskStore.updateTask(taskId, task -> {
                task.setStatus("parsing");
                task.setMessage("正在解析文档");
            });

            byte[] fileBytes = Files.readAllBytes(path);
            List<Document> documentList = knowledgeBaseDocumentParser.parse(
                    taskId,
                    knowledgeBaseId,
                    fileBytes,
                    fileName,
                    fileType
            );

            if (documentList.isEmpty()) {
                throw new BusinessException(
                        ResultCodeEnum.DOCUMENT_PARSE_ERROR.getCode(),
                        "文档中未解析到可向量化的文本内容"
                );
            }

            int totalChunks = documentList.size();
            int totalBatches = (totalChunks + BATCH_SIZE - 1) / BATCH_SIZE;

            knowledgeBaseUploadTaskStore.updateTask(taskId, task -> {
                task.setStatus("embedding");
                task.setMessage("开始分批向量化并上传");
                task.setTotalChunks(totalChunks);
                task.setProcessedChunks(0);
                task.setBatchSize(BATCH_SIZE);
                task.setCurrentBatch(0);
                task.setTotalBatches(totalBatches);
            });

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int fromIndex = batchIndex * BATCH_SIZE;
                int toIndex = Math.min(fromIndex + BATCH_SIZE, totalChunks);

                List<Document> batch = new ArrayList<>(documentList.subList(fromIndex, toIndex));
                vectorStore.add(batch);

                int currentBatch = batchIndex + 1;
                int processedChunks = toIndex;

                knowledgeBaseUploadTaskStore.updateTask(taskId, task -> {
                    task.setStatus("embedding");
                    task.setMessage("正在上传第 " + currentBatch + "/" + totalBatches + " 批");
                    task.setCurrentBatch(currentBatch);
                    task.setProcessedChunks(processedChunks);
                });
            }

            knowledgeBaseStore.addKnowledgeBase(knowledgeBaseId);

            knowledgeBaseUploadTaskStore.updateTask(taskId, task -> {
                task.setStatus("completed");
                task.setMessage("知识库上传完成");
                task.setProcessedChunks(totalChunks);
                task.setFinishTime(LocalDateTime.now());
            });
        } catch (Exception exception) {
            log.error("knowledge base upload failed, taskId={}, knowledgeBaseId={}",
                    taskId, knowledgeBaseId, exception);
            // 清理部分转换成功向量
            cleanupPartialVectors(taskId);

            String errorMessage = exception.getMessage() == null ? "知识库上传失败" : exception.getMessage();

            knowledgeBaseUploadTaskStore.updateTask(taskId, task -> {
                task.setStatus("failed");
                task.setMessage("知识库上传失败");
                task.setErrorMessage(errorMessage);
                task.setFinishTime(LocalDateTime.now());
            });
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (Exception exception) {
                log.warn("delete temp upload file failed, path={}", tempFilePath, exception);
            }

            boolean released = knowledgeBaseUploadLockStore.release(knowledgeBaseId, taskId);
            log.info("release knowledge base upload lock, knowledgeBaseId={}, taskId={}, released={}",
                    knowledgeBaseId, taskId, released);
        }
    }

    private void cleanupPartialVectors(String taskId) {
        try {
            FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
            vectorStore.delete(
                    filterExpressionBuilder.eq(KnowledgeMetadataConstants.TASK_ID, taskId).build()
            );
            log.info("cleanup partial vectors success, taskId={}", taskId);
        } catch (Exception exception) {
            log.error("cleanup partial vectors failed, taskId={}", taskId, exception);
        }
    }

}