package com.coolfreeze23.vbparity.variants.mixin.client;

import com.blackgear.platform.core.util.config.ConfigBuilder;
import com.blackgear.vanillabackport.client.ClientConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * VARIANTS-14: native always draws the coloured wool undercoat, so VanillaBackport's {@code use_sheep_wool_undercoat}
 * defaults to true. Only the default changes: a config file that already says false keeps the undercoat off.
 */
@Mixin(ClientConfig.class)
public abstract class ClientConfigMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/platform/core/util/config/ConfigBuilder;define(Ljava/lang/String;Z)Lcom/blackgear/platform/core/util/config/ConfigBuilder$ConfigValue;")
    )
    private ConfigBuilder.ConfigValue<Boolean> vbp$undercoatOnByDefault(ConfigBuilder builder, String path, boolean defaultValue,
                                                                        Operation<ConfigBuilder.ConfigValue<Boolean>> original) {
        return original.call(builder, path, "use_sheep_wool_undercoat".equals(path) || defaultValue);
    }
}
