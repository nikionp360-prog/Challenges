package dev.challenges.basti.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/** ItemStack-Builder fuer die GUIs. */
public final class Items {
    private Items() {}

    public static ItemStack of(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Text.c(name));
        if (lore.length > 0) meta.lore(Arrays.stream(lore).map(Text::c).toList());
        it.setItemMeta(meta);
        return it;
    }

    /** Nur Lore setzen, Standardname des Items bleibt erhalten. */
    public static ItemStack withLore(Material m, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null && lore.length > 0) {
            meta.lore(Arrays.stream(lore).map(Text::c).toList());
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack head(UUID id, String name, String... lore) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        meta.displayName(Text.c(name));
        if (lore.length > 0) meta.lore(Arrays.stream(lore).map(Text::c).toList());
        it.setItemMeta(meta);
        return it;
    }

    public static Material eggMaterial(EntityType t) {
        Material m = Material.matchMaterial(MobPool.key(t).toUpperCase(Locale.ROOT) + "_SPAWN_EGG");
        return m != null ? m : Material.EGG;
    }

    /** Spawn-Ei als Icon fuer einen Mob. name == null -> Standardname. */
    public static ItemStack egg(EntityType t, int amount, String name, String... lore) {
        ItemStack it = new ItemStack(eggMaterial(t), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (name != null) meta.displayName(Text.c(name));
            if (lore.length > 0) meta.lore(Arrays.stream(lore).map(Text::c).toList());
            it.setItemMeta(meta);
        }
        return it;
    }
}
