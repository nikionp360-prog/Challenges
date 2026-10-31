package dev.challenges.basti.utils;

import dev.challenges.basti.BastiPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Duenner Wrapper um die config.yml + Nachrichten-Helfer. */
public final class ConfigManager {
    private final BastiPlugin plugin;

    public ConfigManager(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public void save() {
        plugin.saveConfig();
    }

    public Component msg(String text) {
        return Text.c(cfg().getString("prefix", "&6[Challenge] &7") + text);
    }

    public void send(CommandSender to, String text) {
        to.sendMessage(msg(text));
    }

    public void broadcast(String text) {
        Bukkit.broadcast(msg(text));
    }

    public Set<String> upperSet(String path) {
        Set<String> out = new HashSet<>();
        for (String s : cfg().getStringList(path)) out.add(s.toUpperCase(Locale.ROOT));
        return out;
    }

    public Set<String> lowerSet(String path) {
        Set<String> out = new HashSet<>();
        for (String s : cfg().getStringList(path)) out.add(s.toLowerCase(Locale.ROOT));
        return out;
    }
}
