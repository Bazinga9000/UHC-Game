package xyz.baz9k.UHCGame.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;

import java.awt.*;
import java.util.Arrays;

public class TeamDisplay {
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

    public static Color getColor(int t) {
        if (t < 0) {
            throw new IllegalArgumentException("Team index must be positive.");
        }
        if (t > NUM_TEAM_COLORS) {
            throw new IllegalArgumentException("Team index must be less than number of predefined team colors (" + NUM_TEAM_COLORS + ")");
        }
        return teamColors[t];
    }

    public static ChatColor getChatColor(int t) {
        return ChatColor.of(getColor(t));
    }

    public static int getNumTeamColors() {
        return NUM_TEAM_COLORS;
    }

    private static ColoredStringBuilder getPrefixBuilder(int t) {
        if (t == 0) {
            return ColoredStringBuilder.of("[S]", getChatColor(0)).italic(true);
        } else {
            return ColoredStringBuilder.of("[" + t + "]", getChatColor(t)).bold(true);
        }
    }
    public static String getPrefix(int t) {
        if (t == 0) {
            return getPrefixBuilder(t).toString();
        } else {
            return getPrefixBuilder(t).toString();
        }
    }

    public static String getPrefixWithSpace(int t) {
        return getPrefixBuilder(t).append(" ").toString();
    }

    public static String prefixed(int t, String name) {
        return getPrefixBuilder(t).append(" ").append(name).toString();
    }

    public static BaseComponent[] getName(int t) {
        if (t == 0) {
            return ColoredStringBuilder.of("Spectators", getChatColor(0)).italic(true).toComponents();
        } else {
            return ColoredStringBuilder.of("Team " + t, getChatColor(t)).bold(true).toComponents();
        }
    }
}
