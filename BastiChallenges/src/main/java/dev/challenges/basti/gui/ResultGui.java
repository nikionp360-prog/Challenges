package dev.challenges.basti.gui;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.challenges.ForceItemChallenge;
import dev.challenges.basti.challenges.ForceMobChallenge;
import dev.challenges.basti.challenges.MonsterArmyManager;
import dev.challenges.basti.utils.Items;
import dev.challenges.basti.utils.MobPool;
import dev.challenges.basti.utils.Text;
import dev.challenges.basti.utils.TimerManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /result: Leaderboard (Platz 1, 2, 3 ...) mit Spielerkopf + den ersten gefundenen Items.
 * Klick auf den Kopf oeffnet die Detailansicht mit ALLEN gefundenen Items/Mobs (mit Seiten).
 */
public final class ResultGui extends BaseGui {
    public enum Type {
        FORCEITEM("Force Item"), FORCEMOB("Force Mob"), ARMY("Monster Army");

        private final String display;
        Type(String display) { this.display = display; }
        public String display() { return display; }
    }

    private record Row(UUID id, String name, String scoreText, List<ItemStack> icons) {}

    private static final int PAGE_SIZE = 45;

    private final Type type;
    private final List<Row> rows = new ArrayList<>();
    private Row detail;
    private int page;

    public ResultGui(BastiPlugin plugin, Type type) {
        super(plugin);
        this.type = type;
        create(54, Text.c("&8Ergebnis: " + type.display()));
        loadRows();
        renderLeaderboard();
    }

    // ------------------------------------------------------------------ Daten

    private void loadRows() {
        switch (type) {
            case FORCEITEM -> {
                List<Map.Entry<UUID, List<ForceItemChallenge.Found>>> entries =
                        new ArrayList<>(plugin.challenges().forceItem().getFound().entrySet());
                entries.sort(rankComparator(ForceItemChallenge.Found::millis));
                for (var en : entries) {
                    List<ItemStack> icons = new ArrayList<>();
                    int n = 1;
                    for (ForceItemChallenge.Found f : en.getValue()) {
                        icons.add(Items.withLore(f.item(), "&7#" + n++ + " nach &e" + TimerManager.formatMillis(f.millis())));
                    }
                    rows.add(new Row(en.getKey(), Text.name(en.getKey()), en.getValue().size() + " Items", icons));
                }
            }
            case FORCEMOB -> {
                List<Map.Entry<UUID, List<ForceMobChallenge.Found>>> entries =
                        new ArrayList<>(plugin.challenges().forceMob().getFound().entrySet());
                entries.sort(rankComparator(ForceMobChallenge.Found::millis));
                for (var en : entries) {
                    List<ItemStack> icons = new ArrayList<>();
                    int n = 1;
                    for (ForceMobChallenge.Found f : en.getValue()) {
                        icons.add(Items.egg(f.type(), 1, null, "&7#" + n++ + " nach &e" + TimerManager.formatMillis(f.millis())));
                    }
                    rows.add(new Row(en.getKey(), Text.name(en.getKey()), en.getValue().size() + " Mobs", icons));
                }
            }
            case ARMY -> {
                MonsterArmyManager army = plugin.challenges().army();
                for (MonsterArmyManager.Fight f : army.ranking()) {
                    List<ItemStack> icons = new ArrayList<>();
                    List<Map<EntityType, Integer>> waves = army.getWaves(f.player);
                    for (int w = 0; w < waves.size(); w++) {
                        for (Map.Entry<EntityType, Integer> e : waves.get(w).entrySet()) {
                            icons.add(Items.egg(e.getKey(), e.getValue(), null,
                                    "&7Welle &e" + (w + 1) + "&7: &f" + e.getValue() + "x " + Text.pretty(MobPool.key(e.getKey()))));
                        }
                    }
                    String score = f.finished
                            ? "&a" + TimerManager.formatPrecise(f.finishMillis)
                            : "&cnicht fertig";
                    rows.add(new Row(f.player, Text.name(f.player), score, icons));
                }
            }
        }
    }

    /** Meiste Funde zuerst; bei Gleichstand gewinnt, wer seinen letzten Fund frueher hatte. */
    private static <F> Comparator<Map.Entry<UUID, List<F>>> rankComparator(java.util.function.ToLongFunction<F> millis) {
        return (a, b) -> {
            int c = Integer.compare(b.getValue().size(), a.getValue().size());
            if (c != 0) return c;
            long la = a.getValue().isEmpty() ? Long.MAX_VALUE : millis.applyAsLong(a.getValue().get(a.getValue().size() - 1));
            long lb = b.getValue().isEmpty() ? Long.MAX_VALUE : millis.applyAsLong(b.getValue().get(b.getValue().size() - 1));
            return Long.compare(la, lb);
        };
    }

    // ------------------------------------------------------------------ Ansichten

    private void renderLeaderboard() {
        detail = null;
        inv.clear();
        for (int r = 0; r < Math.min(6, rows.size()); r++) {
            Row row = rows.get(r);
            String color = r == 0 ? "&6" : r == 1 ? "&7" : r == 2 ? "&c" : "&f";
            inv.setItem(r * 9, Items.head(row.id(), color + "Platz " + (r + 1) + " &f- " + row.name(),
                    "&7" + row.scoreText(), "&8Klick fuer alle Details"));
            boolean more = row.icons().size() > 8;
            int limit = more ? 7 : row.icons().size();
            for (int i = 0; i < limit; i++) inv.setItem(r * 9 + 1 + i, row.icons().get(i));
            if (more) {
                inv.setItem(r * 9 + 8, Items.of(Material.PAPER, "&e+" + (row.icons().size() - 7) + " weitere",
                        "&7Klick auf den Kopf"));
            }
        }
        if (rows.isEmpty()) {
            inv.setItem(22, Items.of(Material.BARRIER, "&cNoch keine Ergebnisse"));
        }
    }

    private void renderDetail() {
        inv.clear();
        List<ItemStack> icons = detail.icons();
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < icons.size(); i++) inv.setItem(i, icons.get(start + i));
        inv.setItem(45, Items.of(Material.ARROW, "&eZurueck zum Leaderboard"));
        if (page > 0) inv.setItem(46, Items.of(Material.SPECTRAL_ARROW, "&eVorherige Seite"));
        inv.setItem(49, Items.head(detail.id(), "&f" + detail.name(), "&7" + detail.scoreText(),
                "&7Seite &e" + (page + 1) + "&7/" + Math.max(1, (icons.size() + PAGE_SIZE - 1) / PAGE_SIZE)));
        if (start + PAGE_SIZE < icons.size()) inv.setItem(52, Items.of(Material.SPECTRAL_ARROW, "&eNaechste Seite"));
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (detail == null) {
            int r = slot / 9;
            if (slot % 9 == 0 && r < Math.min(6, rows.size())) {
                detail = rows.get(r);
                page = 0;
                renderDetail();
            }
            return;
        }
        if (slot == 45) {
            renderLeaderboard();
        } else if (slot == 46 && page > 0) {
            page--;
            renderDetail();
        } else if (slot == 52 && (page + 1) * PAGE_SIZE < detail.icons().size()) {
            page++;
            renderDetail();
        }
    }

    public static void openForAll(BastiPlugin plugin, Type type) {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) new ResultGui(plugin, type).open(p);
    }
}
