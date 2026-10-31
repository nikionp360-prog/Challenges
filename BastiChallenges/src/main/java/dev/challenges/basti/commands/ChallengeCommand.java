package dev.challenges.basti.commands;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.challenges.ChallengeMode;
import dev.challenges.basti.challenges.MonsterArmyManager;
import dev.challenges.basti.gui.SettingsGui;
import dev.challenges.basti.utils.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /challenge (Alias: /basti, /ch) - Admin-Steuerung. */
public final class ChallengeCommand implements CommandExecutor, TabCompleter {
    private final BastiPlugin plugin;

    public ChallengeCommand(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String text) {
        plugin.config().send(s, text);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        boolean publicSub = a.length >= 2 && a[0].equalsIgnoreCase("army") && a[1].equalsIgnoreCase("build");
        if (!publicSub && !s.hasPermission("challenge.admin")) {
            msg(s, "&cDafuer fehlt dir die Berechtigung.");
            return true;
        }
        if (a.length == 0) {
            help(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "timer" -> timer(s, a);
            case "mode" -> mode(s, a);
            case "randomizer" -> randomizer(s, a);
            case "settings" -> {
                if (s instanceof Player p) new SettingsGui(plugin).open(p);
                else msg(s, "&cNur ingame.");
            }
            case "arena" -> arena(s, a);
            case "army" -> army(s, a);
            default -> help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        msg(s, "&eBefehle:");
        s.sendMessage(plugin.config().msg("&f/challenge timer <start|pause|resume|stop|reset|settime <zeit>|direction <up|down>>"));
        s.sendMessage(plugin.config().msg("&f/challenge mode <forceitem|forcemob|armybattle|forceskill> <on|off>"));
        s.sendMessage(plugin.config().msg("&f/challenge randomizer <on|off> &7bzw. &f<modus> <on|off> &7bzw. &fseed [zahl]"));
        s.sendMessage(plugin.config().msg("&f/challenge settings &7- Einstellungs-GUI"));
        s.sendMessage(plugin.config().msg("&f/challenge arena <setspawn|buildbox [teleport]|clear>"));
        s.sendMessage(plugin.config().msg("&f/challenge army <collect|endcollect|build|startfight [force]|stop|status>"));
        s.sendMessage(plugin.config().msg("&f/result [forceitem|forcemob|army] &7- Leaderboard"));
        s.sendMessage(plugin.config().msg("&f/forceitem skip &7| &f/forcemob skip"));
    }

    // ------------------------------------------------------------------ timer

    private void timer(CommandSender s, String[] a) {
        if (a.length < 2) {
            msg(s, "&cNutzung: /challenge timer <start|pause|resume|stop|reset|settime|direction>");
            return;
        }
        TimerManager t = plugin.timer();
        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                MonsterArmyManager.Phase ph = plugin.challenges().army().phase();
                if (plugin.challenges().isActive(ChallengeMode.ARMYBATTLE)
                        && (ph == MonsterArmyManager.Phase.BUILD || ph == MonsterArmyManager.Phase.FIGHT)) {
                    msg(s, "&cArmy Battle laeuft: nutze &f/challenge army ...&c.");
                } else if (t.getState() == TimerManager.State.PAUSED) {
                    t.resume();
                    plugin.config().broadcast("&aTimer fortgesetzt.");
                } else if (t.getState() == TimerManager.State.RUNNING) {
                    msg(s, "&cDer Timer laeuft bereits.");
                } else {
                    plugin.challenges().start();
                }
            }
            case "pause" -> {
                t.pause();
                plugin.config().broadcast("&eTimer pausiert.");
            }
            case "resume" -> {
                t.resume();
                plugin.config().broadcast("&aTimer fortgesetzt.");
            }
            case "stop" -> {
                t.finish();
                plugin.challenges().onTimerFinished();
            }
            case "reset" -> plugin.challenges().reset();
            case "settime" -> {
                if (a.length < 3) {
                    msg(s, "&cNutzung: /challenge timer settime <sekunden | mm:ss | hh:mm:ss>");
                    return;
                }
                try {
                    long sec = parseSeconds(a[2]);
                    t.setTime(sec);
                    msg(s, "&7Zeit gesetzt auf &e" + TimerManager.formatSeconds(sec) + "&7.");
                } catch (NumberFormatException ex) {
                    msg(s, "&cUngueltige Zeit: &f" + a[2]);
                }
            }
            case "direction" -> {
                if (a.length < 3) {
                    msg(s, "&cNutzung: /challenge timer direction <up|down>");
                    return;
                }
                boolean down = a[2].equalsIgnoreCase("down") || a[2].equalsIgnoreCase("countdown");
                t.setCountdown(down);
                plugin.getConfig().set("timer.direction", down ? "COUNTDOWN" : "COUNTUP");
                plugin.saveConfig();
                msg(s, "&7Timer-Richtung: &e" + (down ? "Countdown" : "Countup"));
            }
            default -> msg(s, "&cUnbekannte Timer-Aktion.");
        }
    }

    private static long parseSeconds(String in) {
        long total = 0;
        for (String part : in.split(":")) total = total * 60 + Long.parseLong(part.trim());
        return total;
    }

    // ------------------------------------------------------------------ mode / randomizer

    private static Boolean onOff(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "on", "an", "true" -> Boolean.TRUE;
            case "off", "aus", "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    private void mode(CommandSender s, String[] a) {
        if (a.length < 3) {
            for (ChallengeMode m : ChallengeMode.values()) {
                msg(s, "&f" + m.key() + "&7: " + (plugin.challenges().isActive(m) ? "&aAN" : "&cAUS"));
            }
            msg(s, "&7Aendern: /challenge mode <modus> <on|off>");
            return;
        }
        ChallengeMode m = ChallengeMode.fromKey(a[1]);
        Boolean on = onOff(a[2]);
        if (m == null || on == null) {
            msg(s, "&cNutzung: /challenge mode <forceitem|forcemob|armybattle|forceskill> <on|off>");
            return;
        }
        plugin.challenges().setMode(m, on);
        msg(s, "&7Modus &f" + m.display() + "&7: " + (on ? "&aAN" : "&cAUS"));
        if (m == ChallengeMode.FORCESKILL && on) {
            msg(s, "&eHinweis: Force Skill ist noch nicht implementiert (reserviert).");
        }
    }

    private void randomizer(CommandSender s, String[] a) {
        var cm = plugin.challenges();
        if (a.length == 1) {
            msg(s, "&7Randomizer global: " + (cm.isRandomizerGlobal() ? "&aAN" : "&cAUS") + " &7(Seed &f" + cm.randomizer().seed() + "&7)");
            for (ChallengeMode m : ChallengeMode.values()) {
                msg(s, " &f" + m.key() + "&7: " + (cm.getRandomizerFlag(m) ? "&aAN" : "&cAUS"));
            }
            return;
        }
        if (a[1].equalsIgnoreCase("seed")) {
            long seed = 0;
            try {
                if (a.length >= 3) seed = Long.parseLong(a[2]);
            } catch (NumberFormatException ex) {
                msg(s, "&cSeed muss eine Zahl sein.");
                return;
            }
            cm.randomizer().reseed(seed);
            msg(s, "&7Neuer Randomizer-Seed: &f" + cm.randomizer().seed());
            return;
        }
        if (a.length == 2) {
            Boolean on = onOff(a[1]);
            if (on == null) {
                msg(s, "&cNutzung: /challenge randomizer <on|off>");
                return;
            }
            cm.setRandomizerGlobal(on);
            plugin.config().broadcast("&7Randomizer: " + (on ? "&aAN" : "&cAUS"));
            return;
        }
        ChallengeMode m = ChallengeMode.fromKey(a[1]);
        Boolean on = onOff(a[2]);
        if (m == null || on == null) {
            msg(s, "&cNutzung: /challenge randomizer <modus> <on|off>");
            return;
        }
        cm.setRandomizerFlag(m, on);
        msg(s, "&7Randomizer fuer &f" + m.display() + "&7: " + (on ? "&aAN" : "&cAUS"));
    }

    // ------------------------------------------------------------------ arena

    private void arena(CommandSender s, String[] a) {
        if (a.length < 2) {
            msg(s, "&cNutzung: /challenge arena <setspawn|buildbox [teleport]|clear>");
            return;
        }
        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "setspawn" -> {
                if (!(s instanceof Player p)) {
                    msg(s, "&cNur ingame.");
                    return;
                }
                plugin.arenas().setOrigin(p.getLocation());
                msg(s, "&aArena-Origin gesetzt. &7Die Boxen werden ab hier in +X-Richtung gebaut (Boden-Hoehe = deine Fuss-Hoehe).");
            }
            case "buildbox" -> {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR);
                boolean tp = a.length >= 3 && a[2].equalsIgnoreCase("teleport");
                if (plugin.arenas().buildBoxes(players, tp, true)) {
                    msg(s, "&aBoxen gebaut fuer &e" + players.size() + " &aSpieler.");
                } else {
                    msg(s, "&cErst &f/challenge arena setspawn &cnutzen.");
                }
            }
            case "clear" -> {
                plugin.arenas().clearWalls();
                msg(s, "&7Waende entfernt.");
            }
            default -> msg(s, "&cNutzung: /challenge arena <setspawn|buildbox [teleport]|clear>");
        }
    }

    // ------------------------------------------------------------------ army

    private void army(CommandSender s, String[] a) {
        if (a.length < 2) {
            msg(s, "&cNutzung: /challenge army <collect|endcollect|build|startfight [force]|stop|status>");
            return;
        }
        MonsterArmyManager army = plugin.challenges().army();
        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "collect" -> {
                if (plugin.timer().getState() != TimerManager.State.STOPPED) {
                    msg(s, "&cTimer erst stoppen/zuruecksetzen (/challenge timer reset).");
                    return;
                }
                if (!plugin.challenges().isActive(ChallengeMode.ARMYBATTLE)) {
                    plugin.challenges().setMode(ChallengeMode.ARMYBATTLE, true);
                }
                army.stop();
                plugin.challenges().start();
            }
            case "endcollect" -> {
                if (army.phase() != MonsterArmyManager.Phase.COLLECT) {
                    msg(s, "&cEs laeuft keine Sammelphase.");
                    return;
                }
                army.endCollect();
            }
            case "build" -> {
                if (s instanceof Player p) army.openBuilder(p);
                else msg(s, "&cNur ingame.");
            }
            case "startfight" -> army.startFight(s, a.length >= 3 && a[2].equalsIgnoreCase("force"));
            case "stop" -> {
                army.stop();
                msg(s, "&7Army Battle zurueckgesetzt.");
            }
            case "status" -> army.status(s);
            default -> msg(s, "&cUnbekannte Army-Aktion.");
        }
    }

    // ------------------------------------------------------------------ Tab-Completion

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        List<String> modes = List.of("forceitem", "forcemob", "armybattle", "forceskill");
        List<String> onoff = List.of("on", "off");
        if (a.length == 1) {
            return filter(List.of("timer", "mode", "randomizer", "settings", "arena", "army", "help"), a[0]);
        }
        if (a.length == 2) {
            return switch (a[0].toLowerCase(Locale.ROOT)) {
                case "timer" -> filter(List.of("start", "pause", "resume", "stop", "reset", "settime", "direction"), a[1]);
                case "mode" -> filter(modes, a[1]);
                case "randomizer" -> {
                    List<String> opts = new ArrayList<>(onoff);
                    opts.add("seed");
                    opts.addAll(modes);
                    yield filter(opts, a[1]);
                }
                case "arena" -> filter(List.of("setspawn", "buildbox", "clear"), a[1]);
                case "army" -> filter(List.of("collect", "endcollect", "build", "startfight", "stop", "status"), a[1]);
                default -> List.of();
            };
        }
        if (a.length == 3) {
            String sub = a[0].toLowerCase(Locale.ROOT);
            if (sub.equals("mode") || (sub.equals("randomizer") && ChallengeMode.fromKey(a[1]) != null)) return filter(onoff, a[2]);
            if (sub.equals("timer") && a[1].equalsIgnoreCase("direction")) return filter(List.of("up", "down"), a[2]);
            if (sub.equals("arena") && a[1].equalsIgnoreCase("buildbox")) return filter(List.of("teleport"), a[2]);
            if (sub.equals("army") && a[1].equalsIgnoreCase("startfight")) return filter(List.of("force"), a[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(p)) out.add(o);
        return out;
    }
}
