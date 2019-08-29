import commandsAndTexts.commands.CommandsEn;
import org.apache.log4j.Logger;

public class CommandsHandler {

    private static final Logger log = Logger.getLogger(CommandsHandler.class);

    public static void handleAllCommands(String messageText, long chatId, Integer messageId, boolean isUpdatePersonalDirectMessage){
        if (messageText.contains("/help")) { // Print HELP for all messages in ONE message
            log.info("Message text contains /help - show commandsAndTexts list");
            StringBuilder helpText = new StringBuilder();
            for (CommandsEn commands : CommandsEn.values()) {
                helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
            }
            Main.bot.sendReplyMessageToChatID(chatId, helpText.toString(), messageId);

        } else { // Print help to command and handle it
            log.info("Message text contains / - it's a command");
            String helpText = "";

            for (CommandsEn commands : CommandsEn.values()) {
                if (messageText.contains(commands.name())) {
                    log.info("Message test contains - command name: " + commands.name());
                    helpText = commands.value;
                }
            }
            Main.bot.sendReplyMessageToChatID(chatId, helpText, messageId);
        }
    }

    public static void recognizeAndHandleCommand(String commandText, String commandValue, boolean isUpdatePersonalDirectMessage, long chatId){

        if(commandText.contains(CommandsEn.defaultLanguageAdm.value)){

        }



    }



}
