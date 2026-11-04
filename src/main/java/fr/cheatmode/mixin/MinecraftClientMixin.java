package fr.cheatmode.mixin;

import fr.cheatmode.Outlines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Contour lumineux (à travers les murs) pour l'ESP et le X-Ray, uniquement de notre côté. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void cheatmode$outline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (Outlines.shouldOutline(entity)) cir.setReturnValue(true);
        } catch (Throwable ignored) {
            // ne jamais faire planter le rendu
        }
    }
}
