package fr.cheatmode;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Accès au serveur intégré du monde solo. Tout ce qui doit être "vrai" côté serveur
 * (mode de jeu, téléportation, vol, effets) passe par ici.
 */
public final class Server {

    private Server() {}

    /** true seulement en monde solo (serveur intégré). */
    public static boolean available() {
        return MinecraftClient.getInstance().getServer() != null;
    }

    /** Exécute des commandes avec les droits du serveur, sans message dans le chat. */
    public static void commands(List<String> cmds) {
        IntegratedServer s = MinecraftClient.getInstance().getServer();
        if (s == null || cmds.isEmpty()) return;
        List<String> copy = List.copyOf(cmds);
        s.execute(() -> {
            var source = s.getCommandSource().withSilent();
            for (String c : copy) s.getCommandManager().parseAndExecute(source, c);
        });
    }

    public static void command(String cmd) {
        commands(List.of(cmd));
    }

    /** Exécute du code sur le thread serveur avec la version serveur du joueur. */
    public static void withPlayer(UUID id, Consumer<ServerPlayerEntity> action) {
        IntegratedServer s = MinecraftClient.getInstance().getServer();
        if (s == null) return;
        s.execute(() -> {
            ServerPlayerEntity sp = s.getPlayerManager().getPlayer(id);
            if (sp != null) action.accept(sp);
        });
    }
}
