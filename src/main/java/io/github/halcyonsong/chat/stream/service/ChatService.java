package io.github.halcyonsong.chat.stream.service;

import io.github.halcyonsong.chat.stream.pojo.dto.ChatDTO;
import io.github.halcyonsong.chat.stream.pojo.vo.ChatEventVO;
import reactor.core.publisher.Flux;

public interface ChatService {

    // 流式输出对话
    Flux<ChatEventVO> chat(ChatDTO chatDTO);

    // 停止生成
    void interrupt(String sessionId);

}
