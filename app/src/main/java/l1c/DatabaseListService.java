package l1c;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Загрузка и сортировка списка зарегистрированных информационных баз 1С
 * из файла ibases.v8i (папка AppData пользователя).
 * <p>
 * Класс не содержит UI-логики: ошибки пробрасываются вызывающему коду,
 * который сам решает, как их показать пользователю.
 */
public final class DatabaseListService {

    private static final Path IBASES_PATH = Paths.get(System.getProperty("user.home"),
            "AppData", "Roaming", "1C", "1CEStart", "ibases.v8i");

    private DatabaseListService() {
    }

    /**
     * Возвращает список информационных баз из ibases.v8i, отсортированный
     * по имени базы.
     *
     * @return отсортированный список баз
     * @throws IOException                  если файл списка баз не найден или не читается
     * @throws ParserConfigurationException если не удалось создать XML-парсер
     * @throws SAXException                 если XML-файл списка баз повреждён
     */
    public static List<BaseEntry> loadAndSortDatabases()
            throws IOException, ParserConfigurationException, SAXException {
        if (!Files.exists(IBASES_PATH)) {
            throw new IOException("Файл списка баз не найден:\n" + IBASES_PATH);
        }

        List<BaseEntry> baseEntries = new ArrayList<>();

        String content = new String(Files.readAllBytes(IBASES_PATH), StandardCharsets.UTF_8);

        if (content.trim().startsWith("<?xml") || content.contains("<infobase>")) {
            parseXmlFormatWithOrder(IBASES_PATH, baseEntries);
        } else {
            parseIniFormatWithOrder(IBASES_PATH, baseEntries);
        }

        baseEntries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return baseEntries;
    }

    /**
     * Удаляет BOM (U+FEFF) из начала строки, если он присутствует.
     */
    private static String stripBom(String line) {
        return (line != null && line.startsWith("\uFEFF")) ? line.substring(1) : line;
    }

    private static void parseIniFormatWithOrder(Path path, List<BaseEntry> entries) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        // Первая строка файла может начинаться с BOM (U+FEFF) — убираем его,
        // иначе заголовок первой секции "[Имя базы]" не будет распознан
        if (!lines.isEmpty()) {
            lines.set(0, stripBom(lines.get(0)));
        }

        String currentName = null;
        String currentConnect = null;
        double currentOrder = Double.MAX_VALUE;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                if (currentName != null && currentConnect != null) {
                    entries.add(new BaseEntry(currentName, currentConnect, currentOrder));
                }
                currentName = line.substring(1, line.length() - 1);
                currentConnect = null;
                currentOrder = Double.MAX_VALUE;
            } else if (line.startsWith("Connect=") && currentName != null) {
                currentConnect = line.substring(8);
            } else if (line.startsWith("OrderInList=") && currentName != null) {
                try {
                    currentOrder = Double.parseDouble(line.substring(12));
                } catch (NumberFormatException e) {
                    currentOrder = Double.MAX_VALUE;
                }
            }
        }

        if (currentName != null && currentConnect != null) {
            entries.add(new BaseEntry(currentName, currentConnect, currentOrder));
        }
    }

    private static void parseXmlFormatWithOrder(Path path, List<BaseEntry> entries)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(path.toFile());

        NodeList infos = doc.getElementsByTagName("infobase");
        for (int i = 0; i < infos.getLength(); i++) {
            Element info = (Element) infos.item(i);
            String name = getTagValue("name", info);
            String connect = getTagValue("connect", info);
            double order = Double.MAX_VALUE;

            NodeList orderNodes = info.getElementsByTagName("orderInList");
            if (orderNodes.getLength() > 0) {
                try {
                    order = Double.parseDouble(orderNodes.item(0).getTextContent());
                } catch (NumberFormatException e) {
                    order = Double.MAX_VALUE;
                }
            }

            if (name != null && !name.isEmpty() && connect != null && !connect.isEmpty()) {
                entries.add(new BaseEntry(name, connect, order));
            }
        }
    }

    private static String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list.getLength() == 0)
            return null;
        return list.item(0).getTextContent();
    }
}