package io.github.halcyonsong.chat.stream.service.support.stream;

import io.github.halcyonsong.chat.common.enums.ChatEventTypeEnum;
import io.github.halcyonsong.chat.stream.pojo.vo.ChatEventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ChatStreamEventSupport {

    private static final String REASONING_CONTENT_METADATA_KEY = "reasoningContent";

    public Flux<ChatEventVO> toChatEvents(ChatResponse chatResponse, ChatStreamState state) {
        AssistantMessage assistantMessage = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .orElse(null);

        if (assistantMessage == null) {
            if (state.getNullResultChunkLogged().compareAndSet(false, true)) {
                log.debug("ignore null-result chat chunk, sessionId={}", state.getSessionId());
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
            state.getAssistantOutputStarted().compareAndSet(false, true);
            state.getOutputBuilder().append(text);

            events.add(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(text)
                    .build());
        }

        return Flux.fromIterable(events);
    }
}