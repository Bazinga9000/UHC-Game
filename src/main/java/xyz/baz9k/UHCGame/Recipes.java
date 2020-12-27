package xyz.baz9k.UHCGame;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

public final class Recipes {
    private UHCGame plugin;
    private List<NamespacedKey> registered = new ArrayList<>();
    public Recipes(UHCGame plugin) {
        this.plugin = plugin;
    }

    @NotNull
    private ShapedRecipe newShapedRecipe(@NotNull String key, @NotNull ItemStack result) {
        NamespacedKey nkey = new NamespacedKey(plugin, key);
        ShapedRecipe recipe = new ShapedRecipe(nkey, result);
        registered.add(nkey);
        return recipe;
    }
    @NotNull
    private ShapelessRecipe newShapelessRecipe(@NotNull String key, @NotNull ItemStack result) {
        NamespacedKey nkey = new NamespacedKey(plugin, key);
        ShapelessRecipe recipe = new ShapelessRecipe(nkey, result);
        registered.add(nkey);
        return recipe;
    }
    public void register() {
        Bukkit.addRecipe(godApple());
    }
    public void unregister() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }


    /* RECIPES */
    private Recipe godApple() {
        ShapedRecipe recipe = newShapedRecipe("recipeGodApple", new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

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
