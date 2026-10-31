package dev.challenges.basti.utils;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Zentraler Timer. Arbeitet mit Zeitstempeln statt mit Tick-Zaehlern, dadurch ist er
 * driftfrei und die Zeit ist auf die Millisekunde genau (wichtig fuer Army-Battle-Zeiten).
 * Angezeigt wird er ueber {@link ActionBar}; das Rendern uebernimmt der ChallengeManager.
 */
public final class TimerManager {
    public enum State { STOPPED, RUNNING, PAUSED }

    private final BastiPlugin plugin;
    private State state = State.STOPPED;
    private boolean countdown;
    private long totalMillis;      // Startwert beim Countdown
    private long baseMillis;       // bereits abgelaufene Zeit (bzw. Offset beim Countup)
    private long resumedAt = -1L;  // Zeitstempel des letzten Starts/Fortsetzens

    public TimerManager(BastiPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        FileConfiguration c = plugin.getConfig();
        countdown = "COUNTDOWN".equalsIgnoreCase(c.getString("timer.direction", "COUNTUP"));
        totalMillis = Math.max(1L, c.getLong("timer.countdown-seconds", 3600L)) * 1000L;
    }

    public State getState() { return state; }
    public boolean isCountdown() { return countdown; }
    public void setCountdown(boolean countdown) { this.countdown = countdown; }
    public void setCountdownSeconds(long seconds) { this.totalMillis = Math.max(1L, seconds) * 1000L; }

    public void start() {
        if (state == State.RUNNING) return;
        if (state == State.STOPPED && countdown && getDisplayMillis() <= 0) baseMillis = 0;
        resumedAt = System.currentTimeMillis();
        state = State.RUNNING;
    }

    public void pause() {
        if (state != State.RUNNING) return;
        baseMillis += System.currentTimeMillis() - resumedAt;
        resumedAt = -1L;
        state = State.PAUSED;
    }

    public void resume() {
        if (state != State.PAUSED) return;
        resumedAt = System.currentTimeMillis();
        state = State.RUNNING;
    }

    public void reset() {
        state = State.STOPPED;
        baseMillis = 0;
        resumedAt = -1L;
    }

    /** Friert die Zeit ein und stoppt (Countdown abgelaufen / Challenge beendet). */
    public void finish() {
        baseMillis = getElapsedMillis();
        resumedAt = -1L;
        state = State.STOPPED;
    }

    /** Setzt die Zeit: beim Countdown den Startwert, beim Countup die aktuelle Zeit. */
    public void setTime(long seconds) {
        long ms = Math.max(0L, seconds) * 1000L;
        if (countdown) {
            totalMillis = Math.max(1000L, ms);
        } else {
            baseMillis = ms;
            if (resumedAt >= 0) resumedAt = System.currentTimeMillis();
        }
    }

    public long getElapsedMillis() {
        long e = baseMillis;
        if (resumedAt >= 0) e += System.currentTimeMillis() - resumedAt;
        return e;
    }

    public long getDisplayMillis() {
        long elapsed = getElapsedMillis();
        return countdown ? Math.max(0L, totalMillis - elapsed) : elapsed;
    }

    public boolean isCountdownFinished() {
        return countdown && state == State.RUNNING && getDisplayMillis() <= 0;
    }

    public boolean isFrozen() {
        return state == State.PAUSED && plugin.getConfig().getBoolean("timer.freeze-players-on-pause", true);
    }

    public String format() {
        return formatSeconds(getDisplayMillis() / 1000L);
    }

    public static String formatSeconds(long s) {
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    public static String formatMillis(long ms) {
        return formatSeconds(ms / 1000L);
    }

    /** MM:SS.mmm fuer exakte Kampfzeiten. */
    public static String formatPrecise(long ms) {
        return String.format("%02d:%02d.%03d", ms / 60000L, (ms / 1000L) % 60L, ms % 1000L);
    }
}
