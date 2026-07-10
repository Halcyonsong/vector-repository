package io.github.halcyonsong.chat.service.impl;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryPageVO;
import io.github.halcyonsong.chat.pojo.vo.SessionVO;
import io.github.halcyonsong.chat.service.ChatSessionService;
import io.github.halcyonsong.chat.service.support.ChatHistoryStore;
import io.github.halcyonsong.chat.service.support.ChatSessionStore;
import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String DEFAULT_TITLE = "untitled";

    private final RedissonRedisChatMemoryRepository chatMemoryRepository;
    private final ChatSessionStore chatSessionStore;
    private final ChatHistoryStore chatHistoryStore;

    @Override // 创建会话方法
    public SessionVO createSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        SessionVO sessionVO = new SessionVO();
        sessionVO.setSessionId(sessionId);
        sessionVO.setTitle(DEFAULT_TITLE);
        sessionVO.setCreateTime(now);
        sessionVO.setUpdateTime(now);
        // 保存会话信息
        chatSessionStore.saveSession(sessionVO);
        // 刷新会话索引
        chatSessionStore.refreshSessionIndex(sessionId, now);

        return sessionVO;
    }

    @Override // 获取会话列表方法
    public List<SessionVO> listSessions() {
        // 获取有序集合操作对象，按时间倒序获取索引
        Set<String> sessionIdSet = chatSessionStore.listSessionIds(100L);

        List<SessionVO> sessionList = new ArrayList<>();
        if (sessionIdSet == null || sessionIdSet.isEmpty()) {
            return sessionList;
        }
        // 遍历会话ID，获取会话信息
        for (String sessionId : sessionIdSet) {
            SessionVO sessionVO = getSession(sessionId);
            if (sessionVO != null) {
                sessionList.add(sessionVO);
            }
        }

        return sessionList;
    }

    @Override // 获取会话信息方法
    public SessionVO getSession(String sessionId) {
        return chatSessionStore.getSession(sessionId);
    }

    @Override // 更新会话方法
    public void touchSession(String sessionId) {
        SessionVO sessionVO = getSession(sessionId);
        if (sessionVO == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        sessionVO.setUpdateTime(now);
        // 重新写入，覆盖更新
        chatSessionStore.saveSession(sessionVO);
        chatSessionStore.refreshSessionIndex(sessionId, now);
    }

    @Override // 删除会话方法
    public boolean deleteSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }

        boolean deleted = chatSessionStore.deleteSession(sessionId);
        chatMemoryRepository.deleteByConversationId(sessionId);
        chatHistoryStore.deleteHistory(sessionId);
        return deleted;
    }

    @Override // 重命名会话方法
    public SessionVO renameSession(String sessionId, String title) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "sessionId 不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "title 不能为空");
        }

        SessionVO sessionVO = getSession(sessionId);
        if (sessionVO == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "会话不存在: " + sessionId);
        }

        String trimmedTitle = title.trim();
        if (trimmedTitle.length() > 100) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "title 长度不能超过 100");
        }

        LocalDateTime now = LocalDateTime.now();
        sessionVO.setTitle(trimmedTitle);
        sessionVO.setUpdateTime(now);

        chatSessionStore.saveSession(sessionVO);
        chatSessionStore.refreshSessionIndex(sessionId, now);

        return sessionVO;
    }

    @Override
    public ChatHistoryPageVO listHistory(String sessionId, Integer beforeIndex) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "sessionId 不能为空");
        }

        if (beforeIndex != null && beforeIndex < 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "beforeIndex 不能小于 0");
        }

        SessionVO sessionVO = getSession(sessionId);
        if (sessionVO == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "会话不存在: " + sessionId);
        }

        return chatHistoryStore.listHistory(sessionId, beforeIndex);
    }




}