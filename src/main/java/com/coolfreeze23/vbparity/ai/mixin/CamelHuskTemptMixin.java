package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-MNT-02: native camels use the FOOD_TEMPTATIONS sensor (Animal.isFood), so a camel husk is tempted by its own food
 * (rabbit foot). The 1.21.1 camel brain VB reuses has CAMEL_TEMPTATIONS, fixed to #camel_food (cactus).
 */
@Mixin(TemptingSensor.class)
public abstract class CamelHuskTemptMixin {
    @Unique
    private static final ThreadLocal<PathfinderMob> VBP$BODY = new ThreadLocal<>();

    @Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;)V", at = @At("HEAD"))
    private void vbp$rememberBody(ServerLevel level, PathfinderMob body, CallbackInfo ci) {
        VBP$BODY.set(body instanceof CamelHusk ? body : null);
    }

    @Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;)V", at = @At("RETURN"))
    private void vbp$forgetBody(ServerLevel level, PathfinderMob body, CallbackInfo ci) {
        VBP$BODY.remove();
    }

    @WrapOperation(method = "isTemptation", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private boolean vbp$camelHuskFood(Predicate<ItemStack> predicate, Object stack, Operation<Boolean> original) {
        PathfinderMob body = VBP$BODY.get();
        return body instanceof CamelHusk husk ? husk.isFood((ItemStack) stack) : original.call(predicate, stack);
    }
}
