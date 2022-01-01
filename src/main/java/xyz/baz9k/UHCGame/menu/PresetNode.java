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

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;
// sorry

public class PresetNode extends Node {
    // Duration: {start} / {movement1} / {stop} / {movement2} / {dmwait}
    // World Border: {initial} → {border1} → {border2} / {deathmatch}
    // Global:
    // { - etc}
    // Teams:
    // { - etc}
    // Players:
    // { - etc}
    // Kit: {name @ index, or Custom}

    private final Map<?, ?> preset;
    static PresetNode defaultPreset;

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
        ValuedNode.cfgRoot.loadPreset(preset);
        return true;
    }
    
    private Optional<Node> node(String cfgKey) {
        return ValuedNode.cfgRoot.findDescendant(cfgKey);
    }

    private Object fromPreset(String path) {
        String[] nodeNames = path.split("\\.");
        Map<?, ?> map = preset;
        
        for (int i = 0; i < nodeNames.length - 1; i++) {
            String nodeName = nodeNames[i];
            var match = preset.get(nodeName);
            if (match != null && match instanceof Map<?, ?> m) {
                    map = m;
                    continue;
            }
            map = null;
            break;
        }

        Object o = null;
        if (map != null) o = map.get(nodeName);
        if (o != null) return formatted(path, o);
        return formatted(path, cfg.get(path));
    }

    @SuppressWarnings("unchecked")
    private String formatted(String cfgKey, Object o) {
        var maybeNode = node(cfgKey);

        if (maybeNode.isPresent()) {
            Node n = maybeNode.get();

            // value, one of:
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
            // just the raw value
            return String.valueOf(o);
        }

        return String.valueOf(o);
    }

    private String settingsText(String path) {
        Object o = preset.get(path);

        String text;
        if (o instanceof Map<?, ?> om) {
            text = om.entrySet().stream()
            .map(e -> {
                var cfgKey = path + "." + (String) e.getKey();
                var maybeNode = node(cfgKey);

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
            text = String.valueOf(o);
        }

        return "\n" + text;
    }

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
