import commandsAndTexts.commands.CommandsEn;
import commandsAndTexts.commands.CommandsRu;
import commandsAndTexts.texts.EnTexts;
import commandsAndTexts.texts.RuTexts;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CommandsHandler {

    private static final Logger log = Logger.getLogger(CommandsHandler.class);

    public static void handleAllCommands(String messageText, long chatId, Integer messageId, boolean isUpdatePersonalDirectMessage, Update update) {

        String currentChatLanguage = UserSettingsHandler.getLanguageToCurrentUser(chatId).toLowerCase();

        if (messageText.contains("/help")) { // Print HELP for all messages in ONE message
            log.info("Message text contains /help - show commandsAndTexts list");
            StringBuilder helpText = new StringBuilder();

            if(currentChatLanguage.contains("en")){
                for (CommandsEn commands : CommandsEn.values()) {
                    helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
                }
            }
            else if(currentChatLanguage.contains("ru")){
                for (CommandsRu commands : CommandsRu.values()) {
                    helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
                }
            }
            Main.bot.sendReplyMessageToChatID(chatId, helpText.toString(), messageId);
        } else { // Print help to command and handle it
            log.info("Message text contains / - it's CAN be a command");
            String helpText = "";

            try {
                boolean enteredCommandWithValue = true;
                if(currentChatLanguage.equals("en")){
                    for (CommandsEn commands : CommandsEn.values()) { // send help for command
                        if (messageText.toLowerCase().equals("/" + commands.name().toLowerCase() + "@" + Main.bot.getBotUsername())) {
                            log.info("Message test contains - command name: " + commands.name());
                            helpText = commands.value;
                            Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
                            enteredCommandWithValue = false; // if command from standart default list without value
                        }
                    }
                }
                else if(currentChatLanguage.equals("ru")){
                    for (CommandsRu commands : CommandsRu.values()) { // send help for command
                        if (messageText.toLowerCase().equals("/" + commands.name().toLowerCase() + "@" + Main.bot.getBotUsername())) {
                            log.info("Message test contains - command name: " + commands.name());
                            helpText = commands.value;
                            Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
                            enteredCommandWithValue = false; // if command from standart default list without value
                        }
                    }
                }
                if(enteredCommandWithValue){ // if command not exactly in list because contains options value we must handle it separate
                    recognizeAndHandleCommand(messageText, isUpdatePersonalDirectMessage, chatId, update);
                }
            } catch (Exception e) {
                log.info("Something going wrong. Command not recognized or is not in command list: " + messageText + " " + e.toString());
            }
        }
    }

    public static void recognizeAndHandleCommand(String incCommand, boolean isUpdatePersonalDirectMessage, long chatId, Update update) {
        String command = incCommand.toLowerCase();
        // handle default language command for bot
        if (command.contains(CommandsEn.defaultLanguageAdm.name().toLowerCase()) && !isUpdatePersonalDirectMessage) {
            if(Main.bot.isUserAdminInChat(update.getMessage().getFrom().getId(), chatId)){
                // if user admin in chat
                if (command.contains("en")) {
                    UserSettingsHandler.setSetupOptionValueInMemory(CommandsEn.defaultLanguageAdm.name(), "En", chatId);
                    Main.bot.sendMessageToChatID(chatId, EnTexts.changeDefaultLanguage.value + " English ");
                }
                else if (command.contains("ru")) {
                    UserSettingsHandler.setSetupOptionValueInMemory(CommandsEn.defaultLanguageAdm.name(), "Ru", chatId);
                    Main.bot.sendMessageToChatID(chatId, RuTexts.changeDefaultLanguage.value + " Русский ");
                }
            }
            else { // if not an admin
                Main.bot.sendMessageToChatID(chatId, EnTexts.adminCheckWrong.name(), true);
            }
        }
    }
}
