package fr.cheatmode;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * X-ray côté serveur : pour chaque minerai proche, on fait apparaître un "BlockDisplay"
 * lumineux (glowing) visible UNIQUEMENT par le joueur. Le contour glowing se voit à travers
 * les murs, donc on repère les minerais sans rien changer au monde.
 */
public final class XrayManager {

    private static final Map<Material, Color> ORES = new EnumMap<>(Material.class);

    static {
        ore(Color.fromRGB(0x40, 0x40, 0x40), Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE);
        ore(Color.fromRGB(0xD8, 0xAF, 0x93), Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
        ore(Color.fromRGB(0xE0, 0x7B, 0x4F), Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
        ore(Color.fromRGB(0xFC, 0xEE, 0x4B), Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE);
        ore(Color.fromRGB(0xFF, 0x20, 0x20), Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE);
        ore(Color.fromRGB(0x20, 0x40, 0xFF), Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE);
        ore(Color.fromRGB(0x20, 0xE0, 0x60), Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
        ore(Color.fromRGB(0x4A, 0xED, 0xD9), Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
        ore(Color.fromRGB(0xF0, 0xF0, 0xF0), Material.NETHER_QUARTZ_ORE);
        ore(Color.fromRGB(0x8B, 0x45, 0x13), Material.ANCIENT_DEBRIS);
        ore(Color.fromRGB(0xB0, 0x30, 0xFF), Material.SPAWNER);
    }

    private static void ore(Color color, Material... materials) {
        for (Material m : materials) ORES.put(m, color);
    }

    private final CheatModePlugin plugin;
    private final Map<UUID, Map<Long, BlockDisplay>> shown = new HashMap<>();
    private final Map<UUID, World> worlds = new HashMap<>();

    public XrayManager(CheatModePlugin plugin) {
        this.plugin = plugin;
    }

    public void refreshAll() {
        for (UUID id : Set.copyOf(plugin.state().players())) {
            if (!plugin.state().has(id, Module.XRAY)) continue;
            Player player = Bukkit.getPlayer(id);
            if (player != null) refresh(player);
        }
    }

    public void refresh(Player player) {
        UUID id = player.getUniqueId();
        World world = player.getWorld();
        if (worlds.get(id) != world) {
            clear(player);
            worlds.put(id, world);
        }

        int radius = Math.max(4, Math.min(48, plugin.getConfig().getInt("xray.radius", 16)));
        int max = Math.max(1, plugin.getConfig().getInt("xray.max-blocks", 400));
        Block center = player.getLocation().getBlock();
        int minY = world.getMinHeight(), maxY = world.getMaxHeight() - 1;

        // 1) Repère les minerais autour du joueur.
        Map<Long, Block> found = new HashMap<>();
        scan:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx, z = center.getZ() + dz;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                for (int dy = -radius; dy <= radius; dy++) {
                    int y = center.getY() + dy;
                    if (y < minY || y > maxY) continue;
                    Block b = world.getBlockAt(x, y, z);
                    if (ORES.containsKey(b.getType())) {
                        found.put(key(x, y, z), b);
                        if (found.size() >= max) break scan;
                    }
                }
            }
        }

        // 2) Retire les affichages des minerais minés ou trop loin.
        Map<Long, BlockDisplay> current = shown.computeIfAbsent(id, k -> new HashMap<>());
        Iterator<Map.Entry<Long, BlockDisplay>> it = current.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, BlockDisplay> e = it.next();
            Block b = found.get(e.getKey());
            BlockDisplay d = e.getValue();
            if (b == null || !d.isValid() || d.getBlock().getMaterial() != b.getType()) {
                d.remove();
                it.remove();
            }
        }

        // 3) Ajoute les nouveaux.
        Set<Long> missing = new HashSet<>(found.keySet());
        missing.removeAll(current.keySet());
        for (long k : missing) {
            Block b = found.get(k);
            Material type = b.getType();
            BlockDisplay display = world.spawn(b.getLocation(), BlockDisplay.class, d -> {
                d.setVisibleByDefault(false);
                d.setPersistent(false);
                d.setBlock(type.createBlockData());
                d.setGlowing(true);
                d.setGlowColorOverride(ORES.get(type));
                d.setBrightness(new Display.Brightness(15, 15));
                // Légèrement plus petit que le vrai bloc pour éviter le scintillement.
                d.setTransformation(new Transformation(
                        new Vector3f(0.02f, 0.02f, 0.02f), new AxisAngle4f(),
                        new Vector3f(0.96f, 0.96f, 0.96f), new AxisAngle4f()));
            });
            player.showEntity(plugin, display);
            current.put(k, display);
        }
    }

    public void clear(Player player) {
        Map<Long, BlockDisplay> map = shown.remove(player.getUniqueId());
        worlds.remove(player.getUniqueId());
        if (map != null) map.values().forEach(BlockDisplay::remove);
    }

    public void clearAll() {
        shown.values().forEach(m -> m.values().forEach(BlockDisplay::remove));
        shown.clear();
        worlds.clear();
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF);
    }
}
