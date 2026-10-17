package l1c;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Окно со списком всех сохранённых учётных данных.
 * Выбранная запись сохраняется как пользователь для текущего адреса.
 */
public final class SavedUsersDialog {

    private SavedUsersDialog() {
    }

    public static void show(RunYBase owner) {
        String address = owner.getCurrentAddress();
        if (address.isEmpty()) {
            RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        List<SavedUserEntry> savedUsers = collectSavedUsers(owner, address);
        if (savedUsers.isEmpty()) {
            RunYBase.showAlert(Alert.AlertType.INFORMATION, "Сохранённые пользователи",
                    "Нет ни одних сохранённых учётных данных!");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner.getMainStage());
        dialog.setTitle("Сохранённые пользователи");

        Label header = new Label("Сохранить выбранного пользователя для адреса: " + address);
        header.setWrapText(true);
        header.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-padding: 0 0 8 0;");

        ListView<SavedUserEntry> listView = new ListView<>(FXCollections.observableArrayList(savedUsers));
        listView.setCellFactory(lv -> new SavedUserListCell());

        Button okButton = owner.createDialogConfirmButton("В_ыбрать");
        okButton.setOnAction(e -> applySavedUser(owner, listView, dialog, address));

        Button cancelButton = owner.createDialogCancelButton("Отмена");
        cancelButton.setOnAction(e -> dialog.close());

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                applySavedUser(owner, listView, dialog, address);
            }
        });

        HBox buttonPanel = new HBox(10, okButton, cancelButton);
        buttonPanel.setAlignment(Pos.CENTER);
        buttonPanel.setPadding(new Insets(10, 0, 0, 0));

        BorderPane pane = new BorderPane();
        pane.setTop(header);
        pane.setCenter(listView);
        pane.setBottom(buttonPanel);
        pane.setPadding(new Insets(10));

        Scene scene = new Scene(pane, 700, 450);

        // Escape закрывает окно без выбора, Ctrl+Enter — выбирает выделенную запись
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                dialog.close();
            } else if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                e.consume();
                applySavedUser(owner, listView, dialog, address);
            }
        });

        dialog.setOnShown(e -> listView.requestFocus());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Собирает все сохранённые учётные данные в список, отсортированный
     * по адресу базы, затем по имени пользователя.
     */
    private static List<SavedUserEntry> collectSavedUsers(RunYBase owner, String currentAddress) {
        List<SavedUserEntry> savedUsers = new ArrayList<>();
        for (Map.Entry<String, UserCredentials> entry : owner.getCredentialsManager().getAllCredentials().entrySet()) {
            UserCredentials cred = entry.getValue();
            if (cred == null || cred.getUsername().isEmpty())
                continue;
            savedUsers.add(new SavedUserEntry(
                    cred.getUsername(),
                    cred.getPassword(),
                    entry.getKey(),
                    owner.getRegisteredAddressMap().get(entry.getKey()),
                    entry.getKey().equals(currentAddress)));
        }
        savedUsers.sort(Comparator.comparing((SavedUserEntry e) -> e.address, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.username, String.CASE_INSENSITIVE_ORDER));
        return savedUsers;
    }

    /**
     * Сохраняет выделенную в списке запись как учётные данные для адреса
     * и закрывает окно выбора.
     */
    private static void applySavedUser(RunYBase owner, ListView<SavedUserEntry> listView, Stage dialog, String address) {
        SavedUserEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите пользователя из списка!");
            return;
        }
        owner.getCredentialsManager().put(address, new UserCredentials(selected.username, selected.password));
        owner.updateUserButtonState();
        dialog.close();
        owner.showAutoClosingAlert("Пользователь '" + selected.username + "' сохранён для адреса:\n" + address,
                "Успешно", 3);
    }
}