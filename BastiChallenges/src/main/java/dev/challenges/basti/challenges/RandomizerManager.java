package dev.challenges.basti.challenges;

import dev.challenges.basti.BastiPlugin;
import dev.challenges.basti.utils.ItemPool;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Deterministische Zuordnung Material -> zufaelliges Item (pro Seed).
 * Gleicher Block/gleiches Rezept ergibt innerhalb eines Seeds immer dasselbe Item.
 */
public final class RandomizerManager {
    private final List<Material> pool;
    private final Map<Material, Material> cache = new HashMap<>();
    private long seed;

    public RandomizerManager(BastiPlugin plugin) {
        this.pool = ItemPool.build(Set.of());
        reseed(plugin.getConfig().getLong("randomizer.seed", 0L));
    }

    /** seed == 0 -> zufaelliger Seed. */
    public void reseed(long newSeed) {
        this.seed = newSeed != 0 ? newSeed : ThreadLocalRandom.current().nextLong();
        cache.clear();
    }

    public long seed() { return seed; }

    public Material map(Material in) {
        return cache.computeIfAbsent(in,
                m -> pool.get(new Random(seed ^ (m.ordinal() * 0x9E3779B97F4A7C15L)).nextInt(pool.size())));
    }
}
