package l1c;

/**
 * Вспомогательная точка входа для теста запуска: печатает каждый полученный
 * аргумент отдельной строкой, чтобы увидеть, что реально дошло до процесса.
 */
public final class ArgPrinter {

    private ArgPrinter() {
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.println("arg[" + i + "]=" + args[i]);
        }
    }
}
