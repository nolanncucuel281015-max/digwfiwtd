package fr.cheatmode;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Surbrillance de blocs à travers les murs. Sur chaque bloc trouvé, on ajoute dans le monde
 * du CLIENT un "block_display" invisible pour le serveur, qui reçoit un contour lumineux
 * (voir {@link Outlines}). Marche donc en solo comme en multijoueur, et le serveur ne voit rien.
 */
public abstract class BlockEsp {

    private static final Set<Integer> IDS = new HashSet<>();
    private static int nextId = -1_000_000; // ids négatifs : jamais utilisés par le serveur

    public static boolean isEspEntity(Entity e) {
        return IDS.contains(e.getId());
    }

    private record Shown(Block block, int id) {}

    private final int max;
    private final Map<BlockPos, Shown> shown = new HashMap<>();
    private ClientWorld world;

    protected BlockEsp(int max) {
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
        if (mc.world != world) { // changement de monde / dimension : les anciens displays ont disparu avec lui
            forget();
            world = mc.world;
        }

        Map<BlockPos, Block> found = new HashMap<>();
        scan(mc, found, max);

        Iterator<Map.Entry<BlockPos, Shown>> it = shown.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Shown> e = it.next();
            if (found.get(e.getKey()) != e.getValue().block()) {
                remove(e.getValue().id());
                it.remove();
            }
        }

        for (Map.Entry<BlockPos, Block> e : found.entrySet()) {
            if (shown.containsKey(e.getKey())) continue;
            Entity display = create(e.getKey(), e.getValue());
            if (display == null) continue;
            world.addEntity(display);
            IDS.add(display.getId());
            shown.put(e.getKey(), new Shown(e.getValue(), display.getId()));
        }
    }

    private Entity create(BlockPos pos, Block block) {
        try {
            float sc = scale(), off = (1 - sc) / 2;
            NbtCompound nbt = StringNbtReader.parse(String.format(Locale.ROOT,
                    "{block_state:{Name:\"%s\"},Glowing:1b,glow_color_override:%d,brightness:{sky:15,block:15},"
                            + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                            + "translation:[%.3ff,%.3ff,%.3ff],scale:[%.3ff,%.3ff,%.3ff]}}",
                    displayBlock(block), color(block), off, off, off, sc, sc, sc));
            DisplayEntity.BlockDisplayEntity d = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
            d.readNbt(nbt);
            d.setId(nextId--);
            d.setPosition(pos.getX(), pos.getY(), pos.getZ());
            return d;
        } catch (Exception ex) {
            return null;
        }
    }

    private void remove(int id) {
        IDS.remove(id);
        if (world != null) world.removeEntity(id, Entity.RemovalReason.DISCARDED);
    }

    public void clear() {
        for (Shown s : shown.values()) remove(s.id());
        shown.clear();
    }

    /** Nouveau monde : on oublie tout (les entités sont parties avec l'ancien monde). */
    public void forget() {
        for (Shown s : shown.values()) IDS.remove(s.id());
        shown.clear();
        world = null;
    }
}
