package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.core.data.ModBuiltInLootTables;
import com.blackgear.vanillabackport.core.util.WorldUtilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VARIANTS-18: egg laying as native {@code Chicken.aiStep}: every chicken (temperate included) drops whatever the
 * {@code gameplay/chicken_lay} gift loot table yields, and the egg sound and {@code ENTITY_PLACE} event happen only when
 * something dropped. The lay timer still ticks once per {@code aiStep}; the vanilla 1.21.1 block (and VanillaBackport's
 * drop hook inside it) is kept from running by restoring the one tick it is about to take.
 */
@Mixin(Chicken.class)
public abstract class ChickenLayMixin extends Animal {
    @Shadow
    public int eggTime;

    protected ChickenLayMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract boolean isChickenJockey();

    @Inject(
        method = "aiStep",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/animal/Chicken;flap:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER)
    )
    private void vbp$layEgg(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel level && this.isAlive() && !this.isBaby() && !this.isChickenJockey()) {
            if (--this.eggTime <= 0) {
                if (WorldUtilities.LootUtils.dropFromGiftLootTable(this, level, ModBuiltInLootTables.CHICKEN_LAY, (l, stack) -> this.spawnAtLocation(stack))) {
                    this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                    this.gameEvent(GameEvent.ENTITY_PLACE);
                }

                this.eggTime = this.random.nextInt(6000) + 6000;
            }

            // Vanilla's condition decrements again right after this; give that tick back so it never reaches zero.
            this.eggTime++;
        }
    }
}
