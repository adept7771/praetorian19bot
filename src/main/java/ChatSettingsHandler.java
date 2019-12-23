import commandsAndTexts.commands.CommandsEn;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

class ChatSettingsHandler {

    private static final Logger log = Logger.getLogger(ChatSettingsHandler.class);
    private static File settingsFile = new File(Main.absolutePath + SettingsForBotGlobal.settingsFileName.value);
    public static long lastSettingsFileUpdateTime;

    /* ------------------------------------ Initialising ------------------------------------ */
    // initialising settings from settings file to memory of bot

    public static void initialiseSettingsFromFileToMemory() {

        log.info("Initialising settings from settings file to memory of bot ");
        log.info("Absolute path to working dir is: " + Main.absolutePath);
        log.info("Is settings file exists? " + settingsFile.exists());

        ArrayList<String> listOfAllChatParametersFromSettingsFile = parseSettingsFileIntoArrayList(settingsFile);
        long updateTime = (new Date().getTime()) / 1000;

        // try to store settings file option by option into memory
        if (!listOfAllChatParametersFromSettingsFile.isEmpty() && !listOfAllChatParametersFromSettingsFile.get(0).equals(" ")) {
            // if setting file not empty we must parse in into map

            log.info("List of parameters from settings file is not empty! Initialising in memory if it contains any options.");

            Main.userSettingsInMemory = parseSettingsArrayListInSettingsMap(listOfAllChatParametersFromSettingsFile);

            Main.lastMemorySettingsUpdateTime = updateTime;
            ChatSettingsHandler.lastSettingsFileUpdateTime = updateTime;

            log.info("Parsed Map with settings from file stored to memory successful");
            log.info("Memory settings update time: " + Main.lastMemorySettingsUpdateTime);
        } else { // if file with setting is empty do nothing
            log.info("List of parameters from settings file is empty! Nothing to initialising and save in memory.");
            Main.lastMemorySettingsUpdateTime = updateTime;
            ChatSettingsHandler.lastSettingsFileUpdateTime = updateTime;
            log.info("Memory update time: " + Main.lastMemorySettingsUpdateTime);
        }

//        //Создаем поток-чтения-байт-из-файла
//        FileInputStream inputStream = new FileInputStream("c:/data.txt");
//        // Создаем поток-записи-байт-в-файл
//        FileOutputStream outputStream = new FileOutputStream("c:/result.txt");
//
//        while (inputStream.available() > 0) //пока есть еще непрочитанные байты
//        {
//            int data = inputStream.read(); // прочитать очередной байт в переменную data
//            outputStream.write(data); // и записать его во второй поток
//        }
//
//        inputStream.close(); //закрываем оба потока. Они больше не нужны.
//        outputStream.close();

        //log.info("test");
    }

    /* ------------------------------------ MEMORY HANDLING ------------------------------------ */

    static public void setSetupOptionValueInMemory(String setupOptionName, String setupOptionValue, long chatId) {
        HashMap<String, String> optionsToStore = new HashMap<String, String>();
        optionsToStore.put(setupOptionName, setupOptionValue);
        if (getSetupOptionValueFromMemory(setupOptionName, chatId) == null) {
            Main.userSettingsInMemory.put(chatId, optionsToStore);
        } else {
            Main.userSettingsInMemory.get(chatId).put(setupOptionName, setupOptionValue);
        }
        log.info("Setup option " + setupOptionName + " value: " + setupOptionValue + " for chat: " + chatId + " was set into memory");

        Main.lastMemorySettingsUpdateTime = (new Date().getTime()) / 1000;
        log.info("Memory update time: " + Main.lastMemorySettingsUpdateTime);
    }

    static public String getSetupOptionValueFromMemory(String setupOption, long chatId) {
        log.info("Recognizing setup option in memory " + setupOption + " for chat id: " + chatId);
        try {
            String optionValue = Main.userSettingsInMemory.get(chatId).get(setupOption.toLowerCase());
            log.info("Option value is: " + optionValue);
            return optionValue;
        } catch (Exception e) {
            log.info("Error while trying to get option from memory (it could be not initialised) " + setupOption + " for chat: " + chatId + " " + e.toString());
            return null;
        }
    }

    static String getLanguageOptionToChat(long chatId) {
        log.info("Try to get language option value for chat id: " + chatId);
        String languageOption = getSetupOptionValueFromMemory(CommandsEn.defaultlanguageadm.toString(), chatId);
        if (languageOption == null) {
            return SettingsForBotGlobal.languageByDefault.value;
        } else {
            return languageOption;
        }
    }

    public boolean compareChatSettingOptionValueInMem(long chatID, String optionNameToCompare, String optionValueToCompare) {
        try {
            return Main.userSettingsInMemory.get(chatID).get(optionNameToCompare).equals(optionValueToCompare);
        } catch (Exception e) {
            log.info(e.toString());
            return false;
        }
    }

    /* -------------------------------- SETTINGS FILE HANDLING ------------------------------------ */

    public static boolean checkMemSettingsAndFileIsSyncedByUpdateTime(){
        log.info("Last memory setting upd time: " + Main.lastMemorySettingsUpdateTime + " Last settings file upd time: " + ChatSettingsHandler.lastSettingsFileUpdateTime);
        return Main.lastMemorySettingsUpdateTime == ChatSettingsHandler.lastSettingsFileUpdateTime;
    }

    public static boolean compareAllSettingsInMemoryAndInFile() {

        log.info("Comparing settings in memory and in file");

        HashMap<Long, HashMap<String, String>> copyOfCurrentSettingsFileInMapView = parseSettingsFileInMap(settingsFile);

        if(copyOfCurrentSettingsFileInMapView.size() == 0 && Main.userSettingsInMemory.size() == 0){
            log.info("Settings in memory and in file is empty both.");
            return true;
        }
        if(copyOfCurrentSettingsFileInMapView.size() == 0 && Main.userSettingsInMemory.size() > 0) {
            log.info("Settings in memory not empty, but settings in file is empty.");
            return false;
        }
        if(copyOfCurrentSettingsFileInMapView.size() > 0 && Main.userSettingsInMemory.size() > 0) {
            log.info("Settings in memory not empty, settings in file is not empty. Start comparing");

            Iterator<Map.Entry<Long, HashMap<String, String>>> iterator = copyOfCurrentSettingsFileInMapView.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, HashMap<String, String>> pair = iterator.next();
                Long chatIDFromFile = pair.getKey();
                HashMap<String, String> mapWithParametersFromFile = pair.getValue();

                try {
                    HashMap<String, String> mapWithParametersFromMemory = Main.userSettingsInMemory.get(chatIDFromFile);
                    if (mapWithParametersFromMemory.equals(copyOfCurrentSettingsFileInMapView)) {
                        log.info("SettingsForBotGlobal for bots in memory is equals to settings in current file.");
                        return true;
                    } else {
                        log.info("SettingsForBotGlobal for bots in memory is NOT equals to settings in current file.");
                        return false;
                    }
                } catch (Exception e) {
                    log.info("Error while comparing setting in bot file and in memory. Return default true.");
                    return true;
                }
            }
            log.info("Error while comparing setting in bot file and in memory. Return default true.");
            return true;
        }
        log.info("Something going wrong while comparing memory settings and file setting. Return true by default.");
        return true;
    }

    public static void storeSettingsMapToSettingsFile(HashMap<Long, HashMap<String, String>> mapWithIncomingSettingsToStore, boolean rewriteOptionsIfExists) {
        log.info("Trying to store settings into settings file. RewriteOption if option setting exists is: " + rewriteOptionsIfExists);
        HashMap<Long, HashMap<String, String>> mapOfFinalSettings = parseSettingsFileInMap(new File(Main.absolutePath + SettingsForBotGlobal.settingsFileName.value));

        if (!mapOfFinalSettings.equals(mapWithIncomingSettingsToStore)) {
            Iterator<Map.Entry<Long, HashMap<String, String>>> iterator = mapWithIncomingSettingsToStore.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Long, HashMap<String, String>> pair = iterator.next();
                Long chatIDmapWithSettingsToStore = pair.getKey();
                HashMap<String, String> mapFromMapWithSettingsToStore = pair.getValue();

                if (mapOfFinalSettings.containsKey(chatIDmapWithSettingsToStore)) { // if we have map with options for chat id
                    HashMap<String, String> mapWithOptionsFromSettingFile = mapOfFinalSettings.get(chatIDmapWithSettingsToStore);

                    Iterator<Map.Entry<String, String>> iterator2 = mapWithOptionsFromSettingFile.entrySet().iterator();
                    while (iterator2.hasNext()) {
                        Map.Entry<String, String> pair2 = iterator2.next();
                        String optionName = pair2.getKey();
                        String optionValue = pair2.getValue();

                        if (mapWithIncomingSettingsToStore.get(chatIDmapWithSettingsToStore).containsKey(optionName)) {
                            if (rewriteOptionsIfExists) { // rewrite option if it exists
                                mapWithIncomingSettingsToStore.get(chatIDmapWithSettingsToStore).put(optionName, optionValue);
                                log.info("Option name " + optionName + " is exists. Rewriting.");
                            } else {
                                log.info("Option name " + optionName + " is exists. Nothing to do.");
                            }
                        } else { // if option didn't found write it
                            mapWithIncomingSettingsToStore.get(chatIDmapWithSettingsToStore).put(optionName, optionValue);
                            log.info("Option name " + optionName + " is not exists in settings file. Writing it.");
                        }
                    }
                } else { // if we don't have map with options for chat id - write it
                    mapOfFinalSettings.put(chatIDmapWithSettingsToStore, mapFromMapWithSettingsToStore);
                    log.info("Options for chatID " + chatIDmapWithSettingsToStore + " is absent. Writing it.");
                }
            }
            // write new map with settings to settings file, delete old settings file
            File oldSettingsFileToDelete = new File(Main.absolutePath + SettingsForBotGlobal.settingsFileName.value);

            if (oldSettingsFileToDelete.delete()) {

                log.info(oldSettingsFileToDelete.getName() + " is deleted! Try to create new one with new settings.");
                writeMapWithSettingsToSettingsFile(mapOfFinalSettings);
            } else {
                log.info("Delete operation for old settings file is failed. Something going wrong while trying to save setting to settings file.");
            }
        } else {
            log.info("Map with settings to store is equals to current settings in settings file. Nothing to store.");
        }
    }

    public static void writeMapWithSettingsToSettingsFile(HashMap<Long, HashMap<String, String>> mapWithSettingsToStore){
        try {
            FileOutputStream out = new FileOutputStream(Main.absolutePath + SettingsForBotGlobal.settingsFileName.value);
            ArrayList<String> listOfDataToWrite = convertMapWithSettingsToList(mapWithSettingsToStore);
            StringBuilder stringDataToWrite = new StringBuilder();
            for(String currentString : listOfDataToWrite){
                stringDataToWrite.append(currentString);
            }

            out.write(stringDataToWrite.toString().getBytes());
            out.close();

            log.info("Map with settings successfully wrote to settings file.");
            ChatSettingsHandler.lastSettingsFileUpdateTime = (new Date().getTime()) / 1000;

        } catch (IOException e) {
            log.info("Map with settings is NOT successfully wrote to settings file.");
            log.info(e);
        }
    }

    static ArrayList<String> convertMapWithSettingsToList(HashMap<Long, HashMap<String, String>> mapWithSettings){
        ArrayList<String> listOfDataToWrite = new ArrayList<>();

        Iterator<Map.Entry<Long, HashMap<String, String>>> iterator = mapWithSettings.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, HashMap<String, String>> pair = iterator.next();
            StringBuilder stringToAdd = new StringBuilder();

            Long chatID = pair.getKey();
            stringToAdd.append(chatID).append(",");

            HashMap<String, String> currentMapFromMapWithSettings = pair.getValue();

            Iterator<Map.Entry<String, String>> iterator2 = currentMapFromMapWithSettings.entrySet().iterator();

            while (iterator2.hasNext()){
                Map.Entry<String, String> pair2 = iterator2.next();
                String optionName = pair2.getKey();
                String optionvalue = pair2.getValue();
                stringToAdd.append(optionName).append("=").append(optionvalue).append(",");
            }
            listOfDataToWrite.add(stringToAdd.toString() + "\n");
        }

        return listOfDataToWrite;
    }

    static HashMap<Long, HashMap<String, String>> parseSettingsFileInMap(File file) {
        return parseSettingsArrayListInSettingsMap(parseSettingsFileIntoArrayList(file));
    }

    public static ArrayList<String> parseSettingsFileIntoArrayList(File file) {

        ArrayList<String> listOfParametersFromSettingsFile = new ArrayList<>();

        if (!file.exists()) {
            try { // create file if it not exists
                log.info("File with setting not found. Creating it now.");
                String data = " ";
                FileOutputStream out = new FileOutputStream(Main.absolutePath + SettingsForBotGlobal.settingsFileName.value);
                out.write(data.getBytes());
                out.close();
            } catch (Exception e) {
                log.info("Error while trying to create file with settings");
            }
        } else {
            try {
                log.info("File with setting is found. Initialising it in MEMORY variable.");

                BufferedReader fileReader = new BufferedReader((new InputStreamReader(new FileInputStream(Main.absolutePath + SettingsForBotGlobal.settingsFileName.value))));

                while (fileReader.ready()) {
                    listOfParametersFromSettingsFile.add(fileReader.readLine());
                }
                fileReader.close();
            } catch (IOException e) {
                log.info("Error while trying to parse Settings File Into Array List " + e);
            }
        }
        return listOfParametersFromSettingsFile;
    }

    static HashMap<Long, HashMap<String, String>> parseSettingsArrayListInSettingsMap(ArrayList<String> listOfParametersFromSettingsFile) {

        if(listOfParametersFromSettingsFile.size() == 0 || listOfParametersFromSettingsFile.get(0).equals(" ")){
            return new HashMap<Long, HashMap<String, String>>();
        }
        else {
            HashMap<Long, HashMap<String, String>> mapWithSettings = new HashMap<>();

            for (String lineWithAllParams : listOfParametersFromSettingsFile) {

                log.info("Current string with settings is: " + lineWithAllParams);

                int indexOfFirstComma = lineWithAllParams.indexOf(",");
                long idOfChat = Long.valueOf(lineWithAllParams.substring(0, indexOfFirstComma));

                mapWithSettings.put(idOfChat, new HashMap<>());

                for (int i = indexOfFirstComma; i < lineWithAllParams.length()-1; i++) {
                    if (String.valueOf(lineWithAllParams.charAt(i)).equals(",")) {
                        int nextIndexOfComma = lineWithAllParams.indexOf(",", i + 1);
                        if (nextIndexOfComma == -1) {
                            nextIndexOfComma = lineWithAllParams.length();
                        }
                        String dataToWrite = lineWithAllParams.substring(i + 1, nextIndexOfComma);
                        mapWithSettings.get(idOfChat).put(
                                dataToWrite.substring(0, dataToWrite.indexOf("=")),
                                dataToWrite.substring(dataToWrite.indexOf("=") + 1)
                        );
                    }
                }
                log.info("Parsed all params for ChatID into map: " + idOfChat);
            }
            return mapWithSettings;
        }
    }
}
