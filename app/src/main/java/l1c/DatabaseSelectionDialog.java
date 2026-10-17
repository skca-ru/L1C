package l1c;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Окно выбора информационной базы из списка зарегистрированных баз
 * (ibases.v8i), с показом заметок каждой базы.
 */
public final class DatabaseSelectionDialog {

    private DatabaseSelectionDialog() {
    }

    public static void show(RunYBase owner) {
        try {
            List<BaseEntry> baseEntries = DatabaseListService.loadAndSortDatabases();

            // Загружаем заметки для каждой базы
            for (BaseEntry entry : baseEntries) {
                String note = owner.getHistoryManager().getNote(entry.connect);
                if (note != null && !note.isEmpty()) {
                    entry.setNote(note);
                }
            }

            if (baseEntries.isEmpty()) {
                RunYBase.showAlert(Alert.AlertType.INFORMATION, "Список баз", "Нет зарегистрированных баз 1С");
                return;
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Выбор базы 1С");
            dialog.setResizable(true);

            ListView<BaseEntry> listView = new ListView<>(FXCollections.observableArrayList(baseEntries));
            listView.setCellFactory(lv -> new BaseEntryListCell());

            Button okButton = owner.createDialogConfirmButton("OK");
            okButton.setOnAction(e -> selectCurrentBase(owner, listView, dialog));

            Button cancelButton = owner.createDialogCancelButton("Отмена");
            cancelButton.setOnAction(e -> dialog.close());

            listView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    selectCurrentBase(owner, listView, dialog);
                }
            });

            HBox buttonPanel = new HBox(10, okButton, cancelButton);
            buttonPanel.setAlignment(Pos.CENTER);
            buttonPanel.setPadding(new Insets(10));

            BorderPane pane = new BorderPane();
            pane.setCenter(listView);
            pane.setBottom(buttonPanel);
            pane.setPadding(new Insets(10));

            Scene scene = new Scene(pane, 650, 450);

            // Escape закрывает окно без выбора базы. Фильтр на фазе захвата
            // надёжнее setOnKeyPressed: событие перехватывается раньше, чем
            // его успеет потребить ListView.
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    e.consume();
                    dialog.close();
                }
            });

            // Фокус на списке сразу после показа окна: без focus owner в сцене
            // клавиатурные события не доставляются и Escape не сработает
            dialog.setOnShown(e -> listView.requestFocus());

            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            RunYBase.showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при чтении списка баз:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Выбирает текущую выделенную базу и закрывает диалог.
     */
    private static void selectCurrentBase(RunYBase owner, ListView<BaseEntry> listView, Stage dialog) {
        BaseEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            owner.setCurrentDatabase(selected.connect, selected.name);
            dialog.close();
        }
    }
}
