package com.coolfreeze23.vbparity.flora.mixin;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** For the worldgen GameTests: build a chunk's noise chunk on the server thread (its structure lookup must run there). */
@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseBasedChunkGeneratorInvoker {
    @Invoker("createNoiseChunk")
    NoiseChunk vbparity$createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState random);
}
