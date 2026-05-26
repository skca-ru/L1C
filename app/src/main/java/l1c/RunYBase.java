// Версия KodaPro
package l1c;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

public class RunYBase extends Application {

    // #region ========== НАСТРОЙКИ ==========
    private static final boolean SHOW_DEBUG_PANEL = false;
    private static final boolean SHOW_RUN_MESSAGE = true;
    private static final int MAX_HISTORY_SIZE = 20;
    private static final String HISTORY_DIR = ".1c_launcher";
    private static final String HISTORY_FILE = "history.xml";
    // #endregion =================================

    // @formatter:off
    // #region ========== ЦВЕТА 1С (белый фон + приглушённые жёлтые акценты) ==========
    private static final String COLOR_BG = "#FFFFFF";
    private static final String COLOR_BUTTON_BG = "#E6C878";
    private static final String COLOR_BUTTON_COPY_BG = "#F3E4BC";
    private static final String COLOR_BUTTON_FG = "#000000";
    private static final String COLOR_ACCENT = "#C8A046";
    private static final String COLOR_TEXT_FG = "#000000";
    private static final String COLOR_INPUT_BG = "#FFFFFF";
    private static final String COLOR_OUTPUT_BG = "#FAFAFA";
    private static final String COLOR_PANEL_BG = "#FFFFFF";
    private static final String COLOR_USER_HAS_CRED = "#B2DAB2";
    private static final String COLOR_USER_NO_CRED = "#F3E4BC";
    // #endregion =================================
    // @formatter:on

    private ComboBox<String> addressComboBox;
    private TextArea outputArea86;
    private TextArea outputArea;
    private RadioButton designerRadio;
    private RadioButton thinRadio;
    private RadioButton thickOrdinaryRadio;
    private RadioButton thickManagedRadio;
    private ToggleGroup modeGroup;
    private CheckBox priorityPlatformCheckbox;
    private TextArea debugArea;
    private ObservableList<String> historyList;

    private Button userCredentialsButton;

    private static final java.util.Map<String, UserCredentials> credentialsMap = new java.util.HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        loadCredentials();
        loadHistoryFromXml();

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        // #region ОбластьАдресаБД
        HBox inputPanel = new HBox(5);
        inputPanel.setAlignment(Pos.CENTER_LEFT);

        Label addressLabel = new Label("Адрес БД:");
        addressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        historyList = FXCollections.observableArrayList(getHistoryList());
        addressComboBox = new ComboBox<>(historyList);
        addressComboBox.setEditable(true);
        addressComboBox.setPromptText("для файловой 'File=\"C:\\1C\\Base\";' для серверной 'Srvr=\"127.0.0.1\";Ref=\"Base\";'");
        addressComboBox.setTooltip(new Tooltip("Например File=\"C:\\1C\\Base\"  или  Srvr=\"127.0.0.1\";Ref=\"Base\""));
        addressComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addressComboBox, Priority.ALWAYS);

        Button selectButton = createFlatButton("…");
        selectButton.setTooltip(new Tooltip("Выбрать из списка зарегистрированных баз"));
        selectButton.setOnAction(e -> selectDatabaseFromList());
        selectButton.setMinSize(30, 30);
        selectButton.setMaxSize(30, 30);

        userCredentialsButton = createButton("Пользователь");
        userCredentialsButton.setOnAction(e -> showUserCredentialsDialog());

        Button generateButton = createButton("Сформировать");
        generateButton.setOnAction(e -> handleButtonClick());

        inputPanel.getChildren().addAll(addressLabel, addressComboBox, selectButton, userCredentialsButton, generateButton);
        root.getChildren().add(inputPanel);
        // #endregion

        // #region Режим запуска
        HBox modePanel = new HBox(15);
        modePanel.setAlignment(Pos.CENTER_LEFT);
        modePanel.setStyle("-fx-background-color: " + COLOR_PANEL_BG + ";");
        modePanel.setPadding(new Insets(5, 0, 5, 0));

        Label modeLabel = new Label("Режим запуска:");
        modeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        modeGroup = new ToggleGroup();
        designerRadio = new RadioButton("Конфигуратор");
        designerRadio.setToggleGroup(modeGroup);
        designerRadio.setSelected(true);
        thinRadio = new RadioButton("Тонкий клиент");
        thinRadio.setToggleGroup(modeGroup);
        thickOrdinaryRadio = new RadioButton("Толстый клиент (Обычное приложение)");
        thickOrdinaryRadio.setToggleGroup(modeGroup);
        thickManagedRadio = new RadioButton("Толстый клиент (Управляемое приложение)");
        thickManagedRadio.setToggleGroup(modeGroup);

        modePanel.getChildren().addAll(modeLabel, designerRadio, thinRadio, thickOrdinaryRadio, thickManagedRadio);
        root.getChildren().add(modePanel);

        HBox priorityPanel = new HBox(5);
        priorityPanel.setAlignment(Pos.CENTER_LEFT);
        priorityPanel.setStyle("-fx-background-color: " + COLOR_PANEL_BG + ";");

        priorityPlatformCheckbox = new CheckBox("Приоритет платформы");
        priorityPlatformCheckbox.setTooltip(new Tooltip("Добавить параметры /AppArch для разрядности платформы"));

        Button helpButton = createFlatButton("?");
        helpButton.setTooltip(new Tooltip(
                "Параметр /AppArch указывает разрядность клиентского приложения:\n" +
                        "x86 — использовать только 32-разрядные версии\n" +
                        "x86_64 — использовать только 64-разрядные версии\n" +
                        "x86_prt — выбрать 32-разрядную при наличии обеих\n" +
                        "x86_64_prt — выбрать 64-разрядную при наличии обеих"));
        helpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Справка: параметр /AppArch",
                "Параметр /AppArch указывает разрядность используемого клиентского приложения на 64-разрядных ОС Windows.\n\n" +
                        "Доступные значения:\n" +
                        "• x86 — использовать только 32-разрядные версии\n" +
                        "• x86_64 — использовать только 64-разрядные версии\n" +
                        "• x86_prt — поиск актуальной версии, при наличии обеих выбрать 32-разрядную\n" +
                        "• x86_64_prt — поиск актуальной версии, при наличии обеих выбрать 64-разрядную"));
        helpButton.setMinSize(25, 25);
        helpButton.setMaxSize(25, 25);

        priorityPanel.getChildren().addAll(priorityPlatformCheckbox, helpButton);
        root.getChildren().add(priorityPanel);
        // #endregion

        root.getChildren().add(new Label());

        // x86
        Label label86 = new Label("Команда для 32-битной платформы (x86):");
        root.getChildren().add(label86);

        HBox p86 = new HBox(5);
        p86.setAlignment(Pos.CENTER_LEFT);
        outputArea86 = new TextArea();
        outputArea86.setWrapText(true);
        outputArea86.setPrefRowCount(4);
        outputArea86.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_OUTPUT_BG + ";");
        HBox.setHgrow(outputArea86, Priority.ALWAYS);

        VBox buttonPanel86 = new VBox(5);
        Button copy86 = createButton("Copy");
        copy86.setStyle(copy86.getStyle() + "-fx-background-color: " + COLOR_BUTTON_COPY_BG + ";");
        copy86.setOnAction(e -> copyToClipboard(outputArea86.getText()));
        Button run86 = createButton("Run");
        run86.setStyle(run86.getStyle() + "-fx-background-color: " + COLOR_BUTTON_COPY_BG + ";");
        run86.setOnAction(e -> runCommand(outputArea86.getText(), "x86"));
        buttonPanel86.getChildren().addAll(copy86, run86);

        p86.getChildren().addAll(outputArea86, buttonPanel86);
        root.getChildren().add(p86);

        // x64
        Label label64 = new Label("Команда для 64-битной платформы (x64):");
        root.getChildren().add(label64);

        HBox p64 = new HBox(5);
        p64.setAlignment(Pos.CENTER_LEFT);
        outputArea = new TextArea();
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(4);
        outputArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_OUTPUT_BG + ";");
        HBox.setHgrow(outputArea, Priority.ALWAYS);

        VBox buttonPanel64 = new VBox(5);
        Button copy64 = createButton("Copy");
        copy64.setStyle(copy64.getStyle() + "-fx-background-color: " + COLOR_BUTTON_COPY_BG + ";");
        copy64.setOnAction(e -> copyToClipboard(outputArea.getText()));
        Button run64 = createButton("Run");
        run64.setStyle(run64.getStyle() + "-fx-background-color: " + COLOR_BUTTON_COPY_BG + ";");
        run64.setOnAction(e -> runCommand(outputArea.getText(), "x64"));
        buttonPanel64.getChildren().addAll(copy64, run64);

        p64.getChildren().addAll(outputArea, buttonPanel64);
        root.getChildren().add(p64);

        if (SHOW_DEBUG_PANEL) {
            root.getChildren().add(new Label("Отладка (вывод команды и ошибок):"));
            debugArea = new TextArea();
            debugArea.setPrefRowCount(8);
            debugArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_OUTPUT_BG + ";");
            debugArea.setStyle(debugArea.getStyle() + "-fx-border-color: " + COLOR_ACCENT + ";");
            root.getChildren().add(debugArea);
        } else {
            debugArea = new TextArea();
        }

        Scene scene = new Scene(root, 1050, SHOW_DEBUG_PANEL ? 700 : 500);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                saveCredentials();
                saveHistoryToXml();
                Platform.exit();
            }
        });

        primaryStage.setTitle("Построитель команды запуска 1С - Примеры: File=\"C:\\1C\\Base\";  или  Srvr=\"127.0.0.1\";Ref=\"Base\";");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            saveCredentials();
            saveHistoryToXml();
        });
        primaryStage.show();

        addContextMenu(addressComboBox.getEditor());
        addContextMenu(outputArea);
        addContextMenu(outputArea86);
        if (debugArea != null) addContextMenu(debugArea);

        autoPasteFromClipboard();

        addressComboBox.requestFocus();

        addressComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> updateUserButtonState());
        updateUserButtonState();
    }

    private Button createButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + COLOR_BUTTON_BG + ";" +
                        "-fx-text-fill: " + COLOR_BUTTON_FG + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 4 10 4 10;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );
        button.setOnMousePressed(e -> button.setStyle(button.getStyle().replace("dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1)", "inset dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1)")));
        button.setOnMouseReleased(e -> button.setStyle(button.getStyle().replace("inset dropshadow", "dropshadow")));
        return button;
    }

    private Button createFlatButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + COLOR_INPUT_BG + ";" +
                        "-fx-text-fill: " + COLOR_TEXT_FG + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-border-color: gray;" +
                        "-fx-border-radius: 3px;" +
                        "-fx-background-radius: 3px;"
        );
        return button;
    }

    private void updateUserButtonState() {
        if (userCredentialsButton == null) return;
        String address = getCurrentAddress();
        UserCredentials cred = credentialsMap.get(address);
        if (cred != null && !cred.getUsername().isEmpty()) {
            userCredentialsButton.setStyle(userCredentialsButton.getStyle().replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + COLOR_USER_HAS_CRED));
            userCredentialsButton.setTooltip(new Tooltip("Учётные данные сохранены: " + cred.getUsername()));
        } else {
            userCredentialsButton.setStyle(userCredentialsButton.getStyle().replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + COLOR_USER_NO_CRED));
            userCredentialsButton.setTooltip(new Tooltip("Нажмите чтобы задать учётные данные"));
        }
    }

    // -----------------------------------------------------------------
    // Учётные данные пользователей
    // -----------------------------------------------------------------

    private static Path getCredentialsPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, HISTORY_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию для истории: " + dir);
        }
        return dir.resolve("credentials.xml");
    }

    private static void loadCredentials() {
        Path path = getCredentialsPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());

            NodeList credNodes = doc.getElementsByTagName("credential");
            for (int i = 0; i < credNodes.getLength(); i++) {
                Element elem = (Element) credNodes.item(i);
                String address = getTagValue("address", elem);
                String username = getTagValue("username", elem);
                String encryptedPassword = getTagValue("password", elem);

                if (address != null && username != null && !address.isEmpty()) {
                    String password = encryptedPassword != null ? decrypt(encryptedPassword) : "";
                    credentialsMap.put(address, new UserCredentials(username, password));
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки учётных данных: " + e.getMessage());
        }
    }

    private static void saveCredentials() {
        Path path = getCredentialsPath();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("credentials");
            doc.appendChild(root);

            for (java.util.Map.Entry<String, UserCredentials> entry : credentialsMap.entrySet()) {
                Element credElem = doc.createElement("credential");

                Element addrElem = doc.createElement("address");
                addrElem.setTextContent(entry.getKey());
                credElem.appendChild(addrElem);

                Element userElem = doc.createElement("username");
                userElem.setTextContent(entry.getValue().getUsername());
                credElem.appendChild(userElem);

                Element passElem = doc.createElement("password");
                passElem.setTextContent(encrypt(entry.getValue().getPassword()));
                credElem.appendChild(passElem);

                root.appendChild(credElem);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(path.toFile());
            transformer.transform(source, result);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения учётных данных: " + e.getMessage());
        }
    }

    private static final String KEY = "1C_Launcher_2026_Secret_Key";

    private static String encrypt(String input) {
        if (input == null || input.isEmpty()) return "";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[inputBytes.length];
        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return Base64.getEncoder().encodeToString(result);
    }

    private static String decrypt(String input) {
        if (input == null || input.isEmpty()) return "";
        byte[] inputBytes = Base64.getDecoder().decode(input);
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[inputBytes.length];
        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return new String(result, StandardCharsets.UTF_8);
    }

    private void showUserCredentialsDialog() {
        String address = getCurrentAddress();
        if (address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Сначала введите адрес базы данных!");
            return;
        }

        UserCredentials existing = credentialsMap.get(address);
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
                    credentialsMap.put(address, new UserCredentials(username, password));
                    saveCredentials();
                    showAlert(Alert.AlertType.INFORMATION, "Успешно", "Учётные данные сохранены для адреса:\n" + address);
                } else if (existing != null) {
                    credentialsMap.remove(address);
                    saveCredentials();
                    showAlert(Alert.AlertType.INFORMATION, "Удалено", "Учётные данные удалены для адреса:\n" + address);
                }
                updateUserButtonState();
            }
        });
    }

    private void askAndRunWithCredentials(String address, String baseCommand) {
        UserCredentials cred = credentialsMap.get(address);
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
                    runCommand(baseCommand, "без учётных данных");
                }
            });
        } else {
            runCommand(baseCommand, "");
        }
    }

    // -----------------------------------------------------------------
    // Работа с историей в XML (домашняя папка)
    // -----------------------------------------------------------------
    private static Path getHistoryPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, HISTORY_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию для истории: " + dir);
        }
        return dir.resolve(HISTORY_FILE);
    }

    private static List<String> getHistoryList() {
        List<String> list = new ArrayList<>();
        Path path = getHistoryPath();
        if (!Files.exists(path)) {
            createDefaultHistoryFile(path);
            return list;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            NodeList addrNodes = doc.getElementsByTagName("address");
            for (int i = 0; i < addrNodes.getLength(); i++) {
                String addr = addrNodes.item(i).getTextContent();
                if (addr != null && !addr.trim().isEmpty() && !list.contains(addr.trim())) {
                    list.add(addr.trim());
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Ошибка загрузки истории из XML. Будет создан новый файл.");
            e.printStackTrace();
            createDefaultHistoryFile(path);
        }
        return list;
    }

    private static void loadHistoryFromXml() {
        // данные уже загружены через getHistoryList()
    }

    private static void createDefaultHistoryFile(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("history");
            doc.appendChild(root);

            root.appendChild(doc.createComment(
                    " Файл истории адресов баз 1С.\n" +
                            " Содержит последние использованные адреса для быстрого выбора.\n" +
                            " Если удалить или очистить этот файл, история будет восстановлена при следующем запуске программы (пустая).\n" +
                            " Рекомендуется не редактировать файл вручную. Если всё же редактируете, сделайте резервную копию.\n"));

            Element addresses = doc.createElement("addresses");
            root.appendChild(addresses);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(path.toFile());
            transformer.transform(source, result);
        } catch (Exception e) {
            System.err.println("Не удалось создать файл истории по умолчанию: " + path);
            e.printStackTrace();
        }
    }

    private void addToHistory(String address) {
        if (address == null || address.isEmpty()) return;
        historyList.remove(address);
        historyList.add(0, address);
        while (historyList.size() > MAX_HISTORY_SIZE) {
            historyList.remove(historyList.size() - 1);
        }
        addressComboBox.setValue(address);
        saveHistoryToXml();
    }

    private void saveHistoryToXml() {
        Path path = getHistoryPath();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("history");
            doc.appendChild(root);

            root.appendChild(doc.createComment(
                    " Файл истории адресов баз 1С.\n" +
                            " Содержит последние использованные адреса для быстрого выбора.\n" +
                            " Если удалить или очистить этот файл, история будет восстановлена при следующем запуске программы (пустая).\n" +
                            " Рекомендуется не редактировать файл вручную. Если всё же редактируете, сделайте резервную копию.\n"));

            Element addresses = doc.createElement("addresses");
            root.appendChild(addresses);

            for (String addr : historyList) {
                Element addrElem = doc.createElement("address");
                addrElem.setTextContent(addr);
                addresses.appendChild(addrElem);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(path.toFile());
            transformer.transform(source, result);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения истории в XML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isDatabaseAddress(String text) {
        if (text == null || text.isEmpty()) return false;
        String trimmed = text.trim();
        return trimmed.startsWith("File=") || trimmed.startsWith("Srvr=");
    }

    private void autoPasteFromClipboard() {
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String text = clipboard.getString();
                if (isDatabaseAddress(text)) {
                    addressComboBox.setValue(text);
                    showAlert(Alert.AlertType.INFORMATION, "Автовставка из буфера",
                            "Обнаружен адрес базы 1С в буфере обмена!\n\nАвтоматически вставлено:\n" + text);
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
        if (designerRadio.isSelected()) return "DESIGNER";
        if (thinRadio.isSelected()) return "ENTERPRISE /ThinClient";
        if (thickOrdinaryRadio.isSelected()) return "ENTERPRISE /RunModeOrdinaryApplication";
        return "ENTERPRISE /RunModeManagedApplication";
    }

    private void handleButtonClick() {
        String text = getCurrentAddress();
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение",
                    "Введите адрес базы данных! Например:\n\nФайловая БД: File=\"C:\\1C\\Base\"\nКлиент-сервер: Srvr=\"127.0.0.1\";Ref=\"Base\";");
            return;
        }

        addToHistory(text);

        String commandPart = getCommandPart();
        outputArea86.setText("");
        outputArea.setText("");

        String escaped = text.replace("\"", "\"\"");
        String cmd86 = "\"C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe\" " + commandPart
                + " /IBConnectionString \"" + escaped + "\"";
        String cmd64 = "\"C:\\Program Files\\1cv8\\common\\1cestart.exe\" " + commandPart + " /IBConnectionString \"" + escaped + "\"";

        UserCredentials cred = credentialsMap.get(text);
        if (cred != null && !cred.getUsername().isEmpty()) {
            cmd86 += " /N \"" + cred.getUsername() + "\"";
            cmd64 += " /N \"" + cred.getUsername() + "\"";
            if (!cred.getPassword().isEmpty()) {
                cmd86 += " /P \"" + cred.getPassword() + "\"";
                cmd64 += " /P \"" + cred.getPassword() + "\"";
            }
        }

        if (priorityPlatformCheckbox.isSelected()) {
            cmd86 += " /AppArch x86";
            cmd64 += " /AppArch x86_64";
        }

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
                        showAutoClosingAlert(mode + " успешно запущен!\nБаза: " + getCurrentAddress() + "\nПлатформа: " + platform,
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
        if (text == null || text.isEmpty()) return;
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
            okButton.setOnAction(e -> {
                BaseEntry selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    addressComboBox.setValue(selected.connect);
                    updateUserButtonState();
                }
                dialog.close();
            });

            Button cancelButton = new Button("Отмена");
            cancelButton.setOnAction(e -> dialog.close());

            listView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    BaseEntry selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        addressComboBox.setValue(selected.connect);
                        updateUserButtonState();
                        dialog.close();
                    }
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
                if (e.getCode() == KeyCode.ESCAPE) dialog.close();
            });

            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при чтении списка баз:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void parseIniFormatWithOrder(Path path, List<BaseEntry> entries) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String currentName = null;
        String currentConnect = null;
        double currentOrder = Double.MAX_VALUE;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

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
        if (list.getLength() == 0) return null;
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

    BaseEntry(String name, String connect, double order) {
        this.name = name;
        this.connect = connect;
        this.order = order;
    }
}

class BaseEntryListCell extends ListCell<BaseEntry> {
    private final Label nameLabel = new Label();
    private final Label connectLabel = new Label();
    private final VBox container = new VBox(2, nameLabel, connectLabel);

    public BaseEntryListCell() {
        container.setPadding(new Insets(5, 10, 5, 10));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        connectLabel.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");
        connectLabel.setPadding(new Insets(0, 0, 0, 20));
    }

    @Override
    protected void updateItem(BaseEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(item.name);
            connectLabel.setText(item.connect);
            if (isSelected()) {
                container.setStyle("-fx-background-color: -fx-selection-bar;");
                nameLabel.setTextFill(Color.WHITE);
                connectLabel.setTextFill(Color.WHITE);
            } else {
                container.setStyle("-fx-background-color: transparent;");
                nameLabel.setTextFill(Color.BLACK);
                connectLabel.setTextFill(Color.BLACK);
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

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
