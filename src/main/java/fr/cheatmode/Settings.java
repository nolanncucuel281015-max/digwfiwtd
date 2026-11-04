package fr.cheatmode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Réglages + position des panneaux du menu, sauvegardés dans config/cheatmode.json. */
public final class Settings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cheatmode.json");

    public static Settings INSTANCE = new Settings();

    // ---- valeurs (les noms de champs sont ceux du fichier json)
    public double killauraRange = 5.5;
    public double legitRange = 3.0;
    public double legitFov = 70;
    public double tpauraRange = 16;
    public double autoclickCps = 12;
    public double speedLevel = 3;
    public double clicktpRange = 120;
    public double xrayRadius = 14;
    public double storageRadius = 48;
    public double triggerDelay = 0;      // ticks en plus après cooldown
    public double aimSpeed = 8;          // degrés par tick
    public double aimRange = 6;
    public double reach = 6;
    public double timerRate = 40;        // ticks par seconde (20 = normal)
    public double jumpBoost = 2;         // multiplicateur
    public double stepHeight = 1;
    public double zoomFov = 30;
    public double nukerRadius = 4;
    public double magnetRange = 12;
    public double fastBreakLevel = 3;

    public Map<String, PanelPos> panels = new LinkedHashMap<>();

    public static final class PanelPos {
        public int x, y;
        public boolean open = true;

        public PanelPos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Position d'un panneau (crée la position par défaut si besoin). */
    public PanelPos panel(Category c) {
        return panels.computeIfAbsent(c.name(), k -> new PanelPos(6 + c.ordinal() * 116, 6));
    }

    public void resetLayout() {
        panels.clear();
    }

    // ---- sauvegarde

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE)) {
            Settings s = GSON.fromJson(r, Settings.class);
            if (s != null) {
                if (s.panels == null) s.panels = new LinkedHashMap<>();
                INSTANCE = s;
            }
        } catch (Exception e) {
            System.err.println("[CheatMode] Config illisible, valeurs par défaut : " + e);
        }
    }

    public static boolean save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(INSTANCE, w);
            }
            return true;
        } catch (IOException e) {
            System.err.println("[CheatMode] Impossible de sauvegarder : " + e);
            return false;
        }
    }

    // ---- réglages modifiables dans le menu

    public record Setting(String name, DoubleSupplier get, DoubleConsumer set, double min, double max, double step) {
        public void change(int direction) {
            double v = Math.round((get.getAsDouble() + direction * step) / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }

        public String format() {
            double v = get.getAsDouble();
            return v == Math.rint(v) ? Integer.toString((int) v) : String.format(java.util.Locale.ROOT, "%.2f", v);
        }
    }

    /** Réglage qui doit être ré-appliqué tout de suite (effet, attribut...) quand il change. */
    private static Setting live(Module m, String name, DoubleSupplier get, DoubleConsumer set,
                                double min, double max, double step) {
        return new Setting(name, get, v -> {
            set.accept(v);
            CheatModeClient.get().reapply(m);
        }, min, max, step);
    }

    public static List<Setting> of(Module m) {
        Settings s = INSTANCE;
        return switch (m) {
            case KILLAURA -> List.of(new Setting("Portée", () -> s.killauraRange, v -> s.killauraRange = v, 3, 6, 0.5));
            case KILLAURA_LEGIT -> List.of(
                    new Setting("Portée", () -> s.legitRange, v -> s.legitRange = v, 2, 4.5, 0.25),
                    new Setting("Angle", () -> s.legitFov, v -> s.legitFov = v, 30, 180, 10));
            case TPAURA -> List.of(new Setting("Portée", () -> s.tpauraRange, v -> s.tpauraRange = v, 4, 32, 2));
            case AUTOCLICK -> List.of(new Setting("CPS", () -> s.autoclickCps, v -> s.autoclickCps = v, 1, 20, 1));
            case SPEED -> List.of(live(m, "Niveau", () -> s.speedLevel, v -> s.speedLevel = v, 1, 10, 1));
            case CLICKTP -> List.of(new Setting("Portée", () -> s.clicktpRange, v -> s.clicktpRange = v, 20, 200, 10));
            case XRAY -> List.of(new Setting("Rayon", () -> s.xrayRadius, v -> s.xrayRadius = v, 6, 32, 2));
            case STORAGEESP -> List.of(new Setting("Rayon", () -> s.storageRadius, v -> s.storageRadius = v, 16, 96, 8));
            case TRIGGERBOT -> List.of(new Setting("Délai", () -> s.triggerDelay, v -> s.triggerDelay = v, 0, 20, 1));
            case AIMASSIST -> List.of(
                    new Setting("Vitesse", () -> s.aimSpeed, v -> s.aimSpeed = v, 1, 30, 1),
                    new Setting("Portée", () -> s.aimRange, v -> s.aimRange = v, 3, 12, 1));
            case REACH -> List.of(live(m, "Portée", () -> s.reach, v -> s.reach = v, 3, 8, 0.5));
            case TIMER -> List.of(live(m, "Ticks/s", () -> s.timerRate, v -> s.timerRate = v, 5, 100, 5));
            case HIGHJUMP -> List.of(live(m, "Force", () -> s.jumpBoost, v -> s.jumpBoost = v, 1, 5, 0.5));
            case STEP -> List.of(live(m, "Hauteur", () -> s.stepHeight, v -> s.stepHeight = v, 1, 5, 0.5));
            case ZOOM -> List.of(new Setting("FOV", () -> s.zoomFov, v -> s.zoomFov = v, 30, 60, 5));
            case NUKER -> List.of(new Setting("Rayon", () -> s.nukerRadius, v -> s.nukerRadius = v, 1, 6, 1));
            case MAGNET -> List.of(new Setting("Portée", () -> s.magnetRange, v -> s.magnetRange = v, 4, 32, 2));
            case FASTBREAK -> List.of(live(m, "Niveau", () -> s.fastBreakLevel, v -> s.fastBreakLevel = v, 1, 10, 1));
            default -> List.of();
        };
    }
}
