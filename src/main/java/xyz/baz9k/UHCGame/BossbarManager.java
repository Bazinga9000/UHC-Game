package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class BossbarManager {
    private UHCGame plugin;
    private GameManager gameManager;

    private NamespacedKey key;
    private KeyedBossBar bossbar;
    private class BossbarStage {
        public String title;
        public BarColor color;
        public BossbarStage(String title, BarColor color) {
            this.title = title;
            this.color = color;
        }
    }
    private BossbarStage[] stages = {
        new BossbarStage(ChatColor.RED + "World Border Begins Shrinking", BarColor.RED), // (Ticks to end of) Still border
        new BossbarStage(ChatColor.BLUE + "World Border Stops Shrinking", BarColor.BLUE), // (Ticks to end of) Border 1
        new BossbarStage(ChatColor.RED + "World Border Begins Shrinking... Again.", BarColor.RED), // (Ticks to end of) Border stops
        new BossbarStage(ChatColor.WHITE + "The Battle at the Top of the World", BarColor.WHITE), // (Ticks to end of) Border 2
        new BossbarStage(ChatColor.DARK_PURPLE + "Heat Death of the Universe", BarColor.PURPLE) // (Ticks to end of) Waiting until DM
    };

    public BossbarManager(UHCGame plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        this.key = new NamespacedKey(plugin, "uhcbb");
        this.bossbar = Bukkit.getBossBar(key);
        if (bossbar == null) {
            this.bossbar = Bukkit.createBossBar(key, null, BarColor.WHITE, BarStyle.SOLID);
        }
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
        // update progress bar
        Duration elapsedDur = Duration.between(gameManager.getLStageInstant(), Instant.now());

        long elapsedSecs = elapsedDur.getSeconds();
        long totalSecs = gameManager.getCurrentStageDuration().getSeconds();
        bossbar.setProgress((double) elapsedSecs / totalSecs);
        // change display title
        String display = getBBStage().title;
        display += " | ";
        display += getTimeString(elapsedSecs);
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
