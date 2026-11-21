package com.coolfreeze23.vbparity.misc.mixin.client;

import com.coolfreeze23.vbparity.misc.client.CloudRangeAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/** Native Video Settings lists Cloud Distance; it goes right after Clouds (the 1.21.1 screen has no native grouping). */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsCloudRangeMixin {
    @ModifyReturnValue(method = "options", at = @At("RETURN"))
    private static OptionInstance<?>[] vbp$addCloudDistance(OptionInstance<?>[] options, Options gameOptions) {
        if (!CloudRangeAccess.nativeClouds()) {
            return options;
        }
        List<OptionInstance<?>> list = new ArrayList<>(List.of(options));
        int index = list.indexOf(gameOptions.cloudStatus());
        list.add(index < 0 ? list.size() : index + 1, ((CloudRangeAccess) gameOptions).vbp$cloudRange());
        return list.toArray(OptionInstance[]::new);
    }
}
