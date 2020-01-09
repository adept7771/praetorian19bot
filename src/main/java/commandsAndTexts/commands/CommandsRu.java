package commandsAndTexts.commands;

public enum CommandsRu {

    help("Привет! Ознакомься со списком команд:"),
    start("Бот работает 24/7 и уже запущен для всех пользователей. Используй /help команду чтобы получить более подробную справку."),
    about("Этот бот создан для автомодерации чат групп и защиты вас от ботов. " +
                  "Если появляется новый пользователь бот с ним " +
                  "свяжется и спросит несколько вопросов. Если пользователь ответит правильно, он сможет писать сообщения без проблем." +
                  " Пользователи вступившие в группу, но не ответившие на вопрос бота и сохраняющие молчание, будут удалены спустя некоторое время автоматически." +
                  " \n\n >>>>>>>>> ВНИМАНИЕ: вы должны выдать боту полные админские права! Группа так же должна иметь ПУБЛИЧНЫЙ тип"),
    defaultlanguageadm("Язык бота для всех пользователей по умолчанию. Админская настройка. Пример: /defaultLanguageadmn /defaultlanguageadmru. Поддерживаемые языки: En, Ru"),
    welcometext("Установить приветственное сообщение для бота (максимум 600 символов, использование кавычек - \" запрещено). Пример: /welcometext текст_приветственного_сообщения"),
//    autocheckLanguageForAll("При этой настройке бот попытается угадать язык пользователя из апдейта, однако если это не получится, будет использован язык бота из настройки ЯЗЫК БОТА ПО УМОЛЧАНИЮ.")
    ;
    public String value;

    CommandsRu(String value) {
        this.value = value;
    }
}
