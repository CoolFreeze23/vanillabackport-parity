package com.coolfreeze23.vbparity.variants.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * VARIANTS-09: native {@code PigModel.createBasePigModel} builds its legs with {@code createBodyMesh(6, true, false, g)},
 * which mirrors the UVs of both left legs. Rebuilds the two left legs of a 1.21.1 {@code createBodyMesh(6, g)} mesh.
 */
public final class PigLegMirror {
    private PigLegMirror() {
    }

    public static MeshDefinition mirrorLeftLegs(MeshDefinition mesh, int legSize, CubeDeformation deformation) {
        PartDefinition root = mesh.getRoot();
        CubeListBuilder leftLeg = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, (float) legSize, 4.0F, deformation);
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(3.0F, (float) (24 - legSize), 7.0F));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(3.0F, (float) (24 - legSize), -5.0F));
        return mesh;
    }
}
