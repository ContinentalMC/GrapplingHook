package me.shoobadom.grappling.inventories;

import de.tr7zw.nbtapi.NBTItem;
import me.shoobadom.grappling.items.CustomEnchants;
import me.shoobadom.grappling.items.ItemManager;
import me.shoobadom.grappling.scheduler.EnchantQueueItem;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static java.lang.Math.min;

public class Enchant implements InventoryHolder {
    private final Inventory inv;
    private final EnchantmentOffer[] offers;
    private final Location orig;
    private final Player p;
    private int cumEn;
    private final Map<Enchantment, Integer> listOf= new HashMap<Enchantment,Integer>();

    public Enchant(EnchantQueueItem eqi) {
        inv = Bukkit.createInventory(this,27,"Enchant Grappling Hook");
        p = eqi.getPlayer();

        EnchantingInventory e = (EnchantingInventory) p.getOpenInventory().getTopInventory();

        orig = eqi.getTableLoc();
        // delete grapple thing
        ItemStack gh = e.getItem();
        ItemStack lapis = e.getSecondary();
        e.setItem(new ItemStack(Material.AIR));
        e.setSecondary(new ItemStack(Material.AIR)); // otherwise we get item dupes
        if (gh.getType() == Material.AIR) {
            gh = e.getItem();
        }

        // need to define three offers based on the levels etc

        listOf.put(CustomEnchants.PROJECTION,3);
        listOf.put(CustomEnchants.GRIP,3);
        listOf.put(CustomEnchants.RETRACTION,3);
        listOf.put(Enchantment.DURABILITY,2);
        cumEn=0; //total enchantment placement
        for (Enchantment key : listOf.keySet()) {
            cumEn+= listOf.get(key);
        }
        offers = generateOffers(eqi.getOffers());


        int lapCount=0;
        if (lapis != null) {
            lapCount=lapis.getAmount();
        }

        updateEnchantArea(lapCount, getPlayerLevel());

        inv.setItem(19,gh);
        inv.setItem(20,lapis);
        int[] grey = {0,1,2,9,11,18};
        ItemStack item= createItem(Material.GRAY_STAINED_GLASS_PANE," ");
        for (int i : grey) {
            inv.setItem(i,item);
        }
        inv.setItem(10,createItem(Material.BOOK,"Enchant your Grappling Hook"));

        p.openInventory(inv);
    }

    private int getPlayerLevel() {
        int getLevel=p.getLevel();
        if (p.getGameMode() == GameMode.CREATIVE) {
            getLevel=-999;
        }
        return getLevel;
    }

    public void updateEnchantArea(int lapCount,int playerLevel) {

        lapCount=min(lapCount,3);

        ItemStack grapple = inv.getItem(19);
        boolean letEnchant = true;
        if (grapple !=null) {
            ItemMeta meta = grapple.getItemMeta();

            if (meta.getEnchants().keySet().size() > 1) {
                // the grapple has enchantments. deny enchanting
                letEnchant=false;
            }

        }


        ItemStack item;
        int lvl;
        for (int y=0;y<3;y++) {
            for (int i =3;i<9;i++) {
                lvl = offers[y].getCost();
                if (offers[y].getEnchantment() == Enchantment.ARROW_FIRE) {
                    lvl =-2;
                } else if (playerLevel==-999){
                    lvl=-1;
                }
                if (!letEnchant || lvl == -2) {
                    item = createItem(Material.BROWN_STAINED_GLASS_PANE," ");
                }else if ((y < lapCount && playerLevel >= offers[y].getCost()) || playerLevel==-999) {
                    item = createEnchantButton(Material.YELLOW_STAINED_GLASS_PANE, CustomEnchants.getEnchantName(offers[y].getEnchantment()),offers[y].getEnchantmentLevel(),lvl,y+1);
                } else {
                    item = createEnchantButton(Material.BROWN_STAINED_GLASS_PANE,CustomEnchants.getEnchantName(offers[y].getEnchantment()),0,lvl,y+1);
                }
                inv.setItem(i+(y*9),item);
            }
        }
    }

    public void enchantGrapple(int choice) {
        // level reduction of player has already taken place FYI, and lapis too
        ItemStack grapple = inv.getItem(19);

        if (ItemManager.isGrapple(new NBTItem(grapple))) {

            ItemMeta meta = grapple.getItemMeta();
            // For now can just substitute enchantments
            Enchantment add = offers[choice].getEnchantment();
            int lvl=offers[choice].getEnchantmentLevel();

            Random r = new Random(p.getEnchantmentSeed());

            int modLvl = offers[choice].getCost();

            int bonus = r.nextInt(50);
            while (bonus <= modLvl+1) {


                int chosen = 1+r.nextInt(cumEn-listOf.get(add));
                for (Enchantment key : listOf.keySet()) {
                    if (key == add) {
                        continue;
                    }
                    chosen-=listOf.get(key);
                    if (chosen <= 0) {
                        meta.addEnchant(key,r.nextInt(3)+1,false);
                        break;
                    }
                }
                modLvl=modLvl/2;
                bonus = r.nextInt(50);
            }
            meta.addEnchant(add,lvl,false);



            grapple.setItemMeta(meta);
            ItemManager.updateHookDetails(grapple);
            int i = r.nextInt();
            p.setEnchantmentSeed(i);
            p.getWorld().playSound(orig, Sound.BLOCK_ENCHANTMENT_TABLE_USE,1F,1F);

            // update table
            int lapCount=0;
            if (inv.getItem(20) != null) {
                lapCount = inv.getItem(20).getAmount();
            }
            updateEnchantArea(lapCount,getPlayerLevel());
        }
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack createEnchantButton(Material mat,String name, int lvl, int lvlReq,int space) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§7"+name+" "+ "I".repeat(Math.max(0, lvl)) +"§f . . . ?");
        List<String> lore = new ArrayList<>();

        if(lvl==0) {
            lore.add("");
            lore.add("§c"+lvlReq+" Levels Required");
        } else if (lvlReq!=-1){
            lore.add("");
            lore.add("§7"+space+" Lapis Lazuli");
            lore.add("§7"+space+" Enchantment Levels"); // for the longest time it said enchanment. bit embarassing
            lore.add("§a"+lvlReq+" Levels Required");
        }

        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }


    @Override
    public Inventory getInventory() {
        return inv;
    }


    private EnchantmentOffer[] generateOffers(int[] lvls) {
        EnchantmentOffer[] of = new EnchantmentOffer[3];
        int levelChosen;
        Random r;
        int chosen;
        int lvl;
        for (int y=0;y<3;y++) {
            lvl = lvls[y];
            of[y] = new EnchantmentOffer(Enchantment.ARROW_FIRE,1,lvl);
            if (lvl == 0) {
                continue;
            }
            r = new Random(p.getEnchantmentSeed()+lvl);
            levelChosen=0;
            while(lvl>0) {
                levelChosen+=1;
                lvl-=r.nextInt(20);
            }
            of[y].setEnchantmentLevel(min(levelChosen,3));
            chosen = 1+r.nextInt(cumEn);
            //p.sendMessage("Chosen"+chosen);
            for (Enchantment key : listOf.keySet()) {
                chosen-=listOf.get(key);
                if (chosen <= 0) {
                    of[y].setEnchantment(key);
                    break;
                }
            }
        }
        return of;
    }
    public EnchantmentOffer[] getOffers(){
        return offers;
    }
    public Location getLocation() {
        return orig;
    }

    public ItemStack getLapis() {
        return inv.getItem(20);
    }
    public ItemStack getGrapple() {
        return inv.getItem(19);
    }
    public int slotLapis() {
        return 20;
    }
    public int slotGrapple() {
        return 19;
    }

}