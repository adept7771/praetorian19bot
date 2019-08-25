import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;

public class MainInit {

    static HashMap<Integer, Integer> newbieMapWithAnswer = new HashMap<Integer, Integer>();
    static HashMap<Integer, Long> newbieMapWithJoinTime = new HashMap<Integer, Long>();
    static HashMap<Integer, Long> newbieMapWithChatId = new HashMap<Integer, Long>();
    static boolean mode = false; // true - production, false - test

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