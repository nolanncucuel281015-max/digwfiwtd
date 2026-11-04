package fr.cheatmode;

public enum Category {
    COMBAT("Combat", 0xFFE04848),
    MOUVEMENT("Mouvement", 0xFF48A8E0),
    RENDU("Rendu", 0xFFB060F0),
    JOUEUR("Joueur", 0xFF50D070),
    MONDE("Monde", 0xFFE0A030);

    public final String display;
    public final int color;

    Category(String display, int color) {
        this.display = display;
        this.color = color;
    }
}
