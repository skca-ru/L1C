/*
 * ВРЕМЕННЫЙ тест: проверяет разбор адреса /S и /F на реальном коде CommandBuilder.
 */
package l1c;

import org.junit.jupiter.api.Test;

class TempShortAddressProbeTest {

    @Test
    void probeRealMethods() {
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
            String param = CommandBuilder.shortAddressParam(in);
            String[] pairArr = CommandBuilder.extractServerAndBase(in);
            String pair = pairArr == null ? "null" : ("{" + pairArr[0] + ", " + pairArr[1] + "}");
            sb.append("IN [").append(in).append("]\n   param  = ").append(param)
                    .append("\n   pair   = ").append(pair).append('\n');
        }
        sb.append("================================\n");
        System.out.println(sb);
    }
}
