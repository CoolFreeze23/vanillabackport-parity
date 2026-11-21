package com.coolfreeze23.vbparity.copper.mixin;

import com.coolfreeze23.vbparity.copper.CopperChestEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** TBLK-103: waxing one half of a double chest also signals and shows the 3003 particles at the other half (after it). */
@Mixin(HoneycombItem.class)
public abstract class HoneycombItemMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional vbparity$copper$partnerHalf(Optional waxed, Function action, Operation<Optional> original, @Local(argsOnly = true) UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);
        Function wrapped = state -> {
            Object result = action.apply(state);
            CopperChestEvents.partnerChange(level, context.getPlayer(), pos, oldState, 3003);
            return result;
        };
        return original.call(waxed, wrapped);
    }
}
