package dev.challenges.basti.listeners;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/** Friert Spieler ein, solange der Timer pausiert ist (optional, siehe config). */
public final class FreezeListener implements Listener {
    private final BastiPlugin plugin;

    public FreezeListener(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean frozen() {
        return plugin.timer().isFrozen();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        if (frozen() && e.hasChangedPosition()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (frozen()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (frozen()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (frozen()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (frozen() && e.getEntity() instanceof Player) e.setCancelled(true);
    }
}
