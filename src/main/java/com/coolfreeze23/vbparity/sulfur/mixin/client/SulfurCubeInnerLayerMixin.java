package com.coolfreeze23.vbparity.sulfur.mixin.client;

import com.blackgear.vanillabackport.client.level.layer.SulfurCubeInnerLayer;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SULFUR-36. Native renders the swallowed block with the stack's block_state component applied, and with light
 * max(entity light, block light emission) (full bright when the block renders emissively, e.g. magma).
 */
@Mixin(SulfurCubeInnerLayer.class)
public abstract class SulfurCubeInnerLayerMixin {
    @ModifyExpressionValue(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;FFFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private BlockState vbp$applyBlockStateComponent(BlockState state, @Local(argsOnly = true) SulfurCube cube) {
        return cube.getContainedBlock().getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
    }

    @WrapOperation(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;FFFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/client/level/layer/SulfurCubeInnerLayer;renderSingleBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
        )
    )
    private void vbp$blockLight(
        SulfurCubeInnerLayer self, BlockState state, PoseStack pose, MultiBufferSource buffer, int packedLight, int overlay,
        Operation<Void> original, @Local(argsOnly = true) SulfurCube cube
    ) {
        int light;
        if (state.emissiveRendering(cube.level(), cube.blockPosition())) {
            light = LightTexture.FULL_BRIGHT;
        } else {
            light = LightTexture.pack(Math.max(LightTexture.block(packedLight), state.getLightEmission()), LightTexture.sky(packedLight));
        }
        original.call(self, state, pose, buffer, light, overlay);
    }
}
