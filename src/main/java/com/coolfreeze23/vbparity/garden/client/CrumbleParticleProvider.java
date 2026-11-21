package com.coolfreeze23.vbparity.garden.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Native TerrainParticle.CrumblingProvider: a terrain particle with zero speed that lives 1-10 ticks. */
public final class CrumbleParticleProvider implements ParticleProvider<BlockParticleOption> {
    @Nullable
    @Override
    public Particle createParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        BlockState state = options.getState();
        if (state.isAir() || state.is(Blocks.MOVING_PISTON) || !state.shouldSpawnTerrainParticles()) {
            return null;
        }
        TerrainParticle particle = new TerrainParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, state).updateSprite(state, options.getPos());
        particle.setParticleSpeed(0.0, 0.0, 0.0);
        particle.setLifetime(level.random.nextInt(10) + 1);
        return particle;
    }
}
