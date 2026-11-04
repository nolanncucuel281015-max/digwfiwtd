package fr.cheatmode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Garde en mémoire quels modules sont activés pour chaque joueur. */
public final class CheatState {

    private final Map<UUID, Set<Module>> enabled = new HashMap<>();

    public boolean has(UUID id, Module module) {
        Set<Module> set = enabled.get(id);
        return set != null && set.contains(module);
    }

    public void set(UUID id, Module module, boolean on) {
        Set<Module> set = enabled.computeIfAbsent(id, k -> EnumSet.noneOf(Module.class));
        if (on) set.add(module); else set.remove(module);
    }

    /** Inverse l'état du module et renvoie le nouvel état. */
    public boolean toggle(UUID id, Module module) {
        boolean now = !has(id, module);
        set(id, module, now);
        return now;
    }

    public Set<Module> get(UUID id) {
        Set<Module> set = enabled.get(id);
        return set == null ? EnumSet.noneOf(Module.class) : EnumSet.copyOf(set);
    }

    public Set<UUID> players() {
        return enabled.keySet();
    }

    public void clear(UUID id) {
        enabled.remove(id);
    }
}
