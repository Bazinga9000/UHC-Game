package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;

import java.awt.*;

public class TeamColors {
    private static final int numTeamColors = 64;
    private static final String[] teamColorCodes = {"c04040", "4040c0", "40c040", "c0c040", "c06b40", "6b40c0", "40c0c0", "6bc040",
            "c09640", "9640c0", "4096c0", "40c096", "96c040", "c040c0", "406bc0", "40c06b", "c04096", "c06bc0",
            "6bc06b", "c0966b", "c0406b", "c06b96", "6b96c0", "96c06b", "c06b6b", "964040", "6b6bc0", "6bc0c0",
            "c0c06b", "966b40", "966bc0", "6bc096", "969640", "406b40", "404096", "40966b", "6b9640", "406b6b",
            "96406b", "406b96", "409640", "40406b", "6b4040", "964096", "409696", "6b406b", "6b6b40", "6b4096",
            "c09696", "404040", "966b6b", "96c0c0", "c0c096", "6b6b6b", "6b9696", "c096c0", "96c096", "969696",
            "6b6b96", "6b966b", "9696c0", "c0c0c0", "966b96", "96966b"};

    private static Color[] teamColors = new Color[numTeamColors];

    static {
        for (int i = 0; i < numTeamColors; i++) {
            teamColors[i] = Color.decode("#" + teamColorCodes[i].toUpperCase());
        }
    }

    public static Color getTeamColor(int teamIndex) {
        if (teamIndex == 0) throw new IllegalArgumentException("Team index must be greater than 0.");
        return teamColors[teamIndex - 1];
    }

    public static ChatColor getTeamChatColor(int teamIndex) {
        return ChatColor.of(getTeamColor(teamIndex));
    }

    public static int getNumTeamColors() {
        return numTeamColors;
    }


}
