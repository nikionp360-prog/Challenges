package dev.challenges.basti.challenges;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.utils.ItemPool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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
 * Instant Force Item: Jeder Spieler hat genau ein Ziel-Item. Sobald es im Inventar liegt,
 * wird es gewertet und SOFORT das naechste Ziel vergeben. Kein Zeit-Intervall.
 */
public final class ForceItemChallenge implements HudProvider {
    public record Found(Material item, long millis) {}

    private final BastiPlugin plugin;
    private final Map<UUID, Material> targets = new HashMap<>();
    private final Map<UUID, List<Found>> found = new LinkedHashMap<>();
    private final Map<UUID, Integer> skips = new HashMap<>();
    private List<Material> pool = List.of();
    private boolean running;

    public ForceItemChallenge(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() { return running; }
    public Map<UUID, List<Found>> getFound() { return found; }

    public void start() {
        pool = ItemPool.build(plugin.config().upperSet("force-item.excluded-items"));
        targets.clear();
        found.clear();
        skips.clear();
        running = !pool.isEmpty();
        if (!running) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) join(p);
        }
    }

    public void stop() {
        running = false;
    }

    private void join(Player p) {
        UUID id = p.getUniqueId();
        found.putIfAbsent(id, new ArrayList<>());
        skips.putIfAbsent(id, plugin.getConfig().getInt("force-item.skips", 3));
        assign(p, false);
    }

    private void assign(Player p, boolean announce) {
        UUID id = p.getUniqueId();
        Material prev = targets.get(id);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Material next = pool.get(rnd.nextInt(pool.size()));
        for (int i = 0; i < 5 && next == prev && pool.size() > 1; i++) next = pool.get(rnd.nextInt(pool.size()));
        targets.put(id, next);
        p.showTitle(Title.title(
                Component.text("Neues Item", NamedTextColor.GREEN),
                Component.translatable(next.translationKey(), NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1500), Duration.ofMillis(300))));
        if (announce) p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    /** Verzoegert auf den naechsten Tick, weil Events vor der Inventar-Aenderung feuern. */
    public void checkLater(Player p) {
        if (!running) return;
        Bukkit.getScheduler().runTask(plugin, () -> check(p));
    }

    /** Prueft, ob das aktuelle Ziel im Inventar ist - wenn ja: werten + sofort naechstes Ziel. */
    public void check(Player p) {
        if (!running || plugin.timer().getState() != dev.challenges.basti.utils.TimerManager.State.RUNNING) return;
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        UUID id = p.getUniqueId();
        Material target = targets.get(id);
        if (target == null) {
            join(p);
            return;
        }
        if (!p.getInventory().contains(target)) return;

        found.computeIfAbsent(id, k -> new ArrayList<>()).add(new Found(target, plugin.timer().getElapsedMillis()));
        if (plugin.getConfig().getBoolean("force-item.remove-item-on-complete", false)) {
            p.getInventory().removeItem(new ItemStack(target, 1));
        }
        p.sendMessage(plugin.config().msg("&aGeschafft: ")
                .append(Component.translatable(target.translationKey(), NamedTextColor.GOLD)));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        assign(p, false);
    }

    public void skip(Player p) {
        if (!running) {
            plugin.config().send(p, "&cForce Item laeuft gerade nicht.");
            return;
        }
        UUID id = p.getUniqueId();
        int left = skips.getOrDefault(id, 0);
        if (left <= 0) {
            plugin.config().send(p, "&cKeine Skips mehr uebrig.");
            return;
        }
        skips.put(id, left - 1);
        plugin.config().send(p, "&eItem uebersprungen. Skips uebrig: &f" + (left - 1));
        assign(p, true);
    }

    @Override
    public Component hud(Player p) {
        if (!running) return null;
        Material t = targets.get(p.getUniqueId());
        if (t == null) return null;
        int count = found.getOrDefault(p.getUniqueId(), List.of()).size();
        return Component.text("Item: ", NamedTextColor.GRAY)
                .append(Component.translatable(t.translationKey(), NamedTextColor.GOLD))
                .append(Component.text(" (" + count + ")", NamedTextColor.GRAY));
    }
}
