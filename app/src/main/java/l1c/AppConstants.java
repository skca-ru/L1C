package l1c;

public class AppConstants {
    
    // Версия приложения
    public static final String VERSION = "2026.07.07.009";
    
    // @formatter:off
    public static final String APP_DATA_DIR        = ".1c_launcher";
    public static final String HISTORY_FILE        = "history.xml";
    public static final String CREDENTIALS_FILE    = "credentials.xml";
    public static final String ENCRYPTION_KEY      = "1C_Launcher_2026_Secret_Key";
    public static final int    MAX_HISTORY_SIZE    = 20;
    public static final int    MAX_NOTE_LENGTH     = 500;
    public static final int    CHOICE_BUTTON_WIDTH = 25;

    // Цвета для рамки при фокусе
    public static final String FOCUS_BORDER_COLOR  = "#0078D7";
    public static final String NORMAL_BORDER_COLOR = "#a0a0a0";

    // @formatter:on


    // Стили для всплывающих подсказок (Tooltip)
    public static final String TOOLTIP_STYLE = """
            -fx-background-color: #F3E4BC;
            -fx-text-fill: #000000;
            -fx-border-color: #C0A050;
            -fx-border-radius: 3px;
            -fx-background-radius: 3px;
            -fx-padding: 5 10 5 10;
            -fx-font-size: 13px;
            -fx-font-weight: normal;
            -fx-max-width: 400px;
            -fx-wrap-text: true;
            """;
}
