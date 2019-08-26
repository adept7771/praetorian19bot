package commands;

public enum Commands {

    help("Hi there! Look down to command list:"),
    start("The bot running 24/7 and already started for all users. Use /help command to het more help."),
    about("This bot created for auto moderate new group members for protect your chat from bots. " +
            "If newbie will appear in chat bot will " +
            "contact with him and ask few questions. If new member will answer it right - bot will give him send message rights." +
            " Also silent members who not wrote any messages in short period after joining to chat should be removed by bot." +
            " \n\n >>>>>>>>> ATTENTION: you must give to bot all admin rights"),
    ;

    public String value;

    Commands(String value) {
        this.value = value;
    }
}
