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
/**
 * Извлекает путь к каталогу из текста, если текст представляет собой путь к каталогу (локальному или UNC).
 * <p>
 * Распознаёт:
 * - Локальные пути: C:\Folder\Subfolder, D:/Folder/Subfolder
 * - UNC пути: \\Server\Share\Folder
 * <p>
 * Не распознаёт строки подключения, уже содержащие File= или Srvr=.
 *
 * @param text текст для анализа
 * @return путь к каталогу, или null, если текст не является путём
 */
public static String extractDirectoryPath(String text) {
    if (text == null || text.isEmpty()) return null;
    String trimmed = text.trim();

    // Если строка уже содержит File= или Srvr=, то это строка подключения, а не каталог
    if (trimmed.startsWith("File=") || trimmed.startsWith("Srvr=")) {
        return null;
    }

    // Проверка на UNC путь (начинается с \\)
    boolean isUnc = trimmed.startsWith("\\\\");
    // Проверка на наличие слэшей (Windows или Unix)
    boolean hasSlash = trimmed.contains("\\") || trimmed.contains("/");
    // Проверка на наличие двоеточия (для локальных путей)
    boolean hasDriveLetter = trimmed.matches("^[A-Za-z]:.*");

    if ((hasDriveLetter && hasSlash) || isUnc) {
        // Это похоже на путь. Нормализуем слэши в \ (для единообразия)
        String normalizedPath = trimmed.replace('/', '\\');
        // Убираем возможные кавычки в начале и конце
        if (normalizedPath.startsWith("\"") && normalizedPath.endsWith("\"")) {
            normalizedPath = normalizedPath.substring(1, normalizedPath.length() - 1);
        }
        return normalizedPath;
    }
    return null;
}
}
