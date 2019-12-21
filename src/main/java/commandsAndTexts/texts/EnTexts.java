package commandsAndTexts.texts;

public enum EnTexts {

    defaultGreetings(" Hi! Antispam filter! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned. How much will be: "),
    spammerBanned(" Spamer banned and spam deleted! Praetorians at your service. Meow! "),
    adminCheckWrong(" Sorry you are not admin for this chat. "),
    removedSilentUser(" Silent user was removed after delay. Meow! "),
    newbieAnswerNotEqualsToGeneratedOne(" Wrong. Sorry entered data is not match with generated one. You will be banned! "),
    newbieCheckSuccess(" Right! Now you can send messages to group. Have a nice chatting. "),
    newbieAnswerContainsOnlyLetters("  Wrong. Sorry entered DATA contains only letters. You will be banned! "),
    changeDefaultLanguage(" Language successfully changed to "),
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
