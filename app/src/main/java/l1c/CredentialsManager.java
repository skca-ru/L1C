package l1c;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
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

/**
 * Хранение учётных данных 1С.
 * На один адрес базы может быть сохранено несколько пользователей:
 * один из них главный (используется по умолчанию при формировании
 * команды), остальные выбираются пользователем вручную через окно выбора.
 */
public class CredentialsManager {
    private static final String KEY = AppConstants.ENCRYPTION_KEY;

    // адрес -> список пользователей адреса (порядок сохранения сохраняется)
    private final Map<String, List<UserCredentials>> credentialsMap = new LinkedHashMap<>();

    public CredentialsManager() {
        loadCredentials();
    }

    /**
     * Возвращает главного (по умолчанию) пользователя для адреса.
     * Если главный не помечен — первым в списке.
     */
    public UserCredentials get(String address) {
        return getMain(address);
    }

    /**
     * Возвращает главного (по умолчанию) пользователя для адреса или null,
     * если для адреса нет ни одного сохранённого пользователя.
     */
    public UserCredentials getMain(String address) {
        List<UserCredentials> list = credentialsMap.get(address);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (UserCredentials cred : list) {
            if (cred.isMain()) {
                return cred;
            }
        }
        return list.get(0);
    }

    /**
     * Возвращает копию списка всех пользователей адреса (пустой список, если их нет).
     */
    public List<UserCredentials> getUsers(String address) {
        List<UserCredentials> list = credentialsMap.get(address);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    /**
     * Добавляет пользователя к адресу или обновляет уже сохранённого с таким же
     * именем. Первый пользователь адреса автоматически становится главным.
     */
    public void put(String address, UserCredentials cred) {
        List<UserCredentials> list = credentialsMap.computeIfAbsent(address, k -> new ArrayList<>());
        UserCredentials existing = findByName(list, cred.getUsername());
        if (existing != null) {
            list.set(list.indexOf(existing), cred);
        } else {
            list.add(cred);
        }
        normalizeMain(list);
        saveCredentials();
    }

    /**
     * Заменяет учётные данные главного пользователя адреса.
     * Если главный не помечен — обновляется первый в списке.
     * Если список пуст — добавляется как новый главный пользователь.
     * Имя нового пользователя не должно совпадать с именами остальных.
     */
    public void putMainUser(String address, UserCredentials cred) {
        List<UserCredentials> list = credentialsMap.computeIfAbsent(address, k -> new ArrayList<>());

        UserCredentials found = null;
        for (UserCredentials c : list) {
            if (c.isMain()) {
                found = c;
                break;
            }
        }
        if (found == null && !list.isEmpty()) {
            found = list.get(0);
        }
        final UserCredentials target = found;

        UserCredentials main = new UserCredentials(cred.getUsername(), cred.getPassword(), true);
        if (target == null) {
            // Нет пользователей — добавляем нового
            list.add(main);
        } else {
            // Удаляем дубликат имени (если новый совпадает с кем-то ещё)
            list.removeIf(c -> c != target && c.getUsername().equals(cred.getUsername()));
            // Заменяем цель на обновлённого пользователя
            int idx = list.indexOf(target);
            if (idx >= 0) {
                list.set(idx, main);
            } else {
                // Цель могла быть удалена как дубликат — добавляем
                list.add(main);
            }
        }
        normalizeMain(list);
        saveCredentials();
    }

    /**
     * Полностью заменяет список пользователей адреса (используется диалогом
     * редактирования). Пустой список удаляет запись адреса целиком.
     */
    public void putUsers(String address, List<UserCredentials> creds) {
        if (creds == null || creds.isEmpty()) {
            credentialsMap.remove(address);
        } else {
            List<UserCredentials> list = new ArrayList<>(creds);
            normalizeMain(list);
            credentialsMap.put(address, list);
        }
        saveCredentials();
    }

    /**
     * Делает указанного пользователя главным для адреса (снимает флаг с остальных).
     */
    public void setMainUser(String address, String username) {
        List<UserCredentials> list = credentialsMap.get(address);
        if (list == null) {
            return;
        }
        boolean found = false;
        for (UserCredentials cred : list) {
            cred.setMain(!found && cred.getUsername().equals(username));
            if (cred.isMain()) {
                found = true;
            }
        }
        saveCredentials();
    }

    /**
     * Удаляет одного пользователя адреса.
     *
     * <p>Главный пользователь не удаляется, если у адреса есть другие пользователи:
     * сначала главным нужно сделать кого-то другого. Единственный пользователь
     * (он же главный) удаляется, запись адреса при этом исчезает полностью.</p>
     *
     * @return {@code true}, если пользователь удалён; {@code false} — если адреса,
     *         пользователя с таким именем нет или удаление запрещено правилом
     */
    public boolean removeUser(String address, String username) {
        List<UserCredentials> list = credentialsMap.get(address);
        if (list == null) {
            return false;
        }

        UserCredentials target = findByName(list, username);
        if (target == null || (target.isMain() && list.size() > 1)) {
            return false;
        }

        list.remove(target);
        if (list.isEmpty()) {
            credentialsMap.remove(address);
        } else {
            normalizeMain(list);
        }
        saveCredentials();
        return true;
    }

    /**
     * Удаляет всех пользователей адреса.
     */
    public void remove(String address) {
        credentialsMap.remove(address);
        saveCredentials();
    }

    public boolean containsKey(String address) {
        return credentialsMap.containsKey(address);
    }

    /**
     * Возвращает копию карты "адрес -> главный пользователь".
     */
    public Map<String, UserCredentials> getAllCredentials() {
        Map<String, UserCredentials> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<UserCredentials>> entry : credentialsMap.entrySet()) {
            UserCredentials main = getMain(entry.getKey());
            if (main != null) {
                result.put(entry.getKey(), main);
            }
        }
        return result;
    }

    /**
     * Возвращает копию карты "адрес -> все пользователи адреса".
     */
    public Map<String, List<UserCredentials>> getAllUsers() {
        Map<String, List<UserCredentials>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<UserCredentials>> entry : credentialsMap.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    private static UserCredentials findByName(List<UserCredentials> list, String username) {
        for (UserCredentials cred : list) {
            if (cred.getUsername().equals(username)) {
                return cred;
            }
        }
        return null;
    }

    /**
     * Гарантирует, что ровно один пользователь списка помечен как главный:
     * если флаги не выставлены — главным становится первый.
     */
    private static void normalizeMain(List<UserCredentials> list) {
        if (list.isEmpty()) {
            return;
        }
        boolean hasMain = list.stream().anyMatch(UserCredentials::isMain);
        if (!hasMain) {
            list.get(0).setMain(true);
            return;
        }
        boolean first = true;
        for (UserCredentials cred : list) {
            if (cred.isMain()) {
                if (first) {
                    first = false;
                } else {
                    cred.setMain(false);
                }
            }
        }
    }

    private Path getCredentialsPath() {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, AppConstants.APP_DATA_DIR);
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
                // Флаг главного пользователя отсутствует в файлах старого формата
                boolean main = Boolean.parseBoolean(getTagValue("main", elem));

                if (address != null && username != null && !address.isEmpty() && !username.isEmpty()) {
                    String password = encryptedPassword != null ? decrypt(encryptedPassword) : "";
                    credentialsMap
                            .computeIfAbsent(address, k -> new ArrayList<>())
                            .add(new UserCredentials(username, password, main));
                }
            }
            for (List<UserCredentials> list : credentialsMap.values()) {
                normalizeMain(list);
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

            for (Map.Entry<String, List<UserCredentials>> entry : credentialsMap.entrySet()) {
                for (UserCredentials cred : entry.getValue()) {
                    Element credElem = doc.createElement("credential");

                    Element addrElem = doc.createElement("address");
                    addrElem.setTextContent(entry.getKey());
                    credElem.appendChild(addrElem);

                    Element userElem = doc.createElement("username");
                    userElem.setTextContent(cred.getUsername());
                    credElem.appendChild(userElem);

                    Element passElem = doc.createElement("password");
                    passElem.setTextContent(encrypt(cred.getPassword()));
                    credElem.appendChild(passElem);

                    Element mainElem = doc.createElement("main");
                    mainElem.setTextContent(Boolean.toString(cred.isMain()));
                    credElem.appendChild(mainElem);

                    root.appendChild(credElem);
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