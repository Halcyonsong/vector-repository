package io.github.halcyonsong.chat.sum.service.support.summary;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSummaryCompressLockSupport {

    private final Set<String> compressingSessionIds = ConcurrentHashMap.newKeySet();

    public boolean tryLock(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }
        return compressingSessionIds.add(sessionId);
    }

    public void unlock(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        compressingSessionIds.remove(sessionId);
    }
}