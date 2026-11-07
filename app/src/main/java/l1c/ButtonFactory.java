package l1c;

import java.util.Objects;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * Фабрика создания и стилизации кнопок приложения.
 *
 * <p>Централизует все стили кнопок, которые раньше были разбросаны по коду:
 * <ul>
 *   <li>{@link #create(String)} — основная жёлтая кнопка с эффектами наведения и нажатия;</li>
 *   <li>{@link #createDialogButton(String)} — "нативная" кнопка диалогов (стиль Windows 11);</li>
 *   <li>{@link #createFlat(String)} — плоская кнопка;</li>
 *   <li>{@link #createSmall(String)} — компактная кнопка фиксированного размера;</li>
 *   <li>{@link #createHelp()} — круглая кнопка-справка "?".</li>
 * </ul>
 *
 * <p>Все методы возвращают готовую кнопку; дополнительные настройки
 * (размер, подсказка, обработчик) навешиваются fluent-методами:
 * <pre>{@code
 * Button run = ButtonFactory.create("Run")
 *         .apply(b -> b.setPrefWidth(80));
 * }</pre>
 */
public final class ButtonFactory {

    /** Минимальная высота стандартной кнопки, px */
    private static final int MIN_BUTTON_HEIGHT = 30;

    /** Минимальная высота диалоговой кнопки, px */
    private static final int MIN_DIALOG_BUTTON_HEIGHT = 30;

    /** Минимальная ширина кнопки диалога (Отмена/Закрыть), px */
    private static final int MIN_DIALOG_BUTTON_WIDTH = 80;

    /** Единая минимальная ширина кнопок в диалогах, px */
    private static final double DIALOG_BUTTON_WIDTH = 100;

    /** Скругление углов основной/плоской кнопки, px */
    private static final int CORNER_RADIUS = 3;

    /** Скругление углов диалоговой кнопки, px */
    private static final int DIALOG_CORNER_RADIUS = 4;

    private ButtonFactory() {
        // Утилитный класс — инстанцирование запрещено
    }

    // ==================================================================
    // Фабричные методы
    // ==================================================================

    /**
     * Создаёт основную кнопку приложения (жёлтая палитра).
     *
     * <p>Стиль и эффекты (подсветка фона при наведении, сдвиг при нажатии)
     * задаются классом {@code .main-button} в файле {@code app.css}:
     * инлайн-стиль не поддерживает селекторы состояния (:hover/:armed).
     *
     * @param text текст кнопки (мнемоники обрабатываются)
     * @return стилизованная кнопка
     */
    public static Button create(String text) {
        return create(text, AppConstants.COLOR_BUTTON_BG);
    }

    /**
     * Создаёт основную кнопку приложения с заданным цветом фона.
     *
     * <p>Базовый цвет передаётся в stylesheet как looked-up цвет
     * {@code -btn-base-color}; псевдокласс :hover в {@code app.css}
     * осветляет его через {@code derive()}.
     *
     * @param text    текст кнопки
     * @param bgColor HEX-цвет фона (например {@code "#E6C878"})
     * @return стилизованная кнопка
     */
    public static Button create(String text, String bgColor) {
        Objects.requireNonNull(bgColor, "bgColor");

        Button button = new Button(text);
        // Чтобы ускорители (мнемоники) на кнопке работали
        button.setMnemonicParsing(true);

        button.getStyleClass().add("main-button");
        button.setMinHeight(MIN_BUTTON_HEIGHT);

        // Базовый цвет фона — looked-up цвет, состояния расписаны в app.css
        button.setStyle("-btn-base-color: " + bgColor + ";");

        return button;
    }

    /**
     * Создаёт кнопку диалогов в "нативном" стиле Windows 11 (светлая тема):
     * белый фон со скруглением 4 px, более тёмный нижний край рамки,
     * затемнение при наведении и нажатии.
     *
     * <p>Состояния читаются из свойств кнопки ({@code hover}/{@code pressed}),
     * а не из обработчиков мыши — иначе при уходе курсора во время нажатия
     * кнопка останется тёмной.
     *
     * @param text текст кнопки
     * @return стилизованная кнопка
     */
    public static Button createDialogButton(String text) {
        Button button = new Button(text);
        button.setMnemonicParsing(true);
        button.setMinWidth(MIN_DIALOG_BUTTON_WIDTH);
        button.setMinHeight(MIN_DIALOG_BUTTON_HEIGHT);

        Runnable repaint = () -> applyNativeButtonStyle(button);
        button.hoverProperty().addListener((obs, was, now) -> repaint.run());
        button.pressedProperty().addListener((obs, was, now) -> repaint.run());
        repaint.run();

        return button;
    }

    /**
     * Кнопка подтверждения в диалогах (OK в выборе базы, Выбрать в истории).
     * Основная кнопка в светлой жёлтой палитре
     * ({@link AppConstants#COLOR_BUTTON_SMALL_BG}).
     *
     * @param text текст кнопки
     * @return стилизованная кнопка
     */
    public static Button createDialogConfirmButton(String text) {
        Button button = create(text, AppConstants.COLOR_BUTTON_SMALL_BG);
        button.setMinWidth(DIALOG_BUTTON_WIDTH);
        return button;
    }

    /**
     * Создаёт плоскую кнопку (белый фон, серая рамка) — стиль полей ввода.
     *
     * @param text текст кнопки
     * @return стилизованная кнопка
     */
    public static Button createFlat(String text) {
        Button button = new Button(text);
        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 12px;
                -fx-border-color: gray;
                -fx-border-radius: %dpx;
                -fx-background-radius: %dpx""",
                AppConstants.COLOR_INPUT_BG, AppConstants.COLOR_TEXT_FG,
                CORNER_RADIUS, CORNER_RADIUS));
        return button;
    }

    /**
     * Создаёт компактную кнопку фиксированного размера (30x22)
     * в светлой жёлтой палитре — например, кнопка истории "И...".
     *
     * @param text текст кнопки
     * @return стилизованная кнопка
     */
    public static Button createSmall(String text) {
        Button button = new Button(text);
        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-weight: bold;
                -fx-font-size: 11px;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-border-radius: %dpx;
                -fx-background-radius: %dpx;
                -fx-padding: 1 4 1 4;
                -fx-min-width: 30; -fx-max-width: 30;
                -fx-min-height: 22; -fx-max-height: 22;""",
                AppConstants.COLOR_BUTTON_SMALL_BG, AppConstants.COLOR_BUTTON_FG,
                AppConstants.COLOR_BUTTON_BORDER, CORNER_RADIUS, CORNER_RADIUS));
        return button;
    }

    /**
     * Создаёт круглую кнопку-справку "?" (16x16) в виде ссылки.
     *
     * @return стилизованная кнопка
     */
    public static Button createHelp() {
        Button button = new Button("?");
        String linkColor = "#0563C1";

        button.setStyle(String.format("""
                -fx-background-color: transparent;
                -fx-text-fill: %s;
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-border-radius: 50%%;
                -fx-background-radius: 50%%;
                -fx-padding: 0""",
                linkColor, linkColor));

        button.setMinSize(16, 16);
        button.setPrefSize(16, 16);
        button.setMaxSize(16, 16);
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);

        return button;
    }

    // ==================================================================
    // Fluent-настройки
    // ==================================================================

    /**
     * Устанавливает единый размер кнопки (min = pref = max).
     *
     * @param button кнопка
     * @param width  ширина, px
     * @param height высота, px
     * @return та же кнопка (для цепочки вызовов)
     */
    public static Button withFixedSize(Button button, double width, double height) {
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
        return button;
    }

    /**
     * Добавляет подсказку в стандартном стиле приложения
     * (палитра из {@link AppConstants#TOOLTIP_STYLE}).
     *
     * @param button кнопка
     * @param text   текст подсказки
     * @return та же кнопка (для цепочки вызовов)
     */
    public static Button withTooltip(Button button, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle(AppConstants.TOOLTIP_STYLE);
        tooltip.setWrapText(true);
        button.setTooltip(tooltip);
        return button;
    }

    /**
     * Назначает обработчик нажатия.
     *
     * @param button  кнопка
     * @param handler обработчик действия
     * @return та же кнопка (для цепочки вызовов)
     */
    public static Button withAction(Button button, EventHandler<ActionEvent> handler) {
        button.setOnAction(handler);
        return button;
    }

    // ==================================================================
    // Внутренние методы стилизации
    // ==================================================================

    /**
     * Палитра диалоговой кнопки Windows 11 (светлая тема).
     * Инлайновый CSS не умеет селекторы :hover/:pressed,
     * поэтому состояния раскрашиваются вручную.
     */
    private static void applyNativeButtonStyle(Button button) {
        // @formatter:off
        String bg, side, bottom, text;
        if (button.isPressed()) {
            bg = "#F5F5F5"; side = "#C4C4C4"; bottom = "#7A7A7A"; text = "#5F5F5F";
        } else if (button.isHover()) {
            bg = "#F9F9F9"; side = "#C9C9C9"; bottom = "#8C8C8C"; text = "#1A1A1A";
        } else {
            bg = "#FDFDFD"; side = "#D8D8D8"; bottom = "#9A9A9A"; text = "#1A1A1A";
        }
        // @formatter:on
        button.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-background-radius: %d;
                -fx-border-color: %s %s %s %s;
                -fx-border-width: 1;
                -fx-border-radius: %d;
                -fx-text-fill: %s;
                -fx-font-size: 12px;""",
                bg, DIALOG_CORNER_RADIUS, side, side, bottom, side,
                DIALOG_CORNER_RADIUS, text));
    }

}
