package xyz.baz9k.UHCGame;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.Ench;
import xyz.baz9k.UHCGame.util.tag.BooleanTagType;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public final class Recipes {
    private final UHCGamePlugin plugin;
    private final Map<NamespacedKey, RecipeProperties> registered = new HashMap<>();
    public Recipes(UHCGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface DeclaredRecipe {
        EnableWhen enable();
    }

    private enum EnableWhen {
        ALWAYS (c -> true),
        PLAYER_DROPS_HEAD (c -> c.playerDropsIndex() == 2),
        PROX_TRACK (ConfigValues::proxTrack);

        private Predicate<ConfigValues> pred;
        private EnableWhen(Predicate<ConfigValues> pred) {
            this.pred = pred;
        }

        public boolean test(ConfigValues cfg) { return pred.test(cfg); }
    }

    public record RecipeProperties(Recipe r, EnableWhen pred) {
        public NamespacedKey key() {
            if (r instanceof Keyed kr) {
                return kr.getKey();
            }
            return null;
        }

        public boolean test(ConfigValues cfg) {
            return pred.test(cfg);
        }
    }

    public void registerAll() {
        // register each @DeclaredRecipe method
        Class<DeclaredRecipe> recipeCls = DeclaredRecipe.class;
        try {
            for (Method m : Recipes.class.getDeclaredMethods()) {
                if (m.isAnnotationPresent(recipeCls)) {
                    Recipe r = (Recipe) m.invoke(this);
                    EnableWhen enableWhen = m.getAnnotation(recipeCls).enable();
                    RecipeProperties props = new RecipeProperties(r, enableWhen);
                    NamespacedKey key = props.key();

                    if (key != null) {
                        registered.put(key, props);
                        Bukkit.addRecipe(r);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Discover all registered recipes for a player
     * @param p Player to register recipes for
     */
    public void discoverFor(@NotNull Player p) {
        p.discoverRecipes(registered.keySet());

        Iterable<Recipe> it = Bukkit::recipeIterator;
        for (Recipe r : it) {
            if (r instanceof Keyed kr) {
                p.discoverRecipe(kr.getKey());
            }
        }
    }

    public boolean isRecipeEnabled(Recipe r) {
        if (r instanceof Keyed kr) {
            NamespacedKey key = kr.getKey();
            if (registered.containsKey(key)) {
                RecipeProperties props = registered.get(key);
                var cfg = plugin.configValues();
                return props.test(cfg);
            };
        }
        return true;
    }

    private @NotNull NamespacedKey key(String s) {
        NamespacedKey nkey = new NamespacedKey(plugin, s);
        return nkey;
    }

    /* RECIPES */
    @DeclaredRecipe(enable = EnableWhen.ALWAYS)
    private Recipe godApple() {
        return new ShapedRecipe(key("godapple"), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE))
            .shape(
                "GGG",
                "GAG",
                "GGG"
            )
            .setIngredient('G', Material.GOLD_BLOCK)
            .setIngredient('A', Material.APPLE);
    }

    /* RECIPES */
    @DeclaredRecipe(enable = EnableWhen.PLAYER_DROPS_HEAD)
    private Recipe goldenHead() {
        NamespacedKey key = key("golden_head");
        ItemStack goldenHead = new ItemStack(Material.GOLDEN_APPLE);

        goldenHead.editMeta(m -> {
            m.displayName(render(new Key("item.golden_head.name").trans()));
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            m.addEnchant(Ench.SILK_TOUCH, 1, true);

            var container = m.getPersistentDataContainer();
            container.set(key, new BooleanTagType(), true);
        });

        return new ShapedRecipe(key, goldenHead)
            .shape(
                "GGG",
                "GPG",
                "GGG"
            )
            .setIngredient('G', Material.GOLD_INGOT)
            .setIngredient('P', Material.PLAYER_HEAD);
    }

    /* RECIPES */
    @DeclaredRecipe(enable = EnableWhen.PROX_TRACK)
    private Recipe proxCompass() {
        NamespacedKey key = key("prox_compass");
        ItemStack proxCompass = new ItemStack(Material.CLOCK);

        proxCompass.editMeta(m -> {
            m.displayName(render(new Key("item.prox_compass.name").trans()));
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            m.addEnchant(Ench.SILK_TOUCH, 1, true);
            
            var d = new Key("item.prox_compass.desc").transMultiline(noDeco(NamedTextColor.GRAY));
            m.lore(d);

            var container = m.getPersistentDataContainer();
            container.set(key, new BooleanTagType(), true);
        });

        return new ShapedRecipe(key, proxCompass)
            .shape(
                "GGG",
                "GCG",
                "GGG"
            )
            .setIngredient('G', Material.GOLD_INGOT)
            .setIngredient('C', Material.COMPASS);
    }
}
