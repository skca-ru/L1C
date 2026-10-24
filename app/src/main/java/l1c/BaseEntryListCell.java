package l1c;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Клетка ListView для отображения зарегистрированной базы 1С с заметкой.
 */
public class BaseEntryListCell extends ListCell<BaseEntry> {
    private final Label nameLabel = new Label();
    private final Label connectLabel = new Label();
    private final Label noteLabel = new Label();
    private final VBox container = new VBox(2, nameLabel, connectLabel, noteLabel);

    public BaseEntryListCell() {
        container.setPadding(new Insets(5, 10, 5, 10));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        connectLabel.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");
        connectLabel.setPadding(new Insets(0, 0, 0, 20));
        noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #C8A046; -fx-font-style: italic;");
        noteLabel.setPadding(new Insets(0, 0, 0, 20));
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(500);
    }

    @Override
    protected void updateItem(BaseEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(item.name);
            connectLabel.setText(item.connect);
            if (item.note != null && !item.note.isEmpty()) {
                // Обрезаем длинную заметку для отображения
                String displayNote = item.note.length() > 80 ? item.note.substring(0, 80) + "..." : item.note;
                noteLabel.setText("\uD83D\uDCDD " + displayNote);
                noteLabel.setVisible(true);
            } else {
                noteLabel.setVisible(false);
            }
            if (isSelected()) {
                container.setStyle("-fx-background-color: -fx-selection-bar;");
                nameLabel.setTextFill(Color.WHITE);
                connectLabel.setTextFill(Color.WHITE);
                noteLabel.setTextFill(Color.WHITE);
            } else {
                container.setStyle("-fx-background-color: transparent;");
                nameLabel.setTextFill(Color.BLACK);
                connectLabel.setTextFill(Color.BLACK);
                noteLabel.setTextFill(Color.GRAY);
            }
            setGraphic(container);
        }
    }
}
