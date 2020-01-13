import commandsAndTexts.commands.CommandsEn;
import commandsAndTexts.commands.CommandsRu;
import commandsAndTexts.texts.EnTexts;
import commandsAndTexts.texts.RuTexts;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CommandsHandler {

    private static final Logger log = Logger.getLogger(CommandsHandler.class);

    public static void handleAllCommands(String messageText, long chatId, Integer messageId, boolean isUpdatePersonalDirectMessage, Update update) {

        String currentChatLanguage = ChatSettingsHandler.getLanguageOptionForChat(chatId).toLowerCase();

        /* ---------------- Print HELP for all messages in ONE message */
        if (messageText.contains("/help")) {
            log.info("Message text contains /help - show commandsAndTexts list");
            StringBuilder helpText = new StringBuilder();

            if (currentChatLanguage.contains("en")) {
                for (CommandsEn commands : CommandsEn.values()) {
                    helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
                }
            } else if (currentChatLanguage.contains("ru")) {
                for (CommandsRu commands : CommandsRu.values()) {
                    helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
                }
            }
            Main.bot.sendReplyMessageToChatID(chatId, helpText.toString(), messageId);
        } else {

            /* --------------- Print help to command and handle it ---- */

            log.info("Message text contains / - it's CAN be a command");
            String helpText = "";
            int messageWithCommandLength = messageText.length();

            if(messageText.contains(SettingsForBotGlobal.nameForProduction.value) ||
                    messageText.contains(SettingsForBotGlobal.nameForTest.value)){
                if(messageText.contains(SettingsForBotGlobal.nameForProduction.value)){

                    // if message with @ in chat
                    messageWithCommandLength -= SettingsForBotGlobal.nameForProduction.value.length();
                }
                else {
                    messageWithCommandLength -= SettingsForBotGlobal.nameForTest.value.length();
                }
                if(messageText.contains("@")){
                    messageWithCommandLength -= 1;
                }
                if(messageText.contains("/")){
                    messageWithCommandLength -= 1;
                }
            }
            else { // if message is reply to bot message
                if(messageText.contains("@")){
                    messageWithCommandLength -= 1;
                }
                if(messageText.contains("/")){
                    messageWithCommandLength -= 1;
                }
            }

            // handle command without options - show help, recognizing if command with option or not implemented by
            // commands length

            if (currentChatLanguage.equals("en")) {
                for (CommandsEn commandFromCommandsList : CommandsEn.values()) { // send help for command

                    if (messageText.toLowerCase().contains(commandFromCommandsList.name().toLowerCase()) &&
                            messageWithCommandLength == (commandFromCommandsList.name().toLowerCase()).length()) {

                        // handle command without options - show help text
                        log.info("Message text contains - command name: " + commandFromCommandsList.name());
                        helpText = commandFromCommandsList.value;
                        Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
                        return;
                    }
                    if (messageText.toLowerCase().contains(commandFromCommandsList.name().toLowerCase())) {
                        recognizeAndHandleCommand(messageText, isUpdatePersonalDirectMessage, chatId, update);
                        return;
                    }
                }
            } else if (currentChatLanguage.equals("ru")) {
                for (CommandsRu commandFromCommandsList : CommandsRu.values()) { // send help for command

                    if (messageText.toLowerCase().contains(commandFromCommandsList.name().toLowerCase()) &&
                            messageWithCommandLength == (commandFromCommandsList.name().toLowerCase()).length()) {

                        // handle command without options - show help text
                        log.info("Message text contains - command name: " + commandFromCommandsList.name());
                        helpText = commandFromCommandsList.value;
                        Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
                        return;
                    }
                    if (messageText.toLowerCase().contains(commandFromCommandsList.name().toLowerCase())) {
                        recognizeAndHandleCommand(messageText, isUpdatePersonalDirectMessage, chatId, update);
                        return;
                    }
                }
            }

            // handle command with options


//            try {
//                boolean enteredCommandWithValue = true;
//
//                // Show help for command ----------------------------- */
//                if (currentChatLanguage.equals("en")) {
//                    for (CommandsEn commands : CommandsEn.values()) { // send help for command
//                        if (messageText.toLowerCase().equals("/" + commands.name().toLowerCase() + "@" + Main.bot.getBotUsername())) {
//                            log.info("Message text contains - command name: " + commands.name());
//                            helpText = commands.value;
//                            Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
//                            enteredCommandWithValue = false; // if command from default list without value
//                        }
//                    }
//                } else if (currentChatLanguage.equals("ru")) {
//                    for (CommandsRu commands : CommandsRu.values()) { // send help for command
//                        if (messageText.toLowerCase().equals("/" + commands.name().toLowerCase() + "@" + Main.bot.getBotUsername())) {
//                            log.info("Message text contains - command name: " + commands.name());
//                            helpText = commands.value;
//                            Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId); // sending help
//                            enteredCommandWithValue = false; // if command from default list without value
//                        }
//                    }
//                }
//
//                // if command not exactly in list because contains options value we must handle it separate
//                if (enteredCommandWithValue) {
//                    recognizeAndHandleCommand(messageText, isUpdatePersonalDirectMessage, chatId, update);
//                }
//
//            } catch (Exception e) {
//                log.info("Something going wrong. Command not recognized or is not in command list: " + messageText + " " + e.toString());
//            }
        }
    }

    /* --------------------------------- COMMANDS HANDLING ------------------------------- */

    public static void recognizeAndHandleCommand(String incCommand, boolean isUpdatePersonalDirectMessage, long chatId, Update update) {
        String command = null;

        if (incCommand.toLowerCase().contains(CommandsEn.welcometext.name())) {
            command = incCommand;
        } else { // in lower case welcome text shows in chat not properly
            command = incCommand.toLowerCase();
        }
        incCommand = null;
        final String currentChatLanguage = ChatSettingsHandler.getLanguageOptionForChat(chatId).toLowerCase();

        // ----------- handle default language command for bot
        if (command.contains(CommandsEn.defaultlanguageadm.name().toLowerCase()) && !isUpdatePersonalDirectMessage) {
            if (Main.bot.isUserAdminInChat(update.getMessage().getFrom().getId(), chatId)) {
                // if user admin in chat
                log.warn("Defining chat language option for chat: " + chatId);
                if (command.contains("en")) {
                    ChatSettingsHandler.setSetupOptionValueInMemory(CommandsEn.defaultlanguageadm.name(), "En", chatId);
                    Main.bot.sendMessageToChatID(chatId, EnTexts.changeDefaultLanguage.value + " English ");
                    return;
                } else if (command.contains("ru")) {
                    ChatSettingsHandler.setSetupOptionValueInMemory(CommandsEn.defaultlanguageadm.name(), "Ru", chatId);
                    Main.bot.sendMessageToChatID(chatId, RuTexts.changeDefaultLanguage.value + " Русский ");
                    return;
                }
            } else { // if not an admin
                Main.bot.sendMessageToChatID(chatId, EnTexts.adminCheckWrong.name(), true);
                return;
            }
        }

        // ----------- welcome text defining
        if (command.contains(CommandsEn.welcometext.name().toLowerCase()) && !isUpdatePersonalDirectMessage &&
                Main.bot.isUserAdminInChat(update.getMessage().getFrom().getId(), chatId)) {
            log.warn("Defining welcome text for chatId: " + chatId);

            if (command.length() > (CommandsEn.welcometext.name().length() + 300)) {
                log.warn("Entered welcome text is longer then expected! For chat " + chatId);
                Main.bot.sendMessageToChatID(chatId, EnTexts.optionSetError.name(), true);
            } else {
                final String welcomeText = command.substring(CommandsEn.welcometext.name().length() + 1).replaceAll("\"", "'");
                ChatSettingsHandler.setSetupOptionValueInMemory(CommandsEn.welcometext.name(), welcomeText, chatId);
                log.warn("Entered text is set! For chat " + chatId);
                if (currentChatLanguage.contains("en")) {
                    Main.bot.sendMessageToChatID(chatId, EnTexts.optionSetSuccess.name(), true);
                } else {
                    Main.bot.sendMessageToChatID(chatId, RuTexts.optionSetSuccess.name(), true);
                }
            }
            return;
        } else {
            Main.bot.sendMessageToChatID(chatId, EnTexts.optionSetError.name(), true);
        }
    }
}
