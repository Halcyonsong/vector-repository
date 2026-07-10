package io.github.halcyonsong.chat.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatRoleEnum {

    USER("user", "用户"),
    ASSISTANT("assistant", "助手");

    private final String value;
    private final String desc;

    public static ChatRoleEnum fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("role 不能为空");
        }

        for (ChatRoleEnum roleEnum : values()) {
            if (roleEnum.value.equalsIgnoreCase(value)) {
                return roleEnum;
            }
        }

        throw new IllegalArgumentException("不支持的聊天角色: " + value);
    }

    public static boolean supports(String value) {
        if (value == null) {
            return false;
        }

        for (ChatRoleEnum roleEnum : values()) {
            if (roleEnum.value.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }
}