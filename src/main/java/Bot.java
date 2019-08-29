import commandsAndTexts.commands.CommandsEn;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot extends TelegramLongPollingBot {

    private static final Logger log = Logger.getLogger(Bot.class);

    public void onUpdateReceived(Update update) {

        log.info("Full update: " + update.toString());

        String regex = "(.)*(\\d)(.)*"; // for check digits in answer
        long currentDateTime = (new Date().getTime()) / 1000;
        Pattern pattern = Pattern.compile(regex);
        long chatId = update.getMessage().getChatId();
        boolean isUpdateFromBot = false, isUpdateContainsReply = false, replyMessageInChatContainsBotName = false, messageInChatContainsBotName = false, isUpdateContainsPersonalPrivateMessageToBot = false;
        Message replyMessage = null;

        try {
            replyMessage = update.getMessage().getReplyToMessage();
            if (replyMessage != null) {
                isUpdateContainsReply = true;
                log.info("Update contains reply message: " + replyMessage);
            }
        } catch (Exception e) {
            replyMessage = null;
        }
        try {
            update.getMessage().getFrom().getBot();
            if (isUpdateFromBot) {
                log.info("Update from bot. Ignoring.");
            }
        } catch (Exception e) {
            isUpdateFromBot = false;
        }

        // LEFT MEMBERS update handling we must remove it from newbies list if user from there
        if (update.getMessage().getLeftChatMember() != null) {
            User leftChatMember = update.getMessage().getLeftChatMember();
            if (!leftChatMember.getBot()) {
                removeLeftMemberFromNewbieList(leftChatMember.getId());
            }
        }

        // Periodic task to check users who doesn't say everything
        log.info("Current newbie lists size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
        checkAndRemoveAllSilentUsers(currentDateTime);

        // NEW MEMBERS update handling with attention message: -------------------------------->
        newMembersWarningMessageAndQuestionGeneration
                ("Hi! ATTENTION! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned. You have " + SettingsForBotGlobal.timePeriodForSilentUsersByDefault.value + " seconds. How much will be ", update);

        // MESSAGES HANDLING ------------------------------------------------------------>
        if (update.hasMessage()) {

            String messageText = update.getMessage().getText();
            Integer messageId = update.getMessage().getMessageId();

            // is it reply message contains bot name?
            if (replyMessage != null) {
                try {
                    log.info("Reply message contains name: " + update.getMessage().getReplyToMessage().getFrom().getUserName());
                    replyMessageInChatContainsBotName = update.getMessage().getReplyToMessage().getFrom().getUserName().equals(getBotUsername());
                } catch (Exception e) {
                    replyMessageInChatContainsBotName = false;
                } finally {
                    log.info("Is reply message name contains bot name status: " + replyMessageInChatContainsBotName);
                }
            }

            // if it direct message in chat, check that message contains bot name
            try {
                messageInChatContainsBotName = messageText.contains("@" + getBotUsername());
            } catch (NullPointerException e) {
                messageInChatContainsBotName = false;
            } finally {
                if (replyMessage == null) {
                    log.info("Chat message contains praetorian bot name: " + messageInChatContainsBotName);
                }
            }

            // if it personal message in personal direct chat
            try {
                if (update.getMessage().getChat().isUserChat()) {
                    isUpdateContainsPersonalPrivateMessageToBot = true;
                    log.info("Update is private message to bot.");
                }
            } catch (Exception e) {
                isUpdateContainsPersonalPrivateMessageToBot = false;
            }

            // ------------- CHECK MENTIONS IN CHAT MESSAGE OR IN REPLY

            if (messageInChatContainsBotName || replyMessageInChatContainsBotName || isUpdateContainsPersonalPrivateMessageToBot) {
                // user also can answer in personal private messages to bot

                // COMMANDS HANDLING -------------------------------->
                if (messageText != null && messageText.contains("/")) {
                    handleAllCommands(messageText, chatId, messageId, isUpdateContainsPersonalPrivateMessageToBot);
                }

                // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                if (MainInit.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) /* if user in newbie list */ && !isUpdateContainsPersonalPrivateMessageToBot) {
                    validateNewbieAnswer(update, messageText, pattern, chatId, messageId, currentDateTime);
                }
            } else { // no mentions of bot or personal private messages to him

                // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                if (MainInit.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) /* if user in newbie list */ && !isUpdateContainsPersonalPrivateMessageToBot) {
                    validateNewbieAnswer(update, messageText, pattern, chatId, messageId, currentDateTime);
                }
            }

        } else { // If we get update without message

        }
    }

    /* ----------------------------- MAIN METHODS ------------------------------------------------------------ */

    private void checkAndRemoveAllSilentUsers(long currentDateTime) {
        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>) MainInit.newbieMapWithAnswer.entrySet()) {

            log.info("Iterating newbie lists.");

            Integer userIdFromMainClass = pair.getKey();
            Long joinTimeFromMainClass = MainInit.newbieMapWithJoinTime.get(userIdFromMainClass);
            Long chatIdFromMainClass = MainInit.newbieMapWithChatId.get(userIdFromMainClass);

            log.info("Current date time: " + currentDateTime + " || Join member datetime: " + joinTimeFromMainClass + " || Difference: " + (currentDateTime - joinTimeFromMainClass));

            if ((currentDateTime - joinTimeFromMainClass) > Long.valueOf(SettingsForBotGlobal.timePeriodForSilentUsersByDefault.value)) {

                log.info("Difference bigger then defined value! " + userIdFromMainClass + " will be kicked");
                kickChatMember(chatIdFromMainClass, userIdFromMainClass, currentDateTime, 3000000);

                MainInit.newbieMapWithAnswer.remove(userIdFromMainClass);
                MainInit.newbieMapWithJoinTime.remove(userIdFromMainClass);
                MainInit.newbieMapWithChatId.remove(userIdFromMainClass);

                log.info("Silent user removed. Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                sendMessageToChatID(chatIdFromMainClass, userIdFromMainClass + " < silent user was removed after delay. Meow!");
            }
        }
    }

    private void kickChatMember(long chatId, int userId, long currentDateTime, int untilDateInSeconds) {
        KickChatMember kickChatMember = new KickChatMember();
        kickChatMember.setChatId(chatId)
                .setUserId(userId)
                .setUntilDate(((int) currentDateTime) + untilDateInSeconds);
        try {
            execute(kickChatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private int normalizeUserAnswer(String stringToNormalize) {
        try { // try to normalize string
            String tmpNewbieAnswer = stringToNormalize.replaceAll("\\s", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([a-z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([A-Z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([а-я])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([А-Я])", "");
            return Integer.valueOf(tmpNewbieAnswer);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void validateNewbieAnswer(Update update, String messageText, Pattern pattern, long chatId,
                                      int messageId, long currentDateTime) {

        log.info("User which posted message is NEWBIE. Check initialising.");

        Integer newbieId = update.getMessage().getFrom().getId();
        Integer generatedNewbieAnswerDigit = MainInit.newbieMapWithAnswer.get(newbieId);
        Integer currentNewbieAnswer = 0;

        String answerText = "";

        if (messageText == null) { // it can be sticker or picture and it must be deleted immediately
            MainInit.newbieMapWithAnswer.remove(newbieId);
            MainInit.newbieMapWithJoinTime.remove(newbieId);
            MainInit.newbieMapWithChatId.remove(newbieId);

            log.info("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

            answerText += "Wrong. Sorry entered data is not match with generated one. You will be banned!";

            sendReplyMessageToChatID(chatId, answerText, messageId);

            // DELETE FIRST WRONG MESSAGE FROM USER
            deleteMessage(chatId, messageId);
            kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
            sendMessageToChatID(chatId, update.getMessage().getFrom().getUserName() + " banned and spamm deleted! Praetorians at your service. Meow!");

        } else { // if message contains any text data

            currentNewbieAnswer = normalizeUserAnswer(messageText);

            log.info("Normalized newbie answer is: " + currentNewbieAnswer);

            Matcher matcher = pattern.matcher(messageText); // contain at least digits

            if (matcher.matches()) { // if contains at least digits
                log.info("Message to bot contains REGEX digital pattern");

                if (currentNewbieAnswer.equals(generatedNewbieAnswerDigit)) { // if user gives us right answer

                    MainInit.newbieMapWithAnswer.remove(newbieId);
                    MainInit.newbieMapWithJoinTime.remove(newbieId);
                    MainInit.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                    answerText += "Right! Now you can send messages to group. Have a nice chatting.";

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                } else { // if user gives us WRONG answer

                    MainInit.newbieMapWithAnswer.remove(newbieId);
                    MainInit.newbieMapWithJoinTime.remove(newbieId);
                    MainInit.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                    answerText += "Wrong. Sorry entered data is not match with generated one. You will be banned!";

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                    // DELETE FIRST WRONG MESSAGE FROM USER
                    deleteMessage(chatId, messageId);
                    kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
                    sendMessageToChatID(chatId, update.getMessage().getFrom() + " banned and spamm deleted! Praetorians at your service. Meow!");
                }
            } else { // if message from newbie but not contains digit answer
                log.info("Message to bot NOT contains any digits. Ban and delete from newbie list.");

                int userId = update.getMessage().getFrom().getId();

                MainInit.newbieMapWithAnswer.remove(userId);
                MainInit.newbieMapWithJoinTime.remove(userId);
                MainInit.newbieMapWithChatId.remove(userId);

                log.info("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
                sendReplyMessageToChatID(chatId,
                        "Wrong. Sorry entered DATA contains only letters. You will be banned!", messageId);

                // DELETE FIRST WRONG MESSAGE FROM USER
                deleteMessageAndSayText(chatId, messageId,
                        update.getMessage().getFrom() + " banned and spamm deleted! Praetorians at your service. Meow!");
            }
        }


    }

    private void handleAllCommands(String messageText, long chatId, Integer messageId, boolean isUpdatePersonalDirectMessage) {
        if (messageText.contains("/help")) { // Print HELP for all messages in ONE message
            log.info("Message text contains /help - show commandsAndTexts list");
            StringBuilder helpText = new StringBuilder();
            for (CommandsEn commands : CommandsEn.values()) {
                helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
            }
            sendReplyMessageToChatID(chatId, helpText.toString(), messageId);

        } else { // Print help to command and handle it
            log.info("Message text contains / - it's a command");
            String helpText = "";

            for (CommandsEn commands : CommandsEn.values()) {
                if (messageText.contains(commands.name())) {
                    log.info("Message test contains - command name: " + commands.name());
                    helpText = commands.value;
                }
            }

            sendReplyMessageToChatID(chatId, helpText, messageId);

        }
    }

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId).setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteMessageAndSayText(long chatId, int messageId, String textToSay) {
        deleteMessage(chatId, messageId);
        sendMessageToChatID(chatId, textToSay);
    }

    private void sendMessageToChatID(long chatId, String messageText) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendReplyMessageToChatID(long chatId, String messageText, int replyToMessageId) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setReplyToMessageId(replyToMessageId)
                .setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void removeLeftMemberFromNewbieList(int leftUserId) {
        if (MainInit.newbieMapWithAnswer.containsKey(leftUserId)) {
            log.info("Silent user: " + leftUserId + " left or was removed from group. It should be deleted from all lists.");
            MainInit.newbieMapWithAnswer.remove(leftUserId);
            MainInit.newbieMapWithJoinTime.remove(leftUserId);
            MainInit.newbieMapWithChatId.remove(leftUserId);
            log.info("Newbie list size: " + +MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
        }
    }

    public void newMembersWarningMessageAndQuestionGeneration(String warningMessage, Update update) {

        if (!update.getMessage().getNewChatMembers().isEmpty()) {
            List<User> newUsersMembersList = update.getMessage().getNewChatMembers();
            log.info("We have an update with a new chat members (" + newUsersMembersList.size() + ")");
            Integer messageId = update.getMessage().getMessageId();

            for (User user : newUsersMembersList) {
                if (!user.getBot()) { // Is added users is bot?
                    log.info("User is not bot. Processing.");

                    int userId = user.getId();
                    int randomDigit = (int) (Math.random() * 100);
                    int randomDigit2 = (int) (Math.random() * 100);
                    int answerDigit = randomDigit + randomDigit2;
                    String helloText = warningMessage + " " + randomDigit + " + " + randomDigit2;

                    MainInit.newbieMapWithAnswer.put(userId, answerDigit);
                    MainInit.newbieMapWithJoinTime.put(userId, new Date().getTime() / 1000);
                    long chatId = update.getMessage().getChatId();
                    MainInit.newbieMapWithChatId.put(userId, chatId);

                    log.info("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
                    sendMessageToChatID(chatId, helloText);

                } else {
                    log.info("User is bot! Ignoring.");
                }
            }
        }
    }



    public String getBotUsername() {
        // Return bot username
        if (SettingsForBotGlobal.botType.value.equals("true")) {
            return SettingsForBotGlobal.nameForProduction.value;
        } else {
            return SettingsForBotGlobal.nameForTest.value;
        }
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        if (SettingsForBotGlobal.botType.value.equals("true")) {
            return SettingsForBotGlobal.tokenForProduction.value;
        } else {
            return SettingsForBotGlobal.tokenForTest.value;
        }
    }
}
