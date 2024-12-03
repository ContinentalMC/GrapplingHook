package me.shoobadom.grappling.events;

import de.tr7zw.nbtapi.NBTItem;
import me.shoobadom.grappling.Grappling;
import me.shoobadom.grappling.items.ItemManager;
import me.shoobadom.grappling.presets.Preset;
import me.shoobadom.grappling.presets.PresetHolder;
import me.shoobadom.grappling.scheduler.Tick;
import me.shoobadom.grappling.util.FileManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.Arrays;
import java.util.List;

public class PlayerGrapple {
    private int waitTime = (int)(20*FileManager.getDouble("hookTimeout"));
    private final double jumpBoost= FileManager.getDouble("jumpBoost");
    private static final Double batAddLocY = 0.9;

    private Arrow hook;
    private final Player p;
    private Bat pb;
    private Bat vb;

    // store (relevant) stats of the gh that was used to shoot:
    private double range;
    private double strengthBlock;
    private double strengthEntity;
    private double pullSpeed;
    private int dur;
    private int durMax;

    private boolean pJustShot = true;

    private final Grappling plugin = Grappling.getInstance();
    private static final String grappleMetaDataString = Grappling.getGrappleMetaDataString();

    private int ogSlot;
    private BukkitTask runner;
    private final boolean grappleNPCs = FileManager.getBool("grappleNPCs");
    private boolean takeDur=false;
    private ItemStack ghItem;

    public PlayerGrapple(Player p,boolean itemMainHand) {
        this.p=p;

        if (itemMainHand) {
            ghItem = p.getInventory().getItemInMainHand();
            ogSlot = p.getInventory().getHeldItemSlot();
        } else {
            ghItem = p.getInventory().getItemInOffHand();
            ogSlot=40;
        }
        if (!ItemManager.isGrapple(ghItem)) {
            Grappling.brd("returning");
            return;
        }


        NBTItem ghNBT = new NBTItem(ghItem);
        String pr = ghNBT.getString("preset");
        if (!PresetHolder.isPreset(pr)) {
            ItemManager.updateHookDetails(ghItem);
            ghNBT = new NBTItem(ghItem);
        }
        if (ghNBT.getInteger("rlt") != 0) {
            return;
        }

        ghNBT.setInteger("rlt",-1);
        dur = ghNBT.getInteger("durability");


        ghNBT.mergeCustomNBT(ghItem);


        Preset preset = PresetHolder.getPreset(pr);
        range = preset.getRange(ghItem);
        double hookSpeed = preset.getHookSpeed(ghItem);
        strengthBlock = preset.getStrengthBlock(ghItem);
        strengthEntity = preset.getStrengthEntity(ghItem);
        pullSpeed = preset.getPullSpeed(ghItem);
        durMax = preset.getDurability();



        Vector lookDir = p.getEyeLocation().getDirection();

        hook = p.getWorld().spawn(p.getEyeLocation().add(lookDir.getX(), lookDir.getY(), lookDir.getZ()), Arrow.class);
        hook.setShooter(p);
        hook.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        hook.setSilent(true);
        hook.setVelocity(lookDir.multiply(hookSpeed*0.5));
        hook.setMetadata(grappleMetaDataString,new FixedMetadataValue(plugin,"-_-"));

        pb = summonBat(p);


        p.getWorld().playSound(p.getEyeLocation(), Sound.ITEM_CROSSBOW_SHOOT, 0.5F, 1.4F);
        Tick.setPG(p,this);
    }

    private Bat summonBat(Entity link) {
        Bat bat = link.getWorld().spawn(link.getLocation(), Bat.class);
        bat.setSilent(true);
        bat.setAI(false);
        bat.setInvulnerable(true);
        bat.setCollidable(false);
        bat.setAwake(false);
        bat.setAware(false);
        bat.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 100000, false, false));
        bat.setMetadata(grappleMetaDataString, new FixedMetadataValue(plugin, "o.o"));
        return bat;
    }


    private List<EntityType> permittedEntities = Arrays.asList(
        // 1.21.3+
            // EntityType.ACACIA_BOAT,
            // EntityType.BAMBOO_RAFT,
            // EntityType.BIRCH_BOAT,
            // EntityType.CHERRY_BOAT,
            // EntityType.DARK_OAK_BOAT,
            // EntityType.JUNGLE_BOAT,
            // EntityType.MANGROVE_BOAT,
            // EntityType.OAK_BOAT,
            // EntityType.PALE_OAK_BOAT,
            // EntityType.SPRUCE_BOAT,
            // EntityType.ACACIA_CHEST_BOAT,
            // EntityType.BAMBOO_CHEST_RAFT,
            // EntityType.BIRCH_CHEST_BOAT,
            // EntityType.CHERRY_CHEST_BOAT,
            // EntityType.DARK_OAK_CHEST_BOAT,
            // EntityType.JUNGLE_CHEST_BOAT,
            // EntityType.MANGROVE_CHEST_BOAT,
            // EntityType.OAK_CHEST_BOAT,
            // EntityType.PALE_OAK_CHEST_BOAT,
            // EntityType.SPRUCE_CHEST_BOAT,
            EntityType.BOAT,
            EntityType.CHEST_BOAT,
            
            EntityType.MINECART,
            EntityType.CHEST_MINECART,
            EntityType.COMMAND_BLOCK_MINECART,
            EntityType.FURNACE_MINECART,
            EntityType.TNT_MINECART,
            EntityType.HOPPER_MINECART,
            EntityType.SPAWNER_MINECART
    );

    public void checkHit(Entity e) {

        if (p.getLocation().distance(hook.getLocation()) > range) {
            terminateGrapple();
            return;
        }
        int maxCount;
        Entity anchor;
        Entity particle;

        if (e != null) {
            if ((e.hasMetadata("NPC") && !grappleNPCs) || !(e instanceof LivingEntity || permittedEntities.contains(e.getType()))) {
                terminateGrapple();
                return;
            }


            hook.remove();
            maxCount = (int) Math.round(strengthEntity*20);
            anchor = p;
            if (e.isInsideVehicle()) {
                particle = e.getVehicle();
            } else {
                particle=e;
            }
            vb = summonBat(particle);
            pb.setLeashHolder(vb);
        } else {
            maxCount = (int) Math.round(strengthBlock*20);
            anchor = hook;
            particle=p;
        }
        double pBatAdd = particle.getHeight()*0.5;
        double aBatAdd = anchor.getHeight()*0.5;


        ItemMeta im = ghItem.getItemMeta();
        assert im != null;
        int unbrLvl = im.getEnchantLevel(Enchantment.UNBREAKING);
        takeDur=p.getGameMode()!= GameMode.CREATIVE && (int) (Math.random()*(unbrLvl+1)) ==0;


        runner =new BukkitRunnable() {
            int counter=0;

            public void run() {
                counter++;

                if (counter > maxCount || pb.isDead() || anchor.isDead() || particle.isDead()) {

                    terminateGrapple();
                    return;
                }

                Location pLoc = particle.getLocation().add(0,pBatAdd,0);
                Location aLoc = anchor.getLocation().add(0,aBatAdd,0);
                Vector pVector = pLoc.toVector();
                Vector aVector = aLoc.toVector();
                boolean prox = isProx(pLoc,aLoc,p.isSneaking());
                boolean superProx = isProx(pLoc,aLoc,true,0.1);
                boolean particleAboveAnchor = aLoc.getY() <= pLoc.getY() && !p.isInWater();
                double multConst = 0.65;

                if (vb !=null) {
                    vb.teleport(pLoc);
                }
                Vector vector = aVector.subtract(pVector);
                if (particleAboveAnchor) {
                    vector.setY(0.0);
                    prox = isProx(pLoc,aLoc,true);

                }
                vector.normalize();


                if (prox) {
                    if (vb!=null) {
                        terminateGrapple();
                        return;
                    }
                    if (superProx) {
                        multConst=0.0;
                    } else {
                        multConst = 0.1;
                    }

                } else {
                    if (anchor.isInWater()) {
                        multConst = 0.3;
                    }
                    if (!particleAboveAnchor) {
                        Vector aVelNorm = particle.getVelocity().normalize();
                        if (vb == null) {
                            vector = aVelNorm.add(vector).normalize();
                        } else {
                            vector = aVelNorm.add(vector).add(new Vector(0,0.3,0)).normalize();
                        }
                    }
                }

                if (particleAboveAnchor) {
                    if (p.isSneaking()) {
                        vector = particle.getVelocity();
                    } else {
                        vector.multiply(multConst*pullSpeed);
                        vector = new Vector(vector.getX(),particle.getVelocity().getY(),vector.getZ());
                    }
                } else {
                    if (Double.isNaN(vector.getX())) {
                        vector.setX(0.0);
                    }
                    if (Double.isNaN(vector.getY())) {
                        vector.setY(0.1);
                    }
                    if (Double.isNaN(vector.getZ())) {
                        vector.setZ(0.0);
                    }
                    vector.multiply(multConst*pullSpeed);
                    particle.setFallDistance(0);
                }
                if (p.isSneaking()) {
                    if (vb!=null) {
                        vector = particle.getVelocity();
                    }
                    if (vector.getY() < 0.01) {
                        vector.setY(-0.6);
                    } else {
                        vector.setY(vector.getY()*-0.6);
                    }
                } else if (counter % 8 == 0) {
                    p.getWorld().playSound(p.getLocation().add(0,batAddLocY,0), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 0.5F, 1.1F);
                }
                particle.setVelocity(vector);

            }
        }.runTaskTimer(plugin,0L,1L);

    }



    public void terminateGrapple() {
        if (pJustShot) {
            return;
        }
        if (runner != null) {
            p.getWorld().playSound(p.getLocation(),Sound.BLOCK_CHAIN_BREAK, 0.9F, 1.2F);
            p.getWorld().playSound(p.getLocation(),Sound.ITEM_CROSSBOW_LOADING_END,1.0F,0.9F);
            runner.cancel();
        }

        if (pb.isLeashed()) {
            pb.setLeashHolder(null);
        }
        pb.remove();
        if (vb != null) {
            vb.remove();
        }
        if (!hook.isDead()){
            hook.remove();
            Location pL = p.getLocation().clone();
            pL.setY(pL.getY() + batAddLocY);
            if (hook.isInBlock()
                    && isProx(pL,hook.getLocation(),false)
                    && pL.getDirection().getY() > 0.2
            ) {
                p.setVelocity(p.getVelocity().setY(0.5*jumpBoost));
            }
        }
        if (takeDur && durMax>0) {
            dur+=1;
            if (dur>=durMax) {
                ghItem.setAmount(0);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK,1F,1F);
            } else {
                NBTItem ghNBT = new NBTItem(ghItem);
                ghNBT.setInteger("durability",dur);
                ghNBT.mergeNBT(ghItem);
                org.bukkit.inventory.meta.Damageable d = (org.bukkit.inventory.meta.Damageable) ghItem.getItemMeta();
                d.setDamage((int) ( ((double) Material.CROSSBOW.getMaxDurability() * dur)/durMax) );
                ghItem.setItemMeta(d);
            }
        }



        Tick.setPG(p,null);
        // tick will also handle setting reload timer
    }


    private boolean isProx(Location one, Location two, boolean ignoreY) {
        return isProx(one,two,ignoreY,1.5);
    }
    private boolean isProx(Location one, Location two, boolean ignoreY, double nu) {

        if (ignoreY) {
            one = one.clone();
            two = two.clone();
            one.setY(0.0);
            two.setY(0.0);
        }
        return one.distance(two) < nu;
    }
    public void moveBatToPlayer() {
        pb.teleport(p.getLocation().clone().add(0,batAddLocY,0));
    }


    public int getGrappleSlot() {
        return ogSlot;
    }
    public void swapHands() {
        if (ogSlot == 40) {
            ogSlot = p.getInventory().getHeldItemSlot();
        } else {
            ogSlot=40;
        }
    }


    public void incrementShootTimer() {
        if (runner!=null) {
            return;
        }
        pJustShot=false;
        waitTime-=1;
        if (waitTime == 0) {
            terminateGrapple();
        }
    }
}
