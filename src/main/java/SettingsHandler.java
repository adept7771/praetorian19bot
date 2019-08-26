import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

class SettingsHandler {

    static { // initial check that settings file is exist

        System.out.println("Absolute path to working dir is: " + MainInit.absolutePath);

        File file = new File(MainInit.absolutePath + "/settings.txt");

        System.out.println("Is settings file exists? " + file.exists());

        ArrayList<String> listOfParametersFromSettingsFile = parseSettingsFileIntoArrayList(file);

        if(!listOfParametersFromSettingsFile.isEmpty()){

            System.out.println("List of parameters from settings file is not empty! Initialising in memory.");

            MainInit.globalSettingForBot = parseSettingsArrayListInSettingsMap(listOfParametersFromSettingsFile);

            System.out.println("Parsed Map stored to memory successful");
        }
        else { // if file with setting is empty
            System.out.println("List of parameters from settings file is empty! Nothing to initialising in memory.");
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

    public static void syncSettingsInMemAndSettingFileIfDifferent(){



    }

    public static boolean checkSettingsInMemAndInFile(){



        return true;
    }

    public static ArrayList<String> parseSettingsFileIntoArrayList(File file){

        ArrayList<String> listOfParametersFromSettingsFile = new ArrayList<>();

        if (!file.exists()) {
            try { // create file if it not exists
                System.out.println("File with setting not found. Creating it now.");
                String data = " ";
                FileOutputStream out = new FileOutputStream(MainInit.absolutePath + "/settings.txt");
                out.write(data.getBytes());
                out.close();
            } catch (Exception e) {
                System.out.println("Error while trying to create file with settings");
            }
        } else {
            try {
                System.out.println("File with setting is found. Initialising it in MEMORY variable.");

                BufferedReader fileReader = new BufferedReader((new InputStreamReader(new FileInputStream(MainInit.absolutePath + "/settings.txt"))));

                while (fileReader.ready()) {
                    listOfParametersFromSettingsFile.add(fileReader.readLine());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return listOfParametersFromSettingsFile;
    }

    static HashMap<Long, HashMap<String, String>> parseSettingsArrayListInSettingsMap(ArrayList<String> listOfParametersFromSettingsFile){

        HashMap<Long, HashMap<String, String>> mapWithSettings = new HashMap<>();

        for(String lineWithAllParams : listOfParametersFromSettingsFile){

            System.out.println("Current string with settings is: " + lineWithAllParams);

            int indexOfFirstComma = lineWithAllParams.indexOf(",");
            long idOfChat = Long.valueOf(lineWithAllParams.substring(0, indexOfFirstComma));

            mapWithSettings.put(idOfChat, new HashMap<>());

            for(int i = indexOfFirstComma; i < lineWithAllParams.length(); i++){
                if(String.valueOf(lineWithAllParams.charAt(i)).equals(",")){
                    int nextIndexOfComma = lineWithAllParams.indexOf("," , i+1);
                    if(nextIndexOfComma == -1){
                        nextIndexOfComma = lineWithAllParams.length();
                    }
                    String dataToWrite = lineWithAllParams.substring(i+1, nextIndexOfComma);
                    mapWithSettings.get(idOfChat).put(
                            dataToWrite.substring(0, dataToWrite.indexOf("=")),
                            dataToWrite.substring(dataToWrite.indexOf("=")+1)
                    );
                }
            }
            System.out.println("Parsed all params for ChatID into map: " + idOfChat);
        }
        return mapWithSettings;
    }
}
