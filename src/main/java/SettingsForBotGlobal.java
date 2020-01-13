public enum SettingsForBotGlobal {

    botType("false"), // false - test bot and true - production
    approveFirstlyTime("1000"), // 70 for test 1000 for prod time period when newbie can be silent without removing 60 = minute
    approveSecondaryTime("400"), // 100 for test 400 for prod interval after first approve. If user will be silent he will be kicked
    userKickedTime("400"), // 100 for test 400 for prod interval which kicked user will be in kicked list. All users from kicked list
    // cant write any messages to chat and all this messages will be removed. Protection for situations when bot send 2 messages or more
    settingsFileName("/settings.txt"), // filename where bot stores user settings in text mode
    executableJarFileName("/praetorian19-1.0-jar-with-dependencies.jar"), // executable filename can be included in linux platforms in filepath
    // this can be produce errors

    tokenForProduction("868108811:AAFEBVlByhnZhYs0heohMiN0bsqDM_nn6IM"), // token types
    tokenForTest("976799861:AAGUy86YD4RHa7qXtyC9ncFyVpEoAr0XZR0"), //

    adminUserId("168171351"), //

    languageByDefault("En"), // En, Ru

    nameForProduction("Praetorian19bot"), // names
    nameForTest("praetorian19testbot"), //
    ;

    public String value;

    SettingsForBotGlobal(String value) {
        this.value = value;
    }
}
