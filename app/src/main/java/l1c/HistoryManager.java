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
 * Управление историей адресов и заметок к базам 1С
 */
public class HistoryManager {
    private static final int MAX_HISTORY_SIZE = AppConstants.MAX_HISTORY_SIZE;
    
    private ObservableList<String> historyList;
    private Map<String, String> notesMap;  // адрес -> заметка
    
    public HistoryManager() {
        historyList = FXCollections.observableArrayList(loadHistoryList());
        notesMap = loadNotes();
    }
    
    /**
     * Получить список истории как ObservableList (для использования в ComboBox)
     */
    public ObservableList<String> getHistoryList() {
        return historyList;
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