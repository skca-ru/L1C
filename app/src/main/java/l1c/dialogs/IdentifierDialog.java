package l1c.dialogs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import l1c.AppConstants;
import l1c.RunYBase;

/**
 * Инструмент «Формирование идентификатора».
 * Преобразует введённую фразу в идентификатор: каждое слово в фразе
 * начинается с прописной (заглавной) буквы, пробелы удаляются.
 * Например: "случайная пауза" -> "СлучайнаяПауза".
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

        Label phraseLabel = new Label("Фраза:");
        Label resultLabel = new Label("Идентификатор:");

        // Идентификатор формируется автоматически при вводе фразы
        phraseField.textProperty().addListener(
                (obs, oldValue, newValue) -> resultField.setText(buildIdentifier(newValue)));

        VBox content = new VBox(8, phraseLabel, phraseField, resultLabel, resultField);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Platform.runLater(phraseField::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
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
     * Формирует идентификатор из фразы по алгоритму:
     * каждое слово в фразе начинается с прописной буквы, пробелы удаляются.
     * Например: "случайная пауза" -> "СлучайнаяПауза".
     *
     * @param phrase исходная фраза (может быть {@code null} или пустой)
     * @return идентификатор; для пустой фразы возвращается пустая строка
     */
    public static String buildIdentifier(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return "";
        }
        // Разбиваем фразу по пробельным символам и каждое слово делаем с заглавной буквы
        String[] words = phrase.trim().split("\\s+");
        StringBuilder identifier = new StringBuilder();
        for (String word : words) {
            identifier.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
        return identifier.toString();
    }
}
