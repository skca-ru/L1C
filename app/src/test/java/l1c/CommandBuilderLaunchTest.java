package l1c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Проверка запуска собранной команды: процесс должен получить ровно те аргументы,
 * которые {@link CommandBuilder#parse(String)} возвращает для этой строки.
 * <p>
 * Раньше запуск шёл списком разобранных аргументов, и кавычки внутри адреса базы
 * терялись (File=""C:\Base"" доходило до процесса как File=C:\Base), поэтому
 * команда, показанная в окне, не совпадала с запускаемой.
 */
@EnabledOnOs(OS.WINDOWS)
class CommandBuilderLaunchTest {

    private static final String EXE_SAMPLE = "C:\\Program Files\\1cv8\\common\\1cestart.exe";

    /** Параметры команды в том виде, в каком их формирует CommandBuilder. */
    private static final String PARAMS = CommandBuilder.connectionStringParam("File=\"C:\\My Base\"")
            + " /N " + CommandBuilder.quoted("Admin")
            + " /P " + CommandBuilder.quoted("p@ss\"1");

    @Test
    @DisplayName("Аргументы процесса совпадают с разбором команды")
    void launchedProcessReceivesParsedArguments() throws Exception {
        String command = '"' + javaExecutable() + "\" -cp \"" + classpath() + "\" l1c.ArgPrinter " + PARAMS;

        List<String> received = run(command);

        assertEquals(CommandBuilder.parse(PARAMS), received, "команда: " + command);
        assertTrue(received.contains("File=\"C:\\My Base\""), received.toString());
        assertTrue(received.contains("p@ss\"1"), received.toString());
    }

    @Test
    @DisplayName("Команда запускается через cmd.exe целой строкой")
    void commandIsPassedToCmdVerbatim() {
        String command = "\"" + EXE_SAMPLE + "\" ENTERPRISE " + PARAMS;

        assertEquals(Arrays.asList("cmd.exe", "/c", "\"" + command + "\""),
                CommandBuilder.toProcessCommand(command));
    }

    /** Печатает аргументы дочернего процесса по одному на строку: arg[индекс]=значение. */
    private static List<String> run(String command) throws Exception {
        Process process = new ProcessBuilder(CommandBuilder.toProcessCommand(command))
                .redirectErrorStream(true).start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                lines.add(line);
        }
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Дочерний процесс не завершился");
        assertEquals(0, process.exitValue(), lines.toString());

        List<String> values = new ArrayList<>();
        for (String line : lines) {
            int separator = line.indexOf('=');
            if (line.startsWith("arg[") && separator > 0)
                values.add(line.substring(separator + 1));
        }
        return values;
    }

    private static String javaExecutable() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
    }

    /** Только классы приложения и тестов — командная строка Windows ограничена по длине. */
    private static String classpath() throws Exception {
        return classesOf(CommandBuilder.class) + File.pathSeparator + classesOf(ArgPrinter.class);
    }

    private static String classesOf(Class<?> type) throws Exception {
        return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    }
}
