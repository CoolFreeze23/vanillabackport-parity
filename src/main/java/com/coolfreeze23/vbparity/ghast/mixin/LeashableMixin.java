package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashDataExtension;
import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashableCallback;
import com.coolfreeze23.vbparity.ghast.GhastSounds;
import com.coolfreeze23.vbparity.ghast.LeashRules;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Leashable.class)
public interface LeashableMixin {
    @Shadow
    private static <E extends Entity & Leashable> void restoreLeashFromSave(E entity, Leashable.LeashData leashData) {
    }

    /**
     * VB's modern leash tick with two current-game details: the leash drops when either end can no longer interact
     * with the level (dead, removed, or a spectator; GHAST-38), and a snapping lead plays item.lead.break at the
     * holder's exact position (GHAST-32). Everything else is VB's physics unchanged.
     */
    @WrapMethod(method = "tickLeash")
    private static <E extends Entity & Leashable> void vbp$tickLeash(E entity, Operation<Void> original) {
        if (!LeashRules.modernLeashes()) {
            original.call(entity);
            return;
        }

        Leashable.LeashData data = entity.getLeashData();
        if (data != null && data.delayedLeashInfo != null) {
            restoreLeashFromSave(entity, data);
        }

        if (data != null && data.leashHolder != null) {
            if (!LeashRules.canInteractWithLevel(entity) || !LeashRules.canInteractWithLevel(data.leashHolder)) {
                entity.dropLeash(true, entity.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS));
            }

            Entity holder = entity.getLeashHolder();
            LeashableCallback callback = (LeashableCallback) entity;
            if (holder != null && holder.level() == entity.level()) {
                double distance = LeashPhysics.distanceBetween(entity, holder);
                callback.vb$whenLeashedTo(holder);
                if (distance > callback.vb$leashSnapDistance()) {
                    entity.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), GhastSounds.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    entity.leashTooFarBehaviour();
                } else if (distance > callback.vb$leashElasticDistance() - holder.getBbWidth() - entity.getBbWidth()
                    && LeashPhysics.checkElasticInteractions(entity, holder, data)) {
                    callback.vb$onElasticLeashPull();
                } else {
                    entity.closeRangeLeashBehaviour(holder);
                }

                LeashDataExtension extension = (LeashDataExtension) (Object) data;
                entity.setYRot((float) (entity.getYRot() - extension.angularMomentum()));
                extension.setAngularMomentum(extension.angularMomentum() * LeashPhysics.angularFriction(entity));
            }
        }
    }

    /** GHAST-33: a fence knot left with nothing tied to it removes itself. */
    @WrapMethod(method = "dropLeash(Lnet/minecraft/world/entity/Entity;ZZ)V")
    private static <E extends Entity & Leashable> void vbp$dropLeash(E entity, boolean broadcastPacket, boolean dropItem, Operation<Void> original) {
        Leashable.LeashData data = entity.getLeashData();
        Entity oldHolder = data != null ? data.leashHolder : null;
        original.call(entity, broadcastPacket, dropItem);
        if (LeashRules.modernLeashes()) {
            LeashRules.notifyLeasheeRemoved(oldHolder, null);
        }
    }

    @WrapMethod(method = "setLeashedTo(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Z)V")
    private static <E extends Entity & Leashable> void vbp$setLeashedTo(E entity, Entity holder, boolean broadcastPacket, Operation<Void> original) {
        Leashable.LeashData data = entity.getLeashData();
        Entity oldHolder = data != null ? data.leashHolder : null;
        original.call(entity, holder, broadcastPacket);
        if (LeashRules.modernLeashes()) {
            LeashRules.notifyLeasheeRemoved(oldHolder, holder);
        }
    }
}
