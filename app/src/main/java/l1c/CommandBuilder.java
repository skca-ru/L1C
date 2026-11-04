package l1c;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Построитель команд запуска 1С:Предприятие 8.
 * <p>
 * Класс не зависит от интерфейса: все параметры задаются явно, поэтому логика
 * формирования строки запуска отделена от JavaFX и может тестироваться
 * независимо от {@link RunYBase}.
 * <p>
 * Пример использования:
 * 
 * <pre>
 * String cmd = new CommandBuilder()
 *         .platform(CommandBuilder.PATH_1CESTART_X64, CommandBuilder.ARCH_X64)
 *         .runMode(CommandBuilder.RunMode.THICK_MANAGED)
 *         .connectionString("Srvr=\"127.0.0.1\";Ref=\"Base\";")
 *         .credentials(cred)
 *         .appArchPriority(true)
 *         .debug(true, "-tcp")
 *         .build();
 * </pre>
 */
public final class CommandBuilder {

    // @formatter:off
    // Типовые пути программы запуска 1С и соответствующие значения /AppArch
    public static final String PATH_1CESTART_X86 = "C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe";
    public static final String PATH_1CESTART_X64 = "C:\\Program Files\\1cv8\\common\\1cestart.exe";
    public static final String ARCH_X86          = "x86";
    public static final String ARCH_X64          = "x86_64";

    // Значение протокола отладки, при котором протокол в команду не добавляется
    public static final String DEBUG_PROTOCOL_DEFAULT = "по умолчанию";
    // @formatter:on

    private static final Pattern FILE_CONNECTION_PATTERN = Pattern.compile("File\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern SERVER_CONNECTION_PATTERN = Pattern
            .compile("Srvr\\s*=\\s*\"([^\"]*)\"\\s*;\\s*Ref\\s*=\\s*\"([^\"]*)\"");
    private static final String FILE_DATABASE_NAME = "\\1cv8.1cd";

    /**
     * Режим запуска — определяет часть команды сразу после пути к 1cestart.exe.
     */
    public enum RunMode {
        /** Конфигуратор. */
        DESIGNER("DESIGNER", true, false),
        /** Тонкий клиент. */
        THIN_CLIENT("ENTERPRISE", false, false),
        /** Толстый клиент — обычное приложение. */
        THICK_ORDINARY("ENTERPRISE /RunModeOrdinaryApplication", false, false),
        /** Толстый клиент — управляемое приложение. */
        THICK_MANAGED("ENTERPRISE /RunModeManagedApplication", false, false),
        /** Реструктуризация: запускается из Конфигуратора, но добавляет /UpdateDBCfg. */
        UPDATE_CONFIG("DESIGNER", true, true);

        private final String commandPart;
        private final boolean designer;
        private final boolean updateConfig;

        RunMode(String commandPart, boolean designer, boolean updateConfig) {
            this.commandPart = commandPart;
            this.designer = designer;
            this.updateConfig = updateConfig;
        }

        /** Часть команды запуска для этого режима. */
        public String commandPart() {
            return commandPart;
        }

        /** Конфигуратор или реструктуризация (запуск из DESIGNER). */
        public boolean isDesigner() {
            return designer;
        }

        /** Нужна ли реструктуризация базы (/UpdateDBCfg). */
        public boolean isUpdateConfig() {
            return updateConfig;
        }

        /** Режим отладки игнорируется Конфигуратором и реструктуризацией. */
        public boolean supportsDebug() {
            return !designer && !updateConfig;
        }
    }

    private String platformPath = PATH_1CESTART_X64;
    private String appArch = ARCH_X64;
    private RunMode runMode = RunMode.THIN_CLIENT;
    private String addressParam;
    private String username;
    private String password;
    private boolean appArchPriority;
    private boolean debugEnabled;
    private String debugProtocol;
    private boolean clearCache;
    private String processingPath;
    private String unlockCode;

    /** Путь к запускаемому файлу платформы и его разрядность для /AppArch. */
    public CommandBuilder platform(String platformPath, String appArch) {
        this.platformPath = platformPath;
        this.appArch = appArch;
        return this;
    }

    /** Режим запуска. */
    public CommandBuilder runMode(RunMode runMode) {
        this.runMode = runMode == null ? RunMode.THIN_CLIENT : runMode;
        return this;
    }

    /** Готовый параметр адреса базы (например, {@code /S "сервер\база"}). */
    public CommandBuilder address(String addressParam) {
        this.addressParam = addressParam;
        return this;
    }

    /** Адрес базы строкой подключения: {@code /IBConnectionString "..."}. */
    public CommandBuilder connectionString(String rawAddress) {
        return address(connectionStringParam(rawAddress));
    }

    /** Адрес базы по имени: {@code /IBName "..."}. */
    public CommandBuilder ibName(String baseName) {
        return address(ibNameParam(baseName));
    }

    /** Учётные данные пользователя ({@code /N} и, при непустом пароле, {@code /P}). */
    public CommandBuilder credentials(UserCredentials cred) {
        if (cred == null)
            return credentials(null, null);
        return credentials(cred.getUsername(), cred.getPassword());
    }

    /** Учётные данные пользователя ({@code /N} и, при непустом пароле, {@code /P}). */
    public CommandBuilder credentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    /** Добавлять ли {@code /AppArch} — «приоритет платформы указанной разрядности». */
    public CommandBuilder appArchPriority(boolean enabled) {
        this.appArchPriority = enabled;
        return this;
    }

    /**
     * Режим отладки {@code /Debug -attach}. Протокол ({@code -tcp}, {@code -http})
     * не добавляется, если он пуст или равен {@link #DEBUG_PROTOCOL_DEFAULT}.
     */
    public CommandBuilder debug(boolean enabled, String protocol) {
        this.debugEnabled = enabled;
        this.debugProtocol = protocol;
        return this;
    }

    /** Добавлять ли {@code /ClearCache}. */
    public CommandBuilder clearCache(boolean enabled) {
        this.clearCache = enabled;
        return this;
    }

    /** Путь к внешней обработке для {@code /Execute} (пустой путь — параметр не добавляется). */
    public CommandBuilder executeProcessing(String processingPath) {
        this.processingPath = processingPath;
        return this;
    }

    /** Код разблокировки установления соединений {@code /UC} (для блокировки connections). */
    public CommandBuilder unlockCode(String unlockCode) {
        this.unlockCode = unlockCode;
        return this;
    }

    /**
     * Собирает строку команды запуска из установленных параметров.
     * Порядок параметров соответствует формату командной строки 1cestart.exe.
     */
    public String build() {
        StringBuilder cmd = new StringBuilder();
        cmd.append('"').append(platformPath).append("\" ");
        cmd.append(runMode.commandPart()).append(' ');
        if (addressParam != null && !addressParam.isEmpty()) {
            cmd.append(addressParam);
        }

        if (username != null && !username.isEmpty()) {
            cmd.append(" /N \"").append(username).append('"');
            if (password != null && !password.isEmpty()) {
                cmd.append(" /P \"").append(password).append('"');
            }
        }

        if (appArchPriority) {
            cmd.append(" /AppArch ").append(appArch);
        }

        if (debugEnabled && runMode.supportsDebug()) {
            cmd.append(" /Debug -attach");
            if (debugProtocol != null && !debugProtocol.isEmpty()
                    && !DEBUG_PROTOCOL_DEFAULT.equals(debugProtocol)) {
                cmd.append(' ').append(debugProtocol);
            }
        }

        if (clearCache) {
            cmd.append(" /ClearCache");
        }

        if (runMode.isUpdateConfig()) {
            cmd.append(" /UpdateDBCfg -v2 -Server");
        }

        if (processingPath != null && !processingPath.trim().isEmpty()) {
            cmd.append(" /Execute \"").append(processingPath.trim()).append('"');
        }

        if (unlockCode != null && !unlockCode.isEmpty()) {
            cmd.append(" /UC \"").append(unlockCode).append('"');
        }

        return cmd.toString();
    }

    // #region ========== Параметры адреса ==========

    /**
     * Формирует параметр {@code /IBConnectionString "..."}; кавычки внутри адреса удваиваются.
     */
    public static String connectionStringParam(String rawAddress) {
        return "/IBConnectionString \"" + escapeQuotes(rawAddress) + "\"";
    }

    /**
     * Формирует параметр {@code /IBName "..."}; кавычки внутри имени удваиваются.
     */
    public static String ibNameParam(String baseName) {
        return "/IBName \"" + escapeQuotes(baseName) + "\"";
    }

    /**
     * Формирует краткий параметр адреса базы:
     * <ul>
     * <li>{@code /S "сервер\база"} — для серверной базы;</li>
     * <li>{@code /F "каталог"} — для файловой базы (путь без имени файла 1Cv8.1CD).</li>
     * </ul>
     * Поддерживаемые форматы адреса:
     * <ul>
     * <li>серверная: {@code Srvr="сервер";Ref="база"}, {@code сервер\база} (в том числе с префиксом /S);</li>
     * <li>файловая: {@code File="каталог"}, путь к каталогу (локальный или UNC).</li>
     * </ul>
     *
     * @return готовый параметр адреса или null, если формат не распознан
     */
    public static String shortAddressParam(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty())
            return null;

        // Файловая база, заданная строкой подключения File="..."
        Matcher fileM = FILE_CONNECTION_PATTERN.matcher(t);
        if (fileM.find()) {
            String dir = fileM.group(1).trim().replace('/', '\\');
            // Если указан файл базы 1Cv8.1CD — оставляем только каталог
            if (dir.toLowerCase(Locale.ROOT).endsWith(FILE_DATABASE_NAME)) {
                dir = dir.substring(0, dir.length() - FILE_DATABASE_NAME.length());
            }
            if (dir.isEmpty())
                return null;
            return "/F \"" + escapeQuotes(dir) + "\"";
        }

        // Файловая база — путь к каталогу (локальный C:\... или UNC \\сервер\папка)
        String dir = StringExtractor.extractDirectoryPath(t);
        if (dir != null && !dir.isEmpty())
            return "/F \"" + escapeQuotes(dir) + "\"";

        // Серверная база — Srvr="...";Ref="..." или сервер\база
        String[] serverBase = extractServerAndBase(t);
        if (serverBase != null && !serverBase[0].isEmpty() && !serverBase[1].isEmpty()) {
            String serverAddress = serverBase[0] + "\\" + serverBase[1];
            return "/S \"" + escapeQuotes(serverAddress) + "\"";
        }

        return null;
    }

    /**
     * Извлекает сервер и имя базы из адреса.
     * Поддерживаемые форматы:
     * <ul>
     * <li>строка подключения: {@code Srvr="сервер";Ref="база";}</li>
     * <li>адрес вида {@code сервер\база} (в том числе с префиксом /S).</li>
     * </ul>
     *
     * @return массив {сервер, база} или null, если формат не распознан
     */
    public static String[] extractServerAndBase(String text) {
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
        Matcher m = SERVER_CONNECTION_PATTERN.matcher(t);
        if (m.find())
            return new String[] { m.group(1).trim(), m.group(2).trim() };

        // Адрес вида сервер\база (UNC-пути \\сервер\папка — это файловые базы, не подходит)
        int slashIdx = t.indexOf('\\');
        if (slashIdx > 0 && !t.startsWith("\\\\")) {
            return new String[] { t.substring(0, slashIdx).trim(), t.substring(slashIdx + 1).trim() };
        }

        return null;
    }

    /** Удваивает кавычки — экранирование значения внутри строки подключения. */
    public static String escapeQuotes(String value) {
        return value == null ? "" : value.replace("\"", "\"\"");
    }

    // #endregion
}
