package dev.challenges.basti.commands;

import dev.challenges.basti.BastiPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** /forceitem skip und /forcemob skip (limitierte Anzahl, siehe config). */
public final class SkipCommand implements CommandExecutor, TabCompleter {
    private final BastiPlugin plugin;

    public SkipCommand(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        if (!(s instanceof Player p)) {
            plugin.config().send(s, "&cNur ingame.");
            return true;
        }
        if (a.length < 1 || !a[0].equalsIgnoreCase("skip")) {
            plugin.config().send(s, "&cNutzung: /" + cmd.getName() + " skip");
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("forceitem")) plugin.challenges().forceItem().skip(p);
        else plugin.challenges().forceMob().skip(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        return a.length == 1 ? List.of("skip") : List.of();
    }
}
