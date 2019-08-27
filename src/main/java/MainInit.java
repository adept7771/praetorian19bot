import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;

public class MainInit {

    public static String absolutePath = MainInit.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    static HashMap<Integer, Integer> newbieMapWithAnswer = new HashMap<>();
    static HashMap<Integer, Long> newbieMapWithJoinTime = new HashMap<>();
    static HashMap<Integer, Long> newbieMapWithChatId = new HashMap<>();

    static UserSettingsHandler userSettingsHandler;
    static HashMap<Long, HashMap<String, String>> userSettingsForBot;

    public static void main(String[] args) {

        // Initialize Api Context
        ApiContextInitializer.init();

        // Instantiate Telegram Bots API
        TelegramBotsApi botsApi = new TelegramBotsApi();

        // Register our bot
        try {
            botsApi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}