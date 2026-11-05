package l1c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Разбор собранной команды должен быть обратен её сборке: кавычки, удвоенные
 * {@link CommandBuilder#escapeQuotes(String)}, восстанавливаются до одинарных.
 * Иначе команда, показанная в окне, запускается с другим адресом базы.
 */
class CommandBuilderParseTest {

    private static final String EXE = "C:\\1C\\1cestart.exe";

    private static String command(String address) {
        return new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .runMode(CommandBuilder.RunMode.THIN_CLIENT)
                .connectionString(address)
                .build();
    }

    @Test
    @DisplayName("Разбор снимает внешние кавычки токенов")
    void parseStripsOuterQuotes() {
        List<String> args = CommandBuilder.parse("\"" + EXE + "\" DESIGNER");

        assertEquals(Arrays.asList(EXE, "DESIGNER"), args);
    }

    @Test
    @DisplayName("Разбор сворачивает удвоенные кавычки внутри значения")
    void parseUnescapesDoubledQuotes() {
        List<String> args = CommandBuilder.parse("/IBConnectionString \"Srvr=\"\"srv\"\";Ref=\"\"Base\"\";\"");

        assertEquals(Arrays.asList("/IBConnectionString", "Srvr=\"srv\";Ref=\"Base\";"), args);
    }

    @Test
    @DisplayName("Пробел внутри значения не разбивает аргумент")
    void parseKeepsSpacesInsideQuotes() {
        List<String> args = CommandBuilder.parse(
                "\"C:\\Program Files\\1cv8\\common\\1cestart.exe\" ENTERPRISE /ClearCache");

        assertEquals(Arrays.asList("C:\\Program Files\\1cv8\\common\\1cestart.exe", "ENTERPRISE", "/ClearCache"), args);
    }

    @Test
    @DisplayName("Сборка и разбор симметричны: адрес возвращается без изменений")
    void buildParseRoundTrip() {
        String[] addresses = {
                "File=\"C:\\1C\\Base\"",
                "File=\"C:\\Моя База\"",
                "Srvr=\"127.0.0.1\";Ref=\"Base\";",
                "Srvr=\"сервер\";Ref=\"База с пробелом\";"
        };

        for (String address : addresses) {
            List<String> args = CommandBuilder.parse(command(address));

            assertEquals(Arrays.asList(EXE, "ENTERPRISE", "/IBConnectionString", address), args, address);
        }
    }

    @Test
    @DisplayName("Краткий адрес (/S, /F) тоже возвращается при разборе")
    void shortAddressRoundTrip() {
        String serverCommand = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .address(CommandBuilder.shortAddressParam("Srvr=\"srv\";Ref=\"Мой База\";"))
                .build();
        assertEquals(Arrays.asList(EXE, "ENTERPRISE", "/S", "srv\\Мой База"),
                CommandBuilder.parse(serverCommand));

        String fileCommand = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .address(CommandBuilder.shortAddressParam("File=\"C:\\1C\\Моя База\""))
                .build();
        assertEquals(Arrays.asList(EXE, "ENTERPRISE", "/F", "C:\\1C\\Моя База"),
                CommandBuilder.parse(fileCommand));
    }

    @Test
    @DisplayName("Пустая команда и null разбираются в пустой список")
    void parseEmpty() {
        assertTrue(CommandBuilder.parse(null).isEmpty());
        assertTrue(CommandBuilder.parse("").isEmpty());
        assertTrue(CommandBuilder.parse("   ").isEmpty());
    }

    @Test
    @DisplayName("Значения /N, /P, /UC экранируются так же, как адрес базы")
    void quotedParamValues() {
        String cmd = new CommandBuilder()
                .platform(EXE, CommandBuilder.ARCH_X64)
                .runMode(CommandBuilder.RunMode.THICK_ORDINARY)
                .connectionString("File=\"C:\\Base\"")
                .credentials("Админ", "p@ss\"word")
                .unlockCode("1\"2")
                .build();

        assertTrue(cmd.contains("/P \"p@ss\"\"word\""), cmd);
        assertTrue(cmd.endsWith("/UC \"1\"\"2\""), cmd);

        List<String> args = CommandBuilder.parse(cmd);
        assertEquals("Админ", args.get(args.indexOf("/N") + 1), cmd);
        assertEquals("p@ss\"word", args.get(args.indexOf("/P") + 1), cmd);
        assertEquals("1\"2", args.get(args.indexOf("/UC") + 1), cmd);
    }

    @Test
    @DisplayName("quoted, escapeQuotes и unescapeQuotes согласованы между собой")
    void quoteHelpers() {
        assertEquals("\"x\"", CommandBuilder.quoted("x"));
        assertEquals("\"File=\"\"C:\\Base\"\"\"", CommandBuilder.quoted("File=\"C:\\Base\""));
        assertEquals("File=\"C:\\Base\"", CommandBuilder.unescapeQuotes("File=\"\"C:\\Base\"\""));
    }
}
