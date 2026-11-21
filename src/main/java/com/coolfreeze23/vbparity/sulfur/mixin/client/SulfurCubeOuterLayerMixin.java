package com.coolfreeze23.vbparity.sulfur.mixin.client;

import com.blackgear.vanillabackport.client.level.layer.SulfurCubeOuterLayer;
import com.blackgear.vanillabackport.client.level.model.entity.sulfur_cube.SmallSulfurCubeModel;
import com.blackgear.vanillabackport.client.level.model.entity.sulfur_cube.SulfurCubeModel;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SULFUR-37. Native draws the outer shell as the main model: an invisible cube is drawn at 15% alpha (0x26FFFFFF,
 * itemEntityTranslucentCull) for viewers allowed to see it (spectators, see-friendly-invisibles teammates), and a glowing
 * invisible cube gets its outline from the outer texture.
 */
@Mixin(SulfurCubeOuterLayer.class)
public abstract class SulfurCubeOuterLayerMixin extends RenderLayer<SulfurCube, SulfurCubeModel> {
    @Shadow
    @Final
    private static ResourceLocation SULFUR_CUBE_OUTER_LOCATION;
    @Shadow
    @Final
    private static ResourceLocation SULFUR_CUBE_OUTER_SMALL_LOCATION;
    @Shadow
    @Final
    private SulfurCubeModel normalModel;
    @Shadow
    @Final
    private SmallSulfurCubeModel smallModel;

    protected SulfurCubeOuterLayerMixin(RenderLayerParent<SulfurCube, SulfurCubeModel> renderer) {
        super(renderer);
    }

    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;FFFFFF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void vbp$invisibleShell(
        PoseStack pose, MultiBufferSource buffer, int packedLight, SulfurCube cube, float limbSwing, float limbSwingAmount,
        float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci
    ) {
        if (!cube.isInvisible()) {
            return;
        }
        ci.cancel();
        Minecraft minecraft = Minecraft.getInstance();
        boolean translucent = minecraft.player != null && !cube.isInvisibleTo(minecraft.player);
        boolean glowing = minecraft.shouldEntityAppearGlowing(cube);
        if (!translucent && !glowing) {
            return;
        }

        @SuppressWarnings("unchecked")
        EntityModel<SulfurCube> model = (EntityModel<SulfurCube>) (EntityModel<?>) (cube.isBaby() ? this.smallModel : this.normalModel);
        ResourceLocation texture = cube.isBaby() ? SULFUR_CUBE_OUTER_SMALL_LOCATION : SULFUR_CUBE_OUTER_LOCATION;
        this.getParentModel().copyPropertiesTo(model);
        model.prepareMobModel(cube, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(cube, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        int overlay = LivingEntityRenderer.getOverlayCoords(cube, 0.0F);
        if (translucent) {
            model.renderToBuffer(pose, buffer.getBuffer(RenderType.itemEntityTranslucentCull(texture)), packedLight, overlay, 0x26FFFFFF);
        } else {
            model.renderToBuffer(pose, buffer.getBuffer(RenderType.outline(texture)), packedLight, overlay, -1);
        }
    }
}
