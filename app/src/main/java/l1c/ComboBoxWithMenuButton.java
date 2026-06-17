package l1c;

import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;

/**
 * Компонент для работы с адресами баз 1С:Предприятие, расширяющий стандартный ComboBox.
 * Содержит:
 * - Поле ввода адреса с историей
 * - Кнопку выбора базы из списка зарегистрированных (…​)
 * - Кнопку контекстного меню для быстрых действий (☰)
 */
class ComboBoxWithMenuButton<T> extends ComboBoxWithButton<T> {
    
    private final Button menuButton;        // кнопка с вертикальными точками (⋮)
    private ContextMenu contextMenu;   

    public ComboBoxWithMenuButton(String exampleTooltipText) {
        this(exampleTooltipText, null);
    }
    
    public ComboBoxWithMenuButton(String exampleTooltipText, ObservableList<T> items) {
        super(exampleTooltipText, items);
        
        // Убираем исходную кнопку из родительского HBox и добавляем свои
        // В родительском классе кнопка называется choiceButton и доступна через геттер
        Button originalChoiceButton = getChoiceButton();
        
        // Создаём новую кнопку - Меню
        menuButton = new Button("☰");
        menuButton.setPrefWidth(AppConstants.CHOICE_BUTTON_WIDTH + 5);
        menuButton.setMaxWidth(AppConstants.CHOICE_BUTTON_WIDTH + 5);
        menuButton.setMinWidth(AppConstants.CHOICE_BUTTON_WIDTH+ + 5);
        menuButton.setFocusTraversable(false);
        menuButton.setCursor(javafx.scene.Cursor.HAND);
        
        // Настраиваем контекстное меню
        setupContextMenu();
        
        getChildren().add(menuButton);

        // Обновляем стили для новой компоновки
        updateCombinedStyle();
    }
    
    /**
     * Настройка меню 
     */
    private void setupContextMenu() {
        contextMenu = new ContextMenu();
        //contextMenu.setStyle("-fx-background-insets: 0, 0 4 0 4;");
        
        MenuItem clearItem = new MenuItem("Очистить");
        clearItem.setOnAction(e -> {
            getComboBox().getEditor().clear();
            getComboBox().setValue(null);
        });
        
        MenuItem pasteItem = new MenuItem("Вставить");
        pasteItem.setOnAction(e -> {
            String clipboardText = javafx.scene.input.Clipboard.getSystemClipboard().getString();
            if (clipboardText != null && !clipboardText.isEmpty()) {
                String extractedAddress = StringExtractor.extractConnectionString(clipboardText);
                if (extractedAddress != null) {
                    getComboBox().getEditor().setText(extractedAddress);
                } else {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Предупреждение");
                    alert.setHeaderText(null);
                    alert.setContentText("Не найден адрес базы 1С в буфере обмена!");
                    alert.showAndWait();
                }
            }
        });
        
        contextMenu.getItems().addAll(clearItem, pasteItem, new SeparatorMenuItem());
        menuButton.setOnAction(e -> contextMenu.show(menuButton, 
                menuButton.localToScreen(0, menuButton.getHeight()).getX(),
                menuButton.localToScreen(0, menuButton.getHeight()).getY()));
    }
    
    /**
     * Обновляет стили для трёхкомпонентного элемента
     * (переопределяет метод родителя)
     */

    // Здесь можно переопределить стили, если нужно
    // Базовая стилизация уже есть в родительском классе
    // При необходимости можно добавить специфические стили для menuButton
    private void updateCombinedStyle() {
        
        // Стиль для кнопки: без рамок прозрачная
        menuButton.setStyle(String.format("""
                        -fx-background-color: transparent;
                        -fx-border-width: 0;
                        -fx-cursor: hand;
                """));


    }
    
    public Button getMenuButton() {
        return menuButton;
    }

    public ContextMenu getContextMenu() {
        return contextMenu;
    }
}