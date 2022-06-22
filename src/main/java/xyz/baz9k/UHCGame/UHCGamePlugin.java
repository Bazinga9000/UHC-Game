package xyz.baz9k.UHCGame;

import java.util.Locale;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.baz9k.UHCGame.util.Debug;

public class UHCGamePlugin extends JavaPlugin {
    private ConfigValues configValues;
    private TeamManager teamManager;
    private GameManager gameManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private LangManager langManager;
    private MenuManager menuManager;
    private WorldManager worldManager;
    private SpreadPlayersManager spreadPlayersManager;
    private SardinesManager sardinesManager;
    private Recipes recipes;

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(Kit.class);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        
        langManager = new LangManager(this);
        Debug.setLogger(getLogger());
        
        GameStage.setPlugin(this);
        configValues = new ConfigValues(this);
        teamManager = new TeamManager(this);
        gameManager = new GameManager(this);
        hudManager = new HUDManager(this);
        bbManager = new BossbarManager(this);
        worldManager = new WorldManager(this);
        menuManager = new MenuManager(this);
        spreadPlayersManager = new SpreadPlayersManager(this);
        sardinesManager = new SardinesManager(this);
        recipes = new Recipes(this);

        Bukkit.getPluginManager().registerEvents(gameManager, this);
        Bukkit.getPluginManager().registerEvents(hudManager, this);
        Bukkit.getPluginManager().registerEvents(menuManager, this);
        Bukkit.getPluginManager().registerEvents(sardinesManager, this);
        Bukkit.getPluginManager().registerEvents(worldManager, this);

        Commands commands = new Commands(this);
        commands.registerAll();
        recipes.registerAll();

        gameManager.loadManagerRefs();
    }

    @Override
    public void onDisable() {
        this.saveConfig();
    }
    
    public ConfigValues configValues() {
        return configValues;
    }
    public TeamManager getTeamManager() {
        return teamManager;
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
    public LangManager getLangManager() {
        return langManager;
    }
    public MenuManager getMenuManager() {
        return menuManager;
    }
    public WorldManager getWorldManager() {
        return worldManager;
    }
    public SpreadPlayersManager spreadPlayers() {
        return spreadPlayersManager;
    }
    public SardinesManager getSardinesManager() {
        return sardinesManager;
    }

    public Recipes getRecipes() {
        return recipes;
    }

    public MultiverseCore getMVCore() {
        return getPlugin(MultiverseCore.class);
    }

    public MVWorldManager getMVWorldManager() {
        return getMVCore().getMVWorldManager();
    }

    public static Locale getLocale() {
        return LangManager.getLocale();
    }
}
