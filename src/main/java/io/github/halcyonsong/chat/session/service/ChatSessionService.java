package io.github.halcyonsong.chat.session.service;

import io.github.halcyonsong.chat.session.pojo.vo.ChatHistoryPageVO;
import io.github.halcyonsong.chat.session.pojo.vo.SessionVO;

import java.util.List;

public interface ChatSessionService {

    SessionVO createSession();

    List<SessionVO> listSessions();

    SessionVO getSession(String sessionId);

    void touchSession(String sessionId);

    boolean deleteSession(String sessionId);

    SessionVO renameSession(String sessionId, String title);

    ChatHistoryPageVO listHistory(String sessionId, Integer beforeIndex);

    ChatHistoryPageVO rollbackLastRound(String sessionId);

}