package io.github.halcyonsong.chat.service.impl;

import io.github.halcyonsong.chat.config.ChatProperties;
import io.github.halcyonsong.chat.enums.ChatEventTypeEnum;
import io.github.halcyonsong.chat.enums.ChatHistoryStatusEnum;
import io.github.halcyonsong.chat.enums.ChatRoleEnum;
import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import io.github.halcyonsong.knowledge.config.RagAdvisorFactory;
import io.github.halcyonsong.chat.pojo.dto.ChatDTO;
import io.github.halcyonsong.chat.pojo.vo.ChatEventVO;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.service.ChatService;
import io.github.halcyonsong.chat.service.ChatSessionService;
import io.github.halcyonsong.chat.service.support.ChatHistoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatSessionService chatSessionService;
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final RagAdvisorFactory ragAdvisorFactory;
    private final ChatHistoryStore chatHistoryStore;
    private final ChatProperties chatProperties;

    private static final String REASONING_CONTENT_METADATA_KEY = "reasoningContent";

    // 记录被请求停止的会话
    private final Set<String> stoppedSessions = ConcurrentHashMap.newKeySet();

    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        // 获取对话请求信息
        String question = chatDTO.getQuestion();
        String sessionId = chatDTO.getSessionId();
        // 初始化默认值
        boolean finalAllowEmptyContext = normalizeAllowEmptyContext(chatDTO.getAllowEmptyContext());
        int finalTopK = normalizeTopK(chatDTO.getTopK());
        double finalSimilarityThreshold = normalizeSimilarityThreshold(chatDTO.getSimilarityThreshold());

        log.info("chat start, sessionId={}, useKnowledgeBase={}, allowEmptyContext={}, knowledgeBaseId={}, topK={}, similarityThreshold={}, questionLength={}",
                sessionId,
                chatDTO.getUseKnowledgeBase(),
                finalAllowEmptyContext,
                chatDTO.getKnowledgeBaseId(),
                finalTopK,
                finalSimilarityThreshold,
                question.length());

        // 更新会话索引
        chatSessionService.touchSession(sessionId);

        // 大模型输出内容的缓存器
        ChatStreamState state = new ChatStreamState(sessionId);

        // 传入记忆上下文和用户问题
        ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(chatProperties.getSystemPrompt())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(question);
        // 是否启用知识库
        try {
            requestSpec = applyKnowledgeBaseAdvisor(
                    requestSpec,
                    chatDTO.getUseKnowledgeBase(),
                    finalAllowEmptyContext,
                    chatDTO.getKnowledgeBaseId(),
                    finalTopK,
                    finalSimilarityThreshold
            );
        } catch (IllegalArgumentException exception) {
            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.ERROR.getValue())
                    .eventData(exception.getMessage())
                    .build());
        }

        appendUserHistory(sessionId, question);

        return requestSpec
                .stream()
                // 获取单次响应
                .chatResponse()
                // 在开始前清理上一次可能残留的停止标记
                .doFirst(() -> stoppedSessions.remove(sessionId))
                // 监听响应，判断是否需要停止
                .takeWhile(interruptPredicate(state))
                // 处理异常情况，确保停止标记被移除
                .doOnError(throwable -> stoppedSessions.remove(sessionId))
                // 包装数据事件
                .flatMap(chatResponse -> toChatEvents(chatResponse, state))
                // 处理异常情况
                .onErrorResume(throwable -> resumeOnError(state, throwable))
                // 处理停止事件
                .concatWith(Flux.defer(() -> buildTerminalEvents(state)))
                // 兜底处理，确保停止标记被移除
                .doFinally(signalType -> {
                    log.debug("chat stream finally, sessionId={}, signalType={}", sessionId, signalType);
                    stoppedSessions.remove(sessionId);
                });
    }

    @Override // 手动中断会话
    public void interrupt(String sessionId) {
        log.info("chat interrupt requested, sessionId={}", sessionId);
        stoppedSessions.add(sessionId);
    }


    // 包装返回数据
    private Flux<ChatEventVO> toChatEvents(ChatResponse chatResponse, ChatStreamState state) {
        AssistantMessage assistantMessage = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .orElse(null);

        if (assistantMessage == null) {
            if (state.nullResultChunkLogged.compareAndSet(false, true)) {
                log.debug("ignore null-result chat chunk, sessionId={}", state.sessionId);
            }
            return Flux.empty();
        }

        String text = assistantMessage.getText();
        Map<String, Object> properties = assistantMessage.getMetadata();
        Object reasoningObject = properties.get(REASONING_CONTENT_METADATA_KEY);
        String reasoningContent = reasoningObject instanceof String reasoning ? reasoning : null;

        List<ChatEventVO> events = new ArrayList<>(2);

        if (StringUtils.hasText(reasoningContent)) {
            events.add(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.REASONING.getValue())
                    .eventData(reasoningContent)
                    .build());
        }

        if (StringUtils.hasText(text)) {
            state.outputBuilder.append(text);
            events.add(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(text)
                    .build());
        }

        return Flux.fromIterable(events);
    }

    // 判断是否启用知识库方法
    private ChatClientRequestSpec applyKnowledgeBaseAdvisor(ChatClientRequestSpec requestSpec,
                                                            Boolean useKnowledgeBase,
                                                            Boolean allowEmptyContext,
                                                            String knowledgeBaseId,
                                                            Integer topK,
                                                            Double similarityThreshold) {
        if (!Boolean.TRUE.equals(useKnowledgeBase)) {
            log.info("knowledge base disabled");
            return requestSpec;
        }

        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "启用知识库时，knowledgeBaseId 不能为空");
        }

        log.info("knowledge base enabled, allowEmptyContext={}, knowledgeBaseId={}, topK={}, similarityThreshold={}",
                  allowEmptyContext, knowledgeBaseId, topK, similarityThreshold);
        return requestSpec.advisors(
                ragAdvisorFactory.create(allowEmptyContext, knowledgeBaseId, topK, similarityThreshold)
        );
    }

    // 初始化默认值
    private boolean normalizeAllowEmptyContext(Boolean allowEmptyContext) {
        return allowEmptyContext == null || allowEmptyContext;
    }
    private int normalizeTopK(Integer topK) {
        return topK == null ? 3 : topK;
    }
    private double normalizeSimilarityThreshold(Double similarityThreshold) {
        return similarityThreshold == null ? 0.5D : similarityThreshold;
    }

    // 追加用户对话历史记录
    private void appendUserHistory(String sessionId, String question) {
        chatHistoryStore.appendMessage(
                sessionId,
                ChatHistoryMessageVO.builder()
                        .role(ChatRoleEnum.USER.getValue())
                        .content(question)
                        .status(ChatHistoryStatusEnum.COMPLETED.getValue())
                        .createTime(LocalDateTime.now())
                        .build()
        );
    }
    // 追加模型回复历史记录
    private void appendAssistantHistory(String sessionId, String content, String status) {
        if (!StringUtils.hasText(content)) {
            return;
        }

        chatHistoryStore.appendMessage(
                sessionId,
                ChatHistoryMessageVO.builder()
                        .role(ChatRoleEnum.ASSISTANT.getValue())
                        .content(content)
                        .status(status)
                        .createTime(LocalDateTime.now())
                        .build()
        );
    }

    // 内部状态类
    private static class ChatStreamState {

        private final String sessionId;
        private final StringBuilder outputBuilder = new StringBuilder();
        private final AtomicBoolean interrupted = new AtomicBoolean(false);
        private final AtomicBoolean failed = new AtomicBoolean(false);
        private final AtomicReference<String> errorMessage = new AtomicReference<>("");
        private final AtomicBoolean nullResultChunkLogged = new AtomicBoolean(false);

        private ChatStreamState(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    // 中断判断
    private Predicate<ChatResponse> interruptPredicate(ChatStreamState state) {
        return chatResponse -> {
            boolean interrupted = stoppedSessions.contains(state.sessionId);
            if (interrupted && state.interrupted.compareAndSet(false, true)) {
                log.info("chat interrupted, sessionId={}, outputLength={}",
                        state.sessionId,
                        state.outputBuilder.length());
            }
            return !interrupted;
        };
    }
    // 服务异常处理
    private Flux<ChatEventVO> resumeOnError(ChatStreamState state, Throwable throwable) {
        log.error("chat stream failed, sessionId={}, outputLength={}",
                state.sessionId,
                state.outputBuilder.length(),
                throwable);
        state.failed.set(true);

        String memoryText;
        if (state.outputBuilder.isEmpty()) {
            memoryText = "本轮回答因服务异常中断，请稍后重试。";
        } else {
            memoryText = state.outputBuilder + "\n\n[系统提示] 本轮回答因服务异常中断，以上内容可能不完整。";
        }

        state.errorMessage.set(memoryText);

        messageWindowChatMemory.add(
                state.sessionId,
                List.of(new AssistantMessage(memoryText))
        );

        appendAssistantHistory(state.sessionId, memoryText, ChatHistoryStatusEnum.ERROR.getValue());

        return Flux.empty();
    }
    // 处理结束事件
    private Flux<ChatEventVO> buildTerminalEvents(ChatStreamState state) {
        if (state.failed.get()) {
            log.warn("chat finished with error, sessionId={}, outputLength={}",
                    state.sessionId,
                    state.outputBuilder.length());

            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.ERROR.getValue())
                    .eventData(state.errorMessage.get())
                    .build());
        }

        if (state.interrupted.get()) {
            log.info("chat finished by interrupt, sessionId={}, outputLength={}",
                    state.sessionId,
                    state.outputBuilder.length());

            if (!state.outputBuilder.isEmpty()) {
                messageWindowChatMemory.add(
                        state.sessionId,
                        List.of(new AssistantMessage(state.outputBuilder.toString()))
                );
            }

            appendAssistantHistory(state.sessionId, state.outputBuilder.toString(), ChatHistoryStatusEnum.INTERRUPTED.getValue());

            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.INTERRUPTED.getValue())
                    .eventData("")
                    .build());
        }

        log.info("chat finished normally, sessionId={}, outputLength={}",
                state.sessionId,
                state.outputBuilder.length());

        appendAssistantHistory(state.sessionId, state.outputBuilder.toString(), ChatHistoryStatusEnum.COMPLETED.getValue());

        return Flux.just(ChatEventVO.builder()
                .eventType(ChatEventTypeEnum.STOP.getValue())
                .eventData("")
                .build());
    }



}
