package io.github.halcyonsong.knowledge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public enum FileTypeEnum {


    // 文本文件
    TXT(".txt", "txt"),

    // PDF文档
    PDF(".pdf", "pdf"),

    // PPT演示文稿 (旧版)
    PPT(".ppt", "ppt"),

    // PPTX演示文稿 (新版)
    PPTX(".pptx", "pptx");

    private final String extension;
    private final String typeCode;


    // 根据文件名解析文件类型
    public static FileTypeEnum resolve(String fileName) {
        if (fileName == null) {
            return null;
        }

        // 统一转小写并获取后缀
        String lowerName = fileName.toLowerCase();
        int dotIndex = lowerName.lastIndexOf('.');
        if (dotIndex == -1) {
            return null;
        }

        String ext = lowerName.substring(dotIndex);

        // 遍历枚举值进行匹配
        for (FileTypeEnum type : values()) {
            if (type.getExtension().equals(ext)) {
                return type;
            }
        }
        return null;
    }

    public static String getAllowedExtensions() {
        List<String> allowedExtensions = new ArrayList<>();
        for (FileTypeEnum type : values()) {
            allowedExtensions.add(type.getExtension());
        }
        return allowedExtensions.toString();
    }

}
