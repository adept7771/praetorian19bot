package commandsAndTexts.texts;

public enum RuTexts {

    defaultGreetings("Привет! ВНИМАНИЕ! Пожалуйста ответьте на это сообщение. Любые другие сваши сообщения будут расценены как спамм. Сколько будет: "),
    spammerBanned(" Спаммер изгнан и мусор вычищен! Преторианцы на вашей службе. Муррр! "),
    adminCheckWrong(" Извините, но вы не админ этого чата. "),
    removedSilentUser(" < пользователь-молчун был выкинут. Мурр! "),
    newbieAnswerNotEqualsToGeneratedOne(" Не правильно. Сожалеем, но вы дали не правильный ответ, вы будете изгнаны! "),
    newbieCheckSuccess(" Верно! Теперь вы можете писать в группу. Приятного общения. "),
    newbieAnswerContainsOnlyLetters("  Ошибка. Введенные данные содержат только буквы. Вы будете изгнаны! "),
    changeDefaultLanguage(" Язык успешно изменен "),
    ;

    public String value;

    RuTexts(String value) {
        this.value = value;
    }

    public static String getValueForKey(String key){
        for(RuTexts text : values()){
            if(text.toString().equals(key)){
                return text.value;
            }
        }
        return null;
    }
}
