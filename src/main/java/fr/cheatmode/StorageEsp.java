package fr.cheatmode;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;

/**
 * StorageESP : coffres, barils, shulkers et coffres de l'End en surbrillance.
 * On cherche dans les block entities des chunks chargés (beaucoup plus rapide qu'un scan de blocs).
 * Le display est un petit cube caché à l'intérieur du coffre : on ne voit que son contour.
 */
public final class StorageEsp extends BlockEsp {

    public StorageEsp() {
        super("cms", 200);
    }

    @Override
    protected void scan(MinecraftClient mc, Map<BlockPos, Block> out, int max) {
        double r = Settings.INSTANCE.storageRadius;
        BlockPos me = mc.player.getBlockPos();
        int cr = (int) Math.ceil(r / 16);
        int cx = me.getX() >> 4, cz = me.getZ() >> 4;
        for (int x = cx - cr; x <= cx + cr; x++) {
            for (int z = cz - cr; z <= cz + cr; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    Block b = e.getValue().getCachedState().getBlock();
                    if (color(b) < 0 || e.getKey().getSquaredDistance(me) > r * r) continue;
                    out.put(e.getKey().toImmutable(), b);
                    if (out.size() >= max) return;
                }
            }
        }
    }

    @Override
    protected int color(Block b) {
        if (b == Blocks.TRAPPED_CHEST) return 0xFF3030;
        if (b instanceof ChestBlock) return 0xFFA030;
        if (b instanceof EnderChestBlock) return 0xA020F0;
        if (b instanceof BarrelBlock) return 0xA06030;
        if (b instanceof ShulkerBoxBlock) return 0xFF60C0;
        return -1;
    }

    @Override
    protected String displayBlock(Block b) {
        return "minecraft:white_concrete";
    }

    @Override
    protected float scale() {
        return 0.8f;
    }
}
