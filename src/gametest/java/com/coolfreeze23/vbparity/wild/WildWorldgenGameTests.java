package com.coolfreeze23.vbparity.wild;

import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.flora.WorldgenProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** WILD-01/02 end to end on a real overworld noise generator. */
@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class WildWorldgenGameTests {
    /**
     * Native dappled-forest coarse dirt is a surface rule (above the preliminary surface, on the floor, not under water):
     * generated dappled forest chunks get coarse dirt near the top, never on deep cave floors below the preliminary surface.
     */
    @GameTest(template = "wild_platform", timeoutTicks = 600)
    public static void wild_dappled_coarse_dirt_stays_at_the_surface(GameTestHelper h) {
        WorldgenProbe probe = new WorldgenProbe(h, NoiseGeneratorSettings.OVERWORLD, 12345L);
        ChunkPos first = probe.find(ModBiomes.DAPPLED_FOREST, 8192, 72, 90);
        h.assertTrue(first != null, "no dappled forest within 8192 blocks (WILD-01 placement)");
        int nearSurface = 0, deep = 0;
        StringBuilder deepAt = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            ChunkPos at = new ChunkPos(first.x + i % 3 - 1, first.z + i / 3 - 1);
            ProtoChunk chunk = probe.generate(at);
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = at.getMinBlockX() + dx, z = at.getMinBlockZ() + dz;
                    int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                    for (int y = chunk.getMinBuildHeight(); y <= top; y++) {
                        if (!chunk.getBlockState(p.set(x, y, z)).is(Blocks.COARSE_DIRT)
                            || !WorldgenProbe.chunkBiome(chunk, p).is(ModBiomes.DAPPLED_FOREST)) {
                            continue;
                        }
                        if (y >= top - 24) {
                            nearSurface++;
                        } else {
                            deep++;
                            if (deepAt.length() < 400) {
                                deepAt.append(p.toShortString()).append(" (top ").append(top).append(") ");
                            }
                        }
                    }
                }
            }
        }
        VBParity.LOG.info("[wild_dappled_coarse_dirt_stays_at_the_surface] around {}: {} near the surface, {} deep", first, nearSurface, deep);
        h.assertTrue(nearSurface > 0, "no dappled coarse dirt at all around " + first);
        h.assertTrue(deep == 0, "coarse dirt on deep floors (below the preliminary surface): " + deep + " " + deepAt);
        h.succeed();
    }
}
