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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot extends TelegramLongPollingBot {

    public void onUpdateReceived(Update update) {

        System.out.println("Full update: " + update.toString());

        String regex = "(.)*(\\d)(.)*"; // for check digits in answer
        long currentDate = (new Date().getTime()) / 1000;
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

        if (update.hasMessage()) {

            String messageText = update.getMessage().getText();
            Integer messageId = update.getMessage().getMessageId();

            // is it reply?

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

            // if it direct, check that message contains bot name

            boolean direcMessageContainsBotName = false;
            try {
                direcMessageContainsBotName = messageText.contains("@" + getBotUsername());
            } catch (NullPointerException e) {
                direcMessageContainsBotName = false;
            } finally {
                if (replyMessage == null) {
                    System.out.println("Direct message contains praetorian bot name: " + direcMessageContainsBotName);
                }
            }

            // ------------- CHECK MENTIONS IN DIRECT MESSAGE OR IN REPLY

            if (direcMessageContainsBotName || replyMessageContainsBotName) {

                // HELP commands handling -------------------------------->

                if (messageText.equals("/help")) { // Print all messages in ONE message
                    String helpText = "";
                    for (Commands commands : Commands.values()) {
                        helpText = helpText + "/" + commands.name() + " ---> " + commands.value + " \n\n";
                    }

                    SendMessage message = new SendMessage() // Create a message object object
                            .setChatId(chatId)
                            .setReplyToMessageId(messageId)
                            .setText(helpText);
                    try {
                        execute(message); // Sending our message object to user
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                    // Print special help text for defined command
                } else if (messageText.contains("/")) {

                    String helpText = "";

                    for (Commands commands : Commands.values()) {
                        if (messageText.contains(commands.name())) {
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

                // Check if user send CODE to unblock and if user is in newbie block list ---------------------->
                else if (Main.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId())) {

                    System.out.println("User which posted message is NEWBIE. Check initialising.");

                    Integer newbieId = update.getMessage().getFrom().getId();
                    Integer generatedNewbieAnswerDigit = Main.newbieMapWithAnswer.get(newbieId);
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

                    System.out.println("Normalized newbie anser is: " + currentNewbieAnswer);

                    Matcher matcher = pattern.matcher(messageText); // contain at least digits

                    if (matcher.matches()) {
                        System.out.println("Message to bot contains REGEX digital pattern");

                        if (currentNewbieAnswer.equals(generatedNewbieAnswerDigit)) {

                            Main.newbieMapWithAnswer.remove(newbieId);
                            Main.newbieMapWithJoinTime.remove(newbieId);
                            Main.newbieMapWithChatId.remove(newbieId);

                            System.out.println("Newbie list size: " + Main.newbieMapWithAnswer.size());

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

                            Main.newbieMapWithAnswer.remove(newbieId);
                            Main.newbieMapWithJoinTime.remove(newbieId);
                            Main.newbieMapWithChatId.remove(newbieId);

                            System.out.println("Newbie list size: " + Main.newbieMapWithAnswer.size());

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
                                    .setUntilDate(((int) currentDate) + 3000000);
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

                        Main.newbieMapWithAnswer.remove(userId);
                        Main.newbieMapWithJoinTime.remove(userId);
                        Main.newbieMapWithChatId.remove(userId);

                        System.out.println("Newbie list size: " + Main.newbieMapWithAnswer.size());

                        answerText += "Wrong. Sorry entered DATA contains only letters. You will be banned!";

                        KickChatMember kickChatMember = new KickChatMember();
                        kickChatMember.setChatId(chatId)
                                .setUserId(update.getMessage().getFrom().getId())
                                .setUntilDate(((int) currentDate) + 3000000);
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
                if (Main.newbieMapWithAnswer.containsKey(update.getMessage().getFrom().getId())) {
                    System.out.println("Message from user is not posted for praetorian. It should be deleted and banned.");

                    int userId = update.getMessage().getFrom().getId();

                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(chatId).setMessageId(messageId);

                    Main.newbieMapWithAnswer.remove(userId);
                    Main.newbieMapWithJoinTime.remove(userId);
                    Main.newbieMapWithChatId.remove(userId);

                    System.out.println("Newbie list size: " + Main.newbieMapWithAnswer.size());

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
                            .setUntilDate(((int) currentDate) + 3000000);
                    try {
                        execute(kickChatMember);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // NEW MEMBERS update handling with attention message:

        if (!update.getMessage().getNewChatMembers().isEmpty()) {
            List<User> newUsersMembersList = update.getMessage().getNewChatMembers();
            System.out.println("We have an update with a new chat members (" + newUsersMembersList.size() + ")");
            Integer messageId = update.getMessage().getMessageId();

            for (User user : newUsersMembersList) {
                if (!user.getBot()) { // Is added users is bot?
                    System.out.println("User is not bot. Processing.");

                    int userId = user.getId();
                    int randomDigit = (int) (Math.random() * 100);
                    int answerDigit = messageId + randomDigit;
                    String helloText = "Hi! ATTENTION! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned. You have 5 minutes. How much will be " + messageId + " + " + randomDigit;

                    Main.newbieMapWithAnswer.put(userId, answerDigit);
                    Main.newbieMapWithJoinTime.put(userId, new Date().getTime());
                    Main.newbieMapWithChatId.put(userId, chatId);

                    System.out.println("Newbie list size: " + Main.newbieMapWithAnswer.size());

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

        // Periodic task to check users who doesn't say everything

        for (Map.Entry<Integer, Integer> pair : (Iterable<Map.Entry<Integer, Integer>>) Main.newbieMapWithAnswer.entrySet()) {
            Integer userIdFromMain = pair.getKey();
            Long joinTimeFromMain = Main.newbieMapWithJoinTime.get(userIdFromMain);
            Long chatIdFromMain = Main.newbieMapWithChatId.get(userIdFromMain);

            if (currentDate - joinTimeFromMain > 300000) {
                KickChatMember kickChatMember = new KickChatMember();
                kickChatMember.setChatId(chatIdFromMain)
                        .setUserId(userIdFromMain)
                        .setUntilDate(((int) currentDate) + 3000000);
                try {
                    execute(kickChatMember);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                Main.newbieMapWithAnswer.remove(userIdFromMain);
                Main.newbieMapWithJoinTime.remove(userIdFromMain);
                Main.newbieMapWithChatId.remove(userIdFromMain);

                System.out.println("Silent user removed. Newbie list size: " + Main.newbieMapWithAnswer.size());

                SendMessage message = new SendMessage()
                        .setChatId(chatIdFromMain)
                        .setText("Silent user was removed after 5 minutes delay. Meow!");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return "Praetorian19bot";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return "868108811:AAFEBVlByhnZhYs0heohMiN0bsqDM_nn6IM";
    }
}
