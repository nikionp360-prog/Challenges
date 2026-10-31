package dev.challenges.basti.gui;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.challenges.MonsterArmyManager;
import dev.challenges.basti.utils.Items;
import dev.challenges.basti.utils.MobPool;
import dev.challenges.basti.utils.Text;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chest-GUI zum Verteilen der gesammelten Mobs auf genau 3 Wellen.
 * Oben: Welle 1-3 (Slots 0-2), Mitte: Mob-Speicher (Slots 9-44), unten: Navigation.
 * Linksklick +1 (Shift +5), Rechtsklick -1 (Shift -5).
 */
public final class WaveBuilderGui extends BaseGui {
    private static final int PER_PAGE = 36;

    private final UUID owner;
    private int wave = 0;
    private int page = 0;
    private List<EntityType> types = new ArrayList<>();

    public WaveBuilderGui(BastiPlugin plugin, Player p) {
        super(plugin);
        this.owner = p.getUniqueId();
        create(54, Text.c("&8Deine Monster-Wellen"));
        render();
    }

    private MonsterArmyManager army() {
        return plugin.challenges().army();
    }

    private void render() {
        MonsterArmyManager army = army();
        inv.clear();

        types = new ArrayList<>(army.storageOf(owner).keySet());
        types.sort((a, b) -> MobPool.key(a).compareTo(MobPool.key(b)));

        for (int w = 0; w < MonsterArmyManager.WAVES; w++) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Mobs: &e" + army.waveSize(owner, w) + "&7/" + army.maxPerWave());
            for (Map.Entry<EntityType, Integer> e : army.getWaves(owner).get(w).entrySet()) {
                lore.add("&8- &f" + e.getValue() + "x " + Text.pretty(MobPool.key(e.getKey())));
            }
            Material m = (w == wave) ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
            inv.setItem(w, Items.of(m, (w == wave ? "&a" : "&7") + "Welle " + (w + 1), lore.toArray(new String[0])));
        }
        inv.setItem(4, Items.of(Material.BOOK, "&eBearbeitest: Welle " + (wave + 1),
                "&7Klicke einen Mob unten, um ihn", "&7in diese Welle zu legen."));
        inv.setItem(8, Items.of(Material.EMERALD, "&aFertig", "&7Fenster schliessen"));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < types.size(); i++) {
            EntityType t = types.get(start + i);
            int stored = army.stored(owner, t);
            int used = army.used(owner, t);
            int in = army.inWave(owner, wave, t);
            inv.setItem(9 + i, Items.egg(t, stored - used, "&f" + Text.pretty(MobPool.key(t)),
                    "&7Gesammelt: &e" + stored,
                    "&7Noch frei: &e" + (stored - used),
                    "&7In Welle " + (wave + 1) + ": &e" + in,
                    "",
                    "&aLinksklick &7+1 &8(Shift: +5)",
                    "&cRechtsklick &7-1 &8(Shift: -5)"));
        }
        if (types.isEmpty()) {
            inv.setItem(22, Items.of(Material.BARRIER, "&cDu hast keine Mobs gesammelt"));
        }
        if (page > 0) inv.setItem(45, Items.of(Material.ARROW, "&eZurueck"));
        inv.setItem(49, Items.of(Material.BARRIER, "&cWelle " + (wave + 1) + " leeren"));
        if (start + PER_PAGE < types.size()) inv.setItem(53, Items.of(Material.ARROW, "&eWeiter"));
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (army().phase() != MonsterArmyManager.Phase.BUILD) {
            p.closeInventory();
            return;
        }
        int slot = e.getRawSlot();
        if (slot >= 0 && slot < MonsterArmyManager.WAVES) {
            wave = slot;
        } else if (slot == 8) {
            p.closeInventory();
            plugin.config().send(p, "&aWellen gespeichert: &e" + army().waveSize(owner, 0) + "/"
                    + army().waveSize(owner, 1) + "/" + army().waveSize(owner, 2) + " &7Mobs.");
            return;
        } else if (slot == 45 && page > 0) {
            page--;
        } else if (slot == 53 && (page + 1) * PER_PAGE < types.size()) {
            page++;
        } else if (slot == 49) {
            army().clearWave(owner, wave);
        } else if (slot >= 9 && slot < 9 + PER_PAGE) {
            int idx = page * PER_PAGE + (slot - 9);
            if (idx < types.size()) {
                int amount = e.isShiftClick() ? 5 : 1;
                if (e.isRightClick()) amount = -amount;
                army().change(owner, wave, types.get(idx), amount);
            }
        } else {
            return;
        }
        render();
    }
}
