package com.coolfreeze23.vbparity.misc.mixin;

import com.coolfreeze23.vbparity.misc.MiscContent;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native SummonCommand.createEntity: after the position check, a monster in Peaceful fails with its own message. */
@Mixin(SummonCommand.class)
public abstract class SummonCommandPeacefulMixin {
    @Unique
    private static final SimpleCommandExceptionType VBP_ERROR_FAILED_PEACEFUL =
            new SimpleCommandExceptionType(Component.translatable("commands.summon.failed.peaceful"));

    @Inject(method = "createEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;copy()Lnet/minecraft/nbt/CompoundTag;"))
    private static void vbp$peaceful(CommandSourceStack source, Holder.Reference<EntityType<?>> type, Vec3 pos, CompoundTag tag,
                                     boolean randomizeProperties, CallbackInfoReturnable<Entity> cir) throws CommandSyntaxException {
        if (source.getLevel().getDifficulty() == Difficulty.PEACEFUL && MiscContent.notAllowedInPeaceful(type.value())) {
            throw VBP_ERROR_FAILED_PEACEFUL.create();
        }
    }
}
