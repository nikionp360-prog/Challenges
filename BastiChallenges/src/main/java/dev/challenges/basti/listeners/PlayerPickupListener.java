package dev.challenges.basti.listeners;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

/** Force Item: Aufheben eines Items loest sofort die Pruefung aus. */
public final class PlayerPickupListener implements Listener {
    private final BastiPlugin plugin;

    public PlayerPickupListener(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) plugin.challenges().forceItem().checkLater(p);
    }
}
