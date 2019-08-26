import additional.Settings;
import commands.Commands;
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
        boolean isUpdateFromBot = false;
        boolean isUpdateContainsReply = false;
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
        } catch (Exception e) {
            isUpdateFromBot = false;
        }


        // LEAVE MEMBERS update handling we must remove it from newbies list if user from it

        if (update.getMessage().getLeftChatMember() != null) {
            User leftChatMember = update.getMessage().getLeftChatMember();
            if (!leftChatMember.getBot()) {
                int leftUserId = leftChatMember.getId();
                if (MainInit.newbieMapWithAnswer.containsKey(leftUserId)) {
                    System.out.println("Silent user: " + leftUserId + " left or was removed from group. It should be deleted from all lists.");
                    MainInit.newbieMapWithAnswer.remove(leftUserId);
                    MainInit.newbieMapWithJoinTime.remove(leftUserId);
                    MainInit.newbieMapWithChatId.remove(leftUserId);
                    System.out.println("Newbie list size: " + +MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());
                }
            }
        }

        // Periodic task to check users who doesn't say everything

        System.out.println("Current newbie lists size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>) MainInit.newbieMapWithAnswer.entrySet()) {

            System.out.println("Iterating newbie lists.");

            Integer userIdFromMain = pair.getKey();
            Long joinTimeFromMain = MainInit.newbieMapWithJoinTime.get(userIdFromMain);
            Long chatIdFromMain = MainInit.newbieMapWithChatId.get(userIdFromMain);

            System.out.println("Current date time: " + currentDateTime + " || Join member datetime: " + joinTimeFromMain + " || Difference: " + (currentDateTime - joinTimeFromMain));

            if ((currentDateTime - joinTimeFromMain) > Long.valueOf(SettingsBotGlobal.timePeriodForSilentUsersByDefault.toString())) {
                System.out.println("Difference bigger then defined value! " + userIdFromMain + " will be kicked");
                KickChatMember kickChatMember = new KickChatMember();
                kickChatMember.setChatId(chatIdFromMain)
                        .setUserId(userIdFromMain)
                        .setUntilDate(((int) currentDateTime) + 3000000);
                try {
                    execute(kickChatMember);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                MainInit.newbieMapWithAnswer.remove(userIdFromMain);
                MainInit.newbieMapWithJoinTime.remove(userIdFromMain);
                MainInit.newbieMapWithChatId.remove(userIdFromMain);

                System.out.println("Silent user removed. Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                SendMessage message = new SendMessage()
                        .setChatId(chatIdFromMain)
                        .setText("Silent user was removed after delay. Meow!");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

        // NEW MEMBERS update handling with attention message: -------------------------------->

        if (!update.getMessage().getNewChatMembers().isEmpty()) {
            List<User> newUsersMembersList = update.getMessage().getNewChatMembers();
            System.out.println("We have an update with a new chat members (" + newUsersMembersList.size() + ")");
            Integer messageId = update.getMessage().getMessageId();

            for (User user : newUsersMembersList) {
                if (!user.getBot()) { // Is added users is bot?
                    System.out.println("User is not bot. Processing.");

                    int userId = user.getId();
                    int randomDigit = (int) (Math.random() * 100);
                    int answerDigit = randomDigit + randomDigit;
                    String helloText = "Hi! ATTENTION! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned. You have 5 minutes. How much will be " + messageId + " + " + randomDigit;

                    MainInit.newbieMapWithAnswer.put(userId, answerDigit);
                    MainInit.newbieMapWithJoinTime.put(userId, new Date().getTime() / 1000);
                    MainInit.newbieMapWithChatId.put(userId, chatId);

                    System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                    SendMessage message = new SendMessage() // Create a message object object
                            .setChatId(update.getMessage().getChatId())
                            .setReplyToMessageId(messageId)
                            .setText(helloText);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("User is bot! Ignoring.");
                }
            }
        }

        // MESSAGES HANDLING ------------------------------------------------------------>

        if (update.hasMessage()) {

            String messageText = update.getMessage().getText();
            Integer messageId = update.getMessage().getMessageId();

            // is it reply message contains bot name?

            boolean replyMessageContainsBotName = false;

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

            boolean messageInChatContainsBotName = false;
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

            boolean isUpdateContainsDirectMessageToBot = false;
            try {
                if (update.getMessage().getChat().isUserChat()){
                    isUpdateContainsDirectMessageToBot = true;
                    System.out.println("Update is private message to bot.");
                };
            } catch (Exception e) {
                isUpdateContainsDirectMessageToBot = false;
            }
            // ------------- CHECK MENTIONS IN DIRECT MESSAGE OR IN REPLY

            if (messageInChatContainsBotName || replyMessageContainsBotName || isUpdateContainsDirectMessageToBot) {

                // COMMANDS HANDLING -------------------------------->

                if (messageText.contains("/")){
                    if (messageText.contains("/help")) { // Print all messages in ONE message
                        System.out.println("Message text contains /help - show commands list");
                        StringBuilder helpText = new StringBuilder();
                        for (Commands commands : Commands.values()) {
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
                    }
                    else {
                        System.out.println("Message text contains / - it's a command");
                        String helpText = "";

                        for (Commands commands : Commands.values()) {
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

                // Check if user send CODE to unblock and if user is in newbie block list ---------------------->
                else if (MainInit.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) && !isUpdateContainsDirectMessageToBot) {

                    System.out.println("User which posted message is NEWBIE. Check initialising.");

                    Integer newbieId = update.getMessage().getFrom().getId();
                    Integer generatedNewbieAnswerDigit = MainInit.newbieMapWithAnswer.get(newbieId);
                    Integer currentNewbieAnswer = 0;
                    try { // try to normalize string
                        String tmpNewbieAnswer = messageText.replaceAll("\\s", "");
                        tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([a-z])", "");
                        tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([A-Z])", "");
                        tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([а-я])", "");
                        tmpNewbieAnswer = tmpNewbieAnswer.replaceAll("([А-Я])", "");
                        currentNewbieAnswer = Integer.valueOf(tmpNewbieAnswer);
                    } catch (NumberFormatException e) {
                        currentNewbieAnswer = 0;
                    }
                    String answerText = "";

                    System.out.println("Normalized newbie answer is: " + currentNewbieAnswer);

                    Matcher matcher = pattern.matcher(messageText); // contain at least digits

                    if (matcher.matches()) {
                        System.out.println("Message to bot contains REGEX digital pattern");

                        if (currentNewbieAnswer.equals(generatedNewbieAnswerDigit)) {

                            MainInit.newbieMapWithAnswer.remove(newbieId);
                            MainInit.newbieMapWithJoinTime.remove(newbieId);
                            MainInit.newbieMapWithChatId.remove(newbieId);

                            System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                            answerText += "Right! Now you can send messages to group. Have a nice chatting.";

                            SendMessage message = new SendMessage()
                                    .setChatId(chatId)
                                    .setReplyToMessageId(messageId)
                                    .setText(answerText);
                            try {
                                execute(message);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }

                        } else {

                            MainInit.newbieMapWithAnswer.remove(newbieId);
                            MainInit.newbieMapWithJoinTime.remove(newbieId);
                            MainInit.newbieMapWithChatId.remove(newbieId);

                            System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                            answerText += "Wrong. Sorry entered data is not match with generated one. You will be banned!";

                            SendMessage message = new SendMessage()
                                    .setChatId(chatId)
                                    .setReplyToMessageId(messageId)
                                    .setText(answerText);
                            try {
                                execute(message);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }

                            // DELETE FIRST WRONG MESSAGE FROM USER

                            DeleteMessage deleteMessage = new DeleteMessage();
                            deleteMessage.setChatId(chatId).setMessageId(messageId);

                            try {
                                execute(deleteMessage);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            KickChatMember kickChatMember = new KickChatMember();
                            kickChatMember.setChatId(chatId)
                                    .setUserId(update.getMessage().getFrom().getId())
                                    .setUntilDate(((int) currentDateTime) + 3000000);
                            try {
                                execute(kickChatMember);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }

                            message = new SendMessage()
                                    .setChatId(chatId)
                                    .setText("Spammer banned and spamm deleted! Praetorians at your service. Meow!");
                            try {
                                execute(message);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("Message to bot NOT contains any digits. Ban and delete from newbie list.");

                        int userId = update.getMessage().getFrom().getId();

                        MainInit.newbieMapWithAnswer.remove(userId);
                        MainInit.newbieMapWithJoinTime.remove(userId);
                        MainInit.newbieMapWithChatId.remove(userId);

                        System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                        answerText += "Wrong. Sorry entered DATA contains only letters. You will be banned!";

                        KickChatMember kickChatMember = new KickChatMember();
                        kickChatMember.setChatId(chatId)
                                .setUserId(update.getMessage().getFrom().getId())
                                .setUntilDate(((int) currentDateTime) + 3000000);
                        try {
                            execute(kickChatMember);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                        SendMessage message = new SendMessage()
                                .setChatId(chatId)
                                .setReplyToMessageId(messageId)
                                .setText(answerText);
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                        // DELETE FIRST WRONG MESSAGE FROM USER

                        DeleteMessage deleteMessage = new DeleteMessage();
                        deleteMessage.setChatId(chatId).setMessageId(messageId);

                        try {
                            execute(deleteMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        message = new SendMessage()
                                .setChatId(chatId)
                                .setText("Spammer banned and spamm deleted! Praetorians at your service. Meow!");
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else { // If we have an update with message from user in newbie block list. Delete it and ban motherfucker.
                if (MainInit.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId()) && !isUpdateContainsDirectMessageToBot) {
                    System.out.println("Message from user is not posted for praetorian. It should be deleted and banned.");

                    int userId = update.getMessage().getFrom().getId();

                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(chatId).setMessageId(messageId);

                    MainInit.newbieMapWithAnswer.remove(userId);
                    MainInit.newbieMapWithJoinTime.remove(userId);
                    MainInit.newbieMapWithChatId.remove(userId);

                    System.out.println("Newbie list size: " + MainInit.newbieMapWithAnswer.size() + " " + MainInit.newbieMapWithJoinTime.size() + " " + MainInit.newbieMapWithChatId.size());

                    try {
                        execute(deleteMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    SendMessage message = new SendMessage()
                            .setChatId(chatId)
                            .setText("Spammer banned and spamm deleted! Praetorians at your service. Meow!");
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                    KickChatMember kickChatMember = new KickChatMember();
                    kickChatMember.setChatId(chatId)
                            .setUserId(update.getMessage().getFrom().getId())
                            .setUntilDate(((int) currentDateTime) + 3000000);
                    try {
                        execute(kickChatMember);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getBotUsername() {
        // Return bot username
        if(MainInit.mode){
            return SettingsBotGlobal.nameForProduction.value;
        }
        else {
            return SettingsBotGlobal.nameForTest.value;
        }
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        if(MainInit.mode){
            return SettingsBotGlobal.tokenForProduction.toString();
        }
        else {
            return SettingsBotGlobal.tokenForTest.toString();
        }
    }
}
