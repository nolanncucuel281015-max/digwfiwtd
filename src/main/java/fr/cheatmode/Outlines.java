package fr.cheatmode;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;

public final class Outlines {

    private Outlines() {}

    public static boolean shouldOutline(Entity e) {
        if (BlockEsp.isEspEntity(e)) return true;
        CheatModeClient cm = CheatModeClient.get();
        return cm != null && cm.isOn(Module.MOBESP) && e instanceof LivingEntity
                && !(e instanceof ArmorStandEntity) && e != MinecraftClient.getInstance().player;
    }
}
