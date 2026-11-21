package com.coolfreeze23.vbparity.flora;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jetbrains.annotations.Nullable;

/**
 * A real overworld noise generator for a chosen preset, built from the server's registries, plus a probe that runs one
 * chunk through biomes, noise and surface (the GameTest world itself is flat). Shared by the worldgen GameTests.
 */
public final class WorldgenProbe {
    public final ServerLevel level;
    public final Holder<NoiseGeneratorSettings> settings;
    public final MultiNoiseBiomeSource biomeSource;
    public final NoiseBasedChunkGenerator generator;
    public final RandomState randomState;
    public final long seed;
    public final Registry<Biome> biomes;
    public final LevelHeightAccessor height;

    public WorldgenProbe(GameTestHelper h, ResourceKey<NoiseGeneratorSettings> preset, long seed) {
        this.level = h.getLevel();
        var access = this.level.registryAccess();
        this.settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(preset);
        Holder<MultiNoiseBiomeSourceParameterList> params = access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
            .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        this.biomeSource = MultiNoiseBiomeSource.createFromPreset(params);
        this.generator = new NoiseBasedChunkGenerator(this.biomeSource, this.settings);
        this.randomState = RandomState.create(this.settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        this.seed = seed;
        this.biomes = access.registryOrThrow(Registries.BIOME);
        this.height = LevelHeightAccessor.create(this.settings.value().noiseSettings().minY(), this.settings.value().noiseSettings().height());
    }

    public Holder<Biome> biomeAt(int x, int y, int z) {
        return this.biomeSource.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), this.randomState.sampler());
    }

    /** First chunk (spiral-free grid scan, step 64) whose column at one of the given y's is the biome. */
    @Nullable
    public ChunkPos find(ResourceKey<Biome> biome, int radius, int... ys) {
        for (int r = 0; r <= radius; r += 64) {
            for (int x = -r; x <= r; x += 64) {
                for (int z = -r; z <= r; z += 64) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    for (int y : ys) {
                        if (this.biomeAt(x, y, z).is(biome)) {
                            return new ChunkPos(x >> 4, z >> 4);
                        }
                    }
                }
            }
        }
        return null;
    }

    /** One chunk through biomes, noise and surface (no carvers, no features). */
    public ProtoChunk generate(ChunkPos pos) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, this.height, this.biomes, null);
        var structures = this.level.structureManager();
        // the noise chunk's structure (beardifier) lookup loads a level chunk, which must happen on this thread; the async
        // stages below then reuse the cached noise chunk
        chunk.getOrCreateNoiseChunk(c -> ((com.coolfreeze23.vbparity.flora.mixin.NoiseBasedChunkGeneratorInvoker) this.generator)
            .vbparity$createNoiseChunk(c, structures, Blender.empty(), this.randomState));
        this.generator.createBiomes(this.randomState, Blender.empty(), structures, chunk).join();
        chunk.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
        this.generator.fillFromNoise(Blender.empty(), this.randomState, structures, chunk).join();
        chunk.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.NOISE);
        this.generator.buildSurface(
            chunk, new WorldGenerationContext(this.generator, this.height), this.randomState, structures,
            new BiomeManager(chunk, BiomeManager.obfuscateSeed(this.seed)), this.biomes, Blender.empty()
        );
        return chunk;
    }

    /** Count of block positions in a chunk matching both tests. */
    public static int count(ChunkAccess chunk, Predicate<BlockPos> where, Predicate<net.minecraft.world.level.block.state.BlockState> what) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    p.set(x0 + dx, y, z0 + dz);
                    if (what.test(chunk.getBlockState(p)) && where.test(p)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** Biome of a generated chunk at a block (from its own noise biomes). */
    public static Holder<Biome> chunkBiome(ChunkAccess chunk, BlockPos pos) {
        return chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
    }

    /** Sets every biome of a loaded chunk of the (flat) test level to one biome. */
    public static void paintBiome(LevelChunk chunk, Holder<Biome> biome, RandomState state) {
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, state.sampler());
        chunk.setUnsaved(true);
    }
}
