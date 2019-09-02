import commandsAndTexts.texts.EnTexts;
import commandsAndTexts.texts.RuTexts;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot extends TelegramLongPollingBot {

    public static final Logger log = Logger.getLogger(Bot.class);
    public static Update currentUpdate;

    static {
        log.info("Bot successfully initialised :3 ");
    }

    public void onUpdateReceived(Update update) {

        log.info("Full update: " + update.toString());
        currentUpdate = update;

        String regex = "(.)*(\\d)(.)*"; // for check digits in answer
        long currentDateTime = (new Date().getTime()) / 1000;
        Pattern pattern = Pattern.compile(regex);
        long chatId = update.getMessage().getChatId();
        boolean isUpdateFromBot = false, isUpdateContainsReply = false, replyMessageInChatContainsBotName = false, messageInChatContainsBotName = false, isUpdateContainsPersonalpublicMessageToBot = false;
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
        log.info("Current newbie lists size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
        checkAndRemoveAllSilentUsers(currentDateTime);

        // NEW MEMBERS update handling with attention message: -------------------------------->
        if (!Bot.currentUpdate.getMessage().getNewChatMembers().isEmpty()) {
            newMembersWarningMessageAndQuestionGeneration();
        }
        else {

            // MESSAGES HANDLING ------------------------------------------------------------>
            if (update.hasMessage()) {

                String messageText = update.getMessage().getText();
                int messageId = update.getMessage().getMessageId();

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
                        isUpdateContainsPersonalpublicMessageToBot = true;
                        log.info("Update is public message to bot in chat.");
                    }
                } catch (Exception e) {
                    isUpdateContainsPersonalpublicMessageToBot = false;
                }

                // ------------- CHECK MENTIONS IN CHAT MESSAGE OR IN REPLY

                if (messageInChatContainsBotName || replyMessageInChatContainsBotName || isUpdateContainsPersonalpublicMessageToBot) {
                    // user also can answer in personal public messages to bot

                    // COMMANDS HANDLING -------------------------------->
                    if (messageText != null && messageText.contains("/")) {
                        CommandsHandler.handleAllCommands(messageText, chatId, messageId, isUpdateContainsPersonalpublicMessageToBot, update);
                    }

                    // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                    if (Main.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) /* if user in newbie list */ && !isUpdateContainsPersonalpublicMessageToBot) {
                        validateNewbieAnswer(update, messageText, pattern, chatId, messageId, currentDateTime);
                    }
                } else { // no mentions of bot or personal public messages to him

                    // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                    if (Main.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) /* if user in newbie list */ && !isUpdateContainsPersonalpublicMessageToBot) {
                        validateNewbieAnswer(update, messageText, pattern, chatId, messageId, currentDateTime);
                    }
                }

            } else { // If we get update without message

            }
        }
    }

    /* ----------------------------- MAIN METHODS ------------------------------------------------------------ */

    public void checkAndRemoveAllSilentUsers(long currentDateTime) {
        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>) Main.newbieMapWithAnswer.entrySet()) {

            log.info("Iterating newbie lists.");

            Integer userIdFromMainClass = pair.getKey();
            Long joinTimeFromMainClass = Main.newbieMapWithJoinTime.get(userIdFromMainClass);
            Long chatIdFromMainClass = Main.newbieMapWithChatId.get(userIdFromMainClass);

            log.info("Current date time: " + currentDateTime + " || Join member datetime: " + joinTimeFromMainClass + " || Difference: " + (currentDateTime - joinTimeFromMainClass));

            if ((currentDateTime - joinTimeFromMainClass) > Long.valueOf(SettingsForBotGlobal.timePeriodForSilentUsersByDefault.value)) {

                log.info("Difference bigger then defined value! " + userIdFromMainClass + " will be kicked");
                kickChatMember(chatIdFromMainClass, userIdFromMainClass, currentDateTime, 3000000);

                Main.newbieMapWithAnswer.remove(userIdFromMainClass);
                Main.newbieMapWithJoinTime.remove(userIdFromMainClass);
                Main.newbieMapWithChatId.remove(userIdFromMainClass);

                log.info("Silent user removed. Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
                String textToSay = getTemplateTextForCurrentLanguage(EnTexts.removedSilentUser.name(), chatIdFromMainClass);
                sendMessageToChatID(chatIdFromMainClass, userIdFromMainClass + textToSay);
            }
        }
    }

    public void kickChatMember(long chatId, int userId, long currentDateTime, int untilDateInSeconds) {
        KickChatMember kickChatMember = new KickChatMember();
        kickChatMember.setChatId(chatId)
                .setUserId(userId)
                .setUntilDate(((int) currentDateTime) + untilDateInSeconds);
        try {
            execute(kickChatMember);
        } catch (TelegramApiException e) {
            log.info("Error while try to kick user " + userId + " in chat id " + chatId + e.toString());
        }
    }

    public ArrayList<ChatMember> getChatAdmins(long chatId) {
        GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
        getChatAdministrators.setChatId(chatId);
        try {
            log.info("Getting chat admins in chatId " + chatId);
            return execute(getChatAdministrators);
        } catch (Exception e) {
            log.info("Error while trying to get admins list in chatId " + chatId + " " + e.toString());
        }
        log.info("Something going wrong while try to get chat admins (return null) in chatId " + chatId);
        return null;
    }

    public boolean isUserAdminInChat(int userId, long chatId) {
        log.info("Checking user " + userId + " is admin in chat: " + chatId);
        ArrayList<ChatMember> adminsList = getChatAdmins(chatId);
        for (ChatMember chatMember : adminsList) {
            int chatMemberIdFromList = chatMember.getUser().getId();
            if (chatMemberIdFromList == userId) {
                log.info("Checked user " + userId + " is admin in chat: " + chatId);
                return true;
            }
        }
        log.info("Error while trying to get admin status or user isn't in admin list. Return false by default.");
        return false;
    }

    public int normalizeUserAnswer(String stringToNormalize) {
        try { // try to normalize string
            String tmpNewbieAnswer = stringToNormalize.replaceAll("\\s", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([a-z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([A-Z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([а-я])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([А-Я])", "");
            return Integer.valueOf(tmpNewbieAnswer);
        } catch (NumberFormatException e) {
            log.info("Can't normalize string. Return 0 by default.");
            return 0;
        }
    }

    public void validateNewbieAnswer(Update update, String messageText, Pattern pattern, long chatId,
                                     int messageId, long currentDateTime) {

        log.info("User which posted message is NEWBIE. Check initialising.");

        Integer newbieId = update.getMessage().getFrom().getId();
        Integer generatedNewbieAnswerDigit = Main.newbieMapWithAnswer.get(newbieId);
        Integer currentNewbieAnswer = 0;

        String answerText = "";

        if (messageText == null) { // it can be sticker or picture and it must be deleted immediately
            Main.newbieMapWithAnswer.remove(newbieId);
            Main.newbieMapWithJoinTime.remove(newbieId);
            Main.newbieMapWithChatId.remove(newbieId);

            log.info("Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

            answerText += getTemplateTextForCurrentLanguage(EnTexts.newbieAnswerNotEqualsToGeneratedOne.name(), chatId);
            sendReplyMessageToChatID(chatId, answerText, messageId);

            // DELETE FIRST WRONG MESSAGE FROM USER
            deleteMessage(chatId, messageId);
            kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
            sendMessageToChatID(chatId, getTemplateTextForCurrentLanguage(EnTexts.spammerBanned.name(), chatId));

        } else { // if message contains any text data

            currentNewbieAnswer = normalizeUserAnswer(messageText);
            log.info("Normalized newbie answer is: " + currentNewbieAnswer);
            Matcher matcher = pattern.matcher(messageText); // contain at least digits

            if (matcher.matches()) { // if contains at least digits
                log.info("Message to bot contains REGEX digital pattern");

                if (currentNewbieAnswer.equals(generatedNewbieAnswerDigit)) { // if user gives us right answer

                    Main.newbieMapWithAnswer.remove(newbieId);
                    Main.newbieMapWithJoinTime.remove(newbieId);
                    Main.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                    answerText += getTemplateTextForCurrentLanguage(EnTexts.newbieCheckSuccess.name(), chatId);

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                } else { // if user gives us WRONG answer

                    Main.newbieMapWithAnswer.remove(newbieId);
                    Main.newbieMapWithJoinTime.remove(newbieId);
                    Main.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                    answerText += getTemplateTextForCurrentLanguage(EnTexts.newbieAnswerNotEqualsToGeneratedOne.name(), chatId);

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                    // DELETE FIRST WRONG MESSAGE FROM USER
                    deleteMessage(chatId, messageId);
                    kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
                    String bannedTextMessage = getTemplateTextForCurrentLanguage(EnTexts.spammerBanned.name(), chatId);

                    sendMessageToChatID(chatId, bannedTextMessage);
                }
            } else { // if message from newbie but not contains digit answer
                log.info("Message to bot NOT contains any digits. Ban and delete from newbie list.");

                int userId = update.getMessage().getFrom().getId();

                Main.newbieMapWithAnswer.remove(userId);
                Main.newbieMapWithJoinTime.remove(userId);
                Main.newbieMapWithChatId.remove(userId);

                log.info("Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
                sendReplyMessageToChatID(chatId, EnTexts.newbieAnswerContainsOnlyLetters.name(), messageId, true);

                // DELETE FIRST WRONG MESSAGE FROM USER
                String bannedTextMessage = getTemplateTextForCurrentLanguage(EnTexts.spammerBanned.name(), chatId);
                deleteMessageAndSayText(chatId, messageId, bannedTextMessage);
            }
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId).setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (Exception e) {
            log.info("Error while trying to delete message in chatId " + chatId + " " + e.toString());
        }
    }

    public void deleteMessageAndSayText(long chatId, int messageId, String textToSay) {
        deleteMessage(chatId, messageId);
        sendMessageToChatID(chatId, textToSay);
    }

    public void sendMessageToChatID(long chatId, String messageText) {
        if (messageText == null || messageText.equals("")) {
            log.info("Send text for message is empty. Nothing to say.");
            return;
        }
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.info("Error while trying to send message in chatId " + chatId + " " + e.toString());
        }
    }

    public void sendMessageToChatID(long chatId, String messageText, boolean messageTextIsTemplateText) {
        String language = UserSettingsHandler.getLanguageToCurrentUser(chatId).toLowerCase();
        if (language.contains("ru") && messageTextIsTemplateText) {
            sendMessageToChatID(chatId, RuTexts.getValueForKey(messageText));
        } else if (language.contains("en") && messageTextIsTemplateText) {
            sendMessageToChatID(chatId, EnTexts.getValueForKey(messageText));
        } else {
            sendMessageToChatID(chatId, messageText);
        }
    }

    public void sendReplyMessageToChatID(long chatId, String messageText, int replyToMessageId) {
        if (messageText == null || messageText.equals("")) {
            log.info("Send text for message is empty. Nothing to say in reply.");
            return;
        }
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setReplyToMessageId(replyToMessageId)
                .setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.info("Error while trying to answer in chatId " + chatId + " to message " + replyToMessageId + " " + e.toString());
        }
    }

    public void sendReplyMessageToChatID(long chatId, String messageText, int replyToMessageId, boolean messageTextIsTemplateText) {
        String language = UserSettingsHandler.getLanguageToCurrentUser(chatId).toLowerCase();
        if (language.contains("ru") && messageTextIsTemplateText) {
            sendReplyMessageToChatID(chatId, RuTexts.getValueForKey(messageText), replyToMessageId);
        } else if (language.contains("en") && messageTextIsTemplateText) {
            sendReplyMessageToChatID(chatId, EnTexts.getValueForKey(messageText), replyToMessageId);
        } else {
            sendReplyMessageToChatID(chatId, messageText, replyToMessageId);
        }
    }

    public void removeLeftMemberFromNewbieList(int leftUserId) {
        if (Main.newbieMapWithAnswer.containsKey(leftUserId)) {
            log.info("Silent user: " + leftUserId + " left or was removed from group. It should be deleted from all lists.");
            Main.newbieMapWithAnswer.remove(leftUserId);
            Main.newbieMapWithJoinTime.remove(leftUserId);
            Main.newbieMapWithChatId.remove(leftUserId);
            log.info("Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
        }
    }

    public void newMembersWarningMessageAndQuestionGeneration() {

        List<User> newUsersMembersList = Bot.currentUpdate.getMessage().getNewChatMembers();
        log.info("We have an update with a new chat members (" + newUsersMembersList.size() + ")");
        Integer messageId = Bot.currentUpdate.getMessage().getMessageId();

        for (User user : newUsersMembersList) {
            if (!user.getBot()) { // Is added users is bot?
                log.info("User is not bot. Processing.");

                int userId = user.getId();
                int randomDigit = (int) (Math.random() * 100);
                int randomDigit2 = (int) (Math.random() * 100);
                int answerDigit = randomDigit + randomDigit2;

                Main.newbieMapWithAnswer.put(userId, answerDigit);
                Main.newbieMapWithJoinTime.put(userId, new Date().getTime() / 1000);
                long chatId = Bot.currentUpdate.getMessage().getChatId();
                Main.newbieMapWithChatId.put(userId, chatId);

                log.info("Newbie list size: " + Main.newbieMapWithAnswer.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                // Send warning greetings message with generated digits
                String warningMessage = getTemplateTextForCurrentLanguage(EnTexts.defaultGreetings.name(), chatId);
                sendMessageToChatID(chatId, warningMessage + " " + randomDigit + " + " + randomDigit2);
            } else {
                log.info("User is bot! Ignoring.");
            }
        }
    }

    public String getTemplateTextForCurrentLanguage(String templateTextName, long chatId) {
        String language = UserSettingsHandler.getLanguageToCurrentUser(chatId).toLowerCase();
        if (language.contains("ru")) {
            return RuTexts.getValueForKey(templateTextName);
        } else if (language.contains("en")) {
            return EnTexts.getValueForKey(templateTextName);
        } else {
            log.info("Sorry no template text found for language " + language + " for phrase template " + templateTextName);
            return null;
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
