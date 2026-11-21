package com.coolfreeze23.vbparity.variants.mixin.client;

import com.blackgear.vanillabackport.client.level.model.entity.pig.ColdPigModel;
import com.coolfreeze23.vbparity.variants.client.PigLegMirror;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-09: cold pigs get the native mirrored left-leg UVs (native {@code ColdPigModel} builds on the base pig mesh). */
@Mixin(ColdPigModel.class)
public abstract class ColdPigModelMixin {
    @WrapOperation(
        method = "createBodyLayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/QuadrupedModel;createBodyMesh(ILnet/minecraft/client/model/geom/builders/CubeDeformation;)Lnet/minecraft/client/model/geom/builders/MeshDefinition;")
    )
    private static MeshDefinition vbp$mirrorLeftLegs(int legSize, CubeDeformation deformation, Operation<MeshDefinition> original) {
        return PigLegMirror.mirrorLeftLegs(original.call(legSize, deformation), legSize, deformation);
    }
}
