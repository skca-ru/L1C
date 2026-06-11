package l1c;

import javafx.scene.layout.Priority;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.collections.ObservableList;
import javafx.scene.layout.Priority;

/**
 * Компонент ComboBox с дополнительной кнопкой справа
 * Используется для выбора адресов баз 1С:Предприятие
 */
class ComboBoxWithButton<T> extends HBox {
    private static final int CHOICE_BUTTON_WIDTH = 25;
    private final ComboBox<T> comboBox;
    private final Button choiceButton;
    private String adressInfoBase;
    private final Tooltip dynamicTooltip;
    private final String exampleTooltipText;

    // Цвета для рамки при фокусе
    private static final String FOCUS_BORDER_COLOR = "#0078D7";
    private static final String NORMAL_BORDER_COLOR = "#a0a0a0";

    public ComboBoxWithButton(String exampleTooltipText) {
        this(exampleTooltipText, null);
    }
    public ComboBoxWithButton(String exampleTooltipText, 
        ObservableList<T> items) {
        super(0); // без промежутка
        // без рамок
        setStyle("""
                    -fx-border-color: white;
                    -fx-border-width: 0;
                """);
        
        this.exampleTooltipText = exampleTooltipText;
        
        comboBox = new ComboBox<>();
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setEditable(true);
        comboBox.setPromptText(RunYBaseHelpTexts.BASE_CONNECTION_PROMPT);
        if (items != null) {
            comboBox.setItems(items);
            
        }
        // Создаём один тултип на все случаи
        dynamicTooltip = new Tooltip(exampleTooltipText);
        dynamicTooltip.setStyle(AppConstants.TOOLTIP_STYLE);
        // TO DO Непонятное не работает кажется 
        dynamicTooltip.setWrapText(true);
        comboBox.setTooltip(dynamicTooltip);

        // Показывать tooltip если необходимо
        comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            updateTooltip();
        });

        choiceButton = new Button("…");
        choiceButton.setPrefWidth(CHOICE_BUTTON_WIDTH);
        choiceButton.setMaxWidth(CHOICE_BUTTON_WIDTH);
        choiceButton.setMinWidth(CHOICE_BUTTON_WIDTH);

        // Установка нормальных стилей
        updateBorderStyle(false);

        // Добавление слушателя фокуса на поле ввода ComboBox
        comboBox.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            updateBorderStyle(isNowFocused);
        });

        // Также слушаем фокус на самой кнопке (на случай если пользователь кликнет на неё)
        choiceButton.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            updateBorderStyle(isNowFocused);
        });

        getChildren().addAll(comboBox, choiceButton);
        HBox.setHgrow(comboBox, Priority.ALWAYS);
        setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Обновляет tooltip в зависимости от состояния полей
     */
    private void updateTooltip() {
        String currentText = comboBox.getEditor().getText();

        if (currentText == null || currentText.trim().isEmpty()) {
            // Поле пустое → показываем пример
            dynamicTooltip.setText(exampleTooltipText);
            comboBox.setTooltip(dynamicTooltip);
        } else if (adressInfoBase != null && !adressInfoBase.trim().isEmpty()) {
            // Поле заполнено и есть адрес → показываем адрес
            dynamicTooltip.setText(adressInfoBase);
            comboBox.setTooltip(dynamicTooltip);
        } else {
            // Поле заполнено, но адрес пустой → скрываем tooltip
            comboBox.setTooltip(null);
        }
    }

    public void setAdressIB(String adress) {
        this.adressInfoBase = adress;
        updateTooltip();
    }

    /**
     * Обновляет стили границ для comboBox и кнопки
     * 
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
        choiceButton.setStyle(String.format("""
                        -fx-background-color: white;
                        -fx-border-color: %s %s %s %s;
                        -fx-border-width: 1px 1px 1px 0;
                        -fx-border-radius: 0 3px 3px 0;
                        -fx-background-radius: 0 3px 3px 0;
                        -fx-cursor: hand;
                """, borderColor, borderColor, borderColor, borderColor));
    }

    public ComboBox<T> getComboBox() {
        return comboBox;
    }

    public Button getChoiceButton() {
        return choiceButton;
    }
}