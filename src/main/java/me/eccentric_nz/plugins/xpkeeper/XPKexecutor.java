package me.eccentric_nz.plugins.xpkeeper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XPKexecutor implements CommandExecutor
{
    private final XPKeeper plugin;
    private final XPKdatabase service = XPKdatabase.getInstance();

    public static final Map<String, String> permissions = Collections.unmodifiableMap(
        new HashMap<String, String>()
        {{
            put("xpkreload", "xpkeeper.admin");
            put("xpkgive", "xpkeeper.admin");
            put("xpkset", "xpkeeper.admin");
            put("xpkwithdraw", "xpkeeper.admin");
            put("xpkcolour", "xpkeeper.admin");

            put("xpkfist", "xpkeeper.fist");
            put("xpklimit", "xpkeeper.limit");
            put("xpkedit", "xpkeeper.editsign");

            put("xpkpay", "xpkeeper.use");
        }}
    );

    public static final Set<String> playerOnlyCommands = Collections.unmodifiableSet(
        new HashSet<String>()
        {{
            add("xpkremove");
            add("xpkedit");
            add("xpkpay");
        }}
    );

    public static final Map<String, String> boolCommands = Collections.unmodifiableMap(
        new HashMap<String, String>()
        {{
            put("xpkfist", "must_use_fist");
            put("xpklimit", "set_limits");
        }}
    );
    public static final Map<String, String> uintCommands = Collections.unmodifiableMap(
        new HashMap<String, String>()
        {{
            put("xpkwithdraw", "withdraw");
        }}
    );

    public XPKexecutor(XPKeeper plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        try
        {
            String cmdName = cmd.getName().toLowerCase();

            Player player = null;
            if (sender instanceof Player)
                player = (Player)sender;
            else if (playerOnlyCommands.contains(cmdName))
            {
                sendRaw(sender, "You may not use this command at the console.");
                return true;
            }

            String requiredPerm = permissions.get(cmdName);
            if (requiredPerm != null && !sender.hasPermission(requiredPerm))
            {
                sendMessage(sender, "messages.no_perms_command");
                return true;
            }

            String opt = boolCommands.get(cmdName);
            if (opt != null)
            {
                boolean b;
                if (args.length > 0)
                {
                    String s = args[0].toLowerCase();
                    b = s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("t") || s.equals("on");
                    if (!b && !s.equals("false") && !s.equals("0") && !s.equals("no") && !s.equals("n") && !s.equals("f") && !s.equals("off"))
                        return false; //Show usage if it's not clearly representing a boolean true or false
                }
                else
                    b = plugin.getConfig().getBoolean(opt);
                plugin.getConfig().set(opt, !b);
                plugin.saveConfig();
                sendRaw(sender, ChatColor.AQUA + opt + ChatColor.RESET + " config value set to: " + !b);
                return true;
            }
            opt = uintCommands.get(cmdName);
            if (opt != null)
            {
                int i = Integer.parseInt(args[0]);
                if (i < 0)
                    throw new NumberFormatException();
                plugin.getConfig().set(opt, i);
                plugin.saveConfig();
                sendRaw(sender, ChatColor.AQUA + opt + ChatColor.RESET + " config value set to: " + i);
                return true;
            }

            int i;
            switch(cmdName)
            {
                case "xpkreload":
                    plugin.reloadConfig();
                    sendRaw(sender, "Config reloaded!");
                    return true;
                case "xpkgive":
                    i = Integer.parseInt(args[1]);
                    new XPKCalculator(getPlayer(args[0])).addXp(i);
                    sendRaw(sender, "Gave " + i + " XP to " + args[0]);
                    return true;
                case "xpkset":
                    i = Integer.parseInt(args[1]);
                    new XPKCalculator(getPlayer(args[0])).setXp(i);
                    sendRaw(sender, "Set " + args[0] + " to " + i + " XP");
                    return true;
                case "xpkcolour":
                    String s = args[0].toLowerCase();
                    if (!colours.containsKey(s))
                    {
                        sendRaw(sender, "Unrecognized colour format. Recognized formats: 4 &4 dark_red darkred");
                        return true;
                    }
                    i = colours.get(s);
                    plugin.getConfig().set("firstline_colour", colourCodes.get(i));
                    plugin.saveConfig();
                    sendRaw(sender, ChatColor.AQUA + "firstline_colour" + ChatColor.RESET + " config value set to: " + colourNames.get(i));
                    return true;
                case "xpkforceremove":
                    String playerName = null;
                    if (player != null)
                        playerName = player.getName();
                    if (args.length == 1)
                    {
                        if (sender.hasPermission("xpkeeper.force"))
                            playerName = args[0];
                        else
                        {
                            sendMessage(sender, "messages.no_perms_command");
                            return true;
                        }
                    }
                    if (playerName == null)
                    {
                        sendRaw(sender, "You must specify a player name when running this command from the console.");
                        return true;
                    }
                    Statement statement = null;
                    ResultSet rsget = null;
                    try
                    {
                        Connection connection = service.getConnection();
                        statement = connection.createStatement();
                        String queryRemoveGet = "SELECT xpk_id FROM xpk WHERE player = '" + playerName + "'";
                        rsget = statement.executeQuery(queryRemoveGet);
                        if (rsget.isBeforeFirst())
                        {
                            String queryRemovePlayer = "DELETE FROM xpk WHERE player = '" + playerName + "'";
                            statement.executeUpdate(queryRemovePlayer);
                        }
                    }
                    catch (SQLException e)
                    {
                        System.err.println("[XPKeeper] Could not get and remove player data: " + e);
                    }
                    finally
                    {
                        try
                        {
                            if (rsget != null)
                                rsget.close();
                            if (statement != null)
                                statement.close();
                        }
                        catch (SQLException e) { }
                    }
                    sendRaw(sender, "All database entries for " + ChatColor.RED + playerName + ChatColor.RESET + " were removed.");
                    return true;
                case "xpkremove":
                    plugin.setRemoving(player.getName());
                    sendRaw(player, "messages.click_sign");
                    return true;
                case "xpkedit":
                    try
                    {
                        Sign sign = (Sign)player.getTargetBlock(null, 10).getState();
                        StringBuilder builder = new StringBuilder(args[0]);
                        for (i = 1; i < args.length; i++)
                            builder.append(' ').append(args[i]);
                        sign.setLine(0, builder.toString());
                        sign.update();
                    }
                    catch (NullPointerException ex)
                    {
                        sendMessage(player, "messages.no_sign");
                    }
                    catch (ClassCastException ex)
                    {
                        sendMessage(player, "messages.look_sign");
                    }
                    return true;
                case "xpkpay":
                    Player receiver = getPlayer(args[0]);
                    i = Integer.parseInt(args[1]);
                    XPKCalculator xpkc_g = new XPKCalculator(player);
                    XPKCalculator xpkc_r = new XPKCalculator(receiver);
                    // check whether the giver has enough to give
                    if (i > xpkc_g.getXp()) {
                        sendMessage(sender, "messages.not_enough");
                        return true;
                    }
                    xpkc_r.addXp(i);
                    xpkc_g.addXp(-i);
                    sendMessage(player, "messages.giver", args[0], i);
                    sendMessage(receiver, "messages.reciever", player.getName(), i);
                    return true;
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            sendMessage(sender, "messages.arguments");
            return false;
        }
        catch (NumberFormatException e)
        {
            sendRaw(sender, "Could not convert argument to number");
            return false;
        }
        catch (IllegalArgumentException e)
        {
            sendMessage(sender, e.getMessage());
            return true;
        }
        return false;
    }

    private void sendMessage(CommandSender s, String msg, Object... args)
    {
        msg = plugin.getConfig().getString(msg);
        if (args.length > 0)
            msg = String.format(msg, args);
        sendRaw(s, msg);
    }
    private void sendRaw(CommandSender s, String msg)
    {
        s.sendMessage(plugin.msgPrefix + msg);
    }
    private Player getPlayer(String s)
    {
        Player p = plugin.getServer().getPlayer(s);
        if (p == null)
            throw new IllegalArgumentException("messages.no_player");
        return p;
    }

    public static final Map<Integer, String> colourNames = Collections.unmodifiableMap(
        new HashMap<Integer, String>()
        {{
            put(0x0, "Black");
            put(0x1, "Dark Blue");
            put(0x2, "Dark Green");
            put(0x3, "Dark Aqua");
            put(0x4, "Dark Red");
            put(0x5, "Purple");
            put(0x6, "Gold");
            put(0x7, "Grey");
            put(0x8, "Dark Grey");
            put(0x9, "Indigo");
            put(0xa, "Bright Green");
            put(0xb, "Aqua");
            put(0xc, "Red");
            put(0xd, "Pink");
            put(0xe, "Yellow");
            put(0xf, "White");
        }}
    );
    public static final Map<Integer, String> colourCodes = Collections.unmodifiableMap(
        new HashMap<Integer, String>()
        {{
            for (int i = 0; i < 16; i++)
                put(i, "&" + Integer.toHexString(i));
        }}
    );
    public static final Map<String, Integer> colours = Collections.unmodifiableMap(
        new HashMap<String, Integer>()
        {{
            for (Map.Entry<Integer, String> e : colourCodes.entrySet())
            {
                Integer i = e.getKey();
                String s = e.getValue();
                put(s, i);
                put(s.substring(1), i);
            }
            for (Map.Entry<Integer, String> e : colourNames.entrySet())
            {
                Integer i = e.getKey();
                String s = e.getValue().toLowerCase();
                put(s.replace(' ', '_'), i);
                put(s.replace(" ", ""), i);
            }
            for (Map.Entry<String, Integer> e : entrySet())
                System.out.println(e.getKey() + ": " + e.getValue());
        }}
    );
}
