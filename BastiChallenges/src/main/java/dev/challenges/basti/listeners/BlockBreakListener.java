package dev.challenges.basti.listeners;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/** Block-Randomizer: Abbau droppt ein zufaelliges (aber pro Seed festes) Item. */
public final class BlockBreakListener implements Listener {
    private final BastiPlugin plugin;

    public BlockBreakListener(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!plugin.challenges().isRandomizerActive()) return;
        if (!plugin.getConfig().getBoolean("randomizer.block-drops", true)) return;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        Block b = e.getBlock();
        // Nur randomisieren, wenn der Block mit diesem Werkzeug ueberhaupt etwas droppen wuerde.
        if (b.getDrops(e.getPlayer().getInventory().getItemInMainHand(), e.getPlayer()).isEmpty()) return;

        e.setDropItems(false);
        Material out = plugin.challenges().randomizer().map(b.getType());
        Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().dropItemNaturally(loc, new ItemStack(out));
    }
}
