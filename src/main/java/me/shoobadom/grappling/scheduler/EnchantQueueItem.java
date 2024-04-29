package me.shoobadom.grappling.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class EnchantQueueItem {
    private final Player p;
    private final Location tableLoc;
    private final int[] offers;
    public EnchantQueueItem(Player p, Location tableLoc, int[] offers) {
        this.p=p;
        this.tableLoc=tableLoc;
        this.offers=offers;
    }
    public int[] getOffers() {
        return offers;
    }
    public Location getTableLoc() {
        return tableLoc;
    }
    public Player getPlayer() {
        return p;
    }
}
