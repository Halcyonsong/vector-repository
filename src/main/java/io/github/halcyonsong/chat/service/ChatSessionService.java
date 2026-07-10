package io.github.halcyonsong.chat.service;

import io.github.halcyonsong.chat.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryPageVO;
import io.github.halcyonsong.chat.pojo.vo.SessionVO;

import java.util.List;

public interface ChatSessionService {

    SessionVO createSession();

    List<SessionVO> listSessions();

    SessionVO getSession(String sessionId);

    void touchSession(String sessionId);

    boolean deleteSession(String sessionId);

    SessionVO renameSession(String sessionId, String title);

    ChatHistoryPageVO listHistory(String sessionId, Integer beforeIndex);

}