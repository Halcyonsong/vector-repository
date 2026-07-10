package io.github.halcyonsong.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatEventTypeEnum {
    DATA(1001, "数据事件"),
    REASONING(1002, "思考事件"),
    STOP(1003, "停止事件"),
    INTERRUPTED(1004, "中断事件"),
    ERROR(1005, "错误事件");

    private final int value;
    private final String desc;

    public static ChatEventTypeEnum fromValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("eventType 不能为空");
        }

        for (ChatEventTypeEnum eventTypeEnum : values()) {
            if (eventTypeEnum.value == value) {
                return eventTypeEnum;
            }
        }
        throw new IllegalArgumentException("不支持的聊天事件类型: " + value);
    }

    public static boolean supports(Integer value) {
        if (value == null) {
            return false;
        }

        for (ChatEventTypeEnum eventTypeEnum : values()) {
            if (eventTypeEnum.value == value) {
                return true;
            }
        }
        return false;
    }

}