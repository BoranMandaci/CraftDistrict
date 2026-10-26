package com.craftdistrict.client;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.assembly.AssemblyCatalog;
import com.craftdistrict.block.AssemblyStationBlock;
import com.craftdistrict.block.DiningTableBlock;
import com.craftdistrict.block.FireDispatchDeskBlock;
import com.craftdistrict.block.HoldingCellBedBlock;
import com.craftdistrict.block.ModularDiningTableBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import com.craftdistrict.block.OfficeChairBlock;
import com.craftdistrict.block.PoliceHoldingCellDoorBlock;
import com.craftdistrict.block.RestaurantSeatBlock;
import com.craftdistrict.block.RestaurantServingCartBlock;
import com.craftdistrict.block.SupermarketSelfCheckoutBlock;
import com.craftdistrict.registry.CraftDistrictBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Event-only debris sampling; no particle ticking, world scans, or server changes. */
public final class DecorationBreakParticles {
    private static final int MAX_TARGETED_FRAGMENTS = 12;
    private Level lastLevel;
    private Block lastBlock;
    private long lastOrigin;
    private long lastPosition;
    private long lastTick;

    public static boolean handles(BlockState state) {
        return CraftDistrict.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())
                && (state.getBlock() instanceof AssemblyStationBlock
                    || AssemblyCatalog.isDecoration(state.getBlock().asItem()));
    }

    public static boolean strongReduction(BlockState state) {
        Block block = state.getBlock();
        return block instanceof DiningTableBlock || block instanceof ModularDiningTableBlock
                || (block instanceof RestaurantSeatBlock && !(block instanceof OfficeChairBlock))
                || block instanceof RestaurantServingCartBlock
                || block instanceof AssemblyStationBlock || block instanceof HoldingCellBedBlock
                || block instanceof FireDispatchDeskBlock || block instanceof SupermarketSelfCheckoutBlock
                || state.is(CraftDistrictBlocks.Police.BLUE_CHAIR.get())
                || state.is(CraftDistrictBlocks.Restaurant.STAINLESS_STEEL_PREP_TABLE.get())
                || state.is(CraftDistrictBlocks.Restaurant.KITCHEN_SINK_STATION.get())
                || state.is(CraftDistrictBlocks.Restaurant.FOOD_PICKUP_COUNTER.get());
    }

    /** Partner-removal effects share an origin; repeated breaks at the same position remain valid. */
    public boolean duplicate(Level level, long tick, BlockState state, BlockPos pos) {
        long origin = origin(state, pos).asLong();
        long age = tick - lastTick;
        if (lastLevel == level && lastBlock == state.getBlock() && lastOrigin == origin
                && lastPosition != pos.asLong() && age >= 0 && age <= 2
                // Partner packets may cross a tick boundary. Suppress them only
                // after the broken part is gone, preserving newly placed replacements.
                && !level.getBlockState(BlockPos.of(lastPosition)).is(lastBlock)) return true;
        lastLevel = level;
        lastBlock = state.getBlock();
        lastOrigin = origin;
        lastPosition = pos.asLong();
        lastTick = tick;
        return false;
    }

    private static BlockPos origin(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof MultiBlockDecorationBlock block) return block.structureOrigin(state, pos);
        if (state.getBlock() instanceof HoldingCellBedBlock) {
            if (state.getValue(HoldingCellBedBlock.LEGACY_LAYOUT)) return state.getValue(HoldingCellBedBlock.PRIMARY)
                    ? pos : pos.relative(state.getValue(BedBlock.FACING).getCounterClockWise());
            return state.getValue(BedBlock.PART) == BedPart.HEAD
                    ? pos.relative(state.getValue(BedBlock.FACING).getOpposite()) : pos;
        }
        if (state.getBlock() instanceof PoliceHoldingCellDoorBlock
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return pos.below();
        return pos;
    }

    @FunctionalInterface
    public interface FragmentConsumer {
        void accept(double x, double y, double z, double vx, double vy, double vz);
    }

    public static void sample(VoxelShape shape, FragmentConsumer output) {
        shape.forAllBoxes(new Grid(output));
    }

    public static void sample(BlockState state, VoxelShape shape, FragmentConsumer output) {
        if (!strongReduction(state)) {
            sample(shape, output);
            return;
        }
        Grid grid = new Grid(output);
        grid.countOnly = true;
        shape.forAllBoxes(grid);
        if (grid.candidates == 0) return;
        // Assembly Station previously used the full vanilla burst. Other targets
        // already used the one-in-three sampler; reduce that count by about 80%.
        int previous = state.getBlock() instanceof AssemblyStationBlock ? grid.vanillaCandidates : grid.candidates;
        grid.limit = Math.min(grid.candidates, Math.min(MAX_TARGETED_FRAGMENTS, Math.max(3, (previous + 4) / 5)));
        grid.accumulator = grid.candidates / 2;
        grid.countOnly = false;
        grid.rowPhase = 0;
        shape.forAllBoxes(grid);
    }

    private static final class Grid implements Shapes.DoubleLineConsumer {
        private final FragmentConsumer output;
        private int rowPhase;
        private boolean countOnly;
        private int candidates;
        private int vanillaCandidates;
        private int limit = Integer.MAX_VALUE;
        private int accumulator;

        private Grid(FragmentConsumer output) { this.output = output; }

        @Override public void consume(double x, double y, double z, double X, double Y, double Z) {
            double width = Math.min(1, X - x), height = Math.min(1, Y - y), depth = Math.min(1, Z - z);
            int nx = Math.max(2, Mth.ceil(width / .25));
            int ny = Math.max(2, Mth.ceil(height / .25));
            int nz = Math.max(2, Mth.ceil(depth / .25));
            if (countOnly) {
                int rows = nx * ny;
                vanillaCandidates += rows * nz;
                candidates += rows / 3 * nz;
                for (int row = 0; row < rows % 3; row++) {
                    if (rowPhase < nz) candidates += 1 + (nz - 1 - rowPhase) / 3;
                    rowPhase = (rowPhase + 1) % 3;
                }
                return;
            }
            for (int i = 0; i < nx; i++) {
                double fx = (i + .5) / nx;
                for (int j = 0; j < ny; j++) {
                    double fy = (j + .5) / ny;
                    // Rotate the retained Z sample between rows instead of keeping one flat layer.
                    for (int k = rowPhase; k < nz; k += 3) {
                        if (limit < candidates) {
                            accumulator += limit;
                            if (accumulator < candidates) continue;
                            accumulator -= candidates;
                        }
                        double fz = (k + .5) / nz;
                        output.accept(x + fx * width, y + fy * height, z + fz * depth,
                                fx - .5, fy - .5, fz - .5);
                    }
                    rowPhase = (rowPhase + 1) % 3;
                }
            }
        }
    }
}
