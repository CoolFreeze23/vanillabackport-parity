package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.coolfreeze23.vbparity.ghast.LeashRules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-34: the current game's LeadItem.bindPlayerMobs. Mobs are searched in a 32-block box centred on the fence,
 * each is tied only if it is within its leash snap distance of the knot, and a new knot that ties nothing is removed.
 */
@Mixin(LeadItem.class)
public abstract class LeadItemMixin {
    @Inject(method = "bindPlayerMobs", at = @At("HEAD"), cancellable = true)
    private static void vbp$bindPlayerMobs(Player player, Level level, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LeashRules.modernLeashes()) {
            return;
        }

        List<Leashable> toLeash = LeashPhysics.leashableInArea(level, Vec3.atCenterOf(pos), leashable -> leashable.getLeashHolder() == player);
        if (toLeash.isEmpty()) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        LeashFenceKnotEntity existing = LeashRules.getKnot(level, pos);
        LeashFenceKnotEntity knot = existing;
        if (knot == null) {
            knot = new LeashFenceKnotEntity(level, pos);
            level.addFreshEntity(knot);
        }

        boolean anyLeashed = false;
        for (Leashable leashable : toLeash) {
            if (LeashPhysics.canAttachLeash(leashable, knot)) {
                leashable.setLeashedTo(knot, true);
                anyLeashed = true;
            }
        }

        if (anyLeashed) {
            knot.playPlacementSound();
            level.gameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Context.of(player));
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            if (existing == null) {
                knot.discard();
            }

            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    /**
     * GHAST-34: the current game's LeadItem.useOn returns the bind result on the server and PASS on the client, so
     * the arm only swings (server-driven) when a mob was actually tied. 1.21.1 always returned sidedSuccess.
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void vbp$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LeashRules.modernLeashes()) {
            return;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.getBlockState(pos).is(BlockTags.FENCES) && !level.isClientSide() && player != null) {
            cir.setReturnValue(LeadItem.bindPlayerMobs(player, level, pos));
        } else {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
