package fr.cheatmode;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
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
 * Surbrillance de blocs à travers les murs : sur chaque bloc trouvé, on fait apparaître un
 * "block_display" glowing (contour de couleur visible à travers tout). Aucune texture n'est
 * modifiée, donc ça marche aussi avec Sodium et les resource packs.
 */
public abstract class BlockEsp {

    private final String tag;
    private final int max;
    private final Map<BlockPos, Block> shown = new HashMap<>();
    private RegistryKey<World> dimension;
    /** Identifiant de session : permet de nettoyer les restes d'une partie précédente. */
    private String session = newSession();

    protected BlockEsp(String tag, int max) {
        this.tag = tag;
        this.max = max;
    }

    /** Remplit {@code out} avec les blocs à surligner (au plus {@code max}). */
    protected abstract void scan(MinecraftClient mc, Map<BlockPos, Block> out, int max);

    protected abstract int color(Block b);

    /** Id du bloc affiché dans le display. */
    protected abstract String displayBlock(Block b);

    /** Taille du display (1 = bloc entier). */
    protected abstract float scale();

    public void refresh(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        List<String> cmds = new ArrayList<>();
        if (mc.world.getRegistryKey() != dimension) {
            clear();
            dimension = mc.world.getRegistryKey();
        }

        Map<BlockPos, Block> found = new HashMap<>();
        scan(mc, found, max);

        Iterator<Map.Entry<BlockPos, Block>> it = shown.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Block> e = it.next();
            if (found.get(e.getKey()) != e.getValue()) {
                cmds.add("kill @e[type=minecraft:block_display,tag=" + posTag(e.getKey()) + "]");
                it.remove();
            }
        }

        float sc = scale(), off = (1 - sc) / 2;
        for (Map.Entry<BlockPos, Block> e : found.entrySet()) {
            if (shown.containsKey(e.getKey())) continue;
            BlockPos b = e.getKey();
            cmds.add(String.format(Locale.ROOT,
                    "summon minecraft:block_display %d %d %d {block_state:{Name:\"%s\"},Glowing:1b,"
                            + "glow_color_override:%d,brightness:{sky:15,block:15},"
                            + "Tags:[\"%s\",\"%ss_%s\",\"%s\"],"
                            + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                            + "translation:[%.3ff,%.3ff,%.3ff],scale:[%.3ff,%.3ff,%.3ff]}}",
                    b.getX(), b.getY(), b.getZ(), displayBlock(e.getValue()), color(e.getValue()),
                    tag, tag, session, posTag(b), off, off, off, sc, sc, sc));
            shown.put(b, e.getValue());
        }

        // Restes d'anciennes sessions (chunks qui étaient déchargés au moment du nettoyage).
        cmds.add("kill @e[type=minecraft:block_display,tag=" + tag + ",tag=!" + tag + "s_" + session + "]");
        Server.commands(cmds);
    }

    public void clear() {
        shown.clear();
        Server.command("kill @e[type=minecraft:block_display,tag=" + tag + "]");
    }

    /** Nouvelle partie : on oublie tout sans envoyer de commande. */
    public void reset() {
        shown.clear();
        dimension = null;
        session = newSession();
    }

    private String posTag(BlockPos p) {
        return (tag + "_" + p.getX() + "_" + p.getY() + "_" + p.getZ()).replace('-', 'n');
    }

    private static String newSession() {
        return Long.toString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE, 36);
    }
}
