package l1c.dialogs;

import java.util.Locale;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import l1c.AppConstants;
import l1c.RunYBase;
import l1c.RunYBaseHelpTexts;
import l1c.UserCredentials;


/**
 * Диалог ввода и сохранения учётных данных пользователя для текущей базы.
 */
public final class UserCredentialsDialog {

    private UserCredentialsDialog() {
    }

    public static void show(RunYBase owner) {
        String address = owner.getCurrentAddress();
        if (address.isEmpty()) {
            RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        UserCredentials existing = owner.getCredentialsManager().get(address);
        String currentUsername = existing != null ? existing.getUsername() : "";
        String currentPassword = existing != null ? existing.getPassword() : "";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Учётные данные для базы");
        dialog.setHeaderText(address);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ComboBox<String> usernameBox = createUserNameField(currentUsername);
        PasswordField passwordField = new PasswordField();
        passwordField.setText(currentPassword);
        passwordField.setPromptText("Пароль");

        grid.add(new Label("Имя пользователя:"), 0, 0);
        grid.add(usernameBox, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(usernameBox.getEditor()::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            if (result == okButtonType) {
                String username = getUserName(usernameBox);
                String password = passwordField.getText();

                if (!username.isEmpty()) {
                    owner.getCredentialsManager().put(address, new UserCredentials(username, password));
                    owner.showAutoClosingAlert("Учётные данные сохранены для адреса:\n" + address,
                            "Пользователь: " + username, 3);
                } else if (existing != null) {
                    owner.getCredentialsManager().remove(address);
                    RunYBase.showAlert(Alert.AlertType.INFORMATION, "Удалено",
                            "Учётные данные удалены для адреса:\n" + address);
                }
                owner.updateUserButtonState();
            }
        });
    }

    /**
     * Создаёт поле имени пользователя со списком часто используемых имён.
     * Значение можно выбрать из раскрытого списка или ввести с клавиатуры.
     */
    private static ComboBox<String> createUserNameField(String value) {
        FilteredList<String> items = new FilteredList<>(
                FXCollections.observableArrayList(AppConstants.COMMON_USER_NAMES));
        ComboBox<String> box = new ComboBox<>(items);
        box.setEditable(true);
        // Пустое значение отдаём через promptText, поэтому null, а не ""
        box.setValue(value == null || value.isEmpty() ? null : value);
        box.setPromptText("Имя пользователя");
        box.setMaxWidth(Double.MAX_VALUE);

        // Фильтруем подсказки по мере ввода (без учёта регистра)
        box.getEditor().textProperty().addListener((obs, old, text) -> {
            String filter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            items.setPredicate(filter.isEmpty() ? s -> true : s -> s.toLowerCase(Locale.ROOT).contains(filter));
        });
        // При открытии списка показываем все имена, а не только отфильтрованные
        box.showingProperty().addListener((obs, was, isShown) -> {
            if (isShown) {
                items.setPredicate(s -> true);
            }
        });

        Tooltip tooltip = new Tooltip(String.format(RunYBaseHelpTexts.USER_NAME_COMMON_TOOLTIP,
                String.join(", ", AppConstants.COMMON_USER_NAMES)));
        tooltip.setStyle(AppConstants.TOOLTIP_STYLE);
        box.setTooltip(tooltip);
        return box;
    }

    /**
     * Возвращает введённое или выбранное имя пользователя без пробелов по краям.
     * У редактируемого ComboBox надёжнее читать поле ввода: значение свойства
     * value обновляется только после потери фокуса.
     */
    private static String getUserName(ComboBox<String> box) {
        String typed = box.getEditor() != null ? box.getEditor().getText() : null;
        if (typed == null || typed.isBlank()) {
            typed = box.getValue();
        }
        return typed == null ? "" : typed.trim();
    }
}
