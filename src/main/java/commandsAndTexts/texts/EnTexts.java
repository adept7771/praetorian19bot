package commandsAndTexts.texts;

public enum EnTexts {

    defaultGreetings(" Hi! Antispam filter! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned."),
    halfGreetings(" \n\n Answer to a question or you'll be banned. How much will be: "),
    spammerBanned(" Spamer banned and spam deleted! Praetorians at your service. Meow! "),
    adminCheckWrong(" Sorry you are not admin for this chat. "),
    removedSilentUser(" Silent user was removed after delay. Meow! "),
    newbieAnswerNotEqualsToGeneratedOne(" Wrong. Sorry entered data is not match with generated one. You will be banned! "),
    newbieCheckSuccess(" Right! Now you can send messages to group. But we will watch for your actions in chat. If we will detect bot actions you will be removed. Have a nice chatting. "),
    newbieAnswerContainsOnlyLetters("  Wrong. Sorry entered DATA contains only letters. You will be banned! "),
    changeDefaultLanguage(" Language successfully changed to "),
    optionSetSuccess(" Defined option was set successfully "),
    optionSetError(" Something going wrong while try to set option. Maybe you're not an admin or your command doesn't have a right format? "),
    kickedMemberJoinedInBanTime(" User was kicked short time ago. He must wait until ban time end. "),
    ;

    public String value;

    EnTexts(String value) {
        this.value = value;
    }

    public static String getValueForKey(String key){
        for(EnTexts text : values()){
            if(text.toString().equals(key)){
                return text.value;
            }
        }
        return null;
    }
}
