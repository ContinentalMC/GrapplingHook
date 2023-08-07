package me.shoobadom.grappling.presets;


import me.shoobadom.grappling.Grappling;
import me.shoobadom.grappling.items.CustomEnchants;
import me.shoobadom.grappling.items.ItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.lang.reflect.Field;
import java.util.*;

public class Preset {
    private boolean valid = true;
    private String name;
    private String displayName = "§6Grappling Hook";



    private int durability = 100;
    private double range = 50.0;
    private double hookSpeed = 3.5;
    private double strengthBlock = 10.0;
    private double strengthEntity = 5.0;
    private double pullSpeed = 1.0;
    private double reloadTime = 3.0;


    private boolean canCraft = false;

    private String[] shape;
    private final Map<Character,String> craftKey = new HashMap<>();


    public Preset(String name,JSONObject info) {
        if (info.keySet().size() ==0) {
            if (Objects.equals(name, "default")) { // hardcoded default
                craftKey.put('C',"CROSSBOW");
                craftKey.put('a',"ARROW");
                craftKey.put('r',"REDSTONE_DUST");
                craftKey.put('c',"CHAIN");
                shape = new String[]{"a  "," cr"," rC"};
                canCraft=true;
            } else {
                valid=false;
            }
        }


        this.name=name.toLowerCase();
        setField(info);

    }

    private void setField(JSONObject jsonObject){
        for (Object key : jsonObject.keySet()) {

            String fieldName = (String) key;
            Object fieldValue = jsonObject.get(key);

            if (fieldValue instanceof JSONObject) {
                if (Objects.equals(fieldName, "key")) {
                    // this needs to be serialised in a different way
                    for (Object s : ((JSONObject) fieldValue).keySet()) {
                        String sName = (String) s;
                        String sValue = (String) ((JSONObject) fieldValue).get(s);
                        craftKey.put(sName.charAt(0),sValue);
                    }
                } else {
                    setField((JSONObject) fieldValue);
                }
            } else {
                try {
                    Field field = getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    if (fieldValue instanceof Long && field.getType() == int.class) {
                        field.set(this, Math.max(0,((Long) fieldValue).intValue()));
                    } else if (fieldValue instanceof JSONArray arr && field.getType() == String[].class) {
                        String[] st = new String[3];
                        boolean val = true;
                        for (int i=0;i<arr.size();i++) {
                            String m =arr.get(i).toString();
                            if (m.length() != 3) {
                                val=false;
                            }
                            st[i] = m;
                        }
                        if (val) {
                            field.set(this, st);
                        }
                    } else {
                        // String or boolean
                        field.set(this, fieldValue);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Handle exception

                    //e.printStackTrace();
                    //...or not
                }
            }
        }

    }
    public void generateRecipe() {
        if (!canCraft || shape.length != 3) {
            return;
        }
        HashSet<Character> keyVals = new HashSet<>();
        for (int i=0;i<3;i++) {
            if (shape[i].length() != 3) {
                return;
            }
            for (int y = 0; y < 3; y++) {
                if (shape[i].charAt(y) != " ".charAt(0)) {
                    keyVals.add(shape[i].charAt(y));
                }
            }
        }
        ItemStack ne = ItemManager.createHook(name);

        ShapedRecipe sr = new ShapedRecipe(NamespacedKey.minecraft("grapple"+name), ne);
        sr.shape(shape[0],shape[1],shape[2]);
        Set<Character> c2 = craftKey.keySet();
        c2.retainAll(keyVals);
        if (c2.size() < ((Set<Character>) keyVals).size()) {
            // some things are undefined
            Grappling.brd("Could not create crafting recipe for '"+name+"' preset:");
            Grappling.brd("There are some keys in the recipe that have been left undefined:");
            for (Character c : keyVals) {
                if (!c2.contains(c)) {
                    Grappling.brd("- "+c);
                }

            }
            Grappling.brd("Please modify the crafting recipe and either remove these keys or define them.");
            return;
        }
        for (Character key : craftKey.keySet()) {
            String block = craftKey.get(key);
            Material m = Material.getMaterial(block.toUpperCase());
            if (m !=null) {
                if (m == Material.CROSSBOW) {
                    RecipeChoice.ExactChoice rc = new RecipeChoice.ExactChoice(new ItemStack(m));
                    sr.setIngredient(key,rc);
                } else {
                    sr.setIngredient(key,m);
                }
            } else {
                Grappling.brd("Could not create crafting recipe for '"+name+"' preset:");
                Grappling.brd("Material "+block.toUpperCase()+" does not exist!");
                return;
            }
        }

        sr.setCategory(CraftingBookCategory.EQUIPMENT);
        Grappling.addRecipe(sr);

    }



    public boolean isValid() {
        return valid;
    }
    public String getNameKey() {
        return name;
    }
    public String getDisplayName() {
        return displayName;
    }

    public int getDurability() {
        return durability;
    }

    public double getRange() {
        return range;
    }
    public double getRange(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return Math.max(0,range) * (1+(meta.getEnchantLevel(CustomEnchants.PROJECTION) * 0.5));
    }

    public double getHookSpeed() {
        return hookSpeed;
    }
    public double getHookSpeed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return Math.max(0,hookSpeed) * (1+(meta.getEnchantLevel(CustomEnchants.PROJECTION) * 0.2));
    }


    public double getStrengthBlock() {
        return strengthBlock;
    }
    public double getStrengthBlock(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return Math.max(0,strengthBlock) * (1+(meta.getEnchantLevel(CustomEnchants.GRIP) * 0.4));
    }

    public double getStrengthEntity() {
        return strengthEntity;
    }
    public double getStrengthEntity(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return Math.max(0,strengthEntity) * (1+(meta.getEnchantLevel(CustomEnchants.GRIP) * 0.4));
    }

    public double getPullSpeed() {
        return pullSpeed;
    }
    public double getPullSpeed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return Math.max(0,pullSpeed) * (1+(meta.getEnchantLevel(CustomEnchants.RETRACTION) * 0.1));
    }

    public double getReloadTime() {
        return reloadTime;
    }

    public double getReloadTime(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return Math.max(0,reloadTime) * (1-(meta.getEnchantLevel(CustomEnchants.RETRACTION) * 0.2));
    }

    public boolean isCanCraft() {
        return canCraft;
    }
}
