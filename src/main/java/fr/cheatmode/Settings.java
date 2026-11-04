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
        return panels.computeIfAbsent(c.name(), k -> new PanelPos(10 + c.ordinal() * 115, 30));
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

    public static List<Setting> of(Module m) {
        Settings s = INSTANCE;
        return switch (m) {
            case KILLAURA -> List.of(new Setting("Portée", () -> s.killauraRange, v -> s.killauraRange = v, 3, 6, 0.5));
            case KILLAURA_LEGIT -> List.of(
                    new Setting("Portée", () -> s.legitRange, v -> s.legitRange = v, 2, 4.5, 0.25),
                    new Setting("Angle", () -> s.legitFov, v -> s.legitFov = v, 30, 180, 10));
            case TPAURA -> List.of(new Setting("Portée", () -> s.tpauraRange, v -> s.tpauraRange = v, 4, 32, 2));
            case AUTOCLICK -> List.of(new Setting("CPS", () -> s.autoclickCps, v -> s.autoclickCps = v, 1, 20, 1));
            case SPEED -> List.of(new Setting("Niveau", () -> s.speedLevel, v -> {
                s.speedLevel = v;
                CheatModeClient.get().refreshSpeed();
            }, 1, 10, 1));
            case CLICKTP -> List.of(new Setting("Portée", () -> s.clicktpRange, v -> s.clicktpRange = v, 20, 200, 10));
            case XRAY -> List.of(new Setting("Rayon", () -> s.xrayRadius, v -> s.xrayRadius = v, 6, 32, 2));
            default -> List.of();
        };
    }
}
