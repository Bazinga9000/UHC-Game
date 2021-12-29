package xyz.baz9k.UHCGame.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.awt.Color;
import java.util.Arrays;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public final class TeamDisplay {
    private TeamDisplay() {}

    private static final int[] teamColorCodes = {
        0x55FFFF, // spec color
        0xc04040, 0x4040c0, 0x40c040, 0xc0c040, 0xc06b40, 0x6b40c0, 0x40c0c0, 0x6bc040,
        0xc09640, 0x9640c0, 0x4096c0, 0x40c096, 0x96c040, 0xc040c0, 0x406bc0, 0x40c06b,
        0xc04096, 0xc06bc0, 0x6bc06b, 0xc0966b, 0xc0406b, 0xc06b96, 0x6b96c0, 0x96c06b,
        0xc06b6b, 0x964040, 0x6b6bc0, 0x6bc0c0, 0xc0c06b, 0x966b40, 0x966bc0, 0x6bc096,
        0x969640, 0x406b40, 0x404096, 0x40966b, 0x6b9640, 0x406b6b, 0x96406b, 0x406b96,
        0x409640, 0x40406b, 0x6b4040, 0x964096, 0x409696, 0x6b406b, 0x6b6b40, 0x6b4096,
        0xc09696, 0x404040, 0x966b6b, 0x96c0c0, 0xc0c096, 0x6b6b6b, 0x6b9696, 0xc096c0,
        0x96c096, 0x969696, 0x6b6b96, 0x6b966b, 0x9696c0, 0xc0c0c0, 0x966b96, 0x96966b
    };
    
    private static final Color[] teamColors = Arrays.stream(teamColorCodes)
        .mapToObj(Color::new)
        .toArray(Color[]::new);
    private static final int NUM_TEAM_COLORS = teamColors.length - 1;

    /**
     * Returns color of the team in type {@link java.awt.Color}.
     * @param t Team number (or 0)
     * @return the {@link java.awt.Color}
     */
    public static Color getColor(int t) {
        if (t < 0) {
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.team.display.index_must_pos");
        }
        if (t > NUM_TEAM_COLORS) {
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.team.display.index_must_under_max", NUM_TEAM_COLORS);
        }
        return teamColors[t];
    }

    /**
     * Returns color of the team in type {@link org.bukkit.Color}. Please refrain from using this.
     * @see #getColor
     * @param t Team number
     * @return Bukkit color
     */
    public static org.bukkit.Color getBukkitColor(int t) {
        Color clr = getColor(t);
        return org.bukkit.Color.fromRGB(clr.getRed(), clr.getGreen(), clr.getBlue());
    }

    /**
     * Returns color of the team in type {@link TextColor}.
     * @param t Team number
     * @return PaperMC {@link TextColor}
     */
    public static TextColor getTextColor(int t) {
        Color clr = getColor(t);
        return TextColor.color(clr.getRGB());
    }

    /**
     * Returns the formatting of the team.
     * This is the color & the decoration of the team name and prefix.
     * @param t Team number
     * @return PaperMC {@link Style}
     */
    public static Style getStyle(int t) {
        TextDecoration deco = t == 0 ? ITALIC : BOLD;
        return Style.style(getTextColor(t), deco);
    }

    /**
     * Returns the chat prefix of the team.
     * @param t Team number
     * @return {@link TextComponent} of the prefix
     */
    public static Component getPrefix(int t) {
        var prefix = trans("xyz.baz9k.uhc.team.prefix", t == 0 ? trans("xyz.baz9k.uhc.team.spectator_abbr") : t)
            .style(getStyle(t));
        return render(prefix);
    }

    /**
     * Returns the chat prefix of the team with a space appended.
     * @param t Team number
     * @return {@link TextComponent}
     */
    public static Component getPrefixWithSpace(int t) {
        return getPrefix(t).append(Component.space());
    }

    /**
     * Returns a player name with the prefix prepended.
     * @param t Team number
     * @param name Player name
     * @return {@link TextComponent}
     */
    public static Component prefixed(int t, String name) {
        return getPrefixWithSpace(t).append(Component.text(name, noDeco(WHITE)));
    }

    /**
     * Returns the name of the team with formatting.
     * @param t Team number
     * @return {@link TextComponent}
     */
    public static Component getName(int t) {
        String key = t == 0 ? "xyz.baz9k.uhc.team.spectator" : "xyz.baz9k.uhc.team.teamed";
        return trans(key, t).style(getStyle(t));
    }
}
