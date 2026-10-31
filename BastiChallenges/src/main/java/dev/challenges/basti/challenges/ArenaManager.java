package dev.challenges.basti.challenges;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Baut pro Spieler eine abgeschlossene Box (Glas/Barriere) nebeneinander ab dem gesetzten Origin.
 * Wird fuer die Army-Battle-Arenen und optional fuer Item-/Mob-Battles genutzt.
 */
public final class ArenaManager {
    private final BastiPlugin plugin;
    private final Map<UUID, Location> spawns = new HashMap<>();
    private final List<Location> wallBlocks = new ArrayList<>();
    private BukkitTask removalTask;

    public ArenaManager(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    public Location getOrigin() {
        FileConfiguration c = plugin.getConfig();
        String worldName = c.getString("arena.origin.world");
        if (worldName == null) return null;
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, c.getInt("arena.origin.x"), c.getInt("arena.origin.y"), c.getInt("arena.origin.z"));
    }

    public void setOrigin(Location l) {
        FileConfiguration c = plugin.getConfig();
        c.set("arena.origin.world", l.getWorld().getName());
        c.set("arena.origin.x", l.getBlockX());
        c.set("arena.origin.y", l.getBlockY());
        c.set("arena.origin.z", l.getBlockZ());
        plugin.saveConfig();
    }

    public Location getSpawn(UUID id) {
        Location l = spawns.get(id);
        return l == null ? null : l.clone();
    }

    /**
     * Baut eine Box pro Spieler. Gibt false zurueck, wenn noch kein Origin gesetzt ist.
     * @param scheduleRemoval Waende nach arena.wall-removal-seconds automatisch entfernen
     */
    public boolean buildBoxes(List<Player> players, boolean teleport, boolean scheduleRemoval) {
        Location origin = getOrigin();
        if (origin == null || origin.getWorld() == null) return false;

        FileConfiguration c = plugin.getConfig();
        int size = Math.max(4, c.getInt("arena.size", 16));
        int height = Math.max(3, c.getInt("arena.height", 8));
        int spacing = Math.max(0, c.getInt("arena.spacing", 4));
        Material wall = blockMaterial(c.getString("arena.wall-material", "GLASS"), Material.GLASS);
        Material floor = blockMaterial(c.getString("arena.floor-material", "STONE_BRICKS"), Material.STONE_BRICKS);

        World w = origin.getWorld();
        int ox = origin.getBlockX(), oy = origin.getBlockY(), oz = origin.getBlockZ();
        int step = size + 2 + spacing;

        clearWalls();
        spawns.clear();

        for (int i = 0; i < players.size(); i++) {
            int x0 = ox + i * step;
            int z0 = oz;
            for (int x = x0; x <= x0 + size + 1; x++) {
                for (int z = z0; z <= z0 + size + 1; z++) {
                    boolean edge = x == x0 || x == x0 + size + 1 || z == z0 || z == z0 + size + 1;
                    w.getBlockAt(x, oy - 1, z).setType(floor, false);
                    for (int y = oy; y <= oy + height; y++) {
                        Block b = w.getBlockAt(x, y, z);
                        if (edge || y == oy + height) {
                            b.setType(wall, false);
                            wallBlocks.add(b.getLocation());
                        } else {
                            b.setType(Material.AIR, false);
                        }
                    }
                }
            }
            Location spawn = new Location(w, x0 + 1 + size / 2.0, oy, z0 + 1 + size / 2.0, 0f, 0f);
            spawns.put(players.get(i).getUniqueId(), spawn);
            if (teleport) players.get(i).teleport(spawn);
        }

        if (scheduleRemoval) {
            int seconds = c.getInt("arena.wall-removal-seconds", 0);
            if (seconds > 0) {
                removalTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    clearWalls();
                    plugin.config().broadcast("&eDie Schutzzeit ist vorbei - die Waende sind weg!");
                }, seconds * 20L);
            }
        }
        return true;
    }

    /** Entfernt Waende und Decke aller Boxen (Boden bleibt stehen). */
    public void clearWalls() {
        if (removalTask != null) {
            removalTask.cancel();
            removalTask = null;
        }
        for (Location l : wallBlocks) l.getBlock().setType(Material.AIR, false);
        wallBlocks.clear();
    }

    private static Material blockMaterial(String name, Material fallback) {
        Material m = name == null ? null : Material.matchMaterial(name);
        return (m != null && m.isBlock()) ? m : fallback;
    }
}
