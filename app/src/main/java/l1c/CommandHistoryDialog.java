package l1c;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Полноценное окно истории команд для выбранной платформы.
 * Команды отображаются полностью, каждую можно скопировать,
 * двойной клик вставляет команду в TextArea и закрывает окно.
 */
public final class CommandHistoryDialog {

    private CommandHistoryDialog() {
    }

    public static void show(RunYBase owner, String platform, TextArea textArea) {
        Stage historyStage = new Stage();
        historyStage.initModality(Modality.APPLICATION_MODAL);
        historyStage.initOwner(owner.getMainStage());
        historyStage.setTitle("История команд — " + platform);
        historyStage.setResizable(true);

        // Заголовки колонок истории
        HBox headerRow = new HBox(10);
        headerRow.setPadding(new Insets(5, 5, 5, 5));
        headerRow.setStyle("-fx-background-color: " + AppConstants.COLOR_BUTTON_SMALL_BG + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label hdrBase = new Label("База");
        hdrBase.setMinWidth(180); hdrBase.setMaxWidth(180);
        Label hdrMode = new Label("Режим");
        hdrMode.setMinWidth(160); hdrMode.setMaxWidth(160);
        Label hdrCmd = new Label("Команда");
        hdrCmd.setMaxWidth(500); HBox.setHgrow(hdrCmd, Priority.ALWAYS);
        Label hdrTime = new Label("Последний запуск");
        hdrTime.setMinWidth(110); hdrTime.setMaxWidth(110);
        headerRow.getChildren().addAll(hdrBase, hdrMode, hdrCmd, hdrTime);

        // ListView для отображения команд
        ListView<HistoryManager.CommandEntry> historyListView = new ListView<>(owner.getHistoryManager().getCommandHistoryList(platform));
        historyListView.setCellFactory(lv -> new HistoryListCell());
        historyListView.setPrefHeight(250);

        // Кнопка копирования — второстепенная
        Button copyButton = owner.createDialogCancelButton("Копировать");
        copyButton.setTooltip(owner.createTooltip("Скопировать выбранную команду в буфер обмена"));
        copyButton.setOnAction(e -> {
            HistoryManager.CommandEntry selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.command != null && !selected.command.isEmpty()) {
                owner.copyToClipboard(selected.command, historyStage);
            } else {
                RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите команду для копирования!");
            }
        });

        // Кнопка выбора (вставка в поле вывода) — подтверждающая, как OK в окне выбора базы
        Button selectButton = owner.createDialogConfirmButton("В_ыбрать");
        selectButton.setTooltip(owner.createTooltip("Вставить выбранную команду в поле вывода и закрыть окно (Ctrl+Enter)"));
        selectButton.setOnAction(e -> {
            HistoryManager.CommandEntry selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.command != null && !selected.command.isEmpty()) {
                textArea.setText(selected.command);
                historyStage.close();
            } else {
                RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите команду!");
            }
        });

        // Кнопка закрытия — второстепенная, как Отмена в окне выбора базы
        Button closeButton = owner.createDialogCancelButton("Закрыть");
        closeButton.setOnAction(e -> historyStage.close());

        // Панель кнопок
        HBox buttonPanel = new HBox(10, selectButton, copyButton, closeButton);
        buttonPanel.setAlignment(Pos.CENTER);
        buttonPanel.setPadding(new Insets(10, 0, 0, 0));

        // Основной контейнер
        VBox mainBox = new VBox(0, headerRow, historyListView);
        mainBox.setPadding(new Insets(0, 0, 0, 0));

        BorderPane pane = new BorderPane();
        pane.setCenter(mainBox);
        pane.setBottom(buttonPanel);
        pane.setPadding(new Insets(10));

        Scene scene = new Scene(pane, 1000, 400);
        scene.setFill(Color.TRANSPARENT);
        historyStage.setScene(scene);

        // Двойной клик вставляет команду и закрывает окно
        historyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                HistoryManager.CommandEntry selected = historyListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.command != null && !selected.command.isEmpty()) {
                    textArea.setText(selected.command);
                    historyStage.close();
                }
            }
        });

        // Escape закрывает окно
        scene.addEventHandler(KeyEvent.ANY, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                historyStage.close();
            }
        });

        // Ctrl+Enter — нажать кнопку "Выбрать"
        scene.addEventHandler(KeyEvent.ANY, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                e.consume();
                selectButton.fire();
            }
        });
        historyListView.addEventHandler(KeyEvent.ANY, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                historyStage.close();
            }
        });
        pane.addEventHandler(KeyEvent.ANY, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                historyStage.close();
            }
        });

        historyStage.show();

        // Запрашиваем фокус на ListView после показа окна
        historyListView.setFocusTraversable(true);
        Platform.runLater(() -> historyListView.requestFocus());
    }
}