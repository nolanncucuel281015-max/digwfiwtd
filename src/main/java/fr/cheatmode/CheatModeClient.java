package fr.cheatmode;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * CheatMode : fly, speed, nofall, autoclick, killaura (normale / legit), tpaura, click TP,
 * x-ray, sneak, et les commandes -gm. Ne fonctionne qu'en monde solo.
 */
public final class CheatModeClient implements ClientModInitializer {

    private static final String PREFIX = "-";

    // Réglages
    private static final double KILLAURA_RANGE = 5.5;
    private static final double LEGIT_RANGE = 3.0;
    private static final double LEGIT_FOV = 70;    // degrés devant toi
    private static final double TPAURA_RANGE = 16;
    private static final int AUTOCLICK_CPS = 12;
    private static final double CLICKTP_RANGE = 120;

    private final Set<Module> on = EnumSet.noneOf(Module.class);
    private final Xray xray = new Xray();
    private KeyBinding presetKey;
    private int speedLevel = 3;
    private long tick;
    private boolean lastUse;

    @Override
    public void onInitializeClient() {
        presetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cheatmode.preset", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.cheatmode"));

        ClientSendMessageEvents.ALLOW_CHAT.register(this::onChat);
        ClientTickEvents.START_CLIENT_TICK.register(this::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(this::endTick);
        HudRenderCallback.EVENT.register((ctx, counter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || on.isEmpty()) return;
            int y = 2;
            for (Module m : on) {
                ctx.drawTextWithShadow(mc.textRenderer, m.display, 2, y, 0x55FF55);
                y += 10;
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            on.clear();
            xray.reset();
            // Nettoie d'éventuels restes de x-ray d'une partie précédente.
            Server.command("kill @e[type=minecraft:block_display,tag=cmx]");
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            on.clear();
            xray.reset();
        });
    }

    // =================================================================== chat

    /** @return false pour annuler l'envoi du message. */
    private boolean onChat(String message) {
        String msg = message.trim();
        if (!msg.startsWith(PREFIX) || msg.length() <= PREFIX.length()) return true;
        String[] args = msg.substring(PREFIX.length()).trim().split("\\s+");
        if (!isCommand(args[0])) return true; // ex: "-_-" reste un message normal
        MinecraftClient.getInstance().execute(() -> handle(args));
        return false;
    }

    private static boolean isCommand(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "gm", "gamemode", "fly", "speed", "nofall", "nf", "autoclick", "ac", "killaura", "ka",
                 "legit", "tpaura", "tpa", "clicktp", "ctp", "xray", "sneak", "x", "off", "list", "help" -> true;
            default -> false;
        };
    }

    private void handle(String[] args) {
        if (!Server.available()) {
            say("§cCheatMode ne marche qu'en monde solo.");
            return;
        }
        String cmd = args[0].toLowerCase(Locale.ROOT);
        String arg = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (cmd) {
            case "gm", "gamemode" -> gamemode(arg);
            case "fly" -> setFly(!on.contains(Module.FLY));
            case "speed" -> speed(arg);
            case "nofall", "nf" -> toggle(Module.NOFALL);
            case "autoclick", "ac" -> toggle(Module.AUTOCLICK);
            case "killaura", "ka" -> {
                if (arg.equals("legit")) legitAura(!on.contains(Module.KILLAURA_LEGIT));
                else { on.remove(Module.KILLAURA_LEGIT); toggle(Module.KILLAURA); }
            }
            case "legit" -> legitAura(!on.contains(Module.KILLAURA_LEGIT));
            case "tpaura", "tpa" -> toggle(Module.TPAURA);
            case "clicktp", "ctp" -> {
                if (toggle(Module.CLICKTP)) say("§7Clic droit avec une plume pour te téléporter.");
            }
            case "xray" -> setXray(!on.contains(Module.XRAY));
            case "sneak" -> setSneak(!on.contains(Module.SNEAK));
            case "x" -> preset();
            case "off" -> { disableAll(); say("§eTous les modules sont désactivés."); }
            case "list" -> say("§bActifs : " + (on.isEmpty() ? "aucun"
                    : String.join(", ", on.stream().map(m -> m.display).toList())));
            default -> help();
        }
    }

    // =================================================================== commandes

    private void gamemode(String arg) {
        String mode = switch (arg) {
            case "creative", "creatif", "créatif", "c", "1" -> "creative";
            case "survival", "survie", "s", "0" -> "survival";
            case "spectator", "spectateur", "spec", "sp", "3" -> "spectator";
            case "adventure", "aventure", "a", "2" -> "adventure";
            default -> null;
        };
        if (mode == null) {
            say("§cUsage : -gm creative | survival | spectator | adventure");
            return;
        }
        Server.command("gamemode " + mode + " " + me());
        say("§7Mode de jeu : §b" + mode);
    }

    private void speed(String arg) {
        if (!arg.isEmpty()) {
            try {
                speedLevel = Math.max(1, Math.min(10, Integer.parseInt(arg)));
            } catch (NumberFormatException e) {
                say("§cUsage : -speed [niveau 1-10]");
                return;
            }
            on.add(Module.SPEED);
        } else if (on.contains(Module.SPEED)) {
            on.remove(Module.SPEED);
        } else {
            on.add(Module.SPEED);
        }
        if (on.contains(Module.SPEED)) giveSpeed();
        else Server.command("effect clear " + me() + " minecraft:speed");
        say("§7Speed : " + (on.contains(Module.SPEED) ? "§aON §7(niveau " + speedLevel + ")" : "§cOFF"));
    }

    private void giveSpeed() {
        Server.commands(List.of(
                "effect clear " + me() + " minecraft:speed",
                "effect give " + me() + " minecraft:speed infinite " + (speedLevel - 1) + " true"));
    }

    private boolean toggle(Module m) {
        boolean now = !on.contains(m);
        set(m, now);
        return now;
    }

    private void set(Module m, boolean value) {
        if (value) on.add(m); else on.remove(m);
        say("§7" + m.display + " : " + (value ? "§aON" : "§cOFF"));
    }

    private void setFly(boolean value) {
        set(Module.FLY, value);
        if (!value) {
            Server.withPlayer(UUID.fromString(me()), sp -> {
                if (!sp.isCreative() && !sp.isSpectator()) {
                    sp.getAbilities().allowFlying = false;
                    sp.getAbilities().flying = false;
                    sp.sendAbilitiesUpdate();
                }
            });
        }
    }

    private void legitAura(boolean value) {
        on.remove(Module.KILLAURA);
        set(Module.KILLAURA_LEGIT, value);
    }

    private void setXray(boolean value) {
        set(Module.XRAY, value);
        if (value) xray.refresh(MinecraftClient.getInstance()); else xray.clear();
    }

    private void setSneak(boolean value) {
        set(Module.SNEAK, value);
        if (!value) MinecraftClient.getInstance().options.sneakKey.setPressed(false);
    }

    /** Touche X (ou -x) : x-ray + sneak + killaura legit. Re-appuie pour couper. */
    private void preset() {
        boolean value = !(on.contains(Module.XRAY) && on.contains(Module.SNEAK) && on.contains(Module.KILLAURA_LEGIT));
        setXray(value);
        setSneak(value);
        legitAura(value);
    }

    private void disableAll() {
        if (on.contains(Module.XRAY)) xray.clear();
        if (on.contains(Module.SNEAK)) MinecraftClient.getInstance().options.sneakKey.setPressed(false);
        if (on.contains(Module.SPEED)) Server.command("effect clear " + me() + " minecraft:speed");
        boolean fly = on.contains(Module.FLY);
        on.clear();
        if (fly) setFly(false);
    }

    private void help() {
        say("§6===== CheatMode (solo) =====");
        String[][] lines = {
                {"gm <creative|survival|spectator>", "mode de jeu"},
                {"fly", "vol"},
                {"speed [niveau]", "vitesse (niveau 3 par défaut)"},
                {"nofall", "aucun dégât de chute"},
                {"autoclick", "maintiens clic gauche : clique tout seul"},
                {"killaura", "frappe les monstres autour de toi"},
                {"killaura legit", "une cible devant toi, au rythme du cooldown"},
                {"tpaura", "se téléporte derrière les monstres"},
                {"clicktp", "clic droit avec une plume = TP"},
                {"xray", "minerais visibles à travers les murs"},
                {"sneak", "accroupi en permanence"},
                {"x  (ou touche X)", "x-ray + sneak + killaura legit"},
                {"list / off", "modules actifs / tout couper"},
        };
        for (String[] l : lines) say("§e" + PREFIX + l[0] + " §7- " + l[1]);
    }

    // =================================================================== tick

    private void startTick(MinecraftClient mc) {
        // Avant la lecture des touches par le jeu : le sneak reste enfoncé.
        if (mc.player != null && on.contains(Module.SNEAK)) mc.options.sneakKey.setPressed(true);
    }

    private void endTick(MinecraftClient mc) {
        tick++;
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        while (presetKey.wasPressed()) {
            if (Server.available()) preset();
            else say("§cCheatMode ne marche qu'en monde solo.");
        }
        if (on.isEmpty()) return;
        if (!Server.available()) { // sécurité : jamais actif en multijoueur
            on.clear();
            return;
        }

        serverUpkeep(player);
        if (on.contains(Module.SPEED) && tick % 40 == 0 && !player.hasStatusEffect(StatusEffects.SPEED)) {
            giveSpeed(); // l'effet a disparu (mort, lait...)
        }
        if (player.isSpectator() || !player.isAlive()) return;

        if (on.contains(Module.AUTOCLICK)) autoClick(mc, player);
        if (on.contains(Module.KILLAURA)) killAura(mc, player);
        if (on.contains(Module.KILLAURA_LEGIT)) legitAura(mc, player);
        if (on.contains(Module.TPAURA)) tpAura(mc, player);
        if (on.contains(Module.CLICKTP)) clickTp(mc, player);
        if (on.contains(Module.XRAY) && tick % 40 == 0) xray.refresh(mc);
    }

    /** Fly et NoFall sont appliqués directement sur le joueur côté serveur. */
    private void serverUpkeep(ClientPlayerEntity player) {
        boolean fly = on.contains(Module.FLY), noFall = on.contains(Module.NOFALL);
        if (!fly && !noFall) return;
        Server.withPlayer(player.getUuid(), sp -> {
            if (fly && !sp.getAbilities().allowFlying) { // remis à false quand on passe en survie
                sp.getAbilities().allowFlying = true;
                sp.sendAbilitiesUpdate();
            }
            if (noFall) sp.onLanding(); // remet la distance de chute à 0
        });
        if (fly) player.getAbilities().allowFlying = true;
    }

    private void autoClick(MinecraftClient mc, ClientPlayerEntity player) {
        if (!mc.options.attackKey.isPressed()) return;
        long interval = Math.max(1, Math.round(20.0 / AUTOCLICK_CPS));
        if (tick % interval != 0) return;
        player.swingHand(Hand.MAIN_HAND);
        if (mc.crosshairTarget instanceof EntityHitResult hit && valid(player, hit.getEntity(), true)) {
            mc.interactionManager.attackEntity(player, hit.getEntity());
        }
    }

    /** Normale : frappe tous les monstres à portée toutes les 10 ticks. */
    private void killAura(MinecraftClient mc, ClientPlayerEntity player) {
        if (tick % 10 != 0) return;
        for (LivingEntity target : targets(mc, player, KILLAURA_RANGE)) {
            mc.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    /** Legit : une cible proche, devant toi, visible, et seulement quand le coup est chargé. */
    private void legitAura(MinecraftClient mc, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        Vec3d look = player.getRotationVec(1.0f);
        double minDot = Math.cos(Math.toRadians(LEGIT_FOV / 2));
        for (LivingEntity target : targets(mc, player, LEGIT_RANGE)) {
            Vec3d to = target.getBoundingBox().getCenter().subtract(player.getEyePos()).normalize();
            if (look.dotProduct(to) < minDot || !player.canSee(target)) continue;
            mc.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            return;
        }
    }

    /** Se téléporte derrière le monstre le plus proche et le frappe. */
    private void tpAura(MinecraftClient mc, ClientPlayerEntity player) {
        if (tick % 10 != 0) return;
        List<LivingEntity> list = targets(mc, player, TPAURA_RANGE);
        if (list.isEmpty()) return;
        LivingEntity target = list.get(0);

        Vec3d behind = target.getPos().subtract(Vec3d.fromPolar(0, target.getYaw()).multiply(1.5));
        Box box = player.getBoundingBox().offset(behind.subtract(player.getPos()));
        if (!mc.world.isSpaceEmpty(player, box)) behind = target.getPos();

        int targetId = target.getId();
        String cmd = String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f facing entity %s eyes",
                me(), behind.x, behind.y, behind.z, target.getUuidAsString());
        Server.command(cmd);
        Server.withPlayer(player.getUuid(), sp -> {
            Entity e = sp.getWorld().getEntityById(targetId);
            if (e != null && e.isAlive()) sp.attack(e);
            sp.onLanding();
        });
        player.swingHand(Hand.MAIN_HAND);
    }

    /** Clic droit avec une plume : téléportation là où tu regardes. */
    private void clickTp(MinecraftClient mc, ClientPlayerEntity player) {
        boolean use = mc.options.useKey.isPressed();
        boolean pressed = use && !lastUse;
        lastUse = use;
        if (!pressed || !player.getMainHandStack().isOf(Items.FEATHER)) return;

        HitResult hit = player.raycast(CLICKTP_RANGE, 1.0f, false);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            say("§cAucun bloc en vue.");
            return;
        }
        BlockPos pos = bhr.getBlockPos().offset(bhr.getSide());
        for (int i = 0; i < 4 && !(free(mc, pos) && free(mc, pos.up())); i++) pos = pos.up();
        for (int i = 0; i < 4 && free(mc, pos.down()); i++) pos = pos.down();

        Server.command(String.format(Locale.ROOT, "tp %s %.1f %d %.1f",
                me(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        Server.withPlayer(player.getUuid(), sp -> sp.onLanding());
    }

    private static boolean free(MinecraftClient mc, BlockPos pos) {
        return mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    // =================================================================== ciblage

    private List<LivingEntity> targets(MinecraftClient mc, ClientPlayerEntity player, double range) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && valid(player, e, false)
                    && player.squaredDistanceTo(e) <= range * range) {
                out.add(living);
            }
        }
        out.sort(Comparator.comparingDouble(player::squaredDistanceTo));
        return out;
    }

    /**
     * @param anything autoclick : tout ce qu'on vise (sauf joueurs).
     *                 Sinon : uniquement les monstres hostiles.
     */
    private static boolean valid(ClientPlayerEntity self, Entity e, boolean anything) {
        if (!(e instanceof LivingEntity living) || e == self || !living.isAlive()) return false;
        if (e instanceof ArmorStandEntity || e instanceof PlayerEntity) return false;
        return anything || e instanceof Monster;
    }

    // =================================================================== utilitaires

    private static String me() {
        return MinecraftClient.getInstance().player.getUuidAsString();
    }

    private static void say(String msg) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§6[CheatMode] §r" + msg));
    }
}
