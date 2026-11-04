package fr.cheatmode;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * X-ray : pour chaque minerai proche, on fait apparaître un "block_display" glowing par-dessus.
 * Le contour glowing se voit à travers les murs, avec une couleur par minerai.
 * Aucune texture n'est modifiée, donc ça marche aussi avec Sodium et les resource packs.
 */
public final class Xray {

    private static final Map<Block, Integer> ORES = new HashMap<>();

    static {
        put(0x404040, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        put(0xD8AF93, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        put(0xE07B4F, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        put(0xFCEE4B, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        put(0xFF2020, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        put(0x2040FF, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        put(0x20E060, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        put(0x4AEDD9, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        put(0xF0F0F0, Blocks.NETHER_QUARTZ_ORE);
        put(0x8B4513, Blocks.ANCIENT_DEBRIS);
        put(0xB030FF, Blocks.SPAWNER);
    }

    private static void put(int color, Block... blocks) {
        for (Block b : blocks) ORES.put(b, color);
    }

    private static final int MAX = 350;

    private final Map<BlockPos, Block> shown = new HashMap<>();
    private RegistryKey<World> dimension;
    /** Identifiant de session : permet de nettoyer les restes d'une partie précédente. */
    private String session = newSession();

    private static String newSession() {
        return Long.toString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE, 36);
    }

    public void refresh(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        List<String> cmds = new ArrayList<>();

        if (mc.world.getRegistryKey() != dimension) {
            clear();
            dimension = mc.world.getRegistryKey();
        }

        // 1) Minerais autour du joueur (le client connaît déjà les blocs).
        final int RADIUS = (int) Settings.INSTANCE.xrayRadius;
        BlockPos center = mc.player.getBlockPos();
        Map<BlockPos, Block> found = new HashMap<>();
        BlockPos.Mutable p = new BlockPos.Mutable();
        scan:
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (mc.world.isOutOfHeightLimit(p.getY())) continue;
                    Block b = mc.world.getBlockState(p).getBlock();
                    if (ORES.containsKey(b)) {
                        found.put(p.toImmutable(), b);
                        if (found.size() >= MAX) break scan;
                    }
                }
            }
        }

        // 2) Retire ceux qui ont été minés ou qui sont trop loin.
        Iterator<Map.Entry<BlockPos, Block>> it = shown.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Block> e = it.next();
            if (found.get(e.getKey()) != e.getValue()) {
                cmds.add("kill @e[type=minecraft:block_display,tag=" + tag(e.getKey()) + "]");
                it.remove();
            }
        }

        // 3) Ajoute les nouveaux.
        for (Map.Entry<BlockPos, Block> e : found.entrySet()) {
            if (shown.containsKey(e.getKey())) continue;
            BlockPos b = e.getKey();
            cmds.add(String.format(Locale.ROOT,
                    "summon minecraft:block_display %d %d %d {block_state:{Name:\"%s\"},Glowing:1b,"
                            + "glow_color_override:%d,brightness:{sky:15,block:15},"
                            + "Tags:[\"cmx\",\"cmxs_%s\",\"%s\"],"
                            + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                            + "translation:[0.02f,0.02f,0.02f],scale:[0.96f,0.96f,0.96f]}}",
                    b.getX(), b.getY(), b.getZ(), Registries.BLOCK.getId(e.getValue()),
                    ORES.get(e.getValue()), session, tag(b)));
            shown.put(b, e.getValue());
        }

        // Restes d'anciennes sessions (chunks qui étaient déchargés au moment du nettoyage).
        cmds.add("kill @e[type=minecraft:block_display,tag=cmx,tag=!cmxs_" + session + "]");
        Server.commands(cmds);
    }

    public void clear() {
        shown.clear();
        Server.command("kill @e[type=minecraft:block_display,tag=cmx]");
    }

    /** Nouvelle partie : on oublie tout sans envoyer de commande. */
    public void reset() {
        shown.clear();
        dimension = null;
        session = newSession();
    }

    private static String tag(BlockPos p) {
        return ("cmx_" + p.getX() + "_" + p.getY() + "_" + p.getZ()).replace('-', 'n');
    }
}
