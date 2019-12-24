public enum SettingsForBotGlobal {

    botType("false"), // false - test bot and true - production
    timePeriodForSilentUsersByDefault("1000"), // time period when newbie can be silent without removing 60 = minute
    timePeriodForSecondaryApproveUser("86400"), // interval after first approve. If user will be silent he will be kicked
    settingsFileName("/settings.txt"), // filename where bot stores user settings in text mode

    tokenForProduction("868108811:AAFEBVlByhnZhYs0heohMiN0bsqDM_nn6IM"), // token types
    tokenForTest("976799861:AAGUy86YD4RHa7qXtyC9ncFyVpEoAr0XZR0"), //

    languageByDefault("En"), // En, Ru

    nameForProduction("Praetorian19bot"), // names
    nameForTest("praetorian19testbot"), //
    ;

    public String value;

    SettingsForBotGlobal(String value) {
        this.value = value;
    }
}
