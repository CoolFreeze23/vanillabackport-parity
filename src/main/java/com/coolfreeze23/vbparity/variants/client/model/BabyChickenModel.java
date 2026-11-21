package com.coolfreeze23.vbparity.variants.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Port of native 26.2 {@code BabyChickenModel} (16x16). The chick has no separate head; the empty head, beak and
 * red_thing parts only satisfy the 1.21.1 {@code ChickenModel} constructor.
 */
public class BabyChickenModel extends ChickenModel<Chicken> {
    public BabyChickenModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("beak", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("red_thing", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.25F, -0.75F, 4.0F, 4.0F, 4.0F).texOffs(10, 8).addBox(-1.0F, -0.25F, -1.75F, 2.0F, 1.0F, 1.0F),
            PartPose.offset(0.0F, 20.25F, -1.25F)
        );
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(2, 2).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F).texOffs(0, 1).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 0.0F, 1.0F),
            PartPose.offset(1.0F, 22.0F, 0.5F)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 2).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F).texOffs(0, 0).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 0.0F, 1.0F),
            PartPose.offset(-1.0F, 22.0F, 0.5F)
        );
        root.addOrReplaceChild(
            "right_wing", CubeListBuilder.create().texOffs(6, 8).addBox(0.0F, 0.0F, -1.0F, 1.0F, 0.0F, 2.0F), PartPose.offset(2.0F, 20.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "left_wing", CubeListBuilder.create().texOffs(4, 8).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 2.0F), PartPose.offset(-2.0F, 20.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.headParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
        this.bodyParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
    }
}
