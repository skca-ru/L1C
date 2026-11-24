package l1c.dialogs;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.util.Duration;
import l1c.AppConstants;
import l1c.HistoryManager;
import l1c.RunYBase;

/**
 * Инструмент «Формирование идентификатора».
 * Преобразует введённую фразу в идентификатор: каждое слово в фразе
 * начинается с прописной (заглавной) буквы, пробелы удаляются.
 * Префикс используется как есть (без изменений) и ставится в начало результата.
 * Например: "случайная пауза" -> "СлучайнаяПауза",
 * "ск_" + "случайная пауза" -> "ск_СлучайнаяПауза".
 */
public final class IdentifierDialog {

    private IdentifierDialog() {
    }

    public static void show(RunYBase owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Формирование идентификатора");
        dialog.setHeaderText("Введите фразу — из неё будет сформирован идентификатор");
        dialog.initOwner(owner.getMainStage());

        ButtonType copyButtonType = new ButtonType("Копировать", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButtonType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(copyButtonType, closeButtonType);

        TextField phraseField = new TextField();
        phraseField.setPromptText("Например: случайная пауза");
        phraseField.setStyle("-fx-background-color: " + AppConstants.COLOR_INPUT_BG
                + "; -fx-border-color: " + AppConstants.COLOR_BUTTON_BORDER
                + "; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");

        TextField resultField = new TextField();
        resultField.setEditable(false);
        resultField.setPromptText("Идентификатор появится здесь");
        resultField.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-background-color: "
                + AppConstants.COLOR_OUTPUT_BG
                + "; -fx-border-color: " + AppConstants.COLOR_BUTTON_BORDER
                + "; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");

        Label prefixLabel = new Label("Префикс:");
        Label phraseLabel = new Label("Фраза:");
        Label resultLabel = new Label("Идентификатор:");

        // Поле префикса с историей значений (сохраняется между сеансами)
        HistoryManager historyManager = owner.getHistoryManager();
        ObservableList<String> prefixHistory = historyManager.getPrefixHistoryList();
        ComboBox<String> prefixField = new ComboBox<>(prefixHistory);
        prefixField.setEditable(true);
        prefixField.setPromptText("Например: Заказ клиента");
        prefixField.setStyle("-fx-background-color: " + AppConstants.COLOR_INPUT_BG
                + "; -fx-border-color: " + AppConstants.COLOR_BUTTON_BORDER
                + "; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        // Восстанавливаем последний использованный префикс
        if (!prefixHistory.isEmpty()) {
            prefixField.getEditor().setText(prefixHistory.get(0));
        }

        // Таймер debounce: пересчёт идентификатора с задержкой 300 мс
        PauseTransition debounceTimer = new PauseTransition(Duration.millis(300));
        debounceTimer.setOnFinished(e ->
                resultField.setText(buildIdentifier(
                        prefixField.getEditor().getText(),
                        phraseField.getText())));

        // Запускаем таймер заново при каждом изменении любого из полей
        ChangeListener<String> restartDebounce = (obs, oldVal, newVal) -> {
            debounceTimer.stop();
            debounceTimer.playFromStart();
        };
        phraseField.textProperty().addListener(restartDebounce);
        prefixField.getEditor().textProperty().addListener(restartDebounce);

        Button pasteButton = new Button("📋");
        pasteButton.setPrefWidth(36);
        pasteButton.setStyle("-fx-background-color: " + AppConstants.COLOR_BUTTON_SMALL_BG
                + "; -fx-border-color: " + AppConstants.COLOR_BUTTON_BORDER
                + "; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        pasteButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            String text = clipboard.getString();
            if (text != null && !text.isBlank()) {
                phraseField.setText(text);
            }
        });

        HBox.setHgrow(phraseField, Priority.ALWAYS);
        HBox phraseBox = new HBox(6, phraseField, pasteButton);
        phraseBox.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(8, prefixLabel, prefixField, phraseLabel, phraseBox, resultLabel, resultField);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Platform.runLater(phraseField::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            // Сохраняем префикс в историю при любой завершающей кнопке, если он непустой
            String prefixValue = prefixField.getEditor().getText();
            if (prefixValue != null && !prefixValue.trim().isEmpty()) {
                historyManager.addPrefixToHistory(prefixValue);
            }
            if (result == copyButtonType) {
                String identifier = resultField.getText();
                if (identifier.isEmpty()) {
                    RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение",
                            "Сначала введите фразу для формирования идентификатора.");
                } else {
                    owner.copyToClipboard(identifier,
                            dialog.getDialogPane().getScene() != null
                                    ? dialog.getDialogPane().getScene().getWindow() : null);
                }
            }
            // Если result == closeButtonType, просто закрываем диалог
        });
    }

    /**
     * Формирует идентификатор из фразы с учётом необязательного префикса.
     * Префикс используется как есть (без изменений), к нему приклеивается
     * капитализированная фраза.
     * Например: prefix="ск_", phrase="случайная пауза" -> "ск_СлучайнаяПауза".
     *
     * @param prefix необязательный префикс (может быть {@code null} или пустым)
     * @param phrase исходная фраза (может быть {@code null} или пустой)
     * @return идентификатор; для пустой phrase возвращается только prefix
     */
    public static String buildIdentifier(String prefix, String phrase) {
        String prefixPart = (prefix != null) ? prefix.trim() : "";
        String phrasePart = capitalizeWords(phrase);
        return prefixPart + phrasePart;
    }

    /**
     * Формирует идентификатор из фразы без префикса.
     *
     * @param phrase исходная фраза (может быть {@code null} или пустой)
     * @return идентификатор; для пустой фразы возвращается пустая строка
     */
    public static String buildIdentifier(String phrase) {
        return buildIdentifier(null, phrase);
    }

    /**
     * Капитализирует каждое слово строки и удаляет пробелы между ними.
     */
    private static String capitalizeWords(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }
}
