package fr.cheatmode;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Tourne à chaque tick et fait marcher killaura, tpaura, autoclick et sneak. */
public final class CheatTask implements Runnable {

    private final CheatModePlugin plugin;
    private long tick;

    public CheatTask(CheatModePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tick++;
        CheatState state = plugin.state();
        for (UUID id : List.copyOf(state.players())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) continue;

            if (state.has(id, Module.SNEAK) && !player.isSneaking()) player.setSneaking(true);
            if (state.has(id, Module.AUTOCLICK)) autoClick(player);
            if (state.has(id, Module.KILLAURA)) killAura(player);
            if (state.has(id, Module.KILLAURA_LEGIT)) legitAura(player);
            if (state.has(id, Module.TPAURA)) tpAura(player);
        }
    }

    // ---------------------------------------------------------------- autoclick

    private void autoClick(Player player) {
        FileConfiguration c = plugin.getConfig();
        int cps = Math.max(1, Math.min(20, c.getInt("autoclick.cps", 12)));
        long interval = Math.max(1, Math.round(20.0 / cps));
        if (tick % interval != 0) return;

        Entity target = player.getTargetEntity(Math.max(1, c.getInt("autoclick.reach", 4)));
        player.swingMainHand();
        if (target instanceof LivingEntity living && isValid(player, living, true)) {
            player.attack(living);
        }
    }

    // ---------------------------------------------------------------- killaura

    /** Normale : frappe tout ce qui est à portée, toutes les 10 ticks (temps d'invincibilité vanilla). */
    private void killAura(Player player) {
        if (tick % 10 != 0) return;
        double range = plugin.getConfig().getDouble("killaura.range", 6.0);
        for (LivingEntity target : targets(player, range)) {
            player.swingMainHand();
            player.attack(target);
        }
    }

    /**
     * Legit : une seule cible, courte portée, uniquement devant toi, visible,
     * et seulement quand la barre d'attaque est rechargée (comme un vrai clic au bon moment).
     */
    private void legitAura(Player player) {
        if (player.getAttackCooldown() < 0.95f) return;
        FileConfiguration c = plugin.getConfig();
        double range = c.getDouble("killaura.legit-range", 3.2);
        double fov = Math.toRadians(c.getDouble("killaura.legit-fov", 70));
        Vector look = player.getEyeLocation().getDirection();

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity target : targets(player, range)) {
            Vector to = target.getLocation().add(0, target.getHeight() / 2, 0).toVector()
                    .subtract(player.getEyeLocation().toVector());
            if (look.angle(to) > fov / 2) continue;
            if (!player.hasLineOfSight(target)) continue;
            double d = to.lengthSquared();
            if (d < bestDist) {
                bestDist = d;
                best = target;
            }
        }
        if (best != null) {
            player.swingMainHand();
            player.attack(best);
        }
    }

    // ---------------------------------------------------------------- tpaura

    private void tpAura(Player player) {
        FileConfiguration c = plugin.getConfig();
        long interval = Math.max(2, c.getLong("tpaura.interval-ticks", 10));
        if (tick % interval != 0) return;

        List<LivingEntity> list = targets(player, c.getDouble("tpaura.range", 16.0));
        if (list.isEmpty()) return;
        LivingEntity target = list.get(0);

        // Se place 1.5 bloc derrière la cible, en la regardant.
        Location tl = target.getLocation();
        Vector back = tl.getDirection().setY(0);
        if (back.lengthSquared() < 1.0E-4) back = new Vector(0, 0, 1);
        Location dest = tl.clone().subtract(back.normalize().multiply(1.5));
        if (!dest.getBlock().isPassable() || !dest.clone().add(0, 1, 0).getBlock().isPassable()) {
            dest = tl.clone(); // pas de place derrière : on se met sur elle
        }
        Vector face = target.getEyeLocation().toVector().subtract(dest.clone().add(0, player.getEyeHeight(), 0).toVector());
        dest.setDirection(face);

        player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setFallDistance(0);
        player.swingMainHand();
        player.attack(target);
    }

    // ---------------------------------------------------------------- ciblage

    /** Cibles valides autour du joueur, de la plus proche à la plus lointaine. */
    private List<LivingEntity> targets(Player player, double range) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity living && isValid(player, living, false)
                    && living.getLocation().distanceSquared(player.getLocation()) <= range * range) {
                out.add(living);
            }
        }
        Location me = player.getLocation();
        out.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(me)));
        return out;
    }

    /**
     * @param anything true pour l'autoclick : on frappe ce qu'on vise, quel que soit le type
     *                 (sauf joueurs si désactivé).
     */
    private boolean isValid(Player self, LivingEntity e, boolean anything) {
        if (e == self || e.isDead() || !e.isValid() || e instanceof ArmorStand || e.isInvulnerable()) return false;
        FileConfiguration c = plugin.getConfig();
        if (e instanceof Player p) {
            return c.getBoolean("killaura.target-players", false)
                    && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR;
        }
        if (anything) return true;
        if (e instanceof Enemy) return true;
        return e instanceof Ageable && c.getBoolean("killaura.target-animals", false);
    }
}
