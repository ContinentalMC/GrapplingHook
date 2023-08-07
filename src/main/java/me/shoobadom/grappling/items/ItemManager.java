package me.shoobadom.grappling.items;

import de.tr7zw.nbtapi.NBTItem;
import me.shoobadom.grappling.presets.Preset;
import me.shoobadom.grappling.presets.PresetHolder;
import me.shoobadom.grappling.util.FileManager;
import me.shoobadom.grappling.util.ShoobUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    public static boolean isGrapple(NBTItem gr) {
        return gr.getString("itemClass").contentEquals("Grapple");
    }
    public static boolean isGrapple(ItemStack it) {
        if (it ==null || it.getType()!=Material.CROSSBOW) {
            return false;
        }
        return isGrapple(new NBTItem(it));
    }
    public static ItemStack createHook() {
        return createHook("default");
    }
    public static ItemStack createHook(String presetName) {
        if (!PresetHolder.isPreset(presetName)) {
            presetName = "default";
        } else {
            presetName=presetName.toLowerCase();
        }
        Preset p = PresetHolder.getPreset(presetName);



        ItemStack item = new ItemStack(Material.CROSSBOW);

        NBTItem basedItem = new NBTItem(item);
        basedItem.setString("itemClass", "Grapple");

        basedItem.setInteger("rlt", 0);

        // tag to identify its preset
        basedItem.setString("preset",presetName);
        basedItem.setInteger("durability",0);
        ItemStack itemFinal = basedItem.getItem();
        ItemMeta meta = itemFinal.getItemMeta();
        assert meta != null; // intelliJ warning won't just leave me alone
        meta.setDisplayName(p.getDisplayName());
        meta.setCustomModelData(383595);


        meta.addEnchant(Enchantment.QUICK_CHARGE, 6, true);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_ENCHANTS);

        itemFinal.setItemMeta(meta);


        return itemFinal;
    }

    public static ItemStack updateHookDetails(ItemStack item) {
        // we need to get preset name
        NBTItem itemNBT = new NBTItem(item);
        String pr = itemNBT.getString("preset");

        if (!PresetHolder.isPreset(pr)) {
            itemNBT.setString("preset","default");
            pr="default";
        }
        itemNBT.mergeNBT(item);
        Preset p = PresetHolder.getPreset(pr);

        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(); //lore is a list of lines
        // Enchantments
        for (Enchantment e : meta.getEnchants().keySet()) {
            if (!e.equals(Enchantment.QUICK_CHARGE)) {
                String newName=CustomEnchants.getEnchantName(e);
                if (e.equals(Enchantment.MENDING)){
                    lore.add("§7Mending"); // as it has no levels
                } else {
                    String ok="";
                    for (int i=0;i<meta.getEnchantLevel(e);i++) {
                        ok+="I";
                    }
                    lore.add("§7"+newName+" "+ok);
                }

            }
        }


        if (FileManager.getShowStatsDefaultGrapple()|| !pr.equalsIgnoreCase("default")) {
            if (meta.getEnchants().keySet().size() != 0) {lore.add("");}

            lore.add("§7Rope length: " + ShoobUtils.formatDouble(p.getRange(item))+"m");//itemNBT.getDouble("range")) + "m");
            lore.add("§7Projectile speed: " + ShoobUtils.formatDouble(p.getHookSpeed(item)));

            lore.add("§7Strength (blocks): " + ShoobUtils.formatDouble(p.getStrengthBlock(item)) + "s");
            lore.add("§7Strength (entities): " + ShoobUtils.formatDouble(p.getStrengthEntity(item)) + "s");

            lore.add("§7Pull speed: " + ShoobUtils.formatDouble(p.getPullSpeed(item)));
            lore.add("§7Reload time: " + ShoobUtils.formatDouble(p.getReloadTime(item)) + "s");
        }




        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
