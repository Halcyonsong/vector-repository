package io.github.halcyonsong.chat.sum.service.support.merge;

import io.github.halcyonsong.chat.sum.pojo.vo.ChatSummaryVO;
import io.github.halcyonsong.chat.sum.service.support.summary.ChatSummaryPromptSupport;
import io.github.halcyonsong.chat.sum.service.support.summary.ChatSummaryStore;
import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSummaryMergeSupport {

    private static final int MAX_SUMMARY_COUNT = 5;
    private static final int MERGE_SOURCE_SUMMARY_COUNT = 3;

    private final ChatClient chatClient;
    private final ChatSummaryStore chatSummaryStore;
    private final ChatSummaryPromptSupport chatSummaryPromptSupport;
    private final ChatSummaryMergePromptSupport chatSummaryMergePromptSupport;

    public void mergeOldSummariesIfNecessary(String sessionId) {
        List<ChatSummaryVO> summaryList = chatSummaryStore.listSummaries(sessionId);
        if (summaryList.size() < MAX_SUMMARY_COUNT) {
            return;
        }

        while (summaryList.size() >= MAX_SUMMARY_COUNT) {
            List<ChatSummaryVO> oldestSummaries = new ArrayList<>(
                    summaryList.subList(0, MERGE_SOURCE_SUMMARY_COUNT)
            );

            ChatSummaryVO mergedSummary = mergeSummaries(oldestSummaries);

            List<ChatSummaryVO> mergedSummaryList = new ArrayList<>();
            mergedSummaryList.add(mergedSummary);
            mergedSummaryList.addAll(summaryList.subList(MERGE_SOURCE_SUMMARY_COUNT, summaryList.size()));

            chatSummaryStore.replaceSummaries(sessionId, mergedSummaryList);

            log.info("old summaries merged, sessionId={}, mergedCount={}, summaryCountAfter={}",
                    sessionId, oldestSummaries.size(), mergedSummaryList.size());

            summaryList = mergedSummaryList;
        }
    }

    private ChatSummaryVO mergeSummaries(List<ChatSummaryVO> summaryList) {
        if (CollectionUtils.isEmpty(summaryList) || summaryList.size() < MERGE_SOURCE_SUMMARY_COUNT) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "待合并摘要数量不足");
        }

        String mergedContent = summarizeSummaryList(summaryList);
        ChatSummaryVO firstSummary = summaryList.get(0);
        ChatSummaryVO lastSummary = summaryList.get(summaryList.size() - 1);

        int totalMessageCount = 0;
        for (ChatSummaryVO summary : summaryList) {
            if (summary != null && summary.getMessageCount() != null) {
                totalMessageCount += summary.getMessageCount();
            }
        }

        return ChatSummaryVO.builder()
                .content(mergedContent)
                .startIndex(firstSummary.getStartIndex())
                .endIndex(lastSummary.getEndIndex())
                .messageCount(totalMessageCount)
                .createTime(LocalDateTime.now())
                .build();
    }

    private String summarizeSummaryList(List<ChatSummaryVO> summaryList) {
        String systemPrompt = chatSummaryPromptSupport.buildSummarySystemPrompt();
        String userPrompt = chatSummaryMergePromptSupport.buildMergeUserPrompt(summaryList);

        log.info("summary merge start, summaryCount={}, promptLength={}",
                summaryList.size(), userPrompt.length());

        String mergedSummary = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        if (!StringUtils.hasText(mergedSummary)) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "摘要再压缩失败，模型未返回有效内容");
        }

        String trimmedSummary = mergedSummary.trim();
        log.info("summary merge finished, summaryLength={}", trimmedSummary.length());
        return trimmedSummary;
    }
}