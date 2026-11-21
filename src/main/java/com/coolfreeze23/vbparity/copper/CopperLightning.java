package com.coolfreeze23.vbparity.copper;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.DataMapHooks;

/** Native (26.2) LightningBolt copper cleaning, adapted to VB where every lightning rod, waxed or not, is WeatheringCopper. */
public final class CopperLightning {
    private CopperLightning() {
    }

    public static boolean isWaxed(Block block) {
        return HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(block) || DataMapHooks.getBlockUnwaxed(block) != null;
    }

    /** True for copper that weathers: VB's waxed rods implement WeatheringCopper but native waxed blocks do not. */
    public static boolean isWeatheringCopper(Block block) {
        return block instanceof WeatheringCopper && !isWaxed(block);
    }

    public static void clearCopperOnLightningStrike(Level level, BlockPos struckPos) {
        BlockState struckState = level.getBlockState(struckPos);
        Block struck = struckState.getBlock();
        boolean isWaxed = isWaxed(struck);
        boolean isWeatheringCopper = isWeatheringCopper(struck);
        if (isWeatheringCopper || isWaxed) {
            if (isWeatheringCopper) {
                level.setBlockAndUpdate(struckPos, WeatheringCopper.getFirst(level.getBlockState(struckPos)));
            }

            BlockPos.MutableBlockPos workPos = struckPos.mutable();
            RandomSource random = level.getRandom();
            int strikesCount = random.nextInt(3) + 3;
            for (int strike = 0; strike < strikesCount; strike++) {
                int stepCount = random.nextInt(8) + 1;
                randomWalkCleaningCopper(level, struckPos, workPos, stepCount);
            }
        }
    }

    private static void randomWalkCleaningCopper(Level level, BlockPos originalStrikePos, BlockPos.MutableBlockPos workPos, int stepCount) {
        workPos.set(originalStrikePos);
        for (int step = 0; step < stepCount; step++) {
            Optional<BlockPos> stepPos = randomStepCleaningCopper(level, workPos);
            if (stepPos.isEmpty()) {
                break;
            }
            workPos.set(stepPos.get());
        }
    }

    private static Optional<BlockPos> randomStepCleaningCopper(Level level, BlockPos pos) {
        for (BlockPos candidate : BlockPos.randomInCube(level.getRandom(), 10, pos, 1)) {
            BlockState state = level.getBlockState(candidate);
            if (isWeatheringCopper(state.getBlock())) {
                WeatheringCopper.getPrevious(state).ifPresent(previous -> level.setBlockAndUpdate(candidate, previous));
                level.levelEvent(3002, candidate, -1);
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
