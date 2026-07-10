package io.github.halcyonsong.chat.stream.service.support.stream;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class ChatStreamState {

    private final String sessionId;
    private final StringBuilder outputBuilder = new StringBuilder();
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private final AtomicBoolean failed = new AtomicBoolean(false);
    private final AtomicReference<String> errorMessage = new AtomicReference<>("");
    private final AtomicBoolean nullResultChunkLogged = new AtomicBoolean(false);
    private final AtomicBoolean assistantOutputStarted = new AtomicBoolean(false);

    public ChatStreamState(String sessionId) {
        this.sessionId = sessionId;
    }
}