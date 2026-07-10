package io.github.halcyonsong.chat.stream.service.support.stream;

import io.github.halcyonsong.chat.common.enums.ChatEventTypeEnum;
import io.github.halcyonsong.chat.common.enums.ChatHistoryStatusEnum;
import io.github.halcyonsong.chat.stream.pojo.vo.ChatEventVO;
import io.github.halcyonsong.chat.session.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.session.service.support.history.ChatHistorySupport;
import io.github.halcyonsong.chat.session.service.support.session.ChatSessionRollbackSupport;
import io.github.halcyonsong.chat.sum.service.ChatSummaryService;
import io.github.halcyonsong.chat.sum.service.support.summary.ChatSummaryCompressLockSupport;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamLifecycleSupport {

    private final MessageWindowChatMemory messageWindowChatMemory;
    private final ChatHistorySupport chatHistorySupport;
    private final ChatSessionRollbackSupport chatSessionRollbackSupport;
    private final ChatSummaryService chatSummaryService;
    private final ChatSummaryCompressLockSupport chatSummaryCompressLockSupport;

    @Resource(name = "chatSummaryExecutor")
    private Executor chatSummaryExecutor;

    public Flux<ChatEventVO> resumeOnError(ChatStreamState state, Throwable throwable) {
        if (state.getInterrupted().get()) {
            log.info("chat stream cancelled after interrupt, sessionId={}, outputLength={}, message={}",
                    state.getSessionId(),
                    state.getOutputBuilder().length(),
                    throwable.getMessage());
            return Flux.empty();
        }

        log.error("chat stream failed, sessionId={}, outputLength={}",
                state.getSessionId(),
                state.getOutputBuilder().length(),
                throwable);

        state.getFailed().set(true);

        String memoryText;
        if (state.getOutputBuilder().isEmpty()) {
            memoryText = "本轮回答因服务异常中断，请稍后重试。";
        } else {
            memoryText = state.getOutputBuilder() + "\n\n[系统提示] 本轮回答因服务异常中断，以上内容可能不完整。";
        }

        state.getErrorMessage().set(memoryText);

        messageWindowChatMemory.add(
                state.getSessionId(),
                List.of(new AssistantMessage(memoryText))
        );

        chatHistorySupport.appendAssistantHistory(
                state.getSessionId(),
                memoryText,
                ChatHistoryStatusEnum.ERROR.getValue()
        );

        return Flux.empty();
    }

    public Flux<ChatEventVO> buildTerminalEvents(ChatStreamState state) {
        if (state.getFailed().get()) {
            log.warn("chat finished with error, sessionId={}, outputLength={}",
                    state.getSessionId(),
                    state.getOutputBuilder().length());

            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.ERROR.getValue())
                    .eventData(state.getErrorMessage().get())
                    .build());
        }

        if (state.getInterrupted().get()) {
            log.info("chat finished by interrupt, sessionId={}, outputLength={}, assistantOutputStarted={}",
                    state.getSessionId(),
                    state.getOutputBuilder().length(),
                    state.getAssistantOutputStarted().get());

            if (!state.getAssistantOutputStarted().get()) {
                rollbackInterruptedRound(state);

                return Flux.just(ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.INTERRUPTED.getValue())
                        .eventData("")
                        .build());
            }

            String interruptedContent = state.getOutputBuilder().toString();
            if (StringUtils.hasText(interruptedContent)) {
                messageWindowChatMemory.add(
                        state.getSessionId(),
                        List.of(new AssistantMessage(interruptedContent))
                );

                chatHistorySupport.appendAssistantHistory(
                        state.getSessionId(),
                        interruptedContent,
                        ChatHistoryStatusEnum.INTERRUPTED.getValue()
                );
            }

            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.INTERRUPTED.getValue())
                    .eventData("")
                    .build());
        }

        log.info("chat finished normally, sessionId={}, outputLength={}",
                state.getSessionId(),
                state.getOutputBuilder().length());

        chatHistorySupport.appendAssistantHistory(
                state.getSessionId(),
                state.getOutputBuilder().toString(),
                ChatHistoryStatusEnum.COMPLETED.getValue()
        );
        // 触发异步压缩
        triggerSummaryCompressionAsync(state.getSessionId());

        return Flux.just(ChatEventVO.builder()
                .eventType(ChatEventTypeEnum.STOP.getValue())
                .eventData("")
                .build());
    }

    private void rollbackInterruptedRound(ChatStreamState state) {
        try {
            List<ChatHistoryMessageVO> remainingHistory =
                    chatSessionRollbackSupport.rollbackLastRound(state.getSessionId());

            log.info("rollback last round after pre-first-token interrupt, sessionId={}, remainingHistorySize={}",
                    state.getSessionId(),
                    remainingHistory.size());
        } catch (Exception exception) {
            log.error("rollback last round failed after pre-first-token interrupt, sessionId={}",
                    state.getSessionId(),
                    exception);
        }
    }

    // 异步压缩会话历史消息
    private void triggerSummaryCompressionAsync(String sessionId) {
        if (!chatSummaryCompressLockSupport.tryLock(sessionId)) {
            log.debug("skip summary compression because another task is running, sessionId={}", sessionId);
            return;
        }

        chatSummaryExecutor.execute(() -> {
            try {
                chatSummaryService.compressPendingHistory(sessionId);
            } catch (Exception exception) {
                log.error("async summary compression failed, sessionId={}", sessionId, exception);
            } finally {
                chatSummaryCompressLockSupport.unlock(sessionId);
            }
        });
    }


}