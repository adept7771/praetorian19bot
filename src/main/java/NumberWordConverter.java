import org.apache.log4j.Logger;

import java.util.Random;

public class NumberWordConverter {

    private static final Logger log = Logger.getLogger(NumberWordConverter.class);

    public static final String[] unitsEn = {
            "", "one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    };

    public static final String[] unitsRu = {
            "", "один", "два", "три", "четыре", "пять", "шесть", "семь",
            "восемь", "девять", "десять", "одиннадцать", "двеннадцать", "триннадцать", "четырнадцать",
            "пятнадцать", "шестнадцать", "семьнадцать", "восемьнадцать", "девятьнадцать"
    };

    public static final String[] tensEn = {
            "",        // 0
            "",        // 1
            "twenty",  // 2
            "thirty",  // 3
            "forty",   // 4
            "fifty",   // 5
            "sixty",   // 6
            "seventy", // 7
            "eighty",  // 8
            "ninety"   // 9
    };

    public static final String[] tensRu = {
            "",        // 0
            "",        // 1
            "двадцать",  // 2
            "тридцать",  // 3
            "сорок",   // 4
            "пятьдесят",   // 5
            "шестьдесят",   // 6
            "семьдесят", // 7
            "восемьдесят",  // 8
            "девяносто"   // 9
    };

    public static String convert(final int n, String language, boolean letterMasking) {

        if(language.toLowerCase().contains("en")){
            if (n == 0) {
                return "zero";
            }

            if (n < 0) {
                return "minus " + convert(-n, language, letterMasking);
            }

            if (n < 20) {
                return unitsEn[n];
            }

            if (n < 100) {
                return tensEn[n / 10] + ((n % 10 != 0) ? " " : "") + unitsEn[n % 10];
            }

            if (n < 1000) {
                return unitsEn[n / 100] + " hundred" + ((n % 100 != 0) ? " " : "") + convert(n % 100, language, letterMasking);
            }

            if (n < 1000000) {
                return convert(n / 1000, language, letterMasking) + " thousand" + ((n % 1000 != 0) ? " " : "") + convert(n % 1000, language, letterMasking);
            }

            if (n < 1000000000) {
                return convert(n / 1000000, language, letterMasking) + " million" + ((n % 1000000 != 0) ? " " : "") + convert(n % 1000000, language, letterMasking);
            }

            return convert(n / 1000000000, language, letterMasking) + " billion"  + ((n % 1000000000 != 0) ? " " : "") + convert(n % 1000000000, language, letterMasking);
        }

        else if(language.toLowerCase().contains("ru")){
            if (n == 0) {
                return maskLetters("ноль", letterMasking);
            }

            if (n < 0) {
                return maskLetters("minus " + convert(-n, language, letterMasking), letterMasking);
            }

            if (n < 20) {
                return maskLetters(unitsRu[n], letterMasking);
            }

            if (n < 100) {
                return maskLetters(tensRu[n / 10] + ((n % 10 != 0) ? " " : "") + unitsRu[n % 10], letterMasking);
            }

            if (n < 1000) {
                if(n >= 100 && n < 200){
                    return maskLetters(("сто" + ((n % 100 != 0) ? " " : "") + convert(n % 100, language, false)), letterMasking);
                }
                if(n >= 200 && n < 300){
                    return maskLetters("двести" + ((n % 100 != 0) ? " " : "") + convert(n % 100, language, false), letterMasking);
                }
                if(n >= 300 && n < 400){
                    return maskLetters("триста" + ((n % 100 != 0) ? " " : "") + convert(n % 100, language, false), letterMasking);
                }
                if(n >= 400 && n < 500){
                    return maskLetters("четыреста" + ((n % 100 != 0) ? " " : "") + convert(n % 100, language, false), letterMasking);
                }
                return maskLetters(unitsRu[n / 100] + "сот" + ((n % 100 != 0) ? " " : "") + convert(n % 100, language, false), letterMasking);
            }

            if (n < 1000000) {
                return maskLetters(convert(n / 1000, language, letterMasking) + " тысяч" + ((n % 1000 != 0) ? " " : "") + convert(n % 1000, language, letterMasking), letterMasking);
            }

            if (n < 1000000000) {
                return maskLetters(convert(n / 1000000, language, letterMasking) + " миллионов" + ((n % 1000000 != 0) ? " " : "") + convert(n % 1000000, language, letterMasking), letterMasking);
            }

            return maskLetters(convert(n / 1000000000, language, letterMasking) + " биллионов"  + ((n % 1000000000 != 0) ? " " : "") + convert(n % 1000000000, language, letterMasking), letterMasking);

        }
        log.info("Error while trying to convert entered digital to letters.");
        return "";
    }

    public static String maskLetters(String cleanString, boolean maskLetters){
        if(!maskLetters){
            return cleanString;
        }
        else {
            StringBuilder resultMaskedString = new StringBuilder();
            for(int i = 0; i < cleanString.length(); i++){
                String currentChar = cleanString.substring(i, i+1);

                // Cyrillic to Latin ----------------------------

                switch (currentChar) {
                    case ("а"):
                        resultMaskedString.append("a");
                        break;
                    case ("в"):
                        resultMaskedString.append("B");
                        break;
                    case ("е"):
                        resultMaskedString.append("e");
                        break;
                    case ("ж"):
                        resultMaskedString.append("}|{");
                        break;
                    case ("з"):
                        resultMaskedString.append("3");
                        break;
                    case ("и"):
                        resultMaskedString.append("u");
                        break;
                    case ("к"):
                        resultMaskedString.append("K");
                        break;
                    case ("м"):
                        resultMaskedString.append("M");
                        break;
                    case ("н"):
                        resultMaskedString.append("H");
                        break;
                    case ("о"):
                        resultMaskedString.append("0");
                        break;
                    case ("р"):
                        resultMaskedString.append("p");
                        break;
                    case ("с"):
                        resultMaskedString.append("c");
                        break;
                    case ("т"):
                        resultMaskedString.append("T");
                        break;
                    case ("у"):
                        resultMaskedString.append("y");
                        break;
                    case ("ч"):
                        resultMaskedString.append("4");
                        break;
                    case ("ь"):
                        resultMaskedString.append("b");
                        break;
                    case ("х"):
                        resultMaskedString.append("x");
                        break;
                    case ("я"):
                        resultMaskedString.append("R");
                        break;
                    case (" "):
                        resultMaskedString.append(" ");
                        break;
                    default:
                        resultMaskedString.append(currentChar);
                        break;
                }
            }
            return resultMaskedString.toString();
        }
    }

    public static void main(final String[] args) {
        final Random generator = new Random();

        int n;

        for (int i = 0; i < 20; i++) {
            n = generator.nextInt(1000);

            System.out.printf("%10d = '%s'%n", n, convert(n, "ru", true));
        }

//        int b = 278;
//        System.out.printf("%10d = '%s'%n", b, convert(b, "ru", true));

//        n = 1000;
//        System.out.printf("%10d = '%s'%n", n, convert(n));
//
//        n = 2000;
//        System.out.printf("%10d = '%s'%n", n, convert(n));
//
//        n = 10000;
//        System.out.printf("%10d = '%s'%n", n, convert(n));
//
//        n = 11000;
//        System.out.printf("%10d = '%s'%n", n, convert(n));
//
//        n = 999999999;
//        System.out.printf("%10d = '%s'%n", n, convert(n));
//
//        n = Integer.MAX_VALUE;
//        System.out.printf("%10d = '%s'%n", n, convert(n));
    }
}
