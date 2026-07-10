package io.github.halcyonsong.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatHistoryStatusEnum {

    COMPLETED("completed", "已完成"),
    INTERRUPTED("interrupted", "已中断"),
    ERROR("error", "异常结束");

    private final String value;
    private final String desc;

    public static ChatHistoryStatusEnum fromValue(String value) {
        for (ChatHistoryStatusEnum statusEnum : values()) {
            if (statusEnum.value.equalsIgnoreCase(value)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("不支持的聊天历史状态: " + value);
    }

    public static boolean supports(String value) {
        if (value == null) {
            return false;
        }

        for (ChatHistoryStatusEnum statusEnum : values()) {
            if (statusEnum.value.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }


}