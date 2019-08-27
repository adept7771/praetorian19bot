import java.io.*;
import java.util.*;

class UserSettingsHandler {

    private static File settingsFile = new File(MainInit.absolutePath + SettingsBotGlobal.settingsFileName);
    public static long lastSettingsSavedTime;

    static { // initial check that settings file is exist

        System.out.println("Absolute path to working dir is: " + MainInit.absolutePath);

        System.out.println("Is settings file exists? " + settingsFile.exists());

        ArrayList<String> listOfParametersFromSettingsFile = parseSettingsFileIntoArrayList(settingsFile);

        if (!listOfParametersFromSettingsFile.isEmpty()) {

            System.out.println("List of parameters from settings file is not empty! Initialising in memory.");

            MainInit.userSettingsForBot = parseSettingsArrayListInSettingsMap(listOfParametersFromSettingsFile);

            System.out.println("Parsed Map with settings from file stored to memory successful");
            UserSettingsHandler.lastSettingsSavedTime = (new Date().getTime()) / 1000;

        } else { // if file with setting is empty

            System.out.println("List of parameters from settings file is empty! Nothing to initialising and save in memory.");
            UserSettingsHandler.lastSettingsSavedTime = (new Date().getTime()) / 1000;
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

        //System.out.println("test");
    }

    public static void storeSettingsFromMemoryToFile() {


    }

    public boolean compareChatSettingOptionValueInMem(long chatID, String optionNameToCompare, String optionValueToCompare){
        try{
            return MainInit.userSettingsForBot.get(chatID).get(optionNameToCompare).equals(optionValueToCompare);
        }
        catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    public static boolean compareAllSettingsInMemoryAndInFile() {

        System.out.println("Comparing settings in mem and in file");

        HashMap<Long, HashMap<String, String>> copyOfCurrentSettingsFileInMapView = parseSettingsFileInMap(settingsFile);

        Iterator<Map.Entry<Long, HashMap<String, String>>> iterator = copyOfCurrentSettingsFileInMapView.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Long, HashMap<String, String>> pair = iterator.next();
            Long chatIDFromFile = pair.getKey();
            HashMap<String, String> mapWithParametersFromFile = pair.getValue();

            try{
                HashMap<String, String> mapWithParametersFromMemory = MainInit.userSettingsForBot.get(chatIDFromFile);
                if(mapWithParametersFromMemory.equals(copyOfCurrentSettingsFileInMapView)){
                    System.out.println("SettingsBotGlobal for bots in memory is equals to settings in current file.");
                    return true;
                }
                else {
                    System.out.println("SettingsBotGlobal for bots in memory is NOT equals to settings in current file.");
                    return false;
                }
            }
            catch (Exception e){
                System.out.println("Error while comparing setting in bot file and in memory. Return defaul true.");
                return true;
            }
        }
        System.out.println("Error while comparing setting in bot file and in memory. Return defaul true.");
        return true;
    }

    static HashMap<Long, HashMap<String, String>> parseSettingsFileInMap(File file) {
        return parseSettingsArrayListInSettingsMap(parseSettingsFileIntoArrayList(file));
    }

    public static ArrayList<String> parseSettingsFileIntoArrayList(File file) {

        ArrayList<String> listOfParametersFromSettingsFile = new ArrayList<>();

        if (!file.exists()) {
            try { // create file if it not exists
                System.out.println("File with setting not found. Creating it now.");
                String data = " ";
                FileOutputStream out = new FileOutputStream(MainInit.absolutePath + SettingsBotGlobal.settingsFileName);
                out.write(data.getBytes());
                out.close();
            } catch (Exception e) {
                System.out.println("Error while trying to create file with settings");
            }
        } else {
            try {
                System.out.println("File with setting is found. Initialising it in MEMORY variable.");

                BufferedReader fileReader = new BufferedReader((new InputStreamReader(new FileInputStream(MainInit.absolutePath + SettingsBotGlobal.settingsFileName))));

                while (fileReader.ready()) {
                    listOfParametersFromSettingsFile.add(fileReader.readLine());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return listOfParametersFromSettingsFile;
    }

    static HashMap<Long, HashMap<String, String>> parseSettingsArrayListInSettingsMap(ArrayList<String> listOfParametersFromSettingsFile) {

        HashMap<Long, HashMap<String, String>> mapWithSettings = new HashMap<>();

        for (String lineWithAllParams : listOfParametersFromSettingsFile) {

            System.out.println("Current string with settings is: " + lineWithAllParams);

            int indexOfFirstComma = lineWithAllParams.indexOf(",");
            long idOfChat = Long.valueOf(lineWithAllParams.substring(0, indexOfFirstComma));

            mapWithSettings.put(idOfChat, new HashMap<>());

            for (int i = indexOfFirstComma; i < lineWithAllParams.length(); i++) {
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
            System.out.println("Parsed all params for ChatID into map: " + idOfChat);
        }
        return mapWithSettings;
    }
}