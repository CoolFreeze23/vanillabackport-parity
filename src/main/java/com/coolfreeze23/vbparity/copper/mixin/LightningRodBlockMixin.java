package com.coolfreeze23.vbparity.copper.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

/**
 * COPPER-11: VB summons its own channeling bolt (bottom centre of the block above, WEATHER thunder at the block corner)
 * for its seven rods. Native handles every rod through the channeling enchantment data, which this add-on overrides to
 * test {@code #minecraft:lightning_rods}. This higher-priority mixin replaces VB's merged {@code vb$onProjectileHit}
 * on lightning rods with the BlockExtension default (a no-op).
 */
@Mixin(value = LightningRodBlock.class, priority = 1500)
public abstract class LightningRodBlockMixin {
    public void vb$onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
    }
}
