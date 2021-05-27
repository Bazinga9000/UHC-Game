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
import net.kyori.adventure.text.TranslatableComponent;
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
    WB_STILL   (BossBar.Color.RED,    new ConfigDur("intervals.start"),     new ConfigWBSize("wb_size.initial"),    true,  trans("xyz.baz9k.uhc.bossbar.wb_still").color(NamedTextColor.RED),           Component.translatable("xyz.baz9k.uhc.chat.stage_base.wb_still", NamedTextColor.GREEN, BOLD)),
    WB_1       (BossBar.Color.BLUE,   new ConfigDur("intervals.movement1"), new ConfigWBSize("wb_size.border1"),    false, trans("xyz.baz9k.uhc.bossbar.wb_1").color(NamedTextColor.BLUE),              Component.translatable("xyz.baz9k.uhc.chat.stage_base.wb_1", NamedTextColor.RED, BOLD)),
    WB_STOP    (BossBar.Color.RED,    new ConfigDur("intervals.stop"),      new ConfigWBSize("wb_size.border1"),    true,  trans("xyz.baz9k.uhc.bossbar.wb_stop").color(NamedTextColor.RED),            Component.translatable("xyz.baz9k.uhc.chat.stage_base.wb_stop", NamedTextColor.AQUA)),
    WB_2       (BossBar.Color.BLUE,   new ConfigDur("intervals.movement2"), new ConfigWBSize("wb_size.border2"),    false, trans("xyz.baz9k.uhc.bossbar.wb_2").color(NamedTextColor.BLUE),              Component.translatable("xyz.baz9k.uhc.chat.stage_base.wb_2", NamedTextColor.RED)),
    DM_WAIT    (BossBar.Color.WHITE,  new ConfigDur("intervals.dmwait"),    new ConfigWBSize("wb_size.border2"),    true,  trans("xyz.baz9k.uhc.bossbar.dm_wait").color(NamedTextColor.WHITE),          Component.translatable("xyz.baz9k.uhc.chat.stage_base.dm_wait", NamedTextColor.DARK_AQUA)),
    DEATHMATCH (BossBar.Color.PURPLE, new ConfigDur(FOREVER.getDuration()), new ConfigWBSize("wb_size.deathmatch"), true,  trans("xyz.baz9k.uhc.bossbar.deathmatch").color(NamedTextColor.DARK_PURPLE), Component.translatable("xyz.baz9k.uhc.chat.stage_base.deathmatch", NamedTextColor.BLUE, BOLD));
    
    private static UHCGame plugin;
    public static void setPlugin(UHCGame plugin) { GameStage.plugin = plugin; }

    private record ConfigDur(String id, Duration def) {
        public ConfigDur(String id)    { this(id, null);  }
        public ConfigDur(Duration def) { this(null, def); }
        
        public Duration get() {
            var cfg = plugin.getConfig();
            if (id != null) return Duration.ofSeconds(cfg.getInt(id));
            return def;
        }
    }

    private record ConfigWBSize(String id, double def) {
        public ConfigWBSize(String id)    { this(id, -1);  }
        public ConfigWBSize(double def)   { this(null, def); }
        
        public double get() {
            var cfg = plugin.getConfig();
            if (id != null) return cfg.getDouble(id);
            return def;
        }
    }

    private final BossBar.Color bbClr;
    private final ConfigDur dur;
    private final ConfigWBSize wbSize;
    private final Component bbTitle;
    private final Component baseChatMsg;
    private final Style bodyStyle;

    private final boolean isWBInstant;
    /**
     * NOT_IN_GAME
     */
    private GameStage() { 
        this(BossBar.Color.WHITE, new ConfigDur(Duration.ZERO), new ConfigWBSize(-1), false, Component.empty(), Component.empty());
    }
    

    /**
     * @param bbClr Color of the boss bar
     * @param dur Duration of the stage (this can either be a config ID or a duration)
     * <p> If zero, the stage will be skipped in iteration
     * @param wbDiameter Diameter of the world border that this stage progresses to (this can either be a config ID or a double)
     * @param isWBInstant True if WB instantly jumps to this border at the start, false if progresses to WB by the end
     * @param bbTitle Title of the boss bar as a component (so, with colors and formatting)
     * @param baseChatMsg The base chat message, before color and additional warnings are added
     */
    private GameStage(@NotNull BossBar.Color bbClr, @NotNull ConfigDur dur, ConfigWBSize wbDiameter, boolean isWBInstant, @NotNull Component bbTitle, @NotNull Component baseChatMsg) {
        // bossbar
        this.bbClr = bbClr;
        this.bbTitle = bbTitle;

        // stage specific
        this.dur = dur;
        this.wbSize = wbDiameter;
        this.isWBInstant = isWBInstant;

        // message body
        this.baseChatMsg = baseChatMsg;
        this.bodyStyle = baseChatMsg.style();
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
     * @return the next stage that has a non-zero duration
     * <p> returns null if called on {@link #NOT_IN_GAME} or there is no further active stage
     */
    @Nullable
    public GameStage next() {
        if (this == NOT_IN_GAME) return null;
        
        return Arrays.stream(values())
                     .skip(ordinal() + 1)
                     .filter(GameStage::isActive)
                     .findFirst()
                     .orElse(null);
    }

    /**
     * Last stage before DM that has a non-zero duration
     * <p> returns null if every stage has a 0 duration (Should not be possible normally).
     */
    @Nullable
    private static GameStage last() {
        GameStage[] values = values();

        // iter every stage in reverse EXCEPT NOT_IN_GAME and DEATHMATCH
        for (int i = values.length - 2; i >= 1; i--) {
            GameStage gs = values[i];
            if (gs.isActive()) {
                return gs;
            };
        }
        return null;
    }

    /* PROPERTIES */
    public BossBar.Color getBBColor() {
        return bbClr;
    }

    public Component getBBTitle() {
        return bbTitle;
    }

    public Duration duration() {
        return dur.get();
    }
    
    public boolean isActive() {
        return !duration().isZero();
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
     * Gives a builder that starts a warning message by the game.
     * [warn prefix] [message]
     * <p>
     * {@literal <!> World border is shrinking in ---}
     */
    private static TextComponent.Builder getMessageBuilder() {
        return Component.text()
                        .append(
                            Component.text("<", TextColor.color(0xCFCFFF), BOLD),
                            Component.translatable("xyz.baz9k.uhc.chat.name", TextColor.color(0xA679FE), BOLD),
                            Component.text("> ", TextColor.color(0xCFCFFF), BOLD)
                        );
    }

    /**
     * Sends the linked message in chat.
     */
    public void sendMessage() {
        if (this == NOT_IN_GAME) return;
        if (this == DEATHMATCH) {
            Bukkit.getServer().sendMessage(getMessageBuilder().append(baseChatMsg));
            return;
        }

        TranslatableComponent situation = trans("xyz.baz9k.uhc.chat.warning.no_warn").style(bodyStyle);
        Component subject = trans(this == WB_STILL ? "xyz.baz9k.uhc.chat.wb.name" : "xyz.baz9k.uhc.chat.wb.pronoun");

        GameStage nextGrad = next();
        if (!nextGrad.isWBInstant) {
            situation = trans(nextGrad.isActive() ? "xyz.baz9k.uhc.chat.warning.wb_will_shrink" : "xyz.baz9k.uhc.chat.warning.wb_will_instant_shrink");
        }
        if (!isWBInstant) {
            situation = trans(isActive() ? "xyz.baz9k.uhc.chat.warning.wb_just_shrink" : "xyz.baz9k.uhc.chat.warning.wb_just_instant_shrink");
        }
        
        TextComponent.Builder s = getMessageBuilder();
        
        s.append(situation.style(bodyStyle).args(baseChatMsg, subject, Component.text(wbDiameter() / 2), getWordTime(duration())));
        if (this == last()) {
            s.append(Component.space()).append(trans("xyz.baz9k.uhc.chat.warning.dm_warn", getWordTime(duration())).style(bodyStyle));
        }
        
        Bukkit.getServer().sendMessage(s);
    }
}
