package l1c;

import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * Компонент ComboBox с дополнительной кнопкой справа
 * Используется для выбора адресов баз 1С:Предприятие
 */
class ComboBoxWithButton<T> extends HBox {
    private static final int ACTION_BUTTON_WIDTH = 25;
    private final ComboBox<T> comboBox;
    private final Button choiseButton;
    
    // Цвета для рамки при фокусе
    private static final String FOCUS_BORDER_COLOR = "#0078D7";
    private static final String NORMAL_BORDER_COLOR = "#a0a0a0";
    
    public ComboBoxWithButton(String exampleTooltipText) {
        super(0); // без промежутка
        // без рамок
        setStyle("""
            -fx-border-color: white;
            -fx-border-width: 0;
        """);
        comboBox = new ComboBox<>();
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setEditable(true);
        comboBox.setPromptText(RunYBaseHelpTexts.BASE_CONNECTION_PROMPT);
        
        Tooltip exampleTooltip = new Tooltip(exampleTooltipText);
        exampleTooltip.setStyle("""
                -fx-background-color: #F3E4BC;
                -fx-text-fill: #000000;
                -fx-border-color: #C0A050;
                -fx-border-radius: 3px;
                -fx-background-radius: 3px;
                -fx-padding: 5 10 5 10;
                -fx-font-size: 13px;
        """);
        comboBox.setTooltip(exampleTooltip);
        
        // Показывать tooltip только когда поле пустое
        comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                comboBox.setTooltip(exampleTooltip);
            } else {
                comboBox.setTooltip(null);
            }
        });
        
        choiseButton = new Button("…");
        choiseButton.setPrefWidth(ACTION_BUTTON_WIDTH);
        choiseButton.setMaxWidth(ACTION_BUTTON_WIDTH);
        choiseButton.setMinWidth(ACTION_BUTTON_WIDTH);
        
        // Установка нормальных стилей
        updateBorderStyle(false);
        
        // Добавление слушателя фокуса на поле ввода ComboBox
        comboBox.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            updateBorderStyle(isNowFocused);
        });
        
        // Также слушаем фокус на самой кнопке (на случай если пользователь кликнет на неё)
        choiseButton.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            updateBorderStyle(isNowFocused);
        });
        
        getChildren().addAll(comboBox, choiseButton);
        HBox.setHgrow(comboBox, Priority.ALWAYS);
        setMaxWidth(Double.MAX_VALUE);
    }
    
    /**
     * Обновляет стили границ для comboBox и кнопки
     * @param focused находится ли в фокусе поле ввода или кнопка
     */
    private void updateBorderStyle(boolean focused) {
        String borderColor = focused ? FOCUS_BORDER_COLOR : NORMAL_BORDER_COLOR;
        
        // Стиль для ComboBox: левая, верхняя, нижняя границы + скругление слева
        comboBox.setStyle(String.format("""
                -fx-border-color: %s %s %s %s;
                -fx-border-width: 1px 0 1px 1px;
                -fx-border-radius: 3px 0 0 3px;
                -fx-background-radius: 3px 0 0 3px;
                -fx-background-color: white;
        """, borderColor, borderColor, borderColor, borderColor));
        
        // Стилизация кнопки-стрелки через CSS
        var arrowButton = comboBox.lookup(".arrow-button");
        if (arrowButton != null) {
            arrowButton.setStyle(String.format("""
                -fx-background-color: white;
                -fx-border-width: 0 1px 0 1px;            
                -fx-border-color: black %s black %s;
            """, NORMAL_BORDER_COLOR, NORMAL_BORDER_COLOR));
        }
 
        // Стиль для кнопки: правая, верхняя, нижняя границы + скругление справа
        choiseButton.setStyle(String.format("""
                -fx-background-color: white;
                -fx-border-color: %s %s %s %s;
                -fx-border-width: 1px 1px 1px 0;
                -fx-border-radius: 0 3px 3px 0;
                -fx-background-radius: 0 3px 3px 0;
                -fx-cursor: hand;
        """, borderColor, borderColor, borderColor, borderColor));
    }
    
    public ComboBox<T> getComboBox() { return comboBox; }
    public Button getChoiseButton() { return choiseButton; }
}
