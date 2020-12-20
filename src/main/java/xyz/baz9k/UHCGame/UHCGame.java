package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import xyz.baz9k.UHCGame.util.TeamColors;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class UHCGame extends JavaPlugin {
    private GameManager gameManager;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);
        getServer().getPluginManager().registerEvents(gameManager, this);
        ScoreboardManager sbm = getServer().getScoreboardManager();
        Scoreboard scoreboard = sbm.getMainScoreboard();
        Commands commands = new Commands(this);
        commands.registerAll();

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

    public GameManager getGameManager() {
        return gameManager;
    }
}
