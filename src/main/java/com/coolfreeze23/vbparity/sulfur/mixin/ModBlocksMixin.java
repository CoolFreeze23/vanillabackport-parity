package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundTypes;
import com.blackgear.vanillabackport.common.level.blocks.properties.SharedBlockProperties;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.coolfreeze23.vbparity.sulfur.PropertiesCopy;
import com.coolfreeze23.vbparity.sulfur.SharedPropertiesGuard;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * VB passes shared, mutable {@link BlockBehaviour.Properties} to Platform's BlockRegistry, which builds the block later from
 * the same instance. Every later {@code .mapColor()/.sound()} on the shared builder then leaks into blocks registered
 * earlier (all shelves end up light gray, all sulfur blocks get potent sulfur sounds). Each registration in ModBlocks now
 * gets its own snapshot of the builder as it was at that call, and potent sulfur builds on a copy instead of mutating the
 * shared sulfur builder (native: ofFullCopy(SULFUR).sound(POTENT_SULFUR).mapColor(GOLD)).
 */
@Mixin(ModBlocks.class)
public abstract class ModBlocksMixin {
    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/platform/core/helper/BlockRegistry;register(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Ljava/util/function/Supplier;"
        ),
        index = 2
    )
    private static BlockBehaviour.Properties vbp$snapshotFactory(BlockBehaviour.Properties properties) {
        SharedPropertiesGuard.fill();
        return PropertiesCopy.copyOf(properties);
    }

    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/platform/core/helper/BlockRegistry;register(Ljava/lang/String;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Ljava/util/function/Supplier;"
        ),
        index = 1
    )
    private static BlockBehaviour.Properties vbp$snapshotPlain(BlockBehaviour.Properties properties) {
        SharedPropertiesGuard.fill();
        return PropertiesCopy.copyOf(properties);
    }

    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/platform/core/helper/BlockRegistry;register(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;Ljava/util/function/BiFunction;Lnet/minecraft/world/item/Item$Properties;)Ljava/util/function/Supplier;"
        ),
        index = 2
    )
    private static BlockBehaviour.Properties vbp$snapshotWithItem(BlockBehaviour.Properties properties) {
        SharedPropertiesGuard.fill();
        return PropertiesCopy.copyOf(properties);
    }

    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blackgear/platform/core/helper/BlockRegistry;registerNoItem(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Ljava/util/function/Supplier;"
        ),
        index = 2
    )
    private static BlockBehaviour.Properties vbp$snapshotNoItem(BlockBehaviour.Properties properties) {
        SharedPropertiesGuard.fill();
        return PropertiesCopy.copyOf(properties);
    }

    @WrapOperation(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;sound(Lnet/minecraft/world/level/block/SoundType;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;"
        )
    )
    private static BlockBehaviour.Properties vbp$potentSulfurOwnProperties(
        BlockBehaviour.Properties properties, SoundType sound, Operation<BlockBehaviour.Properties> original
    ) {
        if (properties == SharedBlockProperties.SULFUR && sound == ModSoundTypes.POTENT_SULFUR) {
            return original.call(PropertiesCopy.copyOf(properties), sound).mapColor(MapColor.GOLD);
        }
        return original.call(properties, sound);
    }
}
