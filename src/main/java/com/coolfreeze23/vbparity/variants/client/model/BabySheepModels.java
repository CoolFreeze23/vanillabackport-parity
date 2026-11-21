package com.coolfreeze23.vbparity.variants.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.animal.Sheep;

/**
 * VARIANTS-12: port of native 26.2 {@code BabySheepModel} (64x32). Native uses the same mesh for the lamb body
 * ({@code SHEEP_BABY}) and its wool ({@code SHEEP_BABY_WOOL}, drawn with {@code sheep_wool_baby.png}).
 * The head-eat animation follows native {@code SheepModel.setupAnim}: the head drops by {@code 9 * ageScale} = 4.5.
 */
public final class BabySheepModels {
    private static final float HEAD_Y = 15.5F;
    private static final float AGE_SCALE = 0.5F;

    private BabySheepModels() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -2.0F, -4.5F, 6.0F, 4.0F, 9.0F), PartPose.offset(0.0F, 17.0F, 0.5F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.5F, -3.5F, 5.0F, 5.0F, 5.0F), PartPose.offset(0.0F, HEAD_Y, -2.5F));
        root.addOrReplaceChild(
            "right_hind_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(-2.0F, 19.0F, 3.0F)
        );
        root.addOrReplaceChild(
            "left_hind_leg", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(2.0F, 19.0F, 3.0F)
        );
        root.addOrReplaceChild(
            "right_front_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(-2.0F, 19.0F, -2.0F)
        );
        root.addOrReplaceChild(
            "left_front_leg", CubeListBuilder.create().texOffs(24, 5).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(2.0F, 19.0F, -2.0F)
        );
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static class Body extends SheepModel<Sheep> {
        public Body(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(Sheep entity, float limbSwing, float limbSwingAmount, float partialTick) {
            super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            this.head.y = HEAD_Y + entity.getHeadEatPositionScale(partialTick) * 9.0F * AGE_SCALE;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.headParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
            this.bodyParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
        }
    }

    public static class Wool extends SheepFurModel<Sheep> {
        public Wool(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(Sheep entity, float limbSwing, float limbSwingAmount, float partialTick) {
            super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            this.head.y = HEAD_Y + entity.getHeadEatPositionScale(partialTick) * 9.0F * AGE_SCALE;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.headParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
            this.bodyParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
        }
    }
}
