package com.coolfreeze23.vbparity.variants.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native 26.2 {@code SheepModel.createBodyLayer} builds its legs with {@code createBodyMesh(12, false, true, g)}, which
 * mirrors the UVs of both right legs. 1.21.1 builds them unmirrored. The same layer ({@code ModelLayers.SHEEP}) is what
 * VanillaBackport's wool undercoat bakes, and native's undercoat uses the sheep body layer too, so both get the mirror.
 */
@Mixin(SheepModel.class)
public abstract class SheepModelMixin {
    @WrapOperation(
        method = "createBodyLayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/QuadrupedModel;createBodyMesh(ILnet/minecraft/client/model/geom/builders/CubeDeformation;)Lnet/minecraft/client/model/geom/builders/MeshDefinition;")
    )
    private static MeshDefinition vbp$mirrorRightLegs(int legSize, CubeDeformation deformation, Operation<MeshDefinition> original) {
        return vbp$mirror(original.call(legSize, deformation), legSize, deformation);
    }

    @Unique
    private static MeshDefinition vbp$mirror(MeshDefinition mesh, int legSize, CubeDeformation deformation) {
        PartDefinition root = mesh.getRoot();
        CubeListBuilder rightLeg = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, (float) legSize, 4.0F, deformation);
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-3.0F, (float) (24 - legSize), 7.0F));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-3.0F, (float) (24 - legSize), -5.0F));
        return mesh;
    }
}
