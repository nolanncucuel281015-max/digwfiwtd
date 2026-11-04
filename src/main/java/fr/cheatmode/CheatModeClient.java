package fr.cheatmode;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CheatMode : une quarantaine de modules (combat, mouvement, rendu, joueur, monde),
 * un menu (Shift droit) et des commandes "-xxx" dans le chat. Monde solo uniquement.
 */
public final class CheatModeClient implements ClientModInitializer {

    public static final String PREFIX = "-";
    private static CheatModeClient instance;

    private final Set<Module> on = EnumSet.noneOf(Module.class);
    private Cheats cheats;
    private KeyBinding presetKey, menuKey;
    KeyBinding zoomKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        Settings.load();
        cheats = new Cheats(this);

        menuKey = key("key.cheatmode.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        presetKey = key("key.cheatmode.preset", GLFW.GLFW_KEY_X);
        zoomKey = key("key.cheatmode.zoom", GLFW.GLFW_KEY_C);

        ClientSendMessageEvents.ALLOW_CHAT.register(this::onChat);
        ClientTickEvents.START_CLIENT_TICK.register(mc -> cheats.startTick(mc));
        ClientTickEvents.END_CLIENT_TICK.register(this::endTick);
        HudRenderCallback.EVENT.register((ctx, counter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || on.isEmpty() || mc.currentScreen instanceof ClickGui) return;
            int y = 2;
            for (Module m : on) {
                ctx.drawTextWithShadow(mc.textRenderer, m.display, 2, y, m.category.color);
                y += 10;
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            on.clear();
            cheats.onJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            on.clear();
            cheats.onLeave();
        });
    }

    private static KeyBinding key(String id, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, code, "category.cheatmode"));
    }

    private void endTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        while (menuKey.wasPressed()) mc.setScreen(new ClickGui());
        while (presetKey.wasPressed()) runPreset();
        if (on.isEmpty()) {
            cheats.idleTick(mc);
            return;
        }
        if (!active()) { // serveur multijoueur non autorisé : on coupe tout
            on.clear();
            return;
        }
        cheats.tick(mc);
    }

    // =================================================================== API

    public static CheatModeClient get() {
        return instance;
    }

    /** Monde solo (serveur intégré) : tous les modules marchent. */
    public static boolean solo() {
        return Server.available();
    }

    /** Adresse du serveur multijoueur actuel, ou null. */
    public static String currentServer() {
        var entry = MinecraftClient.getInstance().getCurrentServerEntry();
        return entry == null || solo() ? null : entry.address.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean allowedServer() {
        String s = currentServer();
        return s != null && Settings.INSTANCE.allowedServers.contains(s);
    }

    /** Le mod a le droit de s'activer ici. */
    public static boolean active() {
        return solo() || allowedServer();
    }

    /** Vérifie qu'on peut activer ce module ici ; sinon explique pourquoi. */
    private static boolean canUse(Module m) {
        if (!active()) {
            notAllowed();
            return false;
        }
        if (!solo() && !m.multiplayer()) {
            say("§c" + m.display + " : solo uniquement (en multijoueur c'est le serveur qui décide).");
            return false;
        }
        return true;
    }

    private static void notAllowed() {
        if (currentServer() != null) {
            say("§cCe serveur n'est pas autorisé. Si c'est un serveur anarchie (cheats permis), tape §e-allow§c.");
        } else {
            say("§cCheatMode marche en solo et sur les serveurs autorisés avec -allow.");
        }
    }

    public boolean isOn(Module m) {
        return on.contains(m);
    }

    public void toggleModule(Module m) {
        setModule(m, !on.contains(m), false);
    }

    public void setModule(Module m, boolean value, boolean silent) {
        if (value == on.contains(m)) return;
        if (value && !canUse(m)) return;
        if (value) {
            if (m == Module.KILLAURA) setModule(Module.KILLAURA_LEGIT, false, true);
            if (m == Module.KILLAURA_LEGIT) setModule(Module.KILLAURA, false, true);
            on.add(m);
            cheats.onEnable(m);
        } else {
            on.remove(m);
            cheats.onDisable(m);
        }
        if (!silent) say("§7" + m.display + " : " + (value ? "§aON" : "§cOFF"));
    }

    /** Ré-applique un module actif après un changement de réglage (niveau de speed, reach...). */
    public void reapply(Module m) {
        if (on.contains(m)) cheats.reapply(m);
    }

    public void runGamemode(String mode) {
        if (!active()) {
            notAllowed();
            return;
        }
        if (solo()) {
            Server.command("gamemode " + mode + " " + me());
            say("§7Mode de jeu : §b" + mode);
        } else {
            // En multijoueur, seul le serveur peut changer ton mode : il faut y être OP.
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamemode " + mode);
            say("§7Commande /gamemode " + mode + " envoyée §8(il faut être OP sur le serveur)");
        }
    }

    /** Touche X : x-ray + sneak + killaura legit. Re-appuie pour couper. */
    public void runPreset() {
        if (!active()) {
            notAllowed();
            return;
        }
        boolean value = !(isOn(Module.XRAY) && isOn(Module.SNEAK) && isOn(Module.KILLAURA_LEGIT));
        setModule(Module.XRAY, value, false);
        setModule(Module.SNEAK, value, false);
        setModule(Module.KILLAURA_LEGIT, value, false);
    }

    public void disableAll() {
        for (Module m : List.copyOf(on)) setModule(m, false, true);
    }

    // =================================================================== chat

    /** @return false pour annuler l'envoi du message. */
    private boolean onChat(String message) {
        String msg = message.trim();
        if (!msg.startsWith(PREFIX) || msg.length() <= PREFIX.length()) return true;
        String[] args = msg.substring(PREFIX.length()).trim().split("\\s+");
        String cmd = args[0].toLowerCase(Locale.ROOT);
        boolean known = Module.byCommand(cmd) != null || switch (cmd) {
            case "gm", "gamemode", "x", "off", "list", "help", "menu", "allow", "disallow", "servers" -> true;
            default -> false;
        };
        if (!known) return true; // ex: "-_-" reste un message normal
        MinecraftClient.getInstance().execute(() -> handle(args));
        return false;
    }

    private void handle(String[] args) {
        String cmd = args[0].toLowerCase(Locale.ROOT);
        String arg = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (cmd) {
            case "help" -> { help(); return; }
            case "menu" -> { MinecraftClient.getInstance().setScreen(new ClickGui()); return; }
            case "allow" -> { allow(); return; }
            case "disallow" -> { disallow(); return; }
            case "servers" -> {
                List<String> list = Settings.INSTANCE.allowedServers;
                say("§bServeurs autorisés : " + (list.isEmpty() ? "aucun" : String.join(", ", list)));
                return;
            }
            default -> {}
        }
        if (!active()) {
            notAllowed();
            return;
        }
        switch (cmd) {
            case "gm", "gamemode" -> gamemode(arg);
            case "x" -> runPreset();
            case "off" -> { disableAll(); say("§eTous les modules sont désactivés."); }
            case "list" -> say("§bActifs : " + (on.isEmpty() ? "aucun"
                    : on.stream().map(m -> m.display).collect(Collectors.joining(", "))));
            default -> {
                Module m = Module.byCommand(cmd);
                if (m == Module.KILLAURA && arg.equals("legit")) m = Module.KILLAURA_LEGIT;
                else if (!arg.isEmpty() && setValue(m, arg)) return;
                toggleModule(m);
                if (m == Module.CLICKTP && isOn(m)) say("§7Clic droit avec une plume pour te téléporter.");
                if (m == Module.ZOOM && isOn(m)) say("§7Maintiens C pour zoomer.");
            }
        }
    }

    /** "-speed 5", "-reach 7", "-timer 60" : règle le premier réglage du module et l'active. */
    private boolean setValue(Module m, String arg) {
        List<Settings.Setting> settings = Settings.of(m);
        if (settings.isEmpty()) return false;
        double v;
        try {
            v = Double.parseDouble(arg.replace(',', '.'));
        } catch (NumberFormatException e) {
            return false;
        }
        Settings.Setting s = settings.get(0);
        s.set().accept(Math.max(s.min(), Math.min(s.max(), v)));
        if (!isOn(m)) setModule(m, true, true);
        say("§7" + m.display + " : §aON §7(" + s.name() + " " + s.format() + ")");
        return true;
    }

    private void allow() {
        String s = currentServer();
        if (s == null) {
            say("§cRejoins d'abord le serveur, puis tape -allow.");
            return;
        }
        if (!Settings.INSTANCE.allowedServers.contains(s)) {
            Settings.INSTANCE.allowedServers.add(s);
            Settings.save();
        }
        say("§a" + s + " autorisé. §7Ouvre le menu (Shift droit) ou tape -help.");
        say("§eN'autorise que des serveurs anarchie : ailleurs les cheats sont interdits et tu risques un ban.");
    }

    private void disallow() {
        String s = currentServer();
        if (s == null || !Settings.INSTANCE.allowedServers.remove(s)) {
            say("§7Ce serveur n'était pas dans la liste.");
            return;
        }
        Settings.save();
        disableAll();
        say("§e" + s + " retiré de la liste, modules coupés.");
    }

    private void gamemode(String arg) {
        String mode = switch (arg) {
            case "creative", "creatif", "créatif", "c", "1" -> "creative";
            case "survival", "survie", "s", "0" -> "survival";
            case "spectator", "spectateur", "spec", "sp", "3" -> "spectator";
            case "adventure", "aventure", "a", "2" -> "adventure";
            default -> null;
        };
        if (mode == null) say("§cUsage : -gm creative | survival | spectator | adventure");
        else runGamemode(mode);
    }

    private void help() {
        say("§6===== CheatMode =====");
        say("§7Solo : tout marche. Multijoueur : §e-allow§7 sur un serveur anarchie, §e-disallow§7, §e-servers");
        say("§eShift droit §7: menu   §eX §7: x-ray + sneak + killaura legit   §eC §7: zoom");
        say("§e-gm <creative|survival|spectator|adventure>§7, §e-list§7, §e-off§7, §e-x");
        say("§7Réglage direct : §e-speed 5§7, §e-reach 7§7, §e-timer 60§7, §e-killaura legit");
        for (Category c : Category.values()) {
            String list = java.util.Arrays.stream(Module.values()).filter(m -> m.category == c)
                    .map(m -> PREFIX + m.command()).collect(Collectors.joining(" "));
            say(String.format("§%s%s §7: %s", c == Category.COMBAT ? "c" : c == Category.MOUVEMENT ? "b"
                    : c == Category.RENDU ? "d" : c == Category.JOUEUR ? "a" : "6", c.display, list));
        }
    }

    // =================================================================== utilitaires

    static String me() {
        return MinecraftClient.getInstance().player.getUuidAsString();
    }

    static void say(String msg) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§6[CheatMode] §r" + msg));
    }
}
