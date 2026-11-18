# KODA.md — Контекст проекта для KODA

## 📋 Обзор проекта

**Название:** Запускалка баз 1С (launcher1Cbases)  
**Тип:** JavaFX-приложение для формирования команд запуска 1С:Предприятие  
**Язык:** Java 21 (компиляция для совместимости с Java 17)  
**Система сборки:** Gradle 9.2.0  
**Версия приложения:** 2026.06.11.005  
**ОС разработки:** Windows  

### Назначение
Приложение предназначено для удобного формирования команд запуска 1С:Предприятие 8 в различных режимах:
- Конфигуратор (DESIGNER)
- Тонкий клиент (ENTERPRISE)
- Толстый клиент — управляемое приложение
- Толстый клиент — обычное приложение

Поддерживает работу с файловыми и клиент-серверными базами 1С.

---

## 🏗️ Структура проекта

```
├── app/                          # Основной подпроект
│   ├── build.gradle             # Конфигурация JavaFX, зависимости, сборка fat JAR
│   └── src/
│       ├── main/java/l1c/       # Исходный код
│       │   ├── RunYBase.java            # Основное приложение (JavaFX UI)
│       │   ├── AppConstants.java        # Константы (версия, пути, стили)
│       │   ├── RunYBaseHelpTexts.java   # Тексты справки
│       │   ├── CommandBuilder.java      # Построение/разбор команд запуска 1С
│       │   ├── UiFactory.java           # Фабрика UI-элементов (кнопки, бейджи)
│       │   ├── HistoryManager.java      # Управление историей адресов (XML)
│       │   ├── HistoryListCell.java     # Отрисовка ячейки истории
│       │   ├── CredentialsManager.java  # Управление учётными данными (шифрование)
│       │   ├── UserCredentials.java     # Данные учётной записи
│       │   ├── SavedUserEntry.java      # Запись сохранённого пользователя
│       │   ├── SavedUserListCell.java   # Отрисовка ячейки сохранённых пользователей
│       │   ├── BaseEntry.java           # Запись базы 1С (из ibases.v8i)
│       │   ├── BaseEntryListCell.java   # Отрисовка ячейки списка баз
│       │   ├── DatabaseListService.java # Чтение списка баз из файлов 1С
│       │   ├── StringExtractor.java     # Утилита извлечения строк подключения
│       │   ├── ComboBoxWithButton.java  # ComboBox с кнопкой
│       │   ├── ComboBoxWithMenuButton.java # ComboBox с кнопкой меню
│       │   └── dialogs/                 # Модальные диалоги
│       │       ├── CommandHistoryDialog.java  # История команд
│       │       ├── DatabaseSelectionDialog.java # Выбор базы из списка
│       │       ├── NoteDialog.java            # Заметка о базе
│       │       ├── SavedUsersDialog.java      # Список сохранённых пользователей
│       │       └── UserCredentialsDialog.java # Учётные данные пользователя
│       ├── main/resources/l1c/
│       │   └── app.css # Стили JavaFX (цветовая схема 1С)
│       └── test/java/l1c/       # Тесты (JUnit 5)
│           ├── AppTest.java
│           ├── CommandBuilderTest.java
│           ├── CommandBuilderParseTest.java
│           ├── CommandBuilderLaunchTest.java
│           ├── ArgPrinter.java
│           └── TempShortAddressProbeTest.java
├── gradle/                       # Конфигурация Gradle
│   ├── libs.versions.toml        # Управление зависимостями (Version Catalog)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .koda/                        # Конфигурация KODA
│   └── project-rules.md         # Правила проекта
├── .vscode/                       # Настройки VS Code
│   ├── launch.json
│   └── settings.json
├── .continue/config.json          # Конфигурация Continue (AI)
├── META-INF/MANIFEST.MF
├── KODA.md
├── README.md
├── gradle.properties
├── gradlew / gradlew.bat
├── settings.gradle
├── commit-with-version.ps1        # Скрипт обновления версии
├── run-app.bat / run.bat          # Скрипты запуска
└── run_output.txt
```

---

## 🛠️ Технологический стек

| Компонент | Версия | Описание |
|-----------|--------|----------|
| **Java** | 21 (bytecode 17) | Основной язык |
| **JavaFX** | 21.0.2 | GUI-фреймворк (javafx.controls) |
| **Gradle** | 9.2.0 | Система сборки |
| **Guava** | 33.4.6-jre | Google Core Libraries |
| **JUnit 5** | 5.12.1 | Фреймворк тестирования |

---

## ⚙️ Сборка и запуск

### Команды Gradle

```powershell
# Сборка проекта
gradlew build

# Очистка и сборка
gradlew clean build

# Запуск приложения
gradlew run

# Создание JAR-файла
gradlew jar

# Запуск тестов
gradlew test
```

### Пакетный запуск (Windows)

```powershell
gradlew.bat clean build
```

### Ручной запуск JAR

```powershell
java --module-path "lib\javafx-sdk-21.0.2\lib" --add-modules javafx.controls -jar app\build\libs\launcher1c-*.jar
```

---

## 🎨 Архитектура и стиль

### UI-компоненты
- **BorderPane** — корневой контейнер
- **VBox/HBox** — компоновка элементов
- **RadioButton** — выбор режима запуска
- **CheckBox** — опции (отладка, приоритет платформы)
- **ComboBox** — ввод адреса с историей
- **TextArea** — вывод команд для x86/x64

### Цветовая схема (стиль 1С)
```
Фон:              #FFFFFF (белый)
Кнопки:           #E6C878 (приглушённый жёлтый)
Кнопки малые:     #F3E4BC (светло-жёлтый)
Текст:            #000000 (чёрный)
Границы:          #C0A050 (коричнево-жёлтый)
Акцент:           #C8A046
Tooltip:          #F3E4BC фон, #C0A050 граница
```

### Паттерны кода
- **Singleton-подобный** для менеджеров (HistoryManager, CredentialsManager)
- **Factory Method** — создание кнопок, tooltip с общим стилем
- **Builder** — построение команд запуска через `StringBuilder`
- **Observer** — слушатели свойств JavaFX

### Особенности реализации
- Компиляция для Java 17 (`options.release = 17`) для кросс-версионной совместимости
- Fat JAR с упаковкой всех зависимостей
- Исключение файлов подписи при сборке
- Шифрование паролей через XOR с ключом + Base64 (не криптографически стойкое)
- Сохранение данных в `%USERPROFILE%\.1c_launcher\`

---

## 📝 Правила разработки

### Язык
- **Комментарии и строки** — русский язык
- **Идентификаторы** — английский язык (camelCase для методов/переменных, PascalCase для классов)

### Коммиты
- **Описания коммитов на русском языке**
- Не коммитить без явного разрешения разработчика

### Форматирование
- Использовать `@formatter:off` / `@formatter:on` для блоков с особым форматированием
- Регионы `#region` / `#endregion` для группировки кода

### Изменения
- При правках показывать только изменённые участки
- Использовать "ленивые" комментарии для пропусков (>20 строк)
- Следовать существующему стилю кода

### Тестирование
- Запускать тесты после изменений: `gradlew test`
- Использовать JUnit 5

---

## 🔧 Параметры запуска 1С

### Режимы
| Режим | Команда |
|-------|---------|
| Конфигуратор | `DESIGNER` |
| Предприятие (тонкий) | `ENTERPRISE` |
| Толстый (управляемое) | `ENTERPRISE /RunModeManagedApplication` |
| Толстый (обычное) | `ENTERPRISE /RunModeOrdinaryApplication` |

### Дополнительные параметры
| Параметр | Описание |
|----------|----------|
| `/IBConnectionString` | Адрес базы (File=... или Srvr=...;Ref=...) |
| `/N` / `/P` | Имя пользователя / Пароль |
| `/AppArch` | Разрядность (x86 / x86_64) |
| `/Debug -attach` | Режим отладки (только для Enterprise, не для Конфигуратора) |
| `-tcp` / `-http` | Протокол отладки |

### Важное
- **Параметр отладки игнорируется для Конфигуратора**
- Пробелы в путях требуют кавычек

---

## 📊 История и учётные данные

### Хранение данных
```
%USERPROFILE%\.1c_launcher\
├── history.xml        # История адресов (XML)
└── credentials.xml    # Учётные данные (XML, зашифровано)
```

### Формат истории (XML)
```xml
<history>
  <!-- Комментарий -->
  <addresses>
    <address>File="C:\1C\Base"</address>
    <address>Srvr="127.0.0.1";Ref="Base"</address>
  </addresses>
</history>
```

---

## 🚀 Быстрый старт

1. **Сборка:**
   ```powershell
   gradlew clean build
   ```

2. **Запуск:**
   ```powershell
   gradlew run
   ```

3. **Использование:**
   - Введите адрес базы (например: `File="C:\1C\Base"`)
   - Выберите режим запуска
   - Настройте опции (отладка, приоритет платформы)
   - Нажмите "Сформировать"
   - Скопируйте команду или запустите напрямую

---

## 📌 Примечания для KODA

- **Разработка ведется в Windows** — все команды и пути рассчитаны на Windows/PowerShell
- **Python не установлен** — не пытаться использовать `python`/`pip`/скрипты на Python, не предлагать их как инструмент сборки или проверки
- При добавлении нового функционала следовать существующим паттернам
- Не добавлять copyright/license заголовки без запроса
- Исправлять корневую причину проблем, а не симптомы
- Перед изменениями читать соответствующие файлы
- Все описания и комментарии на русском языке
