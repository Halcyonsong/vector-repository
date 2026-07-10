package io.github.halcyonsong.chat.controller;

import io.github.halcyonsong.chat.pojo.vo.ChatHistoryPageVO;
import io.github.halcyonsong.common.result.Result;
import io.github.halcyonsong.chat.pojo.dto.RenameSessionDTO;
import io.github.halcyonsong.chat.pojo.vo.ChatHistoryMessageVO;
import io.github.halcyonsong.chat.pojo.vo.SessionVO;
import io.github.halcyonsong.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping("/create")
    public Result<SessionVO> createSession() {
        return Result.success(chatSessionService.createSession());
    }

    @GetMapping("/list")
    public Result<List<SessionVO>> listSessions() {
        return Result.success(chatSessionService.listSessions());
    }

    @GetMapping("/get")
    public Result<SessionVO> getSession(@RequestParam("sessionId") String sessionId) {
        return Result.success(chatSessionService.getSession(sessionId));
    }

    @DeleteMapping("/delete")
    public Result<Boolean> deleteSession(@RequestParam("sessionId") String sessionId) {
        return Result.success(chatSessionService.deleteSession(sessionId));
    }

    @PostMapping("/rename")
    public Result<SessionVO> renameSession(@Valid @RequestBody RenameSessionDTO renameSessionDTO) {
        return Result.success(
                chatSessionService.renameSession(renameSessionDTO.getSessionId(), renameSessionDTO.getTitle())
        );
    }

    @GetMapping("/history")
    public Result<ChatHistoryPageVO> listHistory(@RequestParam("sessionId") String sessionId,
                                                 @RequestParam(value = "beforeIndex", required = false) Integer beforeIndex) {
        return Result.success(chatSessionService.listHistory(sessionId, beforeIndex));
    }



}