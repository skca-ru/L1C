package l1c;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Диалог редактирования текстовой заметки, привязанной к адресу базы.
 */
public final class NoteDialog {

    private NoteDialog() {
    }

    public static void show(RunYBase owner) {
        String address = owner.getCurrentAddress();
        if (address.isEmpty()) {
            RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        String currentNote = owner.getHistoryManager().getNote(address);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(RunYBaseHelpTexts.NOTE_DIALOG_TITLE);
        dialog.setHeaderText(address);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearButtonType = new ButtonType("Очистить", ButtonBar.ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, clearButtonType, cancelButtonType);

        TextArea noteArea = new TextArea(currentNote != null ? currentNote : "");
        noteArea.setPrefRowCount(6);
        noteArea.setPrefColumnCount(50);
        noteArea.setWrapText(true);
        noteArea.setPromptText("Введите текстовую заметку к этой базе...\n" +
                "Например: \"Проверить печатную форму заказа клиента\"");
        noteArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-background-color: " +
                AppConstants.COLOR_INPUT_BG + "; -fx-border-color: " + AppConstants.COLOR_BUTTON_BORDER + "; -fx-border-width: 1px;");

        Label charCountLabel = new Label("0 / " + AppConstants.MAX_NOTE_LENGTH + " символов");
        charCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        noteArea.textProperty().addListener((obs, old, newVal) -> {
            int len = newVal == null ? 0 : newVal.length();
            charCountLabel.setText(len + " / " + AppConstants.MAX_NOTE_LENGTH + " символов");
            charCountLabel.setStyle(len > AppConstants.MAX_NOTE_LENGTH ? "-fx-font-size: 10px; -fx-text-fill: red;"
                    : "-fx-font-size: 10px; -fx-text-fill: gray;");
        });

        VBox content = new VBox(8, noteArea, charCountLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Platform.runLater(noteArea::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            if (result == okButtonType) {
                // Сохраняем введённую заметку
                String note = noteArea.getText();
                owner.getHistoryManager().saveNote(address, note);
                owner.updateBaseInfoDisplay(address);

                if (note.trim().isEmpty()) {
                    RunYBase.showAlert(Alert.AlertType.INFORMATION, "Удалено",
                            "Заметка удалена для адреса:\n" + address);
                } else {
                    owner.showAutoClosingAlert("Заметка сохранена для адреса:\n" + address, "Успешно", 3);
                }
            } else if (result == clearButtonType) {
                // Очищаем заметку (сохраняем пустую строку)
                owner.getHistoryManager().saveNote(address, "");
                owner.updateBaseInfoDisplay(address);
                RunYBase.showAlert(Alert.AlertType.INFORMATION, "Удалено",
                        "Заметка удалена для адреса:\n" + address);
            }
            // Если result == cancelButtonType, ничего не делаем
        });
    }
}