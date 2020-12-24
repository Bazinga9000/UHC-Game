package xyz.baz9k.UHCGame;

import com.onarandombox.MultiverseCore.MultiverseCore;

import org.bukkit.plugin.Plugin;
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

    public MultiverseCore getMVCore() {
        Plugin plugin = getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (plugin instanceof MultiverseCore) return (MultiverseCore) plugin;
        throw new RuntimeException("Plugin Multiverse-Core is missing.");
    }
}
