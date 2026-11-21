package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.blocks.CopperChestBlock;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.google.common.collect.BiMap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.DataMapHooks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperChestBlock.class)
public abstract class CopperChestBlockMixin extends ChestBlock {
    private CopperChestBlockMixin(Properties properties, Supplier<BlockEntityType<? extends ChestBlockEntity>> type) {
        super(properties, type);
    }

    @Shadow
    @Nullable
    private Direction candidatePartnerFacing(Level level, BlockPos pos, Direction direction) {
        throw new AssertionError();
    }

    @Shadow
    private ChestType getChestType(Level level, BlockPos pos, Direction facing) {
        throw new AssertionError();
    }

    @Shadow
    public abstract boolean chestCanConnectTo(BlockState blockState);

    @Shadow
    private static BlockState getLeastOxidizedChestOfConnectedBlocks(BlockState state, Level level, BlockPos pos) {
        throw new AssertionError();
    }

    /** COPPER-05: waxed copper blocks build the matching waxed copper chest (native zips waxed to waxed). */
    @WrapOperation(
        method = "getFromCopperBlock",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private static Object vbparity$waxedChest(Map<Object, Object> map, Object copperBlock, Object fallback, Operation<Object> original) {
        Supplier<Block> waxed = null;
        if (copperBlock == Blocks.WAXED_COPPER_BLOCK) {
            waxed = ModBlocks.WAXED_COPPER_CHEST;
        } else if (copperBlock == Blocks.WAXED_EXPOSED_COPPER) {
            waxed = ModBlocks.WAXED_EXPOSED_COPPER_CHEST;
        } else if (copperBlock == Blocks.WAXED_WEATHERED_COPPER) {
            waxed = ModBlocks.WAXED_WEATHERED_COPPER_CHEST;
        } else if (copperBlock == Blocks.WAXED_OXIDIZED_COPPER) {
            waxed = ModBlocks.WAXED_OXIDIZED_COPPER_CHEST;
        }
        return waxed != null ? waxed : original.call(map, copperBlock, fallback);
    }

    /**
     * COPPER-06: VB registers its waxed chests through Platform's waxable map (served by NeoForge DataMapHooks), not
     * HoneycombItem.WAX_OFF_BY_BLOCK, so unwaxBlock found nothing and a waxed half kept its wax when pairing with an
     * unwaxed one. Native unwaxes both halves first.
     */
    @WrapOperation(
        method = "unwaxBlock",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private static Object vbparity$unwaxThroughDataMaps(BiMap<Object, Object> waxOff, Object waxed, Operation<Object> original) {
        Object unwaxed = original.call(waxOff, waxed);
        return unwaxed != null ? unwaxed : DataMapHooks.getBlockUnwaxed((Block) waxed);
    }

    /**
     * COPPER-06: native ChestBlock.getStateForPlacement using chestCanConnectTo (any copper chest), followed by the
     * least-oxidized conversion. The 1.21.1 super only pairs with exactly the same block.
     */
    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void vbparity$getStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ChestType type = ChestType.SINGLE;
        Direction facingDirection = context.getHorizontalDirection().getOpposite();
        FluidState replacedFluidState = level.getFluidState(pos);
        boolean secondaryUse = context.isSecondaryUseActive();
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isHorizontal() && secondaryUse) {
            Direction neighbourFacing = this.candidatePartnerFacing(level, pos, clickedFace.getOpposite());
            if (neighbourFacing != null && neighbourFacing.getAxis() != clickedFace.getAxis()) {
                facingDirection = neighbourFacing;
                type = neighbourFacing.getCounterClockWise() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        }

        if (type == ChestType.SINGLE && !secondaryUse) {
            type = this.getChestType(level, pos, facingDirection);
        }

        BlockState state = this.defaultBlockState()
            .setValue(FACING, facingDirection)
            .setValue(TYPE, type)
            .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
        cir.setReturnValue(getLeastOxidizedChestOfConnectedBlocks(state, level, pos));
    }

    /**
     * COPPER-06: when a neighbouring copper chest pairs with this one, native turns this half into the partner's block
     * (the least-oxidized one chosen at placement). VB tested the pre-update state and left a mixed double chest.
     */
    @ModifyReturnValue(method = "updateShape", at = @At("RETURN"))
    private BlockState vbparity$matchPartner(
        BlockState result, BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos
    ) {
        if (this.chestCanConnectTo(neighborState)
            && result.getBlock() instanceof CopperChestBlock
            && result.getValue(TYPE) != ChestType.SINGLE
            && getConnectedDirection(result) == direction
            && !result.is(neighborState.getBlock())) {
            return neighborState.getBlock().withPropertiesOf(result);
        }
        return result;
    }
}
