package commandsAndTexts.texts;

public enum TextsEn {


    spammerBanned("Spammer banned and spamm deleted! Praetorians at your service. Meow!"),
    adminCheckWrong("Sorry you are not admin for this chat."),
    ;

    public String value;

    TextsEn(String value) {
        this.value = value;
    }
}
