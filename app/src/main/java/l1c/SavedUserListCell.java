package l1c;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Клетка ListView для отображения сохранённого пользователя.
 */
public class SavedUserListCell extends ListCell<SavedUserEntry> {
    private final Label userLabel = new Label();
    private final Label addressLabel = new Label();
    private final VBox container = new VBox(2, userLabel, addressLabel);

    public SavedUserListCell() {
        container.setPadding(new Insets(5, 10, 5, 10));
        userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        addressLabel.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");
        addressLabel.setPadding(new Insets(0, 0, 0, 20));
        addressLabel.setWrapText(true);
        addressLabel.setMaxWidth(600);
    }

    @Override
    protected void updateItem(SavedUserEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        String mark = item.forCurrentAddress ? "  (текущая база)" : "";
        userLabel.setText("\uD83D\uDC64 " + item.username + mark + (item.password.isEmpty() ? "" : "  \uD83D\uDD11"));

        String location = item.baseName != null ? item.baseName + " — " + item.address : item.address;
        addressLabel.setText(location);

        if (isSelected()) {
            container.setStyle("-fx-background-color: -fx-selection-bar;");
            userLabel.setTextFill(Color.WHITE);
            addressLabel.setTextFill(Color.WHITE);
        } else {
            container.setStyle("-fx-background-color: transparent;");
            userLabel.setTextFill(Color.BLACK);
            addressLabel.setTextFill(Color.BLACK);
        }
        setGraphic(container);
    }
}
