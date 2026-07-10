package io.github.halcyonsong.chat.session.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryPageVO {

    // 本次返回的历史消息，按时间正序
    private List<ChatHistoryMessageVO> records;

    // 下一次向上查询时传入的游标；为 null 表示没有更多
    private Integer nextCursor;

    // 是否还有更早的消息
    private boolean hasMore;

    // 当前会话总消息数
    private long total;
}