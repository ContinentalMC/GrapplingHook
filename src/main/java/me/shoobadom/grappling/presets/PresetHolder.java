package me.shoobadom.grappling.presets;

import java.util.HashMap;
import java.util.Set;

public class PresetHolder {
    private static final HashMap<String,Preset> hash = new HashMap<>();

    public static void add(String s, Preset p) {
        hash.put(s.toLowerCase(),p);
        p.generateRecipe();
    }

    public static boolean isPreset(String s) {
        return hash.containsKey(s.toLowerCase());
    }

    public static Preset getPreset(String s) {
        if (!isPreset(s)) {
            return hash.get("default");
        }

        return hash.get(s.toLowerCase());
    }

    public static Set<String> getPresetList() {
        return hash.keySet();
    }

}
