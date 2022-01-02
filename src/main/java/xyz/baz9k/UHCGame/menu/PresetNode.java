package xyz.baz9k.UHCGame.menu;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import xyz.baz9k.UHCGame.Kit;
import xyz.baz9k.UHCGame.menu.NodeItemStack.ExtraLore;
import xyz.baz9k.UHCGame.menu.NodeItemStack.ItemProperties;
import xyz.baz9k.UHCGame.util.Path;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;
// sorry

/**
 * {@link Node} that stores the preset and can set the game preset.
 */
public final class PresetNode extends Node {
    // Duration: {start} / {movement1} / {stop} / {movement2} / {dmwait}
    // World Border: {initial} → {border1} → {border2} / {deathmatch}
    // Global:
    // { - etc}
    // Teams:
    // { - etc}
    // Players:
    // { - etc}
    // Kit: {name @ index, or Custom}

    private final Map<?, ?> preset; // nested map of preset settings

    public PresetNode(BranchNode parent, int parentSlot, String nodeName, ItemProperties<?> props, Map<?, ?> preset) {
        super(parent, parentSlot, nodeName, props);
        this.preset = preset;

        props.extraLore(o -> {
            return new ExtraLore(new Key("menu.inv.config.presets.extra_lore"), 
                fromPreset("intervals.start"), fromPreset("intervals.movement1"), fromPreset("intervals.stop"), fromPreset("intervals.movement2"), fromPreset("intervals.dmwait"),
                fromPreset("wb_size.initial"), fromPreset("wb_size.border1"), fromPreset("wb_size.border2"), fromPreset("wb_size.deathmatch"),
                settingsText("global"),
                settingsText("team"),
                settingsText("player"),
                kitSettings()
            );
        });
    }

    @Override
    public boolean click(@NotNull Player p) {
        // load prefix to config
        for (var e : preset.entrySet()) {
            String path = (String) e.getKey();
            Object val = e.getValue();

            cfg.set(path, val);
        }

        return true;
    }
    
    /**
     * @param cfgKey config key
     * @return get node associated from the config key, if it exists
     */
    private Optional<Node> node(String cfgKey) {
        return ValuedNode.cfgRoot.findDescendant(cfgKey);
    }

    /**
     * @param path config key
     * @return get a format arg located at the specified config key
     */
    private String fromPreset(String path) {
        Optional<Object> o = new Path(path).traverse(preset);
        return formatted(path, o.orElseGet(() -> cfg.get(path)));
    }

    /**
     * Format an config value into a format argument based the formatting 
     * settings of the node associated with the specified config key
     * @param cfgKey config key 
     * @param o config value to format
     * @return format argument
     */
    @SuppressWarnings("unchecked")
    private String formatted(String cfgKey, Object o) {
        Optional<Node> maybeNode = node(cfgKey);

        if (maybeNode.isPresent()) {
            Node n = maybeNode.get();

            // VN & OVN have specialized formatting
            if (n instanceof OptionValuedNode ovn) {
                // option selected
                return String.valueOf(ovn.optDesc((int) o));
            }
            if (n instanceof ValuedNode vn) {
                // the format argument
                Object[] fmtArgs = ((ItemProperties<Object>) vn.itemProperties).format(o);
                if (fmtArgs.length == 1) {
                    return String.valueOf(fmtArgs[0]);
                }
                return Arrays.toString(fmtArgs);
            }
        }

        return String.valueOf(o);
    }

    /**
     * @param path the path
     * @return formatted settings for path. This doesn't evaluate deeply.
     */
    private String settingsText(String path) {
        Path p = new Path(path);
        Optional<Object> o = p.traverse(preset);

        String text;
        if (o.isPresent() && o.get() instanceof Map<?, ?> om) {
            text = om.entrySet().stream()
            .map(e -> {
                String cfgKey = Path.join(path, (String) e.getKey());
                Optional<Node> maybeNode = node(cfgKey);

                if (maybeNode.isPresent()) {
                    Node n = maybeNode.get();
                    String langKey = n.langKey();

                    // name of node
                    String k = renderString(NodeItemStack.nameFromID(langKey));
                    String v = formatted(cfgKey, e.getValue());
                    return String.format(" - %s: %s", k, v);
                }

                // if node doesn't exist, this cfg option must be invalid, return nothing
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));
        } else {
            text = String.valueOf(o.orElse(null));
        }

        return "\n" + text;
    }

    /**
     * Display kit
     */
    private Component kitSettings() {
        Object kit = fromPreset("kit");
        var kitParent = ValuedNode.cfgRoot.findDescendant("kit");

        String kitKey = "none";

        
        if (kit instanceof Kit) {
            kitKey = "custom";
        } else if (kit instanceof Integer ki && ki != 0) {
            if (kitParent.isPresent()) {
                var p = (BranchNode) kitParent.get();
                kitKey = p.getChildren()[ki].nodeName;
            }
        }

        return NodeItemStack.nameFromID("config.kit." + kitKey);
    }
}
