package fr.cheatmode;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Menu (Shift droit) : un panneau par catégorie.
 * - clic gauche sur un module : ON / OFF
 * - clic droit sur un module : affiche ses réglages
 * - réglage : clic gauche = +, clic droit = -
 * - "Modifier" : permet de déplacer les panneaux en les tirant par leur titre
 * - clic droit sur un titre : replier / déplier le panneau
 * - "Sauvegarder" : enregistre positions et réglages dans config/cheatmode.json
 */
public final class ClickGui extends Screen {

    private static final int W = 110, HEADER = 14, ROW = 13, SUB = 11;

    /** Ordre d'affichage : le dernier est au-dessus. */
    private final List<Category> order = new ArrayList<>(List.of(Category.values()));
    private final Set<Module> expanded = EnumSet.noneOf(Module.class);

    private boolean editMode;
    private boolean dirty;
    private Category dragging;
    private int dragOffX, dragOffY;
    private String status = "";
    private long statusUntil;

    public ClickGui() {
        super(Text.literal("CheatMode"));
    }

    // =================================================================== boutons

    @Override
    protected void init() {
        int y = height - 26, w = 90, gap = 6;
        int x = width / 2 - (4 * w + 3 * gap) / 2;
        addDrawableChild(ButtonWidget.builder(editLabel(), b -> {
            editMode = !editMode;
            b.setMessage(editLabel());
        }).dimensions(x, y, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sauvegarder"), b -> {
            boolean ok = Settings.save();
            if (ok) dirty = false;
            flash(ok ? "§aSauvegardé !" : "§cErreur de sauvegarde");
        }).dimensions(x + (w + gap), y, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Réinitialiser"), b -> {
            Settings.INSTANCE.resetLayout();
            dirty = true;
            flash("§ePositions par défaut (pense à sauvegarder)");
        }).dimensions(x + 2 * (w + gap), y, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), b -> close())
                .dimensions(x + 3 * (w + gap), y, w, 20).build());
    }

    private Text editLabel() {
        return Text.literal(editMode ? "§aModifier : ON" : "Modifier : OFF");
    }

    private void flash(String msg) {
        status = msg;
        statusUntil = System.currentTimeMillis() + 2500;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // =================================================================== contenu des panneaux

    private record Row(String label, boolean active, boolean sub, String desc, Click click) {}

    @FunctionalInterface
    private interface Click {
        void on(int button);
    }

    private List<Row> rows(Category c) {
        CheatModeClient cm = CheatModeClient.get();
        List<Row> rows = new ArrayList<>();
        for (Module m : Module.values()) {
            if (m.category != c) continue;
            List<Settings.Setting> settings = Settings.of(m);
            String arrow = settings.isEmpty() ? "" : (expanded.contains(m) ? " §7▾" : " §7▸");
            boolean blocked = !CheatModeClient.solo() && !m.multiplayer();
            String label = blocked ? "§8" + m.display + " (solo)" : m.display + arrow;
            String desc = blocked ? m.description + " — solo uniquement" : m.description;
            rows.add(new Row(label, cm.isOn(m), false, desc, button -> {
                if (button == 0) cm.toggleModule(m);
                else if (button == 1 && !settings.isEmpty()) {
                    if (!expanded.remove(m)) expanded.add(m);
                }
            }));
            if (expanded.contains(m)) {
                for (Settings.Setting s : settings) {
                    rows.add(new Row("§7" + s.name() + " : §f" + s.format(), false, true,
                            "Clic gauche : +   Clic droit : -", button -> {
                        s.change(button == 1 ? -1 : 1);
                        dirty = true;
                    }));
                }
            }
        }
        if (c == Category.JOUEUR) {
            GameMode current = client != null && client.interactionManager != null
                    ? client.interactionManager.getCurrentGameMode() : null;
            rows.add(gm("Créatif", "creative", GameMode.CREATIVE, current));
            rows.add(gm("Survie", "survival", GameMode.SURVIVAL, current));
            rows.add(gm("Spectateur", "spectator", GameMode.SPECTATOR, current));
            rows.add(gm("Aventure", "adventure", GameMode.ADVENTURE, current));
            boolean preset = cm.isOn(Module.XRAY) && cm.isOn(Module.SNEAK) && cm.isOn(Module.KILLAURA_LEGIT);
            rows.add(new Row("Preset X", preset, false, "X-Ray + Sneak + KillAura Legit (touche X)",
                    b -> { if (b == 0) cm.runPreset(); }));
        }
        return rows;
    }

    private Row gm(String label, String cmd, GameMode mode, GameMode current) {
        return new Row("GM " + label, mode == current, false, "Passe en mode " + label.toLowerCase(),
                b -> { if (b == 0) CheatModeClient.get().runGamemode(cmd); });
    }

    private int panelHeight(Category c, List<Row> rows) {
        if (!Settings.INSTANCE.panel(c).open) return HEADER;
        int h = HEADER;
        for (Row r : rows) h += r.sub ? SUB : ROW;
        return h + 2;
    }

    // =================================================================== rendu

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x66000000); // fond sombre léger, sans flou
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // fond + boutons
        String hoverDesc = null;

        for (Category c : order) {
            Settings.PanelPos pos = Settings.INSTANCE.panel(c);
            // Reste dans l'écran (si la fenêtre a été réduite)
            pos.x = Math.max(0, Math.min(width - W, pos.x));
            pos.y = Math.max(0, Math.min(height - HEADER, pos.y));
            List<Row> rows = rows(c);
            int h = panelHeight(c, rows);

            ctx.fill(pos.x, pos.y, pos.x + W, pos.y + h, 0xE0101018);
            ctx.fill(pos.x, pos.y, pos.x + W, pos.y + HEADER, c.color);
            String title = c.display + (pos.open ? "" : " §7(+)");
            ctx.drawTextWithShadow(textRenderer, title, pos.x + 4, pos.y + 3, 0xFFFFFFFF);
            if (editMode) {
                ctx.drawTextWithShadow(textRenderer, "✥", pos.x + W - 10, pos.y + 3, 0xFFFFFFFF);
                border(ctx, pos.x - 1, pos.y - 1, W + 2, h + 2, dragging == c ? 0xFFFFFFFF : 0xFFFFD040);
            }
            if (!pos.open) continue;

            int y = pos.y + HEADER + 1;
            for (Row r : rows) {
                int rh = r.sub ? SUB : ROW;
                boolean hover = !editMode && mouseX >= pos.x && mouseX < pos.x + W && mouseY >= y && mouseY < y + rh;
                if (r.active) ctx.fill(pos.x + 1, y, pos.x + W - 1, y + rh, (c.color & 0x00FFFFFF) | 0x90000000);
                if (hover) hoverDesc = r.desc;
                if (hover) ctx.fill(pos.x + 1, y, pos.x + W - 1, y + rh, 0x30FFFFFF);
                ctx.drawTextWithShadow(textRenderer, r.label, pos.x + (r.sub ? 10 : 5), y + (r.sub ? 2 : 3),
                        r.active ? 0xFFFFFFFF : 0xFFC8C8C8);
                y += rh;
            }
        }

        if (hoverDesc != null) {
            int tw = textRenderer.getWidth(hoverDesc);
            int tx = Math.min(mouseX + 10, width - tw - 6), ty = mouseY - 14;
            ctx.fill(tx - 3, ty - 3, tx + tw + 3, ty + 11, 0xF0000000);
            ctx.drawTextWithShadow(textRenderer, hoverDesc, tx, ty, 0xFFFFFFFF);
        }

        String mode = CheatModeClient.solo() ? "§aMode : solo (tous les modules)"
                : CheatModeClient.allowedServer() ? "§aServeur autorisé : " + CheatModeClient.currentServer()
                : CheatModeClient.currentServer() != null ? "§cServeur non autorisé — tape -allow si c'est un serveur anarchie"
                : "";
        ctx.drawCenteredTextWithShadow(textRenderer, mode, width / 2, height - 64, 0xFFFFFFFF);

        // Aide + état en bas
        String help = editMode
                ? "§eMode modification : tire les panneaux par leur titre"
                : "§7Clic gauche : ON/OFF  •  Clic droit : réglages  •  Réglage : gauche +, droit -";
        ctx.drawCenteredTextWithShadow(textRenderer, help, width / 2, height - 40, 0xFFFFFFFF);
        if (System.currentTimeMillis() < statusUntil) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 52, 0xFFFFFFFF);
        } else if (dirty) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§e● Modifications non sauvegardées",
                    width / 2, height - 52, 0xFFFFFFFF);
        }
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    // =================================================================== souris

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true; // boutons du bas
        double mx = click.x(), my = click.y();
        int button = click.button();

        for (int i = order.size() - 1; i >= 0; i--) { // du panneau du dessus vers le dessous
            Category c = order.get(i);
            Settings.PanelPos pos = Settings.INSTANCE.panel(c);
            List<Row> rows = rows(c);
            int h = panelHeight(c, rows);
            if (mx < pos.x || mx >= pos.x + W || my < pos.y || my >= pos.y + h) continue;

            order.remove(i);
            order.add(c); // passe au premier plan

            if (my < pos.y + HEADER) { // titre
                if (button == 1) {
                    pos.open = !pos.open;
                    dirty = true;
                } else if (button == 0 && editMode) {
                    dragging = c;
                    dragOffX = (int) mx - pos.x;
                    dragOffY = (int) my - pos.y;
                }
                return true;
            }
            if (editMode) return true; // en modification on ne clique pas les modules

            int y = pos.y + HEADER + 1;
            for (Row r : rows) {
                int rh = r.sub ? SUB : ROW;
                if (my >= y && my < y + rh) {
                    r.click.on(button);
                    return true;
                }
                y += rh;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x(), my = click.y();
        if (dragging != null && click.button() == 0) {
            Settings.PanelPos pos = Settings.INSTANCE.panel(dragging);
            pos.x = Math.max(0, Math.min(width - W, (int) mx - dragOffX));
            pos.y = Math.max(0, Math.min(height - HEADER, (int) my - dragOffY));
            dirty = true;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null && click.button() == 0) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        if (dirty) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(
                    "§6[CheatMode] §eMenu fermé sans sauvegarder : les changements seront perdus au prochain lancement."));
        }
        super.close();
    }
}
