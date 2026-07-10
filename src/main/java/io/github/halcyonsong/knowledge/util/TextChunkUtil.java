package io.github.halcyonsong.knowledge.util;

import java.util.ArrayList;
import java.util.List;

public final class TextChunkUtil {

    private TextChunkUtil() {
    }

    public static List<String> splitText(String text, int chunkSize, int overlapSize) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize 必须大于 0");
        }

        if (overlapSize < 0 || overlapSize >= chunkSize) {
            throw new IllegalArgumentException("overlapSize 必须大于等于 0 且小于 chunkSize");
        }

        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == textLength) {
                break;
            }

            start = end - overlapSize;
        }

        return chunks;
    }
}