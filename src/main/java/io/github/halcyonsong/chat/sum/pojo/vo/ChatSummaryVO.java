package io.github.halcyonsong.chat.sum.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSummaryVO {

    // 摘要文本
    private String content;
    // 本条摘要对应的历史起始索引（包含）
    private Integer startIndex;
    // 本条摘要对应的历史结束索引（包含）
    private Integer endIndex;
    // 本次压缩覆盖的消息数
    private Integer messageCount;
    // 创建时间
    private LocalDateTime createTime;

}