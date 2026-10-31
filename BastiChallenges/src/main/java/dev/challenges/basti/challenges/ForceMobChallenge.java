package dev.challenges.basti.challenges;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.utils.MobPool;
import dev.challenges.basti.utils.TimerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Instant Force Mob: Jeder Spieler bekommt genau einen Ziel-Mob. Wird er getoetet,
 * zaehlt er sofort und das naechste Ziel wird vergeben. Ein Richtungskompass in der
 * Actionbar zeigt auf den naechsten Mob dieser Art.
 */
public final class ForceMobChallenge implements HudProvider {
    public record Found(EntityType type, long millis) {}

    // Pfeile fuer 8 Richtungen (0 = geradeaus, im Uhrzeigersinn)
    private static final String[] ARROWS_UNI = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};

    private final BastiPlugin plugin;
    private final Map<UUID, EntityType> targets = new HashMap<>();
    private final Map<UUID, List<Found>> found = new LinkedHashMap<>();
    private final Map<UUID, Integer> skips = new HashMap<>();
    private final Map<UUID, String> compass = new HashMap<>();
    private List<EntityType> pool = List.of();
    private boolean running;

    public ForceMobChallenge(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() { return running; }
    public Map<UUID, List<Found>> getFound() { return found; }

    public void start() {
        pool = MobPool.pool(plugin.config().lowerSet("force-mob.excluded-mobs"));
        targets.clear();
        found.clear();
        skips.clear();
        compass.clear();
        running = !pool.isEmpty();
        if (!running) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            join(p);
            if (plugin.getConfig().getBoolean("force-mob.give-compass", true)) {
                p.getInventory().addItem(new ItemStack(Material.COMPASS));
            }
        }
    }

    public void stop() {
        running = false;
    }

    private void join(Player p) {
        UUID id = p.getUniqueId();
        found.putIfAbsent(id, new ArrayList<>());
        skips.putIfAbsent(id, plugin.getConfig().getInt("force-mob.skips", 3));
        assign(p, false);
    }

    private void assign(Player p, boolean announce) {
        UUID id = p.getUniqueId();
        EntityType prev = targets.get(id);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        EntityType next = pool.get(rnd.nextInt(pool.size()));
        for (int i = 0; i < 5 && next.equals(prev) && pool.size() > 1; i++) next = pool.get(rnd.nextInt(pool.size()));
        targets.put(id, next);
        compass.remove(id);
        p.showTitle(Title.title(
                Component.text("Neuer Mob", NamedTextColor.RED),
                Component.translatable(next.translationKey(), NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1500), Duration.ofMillis(300))));
        if (announce) p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    /** Wird vom EntityDeathListener aufgerufen. */
    public void onKill(Player killer, EntityType type) {
        if (!running || plugin.timer().getState() != TimerManager.State.RUNNING) return;
        UUID id = killer.getUniqueId();
        EntityType target = targets.get(id);
        if (target == null) {
            join(killer);
            target = targets.get(id);
        }
        if (target == null || !target.equals(type)) return;

        found.computeIfAbsent(id, k -> new ArrayList<>()).add(new Found(target, plugin.timer().getElapsedMillis()));
        killer.sendMessage(plugin.config().msg("&aGeschafft: ")
                .append(Component.translatable(target.translationKey(), NamedTextColor.GOLD)));
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        assign(killer, false);
    }

    public void skip(Player p) {
        if (!running) {
            plugin.config().send(p, "&cForce Mob laeuft gerade nicht.");
            return;
        }
        UUID id = p.getUniqueId();
        int left = skips.getOrDefault(id, 0);
        if (left <= 0) {
            plugin.config().send(p, "&cKeine Skips mehr uebrig.");
            return;
        }
        skips.put(id, left - 1);
        plugin.config().send(p, "&eMob uebersprungen. Skips uebrig: &f" + (left - 1));
        assign(p, true);
    }

    /** Wird ca. 1x pro Sekunde aufgerufen: naechsten Ziel-Mob suchen, Kompass + Pfeil aktualisieren. */
    public void updateCompass() {
        if (!running) return;
        int radius = plugin.getConfig().getInt("force-mob.compass-radius", 128);
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            EntityType t = targets.get(id);
            if (t == null) continue;

            Location from = p.getLocation();
            Entity best = null;
            double bestD = Double.MAX_VALUE;
            for (Entity e : p.getWorld().getNearbyEntities(from, radius, radius, radius, en -> en.getType().equals(t))) {
                double d = e.getLocation().distanceSquared(from);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            if (best == null) {
                compass.put(id, "kein Ziel in " + radius + "m");
                continue;
            }
            Location tl = best.getLocation();
            p.setCompassTarget(tl);

            double dx = tl.getX() - from.getX();
            double dz = tl.getZ() - from.getZ();
            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double rel = targetYaw - from.getYaw();
            rel = ((rel % 360) + 540) % 360 - 180;           // -180..180, positiv = rechts
            int idx = ((int) Math.round(rel / 45.0) % 8 + 8) % 8;
            compass.put(id, ARROWS_UNI[idx] + " " + (int) Math.sqrt(bestD) + "m");
        }
    }

    @Override
    public Component hud(Player p) {
        if (!running) return null;
        UUID id = p.getUniqueId();
        EntityType t = targets.get(id);
        if (t == null) return null;
        int count = found.getOrDefault(id, List.of()).size();
        Component c = Component.text("Mob: ", NamedTextColor.GRAY)
                .append(Component.translatable(t.translationKey(), NamedTextColor.GOLD))
                .append(Component.text(" (" + count + ")", NamedTextColor.GRAY));
        String info = compass.get(id);
        if (info != null) c = c.append(Component.text("  " + info, NamedTextColor.AQUA));
        return c;
    }
}
