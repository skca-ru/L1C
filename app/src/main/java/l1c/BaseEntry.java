package l1c;

/**
 * Запись зарегистрированной базы 1С из файла ibases.v8i.
 */
public final class BaseEntry {
    public final String name;
    public final String connect;
    public final double order;
    public String note;

    public BaseEntry(String name, String connect, double order) {
        this.name = name;
        this.connect = connect;
        this.order = order;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
