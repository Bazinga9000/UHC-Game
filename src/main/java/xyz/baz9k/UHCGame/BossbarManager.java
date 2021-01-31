package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.util.Utils;

public class BossbarManager {
    private GameManager gameManager;
    private BossBar bossbar;

    public BossbarManager(UHCGame plugin) {
        this.gameManager = plugin.getGameManager();
        this.bossbar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID);
    }

    /**
     * On game start, this function runs to initialize the bossbar.
     */
    public void enable() {
        for (Player p : Bukkit.getOnlinePlayers()) bossbar.addPlayer(p);
        bossbar.setVisible(true);
        updateBossbarStage();
    }

    /**
     * On game end, this function runs to deactivate the bossbar.
     */
    public void disable() {
        bossbar.setVisible(false);
    }

    /**
     * If a player joins midgame, this function runs to display the bossbar to the joining player.
     * @param p
     */
    public void addPlayer(@NotNull Player p) {
        bossbar.addPlayer(p);
    }

    /**
     * This function runs every tick during the game.
     */
    public void tick() {
        if (gameManager.isDeathmatch()) {
            bossbar.setProgress(1);
            bossbar.setTitle(getBBTitle());
            return;
        }
        // update progress bar
        long remainingSecs = gameManager.getRemainingStageDuration().toSeconds(),
                 totalSecs = gameManager.getStageDuration().toSeconds();

        bossbar.setProgress((double) remainingSecs / totalSecs);
        // change display title
        String display = getBBTitle();
        display += " | ";
        display += Utils.getTimeString(remainingSecs);
        bossbar.setTitle(display);
    }

    /**
     * This function updates the bossbar when the stage increments.
     */
    public void updateBossbarStage() {
        if (getBBColor() == null) {
            // should only occur on NOT_IN_GAME
            bossbar.setColor(BarColor.WHITE);
            return;
        }
        bossbar.setColor(getBBColor());
        tick();
    }

    @Nullable
    private BarColor getBBColor() {
        return gameManager.getStage().getBBColor();
    }

    @Nullable
    private String getBBTitle() {
        return gameManager.getStage().getBBTitle();
    }
}
