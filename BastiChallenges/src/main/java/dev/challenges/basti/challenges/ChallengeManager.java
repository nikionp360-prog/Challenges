package dev.challenges.basti.challenges;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.gui.ResultGui;
import dev.challenges.basti.utils.ActionBar;
import dev.challenges.basti.utils.ResultStorage;
import dev.challenges.basti.utils.TimerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Zentrale Steuerung: aktive Modi, globaler/modus-spezifischer Randomizer,
 * Start/Reset/Ende der Challenge und das Rendern der Actionbar (Timer + Modus-Infos).
 */
public final class ChallengeManager {
    private final BastiPlugin plugin;
    private final EnumSet<ChallengeMode> active = EnumSet.noneOf(ChallengeMode.class);
    private final Map<ChallengeMode, Boolean> randomizerFlags = new EnumMap<>(ChallengeMode.class);
    private boolean randomizerGlobal;

    private final ForceItemChallenge forceItem;
    private final ForceMobChallenge forceMob;
    private final MonsterArmyManager army;
    private final RandomizerManager randomizer;
    private final List<HudProvider> providers = new ArrayList<>();

    private BukkitTask task;
    private int tickCounter;
    private ResultGui.Type lastResult;

    public ChallengeManager(BastiPlugin plugin) {
        this.plugin = plugin;
        this.forceItem = new ForceItemChallenge(plugin);
        this.forceMob = new ForceMobChallenge(plugin);
        this.army = new MonsterArmyManager(plugin);
        this.randomizer = new RandomizerManager(plugin);
        providers.add(forceItem);
        providers.add(forceMob);
        providers.add(army);

        FileConfiguration c = plugin.getConfig();
        for (ChallengeMode m : ChallengeMode.values()) {
            if (c.getBoolean("modes." + m.key(), false)) active.add(m);
            randomizerFlags.put(m, c.getBoolean("randomizer.per-mode." + m.key(), true));
        }
        randomizerGlobal = c.getBoolean("randomizer.enabled", false);
    }

    public ForceItemChallenge forceItem() { return forceItem; }
    public ForceMobChallenge forceMob() { return forceMob; }
    public MonsterArmyManager army() { return army; }
    public RandomizerManager randomizer() { return randomizer; }

    // ------------------------------------------------------------------ Modi + Randomizer

    public boolean isActive(ChallengeMode m) { return active.contains(m); }

    public void setMode(ChallengeMode mode, boolean on) {
        if (on) active.add(mode);
        else active.remove(mode);
        plugin.getConfig().set("modes." + mode.key(), on);
        plugin.saveConfig();

        if (plugin.timer().getState() == TimerManager.State.RUNNING) {
            switch (mode) {
                case FORCEITEM -> { if (on) forceItem.start(); else forceItem.stop(); }
                case FORCEMOB -> { if (on) forceMob.start(); else forceMob.stop(); }
                default -> { }
            }
        }
    }

    public boolean isRandomizerGlobal() { return randomizerGlobal; }

    public void setRandomizerGlobal(boolean on) {
        randomizerGlobal = on;
        plugin.getConfig().set("randomizer.enabled", on);
        plugin.saveConfig();
    }

    public boolean getRandomizerFlag(ChallengeMode m) { return randomizerFlags.getOrDefault(m, true); }

    public void setRandomizerFlag(ChallengeMode m, boolean on) {
        randomizerFlags.put(m, on);
        plugin.getConfig().set("randomizer.per-mode." + m.key(), on);
        plugin.saveConfig();
    }

    /** Randomizer greift, wenn er global an ist und (falls Modi aktiv sind) fuer mindestens einen aktiven Modus an ist. */
    public boolean isRandomizerActive() {
        if (!randomizerGlobal) return false;
        if (active.isEmpty()) return true;
        for (ChallengeMode m : active) if (getRandomizerFlag(m)) return true;
        return false;
    }

    // ------------------------------------------------------------------ Ablauf

    public void startTask() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        plugin.arenas().clearWalls();
    }

    /** Startet die Challenge aus dem gestoppten Zustand. */
    public void start() {
        TimerManager t = plugin.timer();

        if (active.contains(ChallengeMode.ARMYBATTLE)
                && (army.phase() == MonsterArmyManager.Phase.IDLE || army.phase() == MonsterArmyManager.Phase.FINISHED)) {
            army.startCollect();
            t.reset();
            t.setCountdown(true);
            t.setCountdownSeconds(plugin.getConfig().getLong("army-battle.collect-seconds", 7200L));
        }

        if (plugin.getConfig().getBoolean("arena.auto-build-on-start", false)
                && (active.contains(ChallengeMode.FORCEITEM) || active.contains(ChallengeMode.FORCEMOB))) {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR);
            if (!plugin.arenas().buildBoxes(players, true, true)) {
                plugin.config().broadcast("&cArena-Boxen nicht gebaut: kein Origin (&f/challenge arena setspawn&c).");
            }
        }

        t.start();
        if (active.contains(ChallengeMode.FORCEITEM)) forceItem.start();
        if (active.contains(ChallengeMode.FORCEMOB)) forceMob.start();
        plugin.config().broadcast("&aChallenge gestartet!");
    }

    public void reset() {
        plugin.timer().reset();
        plugin.timer().reloadFromConfig();
        forceItem.stop();
        forceMob.stop();
        plugin.arenas().clearWalls();
        plugin.config().broadcast("&7Timer zurueckgesetzt.");
    }

    /** Timer abgelaufen bzw. manuell gestoppt. */
    public void onTimerFinished() {
        if (army.phase() == MonsterArmyManager.Phase.COLLECT) {
            army.endCollect();
            return;
        }
        forceItem.stop();
        forceMob.stop();
        plugin.config().broadcast("&6Die Zeit ist vorbei!");

        ResultGui.Type type = null;
        if (active.contains(ChallengeMode.FORCEITEM)) type = ResultGui.Type.FORCEITEM;
        else if (active.contains(ChallengeMode.FORCEMOB)) type = ResultGui.Type.FORCEMOB;
        if (type != null) {
            lastResult = type;
            ResultStorage.saveAsync(plugin);
            for (Player p : Bukkit.getOnlinePlayers()) new ResultGui(plugin, type).open(p);
        }
    }

    public ResultGui.Type defaultResultType() {
        if (lastResult != null) return lastResult;
        if (!forceItem.getFound().isEmpty()) return ResultGui.Type.FORCEITEM;
        if (!forceMob.getFound().isEmpty()) return ResultGui.Type.FORCEMOB;
        if (!army.ranking().isEmpty()) return ResultGui.Type.ARMY;
        return null;
    }

    // ------------------------------------------------------------------ Tick + HUD

    private void tick() {
        tickCounter++;
        TimerManager t = plugin.timer();
        if (t.isCountdownFinished()) {
            t.finish();
            onTimerFinished();
        }

        // Instant-Pruefung als Sicherheitsnetz (Events erledigen den Grossteil sofort)
        if (forceItem.isRunning()) {
            for (Player p : Bukkit.getOnlinePlayers()) forceItem.check(p);
        }
        if (tickCounter % 2 == 0) forceMob.updateCompass();
        army.tick();
        renderHud();
    }

    private void renderHud() {
        TimerManager t = plugin.timer();
        Component sep = Component.text(" | ", NamedTextColor.DARK_GRAY);
        for (Player p : Bukkit.getOnlinePlayers()) {
            List<Component> parts = new ArrayList<>();
            if (t.getState() != TimerManager.State.STOPPED) parts.add(ActionBar.timer(t));
            for (HudProvider h : providers) {
                Component c = h.hud(p);
                if (c != null) parts.add(c);
            }
            if (parts.isEmpty()) continue;
            ActionBar.send(p, Component.join(JoinConfiguration.separator(sep), parts));
        }
    }
}
