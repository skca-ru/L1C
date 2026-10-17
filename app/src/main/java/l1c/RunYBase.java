package l1c;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.*;
import javafx.util.Duration;
import javafx.scene.input.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

public class RunYBase extends Application {

    // @formatter:off
    // #region ========== НАСТРОЙКИ ==========
    private static final boolean SHOW_DEBUG_PANEL    = false;
    private static final boolean SHOW_RUN_MESSAGE    = true;

    // Единая минимальная ширина кнопок в диалогах
    private static final double DIALOG_BUTTON_WIDTH   = 100;
    // #endregion ==================================
    // @formatter:on

    private HistoryManager historyManager;
    private ComboBox<String> addressComboBox;
    private ComboBoxWithMenuButton<String> addressControl;
    private TextArea outputArea86;
    private TextArea outputArea;
    private RadioButton designerRadio;
    private RadioButton thinRadio;
    private RadioButton thickOrdinaryRadio;
    private RadioButton thickManagedRadio;
    private RadioButton updateConfigRadio;
    private ToggleGroup modeGroup;
    private CheckBox priorityPlatformCheckbox;
    private CheckBox debugModeCheckbox;
    private CheckBox clearCacheCheckbox;
    private CheckBox executeProcessingCheckbox;
    private ComboBoxWithMenuButton<String> executeProcessingField;
    private ComboBox<String> debugProtocolCombo;
    private TextArea debugArea;
    private ObservableList<String> historyList;
    private CredentialsManager credentialsManager;

    private Button userCredentialsButton;
    private static final String CREDENTIALS_CLIPBOARD_PREFIX = "L1C_CREDENTIALS_V1:";

    // Label для отображения имени БД и заметки под полем адреса
    private Label baseNameLabel;
    private TextArea baseNoteLabel;

    // Для проверки есть адрес в списке зарегистрированных баз
    private Map<String, String> registeredAddressMap = new HashMap<>();

    // Контейнер для всех элементов интерфейса
    private VBox contentBox;

    // Главное окно — владелец всплывающих уведомлений (Popup)
    private Stage mainStage;

    // Последнее показанное уведомление, чтобы закрывать его перед новым
    private Popup activePopup;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        credentialsManager = new CredentialsManager();

        // Создаём меню
        MenuBar menuBar = createMenuBar();

        // Корневой BorderPane без отступов
        BorderPane borderRoot = new BorderPane();
        borderRoot.setStyle("-fx-background-color: " + AppConstants.COLOR_BG + ";");
        borderRoot.setTop(menuBar);

        // Все остальные элементы помещаем в VBox с отступами
        contentBox = new VBox(8);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: " + AppConstants.COLOR_BG + ";");

        // --- Область Адреса БД ---
        contentBox.getChildren().add(createAddressPanel());

        // --- Режим запуска ---
        contentBox.getChildren().add(createModePanel());

        // Разделитель между секциями (дополнительный отступ)
        contentBox.getChildren().add(new Region());

        // #region Блоки вывода команд для платформ
        createPlatformPanel("x86", 32);
        createPlatformPanel("x64", 64);
        // #endregion

        if (SHOW_DEBUG_PANEL) {
            contentBox.getChildren().add(new Label("Отладка (вывод команды и ошибок):"));
            debugArea = new TextArea();
            debugArea.setPrefRowCount(8);
            debugArea.setStyle(
                    "-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + AppConstants.COLOR_OUTPUT_BG + ";");
            debugArea.setStyle(debugArea.getStyle() + "-fx-border-color: " + AppConstants.COLOR_ACCENT + ";");
            contentBox.getChildren().add(debugArea);
        } else {
            debugArea = new TextArea();
        }

        borderRoot.setCenter(contentBox);

        Scene scene = new Scene(borderRoot, 1050, SHOW_DEBUG_PANEL ? 700 : 500);

        // Стили приложения: подавляют фоны modena.css у внутренних частей кнопок (l1c/app.css)
        URL appCss = RunYBase.class.getResource("/l1c/app.css");
        if (appCss != null) {
            scene.getStylesheets().add(appCss.toExternalForm());
        }

        primaryStage.setTitle(String.format(AppConstants.WINDOW_TITLE, AppConstants.VERSION));
        primaryStage.setScene(scene);
        primaryStage.show();
        mainStage = primaryStage;

        installStandardEditContextMenu(addressComboBox.getEditor());
        installStandardEditContextMenu(outputArea);
        installStandardEditContextMenu(outputArea86);
        if (debugArea != null)
            installStandardEditContextMenu(debugArea);

        autoPasteFromClipboard();

        addressComboBox.requestFocus();

        // Загружаем список зарегистрированных баз для отображения имён
        try {
            List<BaseEntry> baseEntries = loadAndSortDatabases();
            if (!baseEntries.isEmpty()) {
                registeredAddressMap.clear();
                for (BaseEntry entry : baseEntries) {
                    registeredAddressMap.put(entry.connect, entry.name);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибку — просто не будем показывать имена баз
        }
    }

    /**
     * Добавляет текущий адрес из поля ввода в список зарегистрированных баз
     * (ibases.v8i)
     */
    private void addCurrentAddressToDatabaseList() {
        String address = getCurrentAddress();
        if (address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Нет адреса для добавления!");
            return;
        }

        try {
            // Проверяем, есть ли уже такой адрес
            List<BaseEntry> existing = loadAndSortDatabases();
            for (BaseEntry entry : existing) {
                if (entry.connect != null && entry.connect.equals(address)) {
                    showAlert(Alert.AlertType.INFORMATION, "Информация",
                            "Этот адрес уже есть в списке баз под именем:\n" + entry.name);
                    return;
                }
            }

            // Запрашиваем имя для новой базы
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Добавление базы");
            nameDialog.setHeaderText("Введите имя для новой базы");
            nameDialog.setContentText("Имя базы:");
            Optional<String> result = nameDialog.showAndWait();

            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String name = result.get().trim();
                addDatabaseEntryToFile(address, name);
                // Обновляем карту для подсказок
                registeredAddressMap.put(address, name);
                showAlert(Alert.AlertType.INFORMATION, "Успешно",
                        "База '" + name + "' добавлена в список!\n" +
                                "Адрес: " + address);
            } else {
                showAlert(Alert.AlertType.WARNING, "Предупреждение",
                        "Имя не введено, добавление отменено.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Ошибка при добавлении в список баз:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Перечитывает файл ibases.v8i и обновляет registeredAddressMap
     */
    private void refreshDatabaseList() {
        try {
            List<BaseEntry> baseEntries = loadAndSortDatabases();
            if (!baseEntries.isEmpty()) {
                registeredAddressMap.clear();
                for (BaseEntry entry : baseEntries) {
                    registeredAddressMap.put(entry.connect, entry.name);
                }
            }
            showAutoClosingAlert(
                    """
                            Список баз обновлён.
                            Загружено записей:""" + " " + baseEntries.size(),
                    "Обновление", 3);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Ошибка при обновлении списка баз:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Копирует учётные данные (имя пользователя и пароль) для текущего адреса
     * в системный буфер обмена.
     */
    private void copyCredentials() {
        String address = getCurrentAddress();
        if (address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Нет адреса для копирования учётных данных!");
            return;
        }
        UserCredentials cred = credentialsManager.get(address);
        if (cred == null || cred.getUsername().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Для этого адреса нет сохранённых учётных данных!");
            return;
        }
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(serializeCredentials(cred));
        Clipboard.getSystemClipboard().setContent(clipboardContent);
        showAutoClosingAlert("Учётные данные скопированы в буфер обмена для адреса:\n" + address,
                "Копирование", 3);
    }

    /**
     * Вставляет учётные данные из системного буфера обмена для текущего адреса.
     */
    private void pasteCredentials() {
        String address = getCurrentAddress();
        if (address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Нет адреса для вставки учётных данных!");
            return;
        }
        UserCredentials copiedCredentials = deserializeCredentials(Clipboard.getSystemClipboard().getString());
        if (copiedCredentials == null) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение",
                    "В буфере обмена нет учётных данных, скопированных из программы!");
            return;
        }
        // Проверяем, не совпадает ли адрес с исходным (можно и не проверять)
        UserCredentials existing = credentialsManager.get(address);
        if (existing != null && existing.getUsername().equals(copiedCredentials.getUsername())) {
            // Если уже такие же данные, можно предупредить
            // int answer = showConfirmation("Учётные данные уже совпадают. Всё равно
            // заменить?");
            // if (answer != JOptionPane.YES_OPTION) return;
        }
        credentialsManager.put(address,
                new UserCredentials(copiedCredentials.getUsername(), copiedCredentials.getPassword()));
        updateUserButtonState();
        showAutoClosingAlert("Учётные данные вставлены для адреса:\n" + address, "Вставка", 3);
    }

    private String serializeCredentials(UserCredentials credentials) {
        Base64.Encoder encoder = Base64.getEncoder();
        return CREDENTIALS_CLIPBOARD_PREFIX
                + encoder.encodeToString(credentials.getUsername().getBytes(StandardCharsets.UTF_8))
                + ":"
                + encoder.encodeToString(credentials.getPassword().getBytes(StandardCharsets.UTF_8));
    }

    private UserCredentials deserializeCredentials(String clipboardText) {
        if (clipboardText == null || !clipboardText.startsWith(CREDENTIALS_CLIPBOARD_PREFIX)) {
            return null;
        }

        String[] parts = clipboardText.substring(CREDENTIALS_CLIPBOARD_PREFIX.length()).split(":", -1);
        if (parts.length != 2) {
            return null;
        }

        try {
            Base64.Decoder decoder = Base64.getDecoder();
            String username = new String(decoder.decode(parts[0]), StandardCharsets.UTF_8);
            String password = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8);
            return username.isEmpty() ? null : new UserCredentials(username, password);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Button createButton(String text) {
        return createButton(text, AppConstants.COLOR_BUTTON_BG);
    }

    /**
     * Добавляет запись о новой базе в файл ibases.v8i
     */
    private void addDatabaseEntryToFile(String connect, String name) throws IOException {
        Path ibasesPath = Paths.get(System.getProperty("user.home"),
                "AppData", "Roaming", "1C", "1CEStart", "ibases.v8i");

        // Создаём директорию и файл, если их нет
        if (!Files.exists(ibasesPath)) {
            Files.createDirectories(ibasesPath.getParent());
            Files.createFile(ibasesPath);
        }

        // Читаем существующие строки
        List<String> lines = Files.readAllLines(ibasesPath, StandardCharsets.UTF_8);

        // Находим максимальный OrderInList для корректного порядка
        double maxOrder = 0;
        for (String line : lines) {
            if (line.startsWith("OrderInList=")) {
                try {
                    double val = Double.parseDouble(line.substring(12));
                    if (val > maxOrder)
                        maxOrder = val;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Генерируем уникальный ID
        String guid = UUID.randomUUID().toString();

        // Формируем новую секцию
        List<String> newEntry = new ArrayList<>();
        newEntry.add("[" + name + "]");
        newEntry.add("Connect=" + connect);
        newEntry.add("ID=" + guid);
        newEntry.add("OrderInList=" + (maxOrder + 16384));
        newEntry.add("Folder=/");
        newEntry.add("OrderInTree=" + (maxOrder + 16384));
        newEntry.add("External=0");
        newEntry.add("App=Auto");
        newEntry.add("WA=1");
        newEntry.add("DisableLocalSpeechToText=0");
        newEntry.add(""); // пустая строка для разделения

        // Добавляем в конец файла
        lines.addAll(newEntry);
        Files.write(ibasesPath, lines, StandardCharsets.UTF_8);
    }

    private Button createButton(String text, String bgColor) {

        Button button = new Button(text);
        // Что бы ускорители на кнопке работали
        button.setMnemonicParsing(true);

        button.setUserData(bgColor); // Сохраняем базовый цвет

        // Устанавливаем минимальную высоту для всех кнопок
        button.setMinHeight(30);

        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-weight: bold;
                -fx-font-size: 11px;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-border-radius: 3px;
                -fx-background-radius: 3px""",
                bgColor, AppConstants.COLOR_BUTTON_FG, AppConstants.COLOR_BUTTON_BORDER));

        // Эффект наведения курсора
        button.setOnMouseEntered(e -> {
            String baseColor = (String) button.getUserData();
            button.setStyle(String.format("""
                    -fx-background-color: %s;
                    -fx-text-fill: %s;
                    -fx-font-weight: bold;
                    -fx-font-size: 11px;
                    -fx-border-color: %s;
                    -fx-border-width: 1px;
                    -fx-border-radius: 3px;
                    -fx-background-radius: 3px""",
                    adjustColorBrightness(baseColor, 1.1), AppConstants.COLOR_BUTTON_FG, AppConstants.COLOR_BUTTON_BORDER));
        });

        button.setOnMouseExited(e -> {
            String baseColor = (String) button.getUserData();
            button.setStyle(String.format("""
                    -fx-background-color: %s;
                    -fx-text-fill: %s;
                    -fx-font-weight: bold;
                    -fx-font-size: 11px;
                    -fx-border-color: %s;
                    -fx-border-width: 1px;
                    -fx-border-radius: 3px;
                    -fx-background-radius: 3px""",
                    baseColor, AppConstants.COLOR_BUTTON_FG, AppConstants.COLOR_BUTTON_BORDER));
            button.setTranslateY(0);
            button.setTranslateX(0);
        });

        // Эффект нажатия - сдвигаем кнопку на 1px вниз-вправо
        button.setOnMousePressed(e -> {
            button.setTranslateY(1);
            button.setTranslateX(1);
        });

        button.setOnMouseReleased(e -> {
            button.setTranslateY(0);
            button.setTranslateX(0);
        });

        return button;
    }

    /**
     * Кнопка подтверждения в диалогах (OK в выборе базы, Выбрать в истории).
     * Фон задаётся сразу, а не дописывается в style: иначе обработчики
     * наведения из createButton возвращают базовый цвет и кнопка «прыгает».
     */
    public Button createDialogConfirmButton(String text) {
        Button button = createButton(text, AppConstants.COLOR_BUTTON_SMALL_BG);
        button.setMinWidth(DIALOG_BUTTON_WIDTH);
        return button;
    }

    /**
     * Второстепенная кнопка диалога (Отмена, Копировать, Закрыть) —
     * имитирует стандартную кнопку Windows: белое поле, тонкая серая рамка
     * с более тёмным нижним краем, скругление 4 px, при наведении и
     * нажатии фон затемняется.
     */
    public Button createDialogCancelButton(String text) {
        Button button = new Button(text);
        button.setMnemonicParsing(true);
        button.setMinWidth(DIALOG_BUTTON_WIDTH);
        button.setMinHeight(30);

        // Состояние берём из свойств кнопки, а не из обработчиков мыши:
        // иначе при уходе курсора во время нажатия кнопка останется тёмной
        Runnable repaint = () -> applyNativeButtonStyle(button);
        button.hoverProperty().addListener((obs, was, now) -> repaint.run());
        button.pressedProperty().addListener((obs, was, now) -> repaint.run());
        repaint.run();

        return button;
    }

    /**
     * Палитра кнопки Windows 11 (светлая тема). Инлайновый CSS не умеет
     * селекторы :hover/:pressed, поэтому состояния раскрашиваются вручную.
     */
    private static void applyNativeButtonStyle(Button button) {
        // @formatter:off
        String bg, side, bottom, text;
        if (button.isPressed()) {
            bg = "#F5F5F5"; side = "#C4C4C4"; bottom = "#7A7A7A"; text = "#5F5F5F";
        } else if (button.isHover()) {
            bg = "#F9F9F9"; side = "#C9C9C9"; bottom = "#8C8C8C"; text = "#1A1A1A";
        } else {
            bg = "#FDFDFD"; side = "#D8D8D8"; bottom = "#9A9A9A"; text = "#1A1A1A";
        }
        // @formatter:on
        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-background-radius: 4;
                -fx-border-color: %s %s %s %s;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-text-fill: %s;
                -fx-font-size: 12px;""",
                bg, side, side, bottom, side, text));
    }

    private Button createFlatButton(String text) {
        Button button = new Button(text);

        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 12px;
                -fx-border-color: gray;
                -fx-border-radius: 3px;
                -fx-background-radius: 3px""",
                AppConstants.COLOR_INPUT_BG, AppConstants.COLOR_TEXT_FG));
        return button;
    }

     private HBox createHelpOption(ComboBoxWithMenuButton option, TextField field, Button helpButton) {
        HBox box = new HBox(5, option, field, helpButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createHelpOption(CheckBox option, Button helpButton) {
        HBox box = new HBox(3, option, helpButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createHelpOption(CheckBox option, ComboBox<String> combo, Button helpButton) {
        HBox box = new HBox(5, option, combo, helpButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createHelpOption(CheckBox option, TextField field, Button helpButton) {
        HBox box = new HBox(5, option, field, helpButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createHelpOption(CheckBox option, ComboBoxWithMenuButton field, Button helpButton) {
        HBox box = new HBox(5, option, field, helpButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * Создаёт VBox с кнопками Copy и Run для TextArea с командой
     * 
     * @param textArea TextArea с командой
     * @param platform название платформы для отображения (x86, x64 и т.д.)
     * @return VBox с кнопками
     */
    private VBox createRunCopyButtons(TextArea textArea, String platform) {
        VBox buttonPanel = new VBox(5);
        buttonPanel.setAlignment(Pos.CENTER);

        Button copyButton = createButton("Copy");
        copyButton.setStyle(copyButton.getStyle() + "; -fx-background-color: " + AppConstants.COLOR_BUTTON_SMALL_BG + ";");
        copyButton.setMinWidth(80);
        copyButton.setPrefWidth(80);
        copyButton.setMaxWidth(80);
        copyButton.setTooltip(createTooltip("Копировать команду в буфер обмена"));
        copyButton.setOnAction(e -> copyToClipboard(textArea.getText(),
                textArea.getScene() != null ? textArea.getScene().getWindow() : null));

        Button runButton = createButton("Run");
        runButton.setStyle(runButton.getStyle() + "; -fx-background-color: " + AppConstants.COLOR_BUTTON_SMALL_BG + ";");
        runButton.setMinWidth(80);
        runButton.setPrefWidth(80);
        runButton.setMaxWidth(80);
        runButton.setOnAction(e -> {
            String command = textArea.getText();
            String runMode = getRunModeFromCommand(command);
            historyManager.addCommandToHistory(platform, command, runMode);
            runCommand(command, platform);
        });

        buttonPanel.getChildren().addAll(copyButton, runButton);
        return buttonPanel;
    }

    /**
     * Показывает popup со списком исторических команд
     */
    private void showCommandHistoryPopup(Button anchorButton, String platform, TextArea textArea) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox popupContent = new VBox(3);
        popupContent.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-border-color: gray; -fx-border-width: 1px;");
        popupContent.setMaxWidth(800);
        popupContent.setPrefHeight(300);

        ObservableList<HistoryManager.CommandEntry> history = historyManager.getCommandHistoryList(platform);

        if (history == null || history.isEmpty()) {
            Label emptyLabel = new Label("История пуста");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            popupContent.getChildren().add(emptyLabel);
        } else {
            for (HistoryManager.CommandEntry entry : history) {
                String cmd = entry.command;
                Label commandLabel = new Label(cmd.length() > 120 ? cmd.substring(0, 117) + "..." : cmd);
                commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 5 3 5; -fx-background-color: transparent;");
                commandLabel.setTooltip(new Tooltip(cmd));

                final String fullCommand = cmd;
                commandLabel.setOnMouseEntered(e -> commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 5 3 5; -fx-background-color: #E8E0D0;"));
                commandLabel.setOnMouseExited(e -> commandLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 5 3 5; -fx-background-color: transparent;"));
                commandLabel.setOnMouseClicked(e -> {
                    textArea.setText(fullCommand);
                    popup.hide();
                });

                popupContent.getChildren().add(commandLabel);
            }
        }

        popup.getContent().add(popupContent);

        // Позиционирование popup — показываем рядом с кнопкой
        javafx.stage.Stage stage = (javafx.stage.Stage) anchorButton.getScene().getWindow();
        popup.show(stage);
        popup.setAutoFix(true);
    }


    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-padding: 0; -fx-background-insets: 0; -fx-background-radius: 0;");

        // Меню Файл
        Menu fileMenu = new Menu("_Файл");
        fileMenu.setStyle("-fx-padding: 5 10 5 10;");

        MenuItem open1CStartItem = new MenuItem("О_ткрыть стандартный запуск 1С");
        open1CStartItem.setOnAction(e -> launchStandard1CStart());

        MenuItem exitItem = new MenuItem("Вы_ход");
        exitItem.setAccelerator(KeyCombination.valueOf("Shortcut+Q"));
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(open1CStartItem, new javafx.scene.control.SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(fileMenu);

        // Меню Помощь
        Menu helpMenu = new Menu("_Помощь");
        helpMenu.setStyle("-fx-padding: 5 10 5 10;");

        MenuItem aboutItem = new MenuItem("О _программе");
        // Горячая клавиша F1 открывает окно "О программе"
        aboutItem.setAccelerator(KeyCombination.keyCombination("F1"));
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().add(aboutItem);
        menuBar.getMenus().add(helpMenu);

        return menuBar;
    }

    /**
     * Уменьшает собственные отступы составных частей сплит-кнопки: текстовой метки
     * и области стрелки. Иначе к полям самой кнопки добавляются поля модификатора
     * темы JavaFX и суммарные отступы слева/справа получаются слишком большими.
     */
    private void compactSplitButtonPadding(SplitMenuButton button) {
        button.skinProperty().addListener((obs, oldSkin, newSkin) -> applyCompactSplitButtonPadding(button));
        applyCompactSplitButtonPadding(button);
    }

    /** Задаёт компактные отступы метки и стрелки сплит-кнопки. */
    private void applyCompactSplitButtonPadding(SplitMenuButton button) {
        if (button.lookup(".label") instanceof Region) {
            ((Region) button.lookup(".label")).setStyle("-fx-padding: 0 2 0 6;");
        }
        if (button.lookup(".arrow-button") instanceof Region) {
            ((Region) button.lookup(".arrow-button")).setStyle("-fx-padding: 0 4 0 4;");
        }
    }

    /**
     * Создаёт панель с режимами запуска и опциями
     * 
     * @return VBox с панелью режимов и опций
     */
    private VBox createModePanel() {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: " + AppConstants.COLOR_PANEL_BG + ";");
        panel.setPadding(new Insets(5));

        // Основная панель с переключателями режимов
        HBox modeRow = new HBox(15);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        Label modeLabel = new Label("Режим запуска:");
        modeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        modeGroup = new ToggleGroup();
        designerRadio = new RadioButton("Конфигуратор");
        designerRadio.setToggleGroup(modeGroup);
        designerRadio.setSelected(true);
        thinRadio = new RadioButton("Предприятие");
        thinRadio.setToggleGroup(modeGroup);
        thickManagedRadio = new RadioButton("Толстый клиент (Управляемое приложение)");
        thickManagedRadio.setToggleGroup(modeGroup);
        thickOrdinaryRadio = new RadioButton("Толстый клиент (Обычное приложение)");
        thickOrdinaryRadio.setToggleGroup(modeGroup);

        updateConfigRadio = new RadioButton("Обновление конфигурации (версия 2, сервер)");
        updateConfigRadio.setToggleGroup(modeGroup);

        Button modeHelpButton = createHelpButton();
        modeHelpButton.setTooltip(createTooltip(RunYBaseHelpTexts.APP_MODE_INFO));
        modeHelpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Справка: режимы запуска",
                RunYBaseHelpTexts.APP_MODE_INFO));

        modeRow.getChildren().addAll(modeLabel, designerRadio, thinRadio, thickManagedRadio, thickOrdinaryRadio, updateConfigRadio, modeHelpButton);

        // Панель с опциями (отладка, приоритет платформы)
        debugModeCheckbox = new CheckBox("Режим отладки");
        debugModeCheckbox.setSelected(true);
        debugModeCheckbox.setTooltip(createTooltip(RunYBaseHelpTexts.DEBUG_MODE_TOOLTIP));

        debugProtocolCombo = new ComboBox<>(FXCollections.observableArrayList("по умолчанию", "-tcp", "-http"));
        debugProtocolCombo.setValue("по умолчанию");
        debugProtocolCombo.setTooltip(createTooltip(RunYBaseHelpTexts.DEBUG_PROTOCOL_INFO));

        Button debugHelpButton = createHelpButton();
        debugHelpButton.setTooltip(createTooltip(RunYBaseHelpTexts.DEBUG_INFO));
        debugHelpButton.setOnAction(
                e -> showAlert(Alert.AlertType.INFORMATION, "Справка: параметр /Debug", RunYBaseHelpTexts.DEBUG_INFO));

        priorityPlatformCheckbox = new CheckBox("Приоритет платформы");
        priorityPlatformCheckbox.setTooltip(createTooltip(RunYBaseHelpTexts.APP_ARCH_TOOLTIP));

        Button helpButton = createHelpButton();
        helpButton.setTooltip(createTooltip(RunYBaseHelpTexts.APP_ARCH_INFO));
        helpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Справка: параметр /AppArch",
                RunYBaseHelpTexts.APP_ARCH_INFO));

        clearCacheCheckbox = new CheckBox("Очистка кэша (/ClearCache)");
        clearCacheCheckbox.setTooltip(createTooltip(RunYBaseHelpTexts.CLEAR_CACHE_TOOLTIP));

        Button clearCacheHelpButton = createHelpButton();
        clearCacheHelpButton.setTooltip(createTooltip(RunYBaseHelpTexts.CLEAR_CACHE_INFO));
        clearCacheHelpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Справка: параметр /ClearCache",
                RunYBaseHelpTexts.CLEAR_CACHE_INFO));

        executeProcessingCheckbox = new CheckBox("Внешняя обработка (/Execute)");
        executeProcessingCheckbox.setTooltip(createTooltip(RunYBaseHelpTexts.EXECUTE_PROCESSING_TOOLTIP));

        executeProcessingField = new ComboBoxWithMenuButton<String>("Введите имя файла внешней обработки", historyManager.getProcessingHistoryList(), "Путь к файлу внешнего отчета или обработки"); 
        executeProcessingField.setPrefWidth(250);
        //executeProcessingField.setStyle("-fx-background-color: " + AppConstants.COLOR_INPUT_BG + "; -fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 3px;");
        executeProcessingField.setVisible(false);
        // Обработчик нажатия на кнопку выбора 
        executeProcessingField.getChoiceButton().setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл обработки");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Внешние обработки 1С (.epf)", "*.epf"),
                new FileChooser.ExtensionFilter("Внешние отчеты 1С (.erf)", "*.erf"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
            );
            
            File selectedPath = fileChooser.showOpenDialog(null);
            if (selectedPath != null) {
                String processingPath = selectedPath.getAbsolutePath();
                executeProcessingField.getComboBox().getEditor().setText(processingPath);
                addProcessingToHistory(processingPath);
            }
        }); 
        executeProcessingCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            executeProcessingField.setVisible(newVal);
        });

        Button executeHelpButton = createHelpButton();
        executeHelpButton.setTooltip(createTooltip(RunYBaseHelpTexts.EXECUTE_PROCESSING_INFO));
        executeHelpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Справка: параметр /Execute",
                RunYBaseHelpTexts.EXECUTE_PROCESSING_INFO));

        HBox debugOption = createHelpOption(debugModeCheckbox, debugProtocolCombo, debugHelpButton);
        HBox platformOption = createHelpOption(priorityPlatformCheckbox, helpButton);
        HBox clearCacheOption = createHelpOption(clearCacheCheckbox, clearCacheHelpButton);
        HBox executeOption = createHelpOption(executeProcessingCheckbox, executeProcessingField, executeHelpButton);
        //HBox executeOption = new HBox(3, executeProcessingCheckbox );
        //box.setAlignment(Pos.CENTER_LEFT);
             
        HBox optionsRow = new HBox(15, debugOption, platformOption, clearCacheOption, executeOption);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(modeRow, optionsRow);
        return panel;
    }

    /**
     * Создаёт блок вывода команды для указанной платформы
     * 
     * @param platformName название платформы (x86 или x64)
     * @param bits         разрядность (32 или 64)
     */
    private void createPlatformPanel(String platformName, int bits) {
        // Метка с названием платформы
        String label = String.format("Команда для %d-битной платформы (%s):", bits, platformName);
        Label platformLabel = new Label(label);
        platformLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        contentBox.getChildren().add(platformLabel);

        // HBox с TextArea, кнопкой истории и кнопками Run/Copy
        HBox platformRow = new HBox(5);
        platformRow.setAlignment(Pos.CENTER_LEFT);

        // Создаём TextArea
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(4);
        textArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + AppConstants.COLOR_INPUT_BG
                + "; -fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        HBox.setHgrow(textArea, Priority.ALWAYS);

        // Кнопка истории команд (компактная)
        Button historyButton = new Button("И...");
        historyButton.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-weight: bold;
                -fx-font-size: 11px;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-border-radius: 3px;
                -fx-background-radius: 3px;
                -fx-padding: 1 4 1 4;
                -fx-min-width: 30; -fx-max-width: 30;
                -fx-min-height: 22; -fx-max-height: 22;""",
                AppConstants.COLOR_BUTTON_SMALL_BG, AppConstants.COLOR_BUTTON_FG, AppConstants.COLOR_BUTTON_BORDER));
        historyButton.setTooltip(createTooltip("История команд для этой платформы (открыть окно)"));
        historyButton.setOnAction(e -> CommandHistoryDialog.show(this, platformName, textArea));

        // Сохраняем ссылку на TextArea в зависимости от разрядности
        if (bits == 32) {
            outputArea86 = textArea;
        } else {
            outputArea = textArea;
        }

        // Создаём панель с кнопками
        VBox buttonPanel = createRunCopyButtons(textArea, platformName);
        platformRow.getChildren().addAll(textArea, historyButton, buttonPanel);
        contentBox.getChildren().add(platformRow);
    }

    /**
     * Создаёт панель с полем ввода адреса базы данных
     * 
     * @return VBox с панелью адреса
     */
    private VBox createAddressPanel() {
        VBox addressPanel = new VBox(5);
        addressPanel.setStyle("-fx-background-color: " + AppConstants.COLOR_PANEL_BG + ";");
        addressPanel.setPadding(new Insets(5));
        //addressPanel.setAlignment(Pos.CENTER);

        // Верхняя строка с полем ввода и кнопками
        HBox inputPanel = new HBox(5);
        inputPanel.setAlignment(Pos.CENTER_LEFT);

        Label addressLabel = new Label("Адрес БД:");
        addressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        // historyList = FXCollections.observableArrayList(getHistoryList());
        historyManager = new HistoryManager();
        ObservableList<String> historyList = historyManager.getHistoryList();

        addressControl = new ComboBoxWithMenuButton<>(RunYBaseHelpTexts.ADDRESS_EXAMPLE_INFO, historyList,
                RunYBaseHelpTexts.BASE_CONNECTION_PROMPT);
        addressControl.setPrefHeight(30);
        addressControl.setMaxHeight(30);
        addressComboBox = addressControl.getComboBox();
        addressControl.getChoiceButton().setOnAction(e -> DatabaseSelectionDialog.show(this));

        setupAddressContextMenu();

        userCredentialsButton = createButton("П_ользователь");
        userCredentialsButton.setOnAction(e -> UserCredentialsDialog.show(this));

        // Создаём сплит-кнопку Сформировать (у кнопки будет меню)
        SplitMenuButton generateMenuButton = new SplitMenuButton();
        generateMenuButton.setText("С_формировать");
        generateMenuButton.getStyleClass().add("flat-split-button"); // см. l1c/app.css
        generateMenuButton.setMnemonicParsing(true); // для ускорителя Alt+С
        generateMenuButton.setOnAction(e -> handleGenerateCommand()); // основное действие

        // Пункт меню
        MenuItem generateByIBItem = new MenuItem("По имени базы (/IBName)");
        generateByIBItem.setOnAction(e -> handleGenerateByIB());
        generateMenuButton.getItems().add(generateByIBItem);

        // Пункт меню — формирование строки запуска без данных пользователя (/N, /P)
        MenuItem generateWithoutUserDataItem = new MenuItem("Без данных пользователя");
        generateWithoutUserDataItem.setOnAction(e -> handleGenerateCommandWithoutUserData());
        generateMenuButton.getItems().add(generateWithoutUserDataItem);

        // Пункт меню — адрес базы в виде /SСервер\База (серверная) или /FКаталог (файловая)
        MenuItem generateByShortAddressItem = new MenuItem(AppConstants.GENERATE_SHORT_ADDRESS_MENU_TEXT);
        generateByShortAddressItem.setOnAction(e -> handleGenerateByShortAddress());
        generateMenuButton.getItems().add(generateByShortAddressItem);

        String bgColor = AppConstants.COLOR_BUTTON_BG;
        generateMenuButton.setStyle(String.format("""
            /* 1. Основной фон и рамка всей кнопки */
            -fx-background-color: %1$s;
            -fx-border-color: %3$s;
            -fx-border-width: 1px;
            -fx-border-radius: 3px;
            -fx-background-radius: 3px;
            
            /* 2. Настройки шрифта и геометрии */
            -fx-text-fill: %2$s;
            -fx-font-weight: bold;
            -fx-font-size: 11px;
            -fx-padding: 0 6 0 8;
            -fx-min-height: 30;
            
            /* 3. Делаем внутренние части прозрачными */
            -fx-color: transparent;
            -fx-body-color: transparent;
            -fx-inner-border: transparent;
            -fx-shadow-highlight-color: transparent;
            
            /* 4. Принудительно возвращаем нужный цвет для текста и стрелочки */
            -fx-text-base-color: %2$s; /* Цвет основного текста кнопки */
            -fx-mark-color: %2$s;      /* Цвет иконки-стрелочки */
            """, bgColor, AppConstants.COLOR_BUTTON_FG, AppConstants.COLOR_BUTTON_BORDER));

        generateMenuButton.setMinHeight(30);
        // Уменьшаем внутренние отступы text-части, чтобы кнопка не была слишком широкой
        compactSplitButtonPadding(generateMenuButton);

        inputPanel.getChildren().addAll(
                addressLabel, addressControl, userCredentialsButton, generateMenuButton);
        HBox.setHgrow(addressControl, Priority.ALWAYS);
        addressPanel.getChildren().add(inputPanel);

        addressPanel.getChildren().add(createBaseInfoPanel());

        // Настройка слушателя для отображения имени базы и заметки при изменении адреса
        addressComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            updateUserButtonState();
            updateBaseInfoDisplay(newVal);
        });
        updateUserButtonState();
        updateBaseInfoDisplay(null);

        return addressPanel;
    }

    /**
     * Создаёт панель с именем БД и заметкой под полем адреса.
     */
    private VBox createBaseInfoPanel() {
        baseNameLabel = new Label();
        baseNameLabel.setWrapText(true);

        baseNoteLabel = new TextArea();
        baseNoteLabel.setWrapText(true);
        baseNoteLabel.setEditable(false);
        baseNoteLabel.setPrefRowCount(1);
        baseNoteLabel.setMinHeight(Region.USE_PREF_SIZE);
        baseNoteLabel.setStyle("""
                -fx-font-style: italic;
                -fx-font-size: 14px;
                -fx-text-fill: #666666;
                -fx-background: transparent;
                -fx-background-color: transparent;
                -fx-padding: 0; """);
        // 2. Полное удаление фона и отступов внутренней панели через Java API
        baseNoteLabel.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> {
                    // Убираем серую подложку у самого контейнера скина
                    if (newSkin.getNode() instanceof Region) {
                        ((Region) newSkin.getNode()).setBackground(javafx.scene.layout.Background.EMPTY);
                    }

                    // Находим внутренний .content, убираем сдвиг влево и очищаем его фон
                    javafx.scene.Node content = baseNoteLabel.lookup(".content");
                    if (content instanceof Region) {
                        Region contentRegion = (Region) content;
                        contentRegion.setStyle("-fx-padding: 0;");
                        contentRegion.setBackground(javafx.scene.layout.Background.EMPTY);
                    }
                });
            }
        });

        VBox infoPanel = new VBox(2, baseNameLabel, baseNoteLabel);
        infoPanel.setPadding(new Insets(5, 0, 0, 0));
        HBox.setHgrow(infoPanel, Priority.ALWAYS);
        return infoPanel;
    }

    private void setupAddressContextMenu() {
        // Пункт «Вставить» — извлекает строку подключения 1С из буфера обмена
        MenuItem pasteItem = new MenuItem("Вставить");
        pasteItem.setOnAction(e -> {
            String clipText = javafx.scene.input.Clipboard.getSystemClipboard().getString();
            if (clipText != null && !clipText.isEmpty()) {
                String extractedAddress = StringExtractor.extractConnectionString(clipText);
                if (extractedAddress != null) {
                    addressComboBox.getEditor().setText(extractedAddress);
                } else {
                    showAlert(Alert.AlertType.WARNING, "Предупреждение", "Не найден адрес базы 1С в буфере обмена!");
                }
            }
        });
        addressControl.getContextMenu().getItems().add(pasteItem);

        MenuItem pasteDirectoryItem = new MenuItem("Вставить каталог");
        pasteDirectoryItem.setOnAction(e -> {
            String clipTextDir = javafx.scene.input.Clipboard.getSystemClipboard().getString();
            if (clipTextDir != null && !clipTextDir.isEmpty()) {
                String directoryPath = StringExtractor.extractDirectoryPath(clipTextDir);
                if (directoryPath != null) {
                    // Экранируем кавычки внутри пути (заменяем " на "")
                    String escapedPath = directoryPath.replace("\"", "\"\"");
                    String connectionString = "File=\"" + escapedPath + "\"";
                    addressComboBox.getEditor().setText(connectionString);
                } else {
                    showAlert(AlertType.INFORMATION, "Внимание", "В буфере обмена не найден путь к каталогу!");
                }
            }
        });
        addressControl.getContextMenu().getItems().add(pasteDirectoryItem);

        MenuItem selectDirectoryItem = new MenuItem("Выбрать каталог");
        selectDirectoryItem.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Выберите каталог информационной базы");

            File selectedDirectory = directoryChooser.showDialog(addressComboBox.getScene().getWindow());
            if (selectedDirectory != null) {
                String escapedPath = selectedDirectory.getAbsolutePath().replace("\"", "\"\"");
                String connectionString = "File=\"" + escapedPath + "\"";
                addressComboBox.getEditor().setText(connectionString);
            }
        });
        addressControl.getContextMenu().getItems().add(selectDirectoryItem);

        // Добавляем пункт меню "Заметка..."
        MenuItem noteItem = new MenuItem("_Заметка...");
        noteItem.setOnAction(e -> NoteDialog.show(this));
        addressControl.getContextMenu().getItems().add(noteItem);

        // Добавляем пункт меню "Добавить в список баз"
        MenuItem addToDatabaseItem = new MenuItem("Добавить в список баз");
        addToDatabaseItem.setOnAction(e -> addCurrentAddressToDatabaseList());
        addressControl.getContextMenu().getItems().add(addToDatabaseItem);

        MenuItem refreshDatabaseItem = new MenuItem("Обновить список баз");
        refreshDatabaseItem.setOnAction(e -> refreshDatabaseList());
        addressControl.getContextMenu().getItems().add(refreshDatabaseItem);

        addressControl.getContextMenu().getItems().add(new SeparatorMenuItem());

        MenuItem copyCredsItem = new MenuItem("Копировать учётные данные");
        copyCredsItem.setOnAction(e -> copyCredentials());

        MenuItem pasteCredsItem = new MenuItem("Вставить учётные данные");
        pasteCredsItem.setOnAction(e -> pasteCredentials());

        MenuItem savedUsersItem = new MenuItem("Скопировать из ...");
        savedUsersItem.setOnAction(e -> SavedUsersDialog.show(this));
        addressControl.getContextMenu().getItems().addAll(copyCredsItem, pasteCredsItem, savedUsersItem);
    }

    /**
     * Обновляет отображение имени БД и заметки
     */
    public void updateBaseInfoDisplay(String address) {
        if (address == null) {
            address = "";
        }

        address = address.trim();

        // Отображаем имя базы
        if (!address.isEmpty()) {
            String baseName = registeredAddressMap.get(address);
            if (baseName != null) {
                baseNameLabel.setText("📋 Информационная база: " + baseName);
                baseNameLabel.setVisible(true);

                // Также отображаем имя в ComboBox
                addressControl.setAdressIB(baseName);
            } else {
                baseNameLabel.setVisible(false);
                addressControl.setAdressIB(null);
            }

            // Отображаем заметку
            try {
                String note = historyManager.getNote(address);
                if (note != null && !note.trim().isEmpty()) {
                    // Обрезаем длинную заметку для отображения
                    String displayNote = note.length() > 120 ? note.substring(0, 120) + "..." : note;
                    baseNoteLabel.setText("📝 " + displayNote);
                    baseNoteLabel.setVisible(true);
                } else {
                    baseNoteLabel.setVisible(false);
                }
            } catch (Exception e) {
                baseNoteLabel.setVisible(false);
            }
        } else {
            baseNameLabel.setVisible(false);
            baseNoteLabel.setVisible(false);
            addressControl.setAdressIB(null);
        }
    }

    /** Показывает окно "О программе". */
    private void showAboutDialog() {
        showAlert(Alert.AlertType.INFORMATION, AppConstants.ABOUT_TITLE,
                String.format(AppConstants.ABOUT_TEXT, AppConstants.VERSION));
    }

    private Button createHelpButton() {
        Button button = new Button("?");
        String linkColor = "#0563C1";

        button.setStyle(String.format("""
                -fx-background-color: transparent;
                -fx-text-fill: %s;
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-border-radius: 50%%;
                -fx-background-radius: 50%%;
                -fx-padding: 0""",
                linkColor, linkColor));

        button.setMinSize(16, 16);
        button.setPrefSize(16, 16);
        button.setMaxSize(16, 16);
        button.setFocusTraversable(false);

        button.setCursor(javafx.scene.Cursor.HAND);

        return button;
    }

    /**
     * Создаёт Tooltip со стандартным стилем
     */
    public Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle(AppConstants.TOOLTIP_STYLE);
        tooltip.setWrapText(true);
        return tooltip;
    }

    /**
     * Создаёт Tooltip со стандартным стилем и форматированием
     */
    private Tooltip createTooltip(String formatText, Object... args) {
        Tooltip tooltip = new Tooltip(String.format(formatText, args));
        tooltip.setStyle(AppConstants.TOOLTIP_STYLE);
        tooltip.setWrapText(true);
        return tooltip;
    }

    public void updateUserButtonState() {
        if (userCredentialsButton == null)
            return;
        String address = getCurrentAddress();
        UserCredentials cred = credentialsManager.get(address);
        if (cred != null && !cred.getUsername().isEmpty()) {
            userCredentialsButton.setUserData(AppConstants.COLOR_USER_HAS_CRED);
            userCredentialsButton.setStyle(userCredentialsButton.getStyle()
                    .replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + AppConstants.COLOR_USER_HAS_CRED));
            userCredentialsButton
                    .setTooltip(createTooltip(RunYBaseHelpTexts.USER_CRED_HAS_CRED_TOOLTIP, cred.getUsername()));
        } else {
            userCredentialsButton.setUserData(AppConstants.COLOR_USER_NO_CRED);
            userCredentialsButton.setStyle(userCredentialsButton.getStyle()
                    .replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + AppConstants.COLOR_USER_NO_CRED));
            userCredentialsButton.setTooltip(createTooltip(RunYBaseHelpTexts.USER_CRED_NO_CRED_TOOLTIP));
        }
    }

    // -----------------------------------------------------------------
    // Работа с цветами
    // -----------------------------------------------------------------

    /**
     * Увеличивает яркость цвета на заданный коэффициент
     * 
     * @param hexColor HEX цвет (например "#E6C878")
     * @param factor   коэффициент яркости (1.0 - без изменения, >1.0 - светлее,
     *                 <1.0 - темнее)
     * @return новый HEX цвет
     */
    private String adjustColorBrightness(String hexColor, double factor) {
        try {
            String hex = hexColor.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            r = Math.min(255, (int) (r * factor));
            g = Math.min(255, (int) (g * factor));
            b = Math.min(255, (int) (b * factor));

            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
    }


    private void askAndRunWithCredentials(String address, String baseCommand) {
        UserCredentials cred = credentialsManager.get(address);
        if (cred != null && !cred.getUsername().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Запуск с учётными данными");
            alert.setHeaderText(null);
            alert.setContentText("Запустить от имени пользователя:\n" + cred.getUsername() +
                    (cred.getPassword().isEmpty() ? "\n(без пароля)" : ""));

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    String cmd = baseCommand;
                    cmd += " /UserName \"" + cred.getUsername() + "\"";
                    if (!cred.getPassword().isEmpty()) {
                        cmd += " /Password \"" + cred.getPassword() + "\"";
                    }
                    runCommand(cmd, "с учётными данными");
                } else {
                    runCommand(baseCommand, "");
                }
            });
        } else {
            runCommand(baseCommand, "");
        }
    }

    /**
     * Запускает стандартную программу запуска 1С (1cestart.exe)
     */
    private void launchStandard1CStart() {
        try {
            List<String> paths = new ArrayList<>();
            paths.add("C:\\Program Files\\1cv8\\common\\1cestart.exe");
            paths.add("C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe");

            String foundPath = null;
            for (String path : paths) {
                if (Files.exists(Paths.get(path))) {
                    foundPath = path;
                    break;
                }
            }

            if (foundPath != null) {
                ProcessBuilder pb = new ProcessBuilder(foundPath);
                pb.start();
                showAutoClosingAlert("Стандартный запуск 1С запущен!\nПуть: " + foundPath,
                        "Запуск 1cestart", 3);
            } else {
                showAlert(Alert.AlertType.WARNING, "Предупреждение",
                        "Не найден файл 1cestart.exe!\n\nОжидаемые пути:\n" +
                                "C:\\Program Files\\1cv8\\common\\1cestart.exe\n" +
                                "C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe\n\n" +
                                "Убедитесь, что 1С:Предприятие 8 установлена на компьютере.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Ошибка запуска стандартного 1cestart.exe:\n" + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Работа с историей в XML (домашняя папка)
    // -----------------------------------------------------------------
    private void addToHistory(String address) {
        historyManager.addToHistory(address);
        addressComboBox.setValue(address);
    }

    private void addProcessingToHistory(String processingPath) {
        historyManager.addProcessingToHistory(processingPath);
        executeProcessingField.getComboBox().setValue(processingPath);
    }

    private void autoPasteFromClipboard() {
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String text = clipboard.getString();

                // Ищем в тексте строку подключения к 1С
                String extractedAddress = StringExtractor.extractConnectionString(text);

                if (extractedAddress != null) {
                    addressComboBox.setValue(extractedAddress);

                    // Поиск совпадения в списке зарегистрированных баз
                    try {
                        List<BaseEntry> baseEntries = loadAndSortDatabases();
                        for (BaseEntry base : baseEntries) {
                            if (base.connect != null && base.connect.equals(extractedAddress)) {
                                addressControl.setAdressIB(base.name);
                                showAlert(Alert.AlertType.INFORMATION, "Автовставка из буфера",
                                        "Обнаружен адрес базы 1С в буфере обмена!\n\nАвтоматически вставлено:\n"
                                                + extractedAddress +
                                                "\n\nНайдена база: " + base.name);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        // Если не удалось загрузить список баз, просто вставляем адрес
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Автовставка из буфера",
                            "Обнаружен адрес базы 1С в буфере обмена!\n\nАвтоматически вставлено:\n"
                                    + extractedAddress);
                    return;
                }
            }
            addressComboBox.setValue(null);
            addressComboBox.getEditor().setText("");
        } catch (Exception e) {
            addressComboBox.setValue(null);
            addressComboBox.getEditor().setText("");
        }
    }

    public String getCurrentAddress() {
        String text = addressComboBox.getEditor().getText();
        return text == null ? "" : text.trim();
    }

    private String getCommandPart() {
        if (designerRadio.isSelected() || updateConfigRadio.isSelected())
            return "DESIGNER";
        if (thinRadio.isSelected())
            return "ENTERPRISE";
        if (thickOrdinaryRadio.isSelected())
            return "ENTERPRISE /RunModeOrdinaryApplication";
        return "ENTERPRISE /RunModeManagedApplication";
    }

    /**
     * Формирует строку запуска для указанной разрядности платформы
     * 
     * @param platformPath путь к exe файлу (с учётом разрядности)
     * @param appArch      значение параметра /AppArch (x86 или x86_64)
     * @param escaped      экранированный адрес базы
     * @param commandPart  часть команды (DESIGNER или ENTERPRISE ...)
     * @param cred         учётные данные пользователя (могут быть null)
     * @param isDesigner   запущен ли Конфигуратор
     * @param isUpdate     нужна реструктуризация
     * @return сформированная строка запуска
     */
     private String buildCommand(String platformPath, String appArch, String escaped,
        String commandPart, UserCredentials cred, boolean isDesigner, boolean isUpdate) {
        return buildCommandWithAddressParam(platformPath, appArch,
                "/IBConnectionString \"" + escaped + "\"",
                commandPart, cred, isDesigner, isUpdate);
    }

    /**
     * Формирует строку запуска с произвольным указанием адреса базы
     * (например, /IBConnectionString "...", /IBName "..." или /S "сервер\база").
     *
     * @param platformPath путь к exe файлу (с учётом разрядности)
     * @param appArch      значение параметра /AppArch (x86 или x86_64)
     * @param addressParam готовый параметр адреса базы
     * @param commandPart  часть команды (DESIGNER или ENTERPRISE ...)
     * @param cred         учётные данные пользователя (могут быть null)
     * @param isDesigner   запущен ли Конфигуратор
     * @param isUpdate     нужна реструктуризация
     * @return сформированная строка запуска
     */
     private String buildCommandWithAddressParam(String platformPath, String appArch, String addressParam,
        String commandPart, UserCredentials cred, boolean isDesigner, boolean isUpdate) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("\"").append(platformPath).append("\" ");
        cmd.append(commandPart).append(" ");
        cmd.append(addressParam);

        if (cred != null && !cred.getUsername().isEmpty()) {
            cmd.append(" /N \"").append(cred.getUsername()).append("\"");
            if (!cred.getPassword().isEmpty()) {
                cmd.append(" /P \"").append(cred.getPassword()).append("\"");
            }
        }

        if (priorityPlatformCheckbox.isSelected()) {
            cmd.append(" /AppArch ").append(appArch);
        }

        // Режим отладки игнорируется для Конфигуратора и Реструктуризации
        if (debugModeCheckbox.isSelected() && !isDesigner && !isUpdate) {
            String debugParam = " /Debug -attach";
            String protocol = debugProtocolCombo.getValue();
            if (protocol != null && !"по умолчанию".equals(protocol)) {
                debugParam += " " + protocol;
            }
            cmd.append(debugParam);
        }

        if (clearCacheCheckbox.isSelected()) {
            cmd.append(" /ClearCache");
        }
        if (isUpdate) {
                cmd.append(" /UpdateDBCfg -v2 -Server");}

        if (executeProcessingCheckbox.isSelected()) {
            String processingPath = executeProcessingField.getText();
            if (processingPath != null && !processingPath.trim().isEmpty()) {
                cmd.append(" /Execute \"").append(processingPath.trim()).append("\"");
            }
        }

        return cmd.toString();
    }

    /**
     * Формирует строку запуска с использованием /IBName (по имени информационной базы)
     * 
     * @param platformPath путь к exe файлу
     * @param appArch      значение /AppArch
     * @param ibName       экранированное имя базы в кавычках
     * @param commandPart  часть команды (DESIGNER или ENTERPRISE ...)
     * @param cred         учётные данные пользователя
     * @param isDesigner   запущен ли Конфигуратор
     * @param isUpdate     нужна реструктуризация
     * @return сформированная строка запуска
     */
    private String buildCommandWithIBName(String platformPath, String appArch, String ibName,
            String commandPart, UserCredentials cred, boolean isDesigner, boolean isUpdate) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("\"").append(platformPath).append("\" ");
        cmd.append(commandPart).append(" ");
        cmd.append("/IBName ").append(ibName);

        if (cred != null && !cred.getUsername().isEmpty()) {
            cmd.append(" /N \"").append(cred.getUsername()).append("\"");
            if (!cred.getPassword().isEmpty()) {
                cmd.append(" /P \"").append(cred.getPassword()).append("\"");
            }
        }

        if (priorityPlatformCheckbox.isSelected()) {
            cmd.append(" /AppArch ").append(appArch);
        }

        // Режим отладки игнорируется для Конфигуратора и Реструктуризации
        if (debugModeCheckbox.isSelected() && !isDesigner && !isUpdate) {
            String debugParam = " /Debug -attach";
            String protocol = debugProtocolCombo.getValue();
            if (protocol != null && !"по умолчанию".equals(protocol)) {
                debugParam += " " + protocol;
            }
            cmd.append(debugParam);
        }

        if (clearCacheCheckbox.isSelected()) {
            cmd.append(" /ClearCache");
        }
        if (isUpdate) {
            cmd.append(" /UpdateDBCfg -v2 -Server");
        }

        if (executeProcessingCheckbox.isSelected()) {
            String processingPath = executeProcessingField.getText();
            if (processingPath != null && !processingPath.trim().isEmpty()) {
                cmd.append(" /Execute \"").append(processingPath.trim()).append("\"");
            }
        }

        return cmd.toString();
    }

    private void handleGenerateCommand() {
        handleGenerateCommand(true);
    }

    /**
     * Формирует строку запуска без данных пользователя (/N, /P),
     * если для текущей базы сохранены учётные данные.
     */
    private void handleGenerateCommandWithoutUserData() {
        handleGenerateCommand(false);
    }

    /**
     * Основное действие «Сформировать».
     *
     * @param includeCredentials включать ли сохранённые учётные данные (/N, /P) в строку запуска
     */
    private void handleGenerateCommand(boolean includeCredentials) {
        String text = getCurrentAddress();
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", RunYBaseHelpTexts.WARNING_NO_ADDRESS);
            return;
        }

        if (!designerRadio.isSelected() && executeProcessingCheckbox.isSelected()) {
            String processingPath = executeProcessingField.getText();
            if (processingPath == null || processingPath.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Укажите путь к внешней обработке (.bfsl)!");
                return;
            }
            addProcessingToHistory(processingPath);
        }

        UserCredentials cred = credentialsManager.get(text);

        // Вариант «без данных пользователя»: исключаем /N и /P из команды.
        // Если сохранённых данных нет — сообщим, что строка и так сформирована без них.
        if (!includeCredentials) {
            if (cred == null || cred.getUsername().isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Информация",
                        String.format(RunYBaseHelpTexts.INFO_NO_USER_DATA, text));
            }
            cred = null;
        }

        addToHistory(text);

        String commandPart = getCommandPart();
        outputArea86.setText("");
        outputArea.setText("");

        String escaped = text.replace("\"", "\"\"");

        boolean isDesigner = designerRadio.isSelected();
        boolean isUpdate = updateConfigRadio.isSelected();

        String cmd86 = buildCommand(
                "C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe",
                "x86",
                escaped,
                commandPart,
                cred,
                isDesigner,
                isUpdate);

        String cmd64 = buildCommand(
                "C:\\Program Files\\1cv8\\common\\1cestart.exe",
                "x86_64",
                escaped,
                commandPart,
                cred,
                isDesigner,
                isUpdate);

        outputArea86.setText(cmd86);
        outputArea.setText(cmd64);

        if (debugArea != null && SHOW_DEBUG_PANEL)
            debugArea.setText("");
    }

    /**
     * Формирует команду запуска по имени информационной базы (/IBName)
     */
    private void handleGenerateByIB() {
        String baseAddress = getCurrentAddress();
        if (baseAddress.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Введите имя информационной базы!");
            return;
        }

        try {
            List<BaseEntry> allBases = loadAndSortDatabases();

            // Ищем все базы с указанным адресом
            List<BaseEntry> matches = new ArrayList<>();
            for (BaseEntry entry : allBases) {
                if (entry.connect != null && entry.connect.equals(baseAddress)) {
                    matches.add(entry);
                }
            }

            if (matches.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение",
                        "Информационная база с адресом «" + baseAddress + "» не найдена в списке!");
                return;
            }

            if (matches.size() > 1) {
                StringBuilder msg = new StringBuilder();
                msg.append("Найдено ").append(matches.size()).append(" баз с адресом «").append(baseAddress).append("»:\n\n");
                for (int i = 0; i < matches.size(); i++) {
                    msg.append(i + 1).append(". ").append(matches.get(i).name).append("\n");
                    msg.append("   Адрес: ").append(matches.get(i).connect).append("\n\n");
                }
                msg.append("Уточните имя базы или используйте адрес напрямую.");
                showAlert(Alert.AlertType.ERROR, "Ошибка", msg.toString());
                return;
            }

        // Одна база найдена — проверяем, уникально ли её имя
        String foundName = matches.get(0).name;
        List<BaseEntry> sameNameBases = new ArrayList<>();
        for (BaseEntry entry : allBases) {
            if (entry.name != null && entry.name.equals(foundName)) {
                sameNameBases.add(entry);
            }
        }

        if (sameNameBases.size() > 1) {
            // Хотя 1С скорее всего выдаст ошибку не будем ограничивать пользователя, пусть стрка все таки сформируется 
            StringBuilder msg = new StringBuilder();
            msg.append("Имя базы «").append(foundName).append("» не уникально.\n")
               .append("Найдено ").append(sameNameBases.size()).append(" баз с таким именем:\n\n");
            for (int i = 0; i < sameNameBases.size(); i++) {
                msg.append(i + 1).append(". ").append(sameNameBases.get(i).name).append("\n");
                msg.append("   Адрес: ").append(sameNameBases.get(i).connect).append("\n\n");
            }
            msg.append("Запуск по /IBName невозможен: 1С выдаст ошибку при неоднозначном имени.");
            showAlert(Alert.AlertType.INFORMATION, "Ошибка", msg.toString());
            //return;
        }
            // Имя базы найдено — формируем команду с /IBName
            BaseEntry base = matches.get(0);
            String escapedName = base.name.replace("\"", "\"\"");
            String ibNameParam = "\"" + escapedName + "\"";

            String commandPart = getCommandPart();
            outputArea86.setText("");
            outputArea.setText("");

            UserCredentials cred = credentialsManager.get(base.connect);

            boolean isDesigner = designerRadio.isSelected();
            boolean isUpdate = updateConfigRadio.isSelected();

            String cmd86 = buildCommandWithIBName(
                    "C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe",
                    "x86",
                    ibNameParam,
                    commandPart,
                    cred,
                    isDesigner,
                    isUpdate);

            String cmd64 = buildCommandWithIBName(
                    "C:\\Program Files\\1cv8\\common\\1cestart.exe",
                    "x86_64",
                    ibNameParam,
                    commandPart,
                    cred,
                    isDesigner,
                    isUpdate);

            outputArea86.setText(cmd86);
            outputArea.setText(cmd64);

            showAutoClosingAlert("Команда сформирована по имени базы «" + base.name + "»", "Готово", 2);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Ошибка при поиске базы:\n" + e.getMessage());
        }
    }
    /**
     * Формирует команду запуска с кратким указанием адреса базы:
     * - /S "сервер\база" — для серверной базы;
     * - /F "каталог"     — для файловой базы (путь без имени файла 1Cv8.1CD).
     * Тип базы и адрес определяются по содержимому поля адреса.
     */
    private void handleGenerateByShortAddress() {
        String text = getCurrentAddress();
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", RunYBaseHelpTexts.WARNING_NO_ADDRESS);
            return;
        }

        String addressParam = buildShortAddressParam(text);
        if (addressParam == null) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение",
                    AppConstants.SHORT_ADDRESS_NOT_RECOGNIZED_WARNING);
            return;
        }

        // Учётные данные ищутся по исходному адресу, как в основном режиме
        UserCredentials cred = credentialsManager.get(text);

        addToHistory(text);

        String commandPart = getCommandPart();
        outputArea86.setText("");
        outputArea.setText("");

        boolean isDesigner = designerRadio.isSelected();
        boolean isUpdate = updateConfigRadio.isSelected();

        String cmd86 = buildCommandWithAddressParam(
                "C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe",
                "x86",
                addressParam,
                commandPart,
                cred,
                isDesigner,
                isUpdate);

        String cmd64 = buildCommandWithAddressParam(
                "C:\\Program Files\\1cv8\\common\\1cestart.exe",
                "x86_64",
                addressParam,
                commandPart,
                cred,
                isDesigner,
                isUpdate);

        outputArea86.setText(cmd86);
        outputArea.setText(cmd64);

        showAutoClosingAlert("Адрес указан как «" + addressParam + "»", "Готово", 2);
    }

    /**
     * Формирует краткий параметр адреса базы:
     * - /S "сервер\база" — для серверной базы;
     * - /F "каталог"     — для файловой базы.
     *
     * Поддерживаемые форматы адреса:
     * - серверная: Srvr="сервер";Ref="база", сервер\база (в том числе с префиксом /S);
     * - файловая:  File="каталог", путь к каталогу (локальный или UNC).
     *
     * @return готовый параметр адреса или null, если формат не распознан
     */
    private String buildShortAddressParam(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty())
            return null;

        // Файловая база, заданная строкой подключения File="..."
        java.util.regex.Matcher fileM = java.util.regex.Pattern
                .compile("File\\s*=\\s*\"([^\"]*)\"").matcher(t);
        if (fileM.find()) {
            String dir = fileM.group(1).trim().replace('/', '\\');
            // Если указан файл базы 1Cv8.1CD — оставляем только каталог
            if (dir.toLowerCase(java.util.Locale.ROOT).endsWith("\\1cv8.1cd")) {
                dir = dir.substring(0, dir.length() - "\\1cv8.1cd".length());
            }
            if (dir.isEmpty())
                return null;
            return "/F \"" + dir.replace("\"", "\"\"") + "\"";
        }

        // Файловая база — путь к каталогу (локальный C:\... или UNC \\сервер\папка)
        String dir = StringExtractor.extractDirectoryPath(t);
        if (dir != null && !dir.isEmpty())
            return "/F \"" + dir.replace("\"", "\"\"") + "\"";

        // Серверная база — Srvr="...";Ref="..." или сервер\база
        String[] serverBase = extractServerAndBase(t);
        if (serverBase != null && !serverBase[0].isEmpty() && !serverBase[1].isEmpty()) {
            String serverAddress = serverBase[0] + "\\" + serverBase[1];
            return "/S \"" + serverAddress.replace("\"", "\"\"") + "\"";
        }

        return null;
    }

    /**
     * Извлекает сервер и имя базы из адреса.
     * Поддерживаемые форматы:
     * - строка подключения: Srvr="сервер";Ref="база";
     * - адрес вида сервер\база (в том числе с префиксом /S).
     *
     * @return массив {сервер, база} или null, если формат не распознан
     */
    private String[] extractServerAndBase(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty())
            return null;

        // Убираем возможный префикс /S
        if (t.length() > 2 && t.charAt(0) == '/' && (t.charAt(1) == 'S' || t.charAt(1) == 's')) {
            t = t.substring(2).trim();
        }
        // Убираем обрамляющие кавычки
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1).trim();
        }

        // Строка подключения: Srvr="сервер";Ref="база";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Srvr\\s*=\\s*\"([^\"]*)\"\\s*;\\s*Ref\\s*=\\s*\"([^\"]*)\"")
                .matcher(t);
        if (m.find())
            return new String[] { m.group(1).trim(), m.group(2).trim() };

        // Адрес вида сервер\база (UNC-пути \\сервер\папка — это файловые базы, не подходит)
        int slashIdx = t.indexOf('\\');
        if (slashIdx > 0 && !t.startsWith("\\\\")) {
            return new String[] { t.substring(0, slashIdx).trim(), t.substring(slashIdx + 1).trim() };
        }

        return null;
    }

    /**
     * Параметры командной строки 1С, после которых идёт значение.
     * Значения пропускаются при разборе, чтобы имя базы, совпадающее
     * с DESIGNER/ENTERPRISE, не было принято за режим запуска.
     */
    private static final java.util.Set<String> MODE_VALUE_PARAMS = new java.util.HashSet<>(
            java.util.Arrays.asList("/IBCONNECTIONSTRING", "/IBNAME", "/N", "/P", "/EXECUTE",
                    "/DRID", "/L", "/S", "/AP", "/OUT", "/DEBUG", "/FRATESTER"));

    /**
     * Определяет режим запуска по содержимому строки команды,
     * а не по выбранным RadioButton-ам. Это гарантирует, что режим
     * соответствует именно той команде, которая будет запущена
     * (строка могла быть сформирована ранее или отредактирована вручную).
     *
     * @param command полная строка команды из поля вывода
     * @return человекочитаемое название режима, либо "" если распознать не удалось
     */
    private String getRunModeFromCommand(String command) {
        if (command == null || command.trim().isEmpty())
            return "";

        List<String> args = parseCommand(command);
        String mode = "";
        boolean runOrdinary = false;
        boolean runManaged = false;
        boolean updateCfg = false;

        // args.get(0) — путь к 1cestart.exe, режим ищется после него
        for (int i = 1; i < args.size(); i++) {
            String token = args.get(i).toUpperCase();
            if (MODE_VALUE_PARAMS.contains(token)) {
                i++; // пропускаем значение параметра
                continue;
            }
            if (token.equals("DESIGNER"))
                mode = "DESIGNER";
            else if (token.equals("ENTERPRISE"))
                mode = "ENTERPRISE";
            else if (token.startsWith("/RUNMODEORDINARYAPPLICATION"))
                runOrdinary = true;
            else if (token.startsWith("/RUNMODEMANAGEDAPPLICATION"))
                runManaged = true;
            else if (token.startsWith("/UPDATEDBCFG"))
                updateCfg = true;
        }

        if (updateCfg)
            return "Обновление конфигурации";
        if ("DESIGNER".equals(mode))
            return "Конфигуратор";
        if ("ENTERPRISE".equals(mode)) {
            if (runOrdinary)
                return "Толстый клиент (Обычное)";
            if (runManaged)
                return "Толстый клиент (Управляемое)";
            return "Тонкий клиент";
        }
        return "";
    }

    private void runCommand(String command, String platform) {
        if (command == null || command.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Нет команды для запуска!");
            return;
        }
        // Режим определяется по самой команде, а не по выбранным радиокнопкам
        String mode = getRunModeFromCommand(command);
        if (mode.isEmpty())
            mode = "Клиент 1С";
        if (SHOW_DEBUG_PANEL && debugArea != null) {
            debugArea.appendText("=== Запуск (" + platform + ") ===\nКоманда: " + command + "\n");
        }
        try {
            List<String> args = parseCommand(command);
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                output.append(line).append("\n");

            boolean finished = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);

            if (finished) {
                int code = process.exitValue();
                if (code == 0) {
                    if (SHOW_RUN_MESSAGE) {
                        showAutoClosingAlert(
                                mode + " успешно запущен!\nИнформационная база: " + getCurrentAddress() + "\nПлатформа: " + platform,
                                "Запуск 1С", 5);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка запуска " + mode + "!\nКод: " + code);
                }
            } else {
                if (SHOW_RUN_MESSAGE) {
                    showAutoClosingAlert(mode + " запущен (фоновый процесс).\nИнформационная база: " + getCurrentAddress(),
                            "Запуск 1С", 5);
                }
            }
            if (SHOW_DEBUG_PANEL && debugArea != null)
                debugArea.appendText("=== Конец запуска ===\n\n");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка запуска: " + e.getMessage());
            if (SHOW_DEBUG_PANEL && debugArea != null)
                debugArea.appendText("Исключение: " + e + "\n");
        }
    }

    /**
     * Показывает короткое уведомление в правом нижнем углу главного окна.
     * Popup вместо Alert: уведомление немодальное, не перехватывает фокус
     * и закрывается само по истечении delaySeconds (или по клику).
     */
    public void showAutoClosingAlert(String message, String title, int delaySeconds) {
        showAutoClosingAlert(message, title, delaySeconds, null);
    }

    /**
     * Показывает короткое уведомление в правом нижнем углу указанного окна.
     * Нужно, когда действие выполнено из дочернего окна (например, копирование
     * из окна истории) — иначе уведомление появляется за активным окном
     * и пользователь не видит подтверждения.
     *
     * @param owner окно, относительно которого позиционируется уведомление;
     *              null — главное окно приложения
     */
    public void showAutoClosingAlert(String message, String title, int delaySeconds, Window owner) {
        Window anchor = (owner != null) ? owner : mainStage;
        if (anchor == null) {
            return;
        }

        // Прежнее уведомление скрываем, иначе они накладываются друг на друга
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + AppConstants.COLOR_TEXT_FG + ";");

        Label textLabel = new Label(message);
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(420);
        textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppConstants.COLOR_TEXT_FG + ";");

        Label countdownLabel = new Label();
        countdownLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppConstants.COLOR_ACCENT + ";");

        HBox header = new HBox(10, titleLabel, countdownLabel);
        header.setAlignment(Pos.BASELINE_LEFT);

        VBox content = new VBox(4, header, textLabel);
        content.setPadding(new Insets(8, 12, 8, 12));
        content.setStyle("-fx-background-color: " + AppConstants.COLOR_BUTTON_SMALL_BG
                + "; -fx-border-color: " + AppConstants.COLOR_BUTTON_BORDER
                + "; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Popup popup = new Popup();
        popup.getContent().add(content);
        // Клик по уведомлению закрывает его досрочно
        content.setOnMouseClicked(e -> popup.hide());
        activePopup = popup;

        // #region Обратный отсчёт в заголовке: оставшиеся секунды до автозакрытия
        final int[] remaining = {Math.max(1, delaySeconds)};
        countdownLabel.setText("(" + remaining[0] + ")");

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                popup.hide();
            } else {
                countdownLabel.setText("(" + remaining[0] + ")");
            }
        }));
        countdown.setCycleCount(remaining[0]);
        // Останавливаем таймер, если уведомление закрыл пользователь (клик)
        popup.setOnHidden(e -> {
            countdown.stop();
            if (activePopup == popup) {
                activePopup = null;
            }
        });
        // #endregion

        // Размеры нужны до показа, иначе уведомление мелькнет в левом верхнем углу
        content.applyCss();
        content.autosize();

        // Окно могло быть свёрнуто/не иметь размеров — берём координаты экрана
        double anchorRight = anchor.getX() + Math.max(anchor.getWidth(), 0);
        double anchorBottom = anchor.getY() + Math.max(anchor.getHeight(), 0);
        if (anchor.getWidth() <= 0 || anchor.getHeight() <= 0) {
            javafx.geometry.Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            anchorRight = vb.getMaxX();
            anchorBottom = vb.getMaxY();
        }
        double x = anchorRight - content.getWidth() - 20;
        double y = anchorBottom - content.getHeight() - 20;
        popup.show(anchor, x, y);
        countdown.play();
    }

    private static List<String> parseCommand(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ' ' && !inQuotes) {
                if (cur.length() > 0) {
                    args.add(cur.toString());
                    cur.setLength(0);
                }
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0)
            args.add(cur.toString());
        for (int i = 0; i < args.size(); i++) {
            String a = args.get(i);
            if (a.startsWith("\"") && a.endsWith("\"") && a.length() > 1)
                args.set(i, a.substring(1, a.length() - 1));
        }
        return args;
    }

    /**
     * Назначает полю ввода стандартное контекстное меню редактирования
     * ("Вставить", "Вырезать", "Копировать", "Выделить всё").
     * Без него у TextInputControl меню по умолчанию отсутствует.
     */
    private void installStandardEditContextMenu(TextInputControl control) {
        ContextMenu menu = new ContextMenu();
        MenuItem paste = new MenuItem("Вставить");
        paste.setOnAction(e -> control.paste());
        paste.setAccelerator(KeyCombination.keyCombination("Shortcut+V"));
        MenuItem cut = new MenuItem("Вырезать");
        cut.setOnAction(e -> control.cut());
        cut.setAccelerator(KeyCombination.keyCombination("Shortcut+X"));
        MenuItem copy = new MenuItem("Копировать");
        copy.setOnAction(e -> control.copy());
        copy.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        MenuItem selectAll = new MenuItem("Выделить всё");
        selectAll.setOnAction(e -> control.selectAll());
        selectAll.setAccelerator(KeyCombination.keyCombination("Shortcut+A"));
        menu.getItems().addAll(paste, cut, copy, new javafx.scene.control.SeparatorMenuItem(), selectAll);
        control.setContextMenu(menu);
    }

    /**
     * Копирует текст в буфер обмена, показывает подтверждение
     * относительно главного окна.
     */
    private void copyToClipboard(String text) {
        copyToClipboard(text, null);
    }

    /**
     * Копирует текст в буфер обмена и показывает подтверждение
     * относительно указанного окна.
     *
     * @param owner окно-владелец уведомления (null — главное окно)
     */
    public void copyToClipboard(String text, Window owner) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            showAutoClosingAlert("Скопировано в буфер обмена", "Копирование", 2, owner);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText("Не удалось скопировать текст:\n" + e.getMessage());
            Stage ownerStage = (owner instanceof Stage) ? (Stage) owner : mainStage;
            if (ownerStage != null) {
                alert.initOwner(ownerStage);
            }
            alert.showAndWait();
        }
    }

    public static List<BaseEntry> loadAndSortDatabases()
            throws IOException, ParserConfigurationException, SAXException, Exception {
        String userHome = System.getProperty("user.home");
        Path ibasesPath = Paths.get(userHome, "AppData", "Roaming", "1C", "1CEStart", "ibases.v8i");

        if (!Files.exists(ibasesPath)) {
            showAlert(Alert.AlertType.ERROR, "Ошибка",
                    "Файл списка баз не найден:\n" + ibasesPath.toString());
            return new ArrayList<>();
        }

        List<BaseEntry> baseEntries = new ArrayList<>();

        String content = new String(Files.readAllBytes(ibasesPath), StandardCharsets.UTF_8);

        if (content.trim().startsWith("<?xml") || content.contains("<infobase>")) {
            parseXmlFormatWithOrder(ibasesPath, baseEntries);
        } else {
            parseIniFormatWithOrder(ibasesPath, baseEntries);
        }

        baseEntries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return baseEntries;
    }


    /**
     * Удаляет BOM (U+FEFF) из начала строки, если он присутствует
     */
    private static String stripBom(String line) {
        return (line != null && line.startsWith("\uFEFF")) ? line.substring(1) : line;
    }

    private static void parseIniFormatWithOrder(Path path, List<BaseEntry> entries) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        // Первая строка файла может начинаться с BOM (U+FEFF) — убираем его,
        // иначе заголовок первой секции "[Имя базы]" не будет распознан
        if (!lines.isEmpty()) {
            lines.set(0, stripBom(lines.get(0)));
        }

        String currentName = null;
        String currentConnect = null;
        double currentOrder = Double.MAX_VALUE;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                if (currentName != null && currentConnect != null) {
                    entries.add(new BaseEntry(currentName, currentConnect, currentOrder));
                }
                currentName = line.substring(1, line.length() - 1);
                currentConnect = null;
                currentOrder = Double.MAX_VALUE;
            } else if (line.startsWith("Connect=") && currentName != null) {
                currentConnect = line.substring(8);
            } else if (line.startsWith("OrderInList=") && currentName != null) {
                try {
                    currentOrder = Double.parseDouble(line.substring(12));
                } catch (NumberFormatException e) {
                    currentOrder = Double.MAX_VALUE;
                }
            }
        }

        if (currentName != null && currentConnect != null) {
            entries.add(new BaseEntry(currentName, currentConnect, currentOrder));
        }
    }

    private static void parseXmlFormatWithOrder(Path path, List<BaseEntry> entries) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(path.toFile());

        NodeList infos = doc.getElementsByTagName("infobase");
        for (int i = 0; i < infos.getLength(); i++) {
            Element info = (Element) infos.item(i);
            String name = getTagValue("name", info);
            String connect = getTagValue("connect", info);
            double order = Double.MAX_VALUE;

            NodeList orderNodes = info.getElementsByTagName("orderInList");
            if (orderNodes.getLength() > 0) {
                try {
                    order = Double.parseDouble(orderNodes.item(0).getTextContent());
                } catch (NumberFormatException e) {
                    order = Double.MAX_VALUE;
                }
            }

            if (name != null && !name.isEmpty() && connect != null && !connect.isEmpty()) {
                entries.add(new BaseEntry(name, connect, order));
            }
        }
    }

    private static String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list.getLength() == 0)
            return null;
        return list.item(0).getTextContent();
    }

    // -----------------------------------------------------------------
    // Доступ для классов-диалогов
    // -----------------------------------------------------------------

    public CredentialsManager getCredentialsManager() {
        return credentialsManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public Stage getMainStage() {
        return mainStage;
    }

    public Map<String, String> getRegisteredAddressMap() {
        return registeredAddressMap;
    }

    public ComboBox<String> getAddressComboBox() {
        return addressComboBox;
    }

    public ComboBoxWithMenuButton<String> getAddressControl() {
        return addressControl;
    }

    /**
     * Показывает модальное окно. Безопасен для вызова из любого потока:
     * если вызов пришёл не из потока JavaFX, окно ставится в очередь Platform.runLater.
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(type, title, message));
            return;
        }

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

// ========== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ==========

class BaseEntry {
    String name;
    String connect;
    double order;
    String note;

    BaseEntry(String name, String connect, double order) {
        this.name = name;
        this.connect = connect;
        this.order = order;
    }

    void setNote(String note) {
        this.note = note;
    }
}

class BaseEntryListCell extends ListCell<BaseEntry> {
    private final Label nameLabel = new Label();
    private final Label connectLabel = new Label();
    private final Label noteLabel = new Label();
    private final VBox container = new VBox(2, nameLabel, connectLabel, noteLabel);

    public BaseEntryListCell() {
        container.setPadding(new Insets(5, 10, 5, 10));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        connectLabel.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");
        connectLabel.setPadding(new Insets(0, 0, 0, 20));
        noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #C8A046; -fx-font-style: italic;");
        noteLabel.setPadding(new Insets(0, 0, 0, 20));
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(500);
    }

    @Override
    protected void updateItem(BaseEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(item.name);
            connectLabel.setText(item.connect);
            if (item.note != null && !item.note.isEmpty()) {
                // Обрезаем длинную заметку для отображения
                String displayNote = item.note.length() > 80 ? item.note.substring(0, 80) + "..." : item.note;
                noteLabel.setText("📝 " + displayNote);
                noteLabel.setVisible(true);
            } else {
                noteLabel.setVisible(false);
            }
            if (isSelected()) {
                container.setStyle("-fx-background-color: -fx-selection-bar;");
                nameLabel.setTextFill(Color.WHITE);
                connectLabel.setTextFill(Color.WHITE);
                noteLabel.setTextFill(Color.WHITE);
            } else {
                container.setStyle("-fx-background-color: transparent;");
                nameLabel.setTextFill(Color.BLACK);
                connectLabel.setTextFill(Color.BLACK);
                noteLabel.setTextFill(Color.GRAY);
            }
            setGraphic(container);
        }
    }
}

class SavedUserEntry {
    final String username;
    final String password;
    final String address;
    final String baseName;
    final boolean forCurrentAddress;

    SavedUserEntry(String username, String password, String address, String baseName, boolean forCurrentAddress) {
        this.username = username;
        this.password = password;
        this.address = address;
        this.baseName = baseName;
        this.forCurrentAddress = forCurrentAddress;
    }
}

class SavedUserListCell extends ListCell<SavedUserEntry> {
    private final Label userLabel = new Label();
    private final Label addressLabel = new Label();
    private final VBox container = new VBox(2, userLabel, addressLabel);

    public SavedUserListCell() {
        container.setPadding(new Insets(5, 10, 5, 10));
        userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        addressLabel.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");
        addressLabel.setPadding(new Insets(0, 0, 0, 20));
        addressLabel.setWrapText(true);
        addressLabel.setMaxWidth(600);
    }

    @Override
    protected void updateItem(SavedUserEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        String mark = item.forCurrentAddress ? "  (текущая база)" : "";
        userLabel.setText("👤 " + item.username + mark + (item.password.isEmpty() ? "" : "  🔑"));

        String location = item.baseName != null ? item.baseName + " — " + item.address : item.address;
        addressLabel.setText(location);

        if (isSelected()) {
            container.setStyle("-fx-background-color: -fx-selection-bar;");
            userLabel.setTextFill(Color.WHITE);
            addressLabel.setTextFill(Color.WHITE);
        } else {
            container.setStyle("-fx-background-color: transparent;");
            userLabel.setTextFill(Color.BLACK);
            addressLabel.setTextFill(Color.BLACK);
        }
        setGraphic(container);
    }
}

class UserCredentials {
    private String username;
    private String password;

    public UserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
