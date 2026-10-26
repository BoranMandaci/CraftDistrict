package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four in-plane arms; one-hop neighbor checks only, with no tick or plane bridging. */
public class ConnectedLightBlock extends RotatableDecorationBlock {
    public static final BooleanProperty WALL_MOUNTED = SurfaceLightBlock.WALL_MOUNTED;
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right"),
            UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    private static final BooleanProperty[] ARMS = {LEFT, RIGHT, UP, DOWN};
    public enum Kind implements StringRepresentable {
        NEON, STRING;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<ConnectedLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Kind::values).fieldOf("kind").forGetter((ConnectedLightBlock b) -> b.kind),
            propertiesCodec()).apply(instance, ConnectedLightBlock::new));
    //?}
    private final Kind kind;
    private final VoxelShape[][][] shapes = new VoxelShape[2][16][];

    public ConnectedLightBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(WALL_MOUNTED, false)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
        for (int wall = 0; wall < 2; wall++) for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape;
            if (wall == 1) {
                shape = Block.box(7.25, 7.25, 14.875, 8.75, 8.75, 16);
                if (mask == 0) shape = Block.box(4, 7.25, 14.875, 12, 8.75, 16);
                if ((mask & 1) != 0) shape = Shapes.or(shape, Block.box(0, 7.25, 14.875, 7.25, 8.75, 16));
                if ((mask & 2) != 0) shape = Shapes.or(shape, Block.box(8.75, 7.25, 14.875, 16, 8.75, 16));
                if ((mask & 4) != 0) shape = Shapes.or(shape, Block.box(7.25, 8.75, 14.875, 8.75, 16, 16));
                if ((mask & 8) != 0) shape = Shapes.or(shape, Block.box(7.25, 0, 14.875, 8.75, 7.25, 16));
            } else {
                double y = kind == Kind.STRING ? 11 : 14.875;
                shape = Block.box(7.25, y, 7.25, 8.75, 16, 8.75);
                if (mask == 0) shape = Block.box(3, y, 7, 13, 16, 9);
                if ((mask & 1) != 0) shape = Shapes.or(shape, Block.box(0, y, 7.25, 7.25, 16, 8.75));
                if ((mask & 2) != 0) shape = Shapes.or(shape, Block.box(8.75, y, 7.25, 16, 16, 8.75));
                if ((mask & 4) != 0) shape = Shapes.or(shape, Block.box(7.25, y, 0, 8.75, 16, 7.25));
                if ((mask & 8) != 0) shape = Shapes.or(shape, Block.box(7.25, y, 8.75, 8.75, 16, 16));
            }
            shapes[wall][mask] = RestaurantKitchenBlock.rotations(shape);
        }
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WALL_MOUNTED, LEFT, RIGHT, UP, DOWN);
    }

    private static Direction arm(BlockState state, int index) {
        Direction facing = state.getValue(FACING);
        return switch (index) {
            case 0 -> facing.getCounterClockWise();
            case 1 -> facing.getClockWise();
            case 2 -> state.getValue(WALL_MOUNTED) ? Direction.UP : facing;
            default -> state.getValue(WALL_MOUNTED) ? Direction.DOWN : facing.getOpposite();
        };
    }
    private static Direction support(BlockState state) {
        return state.getValue(WALL_MOUNTED) ? state.getValue(FACING).getOpposite() : Direction.UP;
    }
    private boolean connects(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(WALL_MOUNTED).equals(state.getValue(WALL_MOUNTED))
                && (!state.getValue(WALL_MOUNTED) || other.getValue(FACING) == state.getValue(FACING));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        boolean wall = face.getAxis().isHorizontal();
        if (face == Direction.UP || wall && kind == Kind.STRING) return null;
        BlockState state = defaultBlockState().setValue(WALL_MOUNTED, wall)
                .setValue(FACING, wall ? face : context.getHorizontalDirection().getOpposite());
        if (!canSurvive(state, context.getLevel(), context.getClickedPos())) return null;
        for (int i = 0; i < 4; i++) state = state.setValue(ARMS[i], connects(state,
                context.getLevel().getBlockState(context.getClickedPos().relative(arm(state, i)))));
        return state;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction side = support(state);
        BlockPos anchor = pos.relative(side);
        return level.getBlockState(anchor).isFaceSturdy(level, anchor, side.getOpposite());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side == support(state) && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        for (int i = 0; i < 4; i++) if (side == arm(state, i)) return state.setValue(ARMS[i], connects(state, other));
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int i = 0; i < 4; i++) if (state.getValue(ARMS[i])) mask |= 1 << i;
        return shapes[state.getValue(WALL_MOUNTED) ? 1 : 0][mask][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        // Wall-local left/right/up/down are invariant; ceiling arms rotate with facing too.
        return super.rotate(state, rotation);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) return result;
        return result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
