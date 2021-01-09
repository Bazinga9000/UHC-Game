package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.md_5.bungee.api.ChatColor;

import static java.time.temporal.ChronoUnit.FOREVER;

/**
 * {@link Enum} with properties for each stage of the game.
 */
public enum GameStage {
    NOT_IN_GAME,
    WB_STILL   (BarColor.RED,    Duration.ofMinutes(60), 1200, true,  ChatColor.RED + "Border Begins Shrinking",              ""),
    WB_1       (BarColor.BLUE,   Duration.ofMinutes(15), 25,   false, ChatColor.BLUE + "Border Stops Shrinking",              ""),
    WB_STOP    (BarColor.RED,    Duration.ofMinutes(5),  25,   true,  ChatColor.RED + "Border Begins Shrinking... Again.",    ""),
    WB_2       (BarColor.BLUE,   Duration.ofMinutes(10), 3,    false, ChatColor.BLUE + "Border Stops Shrinking... Again",     ""),
    DM_WAIT    (BarColor.WHITE,  Duration.ofMinutes(5),  3,    true,  ChatColor.WHITE + "The Battle at the Top of the World", ""),
    DEATHMATCH (BarColor.PURPLE, FOREVER.getDuration(),  20,   true,  ChatColor.DARK_PURPLE + "∞",                            "");

    private static UHCGame plugin;

    private final BarColor bbClr;
    private final Duration dur;
    private final double wbSize;
    private final String bbTitle;
    private final String chatMessage;

    private final boolean isWBInstant;
    /**
     * NOT_IN_GAME
     */
    private GameStage() { 
        bbClr = null;
        dur = null;
        wbSize = -1;
        bbTitle = null;
        chatMessage = null;
        isWBInstant = false;
    }
    
    /**

     * @param bbClr Color of bossbar
     * @param dur Stage duration
     * @param wbDiameter Diameter of world border at this stage
     * @param isWBInstant If true, the world border is immediately set to the size provided by wbSize. If false, the world border gradually reaches wbSize by the end of the stage.
     * @param bbTitle Title of bossbar
     * @param chatMessage Chat message to send at the start of the stage
     */
    private GameStage(@NotNull BarColor bbClr, @NotNull Duration dur, int wbDiameter, boolean isWBInstant, @NotNull String bbTitle, @NotNull String chatMessage) {
        this.bbClr = bbClr;
        this.bbTitle = bbTitle;
        this.dur = dur;
        this.wbSize = wbDiameter;
        this.isWBInstant = isWBInstant;
        this.chatMessage = chatMessage;
    }
    
    /**
     * Sets the plugin. Can only be used once.
     * @param p
     */
    public static void setPlugin(UHCGame p) {
        if (plugin != null) return;
        plugin = p;
    }

    @Nullable
    private static GameStage fromOrdinal(int i) {
        return Arrays.stream(values())
                     .filter(s -> s.ordinal() == i)
                     .findFirst()
                     .orElse(null);
    }

    /**
     * 0 = First in game stage
     * @param i
     * @return GameStage connected to index
     */
    @Nullable
    public static GameStage fromIndex(int i) {
        if (i < 0) return null;
        return fromOrdinal(i + 1);
    }
    /**
     * @return the next stage in the GameStage sequence. If NOT_IN_GAME or DEATHMATCH, it will return null.
     */
    @Nullable
    public GameStage next() {
        if (this == NOT_IN_GAME) return null;
        
        return fromOrdinal(ordinal() + 1);
    }

    /* PROPERTIES */
    public BarColor getBBColor() {
        return bbClr;
    }

    public String getBBTitle() {
        return bbTitle;
    }

    public Duration getDuration() {
        return dur;
    }

    /**
     * @return the worldborder diameter of this stage
     */
    public double getWBSize() {
        return wbSize;
    }
    
    /**
     * Updates worlds to align with the stage's world border size.
     * @param worlds
     */
    public void applyWBSize(World... worlds) {
        if (this == NOT_IN_GAME) return;
        for (World w : worlds) {
            if (isWBInstant) {
                w.getWorldBorder().setSize(wbSize);
            } else {
                w.getWorldBorder().setSize(wbSize, dur.toSeconds());
            }
        }
    }

    /**
     * Sends the linked message in chat.
     */
    public void sendMessage() {
        if (this == NOT_IN_GAME) return;
        Bukkit.broadcastMessage(chatMessage);
    }
}
