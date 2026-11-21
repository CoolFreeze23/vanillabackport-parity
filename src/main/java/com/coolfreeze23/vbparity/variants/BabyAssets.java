package com.coolfreeze23.vbparity.variants;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.ClientAsset;
import com.blackgear.vanillabackport.common.api.modules.mob_variant.ModelAndTexture;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * VARIANTS-10: native cow, pig and chicken variants carry a {@code baby_asset_id} next to {@code asset_id}
 * ({@code CowVariant.DIRECT_CODEC} etc.). VanillaBackport's {@code ModelAndTexture} codec has no such field, so a
 * datapack variant could not give its calves, piglets or chicks their own texture. The field is added to the
 * {@code ModelAndTexture} map codec that every VanillaBackport variant embeds. It stays optional (native requires it)
 * so that existing VanillaBackport-format datapacks keep loading; without it a baby falls back to the native baby
 * texture of the variant id, or to VanillaBackport's own rendering.
 */
public final class BabyAssets {
    public static final String FIELD = "baby_asset_id";

    private BabyAssets() {
    }

    /** Added to {@code ModelAndTexture} by {@code ModelAndTextureMixin}. */
    public interface Holder {
        @Nullable
        ClientAsset vbp$getBabyAsset();

        void vbp$setBabyAsset(@Nullable ClientAsset asset);
    }

    @Nullable
    public static ClientAsset get(@Nullable ModelAndTexture<?> modelAndTexture) {
        return modelAndTexture instanceof Holder holder ? holder.vbp$getBabyAsset() : null;
    }

    public static <T> MapCodec<ModelAndTexture<T>> withBabyAsset(MapCodec<ModelAndTexture<T>> base) {
        return RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    base.forGetter(Function.identity()),
                    ClientAsset.CODEC.optionalFieldOf(FIELD).forGetter(value -> Optional.ofNullable(get(value)))
                )
                .apply(instance, (value, baby) -> {
                    baby.ifPresent(asset -> ((Holder) value).vbp$setBabyAsset(asset));
                    return value;
                })
        );
    }
}
