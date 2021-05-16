package xyz.baz9k.UHCGame.config;

import org.bukkit.Material;

/**
 * Provides all the data for one option of the options selectable in an {@link OptionValuedNode}.
 */
public class OptionData {
    // I'm not 100% sure value is necessary so I'm gonna keep it commented out for now
    private String name;
    private Material mat;
    // private String value;

    /**
     * @param name Display name of this option in lore
     * @param mat Material of item when this data is displayed
     */
    public OptionData(String name, Material mat /*, String value */) {
        this.name = name;
        this.mat = mat;
        // this.value = value;
    }

    public String getName() {
        return name;
    }
    public Material getMaterial() {
        return mat;
    }
    // public String getValue() {
    //     return value;
    // }
}
