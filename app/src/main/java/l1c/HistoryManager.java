package l1c;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Управление историей адресов, заметок и команд 1С
 */
public class HistoryManager {
    private static final int MAX_HISTORY_SIZE = AppConstants.MAX_HISTORY_SIZE;
    
    private ObservableList<String> historyList;
    private ObservableList<String> processingHistoryList;
    private Map<String, ObservableList<CommandEntry>> commandHistoryLists;
    private Map<String, String> notesMap;  // адрес -> заметка
    
    /**
     * Запись истории команды с временем последнего использования и режимом запуска
     */
    public static class CommandEntry {
        public final String command;
        public final long lastUsed;
        public final String runMode;  // Режим запуска: "Конфигуратор", "Предприятие" и т.д.
        
        public CommandEntry(String command, long lastUsed) {
            this(command, lastUsed, "");
        }
        
        public CommandEntry(String command, long lastUsed, String runMode) {
            this.command = command;
            this.lastUsed = lastUsed;
            this.runMode = runMode != null ? runMode : "";
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommandEntry that = (CommandEntry) o;
            return command.equals(that.command);
        }
        
        @Override
        public int hashCode() {
            return command.hashCode();
        }
    }
    
    public HistoryManager() {
        historyList = FXCollections.observableArrayList(loadHistoryList());
        processingHistoryList = FXCollections.observableArrayList(loadProcessingHistoryList());
        commandHistoryLists = new HashMap<>();
        commandHistoryLists.put("x86", FXCollections.observableArrayList(loadCommandHistoryList("x86")));
        commandHistoryLists.put("x64", FXCollections.observableArrayList(loadCommandHistoryList("x64")));
        commandHistoryLists.put("x86_64", commandHistoryLists.get("x64"));
        notesMap = loadNotes();
    }
    
    /**
     * Получить список истории как ObservableList (для использования в ComboBox)
     */
    public ObservableList<String> getHistoryList() {
        return historyList;
    }

    /**
     * Получить список истории внешних обработок/отчетов для ComboBox.
     */
    public ObservableList<String> getProcessingHistoryList() {
        return processingHistoryList;
    }

    /**
     * Получить историю команд для платформы.
     */
    public ObservableList<CommandEntry> getCommandHistoryList(String platform) {
        return commandHistoryLists.get(normalizePlatform(platform));
    }

    /**
     * Добавить команду в историю запусков для платформы.
     */
    public void addCommandToHistory(String platform, String command, String runMode) {
        if (command == null || command.trim().isEmpty()) return;
        ObservableList<CommandEntry> commands = getCommandHistoryList(platform);
        String value = command.trim();
        long now = System.currentTimeMillis();
        
        // Проверяем, есть ли уже такая команда
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i).command.equals(value)) {
                // Обновляем timestamp и перемещаем наверх
                commands.remove(i);
                commands.add(0, new CommandEntry(value, now, runMode));
                saveHistoryToXml();
                return;
            }
        }
        
        // Если команды не было — добавляем новую
        commands.add(0, new CommandEntry(value, now, runMode));
        
        while (commands.size() > MAX_HISTORY_SIZE) {
            commands.remove(commands.size() - 1);
        }
        saveHistoryToXml();
    }
    
    /**
     * Получить заметку для адреса
     */
    public String getNote(String address) {
        if (address == null || address.isEmpty()) return null;
        return notesMap.get(address);
    }
    
    /**
     * Сохранить заметку для адреса
     */
    public void saveNote(String address, String note) {
        if (address == null || address.isEmpty()) return;
        if (note == null || note.trim().isEmpty()) {
            notesMap.remove(address);
        } else {
            notesMap.put(address, note.trim());
        }
        saveHistoryToXml();
    }
    
    /**
     * Добавить адрес в историю
     */
    public void addToHistory(String address) {
        if (address == null || address.isEmpty()) return;
        historyList.remove(address);
        historyList.add(0, address);
        while (historyList.size() > MAX_HISTORY_SIZE) {
            historyList.remove(historyList.size() - 1);
        }
        saveHistoryToXml();
    }

    /**
     * Добавить путь внешней обработки/отчета в историю.
     */
    public void addProcessingToHistory(String processingPath) {
        if (processingPath == null || processingPath.trim().isEmpty()) return;
        String value = processingPath.trim();
        processingHistoryList.remove(value);
        processingHistoryList.add(0, value);
        while (processingHistoryList.size() > MAX_HISTORY_SIZE) {
            processingHistoryList.remove(processingHistoryList.size() - 1);
        }
        saveHistoryToXml();
    }
    
    /**
     * Удалить адрес из истории (и его заметку)
     */
    public void removeFromHistory(String address) {
        historyList.remove(address);
        notesMap.remove(address);
        saveHistoryToXml();
    }
    
    /**
     * Получить путь к файлу истории
     */
    private static Path getHistoryPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, AppConstants.APP_DATA_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию для истории: " + dir);
        }
        return dir.resolve(AppConstants.HISTORY_FILE);
    }
    
    /**
     * Получить только текстовое содержимое самого узла (без дочерних элементов)
     */
    private static String getDirectTextContent(Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }
    
    /**
     * Загрузить историю из XML файла
     */
    private static List<String> loadHistoryList() {
        List<String> list = new ArrayList<>();
        Path path = getHistoryPath();
        if (!Files.exists(path)) {
            createDefaultHistoryFile(path);
            return list;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            NodeList addrNodes = doc.getElementsByTagName("address");
            for (int i = 0; i < addrNodes.getLength(); i++) {
                Element addrElem = (Element) addrNodes.item(i);
                String addr = getDirectTextContent(addrElem);
                if (addr != null && !addr.trim().isEmpty() && !list.contains(addr.trim())) {
                    list.add(addr.trim());
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Ошибка загрузки истории из XML. Будет создан новый файл.");
            e.printStackTrace();
            createDefaultHistoryFile(path);
        }
        return list;
    }

    /**
     * Загрузить историю внешних обработок/отчетов из XML файла.
     */
    private static List<String> loadProcessingHistoryList() {
        List<String> list = new ArrayList<>();
        Path path = getHistoryPath();
        if (!Files.exists(path)) {
            createDefaultHistoryFile(path);
            return list;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            NodeList fileNodes = doc.getElementsByTagName("processingFile");
            for (int i = 0; i < fileNodes.getLength(); i++) {
                Element fileElem = (Element) fileNodes.item(i);
                String filePath = getDirectTextContent(fileElem);
                if (filePath != null && !filePath.trim().isEmpty() && !list.contains(filePath.trim())) {
                    list.add(filePath.trim());
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Ошибка загрузки истории внешних обработок из XML.");
            e.printStackTrace();
        }
        return list;
    }

    private static List<CommandEntry> loadCommandHistoryList(String platform) {
        List<CommandEntry> list = new ArrayList<>();
        Path path = getHistoryPath();
        if (!Files.exists(path)) return list;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            NodeList commandNodes = doc.getElementsByTagName("command");
            for (int i = 0; i < commandNodes.getLength(); i++) {
                Element commandElem = (Element) commandNodes.item(i);
                if (!platform.equals(commandElem.getAttribute("platform"))) continue;
                String command = commandElem.getTextContent().trim();
                if (command.isEmpty()) continue;
                
                // Загружаем timestamp из атрибута lastUsed
                String lastUsedAttr = commandElem.getAttribute("lastUsed");
                long lastUsed = 0;
                if (!lastUsedAttr.isEmpty()) {
                    try {
                        lastUsed = Long.parseLong(lastUsedAttr);
                    } catch (NumberFormatException ignored) {
                    }
                }
                
                // Загружаем режим запуска из атрибута runMode
                String runMode = commandElem.getAttribute("runMode");
                
                // Проверяем дубликаты (по команде, берём первый с наибольшим timestamp)
                boolean alreadyAdded = false;
                for (CommandEntry entry : list) {
                    if (entry.command.equals(command)) {
                        if (lastUsed > entry.lastUsed) {
                            // Заменяем на более свежий
                            list.remove(entry);
                            list.add(new CommandEntry(command, lastUsed, runMode));
                        }
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    list.add(new CommandEntry(command, lastUsed, runMode));
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Ошибка загрузки истории команд.");
            e.printStackTrace();
        }
        return list;
    }

    private static String normalizePlatform(String platform) {
        return "x86_64".equals(platform) ? "x64" : platform;
    }
    
    /**
     * Загрузить заметки из XML файла
     */
    private Map<String, String> loadNotes() {
        Map<String, String> map = new LinkedHashMap<>();
        Path path = getHistoryPath();
        if (!Files.exists(path)) {
            return map;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            NodeList addrNodes = doc.getElementsByTagName("address");
            for (int i = 0; i < addrNodes.getLength(); i++) {
                Element addrElem = (Element) addrNodes.item(i);
                String addr = getDirectTextContent(addrElem);
                if (addr == null || addr.trim().isEmpty()) continue;
                
                String note = getNoteFromElement(addrElem);
                if (note != null && !note.isEmpty()) {
                    map.put(addr.trim(), note);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки заметок из XML.");
            e.printStackTrace();
        }
        return map;
    }
    
    /**
     * Извлечь текст заметки из элемента <address>
     */
    private static String getNoteFromElement(Element addrElem) {
        NodeList children = addrElem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("note".equals(child.getNodeName())) {
                String text = child.getTextContent();
                return text != null ? text : "";
            }
        }
        return null;
    }
    
    /**
     * Сохранить историю в XML файл
     */
    private void saveHistoryToXml() {
        Path path = getHistoryPath();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("history");
            doc.appendChild(root);

            root.appendChild(doc.createComment(RunYBaseHelpTexts.HISTORY_COMMENT));

            Element addresses = doc.createElement("addresses");
            root.appendChild(addresses);

            for (String addr : historyList) {
                Element addrElem = doc.createElement("address");
                addrElem.setTextContent(addr);
                
                // Добавляем заметку, если есть
                String note = notesMap.get(addr);
                if (note != null && !note.isEmpty()) {
                    Element noteElem = doc.createElement("note");
                    noteElem.setTextContent(note);
                    addrElem.appendChild(noteElem);
                }
                
                addresses.appendChild(addrElem);
            }

            Element processingFiles = doc.createElement("processingFiles");
            root.appendChild(processingFiles);

            for (String processingPath : processingHistoryList) {
                Element fileElem = doc.createElement("processingFile");
                fileElem.setTextContent(processingPath);
                processingFiles.appendChild(fileElem);
            }

            Element commands = doc.createElement("commands");
            root.appendChild(commands);
            for (String platform : List.of("x86", "x64")) {
                for (CommandEntry entry : commandHistoryLists.get(platform)) {
                    Element commandElem = doc.createElement("command");
                    commandElem.setAttribute("platform", platform);
                    commandElem.setAttribute("lastUsed", String.valueOf(entry.lastUsed));
                    if (entry.runMode != null && !entry.runMode.isEmpty()) {
                        commandElem.setAttribute("runMode", entry.runMode);
                    }
                    commandElem.setTextContent(entry.command);
                    commands.appendChild(commandElem);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(path.toFile());
            transformer.transform(source, result);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения истории в XML: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создать файл истории по умолчанию (пустой)
     */
    private static void createDefaultHistoryFile(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("history");
            doc.appendChild(root);

            root.appendChild(doc.createComment(RunYBaseHelpTexts.HISTORY_COMMENT));

            Element addresses = doc.createElement("addresses");
            root.appendChild(addresses);

            Element processingFiles = doc.createElement("processingFiles");
            root.appendChild(processingFiles);

            Element commands = doc.createElement("commands");
            root.appendChild(commands);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(path.toFile());
            transformer.transform(source, result);
        } catch (Exception e) {
            System.err.println("Не удалось создать файл истории по умолчанию: " + path);
            e.printStackTrace();
        }
    }
}
