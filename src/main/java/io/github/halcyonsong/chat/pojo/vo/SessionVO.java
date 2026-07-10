package io.github.halcyonsong.chat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    // 会话ID
    private String sessionId;

    // 会话标题
    private String title;

    // 创建时间
    private LocalDateTime createTime;

    // 最近活跃时间
    private LocalDateTime updateTime;

}