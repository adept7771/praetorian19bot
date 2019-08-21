import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingBot {

    public void onUpdateReceived(Update update) {

        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {

            long chat_id = update.getMessage().getChatId();

            if(!update.getMessage().getFrom().getBot()){ // sender is not bot

                String message_text = update.getMessage().getText();
                boolean messageContainsBotName = message_text.contains("@" + getBotUsername());
                Message replyMessage = update.getMessage().getReplyToMessage();

                if( messageContainsBotName || (replyMessage != null &&
                        update.getMessage().getReplyToMessage().getFrom().getUserName().equals(getBotUsername()))
                ){
                    SendMessage message = new SendMessage() // Create a message object object
                            .setChatId(chat_id)
                            .setReplyToMessageId(update.getMessage().getMessageId())
                            .setText("Меня указали либо в директ сообщении, либо в реплае. Тебе заняться больше не чем?");
                    try {
                        execute(message); // Sending our message object to user
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
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
