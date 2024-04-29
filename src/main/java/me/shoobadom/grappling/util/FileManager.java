package me.shoobadom.grappling.util;

import me.shoobadom.grappling.Grappling;
import me.shoobadom.grappling.presets.Preset;
import me.shoobadom.grappling.presets.PresetHolder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileManager {

    private static Grappling instance;

    public static void setup(Grappling instance2) {
        instance=instance2;
        readConfigs();


    }
    public static void readConfigs() {
        File[] paths = {new File(instance.getDataFolder(),"grapples.json")};

        instance.saveDefaultConfig();
        instance.reloadConfig();

        for (String s : PresetHolder.getPresetList()) {
            Preset p = PresetHolder.getPreset(s);
            instance.getServer().removeRecipe(p.getNamespacedkey());
        }
        for (File p : paths) {
            if (!p.exists()) {
                instance.saveResource(p.getName(),false);
            }
            if (p.getName().equals("grapples.json")) {
                PresetHolder.clear();
                try {
                    FileReader reader = new FileReader(p);
                    JSONParser parser = new JSONParser();
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p), StandardCharsets.UTF_8));
                    JSONObject jo = (JSONObject) parser.parse(br);

                    if (jo.containsKey("default")&&jo.get("default") instanceof JSONObject) {
                        uploadPreset("default",jo);
                    } else {
                        // upload hardcoded default
                        PresetHolder.add("default",new Preset("default",new JSONObject()));
                    }
                    for (Object o : jo.keySet()) {
                        String s = (String) o;
                        if (s.equalsIgnoreCase("default")){
                            continue;
                        }
                        if (PresetHolder.isPreset(s)) {
                            Grappling.brd("Could not create grapple preset '"+o+"' as it is a duplicate. Preset names are not case-sensitive!");
                            continue;
                        }
                        if (s.split(" ").length != 1) {
                            Grappling.brd("Could not create grapple preset '"+o+"' as it contains spaces in its name. Preset names must be one word!");
                            continue;
                        }
                        uploadPreset(s,jo);
                    }
                    reader.close();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private static void uploadPreset(String name, JSONObject obj) {
        if (!(obj.get(name) instanceof JSONObject)) {
            return;
        }

        Preset preset = new Preset(name,(JSONObject) obj.get(name));
        if (preset.isValid()) {
            PresetHolder.add(name,preset);
        }
    }

    public static double getDouble(String s) {
        return instance.getConfig().getDouble(s);
    }
    public static boolean getBool(String s) {
        return instance.getConfig().getBoolean(s);
    }
}
