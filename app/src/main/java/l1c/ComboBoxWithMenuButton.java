package l1c;

import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;

/**
 * Компонент ComboBox с двумя дополнительными кнопками справа:
 * - Кнопка с тремя горизонтальными точками (…) для выбора из списка (унаследована)
 * - Кнопка с тремя вертикальными точками (⋮) для контекстного меню
 */
class ComboBoxWithMenuButton<T> extends ComboBoxWithButton<T> {
    
    private final Button menuButton;        // кнопка с вертикальными точками (⋮)
    
    public ComboBoxWithMenuButton(String exampleTooltipText) {
        this(exampleTooltipText, null);
    }
    
    public ComboBoxWithMenuButton(String exampleTooltipText, ObservableList<T> items) {
        super(exampleTooltipText, items);
        
        // Убираем исходную кнопку из родительского HBox и добавляем свои
        // В родительском классе кнопка называется choiceButton и доступна через геттер
        Button originalChoiceButton = getChoiceButton();
        
        // Создаём новую кнопку с вертикальными точками
        //menuButton = new Button("\u2630");
        menuButton = new Button("☰");
        menuButton.setPrefWidth(AppConstants.CHOICE_BUTTON_WIDTH + 5);
        menuButton.setMaxWidth(AppConstants.CHOICE_BUTTON_WIDTH + 5);
        menuButton.setMinWidth(AppConstants.CHOICE_BUTTON_WIDTH+ + 5);
        menuButton.setFocusTraversable(false);
        //menuButton.setCursor(javafx.scene.Cursor.HAND);
        
        // Настраиваем контекстное меню
        setupContextMenu();
        
        // Перестраиваем HBox: удаляем старую кнопку и добавляем две
        getChildren().remove(originalChoiceButton);
        getChildren().remove(menuButton);getChildren().addAll(originalChoiceButton, menuButton);
        //getChildren().add(menuButton);

        // Обновляем стили для новой компоновки
        updateCombinedStyle(false);
    }
    
    /**
     * Настройка контекстного меню для кнопки с вертикальными точками
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem clearItem = new MenuItem("Очистить");
        clearItem.setOnAction(e -> {
            getComboBox().getEditor().clear();
            getComboBox().setValue(null);
        });
        
        MenuItem pasteItem = new MenuItem("Вставить");
        pasteItem.setOnAction(e -> {
            String clipboardText = javafx.scene.input.Clipboard.getSystemClipboard().getString();
            if (clipboardText != null && !clipboardText.isEmpty()) {
                getComboBox().getEditor().setText(clipboardText);
            }
        });
        
        contextMenu.getItems().addAll(clearItem, new SeparatorMenuItem(), pasteItem);
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
    private void updateCombinedStyle(boolean focused) {
        String borderColor = focused ? AppConstants.FOCUS_BORDER_COLOR : AppConstants.NORMAL_BORDER_COLOR;

        // Стиль для кнопки: правая, верхняя, нижняя границы + скругление справа
        menuButton.setStyle(String.format("""
                        -fx-background-color: white;
                        -fx-border-color: %s %s %s %s;
                        -fx-border-width: 1px 1px 1px 0;
                        -fx-border-radius: 0 3px 3px 0;
                        -fx-background-radius: 0 3px 3px 0;
                        -fx-cursor: hand;
                """, borderColor, borderColor, borderColor, borderColor));


    }
    
    public Button getMenuButton() {
        return menuButton;
    }
}