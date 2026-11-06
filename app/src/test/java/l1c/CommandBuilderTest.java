package l1c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты построителя команд запуска 1С. Команды проверяются целиком,
 * чтобы порядок и формат параметров соответствовал 1cestart.exe.
 */
class CommandBuilderTest {

    private static final String EXE = "C:\\1C\\1cestart.exe";

    private static CommandBuilder thinAt(String address) {
        return new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .runMode(CommandBuilder.RunMode.THIN_CLIENT)
                .connectionString(address);
    }

    @Test
    @DisplayName("Тонкий клиент: путь, режим и строка подключения с экранированием кавычек")
    void thinClientWithConnectionString() {
        String cmd = thinAt("Srvr=\"127.0.0.1\";Ref=\"Base\";").build();

        assertEquals("\"" + EXE + "\" ENTERPRISE "
                + "/IBConnectionString \"Srvr=\"\"127.0.0.1\"\";Ref=\"\"Base\"\";\"", cmd);
    }

    @Test
    @DisplayName("Режимы запуска формируют свою часть команды")
    void runModeCommandParts() {
        CommandBuilder base = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X86)
                .connectionString("File=\"C:\\Base\"");

        assertTrue(base.runMode(CommandBuilder.RunMode.DESIGNER).build().contains(" DESIGNER "));
        assertTrue(base.runMode(CommandBuilder.RunMode.THIN_CLIENT).build().contains(" ENTERPRISE /IB"));
        assertTrue(base.runMode(CommandBuilder.RunMode.THICK_ORDINARY).build()
                .contains("ENTERPRISE /RunModeOrdinaryApplication"));
        assertTrue(base.runMode(CommandBuilder.RunMode.THICK_MANAGED).build()
                .contains("ENTERPRISE /RunModeManagedApplication"));
    }

    @Test
    @DisplayName("Учётные данные: /N и /P, пустой пароль не добавляет /P")
    void credentials() {
        String cmd = thinAt("File=\"C:\\1C\\Base\"")
                .credentials(new UserCredentials("Администратор", "секрет"))
                .build();
        assertEquals("\"" + EXE + "\" ENTERPRISE /IBConnectionString \"File=\"\"C:\\1C\\Base\"\"\""
                + " /N \"Администратор\" /P \"секрет\"", cmd);

        String noPassword = thinAt("File=\"C:\\1C\\Base\"")
                .credentials(new UserCredentials("Админ", ""))
                .build();
        assertTrue(noPassword.endsWith("/N \"Админ\""), noPassword);

        // Отсутствие пользователя — ни /N, ни /P
        String withoutUser = thinAt("File=\"C:\\1C\\Base\"").credentials((UserCredentials) null).build();
        assertTrue(withoutUser.indexOf("/N") < 0 && withoutUser.indexOf("/P") < 0, withoutUser);
    }

    @Test
    @DisplayName("Приоритет разрядности добавляет /AppArch с значением платформы")
    void appArchPriority() {
        String cmd = thinAt("File=\"C:\\1C\\Base\"").appArchPriority(true).build();
        assertTrue(cmd.endsWith("/AppArch x86_64"), cmd);

        String x86 = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X86)
                .appArchPriority(true)
                .connectionString("File=\"C:\\1C\\Base\"")
                .build();
        assertTrue(x86.endsWith("/AppArch x86"), x86);
    }

    @Test
    @DisplayName("Отладка: протокол добавляется только для клиентских режимов")
    void debugMode() {
        String withProtocol = thinAt("File=\"C:\\Base\"").debug(true, "-tcp").build();
        assertTrue(withProtocol.endsWith("/Debug -attach -tcp"), withProtocol);

        // «по умолчанию» — протокол в команду не пишется
        String defaultProtocol = thinAt("File=\"C:\\Base\"")
                .debug(true, CommandBuilder.DEBUG_PROTOCOL_DEFAULT).build();
        assertTrue(defaultProtocol.endsWith("/Debug -attach"), defaultProtocol);

        // Конфигуратор и реструктуризация игнорируют отладку
        for (CommandBuilder.RunMode mode : new CommandBuilder.RunMode[] {
                CommandBuilder.RunMode.DESIGNER, CommandBuilder.RunMode.UPDATE_CONFIG }) {
            String cmd = thinAt("File=\"C:\\Base\"").runMode(mode).debug(true, "-http").build();
            assertTrue(cmd.indexOf("/Debug") < 0, mode + ": " + cmd);
        }
    }

    @Test
    @DisplayName("Реструктуризация: режим DESIGNER и /UpdateDBCfg")
    void updateConfigMode() {
        String cmd = thinAt("File=\"C:\\Base\"").runMode(CommandBuilder.RunMode.UPDATE_CONFIG).build();
        assertEquals("\"" + EXE + "\" DESIGNER /IBConnectionString \"File=\"\"C:\\Base\"\"\""
                + " /UpdateDBCfg -v2 -Server", cmd);
    }

    @Test
    @DisplayName("Порядок параметров: /N /P, /AppArch, /Debug, /ClearCache, /Execute, /UC")
    void fullCommandParameterOrder() {
        String cmd = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .runMode(CommandBuilder.RunMode.THICK_MANAGED)
                .connectionString("Srvr=\"srv\";Ref=\"Base\";")
                .credentials("admin", "pwd")
                .appArchPriority(true)
                .debug(true, "-http")
                .clearCache(true)
                .executeProcessing("  C:\\tmp\\open.bfsl  ")
                .unlockCode("123")
                .build();

        assertEquals("\"" + EXE + "\" ENTERPRISE /RunModeManagedApplication "
                + "/IBConnectionString \"Srvr=\"\"srv\"\";Ref=\"\"Base\"\";\""
                + " /N \"admin\" /P \"pwd\" /AppArch x86_64 /Debug -attach -http"
                + " /ClearCache /Execute \"C:\\tmp\\open.bfsl\" /UC \"123\"", cmd);
    }

    @Test
    @DisplayName("Команда по имени базы: /IBName")
    void ibNameAddress() {
        String cmd = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .runMode(CommandBuilder.RunMode.THICK_ORDINARY)
                .ibName("Тестовая \"база\"")
                .build();

        assertEquals("\"" + EXE + "\" ENTERPRISE /RunModeOrdinaryApplication "
                + "/IBName \"Тестовая \"\"база\"\"\"", cmd);
    }

    @Test
    @DisplayName("Краткий адрес серверной базы: /S \"сервер\\база\"")
    void shortServerAddress() {
        assertEquals("/S \"srv\\Base\"", CommandBuilder.shortAddressParam("Srvr=\"srv\";Ref=\"Base\";"));
        assertEquals("/S \"srv\\Base\"", CommandBuilder.shortAddressParam("srv\\Base"));
        assertEquals("/S \"srv\\Base\"", CommandBuilder.shortAddressParam("/S \"srv\\Base\""));
        assertEquals("/S \"srv\\Base\"", CommandBuilder.shortAddressParam("  Srvr=\"srv\" ; Ref=\"Base\"  "));
    }

    @Test
    @DisplayName("Краткий адрес файловой базы: /F \"каталог\"")
    void shortFileAddress() {
        assertEquals("/F \"C:\\1C\\Base\"", CommandBuilder.shortAddressParam("File=\"C:\\1C\\Base\""));
        // Указан файл базы — остаётся только каталог
        assertEquals("/F \"C:\\1C\\Base\"", CommandBuilder.shortAddressParam("File=\"C:\\1C\\Base\\1Cv8.1CD\""));
        // Слэши нормализуются, UNC-путь считается файловой базой
        assertEquals("/F \"C:\\1C\\Base\"", CommandBuilder.shortAddressParam("C:/1C/Base"));
        assertEquals("/F \"\\\\srv\\share\\Base\"", CommandBuilder.shortAddressParam("\\\\srv\\share\\Base"));
    }

    @Test
    @DisplayName("Краткий адрес: нераспознанный ввод даёт null")
    void shortAddressUnknown() {
        assertNull(CommandBuilder.shortAddressParam(null));
        assertNull(CommandBuilder.shortAddressParam("   "));
        assertNull(CommandBuilder.shortAddressParam("просто текст"));
        assertNull(CommandBuilder.shortAddressParam("File=\"\""));
    }

    @Test
    @DisplayName("Краткий адрес подставляется в команду как готовый параметр")
    void shortAddressInCommand() {
        String cmd = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .runMode(CommandBuilder.RunMode.THIN_CLIENT)
                .address(CommandBuilder.shortAddressParam("srv\\Base"))
                .build();

        assertEquals("\"" + EXE + "\" ENTERPRISE /S \"srv\\Base\"", cmd);
    }
}
