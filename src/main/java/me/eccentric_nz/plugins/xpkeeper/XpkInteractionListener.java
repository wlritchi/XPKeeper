package me.eccentric_nz.plugins.xpkeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.Material;
import org.bukkit.World;

public class XpkInteractionListener implements Listener {

    private XPKeeper plugin;

    List<BlockFace> faces = Arrays.asList(BlockFace.UP, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH);
    private List<BlockFace> faces2 = new ArrayList<BlockFace>();

    public XpkInteractionListener(XPKeeper plugin) {
        this.plugin = plugin;
        this.faces2.add(BlockFace.EAST);
        this.faces2.add(BlockFace.WEST);
        this.faces2.add(BlockFace.NORTH);
        this.faces2.add(BlockFace.SOUTH);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String firstline = plugin.getConfig().getString("firstline");
        List<Block> blockList = new ArrayList<Block>();
        blockList.addAll(event.blockList());
        for (Block block : blockList) {
            if (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) block.getState();
                String line0 = sign.getLine(0);
                if (line0.equalsIgnoreCase("[" + firstline + "]")) {
                    event.blockList().remove(block);
                    if (block.getType() == Material.SIGN_POST) {
                        Block blockdown = block.getRelative(BlockFace.DOWN, 1);
                        event.blockList().remove(blockdown);
                    }
                    if (block.getType() == Material.WALL_SIGN) {
                        Block blockbehind = null;
                        byte data = block.getData();
                        if (data == 4) {
                            blockbehind = block.getRelative(BlockFace.SOUTH, 1);
                        }
                        if (data == 5) {
                            blockbehind = block.getRelative(BlockFace.NORTH, 1);
                        }
                        if (data == 3) {
                            blockbehind = block.getRelative(BlockFace.EAST, 1);
                        }
                        if (data == 2) {
                            blockbehind = block.getRelative(BlockFace.WEST, 1);
                        }
                        event.blockList().remove(blockbehind);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerBreakSign(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        if (blockType == Material.WALL_SIGN || blockType == Material.SIGN_POST) {
            // check the text on the sign
            String firstline = plugin.getConfig().getString("firstline");
            Sign sign = (Sign) block.getState();
            String line0 = sign.getLine(0);
            String line1 = sign.getLine(1);
            String line2 = sign.getLine(2);
            String line3 = sign.getLine(3);
            if (line0.equalsIgnoreCase("[" + firstline + "]")) {
                event.setCancelled(true);
                sign.setLine(0, line0);
                sign.setLine(1, line1);
                sign.setLine(2, line2);
                sign.setLine(3, line3);
                sign.update();
                player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.use_command"));
            }
        } else {
            // check if breaking block underneath or behind sign
            for (BlockFace bf : faces) {
                Block faceBlock = block.getRelative(bf);
                Material faceBlockType = faceBlock.getType();
                if (faceBlockType == Material.WALL_SIGN) {
                    Sign sign = (Sign) faceBlock.getState();
                    MaterialData m = sign.getData();
                    BlockFace attachedFace;
                    BlockFace chkFace = null;
                    if (m instanceof Attachable) {
                        attachedFace = ((Attachable) m).getAttachedFace();
                        // get opposite face
                        switch (attachedFace) {
                            case EAST:
                                chkFace = BlockFace.WEST;
                                break;
                            case NORTH:
                                chkFace = BlockFace.SOUTH;
                                break;
                            case WEST:
                                chkFace = BlockFace.EAST;
                                break;
                            default:
                                chkFace = BlockFace.NORTH;
                                break;
                        }
                    }
                    if (bf.equals(chkFace)) {
                        xpkSign(faceBlock, event, player);
                    }
                }
                if (bf.equals(BlockFace.UP) && faceBlockType == Material.SIGN_POST) {
                    xpkSign(faceBlock, event, player);
                }
            }
        }
    }

    private void xpkSign(Block b, BlockBreakEvent e, Player p) {
        String firstline = plugin.getConfig().getString("firstline");
        Sign sign = (Sign) b.getState();
        String line0 = sign.getLine(0);
        String line1 = sign.getLine(1);
        String line2 = sign.getLine(2);
        String line3 = sign.getLine(3);
        if (line0.equalsIgnoreCase("[" + firstline + "]")) {
            e.setCancelled(true);
            sign.setLine(0, line0);
            sign.setLine(1, line1);
            sign.setLine(2, line2);
            sign.setLine(3, line3);
            sign.update();
            p.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.no_grief"));
        }
    }


    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (hasXPKSign(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (event.isSticky() && checkXPKSign(event.getRetractLocation().getBlock())) {
            event.setCancelled(true);
        }
    }

    public boolean hasXPKSign(List<Block> blocks) {
        for (Block b : blocks) {
            if (checkXPKSign(b)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkXPKSign(Block b) {
        if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
            return isXPKSign(b);
        } else {
            // check if there is an XPKeeper sign attached to the block
            if (b.getRelative(BlockFace.UP).getType() == Material.SIGN_POST) {
                return isXPKSign(b.getRelative(BlockFace.UP));
            }
            for (BlockFace bf : faces2) {
                if (b.getRelative(bf).getType() == Material.WALL_SIGN) {
                    return isXPKSign(b.getRelative(bf));
                }
            }
            return false;
        }
    }

    private boolean isXPKSign(Block b) {
        Sign s = (Sign) b.getState();
        String line = s.getLine(0);
        if (line.equalsIgnoreCase("[" + plugin.getConfig().getString("firstline") + "]")) {
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void onPlayerSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack is = player.getItemInHand();
        Material inhand = is.getType();
        String playerNameStr = player.getName();
        Block block = event.getClickedBlock();
        if (block != null) {
            String world = block.getLocation().getWorld().getName();
            Material blockType = block.getType();
            Action action = event.getAction();
            if (blockType == Material.WALL_SIGN || blockType == Material.SIGN_POST) {
                // check the text on the sign
                String firstline = plugin.getConfig().getString("firstline");
                Sign sign = (Sign) block.getState();
                String line0 = sign.getLine(0);
                String line1 = sign.getLine(1);
                // check name length
                String sign_str = playerNameStr;
                if (playerNameStr.length() > 15) {
                    sign_str = playerNameStr.substring(0, 15);
                }
                if (line0.equalsIgnoreCase("[" + firstline + "]")) {
                    if (plugin.trackPlayers.containsKey(playerNameStr) && line1.equals(sign_str)) {
                        plugin.trackPlayers.remove(playerNameStr);
                        // set the sign block to AIR and delete the XPKeeper data
                        block.setType(Material.AIR);
                        // drop a sign
                        Location l = block.getLocation();
                        World w = l.getWorld();
                        w.dropItemNaturally(l, new ItemStack(323, 1));
                        // return any kept XP
                        int keptXP = plugin.getKeptXP(playerNameStr, world);
                        new XPKCalculator(player).changeExp(keptXP);
                        // remove database record
                        plugin.delKeptXP(playerNameStr, world);
                        player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.removed"));
                    } else {
                        if (plugin.getConfig().getBoolean("must_use_fist") && inhand != Material.AIR) {
                            player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.use_fist"));
                        } else {
                            if (line1.equals(sign_str)) {
                                XPKCalculator xpkc = new XPKCalculator(player);
                                // get players XP
                                int xp = xpkc.getCurrentExp();
                                if (action == Action.LEFT_CLICK_BLOCK) {
                                    // deposit XP
                                    if (line1.equals(sign_str)) {
                                        // sign is set up so update the amount kept
                                        int keptXP = plugin.getKeptXP(playerNameStr, world);
                                        //int keptLevel = plugin.getKeptLevel(playerNameStr, world);
                                        int newXPamount = xp + keptXP;
                                        int setxp = 0;
                                        int newLevel = xpkc.getLevelForExp(newXPamount);
                                        if (plugin.getConfig().getBoolean("set_limits")) {
                                            List<Double> limits = plugin.getConfig().getDoubleList("limits");
                                            double l = 0;
                                            for (Double d : limits) {
                                                if (!player.hasPermission("xpkeeper.limit.bypass") && player.hasPermission("xpkeeper.limit." + d)) {
                                                    l = d;
                                                    break;
                                                }
                                            }
                                            if (l != 0 && (newLevel + 1) > (int) l) {
                                                player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + "That amount would take you over your maximum deposit level, depositing as much as we can.");
                                                newXPamount = xpkc.getXpForLevel((int) l);
                                                setxp = (xp + keptXP) - newXPamount;
                                            }
                                        }
                                        plugin.setKeptXP(newXPamount, playerNameStr, world);
                                        // calculate level and update the sign
                                        int level = xpkc.getLevelForExp(newXPamount);
                                        int levelxp = xpkc.getXpForLevel(level);
                                        int leftoverxp = newXPamount - levelxp;
                                        sign.setLine(2, "Level: " + level);
                                        sign.setLine(3, "XP: " + leftoverxp);
                                        sign.update();
                                        // remove XP from player
                                        xpkc.setExp(setxp);
                                        player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + String.format(plugin.getConfig().getString("messages.deposit"), (xp - setxp), level));
                                    }
                                }
                                if (action == Action.RIGHT_CLICK_BLOCK) {
                                    // get withdrawal amount - 0 = all, 5 = 5 levels
                                    int withdrawAmount = plugin.getConfig().getInt("withdraw");
                                    int keptXP = plugin.getKeptXP(playerNameStr, world);
                                    if (withdrawAmount == 0 || player.isSneaking()) {
                                        // withdraw XP
                                        xpkc.changeExp(keptXP);
                                        plugin.setKeptXP(0, playerNameStr, world);
                                        // update the sign
                                        sign.setLine(2, "Level: 0");
                                        sign.setLine(3, "XP: 0");
                                        player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.withdraw_all"));
                                    } else {
                                        int levelXP = xpkc.getXpForLevel(xpkc.getLevelForExp(xp) + withdrawAmount) - xp;
                                        if (keptXP > levelXP) {
                                            // calculate remaining XP amount
                                            int remainingXP = keptXP - levelXP;
                                            plugin.setKeptXP(remainingXP, playerNameStr, world);
                                            int newLevel = xpkc.getLevelForExp(remainingXP);
                                            int newlevelxp = xpkc.getXpForLevel(newLevel);
                                            int leftoverxp = remainingXP - newlevelxp;
                                            sign.setLine(2, "Level: " + newLevel);
                                            sign.setLine(3, "XP: " + leftoverxp);
                                            xpkc.changeExp(levelXP);
                                            player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + String.format(plugin.getConfig().getString("messages.withdraw_some"), withdrawAmount));
                                        } else {
                                            xpkc.changeExp(keptXP);
                                            plugin.setKeptXP(0, playerNameStr, world);
                                            // update the sign
                                            sign.setLine(2, "Level: 0");
                                            sign.setLine(3, "XP: 0");
                                            player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.withdraw_all"));
                                        }
                                    }
                                    sign.update();
                                }
                            } else {
                                player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.not_your_sign"));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String world = event.getBlock().getWorld().getName();
        String playerNameStr = player.getName();
        String sign_str = playerNameStr;
        if (playerNameStr.length() > 15) {
            sign_str = playerNameStr.substring(0, 15);
        }
        String xpkLine = event.getLine(0);
        String firstline = "[" + plugin.getConfig().getString("firstline") + "]";
        if (firstline.equalsIgnoreCase(xpkLine)) {
            if (player.hasPermission("xpkeeper.use")) {
                // check to see if they have a keeper already
                int keptXP = plugin.getKeptXP(playerNameStr, world);
                if (keptXP < 0) {
                    plugin.insKeptXP(playerNameStr, world);
                    event.setLine(1, sign_str);
                    event.setLine(2, "Level: 0");
                    event.setLine(3, "XP: 0");
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.have_sign"));
                }
            } else {
                event.setLine(0, "");
                player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.no_perms_create"));
            }
        }
    }
}
