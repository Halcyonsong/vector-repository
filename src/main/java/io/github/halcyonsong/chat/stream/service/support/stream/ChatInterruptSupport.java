package io.github.halcyonsong.chat.stream.service.support.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatInterruptSupport {

    private final Map<String, Sinks.One<Void>> stopSignalMap = new ConcurrentHashMap<>();
    private final Map<String, ChatStreamState> activeStateMap = new ConcurrentHashMap<>();

    public Sinks.One<Void> register(String sessionId, ChatStreamState state) {
        Sinks.One<Void> stopSignal = Sinks.one();
        stopSignalMap.put(sessionId, stopSignal);
        activeStateMap.put(sessionId, state);
        return stopSignal;
    }

    public void interrupt(String sessionId) {
        log.info("chat interrupt requested, sessionId={}", sessionId);

        ChatStreamState state = activeStateMap.get(sessionId);
        if (state != null && state.getInterrupted().compareAndSet(false, true)) {
            log.info("chat interrupt signal emitted, sessionId={}, outputLength={}",
                    sessionId,
                    state.getOutputBuilder().length());
        }

        Sinks.One<Void> stopSignal = stopSignalMap.get(sessionId);
        if (stopSignal != null) {
            stopSignal.tryEmitEmpty();
        }
    }

    public void cleanup(String sessionId, Sinks.One<Void> stopSignal) {
        stopSignalMap.remove(sessionId, stopSignal);
        activeStateMap.remove(sessionId);
    }
}