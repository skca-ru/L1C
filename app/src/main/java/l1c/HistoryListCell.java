package l1c;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Клетка ListView для отображения команды истории с временем последнего использования и режимом запуска.
 */
class HistoryListCell extends ListCell<HistoryManager.CommandEntry> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy в HH:mm");

    // @formatter:off
    private final HBox container      = new HBox(10);
    private final Label baseNameLabel = new Label();
    private final Label commandLabel  = new Label();
    private final Label timeLabel     = new Label();
    private final Label modeLabel     = new Label();
    // @formatter oт

    HistoryListCell() {
        baseNameLabel.setMinWidth(180);
        baseNameLabel.setMaxWidth(180);
        baseNameLabel.setWrapText(true);

        timeLabel.setMinWidth(110);
        timeLabel.setMaxWidth(110);

        modeLabel.setMinWidth(160);
        modeLabel.setMaxWidth(160);

        commandLabel.setWrapText(true);
        commandLabel.setMaxWidth(500);
        HBox.setHgrow(commandLabel, Priority.ALWAYS);

        container.getChildren().addAll(baseNameLabel, modeLabel, commandLabel, timeLabel);
        container.setPadding(new Insets(5, 5, 5, 5));
    }

    @Override
    protected void updateItem(HistoryManager.CommandEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        baseNameLabel.setText(extractBaseName(item.command));
        commandLabel.setText(item.command);
        modeLabel.setText(item.runMode != null && !item.runMode.isEmpty() ? item.runMode : "—");
        timeLabel.setText(item.lastUsed > 0 ? formatLastUsed(item.lastUsed) : "—");

        if (isSelected()) {
            container.setStyle("-fx-background-color: -fx-selection-bar;");
            baseNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white; -fx-alignment: center-left;");
            commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-padding: 2 0 2 0; -fx-text-fill: white;");
            modeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E0E0E0; -fx-alignment: center-left;");
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E0E0E0; -fx-alignment: center-right;");
        } else {
            container.setStyle("-fx-background-color: transparent;");
            baseNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333333; -fx-alignment: center-left;");
            commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-padding: 2 0 2 0;");
            modeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555; -fx-alignment: center-left;");
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555; -fx-alignment: center-right;");
        }
        setGraphic(container);
    }

    /**
     * Извлекает имя базы из строки команды запуска 1С.
     * Для серверной базы (Srvr="...";Ref="...") возвращает Ref — имя базы на сервере.
     * Для файловой базы (File="...") возвращает путь к каталогу базы.
     * Для /IBName возвращает имя базы.
     */
    static String extractBaseName(String command) {
        if (command == null || command.isEmpty()) return "";

        // Ищем /IBConnectionString
        int idx = command.indexOf("/IBConnectionString");
        if (idx >= 0) {
            String rest = command.substring(idx + "/IBConnectionString".length()).trim();
            String connStr = extractQuotedValue(rest);
            if (connStr.isEmpty()) return "";
            return parseConnectionForBaseName(connStr);
        }

        // Ищем /IBName
        idx = command.indexOf("/IBName");
        if (idx >= 0) {
            String rest = command.substring(idx + "/IBName".length()).trim();
            return extractQuotedValue(rest);
        }

        return "";
    }

    /**
     * Извлекает значение из строки, начинающейся с кавычки (с поддержкой удвоенных кавычек).
     */
    private static String extractQuotedValue(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.charAt(0) != '"') {
            int space = s.indexOf(' ');
            return space > 0 ? s.substring(0, space) : s;
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        while (i < s.length()) {
            if (s.charAt(i) == '"') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '"') {
                    sb.append('"');
                    i += 2;
                } else {
                    break;
                }
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Парсит строку подключения и возвращает имя базы:
     * Ref="..." для серверной, File="..." для файловой.
     */
    private static String parseConnectionForBaseName(String connStr) {
        String ref = extractValueByKey(connStr, "Ref");
        if (ref != null && !ref.isEmpty()) return ref;

        String file = extractValueByKey(connStr, "File");
        if (file != null && !file.isEmpty()) return file;

        return "";
    }

    /**
     * Извлекает значение параметра key="value" из строки подключения.
     */
    private static String extractValueByKey(String connStr, String key) {
        String search = key + "=";
        int idx = connStr.indexOf(search);
        if (idx < 0) return null;
        String rest = connStr.substring(idx + search.length());
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return end > 0 ? rest.substring(1, end) : null;
        }
        int end = rest.indexOf(';');
        return end > 0 ? rest.substring(0, end).trim() : rest.trim();
    }

    private String formatLastUsed(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

        if (diff < 0) {
            return dateTime.format(DATE_TIME_FORMATTER);
        }
        if (diff < 60_000) {
            return "Только что";
        }
        if (diff < 3_600_000) {
            return diff / 60_000 + " мин. назад";
        }

        LocalDate nowDate = LocalDate.now();
        LocalDate eventDate = dateTime.toLocalDate();
        String time = String.format("%d:%02d", dateTime.getHour(), dateTime.getMinute());
        if (eventDate.equals(nowDate)) {
            return "Сегодня в " + time;
        }
        if (eventDate.equals(nowDate.minusDays(1))) {
            return "Вчера в " + time;
        }

        long daysDiff = ChronoUnit.DAYS.between(eventDate, nowDate);
        if (daysDiff >= 2 && daysDiff <= 6) {
            return daysDiff + " дн. назад, в " + time;
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
