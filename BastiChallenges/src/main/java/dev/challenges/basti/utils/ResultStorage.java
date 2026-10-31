package dev.challenges.basti.utils;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.challenges.ForceItemChallenge;
import dev.challenges.basti.challenges.ForceMobChallenge;
import dev.challenges.basti.challenges.MonsterArmyManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/** Speichert Ergebnisse in plugins/BastiChallenges/results.yml (Datei-I/O asynchron). */
public final class ResultStorage {
    private ResultStorage() {}

    public static void saveAsync(BastiPlugin plugin) {
        // Daten im Main-Thread in ein eigenes YamlConfiguration kopieren ...
        YamlConfiguration yaml = new YamlConfiguration();
        ForceItemChallenge fi = plugin.challenges().forceItem();
        ForceMobChallenge fm = plugin.challenges().forceMob();
        MonsterArmyManager army = plugin.challenges().army();

        fi.getFound().forEach((id, list) -> {
            yaml.set("forceitem." + id + ".name", Text.name(id));
            yaml.set("forceitem." + id + ".items",
                    list.stream().map(f -> f.item().name() + "@" + f.millis()).toList());
        });
        fm.getFound().forEach((id, list) -> {
            yaml.set("forcemob." + id + ".name", Text.name(id));
            yaml.set("forcemob." + id + ".mobs",
                    list.stream().map(f -> MobPool.key(f.type()) + "@" + f.millis()).toList());
        });
        for (MonsterArmyManager.Fight f : army.ranking()) {
            String base = "army." + f.player;
            yaml.set(base + ".name", Text.name(f.player));
            yaml.set(base + ".finished", f.finished);
            yaml.set(base + ".time-millis", f.finished ? f.finishMillis : -1L);
            yaml.set(base + ".penalty-millis", f.penaltyMillis);
        }

        // ... und das eigentliche Schreiben in einem Async-Task erledigen.
        File file = new File(plugin.getDataFolder(), "results.yml");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDataFolder().mkdirs();
                yaml.save(file);
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte results.yml nicht speichern: " + ex.getMessage());
            }
        });
    }
}
