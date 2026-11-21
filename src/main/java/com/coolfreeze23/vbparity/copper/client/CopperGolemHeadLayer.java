package com.coolfreeze23.vbparity.copper.client;

import com.blackgear.vanillabackport.client.level.model.entity.CopperGolemModel;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

/**
 * COPPER-25: native CopperGolemRenderer adds a CustomHeadLayer; with the antenna moved off HEAD the golem can wear a
 * head item again. The 1.21.1 CustomHeadLayer only applies the head part's own pose, so this is the 1.21.1 layer body
 * with native's transform chain: root, body and head poses, then CopperGolemModel.translateToHead's
 * translate(0, 0.125, 0) and scale 1.0625 (golems have no babies and no villager offset).
 */
public final class CopperGolemHeadLayer<T extends CopperGolem, M extends CopperGolemModel<T>> extends RenderLayer<T, M> {
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final ItemInHandRenderer itemInHandRenderer;

    public CopperGolemHeadLayer(RenderLayerParent<T, M> parent, EntityModelSet modelSet, ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.skullModels = SkullBlockRenderer.createSkullRenderers(modelSet);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T golem, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = golem.getItemBySlot(EquipmentSlot.HEAD);
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        poseStack.pushPose();
        ModelPart root = this.getParentModel().root();
        ModelPart body = root.getChild("body");
        ModelPart head = body.getChild("head");
        root.translateAndRotate(poseStack);
        body.translateAndRotate(poseStack);
        head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.125F, 0.0F);
        poseStack.scale(1.0625F, 1.0625F, 1.0625F);
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skull) {
            poseStack.scale(1.1875F, -1.1875F, -1.1875F);
            ResolvableProfile profile = stack.get(DataComponents.PROFILE);
            poseStack.translate(-0.5, 0.0, -0.5);
            SkullBlock.Type type = skull.getType();
            SkullModelBase model = this.skullModels.get(type);
            RenderType renderType = SkullBlockRenderer.getRenderType(type, profile);
            float walk = golem.walkAnimation.position(partialTicks);
            SkullBlockRenderer.renderSkull(null, 180.0F, walk, poseStack, buffer, packedLight, model, renderType);
        } else if (!(item instanceof ArmorItem armor) || armor.getEquipmentSlot() != EquipmentSlot.HEAD) {
            poseStack.translate(0.0F, -0.25F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(0.625F, -0.625F, -0.625F);
            this.itemInHandRenderer.renderItem(golem, stack, ItemDisplayContext.HEAD, false, poseStack, buffer, packedLight);
        }
        poseStack.popPose();
    }
}
