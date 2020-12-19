package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.awt.*;

public class UHCGame extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new TestListener(), this);
        ScoreboardManager sbm = getServer().getScoreboardManager();
        Scoreboard scoreboard = sbm.getMainScoreboard();

        //register teams with colors
        for (int i = 0; i < TeamColors.getNumTeamColors(); i++) {
            int index = i+1;
            String teamName = "uhc_" + index;
            Team t = scoreboard.getTeam(teamName);
            if (t == null) {
                t = scoreboard.registerNewTeam(teamName);
            }

            t.setAllowFriendlyFire(false);
            BaseComponent teamPrefix = new TextComponent("[" + index + "] ");
            teamPrefix.setBold(true);
            teamPrefix.setColor(TeamColors.getTeamChatColor(index));
            t.setPrefix(teamPrefix.toLegacyText());
        }

        //register spectator team
        Team t = scoreboard.getTeam("uhc_spectators");
        if (t == null) {
            t = scoreboard.registerNewTeam("uhc_spectators");
        }

        BaseComponent teamPrefix = new TextComponent("[S]");
        teamPrefix.setBold(true);
        teamPrefix.setColor(ChatColor.AQUA);
        t.setPrefix(teamPrefix.toLegacyText());

    }
}
