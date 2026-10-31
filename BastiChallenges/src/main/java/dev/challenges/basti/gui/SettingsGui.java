package dev.challenges.basti.gui;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.challenges.ChallengeManager;
import dev.challenges.basti.challenges.ChallengeMode;
import dev.challenges.basti.utils.Items;
import dev.challenges.basti.utils.Text;
import dev.challenges.basti.utils.TimerManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;

/** /challenge settings - Live-Einstellungen per Klick. */
public final class SettingsGui extends BaseGui {
    private static final ChallengeMode[] MODES = ChallengeMode.values();
    private static final Material[] MODE_ICONS = {Material.CHEST, Material.ZOMBIE_HEAD, Material.IRON_SWORD, Material.BOOK};
    private static final int MODE_SLOT_START = 13;

    public SettingsGui(BastiPlugin plugin) {
        super(plugin);
        create(27, Text.c("&8Challenge-Einstellungen"));
        render();
    }

    private static String onOff(boolean b) {
        return b ? "&aAN" : "&cAUS";
    }

    private void render() {
        ChallengeManager cm = plugin.challenges();
        TimerManager t = plugin.timer();
        FileConfiguration c = plugin.getConfig();
        inv.clear();

        inv.setItem(10, Items.of(Material.CLOCK, "&eTimer-Richtung: &f" + (t.isCountdown() ? "Countdown" : "Countup"),
                "&7Klick zum Wechseln"));
        boolean freeze = c.getBoolean("timer.freeze-players-on-pause", true);
        inv.setItem(11, Items.of(freeze ? Material.LIME_DYE : Material.GRAY_DYE,
                "&ePause friert Spieler ein: " + onOff(freeze), "&7Klick zum Umschalten"));
        inv.setItem(12, Items.of(cm.isRandomizerGlobal() ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eRandomizer (global): " + onOff(cm.isRandomizerGlobal()), "&7Klick zum Umschalten"));

        for (int i = 0; i < MODES.length; i++) {
            ChallengeMode m = MODES[i];
            inv.setItem(MODE_SLOT_START + i, Items.of(MODE_ICONS[i],
                    "&6" + m.display() + ": " + onOff(cm.isActive(m)),
                    "&7Linksklick: Modus an/aus",
                    "&7Rechtsklick: Randomizer fuer diesen Modus: " + onOff(cm.getRandomizerFlag(m))));
        }

        long collect = c.getLong("army-battle.collect-seconds", 7200L);
        inv.setItem(19, Items.of(Material.RED_CONCRETE, "&c-15 Min Sammelphase"));
        inv.setItem(20, Items.of(Material.IRON_SWORD, "&cArmy-Sammelphase: &f" + TimerManager.formatSeconds(collect)));
        inv.setItem(21, Items.of(Material.LIME_CONCRETE, "&a+15 Min Sammelphase"));

        long cd = c.getLong("timer.countdown-seconds", 3600L);
        inv.setItem(23, Items.of(Material.RED_CONCRETE, "&c-10 Min Countdown"));
        inv.setItem(24, Items.of(Material.CLOCK, "&eCountdown-Startwert: &f" + TimerManager.formatSeconds(cd)));
        inv.setItem(25, Items.of(Material.LIME_CONCRETE, "&a+10 Min Countdown"));
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        ChallengeManager cm = plugin.challenges();
        TimerManager t = plugin.timer();
        FileConfiguration c = plugin.getConfig();
        int slot = e.getRawSlot();

        if (slot == 10) {
            t.setCountdown(!t.isCountdown());
            c.set("timer.direction", t.isCountdown() ? "COUNTDOWN" : "COUNTUP");
        } else if (slot == 11) {
            c.set("timer.freeze-players-on-pause", !c.getBoolean("timer.freeze-players-on-pause", true));
        } else if (slot == 12) {
            cm.setRandomizerGlobal(!cm.isRandomizerGlobal());
        } else if (slot >= MODE_SLOT_START && slot < MODE_SLOT_START + MODES.length) {
            ChallengeMode m = MODES[slot - MODE_SLOT_START];
            if (e.isRightClick()) cm.setRandomizerFlag(m, !cm.getRandomizerFlag(m));
            else cm.setMode(m, !cm.isActive(m));
        } else if (slot == 19 || slot == 21) {
            long v = c.getLong("army-battle.collect-seconds", 7200L) + (slot == 21 ? 900 : -900);
            c.set("army-battle.collect-seconds", Math.max(900L, v));
        } else if (slot == 23 || slot == 25) {
            long v = c.getLong("timer.countdown-seconds", 3600L) + (slot == 25 ? 600 : -600);
            v = Math.max(600L, v);
            c.set("timer.countdown-seconds", v);
            if (t.getState() == TimerManager.State.STOPPED) t.setCountdownSeconds(v);
        } else {
            return;
        }
        plugin.saveConfig();
        render();
    }
}
