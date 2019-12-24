package commandsAndTexts.texts;

public enum RuTexts {

    defaultGreetings(" Привет! Антиспам защита. Пожалуйста, ответьте на это сообщение. Любые другие ваши сообщения будут расценены как спам. Ответ принимается в форме числа."),
    halfGreetings(" \n\n Сколько будет: "),
    spammerBanned(" Спамер изгнан и мусор вычищен! Преторианцы на вашей службе. Муррр! "),
    adminCheckWrong(" Извините, но вы не админ этого чата. "),
    removedSilentUser(" Пользователь-молчун был выкинут. Мурр! "),
    newbieAnswerNotEqualsToGeneratedOne(" Не правильно. Сожалеем, но вы дали не правильный ответ, вы будете изгнаны! "),
    newbieCheckSuccess(" Верно! Теперь вы можете писать в группу. Приятного общения. "),
    newbieAnswerContainsOnlyLetters("  Ошибка. Введенные данные содержат только буквы. Вы будете изгнаны! "),
    changeDefaultLanguage(" Язык успешно изменен на "),
    optionSetSuccess(" Указанная опция успешно установлена "),
    optionSetError(" Упс! Что-то пошло не так во время установки опции. Возможно вы не админ? Или не выполнили условия команды? "),
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
