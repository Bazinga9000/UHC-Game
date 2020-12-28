package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.md_5.bungee.api.ChatColor;
import xyz.baz9k.UHCGame.util.Utils;

public class BossbarManager {
    private UHCGame plugin;
    private GameManager gameManager;

    private BossBar bossbar;
    private class BossbarStage {
        public @Nullable String title;
        public @NotNull BarColor color;
        public BossbarStage(String title, BarColor color) {
            this.title = title;
            this.color = color;
        }
    }

    private BossbarStage[] stages = {
        new BossbarStage(ChatColor.RED + "Border Begins Shrinking", BarColor.RED), // Still border
        new BossbarStage(ChatColor.BLUE + "Border Stops Shrinking", BarColor.BLUE), // Border 1
        new BossbarStage(ChatColor.RED + "Border Begins Shrinking... Again.", BarColor.RED), // Border stops
        new BossbarStage(ChatColor.BLUE + "Border Stops Shrinking... Again", BarColor.BLUE), // Border 2
        new BossbarStage(ChatColor.WHITE + "The Battle at the Top of the World", BarColor.WHITE), // Waiting until DM
        new BossbarStage(ChatColor.DARK_PURPLE + "∞", BarColor.PURPLE) // DEATHMATCH
    };

    public BossbarManager(UHCGame plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.bossbar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID);
    }

    public void enable() {
        for (Player p : plugin.getServer().getOnlinePlayers()) bossbar.addPlayer(p);
        bossbar.setVisible(true);
        updateBossbarStage();
    }

    public void enable(@NotNull Player p) {
        bossbar.addPlayer(p);
    }

    public void tick() {
        if (gameManager.isDeathmatch()) {
            bossbar.setProgress(1);
            bossbar.setTitle(getBBStage().title);
            return;
        }
        // update progress bar
        long remainingSecs = gameManager.getRemainingStageDuration().getSeconds(),
                 totalSecs = gameManager.getStageDuration().getSeconds();
                 
        bossbar.setProgress((double) remainingSecs / totalSecs);
        // change display title
        String display = getBBStage().title;
        display += " | ";
        display += Utils.getTimeString(remainingSecs);
        bossbar.setTitle(display);
    }

    public void disable() {
        bossbar.setVisible(false);
    }

    public void updateBossbarStage() {
        bossbar.setColor(getBBStage().color);
        tick();
    }

    @NotNull
    private BossbarStage getBBStage() {
        return stages[gameManager.getStage()];
    }
}
