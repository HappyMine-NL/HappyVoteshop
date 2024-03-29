package com.DeStilleGast.Minecraft.HappyMine.VoteShop;

import com.DeStilleGast.Minecraft.HappyMine.VoteShop.sign.CurrencySign;
import com.DeStilleGast.Minecraft.HappyMine.VoteShop.sign.SignShop;
import com.DeStilleGast.Minecraft.HappyMine.database.MySQLConnector;
import com.google.common.base.Joiner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.destillegast.dsgutils.DSGUtils;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by DeStilleGast on 13-11-2016.
 */
public class ShopCore extends JavaPlugin implements Listener, CommandExecutor {

    /*
    * - ui overhaul (votepoints weergeven in items)
    - group/category system
    - config uitbreiden per item
    │ - feedback (true/false)
    │ - showIfPermission (string array)
    │ - removeIfPermissions (string array)
    └ - group/category
    - command om votepoints bij te stellen (add/remove/set)
    - placeholder api support (optionele support, niet verplichten)
    */


    protected ArrayList<ShopItem> shopItems = new ArrayList<>();

    private HashMap<Player, Integer> creator = new HashMap<Player, Integer>();
    private HashMap<Player, ArrayList<String>> commandLines = new HashMap<Player, ArrayList<String>>();

    private File shopFolder;

    private String prefixCreator;
    public String prefix;
    public String boughtMessage;
    protected String poorMessage;
    private String noItemMessage;

    private String aliasCommand;

    private MySQLConnector database;

//    public static final String antiTamper = ChatColor.RESET + "-=[VoteShop item]=-";


    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        this.getCommand("voteshop").setExecutor(this);

        if (!this.getDataFolder().exists()) this.getDataFolder().mkdir();
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&8[&bVote&8]&r")).trim();
            prefixCreator = ChatColor.translateAlternateColorCodes('&', config.getString("createPrefix", "&8[&bVote creator&8]&r")).trim();
            boughtMessage = ChatColor.translateAlternateColorCodes('&', config.getString("boughtMessage", "You bought {item}&r for &e{price}&r VotePoints!")).trim();
            poorMessage = ChatColor.translateAlternateColorCodes('&', config.getString("poorMessage", "You are to poor to buy this item, you have &e{coins}&r VotePoints!")).trim();
            noItemMessage = ChatColor.translateAlternateColorCodes('&', config.getString("noItemMessage", "This server doesn't have any items in the VoteShop!")).trim();

            aliasCommand = config.getString("aliasCommand", "spawn");
        } catch (Exception ex) {
            ex.printStackTrace();
        }



        /* Database */
        File databaseConfigFile = new File(this.getDataFolder(), "database.yml");
        YamlConfiguration cf;

        if (databaseConfigFile.exists()) {
            cf = YamlConfiguration.loadConfiguration(databaseConfigFile);
        } else {
            cf = new YamlConfiguration();
            cf.set("host", "");
            cf.set("database", "Voteshop");
            cf.set("Username", "root");
            cf.set("Password", "toor");
            try {
                cf.save(databaseConfigFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            getLogger().info("Config file created");
        }
        String mysqlHost = cf.getString("host");
        String mysqlDatabase = cf.getString("database", "Voteshop");
        String mysqlUser = cf.getString("Username", "root");
        String mysqlPass = cf.getString("Password", "toor");

        this.database = new MySQLConnector(mysqlHost, mysqlDatabase, mysqlUser, mysqlPass);
        if (!this.database.hasConnection()) getLogger().info("No DATABASE");


        shopFolder = new File(this.getDataFolder(), "shops");

        if (!shopFolder.exists()) shopFolder.mkdirs();


        loadShopItems();


        Plugin utilPlugin = Bukkit.getPluginManager().getPlugin("DSGUtils");
        if(utilPlugin != null){
            ((DSGUtils)utilPlugin).getSignManager().registerHandler("VoteShop", new SignShop(this));
            ((DSGUtils)utilPlugin).getSignManager().registerHandler("VotePoints", new CurrencySign(this));
            getLogger().info("DSGUtilties has been used");
        }
    }

    public void loadShopItems() {
        shopItems.clear();

        for (File f : shopFolder.listFiles()) {
            this.getLogger().info("Shop item found: " + f.getName());
            YamlConfiguration cf = YamlConfiguration.loadConfiguration(f);
            //addPerk(cf.getItemStack("Item"), cf.getInt("refillTime", 5));

            ItemStack item = cf.getItemStack("Item");
            if (item == null) continue;



//            shopItems.add(new ShopItem(item, commands, cf.getInt("price", 100000)));
            shopItems.add(new ShopItem(item, cf, f.getName()));
            this.getLogger().info("Shop item added: " + f.getName());
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            if(p.getOpenInventory().getTopInventory().getHolder() instanceof ShopUI){
                p.closeInventory();
            }
        });

    }

//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent e) {
//        if (e.getInventory().getHolder() != this) return;
//
//        Player p = (Player) e.getWhoClicked();
//        ItemStack itemStack = e.getCurrentItem();
//
//        if (itemStack == null) {
//            return;
//        }
//
//
//        for (ShopItem si : shopItems) {
//            ItemStack shopItm = si.getItemStack();
//
//            if (itemStack.isSimilar(shopItm)) {
//                int myPoints = getCurrency(p);
//                if (myPoints >= si.getPrice()) {
//                    pay(p, si.getPrice());
//
//                    for (String cmd : si.getCommands()) {
//                        cmd = cmd.replace("{player}", p.getName());
//                        getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
//                    }
//
//                    String itemName = "no name";
//                    if (itemStack.hasItemMeta() & itemStack.getItemMeta() != null /* to get rid of annoying build warnings */) {
//                        if (itemStack.getItemMeta().hasDisplayName()) {
//                            itemName = itemStack.getItemMeta().getDisplayName();
//                        } else if (itemStack.getItemMeta().hasLocalizedName()) {
//                            itemName = itemStack.getItemMeta().getLocalizedName();
//                        }
//                    }
//
//                    p.sendMessage(String.format("%s %s", prefix, boughtMessage.replace("{item}", itemName).replace("{price}", si.getPrice() + "")));
//                    this.getLogger().info(String.format("%s player %s has bought '%s'", prefix, p.getName(), itemName));
//                } else {
//                    p.sendMessage(prefix + " " + poorMessage.replace("{coins}", myPoints + ""));
//                }
//            }
//        }
//        e.setCancelled(true);
//    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length == 0) {
                if (shopItems.size() == 0) {
                    sender.sendMessage(prefix + " " + noItemMessage);
                    return true;
                } else {
                    p.performCommand(aliasCommand);
//                    sender.sendMessage(prefix + " shop command is gesloten, bezoek ons via de fysieke shop");
//                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
//                        Inventory shopInventory = openShop(p);
//
//                        Bukkit.getScheduler().runTask(this, () -> p.openInventory(shopInventory));
//                    });
                }
            } else if (args[0].equalsIgnoreCase("create") && sender.isOp()) {
                sender.sendMessage(prefixCreator + label + " addcommand <command>");
                sender.sendMessage(prefixCreator + label + " setname <new name>");
                sender.sendMessage(prefixCreator + label + " setlore <command>");
                sender.sendMessage(prefixCreator + label + " setprice <price>");
                sender.sendMessage(prefixCreator + label + " done");

                commandLines.put(p, new ArrayList<String>());

            } else if (args[0].equalsIgnoreCase("done") && sender.isOp()) {
                try {
                    File shopFolder = new File(this.getDataFolder(), "shops");
                    YamlConfiguration test = YamlConfiguration.loadConfiguration(new File(shopFolder, args[1] + ".yml"));
                    ItemStack newPerk = p.getInventory().getItemInMainHand();

                    test.set("Item", newPerk);
                    test.set("price", creator.getOrDefault(p, 10));
                    test.set("runCommands", commandLines.get(p));

//                    test.set("showIfAllPermission", new ArrayList<String>());
//                    test.set("hideIfAnyPermission", new ArrayList<String>());

                    test.set("enabled", true);

                    test.save(new File(shopFolder, args[1] + ".yml"));

                    if (commandLines.containsKey(p)) {
                        commandLines.remove(p);
                    }

                    sender.sendMessage(prefixCreator + " You will need to update the config of " + args[1] + " and after that run '/voteshop reload'");
                } catch (IOException e) {
                    sender.sendMessage(prefixCreator + " Enter name of config");
                    e.printStackTrace();
                }

            } else if (args[0].equalsIgnoreCase("setname") && sender.isOp()) {
                try {
                    ItemStack it = p.getInventory().getItemInMainHand();
                    ItemMeta im = it.getItemMeta();
                    im.setDisplayName(ChatColor.translateAlternateColorCodes('&', Joiner.on(' ').join(getAllArgs(1, args))));
                    it.setItemMeta(im);


                    sender.sendMessage(prefixCreator + "name set to " + it.getItemMeta().getDisplayName());
                } catch (Exception e) {
                    sender.sendMessage(prefixCreator + label + " setname <new name>");
                    e.printStackTrace();
                }

            } else if (args[0].equalsIgnoreCase("addcommand") && sender.isOp()) {
                try {
                    if (commandLines.containsKey(p)) {
                        ArrayList<String> cmds = commandLines.get(p);
                        cmds.add(Joiner.on(" ").join(getAllArgs(1, args)));
                        commandLines.put(p, cmds);

                        for (String s : cmds) {
                            p.sendMessage(s);
                        }
                    }

                } catch (Exception e) {
                    sender.sendMessage(prefixCreator + label + " addcommand <new command>");
                    e.printStackTrace();
                }

            } else if (args[0].equalsIgnoreCase("setprice") && sender.isOp()) {

                try {
                    creator.put(p, Integer.parseInt(args[1]));
                    sender.sendMessage(prefixCreator + "price set to " + creator.get(p));
                } catch (Exception ex) {
                    sender.sendMessage(prefixCreator + label + " setprice <price>");
                    ex.printStackTrace();
                }
            } else if (args[0].equalsIgnoreCase("reload") && sender.isOp()) {
                this.loadShopItems();
                sender.sendMessage(prefixCreator + "reloaded :D");
            } else if (args[0].equalsIgnoreCase("take") && sender.hasPermission("voteshop.take")) {
                try {
                    String playerName = args[1];
                    int amount = Integer.parseInt(args[2]);

                    Player player = null;

                    for (Player i : Bukkit.getOnlinePlayers()) {
                        if (i.getName().equalsIgnoreCase(playerName)) {
                            player = i;
                            break;
                        }
                    }

                    if (player == null) {
                        sender.sendMessage(prefixCreator + " player " + playerName + " doesn't exist!");
                        return true;
                    }

                    if (getCurrency(p) >= amount) {
                        pay(player, amount);
                        sender.sendMessage(prefixCreator + " successfully took " + amount + " votepoints from " + playerName);
                    } else {
                        sender.sendMessage(prefixCreator + "Player " + amount + " doesn't have enough votepoints!");
                    }
                } catch (Exception ex) {
                    sender.sendMessage(prefixCreator + " take <player> <amount>");
                    ex.printStackTrace();
                }
            }
        } else {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    this.loadShopItems();
                    sender.sendMessage(prefixCreator + "reloaded :D");
                } else {
                    sender.sendMessage(prefixCreator + "onbekende commando");
                }
            } else {
                sender.sendMessage(prefixCreator + " Sadly servers don't have any interfaces to interact with inventories");
            }
        }
        return true;
    }

    private Inventory openShop(Player p) {
        ShopUI newShop = new ShopUI(this, shopItems.stream().filter(ShopItem::isEnabled).map(ShopItem::getItemStack).sorted(Comparator.comparing(ItemStack::getType)).collect(Collectors.toList()), p);

        Bukkit.getPluginManager().registerEvents(newShop, this);
        return newShop.getInventory();
    }

    //easy for sub modules too !
    public static String[] getAllArgs(Integer offset, String[] arg) {
        String[] args = new String[arg.length - offset];

        for (Integer i = offset; i < arg.length; i++) {
            args[i - offset] = arg[i];
        }

        return args;
    }


    public int getCurrency(Player p) {

        try {
            database.open();
            PreparedStatement ps = database.prepareStatement("SELECT `VotePoints` FROM `VoteCurrency` WHERE `UUID`=?");

            ps.setString(1, p.getUniqueId().toString());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("VotePoints");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (database.hasConnection())
                database.close();
        }

        return 0;
    }

    public void pay(Player p, int amount) {

        try {
            database.open();
            PreparedStatement ps = database.prepareStatement("UPDATE `VoteCurrency` SET VotePoints=VotePoints-" + amount + " WHERE `UUID`=?");

            ps.setString(1, p.getUniqueId().toString());
            ps.execute();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (database.hasConnection())
                database.close();
        }
    }

    public ArrayList<ShopItem> getShopItems() {
        return shopItems;
    }

    //
//    @Override
//    public Inventory getInventory() {
//        //"[" + this.shopItems.size() + "] Vote shop: ");// + getCurrency(p) + " VotePoints");
//
//        return shopInventory;
//    }

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onItemGrab(InventoryClickEvent e){
//        if(e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()){
//            ItemMeta im = e.getCurrentItem().getItemMeta();
//            if(!e.isCancelled()){
//                if(im.getLore().contains(antiTamper)){
//                    e.setCancelled(true);
//                }
//            }
//        }
//    }
}
