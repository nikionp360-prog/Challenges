package dev.challenges.basti.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Actionbar-Helfer. */
public final class ActionBar {
    private ActionBar() {}

    public static void send(Player p, Component c) {
        p.sendActionBar(c);
    }

    public static Component timer(TimerManager t) {
        NamedTextColor col = switch (t.getState()) {
            case RUNNING -> NamedTextColor.GREEN;
            case PAUSED -> NamedTextColor.YELLOW;
            case STOPPED -> NamedTextColor.GRAY;
        };
        return Component.text("Zeit: " + t.format(), col);
    }
}
