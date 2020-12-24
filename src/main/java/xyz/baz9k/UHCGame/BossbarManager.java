package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class BossbarManager {
    private UHCGame plugin;
    private GameManager gameManager;

    private BossBar bossbar;
    private class BossbarStage {
        public String title;
        public BarColor color;
        public BossbarStage(String title, BarColor color) {
            this.title = title;
            this.color = color;
        }
    }
    private BossbarStage[] stages = {
        new BossbarStage(ChatColor.RED + "Border Begins Shrinking", BarColor.RED), // Still border
        new BossbarStage(ChatColor.BLUE + "Border Stops Shrinking", BarColor.BLUE), // Border 1
        new BossbarStage(ChatColor.RED + "Border Begins Shrinking... Again.", BarColor.RED), // Border stops
        new BossbarStage(ChatColor.BLUE + "Border Stops Shrinking... Again", BarColor.RED), // Border 2
        new BossbarStage(ChatColor.WHITE + "The Battle at the Top of the World", BarColor.WHITE), // Waiting until DM
        new BossbarStage(ChatColor.DARK_PURPLE + "∞", BarColor.PURPLE) // DEATHMATCH
    };

    public BossbarManager(UHCGame plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.bossbar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID);
    }
    private String getTimeString(long s) {
        if (s < 3600) return String.format("%02d:%02d", s / 60, (s % 60));
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }
    public void enable() {
        for (Player p : plugin.getServer().getOnlinePlayers()) bossbar.addPlayer(p);
        bossbar.setVisible(true);
    }
    public void tick() {
        if (getBBStage() == stages[stages.length - 1]) {
            bossbar.setProgress(1);
            bossbar.setTitle(getBBStage().title);
            return;
        }
        // update progress bar
        Duration stageDuration = gameManager.getCurrentStageDuration();
        Duration remainingDur = Duration.between(Instant.now(), gameManager.getLStageInstant().plus(stageDuration));

        long remainingSecs = remainingDur.getSeconds();
        long totalSecs = stageDuration.getSeconds();
        bossbar.setProgress((double) remainingSecs / totalSecs);
        // change display title
        String display = getBBStage().title;
        display += " | ";
        display += getTimeString(remainingSecs);
        bossbar.setTitle(display);
    }
    public void disable() {
        bossbar.setVisible(false);
    }

    public void updateBossbarStage() {
        bossbar.setColor(getBBStage().color);
        tick();
    }
    private BossbarStage getBBStage() {
        return stages[gameManager.getStage()];
    }
}
