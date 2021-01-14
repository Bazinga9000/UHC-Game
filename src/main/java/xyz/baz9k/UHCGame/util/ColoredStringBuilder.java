package xyz.baz9k.UHCGame.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.*;

public class ColoredStringBuilder {
    private final StringBuilder sb = new StringBuilder();

    public ColoredStringBuilder append(String s, Color c) {
        ChatColor cc = ChatColor.of(c);
        append(s, cc);
        return this;
    }

    public ColoredStringBuilder append(String s, ChatColor... clrs) {
        for (ChatColor clr : clrs) {
            sb.append(clr.toString());
        }
        sb.append(s).append(ChatColor.RESET);
        return this;
    }

    public ColoredStringBuilder append(Object o) {
        sb.append(o.toString());
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
