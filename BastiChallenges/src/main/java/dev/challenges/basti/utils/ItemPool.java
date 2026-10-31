package dev.challenges.basti.utils;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Liste aller sinnvollen Survival-Items (fuer Force Item + Randomizer). */
public final class ItemPool {
    private static final Set<String> BLOCKED = Set.of(
            "BEDROCK", "BARRIER", "LIGHT", "DEBUG_STICK", "KNOWLEDGE_BOOK", "STRUCTURE_VOID",
            "STRUCTURE_BLOCK", "JIGSAW", "SPAWNER", "TRIAL_SPAWNER", "VAULT", "REINFORCED_DEEPSLATE",
            "END_PORTAL_FRAME", "PLAYER_HEAD", "WRITTEN_BOOK", "FARMLAND", "BUDDING_AMETHYST",
            "CHORUS_PLANT", "PETRIFIED_OAK_SLAB", "COMMAND_BLOCK_MINECART", "TEST_BLOCK",
            "TEST_INSTANCE_BLOCK", "HEAVY_CORE");

    private static final String[] BLOCKED_PARTS = {"SPAWN_EGG", "COMMAND_BLOCK", "INFESTED_"};

    private ItemPool() {}

    public static List<Material> build(Set<String> extraExcluded) {
        List<Material> list = new ArrayList<>();
        outer:
        for (Material m : Material.values()) {
            if (m.isLegacy() || m.isAir() || !m.isItem()) continue;
            String n = m.name();
            if (BLOCKED.contains(n) || extraExcluded.contains(n)) continue;
            for (String part : BLOCKED_PARTS) if (n.contains(part)) continue outer;
            list.add(m);
        }
        return list;
    }
}
