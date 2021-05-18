package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.util.Arrays;

import static xyz.baz9k.UHCGame.util.Utils.*;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.format.TextDecoration.*;

import static java.time.temporal.ChronoUnit.FOREVER;

/**
 * {@link Enum} with properties for each stage of the game.
 * <p>
 * {@link #NOT_IN_GAME} should always be the first game stage
 * <p>
 * {@link #DEATHMATCH} should always be the last game stage
 */
public enum GameStage {
    NOT_IN_GAME,
    WB_STILL   (BossBar.Color.RED,    new ConfigDur("intervals.start"),     new ConfigWBSize("wb_size.initial"), true,  Component.text("Border Begins Shrinking", NamedTextColor.RED),             "Let the games begin! Our players have been shuffled across the world! ", Style.style(NamedTextColor.GREEN, BOLD)),
    WB_1       (BossBar.Color.BLUE,   new ConfigDur("intervals.movement1"), new ConfigWBSize("wb_size.border1"), false, Component.text("Border Stops Shrinking", NamedTextColor.BLUE),             "The World Border has begun to shrink! ",                                 Style.style(NamedTextColor.RED, BOLD)),
    WB_STOP    (BossBar.Color.RED,    new ConfigDur("intervals.stop"),      new ConfigWBSize("wb_size.border1"), true,  Component.text("Border Begins Shrinking... Again.", NamedTextColor.RED),   "The World Border has ground to a halt. ",                                Style.style(NamedTextColor.AQUA)),
    WB_2       (BossBar.Color.BLUE,   new ConfigDur("intervals.movement2"), new ConfigWBSize("wb_size.border2"), false, Component.text("Border Stops Shrinking... Again", NamedTextColor.BLUE),    "The World Border has resumed once more! ",                               Style.style(NamedTextColor.RED)),
    DM_WAIT    (BossBar.Color.WHITE,  new ConfigDur("intervals.dmwait"),    new ConfigWBSize("wb_size.border2"), true,  Component.text("The Battle at the Top of the World", NamedTextColor.WHITE),"The World Border has ground to a halt once again! ",                     Style.style(NamedTextColor.DARK_AQUA)),
    DEATHMATCH (BossBar.Color.PURPLE, new ConfigDur(FOREVER.getDuration()), new ConfigWBSize("wb_size.dmwait"),  true,  Component.text("∞", NamedTextColor.DARK_PURPLE),                           "It is time. Let the Battle At The Top Of The World commence! ",          Style.style(NamedTextColor.BLUE, BOLD));
    
    private static UHCGame plugin;
    public static void setPlugin(UHCGame plugin) { GameStage.plugin = plugin; }

    private record ConfigDur(String id, Duration def) {
        public ConfigDur(String id)    { this(id, null);  }
        public ConfigDur(Duration def) { this(null, def); }

        ConfigDur {
            var cfg = plugin.getConfig();
            // this valid check is based off the assumption that the config values' types are not changed
            // (by changing the file and then reloading it)
            if (!cfg.isInt(id)) {
                throw new RuntimeException(String.format("ID %s either does not exist or does not match the correct value type.", id));
            }
        }

        public Duration get() {
            if (id != null) return Duration.ofSeconds(plugin.getConfig().getInt(id));
            return def;
        }
    }

    private record ConfigWBSize(String id, double def) {
        public ConfigWBSize(String id)    { this(id, -1);  }
        public ConfigWBSize(double def)   { this(null, def); }

        ConfigWBSize {
            var cfg = plugin.getConfig();
            // this valid check is based off the assumption that the config values' types are not changed
            // (by changing the file and then reloading it)
            if (!cfg.isDouble(id)) {
                throw new RuntimeException(String.format("ID %s either does not exist or does not match the correct value type.", id));
            }
        }

        public double get() {
            if (id != null) return plugin.getConfig().getDouble(id);
            return def;
        }
    }

    private final BossBar.Color bbClr;
    private final ConfigDur dur;
    private final ConfigWBSize wbSize;
    private final TextComponent bbTitle;
    private final String baseChatMsg;
    private final Style bodyStyle;

    private final boolean isWBInstant;
    /**
     * NOT_IN_GAME
     */
    private GameStage() { 
        this(BossBar.Color.WHITE, new ConfigDur(Duration.ZERO), new ConfigWBSize(-1), false, Component.empty(), "", Style.style(NamedTextColor.WHITE));
    }
    

    /**
     * @param bbClr Color of the boss bar
     * @param dur Duration of the stage
     * @param wbDiameter Diameter of the world border that this stage progresses to
     * @param isWBInstant True if WB instantly jumps to this border at the start, false if progresses to WB by the end
     * @param bbTitle Title of the boss bar as a component (so, with colors and formatting)
     * @param baseChatMsg The base chat message, before color and additional warnings are added
     * @param bodyClr Color of the body message
     * @param bodyFmt Formatting of the body message
     */
    private GameStage(@NotNull BossBar.Color bbClr, @NotNull ConfigDur dur, ConfigWBSize wbDiameter, boolean isWBInstant, @NotNull TextComponent bbTitle, @NotNull String baseChatMsg, Style bodyStyle) {
        // bossbar
        this.bbClr = bbClr;
        this.bbTitle = bbTitle;

        // stage specific
        this.dur = dur;
        this.wbSize = wbDiameter;
        this.isWBInstant = isWBInstant;

        // message body
        this.baseChatMsg = baseChatMsg;
        this.bodyStyle = bodyStyle;
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
     * @return GameStage connected to index. Null if not valid
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
    public BossBar.Color getBBColor() {
        return bbClr;
    }

    public TextComponent getBBTitle() {
        return bbTitle;
    }

    public Duration duration() {
        return dur.get();
    }
    
    public boolean isInstant() {
        return duration().isZero();
    }

    public double wbDiameter() {
        return wbSize.get();
    }

    public double wbRadius() {
        return wbDiameter() / 2;
    }
    
    /**
     * Updates worlds to align with the stage's world border size.
     * @param worlds
     */
    public void applyWBSize(World... worlds) {
        if (this == NOT_IN_GAME) return;
        for (World w : worlds) {
            if (isWBInstant) {
                w.getWorldBorder().setSize(wbDiameter());
            } else {
                w.getWorldBorder().setSize(wbDiameter(), duration().toSeconds());
            }
        }
    }

    /**
     * @return the next stage that has a non-zero duration; returns null if executed on {@link #NOT_IN_GAME} or {@link #DEATHMATCH}
     */
    @Nullable
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
    @Nullable
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
     * Gives a builder that starts a warning message by the game.
     * [warn prefix] [message]
     * <p>
     * {@literal <!> World border is shrinking in ---}
     */
    private static TextComponent.Builder getMessageBuilder() {
        return Component.text()
                        .append(
                            Component.text("<", TextColor.color(0xCFCFFF), BOLD),
                            Component.text("The Boxless One", TextColor.color(0xA679FE), BOLD),
                            Component.text("> ", TextColor.color(0xCFCFFF), BOLD)
                        );
    }

    // String.format(-, base, subject, radius, duration)
    // subject = "It" or "The World Border"
    private static final String wbWillShrinkInstant = "%s%s will immediately shrink to ±%s in %s! Watch out!";
    private static final String wbWillShrink = "%s%s will begin shrinking to ±%s in %s!";

    private static final String wbJustShrinkInstant = "%2$s has immediately shrank to ±%3$s!";
    private static final String wbJustShrink = "%s%s will stop at ±%s in %s.";

    // String.format(-, duration)
    private static final String dmWarn = "If the game does not end within %s, I shall end it myself!";

    /**
     * Sends the linked message in chat.
     */
    public void sendMessage() {
        if (this == NOT_IN_GAME) return;
        if (this == DEATHMATCH) {
            Bukkit.getServer().sendMessage(getMessageBuilder().append(Component.text(baseChatMsg, bodyStyle)));
            return;
        }
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
        
        var s = getMessageBuilder()
                .append(Component.text(String.format(fmtStr, baseChatMsg, subject, wbDiameter() / 2, getWordTimeString(duration())), bodyStyle));

        if (this == lastGradualStage()) {
            s.append(Component.text(String.format(dmWarn, getWordTimeString(duration())), bodyStyle));
        }
        
        Bukkit.getServer().sendMessage(s);
    }
}
