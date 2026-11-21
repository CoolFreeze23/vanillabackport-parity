package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** GHAST-07: the four happy ghast seats sit 1.7 blocks from the centre, not 1.8. */
@Mixin(ModEntityTypes.class)
public abstract class ModEntityTypesMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType$Builder;passengerAttachments([Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static Vec3[] vbp$happyGhastSeats(Vec3[] seats) {
        return new Vec3[]{new Vec3(0.0, 4.0, 1.7), new Vec3(-1.7, 4.0, 0.0), new Vec3(0.0, 4.0, -1.7), new Vec3(1.7, 4.0, 0.0)};
    }
}
