package xyz.baz9k.UHCGame;

import org.bukkit.GameRule;
import org.bukkit.World;

public final class Gamerules {
    public static void set(World w) {
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
        w.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
        w.setGameRule(GameRule.DISABLE_RAIDS, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        w.setGameRule(GameRule.DO_ENTITY_DROPS, true);
        w.setGameRule(GameRule.DO_FIRE_TICK, true);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_LIMITED_CRAFTING, false);
        w.setGameRule(GameRule.DO_MOB_LOOT, true);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, true);
        w.setGameRule(GameRule.DO_TILE_DROPS, true);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, true);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        w.setGameRule(GameRule.DROWNING_DAMAGE, true);
        w.setGameRule(GameRule.FALL_DAMAGE, true);
        w.setGameRule(GameRule.FIRE_DAMAGE, true);
        w.setGameRule(GameRule.FORGIVE_DEAD_PLAYERS, true);
        w.setGameRule(GameRule.KEEP_INVENTORY, false);
        w.setGameRule(GameRule.LOG_ADMIN_COMMANDS, true);
        w.setGameRule(GameRule.MAX_COMMAND_CHAIN_LENGTH, 65536);
        w.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24);
        w.setGameRule(GameRule.MOB_GRIEFING, true);
        w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
        w.setGameRule(GameRule.REDUCED_DEBUG_INFO, false);
        w.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
        w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
        w.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, true);
        w.setGameRule(GameRule.UNIVERSAL_ANGER, false);
    }
}
