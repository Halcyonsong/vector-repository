package io.github.halcyonsong.chat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryMessageVO {

    // user / assistant
    private String role;

    // 消息正文
    private String content;

    // completed / interrupted / error
    private String status;

    private LocalDateTime createTime;
}