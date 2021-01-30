package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.util.Arrays;
import static xyz.baz9k.UHCGame.util.Formats.*;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import xyz.baz9k.UHCGame.util.ColoredText;

import static java.time.temporal.ChronoUnit.FOREVER;

/**
 * {@link Enum} with properties for each stage of the game.
 * 
 * {@link #NOT_IN_GAME} should always be the first game stage
 * {@link #DEATHMATCH} should always be the last game stage
 */
public enum GameStage {
    NOT_IN_GAME,
    WB_STILL   (BarColor.RED,    Duration.ofMinutes(60), 1200, true,  ChatColor.RED + "Border Begins Shrinking",              "Let the games begin! Our players have been shuffled across the world! ", ChatColor.GREEN, BOLD, ITALIC, UNDERLINED),
    WB_1       (BarColor.BLUE,   Duration.ofMinutes(15), 25,   false, ChatColor.BLUE + "Border Stops Shrinking",              "The World Border has begun to shrink! ", ChatColor.RED, BOLD, ITALIC, UNDERLINED),
    WB_STOP    (BarColor.RED,    Duration.ofMinutes(5),  25,   true,  ChatColor.RED + "Border Begins Shrinking... Again.",    "The World Border has ground to a halt. ", ChatColor.AQUA, ITALIC),
    WB_2       (BarColor.BLUE,   Duration.ofMinutes(10), 3,    false, ChatColor.BLUE + "Border Stops Shrinking... Again",     "The World Border has resumed once more! ", ChatColor.RED, ITALIC),
    DM_WAIT    (BarColor.WHITE,  Duration.ofMinutes(5),  3,    true,  ChatColor.WHITE + "The Battle at the Top of the World", "The World Border has ground to a halt once again! ", ChatColor.DARK_AQUA, ITALIC),
    DEATHMATCH (BarColor.PURPLE, FOREVER.getDuration(),  20,   true,  ChatColor.DARK_PURPLE + "∞",                            "It is time. Let the Battle At The Top Of The World commence! ", ChatColor.BLUE, BOLD, ITALIC, UNDERLINED);
    
    private final BarColor bbClr;
    private final Duration dur;
    private final double wbSize;
    private final String bbTitle;
    private final String baseChatMsg;
    private final ChatColor clr;
    private final ChatColor[] fmt;

    private final boolean isWBInstant;
    /**
     * NOT_IN_GAME
     */
    private GameStage() { 
        bbClr = null;
        dur = null;
        wbSize = -1;
        bbTitle = null;
        baseChatMsg = null;
        isWBInstant = false;
        clr = ChatColor.WHITE;
        fmt = new ChatColor[0];
    }
    
    /**
     * @param bbClr Color of bossbar
     * @param dur Stage duration
     * @param wbDiameter Diameter of world border at this stage
     * @param isWBInstant If true, the world border is immediately set to the size provided by wbSize. If false, the world border gradually reaches wbSize by the end of the stage.
     * @param bbTitle Title of bossbar
     * @param baseChatMsg Chat message to send at the START of the stage
     */
    private GameStage(@NotNull BarColor bbClr, @NotNull Duration dur, int wbDiameter, boolean isWBInstant, @NotNull String bbTitle, @NotNull String baseChatMsg, ChatColor clr, ChatColor... fmt) {
        this.bbClr = bbClr;
        this.bbTitle = bbTitle;
        this.dur = dur;
        this.wbSize = wbDiameter;
        this.isWBInstant = isWBInstant;
        this.baseChatMsg = baseChatMsg;
        this.clr = clr;
        this.fmt = fmt;
    }

    @Nullable
    private static GameStage fromOrdinal(int i) {
        try {
            return values()[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
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
    public boolean isInstant() {
        return dur.isZero();
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
     * @return the next stage that has a non-zero duration; returns null if executed on {@link #NOT_IN_GAME} or {@link #DEATHMATCH}
     */
    private GameStage nextGradualStage() {
        if (this == NOT_IN_GAME) return null;

        return Arrays.stream(values())
                     .skip(ordinal() + 1)
                     .filter(gs -> !gs.isInstant())
                     .findFirst()
                     .orElse(null);
    }

    /**
     * Last stage before DM that has a non-zero duration; returns null if every stage has a 0 duration (Should not be possible normally).
     */
    private static GameStage lastGradualStage() {
        GameStage[] values = values();

        // iter every stage in reverse EXCEPT NOT_IN_GAME and DEATHMATCH
        for (int i = values.length - 2; i >= 1; i--) {
            GameStage gs = values[i];
            if (!gs.isInstant()) {
                return gs;
            };
        }
        return null;
    }


    /**
     * [warn prefix] [message]
     * <!> World border is shrinking in ---
     */
    private static final String warnPrefix = ChatColor.BLUE + "<!> ";

    // String.format(-, base, subject, radius, minutes)
    // subject = "It" or "The World Border"
    private static final String wbWillShrinkInstant = "%s%s will immediately shrink to ±%s in %s minutes! Watch out!";
    private static final String wbWillShrink = "%s%s will begin shrinking to ±%s in %s minutes!";

    private static final String wbJustShrinkInstant = "%2$s has immediately shrank to ±%3$s!";
    private static final String wbJustShrink = "%s%s will stop at ±%s in %s minutes.";

    private static final String dmWarn = "If the game does not end within %s minutes, I shall end it myself!";

    /**
     * Sends the linked message in chat.
     */
    public void sendMessage() {
        if (this == NOT_IN_GAME) return;

        /**
         * 
         * Gradual = duration > 0
         * Instant = duration = 0
         * 
         * 1. If the next gradual stage is a world border moving stage (WB_1, WB_2),
         *   a. If it is instant, warn that WB immediately shrinks next stage
         *   b. If it is not instant, warn that WB begins shrinking next stage 
         * 
         * 2. If the current stage is a world border moving stage,
         *   a. If it is instant, display that WB just shrunk.
         *   b. If it is not instant, display that WB is shrinking.
         * 
         * 3. If this is the last gradual stage, additionally display dmWarn.
         * 
         */

        String fmtStr = "%s";
        String subject = this == WB_STILL ? "The World Border" : "It";

        GameStage nextGrad = nextGradualStage();
        if (!nextGrad.isWBInstant) {
            if (nextGrad.isInstant()) {
                fmtStr = wbWillShrinkInstant;
            } else {
                fmtStr = wbWillShrink;
            }
        }
        if (!isWBInstant) {
            if (isInstant()) {
                fmtStr = wbJustShrinkInstant;
            } else {
                fmtStr = wbJustShrink;
            }
        }
        
        var s = new ColoredText(FormatRetention.ALL)
                .appendColored(warnPrefix)
                .append(String.format(fmtStr, baseChatMsg, subject, wbSize / 2, dur.toMinutes()), clr, fmt);

        if (this == lastGradualStage()) {
            s.append(dmWarn);
        }
        
        Bukkit.broadcast(s.toComponents());
    }
}
