package l1c.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import l1c.AppConstants;
import l1c.RunYBase;
import l1c.RunYBaseHelpTexts;
import l1c.UiFactory;
import l1c.UserCredentials;


/**
 * Диалог управления пользователями для текущего адреса.
 *
 * <p><b>{@link #show(RunYBase)}</b> — простой диалог редактирования
 * главного (основного) пользователя: имя и пароль. Открывается по кнопке
 * «Пользователь» и работает с основным пользователем так же, как раньше.</p>
 *
 * <p><b>{@link #showManager(RunYBase)}</b> — менеджер списка пользователей
 * (добавить/редактировать/удалить/сделать главным).
 * Открывается через контекстное меню поля адреса.</p>
 */
public final class UserCredentialsDialog {

    private UserCredentialsDialog() {
    }

    // ========================================================================
    // Публичный API
    // ========================================================================

    /**
     * Открывает простой диалог редактирования главного пользователя.
     * Используется кнопкой «Пользователь».
     */
    public static void show(RunYBase owner) {
        String address = owner.getCurrentAddress();
        if (address.isEmpty()) {
            RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        UserCredentials mainUser = owner.getCredentialsManager().getMain(address);
        String currentUsername = mainUser != null ? mainUser.getUsername() : "";
        String currentPassword = mainUser != null ? mainUser.getPassword() : "";

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
                    owner.getCredentialsManager().putMainUser(address, new UserCredentials(username, password, true));
                    owner.showAutoClosingAlert("Учётные данные сохранены для адреса:\n" + address,
                            "Пользователь: " + username, 3);
                } else if (mainUser != null) {
                    // Пустое имя — удаляем главного пользователя. Пока у базы есть другие
                    // пользователи, главный не удаляется: нужно сначала сделать главным другого.
                    boolean removed = owner.getCredentialsManager().removeUser(address, mainUser.getUsername());
                    if (removed) {
                        RunYBase.showAlert(Alert.AlertType.INFORMATION, "Удалено",
                                "Учётные данные удалены для адреса:\n" + address);
                    } else {
                        RunYBase.showAlert(Alert.AlertType.WARNING, "Удаление запрещено",
                                String.format(RunYBaseHelpTexts.WARNING_MAIN_USER_IN_USE, mainUser.getUsername()));
                    }
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

        // Фильтруем подсказки по мере ввода (без учёта регистра).
        // Важно: менять предикат FilteredList при закрытом списке нельзя — ComboBox
        // при перестройке items обновляет отображаемое значение и затирает ввод,
        // а его программный editor.setText() снова запускает фильтрацию, что даёт
        // бесконечную рекурсию и StackOverflowError. Поэтому фильтруем только пока
        // список раскрыт (тогда значение ещё не выбрано и текст не перезаписывается).
        box.getEditor().textProperty().addListener((obs, old, text) -> {
            if (!box.isShowing()) {
                return;
            }
            String typed = text == null ? "" : text.trim();
            String selected = box.getValue() == null ? "" : box.getValue();
            if (typed.equals(selected)) {
                return;
            }
            String filter = typed.toLowerCase(Locale.ROOT);
            items.setPredicate(filter.isEmpty() ? s -> true : s -> s.toLowerCase(Locale.ROOT).contains(filter));
        });
        // При открытии списка показываем все имена, а не только отфильтрованные
        box.showingProperty().addListener((obs, was, isShown) -> {
            if (isShown && items.getPredicate() != null) {
                items.setPredicate(null);
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

    /**
     * Открывает менеджер списка пользователей.
     * Используется через контекстное меню поля адреса.
     */
    public static void showManager(RunYBase owner) {
        String address = owner.getCurrentAddress();
        if (address.isEmpty()) {
            RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        List<UserCredentials> users = owner.getCredentialsManager().getUsers(address);
        if (users == null) {
            users = new ArrayList<>();
        }
        ObservableList<UserCredentials> list = FXCollections.observableArrayList(users);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner.getMainStage());
        dialog.setTitle("Пользователи для базы — " + address);

        // Список пользователей
        ListView<UserCredentials> listView = new ListView<>(list);
        listView.setCellFactory(lv -> new ListCell<UserCredentials>() {
            @Override
            protected void updateItem(UserCredentials item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String mark = item.isMain() ? "★ " : "  ";
                String pass = item.getPassword().isEmpty() ? RunYBaseHelpTexts.USER_NO_PASS_LABEL
                        : RunYBaseHelpTexts.USER_HAS_PASS_LABEL;
                setText(mark + item.getUsername() + pass);
            }
        });
        listView.setMaxWidth(Double.MAX_VALUE);
        listView.setPrefHeight(150);

        // Кнопки управления. Confirm-кнопки используют main-button из app.css,
        // который теперь автоматически подключается к сцене диалога (UiFactory.attachAppStylesheet).
        // Кнопка «Закрыть» — нативная (белая), а не жёлтая.
        Button mainButton = UiFactory.createDialogConfirmButton(RunYBaseHelpTexts.USER_LIST_SET_MAIN);
        Button removeButton = UiFactory.createDialogConfirmButton(RunYBaseHelpTexts.USER_LIST_DELETE);
        // Кнопка «Сделать главным» не нужна, если выбора нет или выбран уже главный.
        // Кнопка «Удалить» не нужна, если выбора нет, а также если выбран главный
        // при наличии других пользователей: сначала нужно сделать главным другого.
        Runnable refreshButtons = () -> {
            UserCredentials sel = listView.getSelectionModel().getSelectedItem();
            mainButton.setDisable(sel == null || sel.isMain());
            removeButton.setDisable(sel == null || (sel.isMain() && list.size() > 1));
        };
        listView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> refreshButtons.run());
        refreshButtons.run();

        mainButton.setOnAction(e -> {
            UserCredentials sel = listView.getSelectionModel().getSelectedItem();
            if (sel == null || sel.isMain()) {
                return;
            }
            // Меняем только флаг: порядок списка и выделение остаются прежними
            sel.setMain(true);
            for (UserCredentials u : list) {
                if (u != sel) {
                    u.setMain(false);
                }
            }
            listView.refresh();
            refreshButtons.run();
        });

        Button addButton = UiFactory.createDialogConfirmButton(RunYBaseHelpTexts.USER_LIST_ADD);
        addButton.setOnAction(e -> {
            openEditDialog(owner, listView, null, dialog);
            refreshButtons.run();
        });

        Button editButton = UiFactory.createDialogConfirmButton(RunYBaseHelpTexts.USER_LIST_EDIT);
        editButton.setOnAction(e -> {
            UserCredentials sel = listView.getSelectionModel().getSelectedItem();
            if (sel == null) {
                RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите пользователя для редактирования!");
                return;
            }
            openEditDialog(owner, listView, sel, dialog);
            refreshButtons.run();
        });

        removeButton.setOnAction(e -> {
            UserCredentials sel = listView.getSelectionModel().getSelectedItem();
            // Главный удаляется только когда других пользователей нет —
            // то же правило, что в CredentialsManager.removeUser
            if (sel == null || (sel.isMain() && list.size() > 1)) {
                return;
            }
            list.remove(sel);
            listView.refresh();
            refreshButtons.run();
        });

        Button closeBtn = UiFactory.createDialogButton(RunYBaseHelpTexts.USER_LIST_CLOSE);
        closeBtn.setOnAction(e -> {
            owner.getCredentialsManager().putUsers(address, new ArrayList<>(list));
            owner.updateUserButtonState();
            dialog.close();
        });

        HBox buttonPanel = new HBox(10, addButton, editButton, removeButton, mainButton, closeBtn);
        buttonPanel.setAlignment(Pos.CENTER);
        buttonPanel.setPadding(new Insets(10, 0, 0, 0));

        // Для адаптации к ширине окна
        HBox.setHgrow(listView, Priority.ALWAYS);

        VBox pane = new VBox(10, listView, buttonPanel);
        pane.setPadding(new Insets(10));
        pane.setStyle("-fx-background-color: " + AppConstants.COLOR_BG + ";");

        Scene scene = new Scene(pane, 550, 380);
        dialog.setScene(scene);
        // Пункт меню вызывается из открытого ContextMenu. Пока popup закрывается,
        // JavaFX ещё обрабатывает событие меню, и прямой showAndWait() может быть
        // проигнорирован (окно не появляется). Открываем окно на следующем цикле
        // событий, когда меню уже закрыто.
        Platform.runLater(dialog::showAndWait);
    }

    // ========================================================================
    // Внутренние методы менеджера
    // ========================================================================

    /**
     * Открывает диалог редактирования/добавления пользователя (для менеджера).
     */
    private static void openEditDialog(RunYBase owner, ListView<UserCredentials> listView,
            UserCredentials existing, Stage parent) {
        Dialog<ButtonType> editDialog = new Dialog<>();
        editDialog.initOwner(parent);
        editDialog.initModality(Modality.APPLICATION_MODAL);
        editDialog.setTitle(existing == null ? "Добавить пользователя" : "Редактировать пользователя");

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        editDialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label nameLabel = new Label("Имя пользователя:");
        ComboBox<String> nameBox = createUserNameField(existing != null ? existing.getUsername() : null);
        nameBox.setPrefWidth(300);
        if (existing != null) {
            nameBox.setDisable(true); // имя нельзя менять
        }

        Label passLabel = new Label("Пароль:");
        PasswordField passField = new PasswordField();
        if (existing != null) {
            passField.setText(existing.getPassword());
        }
        passField.setPromptText("Пароль");
        passField.setPrefWidth(300);

        CheckBox mainCheck = new CheckBox("Главный (по умолчанию)");
        if (existing != null) {
            mainCheck.setSelected(existing.isMain());
        }

        content.getChildren().addAll(nameLabel, nameBox, passLabel, passField, mainCheck);
        editDialog.getDialogPane().setContent(content);

        editDialog.showAndWait().ifPresent(result -> {
            if (result == okType) {
                // При редактировании имя неизменно, поле отключено
                String username = existing != null ? existing.getUsername() : getUserName(nameBox);

                if (username.isEmpty()) {
                    RunYBase.showAlert(Alert.AlertType.WARNING, "Предупреждение", "Введите имя пользователя!");
                    return;
                }

                ObservableList<UserCredentials> list = listView.getItems();

                if (existing != null) {
                    // Обновляем существующего пользователя (заменяем объект)
                    UserCredentials updated = new UserCredentials(
                            existing.getUsername(), passField.getText(), mainCheck.isSelected());
                    int idx = list.indexOf(existing);
                    if (idx >= 0) {
                        list.set(idx, updated);
                    }
                    // Снять флаг главного у остальных
                    if (mainCheck.isSelected()) {
                        for (UserCredentials u : list) {
                            if (u != updated) {
                                u.setMain(false);
                            }
                        }
                        // Порядок списка не меняем — выделение остаётся на этом пользователе
                    }
                } else {
                    // Добавляем нового
                    UserCredentials newUser = new UserCredentials(
                            username, passField.getText(), mainCheck.isSelected());
                    if (mainCheck.isSelected()) {
                        for (UserCredentials u : list) {
                            u.setMain(false);
                        }
                    }
                    list.add(newUser);
                    // Порядок списка не меняем — выделение остаётся на этом пользователе
                }

                listView.refresh();
            }
        });
    }
}
