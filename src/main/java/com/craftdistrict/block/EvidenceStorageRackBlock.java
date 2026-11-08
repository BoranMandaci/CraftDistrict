package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four-direction shelf joins retain the existing vertically paired rack footprint. */
public final class EvidenceStorageRackBlock extends MedicalSupplyCabinetBlock {
    public static final BooleanProperty FRONT = BooleanProperty.create("front"), BACK = BooleanProperty.create("back");
    public static final IntegerProperty INNER_CORNERS = IntegerProperty.create("inner_corners", 0, 15);
    // Square shelf tiles meet on every edge, including perpendicular and T-shaped joins.
    private static final VoxelShape[] SHAPES = {box(0, 0, 0, 16, 16, 16), box(0, 0, 0, 16, 15.5, 16)};
    //? if >=1.21 {
    public static final MapCodec<EvidenceStorageRackBlock> CODEC = simpleCodec(EvidenceStorageRackBlock::new);
    //?}

    public EvidenceStorageRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FRONT, false).setValue(BACK, false).setValue(INNER_CORNERS, 0));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(FRONT, BACK, INNER_CORNERS);
    }

    @Override protected boolean connectsFacing(Direction facing, Direction otherFacing) {
        return true;
    }

    private boolean complete(LevelReader level, BlockPos origin) {
        if (!level.hasChunkAt(origin) || !level.hasChunkAt(origin.above())) return false;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        return lower.is(this) && lower.getValue(PRIMARY) && upper.is(this) && !upper.getValue(PRIMARY)
                && lower.getValue(FACING) == upper.getValue(FACING);
    }

    @Override public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        BlockState joined = state.setValue(LEFT, complete(level, origin.relative(facing.getCounterClockWise())))
                .setValue(RIGHT, complete(level, origin.relative(facing.getClockWise())))
                .setValue(FRONT, complete(level, origin.relative(facing)))
                .setValue(BACK, complete(level, origin.relative(facing.getOpposite())));
        int corners = 0;
        // Only a four-rack junction loses its top stub; exposed and concave frame corners stay intact.
        for (int corner = 0; corner < 4; corner++) {
            boolean left = (corner & 1) == 0, front = (corner & 2) == 0;
            if (joined.getValue(left ? LEFT : RIGHT) && joined.getValue(front ? FRONT : BACK)
                    && complete(level, origin.relative(left ? facing.getCounterClockWise() : facing.getClockWise())
                    .relative(front ? facing : facing.getOpposite()))) corners |= 1 << corner;
        }
        return joined.setValue(INNER_CORNERS, corners);
    }

    @Override protected void refreshRow(Level level, BlockPos origin, Direction facing) {
        refresh(level, origin, facing);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            refresh(level, origin.relative(side), facing);
            refresh(level, origin.relative(side).relative(side.getClockWise()), facing);
        }
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) return result;
        int corners = state.getValue(INNER_CORNERS);
        return result.setValue(INNER_CORNERS, ((corners & 5) << 1) | ((corners & 10) >> 1));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PRIMARY) ? 0 : 1];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
