package io.github.halcyonsong.knowledge.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseUploadTaskVO {

    private String taskId;
    private String knowledgeBaseId;
    private String fileName;

    // pending / parsing / embedding / completed / failed
    private String status;
    private String message;
    private String errorMessage;

    // 总块数
    private Integer totalChunks;
    // 已处理块数
    private Integer processedChunks;

    // 当前批次大小
    private Integer batchSize;
    // 当前批次
    private Integer currentBatch;
    // 总批次数
    private Integer totalBatches;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}