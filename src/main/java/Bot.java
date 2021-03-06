import commandsAndTexts.commands.CommandsEn;
import commandsAndTexts.texts.EnTexts;
import commandsAndTexts.texts.RuTexts;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot extends TelegramLongPollingBot {

    public static final Logger log = Logger.getLogger(Bot.class);
    public static Update currentUpdate;

    static {
        ChatSettingsHandler.initialiseSettingsFromSettingsFileToMemory();
        log.info("Bot successfully initialised :3 ");
    }

    public void onUpdateReceived(Update update) {

        final long currentDateTime = (new Date().getTime()) / 1000;
        final String regex = "(.)*(\\d)(.)*"; // for check digits in answer
        final Pattern pattern = Pattern.compile(regex); // for check digits in answer
        long chatId = 0;
        boolean isUpdateFromBot = false, isUpdateContainsReply = false, replyMessageInChatContainsBotName = false,
                messageInChatContainsBotName = false, isUpdateContainsPersonalPublicMessageToBot = false;
        boolean isUpdateHasMessage = false;
        Message replyMessage = null;

        // kill old updates to prevent commands overloading
        if ((currentDateTime - update.getMessage().getDate()) > 90) {
            log.info("Update is not actual and older then 90 seconds from now time. Ignoring update: "
                    + update.getUpdateId());
            update = null;
            currentUpdate = null;
            System.gc();
            return;
        } else {
            log.info("Update is actual. Processing. Full update: " + update.toString());
            currentUpdate = update;
            update = null;
            System.gc();
        }

        if (currentUpdate.hasMessage()) {
            isUpdateHasMessage = true;
        }
        if (isUpdateHasMessage) {
            chatId = currentUpdate.getMessage().getChatId();
        }

        // LEFT MEMBERS update handling we must remove it from newbies list if user from there
        if (isUpdateHasMessage) {
            if (currentUpdate.getMessage().getLeftChatMember() != null) {
                User leftChatMember = currentUpdate.getMessage().getLeftChatMember();
                if (!leftChatMember.getBot()) {
                    removeLeftMemberFromAllNewbieLists(leftChatMember.getId());
                    return;
                }
            }
        }

        // periodic task which compare settings in memory and in settings file by update time
        if (!ChatSettingsHandler.checkMemSettingsAndFileIsSyncedByUpdateTime()) {
            ChatSettingsHandler.storeSettingsMapToSettingsFile(Main.userSettingsInMemory,
                    true);
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


        // First and second Periodic tasks to check silent users by timeout
        if (Main.newbieMapWithChatId.size() > 0) {
            log.info("Current newbie first check lists size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                    + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
            firstCheckAndRemoveSilentUsers(currentDateTime);
            if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                log.info("Newbie second check list size for current chat ID: " + chatId + " is: "
                        + Main.newbieToSecondaryApprove.get(chatId).size());
            }
            secondaryCheckValidateAndRemoveSilentUsers(currentDateTime);
            // second periodic task to clean silent users
        } else {
            log.info("Current newbie first check lists is empty.");
            if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                log.info("Newbie second check list size for current chat ID: " + chatId + " is: "
                        + Main.newbieToSecondaryApprove.get(chatId).size());
            }
            secondaryCheckValidateAndRemoveSilentUsers(currentDateTime);
            // second periodic task to clean silent users
        }

        // NEW MEMBERS update handling with attention message: -------------------------------->
        if (Bot.currentUpdate.getMessage().getNewChatMembers().size() > 0) {
            newMembersWarningMessageAndQuestionGeneration();
        } else {

            // MESSAGES HANDLING ------------------------------------------------------------>
            if (isUpdateHasMessage) {

                String messageText = currentUpdate.getMessage().getText();
                int messageId = currentUpdate.getMessage().getMessageId();

                // if user was already kicked check it, if timeinterval less then defined - delete all message from him
                int fromUserId = 0;
                try {
                    fromUserId = currentUpdate.getMessage().getFrom().getId();
                } catch (Exception e) {
                    log.info("Error while trying extract userId in message! " + e);
                }

                if (Main.kickedUsers.containsKey(chatId)) {
                    if (Main.kickedUsers.get(chatId).containsKey(fromUserId) && fromUserId != 0 &&
                            Main.kickedUsers.get(chatId).get(fromUserId)
                                    < Long.parseLong(SettingsForBotGlobal.userKickedTime.value)) {
                        log.info("User was kicked from group, and try to send messages. Delete this message! User: " + fromUserId);
                        deleteMessage(chatId, messageId);
                    }
                }

                // is reply message contains bot name?
                if (replyMessage != null) {
                    try {
                        log.info("Reply message contains name: "
                                + currentUpdate.getMessage().getReplyToMessage().getFrom().getUserName());
                        replyMessageInChatContainsBotName = currentUpdate.getMessage().getReplyToMessage().getFrom()
                                .getUserName().equals(getBotUsername());
                    } catch (Exception e) {
                        replyMessageInChatContainsBotName = false;
                    } finally {
                        log.info("Is reply message name contains bot name status: " + replyMessageInChatContainsBotName);
                    }
                }

                // if it is a direct message in chat, check that message contains bot name
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
                        CommandsHandler.handleAllCommands(messageText, chatId, messageId,
                                isUpdateContainsPersonalPublicMessageToBot, currentUpdate);
                        return;
                    }

                    // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                    if (Main.newbieMapWithGeneratedAnswers.containsKey(currentUpdate.getMessage().getFrom().getId())
                            /* if user in newbie list */ && !isUpdateContainsPersonalPublicMessageToBot) {
                        validateNewbieAnswer(currentUpdate, messageText, pattern, chatId,
                                messageId, currentDateTime);
                        return;
                    }
                } else { // no mentions of bot or personal public messages to him

                    // Check if user send CODE to unblock IN CHAT and if user is in newbie block list ---------------------->
                    if (Main.newbieMapWithGeneratedAnswers.containsKey(currentUpdate.getMessage().getFrom().getId())
                            /* if user in newbie list */ && !isUpdateContainsPersonalPublicMessageToBot) {
                        validateNewbieAnswer(currentUpdate, messageText, pattern, chatId,
                                messageId, currentDateTime);
                    }
                }

            } else { // If we get update without message

            }
        }
    }

    /* ----------------------------- MAIN METHODS ------------------------------------------------------------ */

    public void firstCheckAndRemoveSilentUsers(long currentDateTime) {

        if (Main.newbieMapWithChatId.size() == 0) {
            return;
        }

        log.info("Iterating first newbie lists.");

        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>)
                Main.newbieMapWithGeneratedAnswers.entrySet()) {

            Integer userIdFromMainClass = pair.getKey();
            Long joinTimeFromMainClass = Main.newbieMapWithJoinTime.get(userIdFromMainClass);
            Long chatIdFromMainClass = Main.newbieMapWithChatId.get(userIdFromMainClass);

            log.info("Current date time: " + currentDateTime + " || Join member datetime: " + joinTimeFromMainClass
                    + " || Difference: " + (currentDateTime - joinTimeFromMainClass));

            if ((currentDateTime - joinTimeFromMainClass) > Long.valueOf(SettingsForBotGlobal.approveFirstlyTime.value)) {

                log.info("Difference bigger then defined value! " + userIdFromMainClass + " will be kicked from chat");

                Main.newbieMapWithGeneratedAnswers.remove(userIdFromMainClass);
                Main.newbieMapWithJoinTime.remove(userIdFromMainClass);
                Main.newbieMapWithChatId.remove(userIdFromMainClass);

                kickChatMemberAndOrganizeKickedList(chatIdFromMainClass, userIdFromMainClass,
                        currentDateTime, 3000000);

                log.info("Silent user removed. First newbie list size: " + Main.newbieMapWithGeneratedAnswers.size()
                        + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                if (Main.newbieToSecondaryApprove.containsKey(chatIdFromMainClass)) {
                    if (Main.newbieToSecondaryApprove.get(chatIdFromMainClass).containsKey(userIdFromMainClass)) {
                        log.info("Silent user was removed from first list, but exists in second, removing him from secondary list");
                        Main.newbieToSecondaryApprove.get(chatIdFromMainClass).remove(userIdFromMainClass);
                        if (Main.newbieToSecondaryApprove.get(chatIdFromMainClass).size() == 0) {
                            log.warn("List of secondary check for silent users is empty, deleting it. Chat id: " + chatIdFromMainClass);
                            Main.newbieToSecondaryApprove.remove(chatIdFromMainClass);
                        }
                    }
                }

                String textToSay = getTemplateTextForCurrentLanguage(EnTexts.removedSilentUser.name(),
                        chatIdFromMainClass);
                sendMessageToChatID(chatIdFromMainClass, textToSay);
            }
        }
    }

    public void secondaryCheckValidateAndRemoveSilentUsers(long currentDateTime) {

        final int userIdFromUpdate = currentUpdate.getMessage().getFrom().getId();

        if (Main.newbieToSecondaryApprove.size() == 0) {
            log.warn("Secondary check silent users list are not exists, full secondary check cancelled");
            return;
        }

        if (Main.newbieMapWithJoinTime.containsKey(userIdFromUpdate)) {
            log.warn("User " + userIdFromUpdate + " is author of message and still in first silent list, cancel full " +
                    "secondary silent list check");
            return;
        }

        log.warn("Secondary check silent users list are exists, check initiating");
        Iterator<Map.Entry<Long, HashMap<Integer, Long>>> firstIterator = Main.newbieToSecondaryApprove.entrySet().iterator();

        while (firstIterator.hasNext()) {
            Map.Entry<Long, HashMap<Integer, Long>> pair = firstIterator.next();
            long chatId = pair.getKey();
            HashMap<Integer, Long> mapWithUsersAndTimestamps = pair.getValue();

            if (mapWithUsersAndTimestamps.size() != 0) {
                Iterator<Map.Entry<Integer, Long>> secondIterator = mapWithUsersAndTimestamps.entrySet().iterator();

                while (secondIterator.hasNext()) {
                    Map.Entry<Integer, Long> pair2 = secondIterator.next();
                    int userId = pair2.getKey();
                    long userTimestamp = pair2.getValue();
                    log.warn("User is in secondary check list. Chat id: " + chatId + " userId: " + userId);

                    // if iterated user is author of message and in list and time enough to secondary validate, approve him and delete from list
                    // also he must be not in list from firstly removed users

                    if (userIdFromUpdate == userId && (currentDateTime - userTimestamp) <
                            Long.valueOf(SettingsForBotGlobal.approveSecondaryTime.value)) {

                        log.warn("User sent message in enough time interval, removing from secondary check list. Chat id: "
                                + chatId + " userId: " + userId);

                        Main.newbieToSecondaryApprove.get(chatId).remove(userId);
                        log.warn("Secondary silent users list size for chat: " + chatId + " is "
                                + Main.newbieToSecondaryApprove.get(chatId).size());

                        if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                            log.warn("List of secondary check for silent users is empty, deleting it. Chat id: "
                                    + chatId);
                            Main.newbieToSecondaryApprove.remove(chatId);
                        }
                        return;
                    }

                    // if iterated user is not author of incoming message and is in list of secondary check. Also if interval to answer is exceed.
                    if ((currentDateTime - userTimestamp) >
                            Long.valueOf(SettingsForBotGlobal.approveSecondaryTime.value)) {

                        final String textToSay = getTemplateTextForCurrentLanguage
                                (EnTexts.removedSilentUser.name(), chatId);
                        sendMessageToChatID(chatId, textToSay);

                        log.warn("User: " + userId + " was removed after long delay from chat and secondary check list for silent users");
                        log.warn("Secondary silent users list size for chat: " + chatId + " is "
                                + Main.newbieToSecondaryApprove.get(chatId).size());

                        Main.newbieToSecondaryApprove.get(chatId).remove(userId);
                        if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                            log.warn("List of secondary check for silent users is empty, deleting it. Chat id: " + chatId);
                            Main.newbieToSecondaryApprove.remove(chatId);
                        }

                        kickChatMemberAndOrganizeKickedList(chatId, userId, currentDateTime,
                                3000000);
                    } else {
                        log.warn("User: " + userId + " from secondary check list is silent less then default value. " +
                                "Nothing to do yet. Timestamp difference is: " + (currentDateTime - userTimestamp));
                    }
                }
            } else {
                log.warn("Secondary check list for chat is empty. Nothing to do.");
            }
        }
    }

    public void kickChatMemberAndOrganizeKickedList(long chatId, int userId, long currentDateTime, int untilDateInSeconds) {
        KickChatMember kickChatMember = new KickChatMember();
        kickChatMember.setChatId(chatId)
                .setUserId(userId)
                .setUntilDate(((int) currentDateTime) + untilDateInSeconds);

        if (isUserInChat(chatId, userId)) {
            try {

                execute(kickChatMember);
                log.info("User " + userId + " in chat id " + chatId + " was kicked");

                if (Main.kickedUsers.containsKey(chatId)) {
                    Main.kickedUsers.get(chatId).put(userId, currentDateTime);
                } else {
                    HashMap<Integer, Long> kickedUserMap = new HashMap() {{
                        put(userId, currentDateTime);
                    }};
                    Main.kickedUsers.put(chatId, kickedUserMap);
                    log.info("Kicked users map for chat is empty. Removing map. Chat id / User id: " + chatId + " " + userId);
                }

                if (Main.kickedUsers.containsKey(chatId)) {
                    if (Main.kickedUsers.get(chatId).size() == 0) {
                        Main.kickedUsers.remove(chatId);
                    }
                    log.info("Kicked users map for chat id is empty. Removing map. Chat id: " + chatId);
                }

                if (Main.kickedUsers.containsKey(chatId)) {
                    if (Main.kickedUsers.get(chatId).containsKey(userId)) {
                        long userKickedTime = Main.kickedUsers.get(chatId).get(userId);
                        if ((currentDateTime - userKickedTime) > Long.valueOf(SettingsForBotGlobal.userKickedTime.value)) {
                            Main.kickedUsers.get(chatId).remove(userId);
                            log.info("User ID " + userId + " was removed from kicked users list of chat " + chatId);
                        }
                    }
                }

            } catch (TelegramApiException e) {
                log.info("Error while try to kick user " + userId + " in chat id " + chatId + " " + e.toString());
            }
        } else {
            log.info("Can't kick user " + userId + " in chat id " + chatId + " because he is not in this chat!");
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

    public boolean isUserInChat(long chatId, int userId) {
        GetChatMember getChatMember = new GetChatMember();

        getChatMember.setChatId(chatId);
        getChatMember.setUserId(userId);

        ChatMember chatMember = new ChatMember();
        try {
            log.info("Try to check is userId " + userId + " is in chat: " + chatId);
            chatMember = execute(getChatMember);
        } catch (Exception e) {
            log.info("Error while trying to recognize userId " + userId + "  in chat: " + chatId + " return false by default");
            return false;
        }
        if (chatMember.getStatus().equals("left") && chatMember.getStatus().equals("kicked")) {
            log.info("userId " + userId + " is not in chat: " + chatId);
            return false;
        } else {
            log.info("userId " + userId + " is in chat: " + chatId);
            return true;
        }
    }

    public int normalizeUserAnswer(String stringToNormalize) { // method need if user send some text with digit answer
        try { // try to normalize string
            String tmpNewbieAnswer = stringToNormalize.replaceAll("\\s", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([a-z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([A-Z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([??-??])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([??-??])", "");
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

            log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                    + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

            if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                if (Main.newbieToSecondaryApprove.get(chatId).containsKey(newbieId)) {
                    Main.newbieToSecondaryApprove.get(chatId).remove(newbieId);
                }
                log.info("Secondary approve list size for chat: " + chatId + " is " +
                        Main.newbieToSecondaryApprove.get(chatId).size());
                if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                    log.info("Secondary approve list size for chat: " + chatId + " is " +
                            Main.newbieToSecondaryApprove.get(chatId).size() + " removing map from memory");
                    Main.newbieToSecondaryApprove.remove(chatId);
                }
            }

            answerText += getTemplateTextForCurrentLanguage
                    (EnTexts.newbieAnswerNotEqualsToGeneratedOne.name(), chatId);
            sendReplyMessageToChatID(chatId, answerText, messageId);

            // DELETE FIRST WRONG MESSAGE FROM USER
            deleteMessage(chatId, messageId);
            kickChatMemberAndOrganizeKickedList(chatId, update.getMessage().getFrom().getId(),
                    currentDateTime, 3000000);
            sendMessageToChatID(chatId, getTemplateTextForCurrentLanguage
                    (EnTexts.spammerBanned.name(), chatId));

        } else { // if message contains any text data - validate and remove from first silent list

            currentNewbieAnswer = normalizeUserAnswer(messageText);
            log.info("Normalized newbie answer is: " + currentNewbieAnswer);
            Matcher matcher = pattern.matcher(messageText); // contain at least digits

            if (matcher.matches()) { // if contains at least digits
                log.info("Message to bot contains at least digits");

                if (currentNewbieAnswer.equals(generatedNewbieAnswerDigit)) { // if user gives us right answer

                    // delete user from first check list but stay it in second

                    Main.newbieMapWithGeneratedAnswers.remove(newbieId);
                    Main.newbieMapWithJoinTime.remove(newbieId);
                    Main.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                            + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                    answerText += getTemplateTextForCurrentLanguage(EnTexts.newbieCheckSuccess.name(),
                            chatId);

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                } else { // if user gives us WRONG answer - kick user and delete from both lists

                    Main.newbieMapWithGeneratedAnswers.remove(newbieId);
                    Main.newbieMapWithJoinTime.remove(newbieId);
                    Main.newbieMapWithChatId.remove(newbieId);

                    log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                            + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                    if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                        if (Main.newbieToSecondaryApprove.get(chatId).containsKey(newbieId)) {
                            Main.newbieToSecondaryApprove.get(chatId).remove(newbieId);
                        }
                        log.info("Secondary approve list size for chat: " + chatId + " is "
                                + Main.newbieToSecondaryApprove.get(chatId).size());
                        if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                            log.info("Secondary approve list size for chat: " + chatId + " is "
                                    + Main.newbieToSecondaryApprove.get(chatId).size() + " removing map from memory");
                            Main.newbieToSecondaryApprove.remove(chatId);
                        }
                    }

                    answerText += getTemplateTextForCurrentLanguage(
                            EnTexts.newbieAnswerNotEqualsToGeneratedOne.name(), chatId);

                    sendReplyMessageToChatID(chatId, answerText, messageId);

                    // DELETE FIRST WRONG MESSAGE FROM USER
                    deleteMessage(chatId, messageId);
                    kickChatMemberAndOrganizeKickedList(chatId, update.getMessage().getFrom().getId(),
                            currentDateTime, 3000000);
                    String bannedTextMessage = getTemplateTextForCurrentLanguage(EnTexts.spammerBanned.name(),
                            chatId);

                    sendMessageToChatID(chatId, bannedTextMessage);
                }
            } else { // if message from newbie but not contains digit answer  - kick user and delete from both lists
                log.info("Message to bot NOT contains any digits. Ban and delete from newbie list.");

                int userId = update.getMessage().getFrom().getId();

                Main.newbieMapWithGeneratedAnswers.remove(userId);
                Main.newbieMapWithJoinTime.remove(userId);
                Main.newbieMapWithChatId.remove(userId);

                log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                        + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                    if (Main.newbieToSecondaryApprove.get(chatId).containsKey(newbieId)) {
                        Main.newbieToSecondaryApprove.get(chatId).remove(newbieId);
                    }
                    log.info("Secondary approve list size for chat: " + chatId + " is "
                            + Main.newbieToSecondaryApprove.get(chatId).size());
                    if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                        log.info("Secondary approve list size for chat: " + chatId + " is "
                                + Main.newbieToSecondaryApprove.get(chatId).size() + " removing map from memory");
                        Main.newbieToSecondaryApprove.remove(chatId);
                    }
                }

                kickChatMemberAndOrganizeKickedList(chatId, update.getMessage().getFrom().getId(),
                        currentDateTime, 3000000);
                sendReplyMessageToChatID(chatId, EnTexts.newbieAnswerContainsOnlyLetters.name(),
                        messageId, true);

                // DELETE FIRST WRONG MESSAGE FROM USER
                String bannedTextMessage = getTemplateTextForCurrentLanguage(EnTexts.spammerBanned.name(),
                        chatId);
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
        sendMessageToChatID(chatId,
                messageText, null);
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
        if (userToMention != null) { // turning on inline mentioning by user id if username is null
            messageText = "<a href=\"tg://user?id=" + userToMention.getId() + "\">" +
                    (userToMention.getFirstName() != null ? userToMention.getFirstName() : "") +
                    (userToMention.getLastName() != null ? " " + userToMention.getLastName() : "") +
                    "</a> " + messageText;
            message.setChatId(chatId).setText(messageText).enableHtml(true);
        } else { // send message in it not null
            message.setChatId(chatId).setText(messageText);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.info("Error while trying to send message in chatId " + chatId + " " + e.toString());
        }
    }

    public void sendMessageToChatID(long chatId, String messageText, boolean messageTextIsTemplateText) {
        String language = ChatSettingsHandler.getLanguageOptionForChat(chatId).toLowerCase();
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
            log.info("Error while trying to answer in chatId " + chatId + " to message " + replyToMessageId
                    + " " + e.toString());
        }
    }

    public void sendReplyMessageToChatID(long chatId, String messageText, int replyToMessageId,
                                         boolean messageTextIsTemplateText) {
        String language = ChatSettingsHandler.getLanguageOptionForChat(chatId).toLowerCase();
        if (language.contains("ru") && messageTextIsTemplateText) {
            sendReplyMessageToChatID(chatId, RuTexts.getValueForKey(messageText),
                    replyToMessageId);
        } else if (language.contains("en") && messageTextIsTemplateText) {
            sendReplyMessageToChatID(chatId, EnTexts.getValueForKey(messageText),
                    replyToMessageId);
        } else {
            sendReplyMessageToChatID(chatId, messageText, replyToMessageId);
        }
    }

    public void removeLeftMemberFromAllNewbieLists(int leftUserId) {
        if (Main.newbieMapWithGeneratedAnswers.containsKey(leftUserId)) {
            Main.newbieMapWithGeneratedAnswers.remove(leftUserId);
            Main.newbieMapWithJoinTime.remove(leftUserId);
            Main.newbieMapWithChatId.remove(leftUserId);
            log.info("Silent user: " + leftUserId + " left or was removed from group. " +
                    "It should be deleted from all lists. Removing from first list.");
            log.info("First newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                    + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
        }
        long chatId = currentUpdate.getMessage().getChatId();
        if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
            if (Main.newbieToSecondaryApprove.get(currentUpdate.getMessage().getChatId()).containsKey(leftUserId)) {
                log.info("Removing user: " + leftUserId + " from second list");
                Main.newbieToSecondaryApprove.get(chatId).remove(leftUserId);
                if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                    log.info("Secondary approve list size for chat: " + chatId + " is " +
                            Main.newbieToSecondaryApprove.get(chatId).size() + " removing map from memory");
                    Main.newbieToSecondaryApprove.remove(chatId);
                }
                log.info("Secondary newbie list size: " + Main.newbieToSecondaryApprove.size());
            }
        }
    }

    public void newMembersWarningMessageAndQuestionGeneration() {
        List<User> newUsersMembersList = Bot.currentUpdate.getMessage().getNewChatMembers();
        log.info("We have an update with a new chat members (" + newUsersMembersList.size() + ")");

        for (User user : newUsersMembersList) {
            if (!user.getBot()) { // Is added users is bot?
                log.info("User is not bot. Processing.");

                int userId = user.getId();
                long chatId = Bot.currentUpdate.getMessage().getChatId();

                if (Main.kickedUsers.containsKey(chatId)) {
                    if (Main.kickedUsers.get(chatId).containsKey(userId) && userId != 0 &&
                            Main.kickedUsers.get(chatId).get(userId)
                                    < Long.parseLong(SettingsForBotGlobal.userKickedTime.value)) {

                        log.info("User was kicked from group, and try to join again. Kick him! User: " + userId);
                        final long currentDateTime = (new Date().getTime()) / 1000;
                        kickChatMemberAndOrganizeKickedList(chatId,
                                currentUpdate.getMessage().getFrom().getId(), currentDateTime,
                                3000000);

                        Main.newbieMapWithGeneratedAnswers.remove(userId);
                        Main.newbieMapWithJoinTime.remove(userId);
                        Main.newbieMapWithChatId.remove(userId);

                        log.info("Newbie list size: " + Main.newbieMapWithGeneratedAnswers.size() + " "
                                + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());

                        if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                            if (Main.newbieToSecondaryApprove.get(chatId).containsKey(userId)) {
                                Main.newbieToSecondaryApprove.get(chatId).remove(userId);
                            }
                            log.info("Secondary approve list size for chat: " + chatId + " is "
                                    + Main.newbieToSecondaryApprove.get(chatId).size());
                            if (Main.newbieToSecondaryApprove.get(chatId).size() == 0) {
                                log.info("Secondary approve list size for chat: " + chatId + " is "
                                        + Main.newbieToSecondaryApprove.get(chatId).size() + " removing map from memory");
                                Main.newbieToSecondaryApprove.remove(chatId);
                            }
                        }

                        String answerText = getTemplateTextForCurrentLanguage(EnTexts.kickedMemberJoinedInBanTime.name(), chatId);
                        sendReplyMessageToChatID(chatId, answerText, currentUpdate.getMessage().getMessageId());

                        continue;
                    }
                }

                int randomDigit = ThreadLocalRandom.current().nextInt(1, 1001);
                int randomDigit2 = ThreadLocalRandom.current().nextInt(1, 1001);
                int answerDigit = randomDigit + randomDigit2;

                // put user to first check silent map
                Main.newbieMapWithGeneratedAnswers.put(userId, answerDigit);
                Main.newbieMapWithJoinTime.put(userId, new Date().getTime() / 1000);
                Main.newbieMapWithChatId.put(userId, chatId);

                // put user in secondary check silent map

                if (Main.newbieToSecondaryApprove.containsKey(chatId)) {
                    Main.newbieToSecondaryApprove.get(chatId).put(userId, new Date().getTime() / 1000);

                } else { // if map for silent users not exists create it
                    Main.newbieToSecondaryApprove.put(chatId, new HashMap<Integer, Long>() {{
                        put(userId, new Date().getTime() / 1000);
                    }});
                }

                log.info("Newbie first lists size: " + Main.newbieMapWithGeneratedAnswers.size()
                        + " " + Main.newbieMapWithJoinTime.size() + " " + Main.newbieMapWithChatId.size());
                log.info("Secondary user approve list size for chat " + chatId + " is "
                        + Main.newbieToSecondaryApprove.get(chatId).size());

                // Send warning greetings message with generated digits
                String warningMessage = "";

                // if greeting message defined by user get it from memory
                String setupOptionFromMemory = ChatSettingsHandler
                        .getSetupOptionValueFromMemory(CommandsEn.welcometext.name(), chatId);
                if (setupOptionFromMemory != null) {
                    warningMessage = setupOptionFromMemory;
                    warningMessage += getTemplateTextForCurrentLanguage(EnTexts.halfGreetings.name(),
                            chatId);
                } else { // if greetings message not defined get default greetings
                    warningMessage = getTemplateTextForCurrentLanguage(EnTexts.defaultGreetings.name(),
                            chatId);
                }

                String userName = user.getUserName();
                final String chatLanguageOptionForChat = ChatSettingsHandler.getLanguageOptionForChat(chatId);

                if (userName == null) {
                    sendMessageToChatID(chatId, warningMessage + " " +
                                    NumberWordConverter.convert(randomDigit, chatLanguageOptionForChat,
                                            true)
                                    + " + "
                                    + NumberWordConverter.convert(randomDigit2, chatLanguageOptionForChat,
                            true)
                            , user);
                } else {
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
        String language = ChatSettingsHandler.getLanguageOptionForChat(chatId).toLowerCase();
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
