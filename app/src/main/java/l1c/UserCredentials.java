package l1c;

/**
 * Учётные данные пользователя 1С (имя + пароль + флаг главного).
 * Главный пользователь помечается как "по умолчанию" и используется
 * при формировании команды запуска, если пользователь не выбран вручную.
 */
public class UserCredentials {
    private final String username;
    private final String password;
    private boolean main; // главный пользователь (по умолчанию)

    /**
     * Создаёт учётные данные без пометки "главный".
     */
    public UserCredentials(String username, String password) {
        this(username, password, false);
    }

    /**
     * Создаёт учётные данные с пометкой "главный".
     */
    public UserCredentials(String username, String password, boolean main) {
        this.username = username;
        this.password = password;
        this.main = main;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }
}
