// Р’РµСЂСЃРёСЏ kРџСЂРѕ
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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
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
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

public class RunYBase extends Application {

    // @formatter:off
    // #region ========== РќРђРЎРўР РћР™РљР ==========
    private static final String VERSION = "2026.06.04.002";
    private static final boolean SHOW_DEBUG_PANEL    = false;
    private static final boolean SHOW_RUN_MESSAGE    = true;
    private static final int MAX_HISTORY_SIZE        = 20;
    private static final String HISTORY_DIR          = ".1c_launcher";
    private static final String HISTORY_FILE         = "history.xml";
    private static final String APP_ARCH_INFO        = """
            РџР°СЂР°РјРµС‚СЂ /AppArch СѓРєР°Р·С‹РІР°РµС‚ СЂР°Р·СЂСЏРґРЅРѕСЃС‚СЊ РёСЃРїРѕР»СЊР·СѓРµРјРѕРіРѕ РєР»РёРµРЅС‚СЃРєРѕРіРѕ РїСЂРёР»РѕР¶РµРЅРёСЏ РЅР° 64-СЂР°Р·СЂСЏРґРЅС‹С… РћРЎ Windows.

            Р”РѕСЃС‚СѓРїРЅС‹Рµ Р·РЅР°С‡РµРЅРёСЏ:
            вЂў x86 вЂ” РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ С‚РѕР»СЊРєРѕ 32-СЂР°Р·СЂСЏРґРЅС‹Рµ РІРµСЂСЃРёРё
            вЂў x86_64 вЂ” РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ С‚РѕР»СЊРєРѕ 64-СЂР°Р·СЂСЏРґРЅС‹Рµ РІРµСЂСЃРёРё
            вЂў x86_prt вЂ” РїРѕРёСЃРє Р°РєС‚СѓР°Р»СЊРЅРѕР№ РІРµСЂСЃРёРё, РїСЂРё РЅР°Р»РёС‡РёРё РѕР±РµРёС… РІС‹Р±СЂР°С‚СЊ 32-СЂР°Р·СЂСЏРґРЅСѓСЋ
            вЂў x86_64_prt вЂ” РїРѕРёСЃРє Р°РєС‚СѓР°Р»СЊРЅРѕР№ РІРµСЂСЃРёРё, РїСЂРё РЅР°Р»РёС‡РёРё РѕР±РµРёС… РІС‹Р±СЂР°С‚СЊ 64-СЂР°Р·СЂСЏРґРЅСѓСЋ""";
    private static final String DEBUG_INFO           = """
            /Debug [<СЂРµР¶РёРј>] [-attach]
            вЂ” СѓРєР°Р·С‹РІР°РµС‚, С‡С‚Рѕ РґР°РЅРЅРѕРµ РєР»РёРµРЅС‚СЃРєРѕРµ РїСЂРёР»РѕР¶РµРЅРёРµ Р±СѓРґРµС‚ Р·Р°РїСѓС‰РµРЅРѕ РІ СЂРµР¶РёРјРµ РѕС‚Р»Р°РґРєРё.
            РџСЂРѕС‚РѕРєРѕР», РёСЃРїРѕР»СЊР·СѓРµРјС‹Р№ РґР»СЏ СЂР°Р±РѕС‚С‹ РѕС‚Р»Р°РґС‡РёРєР°, РѕРїСЂРµРґРµР»СЏРµС‚СЃСЏ РїР°СЂР°РјРµС‚СЂРѕРј <СЂРµР¶РёРј>:
            -tcp вЂ“ РґР»СЏ РѕС‚Р»Р°РґРєРё РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ РїСЂРѕС‚РѕРєРѕР» TCP/IP;
            -http вЂ“ РґР»СЏ РѕС‚Р»Р°РґРєРё РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ РїСЂРѕС‚РѕРєРѕР» HTTP.
            Р•СЃР»Рё РІ РєРѕРјР°РЅРґРЅРѕР№ СЃС‚СЂРѕРєРµ СѓРєР°Р·Р°РЅ РїР°СЂР°РјРµС‚СЂ -attach, С‚Рѕ СЌС‚Рѕ РѕР·РЅР°С‡Р°РµС‚, С‡С‚Рѕ РѕС‚Р»Р°РґС‡РёРє Р±СѓРґРµС‚ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїРѕРґРєР»СЋС‡Р°С‚СЊ РїСЂРµРґРјРµС‚С‹ РѕС‚Р»Р°РґРєРё (РєР»РёРµРЅС‚СЃРєРёР№ Рё СЃРµСЂРІРµСЂРЅС‹Р№) Р·Р°РїСѓСЃРєР°РµРјРѕРіРѕ РїСЂРёР»РѕР¶РµРЅРёСЏ, РєРѕС‚РѕСЂС‹Рµ Р±СѓРґСѓС‚ Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅС‹ РЅР° СЃРµСЂРІРµСЂРµ РѕС‚Р»Р°РґРєРё. РџР°СЂР°РјРµС‚СЂ РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ С‚РѕР»СЊРєРѕ РґР»СЏ РѕС‚Р»Р°РґРєРё РїРѕ РїСЂРѕС‚РѕРєРѕР»Сѓ HTTP.""";
    private static final String DEBUG_PROTOCOL_INFO  = """
            Р РµР¶РёРј РѕС‚Р»Р°РґРєРё:
            вЂў РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ вЂ” /Debug (РїСЂРѕС‚РѕРєРѕР» РІС‹Р±РёСЂР°РµС‚СЃСЏ РїР»Р°С‚С„РѕСЂРјРѕР№);
            вЂў -tcp вЂ” /Debug -tcp (РїСЂРѕС‚РѕРєРѕР» TCP/IP);
            вЂў -http вЂ” /Debug -http (РїСЂРѕС‚РѕРєРѕР» HTTP).""";
    private static final String HISTORY_COMMENT ="""
        Р¤Р°Р№Р» РёСЃС‚РѕСЂРёРё Р°РґСЂРµСЃРѕРІ Р±Р°Р· 1РЎ.
        РЎРѕРґРµСЂР¶РёС‚ РїРѕСЃР»РµРґРЅРёРµ РёСЃРїРѕР»СЊР·РѕРІР°РЅРЅС‹Рµ Р°РґСЂРµСЃР° РґР»СЏ Р±С‹СЃС‚СЂРѕРіРѕ РІС‹Р±РѕСЂР°.
        Р•СЃР»Рё СѓРґР°Р»РёС‚СЊ РёР»Рё РѕС‡РёСЃС‚РёС‚СЊ СЌС‚РѕС‚ С„Р°Р№Р», РёСЃС‚РѕСЂРёСЏ Р±СѓРґРµС‚ РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅР° РїСЂРё СЃР»РµРґСѓСЋС‰РµРј Р·Р°РїСѓСЃРєРµ РїСЂРѕРіСЂР°РјРјС‹ (РїСѓСЃС‚Р°СЏ).
        
        Р РµРєРѕРјРµРЅРґСѓРµС‚СЃСЏ РЅРµ СЂРµРґР°РєС‚РёСЂРѕРІР°С‚СЊ С„Р°Р№Р» РІСЂСѓС‡РЅСѓСЋ. Р•СЃР»Рё РІСЃС‘ Р¶Рµ СЂРµРґР°РєС‚РёСЂСѓРµС‚Рµ, СЃРґРµР»Р°Р№С‚Рµ СЂРµР·РµСЂРІРЅСѓСЋ РєРѕРїРёСЋ.""";

    // #endregion =================================

    // #region ========== Р¦Р’Р•РўРђ 1РЎ (Р±РµР»С‹Р№ С„РѕРЅ + РїСЂРёРіР»СѓС€С‘РЅРЅС‹Рµ Р¶С‘Р»С‚С‹Рµ Р°РєС†РµРЅС‚С‹) ==========
    private static final String COLOR_BG             = "#FFFFFF";
    private static final String COLOR_BUTTON_BG      = "#E6C878";
    private static final String COLOR_BUTTON_SMALL_BG = "#F3E4BC";
    private static final String COLOR_BUTTON_FG      = "#000000";
    private static final String COLOR_BUTTON_BORDER  = "#C0A050";
    private static final String COLOR_ACCENT         = "#C8A046";
    private static final String COLOR_TEXT_FG        = "#000000";
    private static final String COLOR_INPUT_BG       = "#FFFFFF";
    private static final String COLOR_OUTPUT_BG      = "#FAFAFA";
    private static final String COLOR_PANEL_BG       = "#FFFFFF";
    private static final String COLOR_USER_HAS_CRED  = "#B2DAB2";
    private static final String COLOR_USER_NO_CRED   = "#F3E4BC";
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
    private CheckBox debugModeCheckbox;
    private ComboBox<String> debugProtocolCombo;
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

        // РЎРѕР·РґР°С‘Рј РјРµРЅСЋ
        MenuBar menuBar = createMenuBar();

        // РљРѕСЂРЅРµРІРѕР№ BorderPane Р±РµР· РѕС‚СЃС‚СѓРїРѕРІ
        BorderPane borderRoot = new BorderPane();
        borderRoot.setStyle("-fx-background-color: " + COLOR_BG + ";");
        borderRoot.setTop(menuBar);

        // Р’СЃРµ РѕСЃС‚Р°Р»СЊРЅС‹Рµ СЌР»РµРјРµРЅС‚С‹ РїРѕРјРµС‰Р°РµРј РІ VBox СЃ РѕС‚СЃС‚СѓРїР°РјРё
        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: " + COLOR_BG + ";");

        // #region РћР±Р»Р°СЃС‚СЊРђРґСЂРµСЃР°Р‘Р”
        HBox inputPanel = new HBox(5);
        inputPanel.setAlignment(Pos.CENTER_LEFT);

        Label addressLabel = new Label("РђРґСЂРµСЃ Р‘Р”:");
        addressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        historyList = FXCollections.observableArrayList(getHistoryList());
        addressComboBox = new ComboBox<>(historyList);
        addressComboBox.setEditable(true);
        addressComboBox.setPromptText("РґР»СЏ С„Р°Р№Р»РѕРІРѕР№ 'File=\"C:\\1C\\Base\";' РґР»СЏ СЃРµСЂРІРµСЂРЅРѕР№ 'Srvr=\"127.0.0.1\";Ref=\"Base\";'");
        addressComboBox.setTooltip(new Tooltip("РќР°РїСЂРёРјРµСЂ File=\"C:\\1C\\Base\"  РёР»Рё  Srvr=\"127.0.0.1\";Ref=\"Base\""));
        addressComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addressComboBox, Priority.ALWAYS);

        Button selectButton = createFlatButton("вЂ¦");
        selectButton.setTooltip(new Tooltip("Р’С‹Р±СЂР°С‚СЊ РёР· СЃРїРёСЃРєР° Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅРЅС‹С… Р±Р°Р·"));
        selectButton.setOnAction(e -> selectDatabaseFromList());
        selectButton.setMinSize(30, 30);
        selectButton.setMaxSize(30, 30);

        userCredentialsButton = createButton("Рџ_РѕР»СЊР·РѕРІР°С‚РµР»СЊ");
        userCredentialsButton.setOnAction(e -> showUserCredentialsDialog());

        Button generateButton = createButton("РЎ_С„РѕСЂРјРёСЂРѕРІР°С‚СЊ");
        generateButton.setOnAction(e -> handleButtonClick());

        inputPanel.getChildren().addAll(
            addressLabel, addressComboBox, selectButton, userCredentialsButton, generateButton);
        contentBox.getChildren().add(inputPanel);
        // #endregion

        // #region Р РµР¶РёРј Р·Р°РїСѓСЃРєР°
        HBox modePanel = new HBox(15);
        modePanel.setAlignment(Pos.CENTER_LEFT);
        modePanel.setStyle("-fx-background-color: " + COLOR_PANEL_BG + ";");
        modePanel.setPadding(new Insets(5, 0, 5, 0));

        Label modeLabel = new Label("Р РµР¶РёРј Р·Р°РїСѓСЃРєР°:");
        modeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        modeGroup = new ToggleGroup();
        designerRadio = new RadioButton("РљРѕРЅС„РёРіСѓСЂР°С‚РѕСЂ");
        designerRadio.setToggleGroup(modeGroup);
        designerRadio.setSelected(true);
        thinRadio = new RadioButton("РўРѕРЅРєРёР№ РєР»РёРµРЅС‚");
        thinRadio.setToggleGroup(modeGroup);
        thickManagedRadio = new RadioButton("РўРѕР»СЃС‚С‹Р№ РєР»РёРµРЅС‚ (РЈРїСЂР°РІР»СЏРµРјРѕРµ РїСЂРёР»РѕР¶РµРЅРёРµ)");
        thickManagedRadio.setToggleGroup(modeGroup);
        thickOrdinaryRadio = new RadioButton("РўРѕР»СЃС‚С‹Р№ РєР»РёРµРЅС‚ (РћР±С‹С‡РЅРѕРµ РїСЂРёР»РѕР¶РµРЅРёРµ)");
        thickOrdinaryRadio.setToggleGroup(modeGroup);

        modePanel.getChildren().addAll(modeLabel, designerRadio, thinRadio, thickManagedRadio, thickOrdinaryRadio);
        contentBox.getChildren().add(modePanel);

        debugModeCheckbox = new CheckBox("Р РµР¶РёРј РѕС‚Р»Р°РґРєРё");
        debugModeCheckbox.setSelected(true); // РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ РІРєР»СЋС‡РµРЅРѕ
        debugModeCheckbox.setTooltip(new Tooltip("Р”РѕР±Р°РІРёС‚СЊ РїР°СЂР°РјРµС‚СЂ /Debug РґР»СЏ Р·Р°РїСѓСЃРєР° РІ СЂРµР¶РёРјРµ РѕС‚Р»Р°РґРєРё"));

        debugProtocolCombo = new ComboBox<>(FXCollections.observableArrayList("РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ", "-tcp", "-http"));
        debugProtocolCombo.setValue("РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ");
        debugProtocolCombo.setTooltip(new Tooltip(DEBUG_PROTOCOL_INFO));

        Button debugHelpButton = createHelpButton();
        debugHelpButton.setTooltip(new Tooltip(DEBUG_INFO));
        debugHelpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "РЎРїСЂР°РІРєР°: РїР°СЂР°РјРµС‚СЂ /Debug", DEBUG_INFO));

        priorityPlatformCheckbox = new CheckBox("РџСЂРёРѕСЂРёС‚РµС‚ РїР»Р°С‚С„РѕСЂРјС‹");
        priorityPlatformCheckbox.setTooltip(new Tooltip("Р”РѕР±Р°РІРёС‚СЊ РїР°СЂР°РјРµС‚СЂС‹ /AppArch РґР»СЏ СЂР°Р·СЂСЏРґРЅРѕСЃС‚Рё РїР»Р°С‚С„РѕСЂРјС‹"));

        Button helpButton = createHelpButton();
        helpButton.setTooltip(new Tooltip(APP_ARCH_INFO));
        helpButton.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "РЎРїСЂР°РІРєР°: РїР°СЂР°РјРµС‚СЂ /AppArch", APP_ARCH_INFO));

        HBox debugOption = createHelpOption(debugModeCheckbox, debugProtocolCombo, debugHelpButton);
        HBox platformOption = createHelpOption(priorityPlatformCheckbox, helpButton);
        HBox optionsPanel = new HBox(15, debugOption, platformOption);
        optionsPanel.setAlignment(Pos.CENTER_LEFT);
        optionsPanel.setStyle("-fx-background-color: " + COLOR_PANEL_BG + ";");
        contentBox.getChildren().add(optionsPanel);
        // #endregion

        contentBox.getChildren().add(new Label());

        // x86
        Label label86 = new Label("РљРѕРјР°РЅРґР° РґР»СЏ 32-Р±РёС‚РЅРѕР№ РїР»Р°С‚С„РѕСЂРјС‹ (x86):");
        label86.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        contentBox.getChildren().add(label86);

        HBox p86 = new HBox(5);
        p86.setAlignment(Pos.CENTER_LEFT);
        outputArea86 = new TextArea();
        outputArea86.setWrapText(true);
        outputArea86.setPrefRowCount(4);
        outputArea86.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_INPUT_BG + "; -fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        HBox.setHgrow(outputArea86, Priority.ALWAYS);

        VBox buttonPanel86 = new VBox(5);
        Button copy86 = createButton("Copy");
        copy86.setStyle(copy86.getStyle() + "-fx-background-color: " + COLOR_BUTTON_SMALL_BG + ";");
        copy86.setOnAction(e -> copyToClipboard(outputArea86.getText()));
        Button run86 = createButton("Run");
        run86.setStyle(run86.getStyle() + "-fx-background-color: " + COLOR_BUTTON_SMALL_BG + ";");
        run86.setOnAction(e -> runCommand(outputArea86.getText(), "x86"));
        buttonPanel86.getChildren().addAll(copy86, run86);

        p86.getChildren().addAll(outputArea86, buttonPanel86);
        contentBox.getChildren().add(p86);

        // x64
        Label label64 = new Label("РљРѕРјР°РЅРґР° РґР»СЏ 64-Р±РёС‚РЅРѕР№ РїР»Р°С‚С„РѕСЂРјС‹ (x64):");
        label64.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        contentBox.getChildren().add(label64);

        HBox p64 = new HBox(5);
        p64.setAlignment(Pos.CENTER_LEFT);
        outputArea = new TextArea();
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(4);
        outputArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-background-color: " + COLOR_INPUT_BG + "; -fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        HBox.setHgrow(outputArea, Priority.ALWAYS);

        VBox buttonPanel64 = new VBox(5);
        Button copy64 = createButton("Copy");
        copy64.setStyle(copy64.getStyle() + "-fx-background-color: " + COLOR_BUTTON_SMALL_BG + ";");
        copy64.setOnAction(e -> copyToClipboard(outputArea.getText()));
        Button run64 = createButton("Run");
        run64.setStyle(run64.getStyle() + "-fx-background-color: " + COLOR_BUTTON_SMALL_BG + ";");
        run64.setOnAction(e -> runCommand(outputArea.getText(), "x64"));
        buttonPanel64.getChildren().addAll(copy64, run64);

        p64.getChildren().addAll(outputArea, buttonPanel64);
        contentBox.getChildren().add(p64);

        if (SHOW_DEBUG_PANEL) {
            contentBox.getChildren().add(new Label("РћС‚Р»Р°РґРєР° (РІС‹РІРѕРґ РєРѕРјР°РЅРґС‹ Рё РѕС€РёР±РѕРє):"));
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
                saveCredentials();
                saveHistoryToXml();
                Platform.exit();
            }
        });

        primaryStage.setTitle(
                "РџРѕСЃС‚СЂРѕРёС‚РµР»СЊ РєРѕРјР°РЅРґС‹ Р·Р°РїСѓСЃРєР° 1РЎ - РџСЂРёРјРµСЂС‹: File=\"C:\\1C\\Base\";  РёР»Рё  Srvr=\"127.0.0.1\";Ref=\"Base\";");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            saveCredentials();
            saveHistoryToXml();
        });
        primaryStage.show();

        addContextMenu(addressComboBox.getEditor());
        addContextMenu(outputArea);
        addContextMenu(outputArea86);
        if (debugArea != null)
            addContextMenu(debugArea);

        autoPasteFromClipboard();

        addressComboBox.requestFocus();

        addressComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> updateUserButtonState());
        updateUserButtonState();
    }

    private Button createButton(String text) {
        return createButton(text, COLOR_BUTTON_BG);
    }

    private Button createButton(String text, String bgColor) {

        Button button = new Button(text);
        // Р§С‚Рѕ Р±С‹ СѓСЃРєРѕСЂРёС‚РµР»Рё РЅР° РєРЅРѕРїРєРµ СЂР°Р±РѕС‚Р°Р»Рё
        button.setMnemonicParsing(true);

        button.setUserData(bgColor); // РЎРѕС…СЂР°РЅСЏРµРј Р±Р°Р·РѕРІС‹Р№ С†РІРµС‚

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

        // Р­С„С„РµРєС‚ РЅР°РІРµРґРµРЅРёСЏ РєСѓСЂСЃРѕСЂР°
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

        // Р­С„С„РµРєС‚ РЅР°Р¶Р°С‚РёСЏ - СЃРґРІРёРіР°РµРј РєРЅРѕРїРєСѓ РЅР° 1px РІРЅРёР·-РІРїСЂР°РІРѕ
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-padding: 0; -fx-background-insets: 0; -fx-background-radius: 0;");

        // РњРµРЅСЋ Р¤Р°Р№Р»
        Menu fileMenu = new Menu("_Р¤Р°Р№Р»");
        fileMenu.setStyle("-fx-padding: 5 10 5 10;"); 
        
        MenuItem exitItem = new MenuItem("Р’С‹_С…РѕРґ");
        exitItem.setAccelerator(KeyCombination.valueOf("Shortcut+Q"));
        exitItem.setOnAction(e -> {
            saveCredentials();
            saveHistoryToXml();
            Platform.exit();
        });
        
        fileMenu.getItems().addAll(exitItem);
        menuBar.getMenus().add(fileMenu);

        // РњРµРЅСЋ РџРѕРјРѕС‰СЊ
        Menu helpMenu = new Menu("_РџРѕРјРѕС‰СЊ");
        helpMenu.setStyle("-fx-padding: 5 10 5 10;");
        
        MenuItem aboutItem = new MenuItem("Рћ _РїСЂРѕРіСЂР°РјРјРµ");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().add(aboutItem);
        menuBar.getMenus().add(helpMenu);

        return menuBar;
    }

    private void showAboutDialog() {
        String message = String.format(
            "РџРѕСЃС‚СЂРѕРёС‚РµР»СЊ РєРѕРјР°РЅРґС‹ Р·Р°РїСѓСЃРєР° 1РЎ\n\n" +
            "Р’РµСЂСЃРёСЏ: %s\n\n" +
            "РџСЂРѕРіСЂР°РјРјР° РґР»СЏ СѓРґРѕР±РЅРѕРіРѕ С„РѕСЂРјРёСЂРѕРІР°РЅРёСЏ\n" +
            "РєРѕРјР°РЅРґ Р·Р°РїСѓСЃРєР° 1РЎ: РџСЂРµРґРїСЂРёСЏС‚РёРµ Рё РљРѕРЅС„РёРіСѓСЂР°С‚РѕСЂ.\n\n" +
            "Р Р°Р·СЂР°Р±РѕС‚Р°РЅРѕ СЃ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРµРј:\n" +
            "вЂў Koda-pro\n" +
            "вЂў Koda-base",
            VERSION
        );
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Рћ РїСЂРѕРіСЂР°РјРјРµ");
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

    private void updateUserButtonState() {
        if (userCredentialsButton == null)
            return;
        String address = getCurrentAddress();
        UserCredentials cred = credentialsMap.get(address);
        if (cred != null && !cred.getUsername().isEmpty()) {
            userCredentialsButton.setUserData(COLOR_USER_HAS_CRED);
            userCredentialsButton.setStyle(userCredentialsButton.getStyle()
                    .replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + COLOR_USER_HAS_CRED));
            userCredentialsButton.setTooltip(new Tooltip("РЈС‡С‘С‚РЅС‹Рµ РґР°РЅРЅС‹Рµ СЃРѕС…СЂР°РЅРµРЅС‹: " + cred.getUsername()));
        } else {
            userCredentialsButton.setUserData(COLOR_USER_NO_CRED);
            userCredentialsButton.setStyle(userCredentialsButton.getStyle()
                    .replaceAll("-fx-background-color: #[A-Fa-f0-9]+", "-fx-background-color: " + COLOR_USER_NO_CRED));
            userCredentialsButton.setTooltip(new Tooltip("РќР°Р¶РјРёС‚Рµ С‡С‚РѕР±С‹ Р·Р°РґР°С‚СЊ СѓС‡С‘С‚РЅС‹Рµ РґР°РЅРЅС‹Рµ"));
        }
    }

    // -----------------------------------------------------------------
    // РЈС‡С‘С‚РЅС‹Рµ РґР°РЅРЅС‹Рµ РїРѕР»СЊР·РѕРІР°С‚РµР»РµР№
    // -----------------------------------------------------------------

    private static Path getCredentialsPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, HISTORY_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕР·РґР°С‚СЊ РґРёСЂРµРєС‚РѕСЂРёСЋ РґР»СЏ РёСЃС‚РѕСЂРёРё: " + dir);
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
            System.err.println("РћС€РёР±РєР° Р·Р°РіСЂСѓР·РєРё СѓС‡С‘С‚РЅС‹С… РґР°РЅРЅС‹С…: " + e.getMessage());
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
            System.err.println("РћС€РёР±РєР° СЃРѕС…СЂР°РЅРµРЅРёСЏ СѓС‡С‘С‚РЅС‹С… РґР°РЅРЅС‹С…: " + e.getMessage());
        }
    }

    private static final String KEY = "1C_Launcher_2026_Secret_Key";

    /**
     * РЈРІРµР»РёС‡РёРІР°РµС‚ СЏСЂРєРѕСЃС‚СЊ С†РІРµС‚Р° РЅР° Р·Р°РґР°РЅРЅС‹Р№ РєРѕСЌС„С„РёС†РёРµРЅС‚
     * @param hexColor HEX С†РІРµС‚ (РЅР°РїСЂРёРјРµСЂ "#E6C878")
     * @param factor РєРѕСЌС„С„РёС†РёРµРЅС‚ СЏСЂРєРѕСЃС‚Рё (1.0 - Р±РµР· РёР·РјРµРЅРµРЅРёСЏ, >1.0 - СЃРІРµС‚Р»РµРµ, <1.0 - С‚РµРјРЅРµРµ)
     * @return РЅРѕРІС‹Р№ HEX С†РІРµС‚
     */
    private String adjustColorBrightness(String hexColor, double factor) {
        try {
            String hex = hexColor.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            r = Math.min(255, (int)(r * factor));
            g = Math.min(255, (int)(g * factor));
            b = Math.min(255, (int)(b * factor));

            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
    }

    private static String encrypt(String input) {
        if (input == null || input.isEmpty())
            return "";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[inputBytes.length];
        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return Base64.getEncoder().encodeToString(result);
    }

    private static String decrypt(String input) {
        if (input == null || input.isEmpty())
            return "";
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
            showAlert(Alert.AlertType.WARNING, "РџСЂРµРґСѓРїСЂРµР¶РґРµРЅРёРµ", "РЎРЅР°С‡Р°Р»Р° РІРІРµРґРёС‚Рµ Р°РґСЂРµСЃ Р±Р°Р·С‹ РґР°РЅРЅС‹С…!");
            return;
        }

        UserCredentials existing = credentialsMap.get(address);
        String currentUsername = existing != null ? existing.getUsername() : "";
        String currentPassword = existing != null ? existing.getPassword() : "";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("РЈС‡С‘С‚РЅС‹Рµ РґР°РЅРЅС‹Рµ РґР»СЏ Р±Р°Р·С‹");
        dialog.setHeaderText(address);

        ButtonType okButtonType = new ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("РћС‚РјРµРЅР°", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField usernameField = new TextField(currentUsername);
        usernameField.setPromptText("РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ");
        PasswordField passwordField = new PasswordField();
        passwordField.setText(currentPassword);
        passwordField.setPromptText("РџР°СЂРѕР»СЊ");

        grid.add(new Label("РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("РџР°СЂРѕР»СЊ:"), 0, 1);
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
                    showAlert(Alert.AlertType.INFORMATION, "РЈСЃРїРµС€РЅРѕ",
                            "РЈС‡С‘С‚РЅС‹Рµ РґР°РЅРЅС‹Рµ СЃРѕС…СЂР°РЅРµРЅС‹ РґР»СЏ Р°РґСЂРµСЃР°:\n" + address);
                } else if (existing != null) {
                    credentialsMap.remove(address);
                    saveCredentials();
                    showAlert(Alert.AlertType.INFORMATION, "РЈРґР°Р»РµРЅРѕ", "РЈС‡С‘С‚РЅС‹Рµ РґР°РЅРЅС‹Рµ СѓРґР°Р»РµРЅС‹ РґР»СЏ Р°РґСЂРµСЃР°:\n" + address);
                }
                updateUserButtonState();
            }
        });
    }

    private void askAndRunWithCredentials(String address, String baseCommand) {
        UserCredentials cred = credentialsMap.get(address);
        if (cred != null && !cred.getUsername().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Р—Р°РїСѓСЃРє СЃ СѓС‡С‘С‚РЅС‹РјРё РґР°РЅРЅС‹РјРё");
            alert.setHeaderText(null);
            alert.setContentText("Р—Р°РїСѓСЃС‚РёС‚СЊ РѕС‚ РёРјРµРЅРё РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ:\n" + cred.getUsername() +
                    (cred.getPassword().isEmpty() ? "\n(Р±РµР· РїР°СЂРѕР»СЏ)" : ""));

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    String cmd = baseCommand;
                    cmd += " /UserName \"" + cred.getUsername() + "\"";
                    if (!cred.getPassword().isEmpty()) {
                        cmd += " /Password \"" + cred.getPassword() + "\"";
                    }
                    runCommand(cmd, "СЃ СѓС‡С‘С‚РЅС‹РјРё РґР°РЅРЅС‹РјРё");
                } else {
                    runCommand(baseCommand, "Р±РµР· СѓС‡С‘С‚РЅС‹С… РґР°РЅРЅС‹С…");
                }
            });
        } else {
            runCommand(baseCommand, "");
        }
    }

    // -----------------------------------------------------------------
    // Р Р°Р±РѕС‚Р° СЃ РёСЃС‚РѕСЂРёРµР№ РІ XML (РґРѕРјР°С€РЅСЏСЏ РїР°РїРєР°)
    // -----------------------------------------------------------------
    private static Path getHistoryPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, HISTORY_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕР·РґР°С‚СЊ РґРёСЂРµРєС‚РѕСЂРёСЋ РґР»СЏ РёСЃС‚РѕСЂРёРё: " + dir);
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
            System.err.println("РћС€РёР±РєР° Р·Р°РіСЂСѓР·РєРё РёСЃС‚РѕСЂРёРё РёР· XML. Р‘СѓРґРµС‚ СЃРѕР·РґР°РЅ РЅРѕРІС‹Р№ С„Р°Р№Р».");
            e.printStackTrace();
            createDefaultHistoryFile(path);
        }
        return list;
    }

    private static void loadHistoryFromXml() {
        // РґР°РЅРЅС‹Рµ СѓР¶Рµ Р·Р°РіСЂСѓР¶РµРЅС‹ С‡РµСЂРµР· getHistoryList()
    }

    private static void createDefaultHistoryFile(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("history");
            doc.appendChild(root);

            root.appendChild(doc.createComment( HISTORY_COMMENT));

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
            System.err.println("РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕР·РґР°С‚СЊ С„Р°Р№Р» РёСЃС‚РѕСЂРёРё РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ: " + path);
            e.printStackTrace();
        }
    }

    private void addToHistory(String address) {
        if (address == null || address.isEmpty())
            return;
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

            root.appendChild(doc.createComment(HISTORY_COMMENT));

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
            System.err.println("РћС€РёР±РєР° СЃРѕС…СЂР°РЅРµРЅРёСЏ РёСЃС‚РѕСЂРёРё РІ XML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isDatabaseAddress(String text) {
        if (text == null || text.isEmpty())
            return false;
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
                    showAlert(Alert.AlertType.INFORMATION, "РђРІС‚РѕРІСЃС‚Р°РІРєР° РёР· Р±СѓС„РµСЂР°",
                            "РћР±РЅР°СЂСѓР¶РµРЅ Р°РґСЂРµСЃ Р±Р°Р·С‹ 1РЎ РІ Р±СѓС„РµСЂРµ РѕР±РјРµРЅР°!\n\nРђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РІСЃС‚Р°РІР»РµРЅРѕ:\n" + text);
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
            return "ENTERPRISE /ThinClient";
        if (thickOrdinaryRadio.isSelected())
            return "ENTERPRISE /RunModeOrdinaryApplication";
        return "ENTERPRISE /RunModeManagedApplication";
    }

    private void handleButtonClick() {
        String text = getCurrentAddress();
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "РџСЂРµРґСѓРїСЂРµР¶РґРµРЅРёРµ",
                    "Р’РІРµРґРёС‚Рµ Р°РґСЂРµСЃ Р±Р°Р·С‹ РґР°РЅРЅС‹С…! РќР°РїСЂРёРјРµСЂ:\n\nР¤Р°Р№Р»РѕРІР°СЏ Р‘Р”: File=\"C:\\1C\\Base\"\nРљР»РёРµРЅС‚-СЃРµСЂРІРµСЂ: Srvr=\"127.0.0.1\";Ref=\"Base\";");
            return;
        }

        addToHistory(text);

        String commandPart = getCommandPart();
        outputArea86.setText("");
        outputArea.setText("");

        String escaped = text.replace("\"", "\"\"");
        String cmd86 = "\"C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe\" " + commandPart
                + " /IBConnectionString \"" + escaped + "\"";
        String cmd64 = "\"C:\\Program Files\\1cv8\\common\\1cestart.exe\" " + commandPart + " /IBConnectionString \""
                + escaped + "\"";

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

        // Р”РѕР±Р°РІР»СЏРµРј СЂРµР¶РёРј РѕС‚Р»Р°РґРєРё
        if (debugModeCheckbox.isSelected()) {
            String debugParam = " /Debug -attach";
            String protocol = debugProtocolCombo.getValue();
            if (protocol != null && !"РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ".equals(protocol)) {
                debugParam += " " + protocol;
            }
            cmd86 += debugParam;
            cmd64 += debugParam;
        }

        outputArea86.setText(cmd86);
        outputArea.setText(cmd64);

        if (debugArea != null && SHOW_DEBUG_PANEL)
            debugArea.setText("");
    }

    private void runCommand(String command, String platform) {
        if (command == null || command.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "РћС€РёР±РєР°", "РќРµС‚ РєРѕРјР°РЅРґС‹ РґР»СЏ Р·Р°РїСѓСЃРєР°!");
            return;
        }
        if (SHOW_DEBUG_PANEL && debugArea != null) {
            debugArea.appendText("=== Р—Р°РїСѓСЃРє (" + platform + ") ===\nРљРѕРјР°РЅРґР°: " + command + "\n");
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
            String mode = designerRadio.isSelected() ? "РљРѕРЅС„РёРіСѓСЂР°С‚РѕСЂ"
                    : thinRadio.isSelected() ? "РўРѕРЅРєРёР№ РєР»РёРµРЅС‚"
                            : thickOrdinaryRadio.isSelected() ? "РўРѕР»СЃС‚С‹Р№ РєР»РёРµРЅС‚ (РћР±С‹С‡РЅРѕРµ)"
                                    : "РўРѕР»СЃС‚С‹Р№ РєР»РёРµРЅС‚ (РЈРїСЂР°РІР»СЏРµРјРѕРµ)";

            if (finished) {
                int code = process.exitValue();
                if (code == 0) {
                    if (SHOW_RUN_MESSAGE) {
                        showAutoClosingAlert(
                                mode + " СѓСЃРїРµС€РЅРѕ Р·Р°РїСѓС‰РµРЅ!\nР‘Р°Р·Р°: " + getCurrentAddress() + "\nРџР»Р°С‚С„РѕСЂРјР°: " + platform,
                                "Р—Р°РїСѓСЃРє 1РЎ", 5);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "РћС€РёР±РєР°", "РћС€РёР±РєР° Р·Р°РїСѓСЃРєР° " + mode + "!\nРљРѕРґ: " + code);
                }
            } else {
                if (SHOW_RUN_MESSAGE) {
                    showAutoClosingAlert(mode + " Р·Р°РїСѓС‰РµРЅ (С„РѕРЅРѕРІС‹Р№ РїСЂРѕС†РµСЃСЃ).\nР‘Р°Р·Р°: " + getCurrentAddress(),
                            "Р—Р°РїСѓСЃРє 1РЎ", 5);
                }
            }
            if (SHOW_DEBUG_PANEL && debugArea != null)
                debugArea.appendText("=== РљРѕРЅРµС† Р·Р°РїСѓСЃРєР° ===\n\n");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "РћС€РёР±РєР°", "РћС€РёР±РєР° Р·Р°РїСѓСЃРєР°: " + e.getMessage());
            if (SHOW_DEBUG_PANEL && debugArea != null)
                debugArea.appendText("РСЃРєР»СЋС‡РµРЅРёРµ: " + e + "\n");
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
        MenuItem paste = new MenuItem("Р’СЃС‚Р°РІРёС‚СЊ");
        paste.setOnAction(e -> control.paste());
        MenuItem cut = new MenuItem("Р’С‹СЂРµР·Р°С‚СЊ");
        cut.setOnAction(e -> control.cut());
        MenuItem copy = new MenuItem("РљРѕРїРёСЂРѕРІР°С‚СЊ");
        copy.setOnAction(e -> control.copy());
        MenuItem selectAll = new MenuItem("Р’С‹РґРµР»РёС‚СЊ РІСЃС‘");
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
        showAlert(Alert.AlertType.INFORMATION, "РЈСЃРїРµС€РЅРѕ", "РљРѕРјР°РЅРґР° СЃРєРѕРїРёСЂРѕРІР°РЅР° РІ Р±СѓС„РµСЂ РѕР±РјРµРЅР°!");
    }

    private static List<BaseEntry> loadAndSortDatabases()
            throws IOException, ParserConfigurationException, SAXException, Exception {
        String userHome = System.getProperty("user.home");
        Path ibasesPath = Paths.get(userHome, "AppData", "Roaming", "1C", "1CEStart", "ibases.v8i");

        if (!Files.exists(ibasesPath)) {
            Platform.runLater(() -> showAlertStatic(Alert.AlertType.ERROR, "РћС€РёР±РєР°",
                    "Р¤Р°Р№Р» СЃРїРёСЃРєР° Р±Р°Р· РЅРµ РЅР°Р№РґРµРЅ:\n" + ibasesPath.toString()));
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
                showAlert(Alert.AlertType.INFORMATION, "РЎРїРёСЃРѕРє Р±Р°Р·", "РќРµС‚ Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅРЅС‹С… Р±Р°Р· 1РЎ");
                return;
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Р’С‹Р±РѕСЂ Р±Р°Р·С‹ 1РЎ");
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

            Button cancelButton = new Button("РћС‚РјРµРЅР°");
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
                if (e.getCode() == KeyCode.ESCAPE)
                    dialog.close();
            });

            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "РћС€РёР±РєР°", "РћС€РёР±РєР° РїСЂРё С‡С‚РµРЅРёРё СЃРїРёСЃРєР° Р±Р°Р·:\n" + e.getMessage());
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

// ========== Р’РЎРџРћРњРћР“РђРўР•Р›Р¬РќР«Р• РљР›РђРЎРЎР« ==========

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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

