package fr.cheatmode;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static fr.cheatmode.CheatModeClient.me;
import static fr.cheatmode.CheatModeClient.say;

/** Comportement de tous les modules. */
final class Cheats {

    /** Effets de potion utilisés par les modules (pour le nettoyage en arrivant dans un monde). */
    private static final Map<RegistryEntry<StatusEffect>, Module> EFFECTS = Map.of(
            StatusEffects.SPEED, Module.SPEED,
            StatusEffects.NIGHT_VISION, Module.FULLBRIGHT,
            StatusEffects.HASTE, Module.FASTBREAK,
            StatusEffects.FIRE_RESISTANCE, Module.ANTIFIRE,
            StatusEffects.WATER_BREATHING, Module.NODROWN);

    private final CheatModeClient cm;
    private final Xray xray = new Xray();
    private final StorageEsp storage = new StorageEsp();

    private long tick;
    private long joinCleanupAt = -1;
    private boolean lastUse, lastJump;
    private int airTicks, triggerWait, antiVoidCooldown;
    private Vec3d lastSafe;
    private Integer savedFov;
    private Double savedTilt;

    Cheats(CheatModeClient cm) {
        this.cm = cm;
    }

    private boolean on(Module m) {
        return cm.isOn(m);
    }

    // =================================================================== cycle de vie

    void onJoin() {
        xray.reset();
        storage.reset();
        lastSafe = null;
        joinCleanupAt = tick + 40; // on attend que les effets du joueur soient synchronisés
        Server.commands(List.of(
                "kill @e[type=minecraft:block_display,tag=cmx]",
                "kill @e[type=minecraft:block_display,tag=cms]"));
    }

    void onLeave() {
        xray.reset();
        storage.reset();
        restoreFov();
        restoreTilt();
    }

    /**
     * Après être entré dans un monde : enlève ce qu'une partie précédente aurait laissé
     * (attributs modifiés, effets infinis, vitesse du jeu).
     */
    private void joinCleanup(MinecraftClient mc) {
        joinCleanupAt = -1;
        if (!Server.available() || mc.player == null) return;
        List<String> cmds = new ArrayList<>();
        for (Module m : List.of(Module.REACH, Module.ANTIKNOCKBACK, Module.HIGHJUMP, Module.STEP, Module.TIMER)) {
            if (!on(m)) cmds.addAll(removeCmds(m));
        }
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            Module m = EFFECTS.get(e.getEffectType());
            if (m != null && e.isInfinite() && !on(m)) cmds.addAll(removeCmds(m));
        }
        Server.commands(cmds);
    }

    void onEnable(Module m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (m) {
            case XRAY -> xray.refresh(mc);
            case STORAGEESP -> storage.refresh(mc);
            case MOBESP -> Server.command(glowCmd());
            case ANTIVOID -> lastSafe = null;
            case NOHURTCAM -> {
                if (savedTilt == null) savedTilt = mc.options.getDamageTiltStrength().getValue();
                mc.options.getDamageTiltStrength().setValue(0.0);
            }
            default -> {}
        }
        Server.commands(applyCmds(m));
    }

    void onDisable(Module m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (m) {
            case XRAY -> xray.clear();
            case STORAGEESP -> storage.clear();
            case MOBESP -> Server.command("execute at " + me()
                    + " run effect clear @e[type=!minecraft:player,distance=..128] minecraft:glowing");
            case SNEAK -> mc.options.sneakKey.setPressed(false);
            case ZOOM -> restoreFov();
            case NOHURTCAM -> restoreTilt();
            case FLY -> Server.withPlayer(mc.player.getUuid(), sp -> {
                if (!sp.isCreative() && !sp.isSpectator()) {
                    sp.getAbilities().allowFlying = false;
                    sp.getAbilities().flying = false;
                    sp.sendAbilitiesUpdate();
                }
            });
            case GOD -> Server.withPlayer(mc.player.getUuid(), sp -> {
                sp.getAbilities().invulnerable = sp.isCreative() || sp.isSpectator();
                sp.sendAbilitiesUpdate();
            });
            default -> {}
        }
        Server.commands(removeCmds(m));
    }

    void reapply(Module m) {
        List<String> cmds = new ArrayList<>(removeCmds(m));
        cmds.addAll(applyCmds(m));
        Server.commands(cmds);
    }

    // =================================================================== modules "commande"

    private static List<String> effect(String id, int amplifier) {
        return List.of("effect give " + me() + " minecraft:" + id + " infinite " + amplifier + " true");
    }

    private static String attr(String id, double value) {
        return String.format(Locale.ROOT, "attribute %s minecraft:%s base set %.4f", me(), id, value);
    }

    /** Commandes qui activent le module (aussi relancées toutes les 5 s, ex. après une mort). */
    private static List<String> applyCmds(Module m) {
        Settings s = Settings.INSTANCE;
        return switch (m) {
            case SPEED -> effect("speed", (int) s.speedLevel - 1);
            case FULLBRIGHT -> effect("night_vision", 0);
            case FASTBREAK -> effect("haste", (int) s.fastBreakLevel - 1);
            case ANTIFIRE -> effect("fire_resistance", 0);
            case NODROWN -> effect("water_breathing", 0);
            case REACH -> List.of(attr("entity_interaction_range", s.reach),
                    attr("block_interaction_range", s.reach + 1.5));
            case ANTIKNOCKBACK -> List.of(attr("knockback_resistance", 1));
            case HIGHJUMP -> List.of(attr("jump_strength", 0.42 * s.jumpBoost));
            case STEP -> List.of(attr("step_height", s.stepHeight));
            case TIMER -> List.of("tick rate " + (int) s.timerRate);
            case ALWAYSDAY -> List.of("gamerule doDaylightCycle false", "time set day");
            case NOWEATHER -> List.of("gamerule doWeatherCycle false", "weather clear");
            default -> List.of();
        };
    }

    /** Commandes qui remettent tout comme avant. */
    private static List<String> removeCmds(Module m) {
        String me = me();
        return switch (m) {
            case SPEED -> List.of("effect clear " + me + " minecraft:speed");
            case FULLBRIGHT -> List.of("effect clear " + me + " minecraft:night_vision");
            case FASTBREAK -> List.of("effect clear " + me + " minecraft:haste");
            case ANTIFIRE -> List.of("effect clear " + me + " minecraft:fire_resistance");
            case NODROWN -> List.of("effect clear " + me + " minecraft:water_breathing");
            case REACH -> List.of(attr("entity_interaction_range", 3), attr("block_interaction_range", 4.5));
            case ANTIKNOCKBACK -> List.of(attr("knockback_resistance", 0));
            case HIGHJUMP -> List.of(attr("jump_strength", 0.41999998688697815));
            case STEP -> List.of(attr("step_height", 0.6));
            case TIMER -> List.of("tick rate 20");
            case ALWAYSDAY -> List.of("gamerule doDaylightCycle true");
            case NOWEATHER -> List.of("gamerule doWeatherCycle true");
            default -> List.of();
        };
    }

    private static String glowCmd() {
        return "execute at " + me()
                + " run effect give @e[type=!minecraft:player,distance=..64] minecraft:glowing 3 0 true";
    }

    // =================================================================== ticks

    void startTick(MinecraftClient mc) {
        // Avant la lecture des touches par le jeu : le sneak reste enfoncé.
        if (mc.player != null && on(Module.SNEAK)) mc.options.sneakKey.setPressed(true);
    }

    /** Tick sans aucun module actif. */
    void idleTick(MinecraftClient mc) {
        tick++;
        if (joinCleanupAt >= 0 && tick >= joinCleanupAt) joinCleanup(mc);
    }

    void tick(MinecraftClient mc) {
        idleTick(mc);
        ClientPlayerEntity p = mc.player;

        serverUpkeep(p);
        if (tick % 100 == 0) {
            List<String> cmds = new ArrayList<>();
            for (Module m : Module.values()) if (on(m)) cmds.addAll(applyCmds(m));
            Server.commands(cmds);
        }
        if (on(Module.MOBESP) && tick % 40 == 0) Server.command(glowCmd());
        if (on(Module.XRAY) && tick % 40 == 0) xray.refresh(mc);
        if (on(Module.STORAGEESP) && tick % 60 == 0) storage.refresh(mc);
        if (on(Module.ZOOM)) zoom(mc);
        if (on(Module.CHESTSTEALER) && tick % 2 == 0) chestStealer(mc, p);
        if (on(Module.AUTORESPAWN) && p.isDead()) {
            p.requestRespawn();
            mc.setScreen(null);
        }
        if (p.isSpectator() || !p.isAlive()) return;

        // Mouvement
        airTicks = p.isOnGround() ? 0 : airTicks + 1;
        if (on(Module.SPRINT)) sprint(p);
        if (on(Module.JESUS)) jesus(p);
        if (on(Module.SPIDER)) spider(p);
        if (on(Module.BUNNYHOP)) bunnyHop(mc, p);
        if (on(Module.AIRJUMP)) airJump(mc, p);
        if (on(Module.ANTIVOID)) antiVoid(mc, p);
        if (on(Module.CLICKTP)) clickTp(mc, p);

        // Combat
        if (on(Module.AIMASSIST)) aimAssist(mc, p);
        if (on(Module.AUTOCLICK)) autoClick(mc, p);
        if (on(Module.TRIGGERBOT)) triggerBot(mc, p);
        if (on(Module.KILLAURA)) killAura(mc, p);
        if (on(Module.KILLAURA_LEGIT)) legitAura(mc, p);
        if (on(Module.TPAURA)) tpAura(mc, p);

        // Joueur
        if (on(Module.AUTOTOOL)) autoTool(mc, p);
    }

    /** Tout ce qui se fait directement sur le joueur côté serveur. */
    private void serverUpkeep(ClientPlayerEntity p) {
        boolean fly = on(Module.FLY), noFall = on(Module.NOFALL), god = on(Module.GOD),
                noHunger = on(Module.NOHUNGER), totem = on(Module.AUTOTOTEM), nuker = on(Module.NUKER),
                scaffold = on(Module.SCAFFOLD), magnet = on(Module.MAGNET);
        if (!(fly || noFall || god || noHunger || totem || nuker || scaffold || magnet)) return;
        final long t = tick;
        final int nukerRadius = (int) Settings.INSTANCE.nukerRadius;
        final double magnetRange = Settings.INSTANCE.magnetRange;

        Server.withPlayer(p.getUuid(), sp -> {
            PlayerAbilities a = sp.getAbilities();
            boolean changed = false;
            if (fly && !a.allowFlying) { a.allowFlying = true; changed = true; } // remis à false en survie
            if (god && !a.invulnerable) { a.invulnerable = true; changed = true; }
            if (changed) sp.sendAbilitiesUpdate();
            if (god && sp.getHealth() < sp.getMaxHealth()) sp.setHealth(sp.getMaxHealth());
            if (noFall) sp.onLanding(); // remet la distance de chute à 0
            if (noHunger) {
                sp.getHungerManager().setFoodLevel(20);
                sp.getHungerManager().setSaturationLevel(5f);
            }
            if (totem) autoTotem(sp);
            if (nuker && t % 2 == 0) nuker(sp, nukerRadius);
            if (scaffold) scaffold(sp);
            if (magnet) magnet(sp, magnetRange);
        });
        if (fly) p.getAbilities().allowFlying = true;
    }

    // =================================================================== serveur

    private static void autoTotem(ServerPlayerEntity sp) {
        if (sp.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        PlayerInventory inv = sp.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) {
                ItemStack off = sp.getOffHandStack();
                sp.setStackInHand(Hand.OFF_HAND, s);
                inv.setStack(i, off);
                return;
            }
        }
    }

    /** Casse jusqu'à 4 blocs par passage, au niveau des pieds et au-dessus. */
    private static void nuker(ServerPlayerEntity sp, int r) {
        World w = sp.getWorld();
        BlockPos c = sp.getBlockPos();
        int broken = 0;
        for (int dy = 0; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = c.add(dx, dy, dz);
                    if (pos.getSquaredDistance(c) > r * r) continue;
                    BlockState st = w.getBlockState(pos);
                    if (st.isAir() || !st.getFluidState().isEmpty() || st.getHardness(w, pos) < 0) continue;
                    if (sp.interactionManager.tryBreakBlock(pos) && ++broken >= 4) return;
                }
            }
        }
    }

    /** Pose le bloc tenu en main sous les pieds. */
    private static void scaffold(ServerPlayerEntity sp) {
        ItemStack hand = sp.getMainHandStack();
        if (!(hand.getItem() instanceof BlockItem bi)) return;
        World w = sp.getWorld();
        BlockPos below = BlockPos.ofFloored(sp.getX(), sp.getY() - 0.5, sp.getZ());
        if (!w.getBlockState(below).isReplaceable()) return;
        BlockState st = bi.getBlock().getDefaultState();
        if (!st.canPlaceAt(w, below)) return;
        w.setBlockState(below, st);
        if (!sp.isCreative()) hand.decrement(1);
    }

    private static void magnet(ServerPlayerEntity sp, double range) {
        Box box = sp.getBoundingBox().expand(range);
        World w = sp.getWorld();
        for (ItemEntity e : w.getEntitiesByClass(ItemEntity.class, box, x -> true)) {
            e.setPosition(sp.getX(), sp.getY(), sp.getZ());
        }
        for (ExperienceOrbEntity e : w.getEntitiesByClass(ExperienceOrbEntity.class, box, x -> true)) {
            e.setPosition(sp.getX(), sp.getY(), sp.getZ());
        }
    }

    // =================================================================== mouvement

    private static void sprint(ClientPlayerEntity p) {
        if (p.forwardSpeed > 0 && !p.isSneaking() && !p.horizontalCollision) p.setSprinting(true);
    }

    private static void jesus(ClientPlayerEntity p) {
        if ((p.isTouchingWater() || p.isInLava()) && !p.isSneaking()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, Math.max(v.y, 0.11), v.z);
        }
    }

    private static void spider(ClientPlayerEntity p) {
        if (p.horizontalCollision) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.2, v.z);
        }
    }

    private static void bunnyHop(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.isOnGround() && (p.forwardSpeed != 0 || p.sidewaysSpeed != 0)
                && !p.isSneaking() && !p.isTouchingWater() && !mc.options.jumpKey.isPressed()) {
            p.jump();
        }
    }

    private void airJump(MinecraftClient mc, ClientPlayerEntity p) {
        boolean jump = mc.options.jumpKey.isPressed();
        if (jump && !lastJump && airTicks > 2 && !p.isTouchingWater() && !p.getAbilities().flying) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.42, v.z);
        }
        lastJump = jump;
    }

    private void antiVoid(MinecraftClient mc, ClientPlayerEntity p) {
        if (antiVoidCooldown > 0) antiVoidCooldown--;
        if (p.isOnGround()) {
            lastSafe = p.getPos();
        } else if (lastSafe != null && antiVoidCooldown == 0 && p.getY() < mc.world.getBottomY() - 2) {
            Server.command(String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f", me(), lastSafe.x, lastSafe.y, lastSafe.z));
            Server.withPlayer(p.getUuid(), ServerPlayerEntity::onLanding);
            p.setVelocity(Vec3d.ZERO);
            antiVoidCooldown = 20;
            say("§eAntiVoid : ramené au sol.");
        }
    }

    /** Clic droit avec une plume : téléportation là où tu regardes. */
    private void clickTp(MinecraftClient mc, ClientPlayerEntity p) {
        boolean use = mc.options.useKey.isPressed();
        boolean pressed = use && !lastUse;
        lastUse = use;
        if (!pressed || mc.currentScreen != null || !p.getMainHandStack().isOf(Items.FEATHER)) return;

        HitResult hit = p.raycast(Settings.INSTANCE.clicktpRange, 1.0f, false);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            say("§cAucun bloc en vue.");
            return;
        }
        BlockPos pos = bhr.getBlockPos().offset(bhr.getSide());
        for (int i = 0; i < 4 && !(free(mc, pos) && free(mc, pos.up())); i++) pos = pos.up();
        for (int i = 0; i < 4 && free(mc, pos.down()); i++) pos = pos.down();

        Server.command(String.format(Locale.ROOT, "tp %s %.1f %d %.1f",
                me(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        Server.withPlayer(p.getUuid(), ServerPlayerEntity::onLanding);
    }

    private static boolean free(MinecraftClient mc, BlockPos pos) {
        return mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    // =================================================================== combat

    private void autoClick(MinecraftClient mc, ClientPlayerEntity p) {
        if (!mc.options.attackKey.isPressed() || mc.currentScreen != null) return;
        long interval = Math.max(1, Math.round(20.0 / Settings.INSTANCE.autoclickCps));
        if (tick % interval != 0) return;
        p.swingHand(Hand.MAIN_HAND);
        if (mc.crosshairTarget instanceof EntityHitResult hit && valid(p, hit.getEntity(), true)) {
            mc.interactionManager.attackEntity(p, hit.getEntity());
        }
    }

    private void triggerBot(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.currentScreen == null && mc.crosshairTarget instanceof EntityHitResult hit
                && valid(p, hit.getEntity(), true) && p.getAttackCooldownProgress(0.5f) >= 1.0f) {
            if (triggerWait++ < Settings.INSTANCE.triggerDelay) return;
            triggerWait = 0;
            mc.interactionManager.attackEntity(p, hit.getEntity());
            p.swingHand(Hand.MAIN_HAND);
        } else {
            triggerWait = 0;
        }
    }

    /** Tourne la vue vers le monstre le plus proche, à vitesse limitée. */
    private void aimAssist(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.currentScreen != null) return;
        Vec3d look = p.getRotationVec(1.0f);
        LivingEntity target = null;
        for (LivingEntity e : targets(mc, p, Settings.INSTANCE.aimRange)) {
            Vec3d to = e.getBoundingBox().getCenter().subtract(p.getEyePos()).normalize();
            if (look.dotProduct(to) >= Math.cos(Math.toRadians(50)) && p.canSee(e)) {
                target = e;
                break;
            }
        }
        if (target == null) return;
        Vec3d d = target.getBoundingBox().getCenter().subtract(p.getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        float speed = (float) Settings.INSTANCE.aimSpeed;
        float dYaw = MathHelper.clamp(MathHelper.wrapDegrees(yaw - p.getYaw()), -speed, speed);
        float dPitch = MathHelper.clamp(pitch - p.getPitch(), -speed, speed);
        p.setYaw(p.getYaw() + dYaw);
        p.setPitch(MathHelper.clamp(p.getPitch() + dPitch, -90, 90));
    }

    /** Normale : frappe tous les monstres à portée toutes les 10 ticks. */
    private void killAura(MinecraftClient mc, ClientPlayerEntity p) {
        if (tick % 10 != 0) return;
        for (LivingEntity target : targets(mc, p, Settings.INSTANCE.killauraRange)) {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    /** Legit : une cible proche, devant toi, visible, et seulement quand le coup est chargé. */
    private void legitAura(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.getAttackCooldownProgress(0.5f) < 1.0f) return;
        Vec3d look = p.getRotationVec(1.0f);
        double minDot = Math.cos(Math.toRadians(Settings.INSTANCE.legitFov / 2));
        for (LivingEntity target : targets(mc, p, Settings.INSTANCE.legitRange)) {
            Vec3d to = target.getBoundingBox().getCenter().subtract(p.getEyePos()).normalize();
            if (look.dotProduct(to) < minDot || !p.canSee(target)) continue;
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            return;
        }
    }

    /** Se téléporte derrière le monstre le plus proche et le frappe. */
    private void tpAura(MinecraftClient mc, ClientPlayerEntity p) {
        if (tick % 10 != 0) return;
        List<LivingEntity> list = targets(mc, p, Settings.INSTANCE.tpauraRange);
        if (list.isEmpty()) return;
        LivingEntity target = list.get(0);

        Vec3d behind = target.getPos().subtract(Vec3d.fromPolar(0, target.getYaw()).multiply(1.5));
        Box box = p.getBoundingBox().offset(behind.subtract(p.getPos()));
        if (!mc.world.isSpaceEmpty(p, box)) behind = target.getPos();

        int targetId = target.getId();
        Server.command(String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f facing entity %s eyes",
                me(), behind.x, behind.y, behind.z, target.getUuidAsString()));
        Server.withPlayer(p.getUuid(), sp -> {
            Entity e = sp.getWorld().getEntityById(targetId);
            if (e != null && e.isAlive()) sp.attack(e);
            sp.onLanding();
        });
        p.swingHand(Hand.MAIN_HAND);
    }

    // =================================================================== joueur / monde

    /** Prend dans la barre d'action l'outil qui mine le plus vite le bloc visé. */
    private static void autoTool(MinecraftClient mc, ClientPlayerEntity p) {
        if (!mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof BlockHitResult bhr)
                || bhr.getType() != HitResult.Type.BLOCK) return;
        BlockState st = mc.world.getBlockState(bhr.getBlockPos());
        PlayerInventory inv = p.getInventory();
        int best = inv.selectedSlot;
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(st);
        for (int i = 0; i < 9; i++) {
            float s = inv.getStack(i).getMiningSpeedMultiplier(st);
            if (s > bestSpeed) {
                best = i;
                bestSpeed = s;
            }
        }
        inv.selectedSlot = best;
    }

    /** Déplace 2 piles par passage du coffre ouvert vers l'inventaire. */
    private static void chestStealer(MinecraftClient mc, ClientPlayerEntity p) {
        if (!(mc.currentScreen instanceof GenericContainerScreen || mc.currentScreen instanceof ShulkerBoxScreen)) return;
        ScreenHandler h = ((HandledScreen<?>) mc.currentScreen).getScreenHandler();
        int containerSlots = h.slots.size() - 36; // les 36 derniers = inventaire du joueur
        int moved = 0;
        for (int i = 0; i < containerSlots && moved < 2; i++) {
            if (h.getSlot(i).hasStack()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, p);
                moved++;
            }
        }
    }

    // =================================================================== rendu

    private void zoom(MinecraftClient mc) {
        boolean held = mc.currentScreen == null && cm.zoomKey.isPressed();
        if (held) {
            var fov = mc.options.getFov();
            if (savedFov == null) savedFov = fov.getValue();
            int target = (int) Settings.INSTANCE.zoomFov;
            if (fov.getValue() != target) fov.setValue(target);
        } else {
            restoreFov();
        }
    }

    private void restoreFov() {
        if (savedFov != null) {
            MinecraftClient.getInstance().options.getFov().setValue(savedFov);
            savedFov = null;
        }
    }

    private void restoreTilt() {
        if (savedTilt != null) {
            MinecraftClient.getInstance().options.getDamageTiltStrength().setValue(savedTilt);
            savedTilt = null;
        }
    }

    // =================================================================== ciblage

    private static List<LivingEntity> targets(MinecraftClient mc, ClientPlayerEntity p, double range) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && valid(p, e, false) && p.squaredDistanceTo(e) <= range * range) {
                out.add(living);
            }
        }
        out.sort(Comparator.comparingDouble(p::squaredDistanceTo));
        return out;
    }

    /**
     * @param anything autoclick / triggerbot : tout ce qu'on vise (sauf joueurs).
     *                 Sinon : uniquement les monstres hostiles.
     */
    private static boolean valid(ClientPlayerEntity self, Entity e, boolean anything) {
        if (!(e instanceof LivingEntity living) || e == self || !living.isAlive()) return false;
        if (e instanceof ArmorStandEntity || e instanceof PlayerEntity) return false;
        return anything || e instanceof Monster;
    }
}
