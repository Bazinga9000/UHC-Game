package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class BossbarManager {
    private GameManager gameManager;
    private BossBar bossbar;

    public BossbarManager(UHCGame plugin) {
        this.gameManager = plugin.getGameManager();
        this.bossbar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    }

    private Audience onlinePlayers() {
        return Audience.audience(Bukkit.getOnlinePlayers());
    }
    
    /**
     * On game start, this function runs to initialize the bossbar.
     */
    public void enable() {
        onlinePlayers().showBossBar(bossbar);
        updateBossbarStage();
    }

    /**
     * On game end, this function runs to deactivate the bossbar.
     */
    public void disable() {
        onlinePlayers().hideBossBar(bossbar);
    }

    /**
     * If a player joins midgame, this function runs to display the bossbar to the joining player.
     * @param p
     */
    public void addPlayer(@NotNull Player p) {
        p.showBossBar(bossbar);
    }

    /**
     * This function runs every tick during the game.
     */
    public void tick() {
        if (gameManager.isDeathmatch()) {
            bossbar.progress(1);
            bossbar.name(getBBTitle());
            return;
        }
        // update progress bar
        long remainingSecs = gameManager.getRemainingStageDuration().toSeconds(),
                 totalSecs = gameManager.getStageDuration().toSeconds();

        bossbar.progress((float) remainingSecs / totalSecs);
        // change display title
        var display = getBBTitle()
            .append(Component.text(" | "))
            .append(Component.text(getTimeString(remainingSecs)));
            
        bossbar.name(display);
    }

    /**
     * This function updates the bossbar when the stage increments.
     */
    public void updateBossbarStage() {
        if (getBBColor() == null) {
            // should only occur on NOT_IN_GAME
            bossbar.color(BossBar.Color.WHITE);
            return;
        }
        bossbar.color(getBBColor());
        tick();
    }

    @Nullable
    private BossBar.Color getBBColor() {
        return gameManager.getStage().getBBColor();
    }

    @Nullable
    private TextComponent getBBTitle() {
        return gameManager.getStage().getBBTitle();
    }
}
