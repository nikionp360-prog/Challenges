package dev.challenges.basti.utils;

import org.bukkit.Registry;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Set;

/** Hilfsfunktionen fuer Mob-Typen (arbeitet ueber die Registry, nicht ueber Enum-Konstanten). */
public final class MobPool {
    private MobPool() {}

    public static String key(EntityType t) {
        return t.getKey().getKey();
    }

    public static boolean isValid(EntityType t, Set<String> excludedLowerKeys) {
        return t.isSpawnable() && t.isAlive() && !excludedLowerKeys.contains(key(t));
    }

    public static List<EntityType> pool(Set<String> excludedLowerKeys) {
        return Registry.ENTITY_TYPE.stream().filter(t -> isValid(t, excludedLowerKeys)).toList();
    }
}
