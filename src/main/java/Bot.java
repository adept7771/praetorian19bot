import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingBot {

    public void onUpdateReceived(Update update) {

        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {


            long chat_id = update.getMessage().getChatId();
            System.out.println("Is sender bot ? " + update.getMessage().getFrom().getBot());
            System.out.println("Is sender mentioned me? " + update.getMessage().getText().contains("@" + getBotUsername()));


            if(!update.getMessage().getFrom().getBot()){

                String message_text = update.getMessage().getText();

                if(message_text.contains("@" + getBotUsername())){

                    SendMessage message = new SendMessage() // Create a message object object
                            .setChatId(chat_id)
                            .setReplyToMessageId(update.getMessage().getMessageId())
                            .setText("Да, ты написал именно мне, долбоебал. Теперь иди на хуй.");
                    try {
                        execute(message); // Sending our message object to user
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

                if(update.getMessage().getReplyToMessage() != null){
                    if(update.getMessage().getReplyToMessage().getFrom().getUserName().equals(getBotUsername())){
                        SendMessage message = new SendMessage() // Create a message object object
                                .setChatId(chat_id)
                                .setReplyToMessageId(update.getMessage().getMessageId())
                                .setText("Не отвечай на мои реплаи пидрилла!");
                        try {
                            execute(message); // Sending our message object to user
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

//            SendMessage message = new SendMessage() // Create a message object object
//                    .setChatId(chat_id)
//                    .setText("очкошник же ты леееее");

//            try {
//                System.out.println("");
//                //execute(message); // Sending our message object to user
//            } catch (TelegramApiException e) {
//                e.printStackTrace();
//            }
        }
    }

    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return "Praetorian19bot";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return "868108811:AAFEBVlByhnZhYs0heohMiN0bsqDM_nn6IM";
    }
}
