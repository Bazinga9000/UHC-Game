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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

public final class Recipes {
    private UHCGame plugin;
    private Set<NamespacedKey> registered = new HashSet<>();
    public Recipes(UHCGame plugin) {
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
    public void unregister() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private static @interface DeclaredRecipe { }

    @NotNull
    private NamespacedKey key(String s) {
        NamespacedKey nkey = new NamespacedKey(plugin, s);
        registered.add(nkey);
        return nkey;
    }

    /* RECIPES */
    @DeclaredRecipe
    private Recipe godApple() {
        ShapedRecipe recipe = new ShapedRecipe(key("recipeGodApple"), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

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
