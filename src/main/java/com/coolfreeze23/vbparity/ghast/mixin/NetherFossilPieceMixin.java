package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GHAST-31: the current game places the fossil's dried ghast wherever the fossil is, with no biome check. VB only
 * does it in nether biomes; this runs the same placement (same seed, RNG and checks) for every other biome, so
 * together every fossil gets its roll. Gated by VB's dried_ghasts option like VB's own placement.
 */
@Mixin(NetherFossilPieces.NetherFossilPiece.class)
public abstract class NetherFossilPieceMixin extends TemplateStructurePiece {
    protected NetherFossilPieceMixin(StructurePieceType type, int genDepth, StructureTemplateManager manager, ResourceLocation location,
                                     String templateName, StructurePlaceSettings settings, BlockPos pos) {
        super(type, genDepth, manager, location, templateName, settings, pos);
    }

    @Inject(method = "postProcess", at = @At("TAIL"))
    private void vbp$driedGhastOutsideNetherBiomes(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                                   RandomSource worldRandom, BoundingBox box, ChunkPos chunkPos, BlockPos origin, CallbackInfo ci) {
        if (!VanillaBackport.COMMON_CONFIG.hasDriedGhasts.get() || level.getBiome(origin).is(BiomeTags.IS_NETHER)) {
            return;
        }

        BoundingBox fossil = this.template().getBoundingBox(this.placeSettings(), this.templatePosition());
        RandomSource random = RandomSource.create(level.getSeed()).forkPositional().at(fossil.getCenter());
        if (random.nextFloat() < 0.5F) {
            int x = fossil.minX() + random.nextInt(fossil.getXSpan());
            int y = fossil.minY();
            int z = fossil.minZ() + random.nextInt(fossil.getZSpan());
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() && box.isInside(pos)) {
                level.setBlock(pos, ModBlocks.DRIED_GHAST.get().defaultBlockState().rotate(Rotation.getRandom(random)), 2);
            }
        }
    }
}
