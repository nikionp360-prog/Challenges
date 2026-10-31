package dev.challenges.basti.challenges;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.gui.ResultGui;
import dev.challenges.basti.gui.WaveBuilderGui;
import dev.challenges.basti.utils.MobPool;
import dev.challenges.basti.utils.ResultStorage;
import dev.challenges.basti.utils.Text;
import dev.challenges.basti.utils.TimerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Monster Army Battle - ohne Punktesystem.
 *
 * Phase 1 COLLECT: Spieler toeten Mobs, jeder Kill landet im persoenlichen Mob-Speicher.
 * Phase 2 BUILD:   Jeder verteilt seine Mobs auf genau 3 Wellen (Chest-GUI).
 * Phase 3 FIGHT:   Jeder Spieler kaempft in seiner eigenen Arena gegen die 3 Wellen eines
 *                  Gegners. Welle n+1 spawnt erst, wenn Welle n komplett besiegt ist.
 *                  Gewinner ist, wer alle 3 Wellen in der kuerzesten Zeit schafft.
 */
public final class MonsterArmyManager implements HudProvider {
    public enum Phase { IDLE, COLLECT, BUILD, FIGHT, FINISHED }

    public static final int WAVES = 3;

    /** Kampf-Zustand eines Spielers. */
    public static final class Fight {
        public final UUID player;
        public final UUID opponent;     // dessen Wellen muss "player" besiegen
        public int wave;                // aktuelle Welle (0-basiert)
        public boolean finished;
        public long finishMillis = -1L;
        public long penaltyMillis;
        boolean spawning = true;        // true, solange die naechste Welle noch nicht gespawnt ist
        Location arena;
        final Set<UUID> alive = new HashSet<>();

        Fight(UUID player, UUID opponent) {
            this.player = player;
            this.opponent = opponent;
        }
    }

    private final BastiPlugin plugin;
    private final NamespacedKey ownerKey;
    private Phase phase = Phase.IDLE;
    private final Map<UUID, Map<EntityType, Integer>> storage = new LinkedHashMap<>();
    private final Map<UUID, List<Map<EntityType, Integer>>> waves = new LinkedHashMap<>();
    private final Map<UUID, Fight> fights = new LinkedHashMap<>();

    public MonsterArmyManager(BastiPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "army_owner");
    }

    public Phase phase() { return phase; }

    public int maxPerWave() {
        return Math.max(1, plugin.getConfig().getInt("army-battle.max-mobs-per-wave", 40));
    }

    private Set<String> excluded() {
        return plugin.config().lowerSet("army-battle.excluded-mobs");
    }

    // ------------------------------------------------------------------ Phase 1: Sammeln

    public void startCollect() {
        cleanupMobs();
        storage.clear();
        waves.clear();
        fights.clear();
        phase = Phase.COLLECT;
        plugin.config().broadcast("&cMonster Army Battle: &7Sammelphase gestartet! Toetet Mobs - sie landen in eurem Mob-Speicher.");
    }

    /** Zaehlt einen Kill im Mob-Speicher (nur in der Sammelphase). */
    public void onKill(Player killer, EntityType type) {
        if (phase != Phase.COLLECT) return;
        if (!MobPool.isValid(type, excluded())) return;
        storage.computeIfAbsent(killer.getUniqueId(), k -> new LinkedHashMap<>()).merge(type, 1, Integer::sum);
    }

    public int totalStored(UUID id) {
        int sum = 0;
        for (int v : storage.getOrDefault(id, Collections.emptyMap()).values()) sum += v;
        return sum;
    }

    /** Sammelphase beenden (Timer abgelaufen oder Admin) und in die Build-Phase wechseln. */
    public void endCollect() {
        if (phase != Phase.COLLECT) return;
        phase = Phase.BUILD;
        plugin.timer().reset();
        plugin.timer().reloadFromConfig();
        plugin.config().broadcast("&cSammelphase vorbei! &7Baut jetzt eure &e" + WAVES + " Wellen &7(&f/challenge army build&7).");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) new WaveBuilderGui(plugin, p).open(p);
        }
    }

    // ------------------------------------------------------------------ Phase 2: Wellen bauen

    public void openBuilder(Player p) {
        if (phase != Phase.BUILD) {
            plugin.config().send(p, "&cDie Build-Phase ist gerade nicht aktiv.");
            return;
        }
        new WaveBuilderGui(plugin, p).open(p);
    }

    private List<Map<EntityType, Integer>> wavesOf(UUID id) {
        return waves.computeIfAbsent(id, k -> {
            List<Map<EntityType, Integer>> l = new ArrayList<>();
            for (int i = 0; i < WAVES; i++) l.add(new LinkedHashMap<>());
            return l;
        });
    }

    public List<Map<EntityType, Integer>> getWaves(UUID id) { return wavesOf(id); }
    public Map<EntityType, Integer> storageOf(UUID id) { return storage.getOrDefault(id, Collections.emptyMap()); }
    public int stored(UUID id, EntityType t) { return storageOf(id).getOrDefault(t, 0); }

    public int used(UUID id, EntityType t) {
        int sum = 0;
        for (Map<EntityType, Integer> w : wavesOf(id)) sum += w.getOrDefault(t, 0);
        return sum;
    }

    public int inWave(UUID id, int wave, EntityType t) { return wavesOf(id).get(wave).getOrDefault(t, 0); }

    public int waveSize(UUID id, int wave) {
        int sum = 0;
        for (int v : wavesOf(id).get(wave).values()) sum += v;
        return sum;
    }

    /** Aendert die Anzahl eines Mob-Typs in einer Welle. Gibt die tatsaechlich angewendete Aenderung zurueck. */
    public int change(UUID id, int wave, EntityType t, int delta) {
        Map<EntityType, Integer> w = wavesOf(id).get(wave);
        int cur = w.getOrDefault(t, 0);
        int applied;
        if (delta > 0) {
            int free = stored(id, t) - used(id, t);
            int space = maxPerWave() - waveSize(id, wave);
            applied = Math.max(0, Math.min(delta, Math.min(free, space)));
        } else {
            applied = -Math.min(-delta, cur);
        }
        if (applied == 0) return 0;
        int now = cur + applied;
        if (now <= 0) w.remove(t);
        else w.put(t, now);
        return applied;
    }

    public void clearWave(UUID id, int wave) { wavesOf(id).get(wave).clear(); }

    // ------------------------------------------------------------------ Phase 3: Kampf

    public void startFight(CommandSender sender, boolean force) {
        if (phase != Phase.BUILD) {
            plugin.config().send(sender, "&cStartfight geht nur in der Build-Phase.");
            return;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR);
        players.sort(Comparator.comparing(Player::getName));
        if (players.isEmpty()) {
            plugin.config().send(sender, "&cKeine Spieler online.");
            return;
        }

        if (!force) {
            List<String> missing = new ArrayList<>();
            for (Player p : players) {
                for (int w = 0; w < WAVES; w++) {
                    if (waveSize(p.getUniqueId(), w) == 0) {
                        missing.add(p.getName() + " (Welle " + (w + 1) + ")");
                    }
                }
            }
            if (!missing.isEmpty()) {
                plugin.config().send(sender, "&cLeere Wellen bei: &f" + String.join(", ", missing)
                        + " &7- mit &f/challenge army startfight force &7trotzdem starten.");
                return;
            }
        }

        if (!plugin.arenas().buildBoxes(players, true, false)) {
            plugin.config().send(sender, "&cKein Arena-Origin gesetzt. Stell dich hin und nutze &f/challenge arena setspawn&c.");
            return;
        }

        fights.clear();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Player opp = players.size() == 1 ? p : players.get((i + 1) % players.size());
            Fight f = new Fight(p.getUniqueId(), opp.getUniqueId());
            f.arena = plugin.arenas().getSpawn(p.getUniqueId());
            fights.put(p.getUniqueId(), f);
        }

        phase = Phase.FIGHT;
        int delay = Math.max(0, plugin.getConfig().getInt("army-battle.start-delay-seconds", 5));
        plugin.config().broadcast("&cKampfphase! &7Jeder kaempft gegen die Wellen seines Gegners. Start in &e" + delay + "s&7.");
        Bukkit.getScheduler().runTaskLater(plugin, this::beginFight, delay * 20L);
    }

    private void beginFight() {
        if (phase != Phase.FIGHT) return;
        TimerManager t = plugin.timer();
        t.reset();
        t.setCountdown(false);
        t.start();
        plugin.config().broadcast("&aLos! &7Die Zeit laeuft.");
        for (Fight f : new ArrayList<>(fights.values())) spawnWave(f);
    }

    private void spawnWave(Fight f) {
        if (phase != Phase.FIGHT || f.finished) return;
        Player p = Bukkit.getPlayer(f.player);
        if (p == null || f.arena == null || f.arena.getWorld() == null) {
            // Spieler offline: spaeter nochmal versuchen
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnWave(f), 40L);
            return;
        }
        f.spawning = false;
        if (f.wave >= WAVES) return;

        Map<EntityType, Integer> wave = wavesOf(f.opponent).get(f.wave);
        World w = f.arena.getWorld();
        double r = Math.max(2.0, plugin.getConfig().getInt("arena.size", 16) / 2.0 - 3.0);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (Map.Entry<EntityType, Integer> en : wave.entrySet()) {
            for (int i = 0; i < en.getValue(); i++) {
                double a = rnd.nextDouble(Math.PI * 2);
                double d = rnd.nextDouble(1.5, r);
                Location loc = f.arena.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                try {
                    Entity e = w.spawnEntity(loc, en.getKey());
                    e.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, f.player.toString());
                    if (e instanceof LivingEntity le) {
                        le.setRemoveWhenFarAway(false);
                    }
                    if (e instanceof Mob m) m.setTarget(p);
                    f.alive.add(e.getUniqueId());
                } catch (IllegalArgumentException ignored) {
                    // Mob-Typ nicht spawnbar -> ueberspringen
                }
            }
        }

        p.showTitle(Title.title(Text.c("&cWelle " + (f.wave + 1) + "/" + WAVES),
                Text.c("&7" + f.alive.size() + " Mobs"),
                Title.Times.times(java.time.Duration.ofMillis(100), java.time.Duration.ofMillis(1200), java.time.Duration.ofMillis(300))));

        if (f.alive.isEmpty()) waveCleared(f); // leere Welle -> direkt weiter
    }

    private void waveCleared(Fight f) {
        if (f.finished || f.spawning) return;
        f.wave++;
        if (f.wave >= WAVES) {
            finishFight(f);
            return;
        }
        f.spawning = true;
        Player p = Bukkit.getPlayer(f.player);
        if (p != null) plugin.config().send(p, "&aWelle " + f.wave + " besiegt! Die naechste kommt gleich ...");
        long delay = Math.max(0, plugin.getConfig().getInt("army-battle.wave-delay-seconds", 3)) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnWave(f), delay);
    }

    private void finishFight(Fight f) {
        f.finished = true;
        f.finishMillis = plugin.timer().getElapsedMillis() + f.penaltyMillis;
        int place = 0;
        for (Fight o : fights.values()) if (o.finished) place++;
        plugin.config().broadcast("&e" + Text.name(f.player) + " &7hat alle Wellen besiegt: &a"
                + TimerManager.formatPrecise(f.finishMillis)
                + (f.penaltyMillis > 0 ? " &8(inkl. " + (f.penaltyMillis / 1000) + "s Strafe)" : "")
                + " &7(fertig als #" + place + ")");
        boolean all = true;
        for (Fight o : fights.values()) if (!o.finished) all = false;
        if (all) endFight();
    }

    /** Beendet den Kampf (alle fertig oder Admin-Abbruch) und zeigt die Ergebnisse. */
    public void endFight() {
        if (phase != Phase.FIGHT) return;
        phase = Phase.FINISHED;
        plugin.timer().finish();
        cleanupMobs();

        List<Fight> ranking = ranking();
        if (!ranking.isEmpty() && ranking.get(0).finished) {
            Fight w = ranking.get(0);
            plugin.config().broadcast("&6Gewinner: &e" + Text.name(w.player) + " &7mit &a" + TimerManager.formatPrecise(w.finishMillis));
        } else {
            plugin.config().broadcast("&7Niemand hat alle Wellen geschafft.");
        }
        ResultStorage.saveAsync(plugin);
        for (Player p : Bukkit.getOnlinePlayers()) new ResultGui(plugin, ResultGui.Type.ARMY).open(p);
    }

    /** Fertige Spieler nach Zeit sortiert, danach nicht Fertige. */
    public List<Fight> ranking() {
        List<Fight> l = new ArrayList<>(fights.values());
        l.sort(Comparator.comparingLong(f -> f.finished ? f.finishMillis : Long.MAX_VALUE));
        return l;
    }

    /** Alle 10 Ticks: tote/entfernte Mobs aus der Liste werfen und ggf. Welle abschliessen. */
    public void tick() {
        if (phase != Phase.FIGHT) return;
        for (Fight f : new ArrayList<>(fights.values())) {
            if (f.finished || f.spawning) continue;
            f.alive.removeIf(id -> {
                Entity e = Bukkit.getEntity(id);
                return e == null || !e.isValid() || e.isDead();
            });
            if (f.alive.isEmpty()) waveCleared(f);
        }
    }

    // ------------------------------------------------------------------ Events

    public boolean isArenaMob(Entity e) {
        return e.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }

    public void onMobDeath(Entity e) {
        String owner = e.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner == null) return;
        Fight f;
        try {
            f = fights.get(UUID.fromString(owner));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (f == null) return;
        f.alive.remove(e.getUniqueId());
        if (f.alive.isEmpty() && !f.spawning && !f.finished) waveCleared(f);
    }

    public void onPlayerDeath(Player p) {
        Fight f = fights.get(p.getUniqueId());
        if (phase != Phase.FIGHT || f == null || f.finished) return;
        long penalty = Math.max(0, plugin.getConfig().getInt("army-battle.death-penalty-seconds", 30)) * 1000L;
        f.penaltyMillis += penalty;
        plugin.config().broadcast("&c" + p.getName() + " &7ist gestorben: &c+" + (penalty / 1000) + "s Strafe");
    }

    /** Respawn-Punkt in der eigenen Arena (oder null, wenn kein Kampf laeuft). */
    public Location respawnLocation(Player p) {
        Fight f = fights.get(p.getUniqueId());
        if (phase != Phase.FIGHT || f == null || f.arena == null) return null;
        return f.arena.clone();
    }

    // ------------------------------------------------------------------ Aufraeumen / Status

    private void cleanupMobs() {
        for (Fight f : fights.values()) {
            for (UUID id : f.alive) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            f.alive.clear();
        }
    }

    /** Alles zuruecksetzen (Admin). */
    public void stop() {
        cleanupMobs();
        fights.clear();
        storage.clear();
        waves.clear();
        phase = Phase.IDLE;
        plugin.timer().reset();
        plugin.timer().reloadFromConfig();
    }

    public void status(CommandSender s) {
        plugin.config().send(s, "&7Army-Phase: &e" + phase);
        if (phase == Phase.COLLECT || phase == Phase.BUILD) {
            for (Map.Entry<UUID, Map<EntityType, Integer>> e : storage.entrySet()) {
                plugin.config().send(s, " &f" + Text.name(e.getKey()) + "&7: &e" + totalStored(e.getKey()) + " &7Mobs gesammelt");
            }
        }
        if (phase == Phase.FIGHT || phase == Phase.FINISHED) {
            for (Fight f : fights.values()) {
                plugin.config().send(s, " &f" + Text.name(f.player) + "&7: Welle &e" + Math.min(f.wave + 1, WAVES)
                        + "&7/" + WAVES + (f.finished ? " &a" + TimerManager.formatPrecise(f.finishMillis) : ""));
            }
        }
    }

    @Override
    public Component hud(Player p) {
        UUID id = p.getUniqueId();
        return switch (phase) {
            case IDLE, FINISHED -> null;
            case COLLECT -> Component.text("Mobs gesammelt: " + totalStored(id), NamedTextColor.RED);
            case BUILD -> Component.text("Wellen bauen: /challenge army build", NamedTextColor.YELLOW);
            case FIGHT -> {
                Fight f = fights.get(id);
                if (f == null) yield null;
                if (f.finished) {
                    yield Component.text("Fertig: " + TimerManager.formatPrecise(f.finishMillis), NamedTextColor.GREEN);
                }
                yield Component.text("Welle " + Math.min(f.wave + 1, WAVES) + "/" + WAVES + " | Mobs: " + f.alive.size(),
                        NamedTextColor.RED);
            }
        };
    }
}
