package me.shoobadom.grappling.util;

import java.text.DecimalFormat;

public class ShoobUtils {
    public static Double formatDouble(Double num) {
        DecimalFormat val = new DecimalFormat("#.#");
        return Double.parseDouble(val.format(num));
    }
    public static boolean isInt(String i) {
        try {
            Integer.parseInt(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean isDouble(String i) {
        try {
            Double.parseDouble(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
