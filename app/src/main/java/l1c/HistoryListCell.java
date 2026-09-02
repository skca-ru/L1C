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

    private final HBox container = new HBox(10);
    private final Label commandLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label modeLabel = new Label();

    HistoryListCell() {
        timeLabel.setMinWidth(110);
        timeLabel.setMaxWidth(110);

        modeLabel.setMinWidth(160);
        modeLabel.setMaxWidth(160);

        commandLabel.setWrapText(true);
        commandLabel.setMaxWidth(500);
        HBox.setHgrow(commandLabel, Priority.ALWAYS);

        container.getChildren().addAll(commandLabel, modeLabel, timeLabel);
        container.setPadding(new Insets(5, 5, 5, 5));
    }

    @Override
    protected void updateItem(HistoryManager.CommandEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        commandLabel.setText(item.command);
        modeLabel.setText(item.runMode != null && !item.runMode.isEmpty() ? item.runMode : "—");
        timeLabel.setText(item.lastUsed > 0 ? formatLastUsed(item.lastUsed) : "—");

        if (isSelected()) {
            container.setStyle("-fx-background-color: -fx-selection-bar;");
            commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-padding: 2 0 2 0; -fx-text-fill: white;");
            modeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E0E0E0; -fx-alignment: center-left;");
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E0E0E0; -fx-alignment: center-right;");
        } else {
            container.setStyle("-fx-background-color: transparent;");
            commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-padding: 2 0 2 0;");
            modeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555; -fx-alignment: center-left;");
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555; -fx-alignment: center-right;");
        }
        setGraphic(container);
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
