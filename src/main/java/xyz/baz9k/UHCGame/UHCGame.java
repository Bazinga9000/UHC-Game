package xyz.baz9k.UHCGame;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UHCGame extends JavaPlugin {
    private DebugPrinter debugPrinter;
    private TeamManager teamManager;
    private GameManager gameManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private Recipes recipes;

    @Override
    public void onEnable() {
        debugPrinter = new DebugPrinter(this);
        teamManager = new TeamManager();
        gameManager = new GameManager(this);
        hudManager = new HUDManager(this);
        bbManager = new BossbarManager(this);
        recipes = new Recipes(this);

        Bukkit.getPluginManager().registerEvents(gameManager, this);
        Bukkit.getPluginManager().registerEvents(hudManager, this);

        Commands commands = new Commands(this);
        commands.registerAll();
        recipes.registerAll();

        gameManager.loadManagerRefs();
    }

    public DebugPrinter getDebugPrinter() {
        return debugPrinter;
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
    public Recipes getRecipes() {
        return recipes;
    }

    public MultiverseCore getMVCore() {
        return getPlugin(MultiverseCore.class);
    }

    public MVWorldManager getMVWorldManager() {
        return getMVCore().getMVWorldManager();
    }
}
