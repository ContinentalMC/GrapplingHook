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
    private static double jumpBoost;
    private static boolean grappleNPCs;
    private static boolean showStatsDef;

    private static double hookTimeout;

    public static void setup(Grappling instance) {
        File[] paths = {new File(instance.getDataFolder(),"grapples.json")};
        instance.saveDefaultConfig();
        for (File p : paths) {
            if (!p.exists()) {
                instance.saveResource(p.getName(),false);
            }
            if (p.getName().equals("grapples.json")) {
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
                        if (((String) o).equalsIgnoreCase("default")){
                            continue;
                        }
                        uploadPreset((String) o,jo);
                    }
                    reader.close();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }

            }
        }
        jumpBoost = instance.getConfig().getDouble("jumpBoost");
        grappleNPCs = instance.getConfig().getBoolean("grappleNPCs");
        hookTimeout= instance.getConfig().getDouble("hookTimeout");
        showStatsDef = instance.getConfig().getBoolean("showStatsOnDefaultGrapple");

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
    public static double getJumpBoost() {
        return jumpBoost;
    }
    public static boolean getGrappleNPCs() {
        return grappleNPCs;
    }
    public static boolean getShowStatsDefaultGrapple() {
        return showStatsDef;
    }
    public static double getHookTimeout() {return hookTimeout;}
}
