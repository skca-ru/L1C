package l1c;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

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

        TextField usernameField = new TextField(currentUsername);
        usernameField.setPromptText("Имя пользователя");
        PasswordField passwordField = new PasswordField();
        passwordField.setText(currentPassword);
        passwordField.setPromptText("Пароль");

        grid.add(new Label("Имя пользователя:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(usernameField::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            if (result == okButtonType) {
                String username = usernameField.getText().trim();
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
}