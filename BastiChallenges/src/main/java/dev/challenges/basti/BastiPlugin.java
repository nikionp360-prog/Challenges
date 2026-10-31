package dev.challenges.basti;

import dev.challenges.basti.challenges.ArenaManager;
import dev.challenges.basti.challenges.ChallengeManager;
import dev.challenges.basti.commands.ChallengeCommand;
import dev.challenges.basti.commands.ResultCommand;
import dev.challenges.basti.commands.SkipCommand;
import dev.challenges.basti.listeners.BlockBreakListener;
import dev.challenges.basti.listeners.CraftListener;
import dev.challenges.basti.listeners.EntityDeathListener;
import dev.challenges.basti.listeners.FreezeListener;
import dev.challenges.basti.listeners.InventoryClickListener;
import dev.challenges.basti.listeners.PlayerPickupListener;
import dev.challenges.basti.utils.ConfigManager;
import dev.challenges.basti.utils.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class BastiPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private TimerManager timer;
    private ArenaManager arenas;
    private ChallengeManager challenges;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        timer = new TimerManager(this);
        arenas = new ArenaManager(this);
        challenges = new ChallengeManager(this);

        ChallengeCommand challengeCommand = new ChallengeCommand(this);
        bind("challenge", challengeCommand, challengeCommand);
        ResultCommand resultCommand = new ResultCommand(this);
        bind("result", resultCommand, resultCommand);
        SkipCommand skipCommand = new SkipCommand(this);
        bind("forceitem", skipCommand, skipCommand);
        bind("forcemob", skipCommand, skipCommand);

        register(new FreezeListener(this));
        register(new BlockBreakListener(this));
        register(new CraftListener(this));
        register(new PlayerPickupListener(this));
        register(new InventoryClickListener(this));
        register(new EntityDeathListener(this));

        challenges.startTask();
        getLogger().info("BastiChallenges aktiviert.");
    }

    @Override
    public void onDisable() {
        if (challenges != null) challenges.shutdown();
    }

    private void bind(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand pc = getCommand(name);
        if (pc == null) {
            getLogger().warning("Befehl nicht in plugin.yml: " + name);
            return;
        }
        pc.setExecutor(executor);
        pc.setTabCompleter(completer);
    }

    private void register(Listener l) {
        Bukkit.getPluginManager().registerEvents(l, this);
    }

    public ConfigManager config() { return configManager; }
    public TimerManager timer() { return timer; }
    public ArenaManager arenas() { return arenas; }
    public ChallengeManager challenges() { return challenges; }
}
