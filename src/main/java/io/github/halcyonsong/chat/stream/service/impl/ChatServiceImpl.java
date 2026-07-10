package io.github.halcyonsong.chat.stream.service.impl;

import io.github.halcyonsong.chat.stream.pojo.dto.ChatDTO;
import io.github.halcyonsong.chat.stream.pojo.vo.ChatEventVO;
import io.github.halcyonsong.chat.stream.service.ChatService;
import io.github.halcyonsong.chat.session.service.ChatSessionService;
import io.github.halcyonsong.chat.stream.service.support.advisor.ChatAdvisorSupport;
import io.github.halcyonsong.chat.session.service.support.history.ChatHistorySupport;
import io.github.halcyonsong.chat.stream.service.support.options.ChatRequestOptionsSupport;
import io.github.halcyonsong.chat.stream.service.support.stream.ChatInterruptSupport;
import io.github.halcyonsong.chat.stream.service.support.stream.ChatStreamEventSupport;
import io.github.halcyonsong.chat.stream.service.support.stream.ChatStreamLifecycleSupport;
import io.github.halcyonsong.chat.stream.service.support.stream.ChatStreamState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatSessionService chatSessionService;
    private final ChatRequestOptionsSupport chatRequestOptionsSupport;
    private final ChatAdvisorSupport chatAdvisorSupport;
    private final ChatHistorySupport chatHistorySupport;
    private final ChatInterruptSupport chatInterruptSupport;
    private final ChatStreamLifecycleSupport chatStreamLifecycleSupport;
    private final ChatStreamEventSupport chatStreamEventSupport;

    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        // 获取对话请求信息
        String question = chatDTO.getQuestion();
        String sessionId = chatDTO.getSessionId();
        // 初始化默认值
        boolean finalAllowEmptyContext = chatRequestOptionsSupport
                .normalizeAllowEmptyContext(chatDTO.getAllowEmptyContext());
        int finalTopK = chatRequestOptionsSupport
                .normalizeTopK(chatDTO.getTopK());
        double finalSimilarityThreshold = chatRequestOptionsSupport
                .normalizeSimilarityThreshold(chatDTO.getSimilarityThreshold());

        log.info(       "chat start, sessionId={},useCustomSystemPrompt={}," +
                        " useKnowledgeBase={}, allowEmptyContext={}," +
                        " knowledgeBaseId={}, topK={}," +
                        " similarityThreshold={}, questionLength={}",
                sessionId, StringUtils.hasText(chatDTO.getSystemPrompt()),
                chatDTO.getUseKnowledgeBase(), finalAllowEmptyContext,
                chatDTO.getKnowledgeBaseId(), finalTopK,
                finalSimilarityThreshold, question.length());

        // 更新会话索引
        chatSessionService.touchSession(sessionId);

        // 大模型输出状态的缓存器
        ChatStreamState state = new ChatStreamState(sessionId);
        Sinks.One<Void> stopSignal = chatInterruptSupport.register(sessionId, state);

        // 传入记忆上下文和用户问题
        ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(chatRequestOptionsSupport.resolveSystemPrompt(sessionId, chatDTO.getSystemPrompt()))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(question);
        // 是否启用知识库
        requestSpec = chatAdvisorSupport.applyKnowledgeBaseAdvisor(
                requestSpec,
                chatDTO.getUseKnowledgeBase(),
                finalAllowEmptyContext,
                chatDTO.getKnowledgeBaseId(),
                finalTopK,
                finalSimilarityThreshold
            );

        // 追加用户问题到记忆上下文
        chatHistorySupport.appendUserHistory(sessionId, question);

        return requestSpec
                .stream()
                .chatResponse()
                // 监听停止信号
                .takeUntilOther(stopSignal.asMono())
                .flatMap(chatResponse -> chatStreamEventSupport.toChatEvents(chatResponse, state))
                .onErrorResume(throwable -> chatStreamLifecycleSupport.resumeOnError(state, throwable))
                .concatWith(Flux.defer(() -> chatStreamLifecycleSupport.buildTerminalEvents(state)))
                .doFinally(signalType -> {
                    log.debug("chat stream finally, sessionId={}, signalType={}", sessionId, signalType);
                    chatInterruptSupport.cleanup(sessionId, stopSignal);
                });
    }

    @Override // 手动中断会话
    public void interrupt(String sessionId) {
        chatInterruptSupport.interrupt(sessionId);
    }


}
