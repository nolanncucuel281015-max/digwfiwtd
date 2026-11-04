package fr.cheatmode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gère toutes les commandes, qu'elles viennent du chat ("-fly") ou de /cheatmode fly.
 */
public final class CommandHandler implements CommandExecutor {

    private final CheatModePlugin plugin;

    public CommandHandler(CheatModePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        handle(player, args.length == 0 ? new String[]{"help"} : args);
        return true;
    }

    /** @param args args[0] = nom de la commande sans le préfixe ("fly", "gm", ...) */
    public void handle(Player player, String[] args) {
        if (!player.hasPermission("cheatmode.use")) {
            player.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        String cmd = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        UUID id = player.getUniqueId();
        CheatState state = plugin.state();

        switch (cmd) {
            case "gm", "gamemode" -> gamemode(player, rest);
            case "fly" -> {
                boolean on = state.toggle(id, Module.FLY);
                applyFly(player, on);
                feedback(player, Module.FLY, on);
            }
            case "autoclick", "ac" -> toggle(player, Module.AUTOCLICK);
            case "killaura", "ka" -> {
                if (rest.length > 0 && rest[0].equalsIgnoreCase("legit")) {
                    toggleLegitAura(player);
                } else {
                    state.set(id, Module.KILLAURA_LEGIT, false);
                    toggle(player, Module.KILLAURA);
                }
            }
            case "legit", "kalegit" -> toggleLegitAura(player);
            case "tpaura", "tpa" -> toggle(player, Module.TPAURA);
            case "clicktp", "ctp" -> {
                boolean on = toggle(player, Module.CLICKTP);
                if (on) {
                    player.sendMessage(Component.text("Clic droit avec une "
                            + plugin.getConfig().getString("clicktp.item", "FEATHER").toLowerCase(Locale.ROOT)
                            + " pour te téléporter.", NamedTextColor.GRAY));
                }
            }
            case "xray" -> setXray(player, !state.has(id, Module.XRAY));
            case "sneak" -> setSneak(player, !state.has(id, Module.SNEAK));
            case "x" -> presetX(player);
            case "off", "reset" -> {
                disableAll(player);
                player.sendMessage(Component.text("Tous les modules sont désactivés.", NamedTextColor.YELLOW));
            }
            case "list", "status" -> status(player);
            default -> help(player);
        }
    }

    // ---------------------------------------------------------------- gamemode

    private void gamemode(Player player, String[] rest) {
        if (rest.length == 0) {
            player.sendMessage(Component.text("Usage : -gm creative | survival | spectator | adventure", NamedTextColor.RED));
            return;
        }
        GameMode mode = switch (rest[0].toLowerCase(Locale.ROOT)) {
            case "creative", "creatif", "créatif", "c", "1" -> GameMode.CREATIVE;
            case "survival", "survie", "s", "0" -> GameMode.SURVIVAL;
            case "spectator", "spectateur", "spec", "sp", "3" -> GameMode.SPECTATOR;
            case "adventure", "aventure", "a", "2" -> GameMode.ADVENTURE;
            default -> null;
        };
        if (mode == null) {
            player.sendMessage(Component.text("Mode inconnu : " + rest[0], NamedTextColor.RED));
            return;
        }
        player.setGameMode(mode);
        // Le changement de mode coupe le vol en survie : on le remet si le fly est actif.
        if (plugin.state().has(player.getUniqueId(), Module.FLY)) applyFly(player, true);
        player.sendMessage(Component.text("Mode de jeu : ", NamedTextColor.GRAY)
                .append(Component.text(mode.name().toLowerCase(Locale.ROOT), NamedTextColor.AQUA)));
    }

    // ---------------------------------------------------------------- modules

    private boolean toggle(Player player, Module module) {
        boolean on = plugin.state().toggle(player.getUniqueId(), module);
        feedback(player, module, on);
        return on;
    }

    private void toggleLegitAura(Player player) {
        plugin.state().set(player.getUniqueId(), Module.KILLAURA, false);
        toggle(player, Module.KILLAURA_LEGIT);
    }

    private void setXray(Player player, boolean on) {
        plugin.state().set(player.getUniqueId(), Module.XRAY, on);
        if (on) plugin.xray().refresh(player); else plugin.xray().clear(player);
        feedback(player, Module.XRAY, on);
    }

    private void setSneak(Player player, boolean on) {
        plugin.state().set(player.getUniqueId(), Module.SNEAK, on);
        if (!on) player.setSneaking(false);
        feedback(player, Module.SNEAK, on);
    }

    /** "-x" : x-ray + sneak + killaura legit d'un coup (ou tout couper si c'était déjà actif). */
    private void presetX(Player player) {
        UUID id = player.getUniqueId();
        CheatState state = plugin.state();
        boolean on = !(state.has(id, Module.XRAY) && state.has(id, Module.SNEAK) && state.has(id, Module.KILLAURA_LEGIT));
        setXray(player, on);
        setSneak(player, on);
        state.set(id, Module.KILLAURA, false);
        state.set(id, Module.KILLAURA_LEGIT, on);
        feedback(player, Module.KILLAURA_LEGIT, on);
    }

    public void disableAll(Player player) {
        UUID id = player.getUniqueId();
        plugin.xray().clear(player);
        if (plugin.state().has(id, Module.SNEAK)) player.setSneaking(false);
        if (plugin.state().has(id, Module.FLY)) applyFly(player, false);
        plugin.state().clear(id);
    }

    public static void applyFly(Player player, boolean on) {
        GameMode gm = player.getGameMode();
        boolean natural = gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
        if (on) {
            player.setAllowFlight(true);
        } else if (!natural) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    // ---------------------------------------------------------------- messages

    private void feedback(Player player, Module module, boolean on) {
        player.sendMessage(Component.text(module.display() + " : ", NamedTextColor.GRAY)
                .append(on ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED)));
    }

    private void status(Player player) {
        Set<Module> on = plugin.state().get(player.getUniqueId());
        String list = on.isEmpty() ? "aucun" : on.stream().map(Module::display).collect(Collectors.joining(", "));
        player.sendMessage(Component.text("Modules actifs : " + list, NamedTextColor.AQUA));
    }

    private void help(Player player) {
        String p = plugin.getConfig().getString("prefix", "-");
        String[][] lines = {
                {"gm <creative|survival|spectator|adventure>", "change ton mode de jeu"},
                {"fly", "vol"},
                {"autoclick", "clique tout seul sur l'entité que tu vises"},
                {"killaura", "attaque les mobs autour de toi"},
                {"killaura legit", "killaura courte portée, devant toi, au rythme du cooldown"},
                {"tpaura", "se téléporte derrière les mobs et les frappe"},
                {"clicktp", "clic droit avec une plume = téléportation"},
                {"xray", "minerais visibles à travers les murs"},
                {"sneak", "accroupi en permanence"},
                {"x", "x-ray + sneak + killaura legit"},
                {"list", "modules actifs"},
                {"off", "tout désactiver"},
        };
        player.sendMessage(Component.text("===== CheatMode =====", NamedTextColor.GOLD));
        for (String[] l : lines) {
            player.sendMessage(Component.text(p + l[0], NamedTextColor.YELLOW)
                    .append(Component.text(" - " + l[1], NamedTextColor.GRAY)));
        }
    }
}
