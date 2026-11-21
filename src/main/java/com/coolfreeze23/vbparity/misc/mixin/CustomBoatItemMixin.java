package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.level.items.CustomBoatItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native BoatItem: {@code EntityType.createDefaultStackConfig(level, stack, player).apply(boat)} (custom name, entity data). */
@Mixin(CustomBoatItem.class)
public abstract class CustomBoatItemMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE",
            target = "Lcom/blackgear/vanillabackport/common/level/items/CustomBoatItem;getBoat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/entity/vehicle/Boat;"))
    private Boat vbp$applyStackConfig(CustomBoatItem item, Level level, HitResult hit, Operation<Boat> original,
                                      @Local(argsOnly = true) Player player, @Local(argsOnly = true) InteractionHand hand) {
        Boat boat = original.call(item, level, hit);
        if (level instanceof ServerLevel serverLevel) {
            EntityType.<Boat>createDefaultStackConfig(serverLevel, player.getItemInHand(hand), player).accept(boat);
        }
        return boat;
    }
}
