package dev.challenges.basti.challenges;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Liefert einen Abschnitt der Actionbar fuer einen Spieler (oder null, wenn nichts anzuzeigen ist). */
public interface HudProvider {
    Component hud(Player player);
}
