package xyz.baz9k.UHCGame.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import static net.kyori.adventure.text.format.NamedTextColor.*;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.baz9k.UHCGame.PlayerState;

import static net.kyori.adventure.text.format.TextDecoration.*;

import java.awt.Color;
import java.util.Arrays;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public final class TeamDisplay {
    private TeamDisplay() {}

    private static final Color[] TEAM_COLORS;
    static {
        int[] teamColorCodes = {
            0xFFFFFF, // ignore this slot for convenience
            0xc04040, 0x4040c0, 0x40c040, 0xc0c040, 0xc06b40, 0x6b40c0, 0x40c0c0, 0x6bc040,
            0xc09640, 0x9640c0, 0x4096c0, 0x40c096, 0x96c040, 0xc040c0, 0x406bc0, 0x40c06b,
            0xc04096, 0xc06bc0, 0x6bc06b, 0xc0966b, 0xc0406b, 0xc06b96, 0x6b96c0, 0x96c06b,
            0xc06b6b, 0x964040, 0x6b6bc0, 0x6bc0c0, 0xc0c06b, 0x966b40, 0x966bc0, 0x6bc096,
            0x969640, 0x406b40, 0x404096, 0x40966b, 0x6b9640, 0x406b6b, 0x96406b, 0x406b96,
            0x409640, 0x40406b, 0x6b4040, 0x964096, 0x409696, 0x6b406b, 0x6b6b40, 0x6b4096,
            0xc09696, 0x404040, 0x966b6b, 0x96c0c0, 0xc0c096, 0x6b6b6b, 0x6b9696, 0xc096c0,
            0x96c096, 0x969696, 0x6b6b96, 0x6b966b, 0x9696c0, 0xc0c0c0, 0x966b96, 0x96966b
        };

        TEAM_COLORS = Arrays.stream(teamColorCodes)
            .mapToObj(Color::new)
            .toArray(Color[]::new);
    }
    private static final Color SPECTATOR_COLOR  = new Color(0x55FFFF);
    private static final Color UNASSIGNED_COLOR = new Color(0xFFFFFF);
    private static final int NUM_TEAM_COLORS = TEAM_COLORS.length - 1;

    private enum DisplayClassifier {
        COMBATANT, WILDCARD, UNASSIGNED, SPECTATOR;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        private static DisplayClassifier of(PlayerState s, int t) {
            return switch (s) {
                case COMBATANT_ALIVE, COMBATANT_DEAD -> {
                    if (t <= 0) yield WILDCARD;
                    yield COMBATANT;
                }
                case COMBATANT_UNASSIGNED -> UNASSIGNED;
                case SPECTATOR -> SPECTATOR;
            };
        }
    }

    /**
     * Returns color of the team in type {@link java.awt.Color}.
     * @param s Player state
     * @param t Team number (or 0)
     * @return the {@link java.awt.Color}
     */
    public static Color getColor(PlayerState s, int t) {
        return switch (DisplayClassifier.of(s, t)) {
            case COMBATANT -> {
                int ci = (t - 1) % NUM_TEAM_COLORS + 1;
                yield TEAM_COLORS[ci];
            }
            case WILDCARD -> TEAM_COLORS[0];
            case UNASSIGNED -> UNASSIGNED_COLOR;
            case SPECTATOR -> SPECTATOR_COLOR;

        };
    }

    /**
     * Returns color of the team in type {@link org.bukkit.Color}. Please refrain from using this.
     * @see #getColor
     * @param s Player state
     * @param t Team number
     * @return Bukkit color
     */
    public static org.bukkit.Color getBukkitColor(PlayerState s, int t) {
        Color clr = getColor(s, t);
        return org.bukkit.Color.fromRGB(clr.getRGB());
    }

    /**
     * Returns color of the team in type {@link TextColor}.
     * @param s Player state
     * @param t Team number
     * @return PaperMC {@link TextColor}
     */
    public static TextColor getTextColor(PlayerState s, int t) {
        Color clr = getColor(s, t);
        return TextColor.color(clr.getRGB());
    }

    /**
     * Returns the formatting of the team.
     * This is the color & the decoration of the team name and prefix.
     * @param s Player state
     * @param t Team number
     * @return PaperMC {@link Style}
     */
    public static Style getStyle(PlayerState s, int t) {
        TextDecoration deco = s.isAssignedCombatant() ? BOLD : ITALIC;
        return Style.style(getTextColor(s, t), deco);
    }

    /**
     * Returns the chat prefix of the team.
     * @param s Player state
     * @param t Team number
     * @return {@link TextComponent} of the prefix
     */
    public static Component getPrefix(PlayerState s, int t) {
        DisplayClassifier dc = DisplayClassifier.of(s, t);
        if (dc == DisplayClassifier.WILDCARD) return Component.empty();
        
        Component prefix = new Key("team.prefix.%s", dc)
            .trans(t)
            .style(getStyle(s, t));
        return render(prefix);
    }

    /**
     * @return get the prefix for dead chat
     */
    public static Component getDeadPrefix() {
        return new Key("team.prefix.dead")
            .trans()
            .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC));
    }

    /**
     * Returns the chat prefix of the team with a space appended.
     * @param s Player state
     * @param t Team number
     * @return {@link TextComponent}
     */
    public static Component getPrefixWithSpace(PlayerState s, int t) {
        DisplayClassifier dc = DisplayClassifier.of(s, t);
        if (dc == DisplayClassifier.WILDCARD) return Component.empty();

        return getPrefix(s, t).append(Component.space());
    }

    /**
     * Returns a player name with the prefix prepended.
     * @param s Player state
     * @param t Team number
     * @param name Player name
     * @return {@link TextComponent}
     */
    public static Component prefixed(PlayerState s, int t, String name) {
        return getPrefixWithSpace(s, t).append(Component.text(name, noDeco(WHITE)));
    }

    /**
     * Returns the name of the team with formatting.
     * @param s Player state
     * @param t Team number
     * @return {@link TextComponent}
     */
    public static Component getName(PlayerState s, int t) {
        return new Key("team.name.%s", DisplayClassifier.of(s, t))
            .trans(t)
            .style(getStyle(s, t));
    }
}
