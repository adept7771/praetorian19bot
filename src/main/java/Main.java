import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;

public class Main {

    public static final Logger log = Logger.getLogger(Main.class);

    public static String absolutePath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    static HashMap<Integer, Integer> newbieMapWithGeneratedAnswers = new HashMap<>();
    static HashMap<Integer, Long> newbieMapWithJoinTime = new HashMap<>();
    static HashMap<Integer, Long> newbieMapWithChatId = new HashMap<>();

    public static HashMap<Long, HashMap<Integer, Long>> newbieToSecondaryApprove = new HashMap<Long, HashMap<Integer, Long>>();
    // ^ this hash map needed to secondary check that user is not a bot. User must wright something in defined time
    // interval after first approve. If it will not be done bot will kick this user from chat.
    // <Long - chatId, HashMap<Integer - userId, Long - joinTime >>

    public static Bot bot;

    static ChatSettingsHandler chatSettingsHandler = new ChatSettingsHandler();

    public static HashMap<Long, HashMap<String, String>> userSettingsInMemory = new HashMap<Long, HashMap<String, String>>();
    // ^ userSettingStorage <Long - chatId, HashMap<String - optionName, String - optionValue>

    public static long lastMemorySettingsUpdateTime;

    public static void main(String[] args) {

        // Initialize Api Context
        ApiContextInitializer.init();

        // Instantiate Telegram Bots API
        TelegramBotsApi botsApi = new TelegramBotsApi();

        // Register our bot
        try {
            log.info("Bot initialising.");
            botsApi.registerBot(bot = new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}