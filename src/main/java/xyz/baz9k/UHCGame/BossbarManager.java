package xyz.baz9k.UHCGame;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import java.time.Duration;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class BossbarManager {
    private final GameManager gameManager;
    private final BossBar bossbar;

    public BossbarManager(UHCGamePlugin plugin) {
        this.gameManager = plugin.getGameManager();
        this.bossbar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    }

    /**
     * Shows the boss bar to some audience.
     * @param audience the audience
     */
    public void enable(Audience audience) {
        audience.showBossBar(bossbar);
        updateBossbarStage();
    }

    /**
     * Hides the boss bar to some audience.
     * @param audience the audience
     */
    public void disable(Audience audience) {
        audience.hideBossBar(bossbar);
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
        long remainingSecs = gameManager.getRemainingStageDuration()
            .map(Duration::toSeconds)
            .orElse(0L);
        long totalSecs = gameManager.getStageDuration().toSeconds();

        bossbar.progress((float) clamp(0, (double) remainingSecs / totalSecs, 1));
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
        bossbar.color(getBBColor());
        tick();
    }

    private BossBar.Color getBBColor() {
        return gameManager.getStage().getBBColor();
    }

    private Component getBBTitle() {
        return gameManager.getStage().getBBTitle();
    }
}
