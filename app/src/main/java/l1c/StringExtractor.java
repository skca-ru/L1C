package l1c;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для извлечения строк подключения к базам 1С из текста.
 * Поддерживает форматы: File="..." и Srvr="...";Ref="..."
 */
public class StringExtractor {

    private static final Pattern FILE_PATTERN = Pattern.compile("File=\"[^\"]+\"");
    private static final Pattern SERVER_PATTERN = Pattern.compile("Srvr=\"[^\"]+\"\\s*;\\s*Ref=\"[^\"]+\"");

    /**
     * Извлекает строку подключения к 1С из произвольного текста.
     * 
     * @param text исходный текст (может содержать лишние слова)
     * @return строку подключения или null, если не найдено
     */
    public static String extractConnectionString(String text) {
        if (text == null || text.isEmpty())
            return null;

        // Шаблон для файловой базы
        Matcher fileMatcher = FILE_PATTERN.matcher(text);
        if (fileMatcher.find()) {
            String filePart = fileMatcher.group();
            // Если после найденного есть ";", захватываем до него (но не включая)
            int endIdx = fileMatcher.end();
            if (endIdx < text.length() && text.charAt(endIdx) == ';') {
                return filePart + ";";
            }
            return filePart;
        }

        // Шаблон для серверной базы
        Matcher serverMatcher = SERVER_PATTERN.matcher(text);
        if (serverMatcher.find()) {
            return serverMatcher.group();
        }

        return null;
    }
}
