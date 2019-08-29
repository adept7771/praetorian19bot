import commandsAndTexts.commands.CommandsEn;
import commandsAndTexts.texts.EnTexts;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CommandsHandler {

    private static final Logger log = Logger.getLogger(CommandsHandler.class);

    public static void handleAllCommands(String messageText, long chatId, Integer messageId, boolean isUpdatePersonalDirectMessage, Update update) {
        if (messageText.contains("/help")) { // Print HELP for all messages in ONE message
            log.info("Message text contains /help - show commandsAndTexts list");
            StringBuilder helpText = new StringBuilder();
            for (CommandsEn commands : CommandsEn.values()) {
                helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
            }
            Main.bot.sendReplyMessageToChatID(chatId, helpText.toString(), messageId);

        } else { // Print help to command and handle it
            log.info("Message text contains / - it's CAN be a command");
            String helpText = "";

            try {
                boolean enteredCommandWithValue = true;
                for (CommandsEn commands : CommandsEn.values()) {
                    if (messageText.toLowerCase().equals("/" + commands.name().toLowerCase() + "@" + Main.bot.getBotUsername())) {
                        log.info("Message test contains - command name: " + commands.name());
                        helpText = commands.value;
                        Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
                        enteredCommandWithValue = false; // if command from standart default list without value
                    }
                }
                if(enteredCommandWithValue){ // if command not exactly in list because contains options value
                    recognizeAndHandleCommand(messageText, isUpdatePersonalDirectMessage, chatId, update);
                }
            } catch (Exception e) {
                log.info("Command not recognized or is not in command list: " + messageText + " " + e.toString());
            }
        }
    }

    public static void recognizeAndHandleCommand(String command, boolean isUpdatePersonalDirectMessage, long chatId, Update update) {
        if (command.toLowerCase().contains(CommandsEn.defaultLanguageAdm.name().toLowerCase()) && !isUpdatePersonalDirectMessage) { // language option for bot
            if(Main.bot.isUserAdminInChat(update.getMessage().getFrom().getId(), chatId)){
                // if user admin in chat

            }
            else { // if not an admin
                Main.bot.sendMessageToChatID(chatId, EnTexts.adminCheckWrong.name());
            }

            if (command.contains("En")) {
                System.out.println("Command contains En");
            }
            else if (command.contains("Ru")) {
                System.out.println("Command contains Ru");
            }
        }
    }
}
