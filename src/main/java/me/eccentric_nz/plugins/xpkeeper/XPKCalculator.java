package me.eccentric_nz.plugins.xpkeeper;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static java.lang.StrictMath.sqrt;

/**
 * @author desht
 *
 * https://github.com/desht/dhutils/blob/master/src/main/java/me/desht/dhutils/ExperienceManager.java
 *
 * Adapted from ExperienceUtils code originally in ScrollingMenuSign.
 *
 * Credit to nisovin
 * (http://forums.bukkit.org/threads/experienceutils-make-giving-taking-exp-a-bit-more-intuitive.54450/#post-1067480)
 * for an implementation that avoids the problems of getTotalExperience(), which
 * doesn't work properly after a player has enchanted something.
 */
public class XPKCalculator {
    // this is to stop the lookup tables growing without control

    private static final int MAX_LEVEL = 100000;
    private static int xpDelta[] = {17};
    private static int xpTotal[] = {0};
    private final String playerName;

    static {
        // 25 is an arbitrary value for the initial table size; the actual value
        // isn't important since the tables are resized as needed.
        resizeLookupTables(25);
    }

    /**
     * Resize the XP lookup tables to a given size. If the lookup tables are
     * already this size or larger, this function does nothing.
     *
     * @param newSize The size of the lookup tables to precompute.
     */
    private static void resizeLookupTables(int newSize) {
        if (newSize <= xpTotal.length) {
            return;
        }

        int[] newDelta = new int[newSize];
        int[] newTotal = new int[newSize];

        // Copy computed values from previous array
        for (int i = 0; i < newSize && i < xpTotal.length; i++) {
            newDelta[i] = xpDelta[i];
            newTotal[i] = xpTotal[i];
        }

        int incr = xpDelta[xpTotal.length - 1];
        for (int i = xpTotal.length; i < newTotal.length; i++) {
            newTotal[i] = newTotal[i - 1] + incr;
            if (i >= 30) {
                incr += 7;
            } else if (i >= 16) {
                incr += 3;
            }
            newDelta[i] = incr;
        }

        xpDelta = newDelta;
        xpTotal = newTotal;
    }

    /**
     * Calculate the level that the given XP quantity corresponds to, without
     * using the lookup tables. This is needed if getLevelForXp() is called with
     * an XP quantity beyond the range of the existing lookup tables.
     *
     * Note: This algorithm overflows at 38347922 XP, or 3331 levels.
     *
     * @param xp
     * @return
     */
    private static int calculateLevelForExp(int xp) {
        if (xp <= 288) {
            return xp / 17;
        }
        if (xp <= 951) {
            return (59 + (int)sqrt(24 * xp - 5159)) / 6;
        }
        return (303 + (int)sqrt(56 * xp - 32511)) / 14;
    }

    /**
     * Get the level that the given amount of XP falls within.
     *
     * @param xp The amount to check for.
     * @return The level that a player with this amount total XP would be.
     */
    public static int getLevelForXp(int xp) {
        if (xp <= 0) {
            return 0;
        }
        if (xp > 38347922) {
            resizeLookupTables(3500);
            while (xp > xpTotal[xpTotal.length - 1]) {
                int newMax = xpTotal.length * 2;
                if (newMax > MAX_LEVEL) {
                    throw new IllegalArgumentException("Level for " + xp + " xp > hard max level " + MAX_LEVEL);
                }
                resizeLookupTables(newMax);
            }
        }
        if (xp > xpTotal[xpTotal.length - 1]) {
            int newMax = calculateLevelForExp(xp) * 2;
            if (newMax > MAX_LEVEL) {
                throw new IllegalArgumentException("Level for " + xp + " xp > hard max level " + MAX_LEVEL);
            }
            resizeLookupTables(newMax);
        }
        int pos = Arrays.binarySearch(xpTotal, xp);
        return pos < 0 ? -pos - 2 : pos;
    }

    /**
     * Return the total XP needed to be the given level.
     *
     * @param level The level to check for.
     * @return The amount of XP needed for the level.
     */
    public static int getXpForLevel(int level) {
        if (level > MAX_LEVEL) {
            throw new IllegalArgumentException("Level " + level + " > hard max level " + MAX_LEVEL);
        }

        if (level >= xpTotal.length) {
            resizeLookupTables(level * 2);
        }

        return xpTotal[level];
    }

    /**
     * Create a new XPKCalculator for the given player.
     *
     * @param player The player for this XPKCalculator object
     */
    public XPKCalculator(Player player) {
        this.playerName = player.getName();
        getPlayer();	// ensure it's a valid player name
    }

    /**
     * Get the Player associated with this XPKCalculator.
     *
     * @return	the Player object
     * @throws IllegalStateException if the player is no longer online
     */
    private Player getPlayer() {
        Player p = Bukkit.getPlayer(playerName);
        if (p == null) {
            throw new IllegalStateException("Player " + playerName + " is not online");
        }
        return p;
    }

    /**
     * Adjust the player's XP by the given amount in an intelligent fashion.
     * Works around some of the non-intuitive behaviour of the basic Bukkit
     * player.giveExp() method.
     *
     * @param amt Amount of XP to add or subtract
     */
    public void addXp(int amt) {
        setXp(getXp() + amt);
    }

    /**
     * Set the player's XP to the given value (constrained to be nonnegative).
     *
     * @param amt New XP value
     */
    public void setXp(int xp) {
        if (xp < 0) {
            xp = 0;
        }

        Player player = getPlayer();
        int curLvl = player.getLevel();
        int newLvl = getLevelForXp(xp);
        if (curLvl != newLvl) {
            player.setLevel(newLvl);
        }

        float pct = ((float) (xp - getXpForLevel(newLvl)) / (float) xpDelta[newLvl]);
        player.setExp(pct);
    }

    /**
     * Get the player's current XP total.
     *
     * @return the player's total XP
     */
    public int getXp() {
        Player player = getPlayer();
        int lvl = player.getLevel();
        int cur = getXpForLevel(lvl) + (int) Math.round(xpDelta[lvl] * player.getExp());
        return cur;
    }
}
