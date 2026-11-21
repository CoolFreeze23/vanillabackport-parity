package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.integrations.MobIntegrations;
import java.util.function.Predicate;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * MAYHEM-08: native Zombie.addBehaviourGoals adds SpearUseGoal at priority 2, but Drowned and ZombifiedPiglin override
 * addBehaviourGoals (drowned get none, zombified piglins only their own at priority 1). VB's "instanceof Zombie"
 * registration also reached those two.
 */
@Mixin(value = MobIntegrations.class, remap = false)
public abstract class MobIntegrationsMixin {
    @ModifyArg(
        method = "registerGoals",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/platform/common/integration/MobIntegration$Event;registerGoal(Ljava/util/function/Predicate;ILjava/util/function/Function;)V", ordinal = 0),
        index = 0
    )
    private static Predicate<Mob> vbp$zombieSpearGoal(Predicate<Mob> original) {
        return mob -> original.test(mob) && !(mob instanceof Drowned) && !(mob instanceof ZombifiedPiglin);
    }
}
