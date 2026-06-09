package l1c;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CredentialsManager {
    private static final String HISTORY_DIR = ".1c_launcher";
    private static final String KEY = "1C_Launcher_2026_Secret_Key";
    private final Map<String, UserCredentials> credentialsMap = new HashMap<>();

    public CredentialsManager() {
        loadCredentials();
    }

    public UserCredentials get(String address) {
        return credentialsMap.get(address);
    }

    public void put(String address, UserCredentials cred) {
        credentialsMap.put(address, cred);
        saveCredentials();
    }

    public void remove(String address) {
        credentialsMap.remove(address);
        saveCredentials();
    }

    public boolean containsKey(String address) {
        return credentialsMap.containsKey(address);
    }

    private Path getCredentialsPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, HISTORY_DIR);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("Не удалось создать директорию для истории: " + dir);
        }
        return dir.resolve("credentials.xml");
    }

    private void loadCredentials() {
        Path path = getCredentialsPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());

            NodeList credNodes = doc.getElementsByTagName("credential");
            for (int i = 0; i < credNodes.getLength(); i++) {
                Element elem = (Element) credNodes.item(i);
                String address = getTagValue("address", elem);
                String username = getTagValue("username", elem);
                String encryptedPassword = getTagValue("password", elem);

                if (address != null && username != null && !address.isEmpty()) {
                    String password = encryptedPassword != null ? decrypt(encryptedPassword) : "";
                    credentialsMap.put(address, new UserCredentials(username, password));
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки учётных данных: " + e.getMessage());
        }
    }

    private void saveCredentials() {
        Path path = getCredentialsPath();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("credentials");
            doc.appendChild(root);

            for (Map.Entry<String, UserCredentials> entry : credentialsMap.entrySet()) {
                Element credElem = doc.createElement("credential");

                Element addrElem = doc.createElement("address");
                addrElem.setTextContent(entry.getKey());
                credElem.appendChild(addrElem);

                Element userElem = doc.createElement("username");
                userElem.setTextContent(entry.getValue().getUsername());
                credElem.appendChild(userElem);

                Element passElem = doc.createElement("password");
                passElem.setTextContent(encrypt(entry.getValue().getPassword()));
                credElem.appendChild(passElem);

                root.appendChild(credElem);
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
            System.err.println("Ошибка сохранения учётных данных: " + e.getMessage());
        }
    }

    private static String encrypt(String input) {
        if (input == null || input.isEmpty()) return "";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[inputBytes.length];
        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return Base64.getEncoder().encodeToString(result);
    }

    private static String decrypt(String input) {
        if (input == null || input.isEmpty()) return "";
        byte[] inputBytes = Base64.getDecoder().decode(input);
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[inputBytes.length];
        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return new String(result, StandardCharsets.UTF_8);
    }

    private String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        return list.item(0).getTextContent();
    }
}