package me.shoobadom.grappling.scheduler;

import de.tr7zw.nbtapi.NBTItem;
import me.shoobadom.grappling.Grappling;
import me.shoobadom.grappling.events.PlayerGrapple;
import me.shoobadom.grappling.inventories.Enchant;
import me.shoobadom.grappling.items.ItemManager;
import me.shoobadom.grappling.presets.Preset;
import me.shoobadom.grappling.presets.PresetHolder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Tick {
    private final static HashMap<UUID, PlayerGrapple> playerMapple= new HashMap<>();
    private static final Grappling plugin = Grappling.getInstance();


    private final static HashMap<UUID,EnchantQueueItem> playerEnchant = new HashMap<>();
    public static void playerEnchantSwitch(EnchantQueueItem eqi) {
        playerEnchant.put(eqi.getPlayer().getUniqueId(),eqi);

    }
    private final static HashSet<UUID> checkLapis = new HashSet<>();
    public static void addLapisCheck(Player p) {checkLapis.add(p.getUniqueId());}
    public static void enableTick() {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack[] toCheck = {p.getInventory().getItemInMainHand(),p.getInventory().getItemInOffHand()};
                for (ItemStack i : toCheck) {
                    if (i == null || i.getType()== Material.AIR) {
                        continue;
                    }
                    NBTItem itemNBT = new NBTItem(i);
                    if (!ItemManager.isGrapple(itemNBT)) {
                        continue;
                    }
                    int rlTime = itemNBT.getInteger("rlt");
                    if (rlTime > 0) {
                        itemNBT.setInteger("rlt",rlTime-1);
                    }
                    if (rlTime-1 == 0) {
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER, 0.5F, 1.15F);
                        itemNBT.setInteger("rlt",0);
                    }
                    if (itemNBT.getInteger("rlt") == -1
                            && getPG(p)==null) {

                        Preset pr = PresetHolder.getPreset(itemNBT.getString("preset"));
                        double rlt =pr.getReloadTime(i);
                        if (rlt==0.0) {
                            itemNBT.setInteger("rlt",0);
                        } else {
                            itemNBT.setInteger("rlt",(int)Math.round(20*rlt));

                        }
                    }
                    itemNBT.mergeNBT(i);
                }
                if (playerEnchant.get(p.getUniqueId()) != null) {
                    Inventory invTop = p.getOpenInventory().getTopInventory();
                    if (invTop.getHolder() instanceof Enchant enchantInv) {
                        ItemStack sword= enchantInv.getGrapple();
                        ItemStack lapis= enchantInv.getLapis();
                        InventoryView inve = p.openEnchanting(enchantInv.getLocation(),false);

                        if (inve == null) {
                            // such as if ench table is deleted
                            p.closeInventory();
                        } else {
                            EnchantingInventory enIn= (EnchantingInventory) inve.getTopInventory();
//                            if (lapis!=null) {
//                                if (lapis.getType() == Material.LAPIS_LAZULI) {
//                                    enIn.setSecondary(lapis);
//                                } else {
////                                    p.getInventory().addItem(lapis);
////                                }
//                            }
                            if (sword !=null) {
                                enIn.setItem(sword);
                            }
                        }
                    } else if (invTop.getType() == InventoryType.ENCHANTING) {
                        EnchantingInventory enchInv = (EnchantingInventory) invTop;
                        if (ItemManager.isGrapple(enchInv.getItem())) {
                            new Enchant(playerEnchant.get(p.getUniqueId()));
                        }

                    }
                }
                if (checkLapis.contains(p.getUniqueId())) {
                    Inventory invTop = p.getOpenInventory().getTopInventory();
                    if (invTop.getHolder() instanceof Enchant en) {
                        ItemStack lapis = en.getLapis();
                        int lapCount=0;
                        if (lapis != null&&lapis.getType()==Material.LAPIS_LAZULI) {
                            lapCount = lapis.getAmount();
                        }
                        int getLevel=p.getLevel();
                        if (p.getGameMode() == GameMode.CREATIVE) {
                            getLevel=-999;
                        }
                        en.updateEnchantArea(lapCount,getLevel);
                    }

                }
                checkLapis.clear();
                playerEnchant.clear();


                PlayerGrapple pg = getPG(p);
                if (pg != null) {
                    pg.incrementShootTimer();

                    if (pg.didPJustShoot()) {
                        pg.removePJustShot();
                    }

                }
            }
        },0L,1L);
    }
    public static void addPlayerToMap(Player p) {
        playerMapple.put(p.getUniqueId(),null);
    }
    public static PlayerGrapple getPG(Player p) {
        return playerMapple.get(p.getUniqueId());
    }
    public static void setPG(Player p, PlayerGrapple pg) {
        playerMapple.put(p.getUniqueId(),pg);
    }
}
