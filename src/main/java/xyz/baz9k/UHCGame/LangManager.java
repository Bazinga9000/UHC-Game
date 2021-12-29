package xyz.baz9k.UHCGame;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.*;

import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;

public class LangManager {
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static Locale pluginLocale = DEFAULT_LOCALE;

    private final UHCGamePlugin plugin;
    private final TranslationRegistry reg;
    private final HashMap<Locale, YamlConfiguration> yamlFileCache = new HashMap<>();
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

    private YamlConfiguration langYaml(Locale l) {
        if (yamlFileCache.containsKey(l)) return yamlFileCache.get(l);

        InputStreamReader langResource;
        String filename = String.format("lang/%s.yml", l);
        try {
            langResource = new InputStreamReader(plugin.getResource(filename));
        } catch (NullPointerException e) {
            throw new Key("err.lang.missing_file").transErr(IllegalArgumentException.class, filename);
        }

        var yaml = YamlConfiguration.loadConfiguration(langResource);
        yamlFileCache.put(l, yaml);
        return yaml;
    }

    public YamlConfiguration langYaml() {
        return langYaml(getLocale());
    }

    private void loadLang(Locale l) {
        if (cfgCache.containsKey(l)) return; // already loaded
        reg.registerAll(pluginLocale, langEntries(pluginLocale));
    }

    private Map<String, MessageFormat> langEntries(Locale l) {
        if (cfgCache.containsKey(l)) return cfgCache.get(l);
        
        // get file in yml form
        var langCfg = langYaml(l);

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
