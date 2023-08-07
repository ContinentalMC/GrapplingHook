package me.shoobadom.grappling.commands;


import me.shoobadom.grappling.Grappling;
import me.shoobadom.grappling.items.ItemManager;
import me.shoobadom.grappling.presets.PresetHolder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class GrappleCommand implements CommandExecutor {
    private final Grappling instance = Grappling.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command c, String s, String[] a) {
        if (!(c.getName().equalsIgnoreCase("grapple")&&sender.isOp())) {
            return true;
        }
        if (a.length == 0) {
            sender.sendMessage(ChatColor.AQUA+"Welcome to Shoobadom's Grappling Hooks. Use '/gh help' for help");
            return true;
        }
        if (a[0].equalsIgnoreCase("help")) {
            if (a.length > 1) {
                if (a[1].equalsIgnoreCase("controls")) {
                    sender.sendMessage(mp("Grappling hook controls:","""
                            If the tool is in your main hand: left click
                            If the tool is in your off hand: right click
                            Clicking will both project the hook and bring it back. As soon as it has latched on to an entity or a block, it will cause either (if latched to a block) the player to move or (if latched on to an entity) the entity to move to the player.
                            Sneaking will pause the reeling."""));
                } else if (a[1].equalsIgnoreCase("give")) {
                    sender.sendMessage(mp("/grapple give ...","""
                            Description: Allows the user to get a grappling hook
                            Usage (gives default preset to command sender): /grapple give
                            Usage (gives default preset): /grapple give <@p | @a | player name>
                            Usage for presets: /grapple give <@p | @a | player name> <presetName>
                            Use '/gh info' to obtain a list of valid presets"""));
                } else if (a[1].equalsIgnoreCase("info")) {
                    sender.sendMessage(mp("/grapple info","""
                            Description: Details information about the plugin such as which presets have been detected.
                            Usage: /grapple info"""));
                } else if (a[1].equalsIgnoreCase("help")) {
                    sender.sendMessage(mp("/grapple help","""
                            Description: Provides help for the user.
                            Usage: /grapple help <command>"""));
                } else {
                    sender.sendMessage(ChatColor.RED+"There is no help for that command as it does not exist!");
                }
            } else {
                sender.sendMessage(mp("""
                                The following is a list of all commands. Use '/gh help <command>' to find out more.
                                [Note: when using commands, preface them with /gh, e.g. '/gh give']""",
                            """
                                        - help
                                        - give
                                        - info
                                        For help with using grappling hook, type 'gh help controls'.
                                        """));
            }
            return true;
        }
        if (a[0].equalsIgnoreCase("info")) {
            TextComponent build = new TextComponent(ChatColor.GOLD + "" + ChatColor.BOLD + "Shoobadom's Grappling Hooks" + ChatColor.RESET + ChatColor.GOLD + " (" + instance.getDescription().getName() + ")" + ChatColor.BOLD + "\nVersion " + instance.getDescription().getVersion() + ChatColor.RESET + ChatColor.AQUA + "\n\nThe following presets have been detected in the presets.json file (hover for details): ");
            if (PresetHolder.getPresetList().isEmpty()) {
                build.addExtra(ChatColor.RED + "\n No valid presets have been detected. You can always reset the presets back to defaults by deleting the presets.json file." + ChatColor.AQUA);
            }

            for (String ok : PresetHolder.getPresetList()) {
                TextComponent sec = new TextComponent(ok);

                ItemMeta hok = ItemManager.createHook(ok).getItemMeta();
                StringBuilder strong = new StringBuilder(hok.getDisplayName());
                if (hok.getLore() != null) {
                    for (String st : hok.getLore()) {
                        strong.append("\n").append(st);
                    }
                }
                HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(strong.toString()));
                sec.setHoverEvent(he);
                build.addExtra("\n- ");
                build.addExtra(sec);

            }
            //k.setColor(ChatColor.AQUA.asBungee());

            build.addExtra(ChatColor.YELLOW+"""
                    
                    If a preset you believe is present in presets.json is not showing up, it may not be set up correctly.
                    I hope you are enjoying my plugin. Please leave feedback so I now what new features you enjoy, or would like to see or improved upon.
                    Thanks for downloading!
                    """);
            sender.spigot().sendMessage(build);
            return true;
        }

        if (a[0].equalsIgnoreCase("give")) {
            if (!( sender instanceof Player p)) {
                sender.sendMessage(ChatColor.RED+"Only players may use this command!");
                return true;
            }
            if (a.length==1) {
                p.getInventory().addItem(ItemManager.createHook());
                return true;
            } else {
                ItemStack item;
                if (a.length==2) {
                    item= ItemManager.createHook();
                } else {
                    if (PresetHolder.isPreset(a[2])) {
                        item = ItemManager.createHook(a[2]);
                    } else {
                        p.sendMessage(ChatColor.RED + "That preset does not exist, or is not formatted correctly in the presets.json file.");
                        return true;
                    }
                }
                String sel=a[1];
                if (sel.equalsIgnoreCase("@p") || sel.equals(p.getName())) {
                    // Return to sender
                    p.getInventory().addItem(item);
                    p.sendMessage(ChatColor.AQUA + "You have been given a grapple.");
                } else if (sel.equalsIgnoreCase("@a")) {
                    // Share grapple
                    p.sendMessage(ChatColor.AQUA + "Distributed grapples amongst all players.");
                    for (Player p2 : instance.getServer().getOnlinePlayers()) {
                        p2.getInventory().addItem(item);
                    }
                } else {// if (selector.equals(tempName)) {
                    Player temp = instance.getServer().getPlayer(sel);
                    if (temp != null) {
                        // Gift wrap
                        temp.getInventory().addItem(item);
                        p.sendMessage(ChatColor.AQUA + "Given grapple to " + temp.getName() + ".");
                    } else {
                        p.sendMessage(ChatColor.RED + "Error. Invalid selector. Use @p, @a, or a specific player name.");
                    }
                }
                return true;
            }

        }
        sender.sendMessage(ChatColor.RED + "Error. Invalid command. For help, use '/gh help'");




        return true;
    }

    private String mp(String title, String body) {
        return ChatColor.YELLOW+title+"\n"+ChatColor.AQUA+body;
    }

}
