package xyz.baz9k.UHCGame;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;

public class LangManager {
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static Locale pluginLocale = DEFAULT_LOCALE;

    private final UHCGamePlugin plugin;
    private TranslationRegistry reg;
    private final Map<Locale, Map<String, MessageFormat>> cfgCache = new HashMap<>();


    public LangManager(UHCGamePlugin plugin) {
        this.plugin = plugin;

        reg = TranslationRegistry.create(new NamespacedKey(plugin, "lang"));
        reg.defaultLocale(DEFAULT_LOCALE);
        
        GlobalTranslator.get().addSource(reg);

        loadLang(DEFAULT_LOCALE);
    }

    public static Locale getLocale() { return pluginLocale; }

    public void setLang(Locale l) {
        loadLang(l);
        pluginLocale = l;
    }

    private void loadLang(Locale l) {
        if (cfgCache.containsKey(l)) return; // already loaded
        reg.registerAll(pluginLocale, langEntries(pluginLocale));
    }

    private Map<String, MessageFormat> langEntries(Locale l) {
        if (cfgCache.containsKey(l)) return cfgCache.get(l);
        
        // get file in yml form
        InputStreamReader langResource;
        String filename = String.format("lang/%s.yml", l);
        try {
            langResource = new InputStreamReader(plugin.getResource(filename));
        } catch (NullPointerException e) {
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.lang.missing_file", filename);
        }

        Configuration langCfg = YamlConfiguration.loadConfiguration(langResource);

        Map<String, MessageFormat> langEnts = new HashMap<>();
        for (var e : langCfg.getValues(true).entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            
            if (v instanceof String msg) {
                langEnts.put(k, new MessageFormat(msg));
            }
        }

        cfgCache.put(l, langEnts);
        return langEnts;
    }
}
