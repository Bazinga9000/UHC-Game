package xyz.baz9k.UHCGame;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

public final class Recipes {
    private final UHCGamePlugin plugin;
    private final Set<NamespacedKey> registered = new HashSet<>();
    public Recipes(UHCGamePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        // register each @Command method
        try {
            for (Method m : Recipes.class.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(DeclaredRecipe.class)) continue;
                Bukkit.addRecipe((Recipe) m.invoke(this));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    public void unregisterAll() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    /**
     * Discover all registered recipes for a player
     * @param p Player to register recipes for
     */
    public void discoverFor(@NotNull Player p) {
        p.discoverRecipes(registered);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface DeclaredRecipe { }

    private @NotNull NamespacedKey key(String s) {
        NamespacedKey nkey = new NamespacedKey(plugin, s);
        registered.add(nkey);
        return nkey;
    }

    /* RECIPES */
    @DeclaredRecipe
    private Recipe godApple() {
        ShapedRecipe recipe = new ShapedRecipe(key("godapple"), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

        recipe.shape(
            "GGG",
            "GAG",
            "GGG"
        )
        .setIngredient('G', Material.GOLD_BLOCK)
        .setIngredient('A', Material.APPLE);

        return recipe;
    }
}
