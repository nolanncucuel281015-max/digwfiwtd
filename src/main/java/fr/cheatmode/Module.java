package fr.cheatmode;

public enum Module {
    FLY("Fly"),
    AUTOCLICK("AutoClick"),
    KILLAURA("KillAura"),
    KILLAURA_LEGIT("KillAura Legit"),
    TPAURA("TpAura"),
    CLICKTP("ClickTP"),
    XRAY("X-Ray"),
    SNEAK("Sneak");

    private final String display;

    Module(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
