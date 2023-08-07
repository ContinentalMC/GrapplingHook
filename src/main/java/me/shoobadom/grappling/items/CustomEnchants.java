package me.shoobadom.grappling.items;

import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CustomEnchants {

    public static final Enchantment GRIP = new EnchantmentWrapper("grip","Gripping",3);
    public static final Enchantment RETRACTION = new EnchantmentWrapper("rope","Retraction",3);
    public static final Enchantment PROJECTION = new EnchantmentWrapper("hoist","Hoist",3);

    public static void register() {
        // register enchantments when server (re)loaded
        Enchantment[] enList = {GRIP, RETRACTION, PROJECTION};

        boolean registered;
        for (Enchantment en : enList) {
            registered = Arrays.stream(Enchantment.values()).toList().contains(en);

            if (!registered) {
                registerEnchantment(en);
            }
        }

    }

    public static void registerEnchantment(Enchantment enchantment) {
        try {
            // The following allows us to add a new enchantment
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null,true);
            Enchantment.registerEnchantment(enchantment);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static String getEnchantName(Enchantment e) {
        if (e.equals(Enchantment.DURABILITY)) {
            return"Unbreaking";
        } else if (e.equals(CustomEnchants.GRIP)) {
            return"Gripping";
        }else if (e.equals(CustomEnchants.PROJECTION)) {
            return"Projection";
        }else if (e.equals(CustomEnchants.RETRACTION)) {
            return"Retraction";
        }
        return"Invalid";
    }

}