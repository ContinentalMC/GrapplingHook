package me.shoobadom.grappling.events;

import de.tr7zw.nbtapi.NBTItem;
import me.shoobadom.grappling.Grappling;
import me.shoobadom.grappling.inventories.Enchant;
import me.shoobadom.grappling.items.CustomEnchants;
import me.shoobadom.grappling.items.ItemManager;
import me.shoobadom.grappling.presets.PresetHolder;
import me.shoobadom.grappling.scheduler.EnchantQueueItem;
import me.shoobadom.grappling.scheduler.Tick;
import me.shoobadom.grappling.util.FileManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

public class Events implements Listener {
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Tick.addPlayerToMap(event.getPlayer());
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event) {
        removePlayerIfPG(event.getPlayer());
    }
    @EventHandler
    public void playerDie(PlayerDeathEvent event) {
        removePlayerIfPG(event.getEntity());
    }
    private void removePlayerIfPG(Player p) {
        PlayerGrapple pg = Tick.getPG(p);
        if (pg == null) {
            return;
        }
        pg.terminateGrapple();
    }

    @EventHandler //(priority= EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack theItem = event.getItem();
        if (theItem != null) {
            Action action = event.getAction();
            Player p = event.getPlayer();

            ItemStack itemMainHand = p.getInventory().getItemInMainHand();
            ItemStack itemOffHand = p.getInventory().getItemInOffHand();

            // Players must left-click air or blocks if grapple is in main hand
            // Players must right-click air if grapple is in offhand (otherwise they may shoot the grapple when placing blocks)

            Boolean mainHand = null;
            if (itemMainHand.getType() != Material.AIR && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
                mainHand = true;
            } else if (itemOffHand.getType() != Material.AIR && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) { // no right clicking block. you may be placing or interacting with something

                // If you hold any item and right click, it registers an event
                // Therefore if you are holding a crossbow in the offhand it triggers
                //	this event twice, causing immediate withdrawal of grapple

                if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
                    mainHand = false;
                }

            }

            if (mainHand==null || (mainHand && !ItemManager.isGrapple(p.getInventory().getItemInMainHand()))
                    || (!mainHand && !ItemManager.isGrapple(p.getInventory().getItemInOffHand()))) {
                return;
            }
            PlayerGrapple pg = Tick.getPG(p);

            if (pg == null) {
                new PlayerGrapple(p,mainHand);
            } else {
                pg.terminateGrapple();
            }
        }


    }

    @EventHandler
    public void playerDamagesBlockWithGrapple(BlockDamageEvent e) {
        Player p = e.getPlayer();
        ItemStack itemMainHand = p.getInventory().getItemInMainHand();

        if (ItemManager.isGrapple(itemMainHand)) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void playerBreaksBlockWithGrapple(BlockBreakEvent e) {
        Player p = e.getPlayer();

        ItemStack itemMainHand = p.getInventory().getItemInMainHand();


        if (ItemManager.isGrapple(itemMainHand)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerDropsGrapple(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        PlayerGrapple pg = Tick.getPG(p);
        if (pg == null) {
            return;
        }
        int slot = pg.getGrappleSlot();
        if ((slot == 40 && p.getInventory().getItemInOffHand().getType() == Material.AIR)
        || p.getInventory().getItemInMainHand().getType() == Material.AIR) {
            pg.terminateGrapple();
        }
    }
    @EventHandler
    public void playerInteractsWithEntity(PlayerInteractEntityEvent e) {
        // INFO player right clicks villager but also holding grapple
        //	Ideally they want to use grapple not start trading
        Player p = e.getPlayer();
        PlayerGrapple pg = Tick.getPG(p);

        if (!ItemManager.isGrapple(p.getInventory().getItemInOffHand())) {
            return;
        }
        if (pg == null) {
            // launch gh
            new PlayerGrapple(p,false);
        } else {
            pg.terminateGrapple();
        }
    }
    private static final String ghmds = Grappling.getGrappleMetaDataString();
    @EventHandler
    public void playerHitsBat(EntityDamageByEntityEvent e) {
        Entity bat = e.getEntity();
        if (bat.getType()== EntityType.BAT && bat.hasMetadata(ghmds)) {
            e.setCancelled(true);
        }
        if (e.getDamager().getType() != EntityType.PLAYER) {
            return;
        }
        Player p = (Player) e.getDamager();
        ItemStack main = p.getInventory().getItemInMainHand();
        if (!ItemManager.isGrapple(main)) {
            return;
        }
        PlayerGrapple pg = Tick.getPG(p);
        if (pg == null) {
            new PlayerGrapple(p,true);
        } else {
            pg.terminateGrapple();
        }
    }
    @EventHandler
    public void batAwakens(BatToggleSleepEvent e) {
        Entity b = e.getEntity();
        if (b.hasMetadata(ghmds)) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void leashDrops(EntityDropItemEvent e) {
        if (e.getEntityType()==EntityType.BAT
        && e.getEntity().hasMetadata(ghmds)) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void playerThatShotGrappleMoves(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        PlayerGrapple pg = Tick.getPG(p);
        if (pg ==null) {
            return;
        }
        pg.moveBatToPlayer();
    }
    @EventHandler
    public void hookHit(ProjectileHitEvent e) {
        if (e.getEntityType()!=EntityType.ARROW) {
            return;
        }
        Arrow hook = (Arrow) e.getEntity();
        if (!(hook.getShooter() instanceof Player p && hook.hasMetadata(ghmds))) {
            return;
        }
        PlayerGrapple pg = Tick.getPG(p);
        if (pg == null) {
            return;
        }
        p.getWorld().playSound(hook.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.9F, 1.2F);
        if (e.getHitBlock() != null) {
            pg.checkHit(null);
        } else if (e.getHitEntity() != null) {
            e.setCancelled(true);
            pg.checkHit(e.getHitEntity());
        }


    }

    @EventHandler
    public void playerThatShotGrappleSwitchesItem(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        PlayerGrapple pg = Tick.getPG(p);
        if (pg==null) {
            return;
        }
        int pgs = pg.getGrappleSlot();
        if (pgs != 40 && e.getNewSlot() != pgs) {
            pg.terminateGrapple();
        }
    }
    @EventHandler
    public void playerSwitchHands(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        PlayerGrapple pg = Tick.getPG(p);
        if (pg!=null) {
            pg.swapHands();
        }
    }
    @EventHandler
    public void playerClickInv(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        Inventory i = e.getView().getTopInventory();
        if (i.getHolder() instanceof Enchant enchantInv) {
            if (e.getSlot() == enchantInv.slotGrapple() && e.getClickedInventory()==i) {
                Tick.playerEnchantSwitch(new EnchantQueueItem(p,null,null));
            } else if ((e.getSlot() == enchantInv.slotLapis() && e.getClickedInventory()==i) || (e.isShiftClick()&&e.getClickedInventory()!=i)) {
                 if (e.getCurrentItem()==null) {
                     //
                     if (p.getItemOnCursor().getType() != Material.LAPIS_LAZULI && p.getItemOnCursor().getType() != Material.AIR) {
                         e.setCancelled(true);
                     }
                 } else {
                     //
                     if (p.getItemOnCursor().getType() == Material.AIR) {
                         e.setCancelled(false);
                     }
                 }
            } else if (e.getClickedInventory()==i) {
                e.setCancelled(true);
                ItemStack cur = e.getCurrentItem();
                if (cur!=null && cur.getType() == Material.YELLOW_STAINED_GLASS_PANE) {
                    int index = e.getSlot()/9;
                    EnchantmentOffer offer = enchantInv.getOffers()[index];
                    ItemStack lapis = enchantInv.getLapis();
                    int lapCount = 0;
                    if (lapis != null) {
                        lapCount=lapis.getAmount();
                    }
                    if (p.getGameMode() != GameMode.CREATIVE) {
                        if (p.getLevel() < offer.getCost() || lapCount < index+1 || lapis.getType() != Material.LAPIS_LAZULI) {
                            return;
                        }
                        p.setLevel(p.getLevel()-index-1);
                        lapis.setAmount(lapCount-index-1);
                    }
                    enchantInv.enchantGrapple(index);
                }
            }

            Tick.addLapisCheck(p);
        }

        if (e.getClickedInventory() != null
                && (
                        (e.getClickedInventory().getType() == InventoryType.PLAYER)
                    || (e.getClickedInventory().getType() == InventoryType.CREATIVE)
        )   ) {
            PlayerGrapple pg = Tick.getPG(p);

            if (pg !=null && e.getSlot() == pg.getGrappleSlot()) {
                pg.terminateGrapple();
            }
        }
    }
    @EventHandler
    public void playerCloseInventory(InventoryCloseEvent e) {
        Inventory i = e.getInventory();
        if (!(i.getHolder() instanceof Enchant en)) {
            return;
        }
        Player p = (Player) e.getPlayer();
        ItemStack[] its = {en.getGrapple(),en.getLapis()};
        for (ItemStack it : its) {
            if (it !=null) {
                p.getInventory().addItem(it);
            }
        }
//        if (en.getGrapple() !=null) {
//            p.getInventory().addItem(en.getGrapple());
//        }
//        if (en.getLapis() !=null) {
//            p.getInventory().addItem(en.getLapis());
//        }
    }

    @EventHandler
    public void playerPutInEnchant(PrepareItemEnchantEvent e) {
        if (!FileManager.getBool("canEnchantGrapples")) {
            return;
        }
        ItemStack it = e.getItem();
        if (!ItemManager.isGrapple(it)) {
            return;
        }
        if (Objects.requireNonNull(it.getItemMeta()).getEnchants().keySet().size() >1) { // it is enchanted already
            return;
        }
        int[] offers = {0,0,0};
        for (int y=0;y<e.getOffers().length;y++) {
            if (e.getOffers()[y]!=null) {
                offers[y] = e.getOffers()[y].getCost();
            }
        }
        Tick.playerEnchantSwitch(new EnchantQueueItem(e.getEnchanter(),e.getEnchantBlock().getLocation(),offers));
    }


    @EventHandler
    public void playerPutGrappleInAnvil(PrepareAnvilEvent e) {
        ItemStack res = e.getResult();
        AnvilInventory inv = e.getInventory();
        ItemStack i1 = inv.getItem(0);
        ItemStack i2 = inv.getItem(1);
        if (!ItemManager.isGrapple(res)) {
            if (!ItemManager.isGrapple(i1) && ItemManager.isGrapple(i2)) {
                e.setResult(new ItemStack(Material.AIR));
            }
            return;
        }
        Enchantment[] enchantCheck = {CustomEnchants.GRIP,CustomEnchants.PROJECTION,CustomEnchants.RETRACTION,Enchantment.DURABILITY,Enchantment.MENDING};
        Enchantment[] enchantSub = {Enchantment.MULTISHOT, Enchantment.PIERCING, Enchantment.QUICK_CHARGE, Enchantment.DURABILITY, Enchantment.MENDING};

        ItemMeta resMeta = res.getItemMeta();



        if (i1==null || i1.getType()==Material.AIR) {
            return;
        }
        ItemMeta i1Meta = i1.getItemMeta();


        if (i2==null || i2.getType() == Material.AIR) {
            // renaming
            for (Enchantment ench : i1Meta.getEnchants().keySet()) {
                resMeta.addEnchant(ench, i1Meta.getEnchantLevel(ench),false );
            }
        } else {
            if (i2.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta i2ESM = (EnchantmentStorageMeta) i2.getItemMeta();
                assert i2ESM != null;
                for (Enchantment ench : i1Meta.getEnchants().keySet()) {
                    resMeta.addEnchant(ench, i1Meta.getEnchantLevel(ench), false);
                }
                for (Enchantment ench : i2ESM.getStoredEnchants().keySet()) {
                    for (int i = 0; i < enchantSub.length; i++) {
                        if (ench.getKey().equals(enchantSub[i].getKey())) {
                            // the book has a grapple enchant
                            int i1EnchantLvl = i1Meta.getEnchantLevel(enchantCheck[i]);
                            int i2EnchantLvl = i2ESM.getStoredEnchantLevel(ench);

                            if (i2EnchantLvl == i1EnchantLvl) {
                                if (i1EnchantLvl != 0) {
                                    resMeta.addEnchant(enchantCheck[i], Math.min(i1EnchantLvl + 1,ench.getMaxLevel()), false);
                                }
                            } else {
                                resMeta.addEnchant(enchantCheck[i], Math.max(Math.min(i2EnchantLvl,enchantCheck[i].getMaxLevel()), i1EnchantLvl), false);
                            }
                        }
                    }
                }
                for (int i = 0; i < 3; i++) {
                    resMeta.removeEnchant(enchantSub[i]); // otherwise we get Invalid III
                }
            } else if (ItemManager.isGrapple(i2)){
                NBTItem i1NBT = new NBTItem(i1);
                NBTItem i2NBT = new NBTItem(i2);
                if (!Objects.equals(i1NBT.getString("preset"), i2NBT.getString("preset"))) {
                    e.setResult(new ItemStack(Material.AIR));
                    return;
                }

                ItemMeta i2Meta = i2.getItemMeta();
                for (Enchantment ench : enchantCheck) {
                    int i1EnchantLvl = i1Meta.getEnchantLevel(ench);
                    int i2EnchantLvl = i2Meta.getEnchantLevel(ench);

                    if (i2EnchantLvl == i1EnchantLvl) {
                        if (i1EnchantLvl!=0) {
                            resMeta.addEnchant(ench, Math.min(i1EnchantLvl + 1,ench.getMaxLevel()), false);
                        }
                    } else {
                        resMeta.addEnchant(ench, Math.min(Math.max(i1EnchantLvl,i2EnchantLvl),ench.getMaxLevel()), false);
                    }
                }
                res.setItemMeta(resMeta);


                NBTItem resNBT = new NBTItem(res);
                int durMax = PresetHolder.getPreset(resNBT.getString("preset")).getDurability();

                resNBT.setInteger("durability", (int)Math.max(0,
                        (i1NBT.getInteger("durability")
                                + i2NBT.getInteger("durability")
                                -(durMax*1.12))// adding 12% (https://minecraft.fandom.com/wiki/Anvil_mechanics#Combining_items)
                        )
                );

                res = resNBT.getItem();
                resMeta = res.getItemMeta();
            }
        }
        res.setItemMeta(resMeta);
        e.setResult(ItemManager.updateHookDetails(res));
    }

    @EventHandler
    public void playerMendGrapple(PlayerItemMendEvent e) {
        ItemStack item = e.getItem();
        NBTItem gh = new NBTItem(item);
        if (!ItemManager.isGrapple(gh)) {
            return;
        }

        int xp = e.getExperienceOrb().getExperience();
        int durMax = PresetHolder.getPreset(gh.getString("preset")).getDurability();
        int dur = Math.max(0,gh.getInteger("durability")-xp*2);
        // ^^ two durability per point of xp (minecraft.fandom.com/wiki/Mending)
        gh.setInteger("durability",dur);
        item = gh.getItem();
        org.bukkit.inventory.meta.Damageable itemDamage = (Damageable) item.getItemMeta();
        itemDamage.setDamage((int) ( ((double) Material.CROSSBOW.getMaxDurability() * dur)/durMax) );
        item.setItemMeta(itemDamage);
        e.getPlayer().getInventory().setItem(e.getSlot(),item);
    }




}
