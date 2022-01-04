package xyz.baz9k.UHCGame;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Collections;
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

    private void loadLang(Locale l) {
        if (cfgCache.containsKey(l)) return; // already loaded
        reg.registerAll(l, langEntries(l));
    }

    private YamlConfiguration langYaml(Locale l) {
        return yamlFileCache.computeIfAbsent(l, locale -> {
            String filename = String.format("lang/%s.yml", l);
            InputStreamReader langResource;
            try {
                langResource = new InputStreamReader(plugin.getResource(filename));
            } catch (NullPointerException e) {
                throw new Key("err.lang.missing_file").transErr(IllegalArgumentException.class, filename);
            }
    
            return YamlConfiguration.loadConfiguration(langResource);
        });
    }

    public YamlConfiguration langYaml() {
        return langYaml(getLocale());
    }

    private Map<String, MessageFormat> langEntries(Locale l) {
        return cfgCache.computeIfAbsent(l, locale -> {
            // get file in yml form
            var langYaml = langYaml(l);
    
            Map<String, MessageFormat> entries = new HashMap<>();
            for (var e : langYaml.getValues(true).entrySet()) {
                if (e.getValue() instanceof String msg) {
                    entries.put(e.getKey(), new MessageFormat(msg));
                }
            }

            return Collections.unmodifiableMap(entries);
        });
    }
}
