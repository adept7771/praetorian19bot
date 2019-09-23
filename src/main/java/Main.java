import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;

public class Main {

    public static String absolutePath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    static HashMap<Integer, Integer> newbieMapWithAnswer = new HashMap<>();
    static HashMap<Integer, Long> newbieMapWithJoinTime = new HashMap<>();
    static HashMap<Integer, Long> newbieMapWithChatId = new HashMap<>();

    public static final Logger log = Logger.getLogger(Main.class);

    public static Bot bot;

    static UserSettingsHandler userSettingsHandler = new UserSettingsHandler();
    public static HashMap<Long, HashMap<String, String>> userSettingsInMemory = new HashMap<Long, HashMap<String, String>>();
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