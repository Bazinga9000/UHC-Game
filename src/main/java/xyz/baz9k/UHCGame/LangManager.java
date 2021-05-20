package xyz.baz9k.UHCGame;

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
    private final UHCGame plugin;
    private TranslationRegistry reg;
    private Locale locale = DEFAULT_LOCALE;
    private final Map<Locale, Map<String, MessageFormat>> cfgCache = new HashMap<>();

    private static final Locale DEFAULT_LOCALE = Locale.US;

    public LangManager(UHCGame plugin) {
        this.plugin = plugin;

        reg = TranslationRegistry.create(new NamespacedKey(plugin, "lang"));
        reg.defaultLocale(DEFAULT_LOCALE);
        
        GlobalTranslator.get().addSource(reg);

        loadLang(DEFAULT_LOCALE);
    }

    public Locale getLocale() { return locale; }

    public void setLang(Locale l) {
        loadLang(l);
        this.locale = l;
    }

    private void loadLang(Locale l) {
        if (cfgCache.containsKey(l)) return; // already loaded
        reg.registerAll(locale, langEntries(locale));
    }

    private Map<String, MessageFormat> langEntries(Locale l) {
        if (cfgCache.containsKey(l)) return cfgCache.get(l);
        
        // get file in yml form
        InputStreamReader langResource;
        try {
            langResource = new InputStreamReader(plugin.getResource(String.format("lang/%s.yml", l)));
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(String.format("Missing file %s", "lang/" + l + ".yml"));
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
