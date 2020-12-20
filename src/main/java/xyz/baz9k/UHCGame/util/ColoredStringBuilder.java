package xyz.baz9k.UHCGame.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.*;

public class ColoredStringBuilder {
    private final StringBuilder sb = new StringBuilder();

    public void append(String s, Color c) {
        ChatColor cc = ChatColor.of(c);
        append(s, cc);
    }

    public void append(String s, ChatColor... c) {
        for (ChatColor cc : c) sb.append(cc.toString());
        sb.append(s).append(ChatColor.RESET);
    }

    public void append(Object o) {
        sb.append(o.toString());
    }

    public String toString() {
        return sb.toString();
    }
}
