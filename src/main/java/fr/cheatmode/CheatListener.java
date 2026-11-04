package fr.cheatmode;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;

public final class CheatListener implements Listener {

    private final CheatModePlugin plugin;

    public CheatListener(CheatModePlugin plugin) {
        this.plugin = plugin;
    }

    /** Intercepte les messages du chat qui commencent par le préfixe ("-fly", "-gm creative", ...). */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String prefix = plugin.getConfig().getString("prefix", "-");
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (!msg.startsWith(prefix) || msg.length() <= prefix.length()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("cheatmode.use")) return; // message normal pour les autres

        event.setCancelled(true); // personne d'autre ne voit la commande
        String[] args = msg.substring(prefix.length()).trim().split("\\s+");
        // Le chat est asynchrone : on repasse sur le thread principal pour toucher au monde.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) plugin.commands().handle(player, args);
        });
    }

    /** ClickTP : clic droit avec l'item configuré (plume par défaut). */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.state().has(player.getUniqueId(), Module.CLICKTP)) return;

        Material item = Material.matchMaterial(plugin.getConfig().getString("clicktp.item", "FEATHER"));
        if (item == null) item = Material.FEATHER;
        if (player.getInventory().getItemInMainHand().getType() != item) return;

        event.setCancelled(true);
        double max = plugin.getConfig().getDouble("clicktp.max-distance", 120);
        RayTraceResult hit = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), max, FluidCollisionMode.NEVER, true);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            player.sendMessage(Component.text("Aucun bloc en vue.", NamedTextColor.RED));
            return;
        }

        Block target = hit.getHitBlock().getRelative(hit.getHitBlockFace());
        // Cherche jusqu'à 4 blocs plus haut un endroit avec 2 blocs d'air pour tenir debout.
        for (int i = 0; i < 4 && !(target.isPassable() && target.getRelative(0, 1, 0).isPassable()); i++) {
            target = target.getRelative(0, 1, 0);
        }
        // Si on vise un mur ou un plafond, on descend jusqu'au sol (max 4 blocs).
        for (int i = 0; i < 4 && target.getRelative(0, -1, 0).isPassable(); i++) {
            target = target.getRelative(0, -1, 0);
        }

        Location dest = target.getLocation().add(0.5, 0, 0.5);
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());
        player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setFallDistance(0);
    }

    /** Pas de dégâts de chute quand le fly ou le clicktp est actif. */
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        var id = player.getUniqueId();
        if (plugin.state().has(id, Module.FLY) || plugin.state().has(id, Module.CLICKTP)
                || plugin.state().has(id, Module.TPAURA)) {
            event.setCancelled(true);
        }
    }

    /** Le sneak forcé ne se relâche pas quand on lâche la touche. */
    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking() && plugin.state().has(event.getPlayer().getUniqueId(), Module.SNEAK)) {
            event.setCancelled(true);
        }
    }

    /** Passer en survie coupe le vol : on le remet au tick suivant si le fly est actif. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.state().has(player.getUniqueId(), Module.FLY)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) CommandHandler.applyFly(player, true);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.commands().disableAll(event.getPlayer());
    }
}
