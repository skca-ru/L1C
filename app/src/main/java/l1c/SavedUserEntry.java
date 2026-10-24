package l1c;

/**
 * Запись сохранённого пользователя для отображения в списке.
 */
public final class SavedUserEntry {
    public final String username;
    public final String password;
    public final String address;
    public final String baseName;
    public final boolean forCurrentAddress;

    public SavedUserEntry(String username, String password, String address, String baseName, boolean forCurrentAddress) {
        this.username = username;
        this.password = password;
        this.address = address;
        this.baseName = baseName;
        this.forCurrentAddress = forCurrentAddress;
    }
}
