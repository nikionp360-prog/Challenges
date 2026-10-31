package dev.challenges.basti.commands;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.gui.ResultGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/** /result [forceitem|forcemob|army] - oeffnet das Leaderboard-GUI. */
public final class ResultCommand implements CommandExecutor, TabCompleter {
    private final BastiPlugin plugin;

    public ResultCommand(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        if (!(s instanceof Player p)) {
            plugin.config().send(s, "&cNur ingame.");
            return true;
        }
        ResultGui.Type type = null;
        if (a.length >= 1) {
            type = switch (a[0].toLowerCase(Locale.ROOT)) {
                case "forceitem", "item" -> ResultGui.Type.FORCEITEM;
                case "forcemob", "mob" -> ResultGui.Type.FORCEMOB;
                case "army", "armybattle" -> ResultGui.Type.ARMY;
                default -> null;
            };
        }
        if (type == null) type = plugin.challenges().defaultResultType();
        if (type == null) {
            plugin.config().send(p, "&cNoch keine Ergebnisse vorhanden.");
            return true;
        }
        new ResultGui(plugin, type).open(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        return a.length == 1 ? List.of("forceitem", "forcemob", "army") : List.of();
    }
}
