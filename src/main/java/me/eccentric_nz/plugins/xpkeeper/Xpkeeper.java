package me.eccentric_nz.plugins.xpkeeper;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Listener;
import org.bukkit.material.Sign;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Xpkeeper extends JavaPlugin implements Listener {

    XPKdatabase service = XPKdatabase.getInstance();
    private XPKexecutor xpkExecutor;
    XPKsign signListener = new XPKsign(this);
    XPKplayer playerListener = new XPKplayer(this);
    XPKbreak breakListener = new XPKbreak(this);
    XPKarrgghh explodeListener = new XPKarrgghh(this);
    PluginManager pm = Bukkit.getServer().getPluginManager();
    String firstline;
    public HashMap<String, Boolean> trackPlayers = new HashMap<String, Boolean>();

    @Override
    public void onDisable() {
        // TODO: Place any custom disable code here.
    }

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);
        this.getConfig().options().copyDefaults(true);
        saveConfig();
        xpkExecutor = new XPKexecutor(this);
        getCommand("setXP").setExecutor(xpkExecutor);
        getCommand("xpkremove").setExecutor(xpkExecutor);
        pm.registerEvents(signListener, this);
        pm.registerEvents(playerListener, this);
        pm.registerEvents(breakListener, this);
        pm.registerEvents(explodeListener, this);
        try {
            String path = getDataFolder() + File.separator + "XPKeeper.db";
            service.setConnection(path);
            service.createTable();
        } catch (Exception e) {
            System.err.println("[XPKeeper] Connection and Tables Error: " + e);
        }
    }

    public int getKeptLevel(String p, String w) {
        int keptLevel = -1;
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            String queryLevelGet = "SELECT level FROM xpk WHERE player = '" + p + "' AND world = '" + w + "'";
            ResultSet rsget = statement.executeQuery(queryLevelGet);
            if (rsget != null && rsget.next()) {
                keptLevel = rsget.getInt("level");
            }
            rsget.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("[XPKeeper] Could not GET Level: " + e);
        }
        return keptLevel;
    }
    public double getKeptXP(String p, String w) {
        double keptXP = -1;
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            String queryXPGet = "SELECT amount FROM xpk WHERE player = '" + p + "' AND world = '" + w + "'";
            ResultSet rsget = statement.executeQuery(queryXPGet);
            if (rsget != null && rsget.next()) {
                keptXP = rsget.getDouble("amount");
            }
            rsget.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("[XPKeeper] Could not GET XP: " + e);
        }
        return keptXP;
    }

    public void setKeptXP(int l, double a, String p, String w) {
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            String queryXPSet = "UPDATE xpk SET level = " + l + ", amount = " + a + " WHERE player = '" + p + "' AND world = '" + w + "'";
            statement.executeUpdate(queryXPSet);
            statement.close();
        } catch (SQLException e) {
            System.err.println("[XPKeeper] Could not SET XP: " + e);
        }
    }

    public void insKeptXP(String p, String w) {
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            String queryXPInsert = "INSERT INTO xpk (player,world,level,amount) VALUES ('" + p + "','" + w + "',0,0)";
            statement.executeUpdate(queryXPInsert);
            statement.close();
        } catch (SQLException e) {
            System.err.println("[XPKeeper] Could not add new database record: " + e);
        }
    }

    public void delKeptXP(String p, String w) {
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            String queryXPDelete = "DELETE FROM xpk WHERE player = '" + p + "' AND world= '" + w + "'";
            statement.executeUpdate(queryXPDelete);
            statement.close();
        } catch (SQLException e) {
            System.err.println("[XPKeeper] Could not delete database record: " + e);
        }
    }

    public BlockFace getFace(Block b) {
        Sign s = (Sign) b.getState().getData();
        BlockFace bf = s.getAttachedFace();
        return bf;
    }
}