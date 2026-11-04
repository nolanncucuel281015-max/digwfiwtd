package fr.cheatmode;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CheatModePlugin extends JavaPlugin {

    private CheatState state;
    private XrayManager xray;
    private CommandHandler commands;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        state = new CheatState();
        xray = new XrayManager(this);
        commands = new CommandHandler(this);

        Bukkit.getPluginManager().registerEvents(new CheatListener(this), this);
        getCommand("cheatmode").setExecutor(commands);

        // Boucle principale : 1 fois par tick (20 fois par seconde)
        Bukkit.getScheduler().runTaskTimer(this, new CheatTask(this), 1L, 1L);
        // Rafraîchissement du x-ray
        long refresh = Math.max(10L, getConfig().getLong("xray.refresh-ticks", 40L));
        Bukkit.getScheduler().runTaskTimer(this, xray::refreshAll, refresh, refresh);

        getLogger().info("CheatMode activé. Tape -help dans le chat.");
    }

    @Override
    public void onDisable() {
        if (xray != null) xray.clearAll();
    }

    public CheatState state() { return state; }
    public XrayManager xray() { return xray; }
    public CommandHandler commands() { return commands; }
}
