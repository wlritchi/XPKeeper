package me.eccentric_nz.plugins.xpkeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
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

    List<BlockFace> adjacentFaces = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    public XpkInteractionListener(XPKeeper plugin) {
        this.plugin = plugin;
    }

    // Returns true if and only if this block represents an XPKeeper sign.
    private boolean isXpkSign(Block block) {
        String firstLine = "[" + plugin.getConfig().getString("firstline") + "]";
        if (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN) {
            Sign sign = (Sign)block.getState();
            if (firstLine.equalsIgnoreCase(sign.getLine(0))) {
                return true;
            }
        }
        return false;
    }

    // Returns true if and only if breaking this block results in the
    // destruction of an XPKeeper sign.
    private boolean supportsXpkSign(Block block) {
        return supportsXpkSign(Arrays.<Block>asList(block));
    }

    // Returns true if and onlf if there is at least one block in the collection
    // that cannot be broken without destroying an XPKeeper sign.
    //
    // Edge case: What if the destruction of two or more blocks causes this, but
    // neither causes it individually? Say, a vine hanging from another vine,
    // but also attcahed to a wall. We do not handle this edge case (and indeed
    // we do not handle vines at all) because we assume that such cases can only
    // occur on blocks against which players may not place signs.
    private boolean supportsXpkSign(List<Block> blocks) {
        Set<Block> checked = new HashSet<Block>();
        Queue<Block> blocksToCheck = new LinkedList<Block>();
        blocksToCheck.addAll(blocks);

        Block block;
        while ((block = blocksToCheck.poll()) != null) {
            if (checked.contains(block)) {
                continue;
            }
            checked.add(block);

            if (isXpkSign(block)) {
                return true;
            }

            for (BlockFace face : adjacentFaces) {
                Block adjacent = block.getRelative(face, 1);
                MaterialData md = adjacent.getState().getData();
                if (md instanceof Attachable) {
                    BlockFace attachedFace = ((Attachable) md).getAttachedFace();
                    switch (attachedFace) {
                        case UP:
                            if (face == BlockFace.DOWN) {
                                blocksToCheck.add(adjacent);
                            }
                            break;
                        case DOWN:
                            if (face == BlockFace.UP) {
                                blocksToCheck.add(adjacent);
                            }
                            break;
                        case NORTH:
                            if (face == BlockFace.SOUTH) {
                                blocksToCheck.add(adjacent);
                            }
                            break;
                        case EAST:
                            if (face == BlockFace.WEST) {
                                blocksToCheck.add(adjacent);
                            }
                            break;
                        case SOUTH:
                            if (face == BlockFace.NORTH) {
                                blocksToCheck.add(adjacent);
                            }
                            break;
                        case WEST:
                            if (face == BlockFace.EAST) {
                                blocksToCheck.add(adjacent);
                            }
                            break;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        List<Block> blockList = new ArrayList<Block>();
        blockList.addAll(event.blockList());

        for (Block block : blockList) {
            if (supportsXpkSign(block)) {
                event.blockList().remove(block);
            }
        }
    }

    @EventHandler
    public void onPlayerBreakSign(BlockBreakEvent event) {
        String firstline = "[" + plugin.getConfig().getString("firstline") + "]";
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        if (isXpkSign(block)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.use_command"));
        } else if (supportsXpkSign(block)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.no_grief"));
        }
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (supportsXpkSign(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (event.isSticky() && supportsXpkSign(event.getRetractLocation().getBlock())) {
            event.setCancelled(true);
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
            if (isXpkSign(block)) {
                // check the text on the sign
                Sign sign = (Sign) block.getState();
                String line1 = sign.getLine(1);
                // check name length
                String sign_str = playerNameStr;
                if (playerNameStr.length() > 15) {
                    sign_str = playerNameStr.substring(0, 15);
                }
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
                                int withdrawLevels = plugin.getConfig().getInt("withdraw");
                                int keptXP = plugin.getKeptXP(playerNameStr, world);
                                int targetXP = xpkc.getXpForLevel(xpkc.getLevelForExp(xp) + withdrawLevels) - xp;
                                if (keptXP <= targetXP || withdrawLevels == 0 || player.isSneaking()) {
                                    // withdraw XP
                                    xpkc.changeExp(keptXP);
                                    plugin.setKeptXP(0, playerNameStr, world);
                                    // update the sign
                                    sign.setLine(2, "Level: 0");
                                    sign.setLine(3, "XP: 0");
                                    player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + plugin.getConfig().getString("messages.withdraw_all"));
                                } else {
                                    // calculate remaining XP amount
                                    int remainingXP = keptXP - targetXP;
                                    plugin.setKeptXP(remainingXP, playerNameStr, world);
                                    int newLevel = xpkc.getLevelForExp(remainingXP);
                                    int newlevelxp = xpkc.getXpForLevel(newLevel);
                                    int leftoverxp = remainingXP - newlevelxp;
                                    sign.setLine(2, "Level: " + newLevel);
                                    sign.setLine(3, "XP: " + leftoverxp);
                                    xpkc.changeExp(targetXP);
                                    player.sendMessage(ChatColor.GRAY + "[XPKeeper] " + ChatColor.RESET + String.format(plugin.getConfig().getString("messages.withdraw_some"), withdrawLevels));
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

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String world = event.getBlock().getWorld().getName();
        String playerNameStr = player.getName();
        String sign_str = playerNameStr;
        if (playerNameStr.length() > 15) {
            sign_str = playerNameStr.substring(0, 15);
        }
        String firstline = "[" + plugin.getConfig().getString("firstline") + "]";
        if (firstline.equalsIgnoreCase(event.getLine(0))) {
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
