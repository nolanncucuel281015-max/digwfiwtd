package fr.cheatmode;

import java.util.List;
import java.util.Locale;

public enum Module {
    // ---- Combat
    KILLAURA("KillAura", Category.COMBAT, "Frappe tous les monstres autour de toi", "ka"),
    KILLAURA_LEGIT("KillAura Legit", Category.COMBAT, "Une cible devant toi, au rythme du cooldown", "legit"),
    TPAURA("TpAura", Category.COMBAT, "Se téléporte derrière les monstres et les frappe", "tpa"),
    AUTOCLICK("AutoClick", Category.COMBAT, "Maintiens clic gauche : clique tout seul", "ac"),
    TRIGGERBOT("TriggerBot", Category.COMBAT, "Frappe dès que tu vises un mob", "trigger"),
    AIMASSIST("AimAssist", Category.COMBAT, "Tourne doucement ta vue vers le monstre proche", "aim", "aimbot"),
    REACH("Reach", Category.COMBAT, "Frappe et casse de plus loin"),
    ANTIKNOCKBACK("AntiKnockback", Category.COMBAT, "Aucun recul quand tu es frappé", "antikb", "velocity"),
    AUTOTOTEM("AutoTotem", Category.COMBAT, "Met un totem dans la main gauche", "totem"),

    // ---- Mouvement
    FLY("Fly", Category.MOUVEMENT, "Vol"),
    SPEED("Speed", Category.MOUVEMENT, "Vitesse"),
    SPRINT("Sprint", Category.MOUVEMENT, "Sprint automatique"),
    NOFALL("NoFall", Category.MOUVEMENT, "Aucun dégât de chute", "nf"),
    CLICKTP("ClickTP", Category.MOUVEMENT, "Clic droit avec une plume = TP", "ctp"),
    SNEAK("Sneak", Category.MOUVEMENT, "Accroupi en permanence"),
    JESUS("Jesus", Category.MOUVEMENT, "Marche sur l'eau et la lave", "waterwalk"),
    SPIDER("Spider", Category.MOUVEMENT, "Grimpe aux murs"),
    HIGHJUMP("HighJump", Category.MOUVEMENT, "Saute plus haut", "hj"),
    STEP("Step", Category.MOUVEMENT, "Monte les blocs sans sauter"),
    BUNNYHOP("BunnyHop", Category.MOUVEMENT, "Saute en continu quand tu avances", "bhop"),
    AIRJUMP("AirJump", Category.MOUVEMENT, "Saute en l'air (saut infini)", "infinitejump"),
    TIMER("Timer", Category.MOUVEMENT, "Accélère tout le jeu"),
    ANTIVOID("AntiVoid", Category.MOUVEMENT, "Te ramène au sol si tu tombes dans le vide"),

    // ---- Rendu
    XRAY("X-Ray", Category.RENDU, "Minerais visibles à travers les murs"),
    STORAGEESP("StorageESP", Category.RENDU, "Coffres, barils, shulkers à travers les murs", "chestesp"),
    MOBESP("MobESP", Category.RENDU, "Contour lumineux sur les mobs", "esp"),
    FULLBRIGHT("Fullbright", Category.RENDU, "Vision nocturne", "fb"),
    ZOOM("Zoom", Category.RENDU, "Maintiens C pour zoomer"),
    NOHURTCAM("NoHurtCam", Category.RENDU, "L'écran ne tremble plus quand tu prends un coup"),
    ALWAYSDAY("AlwaysDay", Category.RENDU, "Toujours le jour", "day"),
    NOWEATHER("NoWeather", Category.RENDU, "Plus de pluie ni d'orage", "noweather"),

    // ---- Joueur
    GOD("GodMode", Category.JOUEUR, "Invincible", "godmode"),
    NOHUNGER("NoHunger", Category.JOUEUR, "Faim toujours pleine", "autoeat"),
    AUTORESPAWN("AutoRespawn", Category.JOUEUR, "Réapparaît tout seul"),
    ANTIFIRE("AntiFire", Category.JOUEUR, "Résistance au feu"),
    NODROWN("NoDrown", Category.JOUEUR, "Respiration sous l'eau"),
    AUTOTOOL("AutoTool", Category.JOUEUR, "Prend le meilleur outil pour miner"),

    // ---- Monde
    NUKER("Nuker", Category.MONDE, "Casse les blocs autour de toi"),
    SCAFFOLD("Scaffold", Category.MONDE, "Pose des blocs sous tes pieds"),
    MAGNET("Magnet", Category.MONDE, "Attire les items et l'XP", "itemmagnet"),
    FASTBREAK("FastBreak", Category.MONDE, "Mine plus vite", "fastmine"),
    CHESTSTEALER("ChestStealer", Category.MONDE, "Vide les coffres tout seul", "stealer");

    public final String display;
    public final Category category;
    public final String description;
    private final List<String> commands;

    Module(String display, Category category, String description, String... aliases) {
        this.display = display;
        this.category = category;
        this.description = description;
        String main = name().toLowerCase(Locale.ROOT).replace("_", "");
        this.commands = new java.util.ArrayList<>(List.of(main));
        this.commands.addAll(List.of(aliases));
    }

    public String command() {
        return commands.get(0);
    }

    public static Module byCommand(String cmd) {
        String c = cmd.toLowerCase(Locale.ROOT);
        for (Module m : values()) if (m.commands.contains(c)) return m;
        return null;
    }
}
