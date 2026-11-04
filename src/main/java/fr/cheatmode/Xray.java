package fr.cheatmode;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** X-ray : minerais en surbrillance, une couleur par minerai. */
public final class Xray extends BlockEsp {

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

    public Xray() {
        super("cmx", 350);
    }

    @Override
    protected void scan(MinecraftClient mc, Map<BlockPos, Block> out, int max) {
        int r = (int) Settings.INSTANCE.xrayRadius;
        BlockPos center = mc.player.getBlockPos();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (mc.world.isOutOfHeightLimit(p.getY())) continue;
                    Block b = mc.world.getBlockState(p).getBlock();
                    if (ORES.containsKey(b)) {
                        out.put(p.toImmutable(), b);
                        if (out.size() >= max) return;
                    }
                }
            }
        }
    }

    @Override
    protected int color(Block b) {
        return ORES.get(b);
    }

    @Override
    protected String displayBlock(Block b) {
        return Registries.BLOCK.getId(b).toString();
    }

    @Override
    protected float scale() {
        return 0.96f;
    }
}
