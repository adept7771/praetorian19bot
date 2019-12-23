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
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot extends TelegramLongPollingBot {

    public static final Logger log = Logger.getLogger(Bot.class);
    public static Update currentUpdate;

    static {
        ChatSettingsHandler.initialiseSettingsFromFileToMemory();
        log.info("Bot successfully initialised :3 ");
    }

    public void onUpdateReceived(Update update) {

        final long currentDateTime = (new Date().getTime()) / 1000;

        // cut old updates to prevent commands overloading
        if(update.hasMessage()){
            if(currentDateTime > update.getMessage().getDate()){
                log.info("Update is older then now time. Ignoring update: " + update.getUpdateId());
                return;
            }
            else {
                log.info("Full update: " + update.toString());
                currentUpdate = update;
                // TODO: kill update variable in memory
            }
        }

        final String regex = "(.)*(\\d)(.)*"; // for check digits in answer
        final Pattern pattern = Pattern.compile(regex);

        final long chatId = currentUpdate.getMessage().getChatId();
        boolean isUpdateFromBot = false, isUpdateContainsReply = false, replyMessageInChatContainsBotName = false,
            messageInChatContainsBotName = false, isUpdateContainsPersonalPublicMessageToBot = false;
        boolean isUpdateHasMessage = false;
        Message replyMessage = null;

        if(currentUpdate.hasMessage()){
            isUpdateHasMessage = true;
        }

        // periodic task which check settings in memory and in settings file by update time
        if(!ChatSettingsHandler.checkMemSettingsAndFileIsSyncedByUpdateTime()){
            // initial comparing
            if(!ChatSettingsHandler.compareAllSettingsInMemoryAndInFile()){
                // all settings at first is stored in mem so mem settings in all cases will be newer then
                // settings in file. So we must copy all settings from memory into settings file
                ChatSettingsHandler.storeSettingsMapToSettingsFile(Main.userSettingsInMemory, true);
            }
        }

        try { // if it reply message save it into variable
            replyMessage = currentUpdate.getMessage().getReplyToMessage();
            if (replyMessage != null) {
                isUpdateContainsReply = true;
                log.info("Update contains reply message: " + replyMessage);
            }
        } catch (Exception e) {
            replyMessage = null;
        }
        try { // ignoring message if it from bot
            currentUpdate.getMessage().getFrom().getBot();
            if (isUpdateFromBot) {
                log.info("Update from bot. Ignoring.");
            }
        } catch (Exception e) {
            log.info("Exception while recognizing update from bot. Ignoring. " + e);
            isUpdateFromBot = false;
        }

        // LEFT MEMBERS update handling we must remove it from newbies list if user from there
        if(isUpdateHasMessage){
            if (currentUpdate.getMessage().getLeftChatMember() != null) {
                User leftChatMember = currentUpdate.getMessage().getLeftChatMember();
                if (!leftChatMember.getBot()) {
                    removeLeftMemberFromNewbieList(leftChatMember.getId());
                }
            }
        }

        // Periodic task to check users who doesn't say everything
        log.info("Current newbie lists size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
        checkAndRemoveAllSilentUsers(currentDateTime);

        // NEW MEMBERS update handling with attention message: -------------------------------->
        if (!Bot.currentUpdate.getMessage().getNewChatMembers().isEmpty()) {
            newMembersWarningMessageAndQuestionGeneration();
        }
        else {

            // MESSAGES HANDLING ------------------------------------------------------------>
            if (isUpdateHasMessage) {

                String messageText = currentUpdate.getMessage().getText();
                int messageId = currentUpdate.getMessage().getMessageId();

                // is it reply message contains bot name?
                if (replyMessage != null) {
                    try {
                        log.info("Reply message contains name: " + currentUpdate.getMessage().getReplyToMessage().getFrom().getUserName());
                        replyMessageInChatContainsBotName = currentUpdate.getMessage().getReplyToMessage().getFrom().getUserName().equals(getBotUsername());
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
                    if (currentUpdate.getMessage().getChat().isUserChat()) {
                        isUpdateContainsPersonalPublicMessageToBot = true;
                        log.info("Update is public message to bot in chat.");
                    }
                } catch (Exception e) {
                    isUpdateContainsPersonalPublicMessageToBot = false;
                }

                // ------------- CHECK MENTIONS IN CHAT MESSAGE OR IN REPLY

                if (messageInChatContainsBotName || replyMessageInChatContainsBotName || isUpdateContainsPersonalPublicMessageToBot) {
                    // user also can answer in personal public messages to bot

                    // COMMANDS HANDLING -------------------------------->
                    if (messageText != null && messageText.contains("/")) {
                        CommandsHandler.handleAllCommands(messageText, chatId, messageId, isUpdateContainsPersonalPublicMessageToBot, currentUpdate);
                    }

                    // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                    if (Main.newbieMapWithGeneratedAnswers.containsKey(currentUpdate.getMessage().getFrom().getId()) /* if user in newbie list */ && !isUpdateContainsPersonalPublicMessageToBot) {
                        validateNewbieAnswer(currentUpdate, messageText, pattern, chatId, messageId, currentDateTime);
                    }
                } else { // no mentions of bot or personal public messages to him

                    // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                    if (Main.newbieMapWithGeneratedAnswers.containsKey(currentUpdate.getMessage().getFrom().getId()) /* if user in newbie list */ && !isUpdateContainsPersonalPublicMessageToBot) {
                        validateNewbieAnswer(currentUpdate, messageText, pattern, chatId, messageId, currentDateTime);
                    }
                }

            } else { // If we get update without message

            }
        }
    }

    /* ----------------------------- MAIN METHODS ------------------------------------------------------------ */

    public void checkAndRemoveAllSilentUsers(long currentDateTime) {
        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>) Main.newbieMapWithGeneratedAnswers.entrySet()) {

            log.info("Iterating newbie lists.");

            Integer userIdFromMainClass = pair.getKey();
            Long joinTimeFromMainClass = Main.newbieMapWithJoinTime.get(userIdFromMainClass);
            Long chatIdFromMainClass = Main.newbieMapWithChatId.get(userIdFromMainClass);

            log.info("Current date time: " + currentDateTime + " || Join member datetime: " + joinTimeFromMainClass + " || Difference: " + (currentDateTime - joinTimeFromMainClass));

            if ((currentDateTime - joinTimeFromMainClass) > Long.valueOf(SettingsForBotGlobal.timePeriodForSilentUsersByDefault.value)) {

                log.info("Difference bigger then defined value! " + userIdFromMainClass + " will be kicked");
                kickChatMember(chatIdFromMainClass, userIdFromMainClass, currentDateTime, 3000000);

                Main.newbieMapWithGeneratedAnswers.remove(userIdFromMainClass);
                Main.newbieMapWithJoinTime.remove(userIdFromMainClass);
                Main.newbieMapWithChatId.remove(userIdFromMainClass);

                log.info("Silent user removed. Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
                String textToSay = getTemplateTextForCurrentLanguage(EnTexts.removedSilentUser.name(), chatIdFromMainClass);
                sendMessageToChatID(chatIdFromMainClass, textToSay);
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
        Integer generatedNewbieAnswerDigit = Main.newbieMapWithGeneratedAnswers.get(newbieId);
        Integer currentNewbieAnswer = 0;

        String answerText = "";

        if (messageText == null) { // it can be sticker or picture and it must be deleted immediately
            Main.newbieMapWithGeneratedAnswers.remove(newbieId);
            Main.newbieMapWithJoinTime.remove(newbieId);
            Main.newbieMapWithChatId.remove(newbieId);

            log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

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

                    Main.newbieMapWithGeneratedAnswers.remove(newbieId);
                    Main.newbieMapWithJoinTime.remove(newbieId);
                    Main.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                    answerText += getTemplateTextForCurrentLanguage(EnTexts.newbieCheckSuccess.name(), chatId);

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                } else { // if user gives us WRONG answer

                    Main.newbieMapWithGeneratedAnswers.remove(newbieId);
                    Main.newbieMapWithJoinTime.remove(newbieId);
                    Main.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

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

                Main.newbieMapWithGeneratedAnswers.remove(userId);
                Main.newbieMapWithJoinTime.remove(userId);
                Main.newbieMapWithChatId.remove(userId);

                log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

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
        sendMessageToChatID(chatId, messageText, null);
    }

    /*
    Send message and mention user by UID
     */
    public void sendMessageToChatID(long chatId, String messageText, User userToMention) {
        SendMessage message = new SendMessage();
        if (messageText == null || messageText.equals("")) {
            log.info("Send text for message is empty. Nothing to say.");
            return;
        }
        if(userToMention != null){ // turning on inline mentioning by user id if username is null
            messageText = "<a href=\"tg://user?id=" + userToMention.getId() + "\">" +
                    (userToMention.getFirstName() != null ? userToMention.getFirstName() : "") +
                    (userToMention.getLastName() != null ? " " + userToMention.getLastName() : "") +
                    "</a> " + messageText;
            message.setChatId(chatId).setText(messageText).enableHtml(true);
        }
        else { // send message in it not null
            message.setChatId(chatId).setText(messageText);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.info("Error while trying to send message in chatId " + chatId + " " + e.toString());
        }
    }

    public void sendMessageToChatID(long chatId, String messageText, boolean messageTextIsTemplateText) {
        String language = ChatSettingsHandler.getLanguageOptionToChat(chatId).toLowerCase();
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
        String language = ChatSettingsHandler.getLanguageOptionToChat(chatId).toLowerCase();
        if (language.contains("ru") && messageTextIsTemplateText) {
            sendReplyMessageToChatID(chatId, RuTexts.getValueForKey(messageText), replyToMessageId);
        } else if (language.contains("en") && messageTextIsTemplateText) {
            sendReplyMessageToChatID(chatId, EnTexts.getValueForKey(messageText), replyToMessageId);
        } else {
            sendReplyMessageToChatID(chatId, messageText, replyToMessageId);
        }
    }

    public void removeLeftMemberFromNewbieList(int leftUserId) {
        if (Main.newbieMapWithGeneratedAnswers.containsKey(leftUserId)) {
            log.info("Silent user: " + leftUserId + " left or was removed from group. It should be deleted from all lists.");
            Main.newbieMapWithGeneratedAnswers.remove(leftUserId);
            Main.newbieMapWithJoinTime.remove(leftUserId);
            Main.newbieMapWithChatId.remove(leftUserId);
            log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
        }
    }

    public void newMembersWarningMessageAndQuestionGeneration() {

        List<User> newUsersMembersList = Bot.currentUpdate.getMessage().getNewChatMembers();
        log.info("We have an update with a new chat members (" + newUsersMembersList.size() + ")");

        for (User user : newUsersMembersList) {
            if (!user.getBot()) { // Is added users is bot?
                log.info("User is not bot. Processing.");

                int userId = user.getId();
                int randomDigit = ThreadLocalRandom.current().nextInt(1, 1001);
                int randomDigit2 = ThreadLocalRandom.current().nextInt(1, 1001);
                int answerDigit = randomDigit + randomDigit2;

                Main.newbieMapWithGeneratedAnswers.put(userId, answerDigit);
                Main.newbieMapWithJoinTime.put(userId, new Date().getTime() / 1000);
                long chatId = Bot.currentUpdate.getMessage().getChatId();
                Main.newbieMapWithChatId.put(userId, chatId);

                log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                // Send warning greetings message with generated digits
                String warningMessage = getTemplateTextForCurrentLanguage(EnTexts.defaultGreetings.name(), chatId);
                String userName = user.getUserName();
                String chatLanguageOptionForChat = ChatSettingsHandler.getLanguageOptionToChat(chatId);
                if(userName == null){
                    sendMessageToChatID(chatId, warningMessage + " " +
                            NumberWordConverter.convert(randomDigit, chatLanguageOptionForChat, true)
                            + " + "
                            + NumberWordConverter.convert(randomDigit2, chatLanguageOptionForChat, true)
                            , user);
                }
                else {
                    userName = "@" + userName;
                    sendMessageToChatID(chatId, userName + warningMessage + " " +
                            NumberWordConverter.convert(randomDigit, chatLanguageOptionForChat, true)
                            + " + " +
                            NumberWordConverter.convert(randomDigit2, chatLanguageOptionForChat, true));
                }
            } else {
                log.info("User is bot! Ignoring.");
            }
        }
    }

    public String getTemplateTextForCurrentLanguage(String templateTextName, long chatId) {
        String language = ChatSettingsHandler.getLanguageOptionToChat(chatId).toLowerCase();
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
