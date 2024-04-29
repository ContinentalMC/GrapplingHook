package me.shoobadom.grappling;

import me.shoobadom.grappling.commands.GrappleCommand;
import me.shoobadom.grappling.events.Events;
import me.shoobadom.grappling.scheduler.Tick;
import me.shoobadom.grappling.util.FileManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Grappling extends JavaPlugin {
    private static Grappling instance;
    private static final Logger logger = Bukkit.getServer().getLogger();

    public static String getGrappleMetaDataString() {
        return "ShGh";
    }

    @Override
    public void onEnable() {
        instance=this;

        FileManager.setup(this);
        getServer().getPluginManager().registerEvents(new Events(),this);
        Metrics metrics = new Metrics(this,18381);
        Tick.enableTick();
        GrappleCommand cmd = new GrappleCommand();
        getCommand("grapple").setExecutor(cmd);
        brd("All faculties of the plugin enabled");
    }


    public static void brd(String s) {
        logger.warning("["+instance.getName()+"] "+s);
    }
    public static void addRecipe(ShapedRecipe sr) {

        if (instance.getServer().getRecipe(sr.getKey())== null) {
            instance.getServer().addRecipe(sr);
        }

    }
    public static Grappling getInstance() {
        return instance;
    }

}
