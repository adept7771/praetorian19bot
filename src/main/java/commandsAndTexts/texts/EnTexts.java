package commandsAndTexts.texts;

public enum EnTexts {

    defaultGreetings("Hi! ATTENTION! Please answer by replying TO THIS message. All other messages will be deleted and you'll be banned. How much will be: "),
    spammerBanned("Spammer banned and spamm deleted! Praetorians at your service. Meow! "),
    adminCheckWrong("Sorry you are not admin for this chat. "),
    ;

    public String value;

    EnTexts(String value) {
        this.value = value;
    }
}
