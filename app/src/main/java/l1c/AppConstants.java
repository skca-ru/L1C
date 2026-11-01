package l1c;

public class AppConstants {
    
    // Р’РµСЂСЃРёСЏ РїСЂРёР»РѕР¶РµРЅРёСЏ
    // Р¤РѕСЂРјР°С‚: РіРѕРґ.РјРµСЃСЏС†.РґРµРЅСЊ.РїРѕСЂСЏРґРєРѕРІС‹Р№ РЅРѕРјРµСЂ
    public static final String VERSION = "2026.11.01.001";
    
    // @formatter:off
    public static final String APP_DATA_DIR        = ".1c_launcher";
    public static final String HISTORY_FILE        = "history.xml";
    public static final String CREDENTIALS_FILE    = "credentials.xml";
    public static final String ENCRYPTION_KEY      = "1C_Launcher_2026_Secret_Key";
    public static final int    MAX_HISTORY_SIZE    = 20;
    public static final int    MAX_NOTE_LENGTH     = 500;
    public static final int    CHOICE_BUTTON_WIDTH = 25;

    // Р¦РІРµС‚Р° РґР»СЏ СЂР°РјРєРё РїСЂРё С„РѕРєСѓСЃРµ
    public static final String FOCUS_BORDER_COLOR  = "#0078D7";
    public static final String NORMAL_BORDER_COLOR = "#a0a0a0";

    // Р¦РІРµС‚Р° РґР»СЏ РёРЅС‚РµСЂС„РµР№СЃР° (Р±РµР»С‹Р№ С„РѕРЅ + РїСЂРёРіР»СѓС€С‘РЅРЅС‹Рµ Р¶С‘Р»С‚С‹Рµ Р°РєС†РµРЅС‚С‹)
    public static final String COLOR_BG              = "#FFFFFF";
    public static final String COLOR_BUTTON_BG       = "#E6C878";
    public static final String COLOR_BUTTON_SMALL_BG = "#F3E4BC";
    public static final String COLOR_BUTTON_FG       = "#000000";
    public static final String COLOR_BUTTON_BORDER   = "#C0A050";
    public static final String COLOR_ACCENT          = "#C8A046";
    public static final String COLOR_TEXT_FG         = "#000000";
    public static final String COLOR_INPUT_BG        = "#FFFFFF";
    public static final String COLOR_OUTPUT_BG       = "#FAFAFA";
    public static final String COLOR_PANEL_BG        = "#FFFFFF";
    public static final String COLOR_USER_HAS_CRED   = "#B2DAB2";
    public static final String COLOR_USER_NO_CRED    = "#F3E4BC";

    // Р¤РѕСЂРјР°С‚РёСЂРѕРІР°РЅРёРµ РєСЂР°С‚РєРѕРіРѕ Р°РґСЂРµСЃР° Р±Р°Р·С‹ РІ РєРѕРјР°РЅРґРµ Р·Р°РїСѓСЃРєР°
    public static final String GENERATE_SHORT_ADDRESS_MENU_TEXT = "РђРґСЂРµСЃ РІ РІРёРґРµ /SРЎРµСЂРІРµСЂ\\Р‘Р°Р·Р° РёР»Рё /FРљР°С‚Р°Р»РѕРі";

    // РџСѓРЅРєС‚ РјРµРЅСЋ РєРЅРѕРїРєРё В«РЎС„РѕСЂРјРёСЂРѕРІР°С‚СЊВ»: С„РѕСЂРјРёСЂРѕРІР°РЅРёРµ СЃС‚СЂРѕРєРё Р·Р°РїСѓСЃРєР° СЃ РєРѕРґРѕРј
    // СЂР°Р·Р±Р»РѕРєРёСЂРѕРІРєРё (/UC) РґР»СЏ Р±Р°Р·С‹ СЃ Р±Р»РѕРєРёСЂРѕРІРєРѕР№ СѓСЃС‚Р°РЅРѕРІРєРё СЃРѕРµРґРёРЅРµРЅРёР№
    public static final String GENERATE_UNLOCK_CODE_MENU_TEXT = "РЎ РєРѕРґРѕРј СЂР°Р·Р±Р»РѕРєРёСЂРѕРІРєРё (/UC)...";
    public static final String SHORT_ADDRESS_NOT_RECOGNIZED_WARNING = "РќРµ СѓРґР°Р»РѕСЃСЊ РѕРїСЂРµРґРµР»РёС‚СЊ Р°РґСЂРµСЃ Р±Р°Р·С‹.\n"
            + "Р”Р»СЏ СЃРµСЂРІРµСЂРЅРѕР№ Р±Р°Р·С‹ Р°РґСЂРµСЃ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ СЃС‚СЂРѕРєРѕР№ РїРѕРґРєР»СЋС‡РµРЅРёСЏ РІРёРґР° Srvr=\"СЃРµСЂРІРµСЂ\";Ref=\"Р±Р°Р·Р°\"\n"
            + "РёР»Рё Р°РґСЂРµСЃРѕРј РІРёРґР° СЃРµСЂРІРµСЂ\\Р±Р°Р·Р°,\n"
            + "РґР»СЏ С„Р°Р№Р»РѕРІРѕР№ вЂ” СЃС‚СЂРѕРєРѕР№ РїРѕРґРєР»СЋС‡РµРЅРёСЏ File=\"РєР°С‚Р°Р»РѕРі\" РёР»Рё РїСѓС‚С‘Рј Рє РєР°С‚Р°Р»РѕРіСѓ Р±Р°Р·С‹.";

    // @formatter:on


    // Р—Р°РіРѕР»РѕРІРѕРє РіР»Р°РІРЅРѕРіРѕ РѕРєРЅР° (%s - РІРµСЂСЃРёСЏ РїСЂРёР»РѕР¶РµРЅРёСЏ)
    public static final String WINDOW_TITLE =
            "РџРѕСЃС‚СЂРѕРёС‚РµР»СЊ РєРѕРјР°РЅРґС‹ Р·Р°РїСѓСЃРєР° 1РЎ (РІРµСЂ. %s) - РџСЂРёРјРµСЂС‹: "
            + "File=\"C:\\1C\\Base\";  РёР»Рё  Srvr=\"127.0.0.1\";Ref=\"Base\";";

    // РўРµРєСЃС‚ РѕРєРЅР° "Рћ РїСЂРѕРіСЂР°РјРјРµ" (%s - РІРµСЂСЃРёСЏ РїСЂРёР»РѕР¶РµРЅРёСЏ)
    public static final String ABOUT_TITLE = "Рћ РїСЂРѕРіСЂР°РјРјРµ";
    public static final String ABOUT_TEXT  = """
            РџРѕСЃС‚СЂРѕРёС‚РµР»СЊ РєРѕРјР°РЅРґС‹ Р·Р°РїСѓСЃРєР° 1РЎ
            Р’РµСЂСЃРёСЏ: %s
            РџСЂРѕРіСЂР°РјРјР° РґР»СЏ СѓРґРѕР±РЅРѕРіРѕ С„РѕСЂРјРёСЂРѕРІР°РЅРёСЏ
            РєРѕРјР°РЅРґ Р·Р°РїСѓСЃРєР° 1РЎ: РџСЂРµРґРїСЂРёСЏС‚РёРµ Рё РљРѕРЅС„РёРіСѓСЂР°С‚РѕСЂ.""";

    // РЎС‚РёР»Рё РґР»СЏ РІСЃРїР»С‹РІР°СЋС‰РёС… РїРѕРґСЃРєР°Р·РѕРє (Tooltip)
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

