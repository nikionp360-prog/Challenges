package dev.challenges.basti.listeners;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/** Crafting-Randomizer + sofortige Force-Item-Pruefung nach dem Craften. */
public final class CraftListener implements Listener {
    private final BastiPlugin plugin;

    public CraftListener(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        if (!plugin.challenges().isRandomizerActive()) return;
        if (!plugin.getConfig().getBoolean("randomizer.crafting", true)) return;
        Recipe r = e.getRecipe();
        if (r == null) return;

        ItemStack vanilla = r.getResult();
        Material out = plugin.challenges().randomizer().map(vanilla.getType());
        int amount = Math.max(1, Math.min(vanilla.getAmount(), out.getMaxStackSize()));
        e.getInventory().setResult(new ItemStack(out, amount));
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) plugin.challenges().forceItem().checkLater(p);
    }
}
