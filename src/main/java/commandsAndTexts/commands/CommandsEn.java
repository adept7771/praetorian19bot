package commandsAndTexts.commands;

public enum CommandsEn {

    help("Hi there! Look down to command list:"),
    start("The bot running 24/7 and already started for all users. Use /help command to get more help."),
    about("This bot created for auto moderate new group members for protect your chat from bots. " +
            "If newbie will appear in chat bot will " +
            "contact with him and ask few questions. If new member will answer it right - bot will give him send message rights." +
            " Also silent members who not wrote any messages in short period after joining to chat should be removed by bot." +
            " \n\n >>>>>>>>> ATTENTION: you must give to bot all admin rights"),
    defaultlanguageadm("Language for all users by default. Admin settings. Example usage: /defaultlanguageadmen /defaultlanguageadmru. Supported: En, Ru"),
    welcometext("Set greeting message to bot (max 600 symbols, using quotes - \" is prohibited). Example usage: /welcometext text_of_welcome "),
    //autocheckLanguageForAll("This option will make bot to recognize user's language from update if it no defined by user. But if it NULL will used GLOBAL language setting for user."),
    ;

    public String value;

    CommandsEn(String value) {
        this.value = value;
    }
}
