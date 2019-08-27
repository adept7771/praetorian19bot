public enum SettingsBotGlobal {

    botType("false"), // false - test bot and true - production
    timePeriodForSilentUsersByDefault("300"), // time period when newbie can be silent without removing 60 = minute
    settingsFileName("/settings.txt"), // filename where bot stores user settings in text mode

    tokenForProduction("868108811:AAFEBVlByhnZhYs0heohMiN0bsqDM_nn6IM"), // token types
    tokenForTest("976799861:AAGUy86YD4RHa7qXtyC9ncFyVpEoAr0XZR0"), //

    languageByDefault("English"),

    nameForProduction("Praetorian19bot"), // names
    nameForTest("praetorian19testbot"), //
    ;

    public String value;

    SettingsBotGlobal(String value) {
        this.value = value;
    }
}
