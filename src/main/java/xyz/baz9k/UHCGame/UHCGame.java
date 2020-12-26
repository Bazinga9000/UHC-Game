package xyz.baz9k.UHCGame;

import org.bukkit.plugin.java.JavaPlugin;

public class UHCGame extends JavaPlugin {
    private TeamManager teamManager;
    private ConfigManager cfgManager;
    private GameManager gameManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;

    @Override
    public void onEnable() {
        teamManager = new TeamManager();
        cfgManager = new ConfigManager();
        gameManager = new GameManager(this);
        hudManager = new HUDManager(this);
        bbManager = new BossbarManager(this);

        getServer().getPluginManager().registerEvents(gameManager, this);
        getServer().getPluginManager().registerEvents(hudManager, this);
        getServer().getPluginManager().registerEvents(cfgManager, this);

        Commands commands = new Commands(this);
        commands.registerAll();

        gameManager.loadManagerRefs();
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
    public ConfigManager getConfigManager() {
        return cfgManager;
    }
    public GameManager getGameManager() {
        return gameManager;
    }
    public HUDManager getHUDManager() {
        return hudManager;
    }
    public BossbarManager getBossbarManager() {
        return bbManager;
    }
}
