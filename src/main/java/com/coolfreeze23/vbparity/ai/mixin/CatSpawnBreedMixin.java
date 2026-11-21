package com.coolfreeze23.vbparity.ai.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AI-VAR-05: native Cat.finalizeSpawn selects from the cat_variant registry in id order. Inside a #cats_spawn_as_black
 * structure all_black has priority 1, so the draw is nextInt(1); elsewhere the priority-0 breeds (all_black included at
 * full moon) are drawn in id order. 1.21.1 drew nextInt(10|11) over the tag order even in a hut.
 */
@Mixin(Cat.class)
public abstract class CatSpawnBreedMixin {
    @WrapOperation(method = "finalizeSpawn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getRandomElementOf(Lnet/minecraft/tags/TagKey;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    @SuppressWarnings("unchecked")
    private Optional<Holder<CatVariant>> vbp$nativeBreedPick(Registry<CatVariant> registry, TagKey<CatVariant> tag, RandomSource random,
                                                             Operation<Optional<Holder<CatVariant>>> original,
                                                             @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ServerLevelAccessor level) {
        Cat self = (Cat) (Object) this;
        if (level.getLevel().structureManager().getStructureWithPieceAt(self.blockPosition(), StructureTags.CATS_SPAWN_AS_BLACK).isValid()) {
            random.nextInt(1);
            return Optional.of(BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(CatVariant.ALL_BLACK));
        }
        List<Holder<CatVariant>> breeds = new ArrayList<>();
        registry.getTag(tag).ifPresent(set -> set.forEach(breeds::add));
        if (breeds.isEmpty()) {
            return Optional.empty();
        }
        breeds.sort((a, b) -> a.unwrapKey().orElseThrow().location().compareTo(b.unwrapKey().orElseThrow().location()));
        return Optional.of(breeds.get(random.nextInt(breeds.size())));
    }
}
