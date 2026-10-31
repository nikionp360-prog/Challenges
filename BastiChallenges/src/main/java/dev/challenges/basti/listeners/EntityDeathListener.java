package dev.challenges.basti.listeners;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.challenges.MonsterArmyManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Mob-Kills (Force Mob, Army-Sammelphase, Arena-Mobs) sowie Spieler-Tod/-Respawn im Kampf. */
public final class EntityDeathListener implements Listener {
    private final BastiPlugin plugin;

    public EntityDeathListener(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player) return;
        MonsterArmyManager army = plugin.challenges().army();

        // 1) Mob aus der Army-Arena
        if (army.isArenaMob(dead)) {
            if (plugin.getConfig().getBoolean("army-battle.clear-drops", true)) {
                e.getDrops().clear();
                e.setDroppedExp(0);
            }
            army.onMobDeath(dead);
            return;
        }

        // 2) Kills durch Spieler: Force Mob + Army-Mob-Speicher
        Player killer = dead.getKiller();
        if (killer != null) {
            plugin.challenges().forceMob().onKill(killer, dead.getType());
            army.onKill(killer, dead.getType());
        }

        // 3) Mob-Drop-Randomizer
        if (plugin.challenges().isRandomizerActive()
                && plugin.getConfig().getBoolean("randomizer.mob-drops", false)
                && !e.getDrops().isEmpty()) {
            List<ItemStack> replaced = new ArrayList<>();
            for (ItemStack drop : e.getDrops()) {
                var out = plugin.challenges().randomizer().map(drop.getType());
                replaced.add(new ItemStack(out, Math.max(1, Math.min(drop.getAmount(), out.getMaxStackSize()))));
            }
            e.getDrops().clear();
            e.getDrops().addAll(replaced);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        MonsterArmyManager army = plugin.challenges().army();
        if (army.phase() != MonsterArmyManager.Phase.FIGHT) return;
        army.onPlayerDeath(p);
        if (plugin.getConfig().getBoolean("army-battle.keep-inventory-on-death", true)) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Location arena = plugin.challenges().army().respawnLocation(e.getPlayer());
        if (arena != null) e.setRespawnLocation(arena);
    }
}
