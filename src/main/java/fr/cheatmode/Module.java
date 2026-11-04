package fr.cheatmode;

public enum Module {
    KILLAURA("KillAura", Category.COMBAT),
    KILLAURA_LEGIT("KillAura Legit", Category.COMBAT),
    TPAURA("TpAura", Category.COMBAT),
    AUTOCLICK("AutoClick", Category.COMBAT),
    FLY("Fly", Category.MOUVEMENT),
    SPEED("Speed", Category.MOUVEMENT),
    NOFALL("NoFall", Category.MOUVEMENT),
    CLICKTP("ClickTP", Category.MOUVEMENT),
    SNEAK("Sneak", Category.MOUVEMENT),
    XRAY("X-Ray", Category.RENDU);

    public final String display;
    public final Category category;

    Module(String display, Category category) {
        this.display = display;
        this.category = category;
    }
}
