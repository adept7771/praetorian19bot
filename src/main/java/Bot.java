import commands.CommandsEn;
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

    public void onUpdateReceived(Update update) {

        System.out.println("Full update: " + update.toString());

        String regex = "(.)*(\\d)(.)*"; // for check digits in answer
        long currentDateTime = (new Date().getTime()) / 1000;
        Pattern pattern = Pattern.compile(regex);
        long chatId = update.getMessage().getChatId();
        boolean isUpdateFromBot = false, isUpdateContainsReply = false, replyMessageContainsBotName = false, messageInChatContainsBotName = false, isUpdateContainsDirectMessageToBot = false;
        Message replyMessage = null;

        try {
            replyMessage = update.getMessage().getReplyToMessage();
            if (replyMessage != null) {
                isUpdateContainsReply = true;
                System.out.println("Update contains reply message: " + replyMessage);
            }
        } catch (Exception e) {
            replyMessage = null;
        }
        try {
            update.getMessage().getFrom().getBot();
            if (isUpdateFromBot) {
                System.out.println("Update from bot. Ignoring.");
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
        System.out.println("Current newbie lists size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
        checkAndRemoveAllSilentUsers(currentDateTime);

        // NEW MEMBERS update handling with attention message: -------------------------------->
        newMembersWarningMessageAndQuestionGeneration
                ("Hi! ATTENTION! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned. You have " + SettingsBotGlobal.timePeriodForSilentUsersByDefault.toString() + " seconds. How much will be ", update);

        // MESSAGES HANDLING ------------------------------------------------------------>
        if (update.hasMessage()) {

            String messageText = update.getMessage().getText();
            Integer messageId = update.getMessage().getMessageId();

            // is it reply message contains bot name?
            if (replyMessage != null) {
                try {
                    System.out.println("Reply message contains name: " + update.getMessage().getReplyToMessage().getFrom().getUserName());
                    replyMessageContainsBotName = update.getMessage().getReplyToMessage().getFrom().getUserName().equals(getBotUsername());
                } catch (Exception e) {
                    replyMessageContainsBotName = false;
                } finally {
                    System.out.println("Is reply message name contains bot name status: " + replyMessageContainsBotName);
                }
            }

            // if it direct message in chat, check that message contains bot name
            try {
                messageInChatContainsBotName = messageText.contains("@" + getBotUsername());
            } catch (NullPointerException e) {
                messageInChatContainsBotName = false;
            } finally {
                if (replyMessage == null) {
                    System.out.println("Chat message contains praetorian bot name: " + messageInChatContainsBotName);
                }
            }

            // if it personal message in personal direct chat
            try {
                if (update.getMessage().getChat().isUserChat()) {
                    isUpdateContainsDirectMessageToBot = true;
                    System.out.println("Update is private message to bot.");
                }
            } catch (Exception e) {
                isUpdateContainsDirectMessageToBot = false;
            }

            // ------------- CHECK MENTIONS IN DIRECT MESSAGE OR IN REPLY
            if (messageInChatContainsBotName || replyMessageContainsBotName || isUpdateContainsDirectMessageToBot) {

                // COMMANDS HANDLING -------------------------------->
                if (messageText != null && messageText.contains("/")) {
                    handleAllCommands(messageText, chatId, messageId);
                }

                // Check if user send CODE to unblock and if user is in newbie block list ---------------------->
                else if (MainInit.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) /* if user in newbie list */) {

                    validateNewbieAnswer(update, messageText, pattern, chatId, messageId, currentDateTime);

                } else { // if message from newbie but not contains digit answer
                    System.out.println("Message to bot NOT contains any digits. Ban and delete from newbie list.");

                    int userId = update.getMessage().getFrom().getId();

                    MainInit.newbieMapWithAnswer.remove(userId);
                    MainInit.newbieMapWithJoinTime.remove(userId);
                    MainInit.newbieMapWithChatId.remove(userId);

                    System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                    kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
                    sendReplyMessageToChatID(chatId,
                            "Wrong. Sorry entered DATA contains only letters. You will be banned!", messageId);

                    // DELETE FIRST WRONG MESSAGE FROM USER
                    deleteMessageAndSayText(chatId, messageId,
                            "Spammer banned and spamm deleted! Praetorians at your service. Meow!");
                }
            }
        } else { // If we have an update with message from user in newbie block list. Delete it and ban motherfucker.
            if (MainInit.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) && !isUpdateContainsDirectMessageToBot) {

                System.out.println("Message from user is not posted for praetorian. But it can contains answer for question. Checking.");

                int userId = update.getMessage().getFrom().getId();

                deleteMessageAndSayText(chatId, update.getMessage().getMessageId(), "Spammer banned and spamm deleted! Praetorians at your service. Meow!");

                MainInit.newbieMapWithAnswer.remove(userId);
                MainInit.newbieMapWithJoinTime.remove(userId);
                MainInit.newbieMapWithChatId.remove(userId);

                System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
            }
        }
    }

    /* ----------------------------- MAIN METHODS ------------------------------------------------------------ */

    private void checkAndRemoveAllSilentUsers(long currentDateTime) {
        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>) MainInit.newbieMapWithAnswer.entrySet()) {

            System.out.println("Iterating newbie lists.");

            Integer userIdFromMainClass = pair.getKey();
            Long joinTimeFromMainClass = MainInit.newbieMapWithJoinTime.get(userIdFromMainClass);
            Long chatIdFromMainClass = MainInit.newbieMapWithChatId.get(userIdFromMainClass);

            System.out.println("Current date time: " + currentDateTime + " || Join member datetime: " + joinTimeFromMainClass + " || Difference: " + (currentDateTime - joinTimeFromMainClass));

            if ((currentDateTime - joinTimeFromMainClass) > Long.valueOf(SettingsBotGlobal.timePeriodForSilentUsersByDefault.value)) {

                System.out.println("Difference bigger then defined value! " + userIdFromMainClass + " will be kicked");
                kickChatMember(chatIdFromMainClass, userIdFromMainClass, currentDateTime, 3000000);

                MainInit.newbieMapWithAnswer.remove(userIdFromMainClass);
                MainInit.newbieMapWithJoinTime.remove(userIdFromMainClass);
                MainInit.newbieMapWithChatId.remove(userIdFromMainClass);

                System.out.println("Silent user removed. Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                sendMessageToChatID(chatIdFromMainClass, "Silent user was removed after delay. Meow!");
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

    private int normalizeUserAnswer(String stringToNormalize){
        try { // try to normalize string
            String tmpNewbieAnswer = stringToNormalize.replaceAll("\\s", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([a-z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([A-Z])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([а-я])", "");
            tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([А-Я])", "");
            return Integer.valueOf(tmpNewbieAnswer);
        } catch (NumberFormatException e) {
            return  0;
        }
    }

    private void validateNewbieAnswer(Update update, String messageText, Pattern pattern, long chatId,
                                      int messageId, long currentDateTime) {

        System.out.println("User which posted message is NEWBIE. Check initialising.");

        Integer newbieId = update.getMessage().getFrom().getId();
        Integer generatedNewbieAnswerDigit = MainInit.newbieMapWithAnswer.get(newbieId);
        Integer currentNewbieAnswer = 0;

        currentNewbieAnswer = normalizeUserAnswer(messageText);

        String answerText = "";

        System.out.println("Normalized newbie answer is: " + currentNewbieAnswer);

        Matcher matcher = pattern.matcher(messageText); // contain at least digits

        if (matcher.matches()) { // if contains at least digits
            System.out.println("Message to bot contains REGEX digital pattern");

            if (currentNewbieAnswer.equals(generatedNewbieAnswerDigit)) { // if user gives us right answer

                MainInit.newbieMapWithAnswer.remove(newbieId);
                MainInit.newbieMapWithJoinTime.remove(newbieId);
                MainInit.newbieMapWithChatId.remove(newbieId);

                System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                answerText += "Right! Now you can send messages to group. Have a nice chatting.";

                sendReplyMessageToChatID(chatId, answerText, messageId);

            } else { // if user gives us WRONG answer

                MainInit.newbieMapWithAnswer.remove(newbieId);
                MainInit.newbieMapWithJoinTime.remove(newbieId);
                MainInit.newbieMapWithChatId.remove(newbieId);

                System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                answerText += "Wrong. Sorry entered data is not match with generated one. You will be banned!";

                sendReplyMessageToChatID(chatId, answerText, messageId);

                // DELETE FIRST WRONG MESSAGE FROM USER
                deleteMessage(chatId, messageId);
                kickChatMember(chatId, update.getMessage().getFrom().getId(), currentDateTime, 3000000);
                sendMessageToChatID(chatId, "Spammer banned and spamm deleted! Praetorians at your service. Meow!");
            }
        }
    }

    private void handleAllCommands(String messageText, long chatId, Integer messageId) {
        if (messageText.contains("/help")) { // Print all messages in ONE message
            System.out.println("Message text contains /help - show commands list");
            StringBuilder helpText = new StringBuilder();
            for (CommandsEn commands : CommandsEn.values()) {
                helpText.append("/").append(commands.name()).append(" ---> ").append(commands.value).append(" \n\n");
            }

            SendMessage message = new SendMessage() // Create a message object object
                    .setChatId(chatId)
                    .setReplyToMessageId(messageId)
                    .setText(helpText.toString());
            try {
                execute(message); // Sending our message object to user
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Message text contains / - it's a command");
            String helpText = "";

            for (CommandsEn commands : CommandsEn.values()) {
                if (messageText.contains(commands.name())) {
                    System.out.println("Message test contains - command name: " + commands.name());
                    helpText = commands.value;
                }
            }

            SendMessage message = new SendMessage()
                    .setChatId(chatId)
                    .setReplyToMessageId(messageId)
                    .setText(helpText);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
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
            System.out.println("Silent user: " + leftUserId + " left or was removed from group. It should be deleted from all lists.");
            MainInit.newbieMapWithAnswer.remove(leftUserId);
            MainInit.newbieMapWithJoinTime.remove(leftUserId);
            MainInit.newbieMapWithChatId.remove(leftUserId);
            System.out.println("Newbie list size: " + +MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
        }
    }

    public void newMembersWarningMessageAndQuestionGeneration(String warningMessage, Update update) {

        if (!update.getMessage().getNewChatMembers().isEmpty()) {
            List<User> newUsersMembersList = update.getMessage().getNewChatMembers();
            System.out.println("We have an update with a new chat members (" + newUsersMembersList.size() + ")");
            Integer messageId = update.getMessage().getMessageId();

            for (User user : newUsersMembersList) {
                if (!user.getBot()) { // Is added users is bot?
                    System.out.println("User is not bot. Processing.");

                    int userId = user.getId();
                    int randomDigit = (int) (Math.random() * 100);
                    int randomDigit2 = (int) (Math.random() * 100);
                    int answerDigit = randomDigit + randomDigit2;
                    String helloText = warningMessage + " " + randomDigit + " + " + randomDigit2;

                    MainInit.newbieMapWithAnswer.put(userId, answerDigit);
                    MainInit.newbieMapWithJoinTime.put(userId, new Date().getTime() / 1000);
                    long chatId = update.getMessage().getChatId();
                    MainInit.newbieMapWithChatId.put(userId, chatId);

                    System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
                    sendMessageToChatID(chatId, helloText);

                } else {
                    System.out.println("User is bot! Ignoring.");
                }
            }
        }
    }

    public String getBotUsername() {
        // Return bot username
        if (SettingsBotGlobal.botType.value.equals("true")) {
            return SettingsBotGlobal.nameForProduction.value;
        } else {
            return SettingsBotGlobal.nameForTest.value;
        }
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        if (SettingsBotGlobal.botType.value.equals("true")) {
            return SettingsBotGlobal.tokenForProduction.value;
        } else {
            return SettingsBotGlobal.tokenForTest.value;
        }
    }
}
