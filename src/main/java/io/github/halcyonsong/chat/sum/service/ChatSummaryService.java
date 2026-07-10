package io.github.halcyonsong.chat.sum.service;

import io.github.halcyonsong.chat.session.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.sum.pojo.vo.ChatSummaryVO;

import java.util.List;
import java.util.Optional;

public interface ChatSummaryService {

    // 将一段历史消息压缩为摘要文本
    String summarizeMessages(List<ChatHistoryMessageVO> historyMessages);

    // 将指定会话的全部历史压缩为摘要文本
    String summarizeSessionHistory(String sessionId);

    // 仅压缩当前会话中“未压缩历史”，不足闸值则不压缩
    Optional<ChatSummaryVO> compressPendingHistory(String sessionId);

    // 查询当前会话所有摘要
    List<ChatSummaryVO> listSummaries(String sessionId);

    // 清空当前会话摘要及游标
    void clearSessionSummaries(String sessionId);

    // 当历史因回滚/删除而缩短时，精确回滚摘要并小幅回退游标
    void realignAfterHistoryShrink(String sessionId, int historySize);
}