package xyz.baz9k.UHCGame.menu;

import java.util.Map;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.menu.NodeItemStack.ItemProperties;

public class PresetNode extends Node {
    private final Map<String, Object> preset;
    
    public PresetNode(BranchNode parent, int parentSlot, String nodeName, ItemProperties<?> props, Map<String, Object> preset) {
        super(parent, parentSlot, nodeName, props);
        this.preset = preset;
    }

    @Override
    public boolean click(@NotNull Player p) {
        ValuedNode.cfgRoot.loadPreset(preset);
        return true;
    }
    
}
