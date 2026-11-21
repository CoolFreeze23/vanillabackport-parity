package com.coolfreeze23.vbparity.misc.mixin.client;

import com.coolfreeze23.vbparity.misc.client.CloudRangeAccess;
import com.coolfreeze23.vbparity.misc.client.NativeCloudRenderer;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native Options.cloudRange: "options.renderCloudsDistance", IntRange(2, 128), default 128, saved as "cloudRange". */
@Mixin(Options.class)
public abstract class OptionsCloudRangeMixin implements CloudRangeAccess {
    @Unique
    private final OptionInstance<Integer> vbp$cloudRange = new OptionInstance<>(
            "options.renderCloudsDistance",
            OptionInstance.noTooltip(),
            (caption, value) -> Options.genericValueLabel(caption, Component.translatable("options.chunks", value)),
            new OptionInstance.IntRange(2, 128, true),
            128,
            value -> NativeCloudRenderer.INSTANCE.markForRebuild());

    @Override
    public OptionInstance<Integer> vbp$cloudRange() {
        return this.vbp$cloudRange;
    }

    @Inject(method = "processOptions", at = @At("TAIL"))
    private void vbp$processCloudRange(Options.FieldAccess accessor, CallbackInfo ci) {
        accessor.process("cloudRange", this.vbp$cloudRange);
    }
}
