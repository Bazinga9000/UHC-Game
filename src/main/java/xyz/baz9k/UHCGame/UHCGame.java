package xyz.baz9k.UHCGame;

import org.bukkit.plugin.java.JavaPlugin;

public class UHCGame extends JavaPlugin {
    private GameManager gameManager;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);
        getServer().getPluginManager().registerEvents(gameManager, this);
        Commands commands = new Commands(this);
        commands.registerAll();
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
