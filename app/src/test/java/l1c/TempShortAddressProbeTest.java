/*
 * ВРЕМЕННЫЙ тест: вызывает реальные приватные методы RunYBase через рефлексию.
 * Задачa — подтвердить работоспособность разбора адреса /S и /F на реальном коде.
 */
package l1c;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class TempShortAddressProbeTest {

    @Test
    void probeRealMethods() throws Exception {
        RunYBase app = new RunYBase();

        Method build = RunYBase.class.getDeclaredMethod("buildShortAddressParam", String.class);
        build.setAccessible(true);
        Method extract = RunYBase.class.getDeclaredMethod("extractServerAndBase", String.class);
        extract.setAccessible(true);

        String[] inputs = {
                "Srvr=\"W1CBASE2022SQL\";Ref=\"Base\";",
                "Srvr=\"srv\";Ref=\"SQL2012\\db\";Usr=\"u\";",
                "srv\\db",
                "srv\\SQL2012\\db",
                "srv:1541\\db",
                "File=\"C:\\Bases\\TestDB\";",
                "File=\"C:\\Bases\\TestDB\\1Cv8.1CD\"",
                "File=\"D:/Bases/TestDB\"",
                "C:\\Bases\\TestDB",
                "\\\\SERVER\\BaseDir",
                "/S srv\\db",
                "/S\"srv\\db\"",
                "\"srv\\db\"",
                "File=C:\\Base",
                "/F\"C:\\Base\"",
                "/F C:\\Base",
                "Srvr=srv;Ref=db",
                "Srvr=\"\";Ref=\"db\";",
                "srv\\",
                "File=\"\"",
                null
        };

        StringBuilder sb = new StringBuilder("\n===== РЕАЛЬНЫЙ РАЗБОР АДРЕСА =====\n");
        for (String in : inputs) {
            Object param = build.invoke(app, in);
            Object[] sbArr = (Object[]) extract.invoke(app, in);
            String pair = sbArr == null ? "null" : ("{" + sbArr[0] + ", " + sbArr[1] + "}");
            sb.append("IN [").append(in).append("]\n   param  = ").append(param)
                    .append("\n   pair   = ").append(pair).append('\n');
        }
        sb.append("================================\n");
        System.out.println(sb);
    }
}
