package com.coolfreeze23.vbparity.misc;

import com.blackgear.vanillabackport.core.VanillaBackport;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Port of the native {@code minecraft:crafting_transmute} recipe (TransmuteRecipe): one input item plus 1-8 material
 * items; the result keeps the input's components. Used by the 16 native bundle dyeing recipes, which show in the
 * recipe book (VB's special bundle-colouring recipe never did). The JSON fields are the native ones; ingredients use
 * the 1.21.1 ingredient format.
 */
public final class TransmuteRecipe implements CraftingRecipe {
    private static final MinMaxBounds.Ints DEFAULT_MATERIAL_COUNT = MinMaxBounds.Ints.exactly(1);
    private static final Codec<MinMaxBounds.Ints> MATERIAL_COUNT_CODEC = MinMaxBounds.Ints.CODEC.validate(bounds -> {
        int min = bounds.min().orElse(1);
        int max = bounds.max().orElse(8);
        return min >= 1 && max <= 8 && min <= max
                ? DataResult.success(bounds)
                : DataResult.error(() -> "material_count must be within 1..8");
    });

    public static final MapCodec<TransmuteRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.category),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(r -> r.input),
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(r -> r.material),
            MATERIAL_COUNT_CODEC.optionalFieldOf("material_count", DEFAULT_MATERIAL_COUNT).forGetter(r -> r.materialCount),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
            Codec.BOOL.optionalFieldOf("add_material_count_to_result", false).forGetter(r -> r.addMaterialCountToResult)
    ).apply(i, TransmuteRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                CraftingBookCategory.STREAM_CODEC.encode(buf, r.category);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.material);
                buf.writeVarInt(r.minMaterialCount());
                buf.writeVarInt(r.maxMaterialCount());
                ItemStack.STREAM_CODEC.encode(buf, r.result);
                buf.writeBoolean(r.addMaterialCountToResult);
            },
            buf -> new TransmuteRecipe(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    CraftingBookCategory.STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    MinMaxBounds.Ints.between(buf.readVarInt(), buf.readVarInt()),
                    ItemStack.STREAM_CODEC.decode(buf),
                    buf.readBoolean()));

    public static final RecipeSerializer<TransmuteRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<TransmuteRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient input;
    private final Ingredient material;
    private final MinMaxBounds.Ints materialCount;
    private final ItemStack result;
    private final boolean addMaterialCountToResult;

    public TransmuteRecipe(String group, CraftingBookCategory category, Ingredient input, Ingredient material,
                           MinMaxBounds.Ints materialCount, ItemStack result, boolean addMaterialCountToResult) {
        this.group = group;
        this.category = category;
        this.input = input;
        this.material = material;
        this.materialCount = materialCount;
        this.result = result;
        this.addMaterialCountToResult = addMaterialCountToResult;
    }

    private int minMaterialCount() {
        return this.materialCount.min().orElse(1);
    }

    private int maxMaterialCount() {
        return this.materialCount.max().orElse(8);
    }

    private int computeResultSize(int materials) {
        return this.addMaterialCountToResult ? materials + this.result.getCount() : this.result.getCount();
    }

    /** Native {@code createWithOriginalComponents}: the input's component patch, then the result's own components. */
    private ItemStack computeResult(ItemStack input, int extraCount) {
        ItemStack out = new ItemStack(this.result.getItemHolder(), this.result.getCount() + extraCount, input.getComponentsPatch());
        out.applyComponents(this.result.getComponentsPatch());
        return out;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int min = this.minMaterialCount();
        int max = this.maxMaterialCount();
        if (input.ingredientCount() < min + 1 || input.ingredientCount() > max + 1) {
            return false;
        }
        ItemStack foundInput = null;
        int materials = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
                if (this.input.test(stack)) {
                    if (foundInput != null) {
                        return false;
                    }
                    foundInput = stack;
                } else {
                    if (!this.material.test(stack)) {
                        return false;
                    }
                    if (++materials > max) {
                        return false;
                    }
                }
            }
        }
        if (foundInput == null || foundInput.isEmpty() || !this.materialCount.matches(materials)) {
            return false;
        }
        if (this.computeResultSize(materials) != 1) {
            return true;
        }
        ItemStack out = this.computeResult(foundInput, 0);
        return !out.isEmpty() && !ItemStack.isSameItemSameComponents(foundInput, out);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (this.addMaterialCountToResult) {
            int materials = 0;
            ItemStack found = ItemStack.EMPTY;
            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack stack = input.getItem(slot);
                if (!stack.isEmpty()) {
                    if (this.input.test(stack)) {
                        found = stack;
                    } else if (this.material.test(stack)) {
                        materials++;
                    }
                }
            }
            return this.computeResult(found, materials);
        }
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && this.input.test(stack)) {
                return this.computeResult(stack, 0);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.minMaterialCount() + 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result.copyWithCount(this.computeResultSize(this.minMaterialCount()));
    }

    /** Recipe-book ghost: the input, then the smallest material count (native has one display per count). */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.input);
        for (int i = 0; i < this.minMaterialCount(); i++) {
            list.add(this.material);
        }
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    /** {@code vbparity:dyeable_bundles}: true while VB's {@code has_dyeable_bundle_recipe} option is on. */
    public record DyeableBundlesCondition() implements ICondition {
        public static final MapCodec<DyeableBundlesCondition> CODEC = MapCodec.unit(new DyeableBundlesCondition());

        @Override
        public boolean test(IContext context) {
            return VanillaBackport.COMMON_CONFIG.hasDyeableBundleRecipe.get();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
