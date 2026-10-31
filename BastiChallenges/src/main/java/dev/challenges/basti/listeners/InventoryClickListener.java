package dev.challenges.basti.listeners;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Leitet Klicks an die GUIs weiter und sperrt jede Item-Bewegung in ihnen. */
public final class InventoryClickListener implements Listener {
    private final BastiPlugin plugin;

    public InventoryClickListener(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof BaseGui gui) {
            e.setCancelled(true);
            int raw = e.getRawSlot();
            if (raw >= 0 && raw < top.getSize() && e.getWhoClicked() instanceof Player) gui.onClick(e);
            return;
        }
        if (e.getWhoClicked() instanceof Player p) plugin.challenges().forceItem().checkLater(p);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof BaseGui) e.setCancelled(true);
    }
}
