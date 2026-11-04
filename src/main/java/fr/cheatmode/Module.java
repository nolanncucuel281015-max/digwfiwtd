package fr.cheatmode;

public enum Module {
    FLY("Fly"),
    SPEED("Speed"),
    NOFALL("NoFall"),
    AUTOCLICK("AutoClick"),
    KILLAURA("KillAura"),
    KILLAURA_LEGIT("KillAura Legit"),
    TPAURA("TpAura"),
    CLICKTP("ClickTP"),
    XRAY("X-Ray"),
    SNEAK("Sneak");

    public final String display;

    Module(String display) {
        this.display = display;
    }
}
