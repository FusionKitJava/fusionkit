package de.marcandreher.fusionkit.util;

public class Validation {
    
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }

        // Only allow optional leading minus and digits, no decimal
        if (!strNum.matches("-?\\d+")) {
            return false;
        }

        try {
            Integer.parseInt(strNum);
            // No need to check range, parseInt throws if out of bounds
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}