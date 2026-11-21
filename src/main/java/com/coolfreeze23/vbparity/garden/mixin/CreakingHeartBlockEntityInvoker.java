package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.CreakingHeartBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the GameTests call the heart's resin spread directly. */
@Mixin(CreakingHeartBlockEntity.class)
public interface CreakingHeartBlockEntityInvoker {
    @Invoker("spreadResin")
    Optional<BlockPos> vbparity$garden$spreadResin(ServerLevel level);
}
