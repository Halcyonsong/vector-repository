package io.github.halcyonsong.chat.controller;


import io.github.halcyonsong.chat.pojo.dto.ChatDTO;
import io.github.halcyonsong.chat.pojo.vo.ChatEventVO;
import io.github.halcyonsong.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

@RequiredArgsConstructor
@RequestMapping("/ai/chat")
@RestController
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@Valid @RequestBody ChatDTO chatDTO) {
        return chatService.chat(chatDTO);
    }

    @PostMapping(value = "/stop")
    public void interrupt(@RequestParam("sessionId") String sessionId) {
        this.chatService.interrupt(sessionId);
    }

}
