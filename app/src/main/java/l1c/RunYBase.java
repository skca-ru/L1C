package l1c;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import javafx.beans.value.*;
import javafx.event.*;
import javafx.util.Duration;
import javafx.scene.input.*;
import javafx.scene.text.Text;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

public class RunYBase extends Application {

    // @formatter:off
    // #region ========== НАСТРОЙКИ ==========
    private static final boolean SHOW_DEBUG_PANEL    = false;
    private static final boolean SHOW_RUN_MESSAGE    = true;
    // #endregion =================================

    // #region ========== ЦВЕТА 1С (белый фон + приглушённые жёлтые акценты) ==========
    private static final String COLOR_BG              = "#FFFFFF";
    private static final String COLOR_BUTTON_BG       = "#E6C878";
    private static final String COLOR_BUTTON_SMALL_BG = "#F3E4BC";
    private static final String COLOR_BUTTON_FG       = "#000000";
    private static final String COLOR_BUTTON_BORDER   = "#C0A050";
    private static final String COLOR_ACCENT          = "#C8A046";
    private static final String COLOR_TEXT_FG         = "#000000";
    private static final String COLOR_INPUT_BG        = "#FFFFFF";
    private static final String COLOR_OUTPUT_BG       = "#FAFAFA";
    private static final String COLOR_PANEL_BG        = "#FFFFFF";
    private static final String COLOR_USER_HAS_CRED   = "#B2DAB2";
    private static final String COLOR_USER_NO_CRED    = "#F3E4BC";
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

    // Label для отображения имени БД и заметки под полем адреса
    private Label baseNameLabel;
    private Label baseNoteLabel;

    // Для проверки есть адрес в списке зарегистрированных баз
    private Map<String, String> registeredAddressMap = new HashMap<>();

    // Контейнер для всех элементов интерфейса
    private VBox contentBox;

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
        borderRoot.setStyle("-fx-background-color: " + COLOR_BG + ";");
        borderRoot.setTop(menuBar);

        // Все остальные элементы помещаем в VBox с отступами
        contentBox = new VBox(8);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: " + COLOR_BG + ";");

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
                    "-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_OUTPUT_BG + ";");
            debugArea.setStyle(debugArea.getStyle() + "-fx-border-color: " + COLOR_ACCENT + ";");
            contentBox.getChildren().add(debugArea);
        } else {
            debugArea = new TextArea();
        }

        borderRoot.setCenter(contentBox);

        Scene scene = new Scene(borderRoot, 1050, SHOW_DEBUG_PANEL ? 700 : 500);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                // saveHistoryToXml();
                Platform.exit();
            }

        });

        primaryStage.setTitle(
                "Построитель команды запуска 1С - Примеры: File=\"C:\\1C\\Base\";  или  Srvr=\"127.0.0.1\";Ref=\"Base\";");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            // saveHistoryToXml();
        });
        primaryStage.show();

        addContextMenu(addressComboBox.getEditor());
        addContextMenu(outputArea);
        addContextMenu(outputArea86);
        if (debugArea != null)
            addContextMenu(debugArea);

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
 * Добавляет текущий адрес из поля ввода в список зарегистрированных баз (ibases.v8i)
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

    private Button createButton(String text) {
        return createButton(text, COLOR_BUTTON_BG);
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
                if (val > maxOrder) maxOrder = val;
            } catch (NumberFormatException ignored) {}
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
                bgColor, COLOR_BUTTON_FG, COLOR_BUTTON_BORDER));

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
                    adjustColorBrightness(baseColor, 1.1), COLOR_BUTTON_FG, COLOR_BUTTON_BORDER));
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
                    baseColor, COLOR_BUTTON_FG, COLOR_BUTTON_BORDER));
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

    private Button createFlatButton(String text) {
        Button button = new Button(text);

        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 12px;
                -fx-border-color: gray;
                -fx-border-radius: 3px;
                -fx-background-radius: 3px""",
                COLOR_INPUT_BG, COLOR_TEXT_FG));
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
        copyButton.setStyle(copyButton.getStyle() + "-fx-background-color: " + COLOR_BUTTON_SMALL_BG + ";");
        copyButton.setMinWidth(80);
        copyButton.setPrefWidth(80);
        copyButton.setMaxWidth(80);
        copyButton.setTooltip(createTooltip("Копировать команду в буфер обмена"));
        copyButton.setOnAction(e -> copyToClipboard(textArea.getText()));

        Button runButton = createButton("Run");
        runButton.setStyle(runButton.getStyle() + "-fx-background-color: " + COLOR_BUTTON_SMALL_BG + ";");
        runButton.setMinWidth(80);
        runButton.setPrefWidth(80);
        runButton.setMaxWidth(80);
        runButton.setOnAction(e -> runCommand(textArea.getText(), platform));

        buttonPanel.getChildren().addAll(copyButton, runButton);
        return buttonPanel;
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
        exitItem.setOnAction(e -> {
            // saveHistoryToXml();
            Platform.exit();
        });

        fileMenu.getItems().addAll(open1CStartItem, new javafx.scene.control.SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(fileMenu);

        // Меню Помощь
        Menu helpMenu = new Menu("_Помощь");
        helpMenu.setStyle("-fx-padding: 5 10 5 10;");

        MenuItem aboutItem = new MenuItem("О _программе");
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().add(aboutItem);
        menuBar.getMenus().add(helpMenu);

        return menuBar;
    }

    /**
     * Создаёт панель с режимами запуска и опциями
     * 
     * @return VBox с панелью режимов и опций
     */
    private VBox createModePanel() {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: " + COLOR_PANEL_BG + ";");
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

        Button modeHelpButton = createHelpButton();
        modeHelpButton.setTooltip(createTooltip(RunYBaseHelpTexts.APP_MODE_INFO));
        modeHelpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Справка: режимы запуска",
                RunYBaseHelpTexts.APP_MODE_INFO));

        modeRow.getChildren().addAll(modeLabel, designerRadio, thinRadio, thickManagedRadio, thickOrdinaryRadio,
                modeHelpButton);

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

        executeProcessingField = new ComboBoxWithMenuButton<String>("Введите имя файла внешней обработки", null, "Путь к файлу внешнего отчета или обработки"); 
        executeProcessingField.setPrefWidth(250);
        executeProcessingField.setStyle("-fx-background-color: " + COLOR_INPUT_BG + "; -fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 3px;");
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
                executeProcessingField.getComboBox().getEditor().setText(selectedPath.getAbsolutePath());
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

        // HBox с TextArea и кнопками
        HBox platformRow = new HBox(5);
        platformRow.setAlignment(Pos.CENTER_LEFT);

        // Создаём TextArea
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(4);
        textArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_INPUT_BG
                + "; -fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        HBox.setHgrow(textArea, Priority.ALWAYS);

        // Сохраняем ссылку на TextArea в зависимости от разрядности
        if (bits == 32) {
            outputArea86 = textArea;
        } else {
            outputArea = textArea;
        }

        // Создаём панель с кнопками
        VBox buttonPanel = createRunCopyButtons(textArea, platformName);
        platformRow.getChildren().addAll(textArea, buttonPanel);
        contentBox.getChildren().add(platformRow);
    }

    /**
     * Создаёт панель с полем ввода адреса базы данных
     * 
     * @return VBox с панелью адреса
     */
    private VBox createAddressPanel() {
        VBox addressPanel = new VBox(5);
        addressPanel.setStyle("-fx-background-color: " + COLOR_PANEL_BG + ";");
        addressPanel.setPadding(new Insets(5));

        // Верхняя строка с полем ввода и кнопками
        HBox inputPanel = new HBox(5);
        inputPanel.setAlignment(Pos.CENTER_LEFT);

        Label addressLabel = new Label("Адрес БД:");
        addressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        // historyList = FXCollections.observableArrayList(getHistoryList());
        historyManager = new HistoryManager();
        ObservableList<String> historyList = historyManager.getHistoryList();

        addressControl = new ComboBoxWithMenuButton<>(RunYBaseHelpTexts.ADDRESS_EXAMPLE_INFO, historyList, RunYBaseHelpTexts.BASE_CONNECTION_PROMPT);
        addressComboBox = addressControl.getComboBox();
        addressControl.getChoiceButton().setOnAction(e -> selectDatabaseFromList());

        // Добавляем пункт меню "Заметка..."
        MenuItem noteItem = new MenuItem("_Заметка...");
        noteItem.setOnAction(e -> showNoteDialog());
        addressControl.getContextMenu().getItems().add(noteItem);

        // Добавляем пункт меню "Добавить в список баз"
        MenuItem addToDatabaseItem = new MenuItem("Добавить в список баз");
        addToDatabaseItem.setOnAction(e -> addCurrentAddressToDatabaseList());
        addressControl.getContextMenu().getItems().add(addToDatabaseItem);

        MenuItem refreshDatabaseItem = new MenuItem("Обновить список баз");
        refreshDatabaseItem.setOnAction(e -> refreshDatabaseList());
        addressControl.getContextMenu().getItems().add(refreshDatabaseItem);

        userCredentialsButton = createButton("П_ользователь");
        userCredentialsButton.setOnAction(e -> showUserCredentialsDialog());

        Button generateButton = createButton("С_формировать");
        generateButton.setOnAction(e -> handleButtonClick());

        inputPanel.getChildren().addAll(
                addressLabel, addressControl, userCredentialsButton, generateButton);
        HBox.setHgrow(addressControl, Priority.ALWAYS);
        addressPanel.getChildren().add(inputPanel);

        // Панель с именем БД и заметкой
        baseNameLabel = new Label();
        baseNameLabel.setWrapText(true);
        //baseNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " + COLOR_ACCENT + ";");

        baseNoteLabel = new Label();
        baseNoteLabel.setWrapText(true);
        baseNoteLabel.setStyle("-fx-font-style: italic; -fx-font-size: 10px; -fx-text-fill: #666666;");
        baseNoteLabel.setPadding(new Insets(2, 0, 0, 0));

        VBox infoPanel = new VBox(2, baseNameLabel, baseNoteLabel);
        infoPanel.setPadding(new Insets(5, 0, 0, 0));
        HBox.setHgrow(infoPanel, Priority.ALWAYS);
        addressPanel.getChildren().add(infoPanel);

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
     * Обновляет отображение имени БД и заметки
     */
    private void updateBaseInfoDisplay(String address) {
        if (address == null) {
            address = "";
        }

        address = address.trim();

        // Отображаем имя базы
        if (!address.isEmpty()) {
            String baseName = registeredAddressMap.get(address);
            if (baseName != null) {
                baseNameLabel.setText("📋 База: " + baseName);
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

    private void showAboutDialog() {
        String message = String.format(
                "Построитель команды запуска 1С\n\n" +
                        "Версия: %s\n\n" +
                        "Программа для удобного формирования\n" +
                        "команд запуска 1С: Предприятие и Конфигуратор.\n\n" +
                        "Разработано с использованием:\n" +
                        "• Koda-pro\n" +
                        "• Koda-base",
                AppConstants.VERSION);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
    private Tooltip createTooltip(String text) {
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

    private void updateUserButtonState() {
        if (userCredentialsButton == null)
            return;
        String address = getCurrentAddress();
        UserCredentials cred = credentialsManager.get(address);
        if (cred != null && !cred.getUsername().isEmpty()) {
            userCredentialsButton.setUserData(COLOR_USER_HAS_CRED);
            userCredentialsButton.setStyle(userCredentialsButton.getStyle()
                    .replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + COLOR_USER_HAS_CRED));
            userCredentialsButton.setTooltip(createTooltip(RunYBaseHelpTexts.USER_CRED_HAS_CRED_TOOLTIP, cred.getUsername()));
        } else {
            userCredentialsButton.setUserData(COLOR_USER_NO_CRED);
            userCredentialsButton.setStyle(userCredentialsButton.getStyle()
                    .replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + COLOR_USER_NO_CRED));
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

    // -----------------------------------------------------------------
    // Учётные данные пользователей
    // -----------------------------------------------------------------

    private void showUserCredentialsDialog() {
        String address = getCurrentAddress();
        if (address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        UserCredentials existing = credentialsManager.get(address);
        String currentUsername = existing != null ? existing.getUsername() : "";
        String currentPassword = existing != null ? existing.getPassword() : "";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Учётные данные для базы");
        dialog.setHeaderText(address);

        ButtonType okButtonType = new ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
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
                    credentialsManager.put(address, new UserCredentials(username, password));
                    showAlert(Alert.AlertType.INFORMATION, "Успешно",
                            "Учётные данные сохранены для адреса:\n" + address);
                } else if (existing != null) {
                    credentialsManager.remove(address);
                    showAlert(Alert.AlertType.INFORMATION, "Удалено", "Учётные данные удалены для адреса:\n" + address);
                }
                updateUserButtonState();
            }
        });
    }

    // -----------------------------------------------------------------
    // Заметки к базам
    // -----------------------------------------------------------------

    private void showNoteDialog() {
        String address = getCurrentAddress();
        if (address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        String currentNote = historyManager.getNote(address);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(RunYBaseHelpTexts.NOTE_DIALOG_TITLE);
        dialog.setHeaderText(address);

        ButtonType okButtonType = new ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType clearButtonType = new ButtonType("Очистить", javafx.scene.control.ButtonBar.ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Отмена", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, clearButtonType, cancelButtonType);

        TextArea noteArea = new TextArea(currentNote != null ? currentNote : "");
        noteArea.setPrefRowCount(6);
        noteArea.setPrefColumnCount(50);
        noteArea.setWrapText(true);
        noteArea.setPromptText("Введите текстовую заметку к этой базе...\n" +
                "Например: \"Проверить печатную форму заказа клиента\"");
        noteArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-background-color: " +
                COLOR_INPUT_BG + "; -fx-border-color: " + COLOR_BUTTON_BORDER + "; -fx-border-width: 1px;");

        Label charCountLabel = new Label("0 / " + AppConstants.MAX_NOTE_LENGTH + " символов");
        charCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        noteArea.textProperty().addListener((obs, old, newVal) -> {
            int len = newVal == null ? 0 : newVal.length();
            charCountLabel.setText(len + " / " + AppConstants.MAX_NOTE_LENGTH + " символов");
            charCountLabel.setStyle(len > AppConstants.MAX_NOTE_LENGTH ?
                    "-fx-font-size: 10px; -fx-text-fill: red;" :
                    "-fx-font-size: 10px; -fx-text-fill: gray;");
        });

        VBox content = new VBox(8, noteArea, charCountLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Platform.runLater(noteArea::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            String note = noteArea.getText();
            historyManager.saveNote(address, note);
            
            if (result == okButtonType) {
                // Обновляем отображение заметки на основной форме
                updateBaseInfoDisplay(address);
                if (note.trim().isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "Удалено",
                            "Заметка удалена для адреса:\n" + address);
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Успешно",
                            "Заметка сохранена для адреса:\n" + address);
                }
            } else if (result == clearButtonType) {
                // Обновляем отображение заметки на основной форме
                updateBaseInfoDisplay(address);
                showAlert(Alert.AlertType.INFORMATION, "Удалено",
                        "Заметка удалена для адреса:\n" + address);
            }
        });
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
                                        "Обнаружен адрес базы 1С в буфере обмена!\n\nАвтоматически вставлено:\n" + extractedAddress +
                                                "\n\nНайдена база: " + base.name);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        // Если не удалось загрузить список баз, просто вставляем адрес
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Автовставка из буфера",
                            "Обнаружен адрес базы 1С в буфере обмена!\n\nАвтоматически вставлено:\n" + extractedAddress);
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

    private String getCurrentAddress() {
        String text = addressComboBox.getEditor().getText();
        return text == null ? "" : text.trim();
    }

    private String getCommandPart() {
        if (designerRadio.isSelected())
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
     * @return сформированная строка запуска
     */
    private String buildCommand(String platformPath, String appArch, String escaped,
            String commandPart, UserCredentials cred, boolean isDesigner) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("\"").append(platformPath).append("\" ");
        cmd.append(commandPart).append(" ");
        cmd.append("/IBConnectionString \"").append(escaped).append("\"");

        if (cred != null && !cred.getUsername().isEmpty()) {
            cmd.append(" /N \"").append(cred.getUsername()).append("\"");
            if (!cred.getPassword().isEmpty()) {
                cmd.append(" /P \"").append(cred.getPassword()).append("\"");
            }
        }

        if (priorityPlatformCheckbox.isSelected()) {
            cmd.append(" /AppArch ").append(appArch);
        }

        // Режим отладки игнорируется для Конфигуратора
        if (debugModeCheckbox.isSelected() && !isDesigner) {
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

        if (executeProcessingCheckbox.isSelected()) {
            String processingPath = executeProcessingField.getText();
            if (processingPath != null && !processingPath.trim().isEmpty()) {
                cmd.append(" /Execute \"").append(processingPath.trim()).append("\"");
            }
        }

        return cmd.toString();
    }

    private void handleButtonClick() {
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
        }

        addToHistory(text);

        String commandPart = getCommandPart();
        outputArea86.setText("");
        outputArea.setText("");

        String escaped = text.replace("\"", "\"\"");
        UserCredentials cred = credentialsManager.get(text);

        boolean isDesigner = designerRadio.isSelected();

        String cmd86 = buildCommand(
                "C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe",
                "x86",
                escaped,
                commandPart,
                cred,
                isDesigner);

        String cmd64 = buildCommand(
                "C:\\Program Files\\1cv8\\common\\1cestart.exe",
                "x86_64",
                escaped,
                commandPart,
                cred,
                isDesigner);

        outputArea86.setText(cmd86);
        outputArea.setText(cmd64);

        if (debugArea != null && SHOW_DEBUG_PANEL)
            debugArea.setText("");
    }

    private void runCommand(String command, String platform) {
        if (command == null || command.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Нет команды для запуска!");
            return;
        }
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
            String mode = designerRadio.isSelected() ? "Конфигуратор"
                    : thinRadio.isSelected() ? "Тонкий клиент"
                            : thickOrdinaryRadio.isSelected() ? "Толстый клиент (Обычное)"
                                    : "Толстый клиент (Управляемое)";

            if (finished) {
                int code = process.exitValue();
                if (code == 0) {
                    if (SHOW_RUN_MESSAGE) {
                        showAutoClosingAlert(
                                mode + " успешно запущен!\nБаза: " + getCurrentAddress() + "\nПлатформа: " + platform,
                                "Запуск 1С", 5);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка запуска " + mode + "!\nКод: " + code);
                }
            } else {
                if (SHOW_RUN_MESSAGE) {
                    showAutoClosingAlert(mode + " запущен (фоновый процесс).\nБаза: " + getCurrentAddress(),
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

    private void showAutoClosingAlert(String message, String title, int delaySeconds) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        PauseTransition delay = new PauseTransition(Duration.seconds(delaySeconds));
        delay.setOnFinished(e -> alert.hide());
        delay.play();

        alert.show();
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

    private void addContextMenu(TextInputControl control) {
        ContextMenu menu = new ContextMenu();
        MenuItem paste = new MenuItem("Вставить");
        paste.setOnAction(e -> control.paste());
        MenuItem cut = new MenuItem("Вырезать");
        cut.setOnAction(e -> control.cut());
        MenuItem copy = new MenuItem("Копировать");
        copy.setOnAction(e -> control.copy());
        MenuItem selectAll = new MenuItem("Выделить всё");
        selectAll.setOnAction(e -> control.selectAll());
        menu.getItems().addAll(paste, cut, copy, new javafx.scene.control.SeparatorMenuItem(), selectAll);
        control.setContextMenu(menu);
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isEmpty())
            return;
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        showAlert(Alert.AlertType.INFORMATION, "Успешно", "Команда скопирована в буфер обмена!");
    }

    private static List<BaseEntry> loadAndSortDatabases()
            throws IOException, ParserConfigurationException, SAXException, Exception {
        String userHome = System.getProperty("user.home");
        Path ibasesPath = Paths.get(userHome, "AppData", "Roaming", "1C", "1CEStart", "ibases.v8i");

        if (!Files.exists(ibasesPath)) {
            Platform.runLater(() -> showAlertStatic(Alert.AlertType.ERROR, "Ошибка",
                    "Файл списка баз не найден:\n" + ibasesPath.toString()));
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

    private void selectDatabaseFromList() {
        try {
            List<BaseEntry> baseEntries = loadAndSortDatabases();

            // Загружаем заметки для каждой базы
            for (BaseEntry entry : baseEntries) {
                String note = historyManager.getNote(entry.connect);
                if (note != null && !note.isEmpty()) {
                    entry.setNote(note);
                }
            }

            if (baseEntries.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Список баз", "Нет зарегистрированных баз 1С");
                return;
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Выбор базы 1С");
            dialog.setResizable(true);

            ListView<BaseEntry> listView = new ListView<>(FXCollections.observableArrayList(baseEntries));
            listView.setCellFactory(lv -> new BaseEntryListCell());

            Button okButton = new Button("OK");
            okButton.setOnAction(e -> selectCurrentBase(listView, dialog));

            Button cancelButton = new Button("Отмена");
            cancelButton.setOnAction(e -> dialog.close());

            listView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    selectCurrentBase(listView, dialog);
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
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE)
                    dialog.close();
            });

            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при чтении списка баз:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Выбирает текущую выделенную базу и закрывает диалог
     */
    private void selectCurrentBase(ListView<BaseEntry> listView, Stage dialog) {
        BaseEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            addressComboBox.setValue(selected.connect);
            addressControl.setAdressIB(selected.name);
            updateUserButtonState();
            dialog.close();
        }
    }

    private static void parseIniFormatWithOrder(Path path, List<BaseEntry> entries) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showAlertStatic(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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
