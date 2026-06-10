package l1c;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Управление историей адресов баз 1С
 */
public class HistoryManager {
    private static final int MAX_HISTORY_SIZE = AppConstants.MAX_HISTORY_SIZE;
    
    private ObservableList<String> historyList;
    
    public HistoryManager() {
        historyList = FXCollections.observableArrayList(loadHistoryList());
    }
    
    /**
     * Получить список истории как ObservableList (для использования в ComboBox)
     */
    public ObservableList<String> getHistoryList() {
        return historyList;
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
                String addr = addrNodes.item(i).getTextContent();
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