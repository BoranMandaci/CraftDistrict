package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Wall cabinets and window blinds share wall placement; panes recess the blind mounts. */
public final class OfficeWallUnitBlock extends BakerySignageBlock {
    public enum Kind implements StringRepresentable {
        CABINET, BLINDS;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty INSET = BooleanProperty.create("inset");
    public static final BooleanProperty PARTITION_BACK = BooleanProperty.create("partition_back");
    public static final BooleanProperty UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(0, 3, 7.5, 16, 16, 16)),
            RestaurantKitchenBlock.rotations(box(0, 0, 14.25, 16, 16, 16)),
            RestaurantKitchenBlock.rotations(box(0, 0, 21.25, 16, 16, 23)),
            RestaurantKitchenBlock.rotations(box(0, 0, 28.75, 16, 16, 30.5))};
    private final Kind kind;
    //? if >=1.21 {
    public static final MapCodec<OfficeWallUnitBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Kind::values).fieldOf("kind").forGetter((OfficeWallUnitBlock b) -> b.kind),
            propertiesCodec()).apply(instance, OfficeWallUnitBlock::new));
    //?}
    public OfficeWallUnitBlock(Kind kind, Properties properties) {
        super(Variant.WALL_SHOP, properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(INSET, false).setValue(PARTITION_BACK, false)
                .setValue(UP, false).setValue(DOWN, false).setValue(OPEN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(INSET, PARTITION_BACK, UP, DOWN, OPEN);
    }
    private static boolean pane(BlockState state) {
        return state.is(Blocks.GLASS_PANE) || state.getBlock() instanceof StainedGlassPaneBlock;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos) || hasMountSupport(state, level, pos);
    }
    private boolean hasMountSupport(BlockState state, LevelReader level, BlockPos pos) {
        BlockState support = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
        if (kind == Kind.BLINDS && support.getBlock() instanceof OfficeGlassPartitionBlock) {
            return support.getValue(FACING).getAxis() == state.getValue(FACING).getAxis();
        }
        BlockPos supportPos = pos.relative(state.getValue(FACING).getOpposite());
        return kind == Kind.BLINDS && pane(support)
                || support.isFaceSturdy(level, supportPos, state.getValue(FACING));
    }
    private boolean inset(BlockState state, LevelReader level, BlockPos pos) {
        if (kind != Kind.BLINDS) return false;
        Direction facing = state.getValue(FACING);
        BlockState support = level.getBlockState(pos.relative(facing.getOpposite()));
        BooleanProperty towardsBlind = switch (facing) {
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case EAST -> BlockStateProperties.EAST;
            default -> BlockStateProperties.WEST;
        };
        // A pane extending towards the mounting face already reaches the block edge.
        return pane(support) && !support.getValue(towardsBlind);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    private boolean matches(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(PARTITION_BACK) == state.getValue(PARTITION_BACK)
                && (state.getValue(PARTITION_BACK) || other.getValue(INSET) == state.getValue(INSET));
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        // A removed wall/pane must not move an already installed blind to a different plane.
        if (hasMountSupport(state, level, pos)) {
            state = state.setValue(INSET, inset(state, level, pos));
            BlockState support = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
            state = state.setValue(PARTITION_BACK, kind == Kind.BLINDS
                    && support.getBlock() instanceof OfficeGlassPartitionBlock
                    && support.getValue(FACING) == state.getValue(FACING));
        }
        // Full-width slats already join horizontally; only stacked ends need rail changes.
        return state.setValue(UP, kind == Kind.BLINDS && matches(state, level.getBlockState(pos.above())))
                .setValue(DOWN, kind == Kind.BLINDS && matches(state, level.getBlockState(pos.below())));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        BlockState result = super.updateShape(state, side, neighbor, level, pos, otherPos);
        return result.isAir() ? result : layout(result, level, pos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = kind == Kind.CABINET ? 0 : state.getValue(PARTITION_BACK) ? 3 : state.getValue(INSET) ? 2 : 1;
        return SHAPES[index][RestaurantKitchenBlock.turn(state)];
    }

    /** Account for the real outline of a recessed mount, without changing player reach. */
    public static double interactionHitLimit(BlockGetter level, BlockHitResult hit, double vanillaLimit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof OfficeWallUnitBlock unit) || unit.kind != Kind.BLINDS
                || !state.getValue(PARTITION_BACK)) return vanillaLimit;
        AABB bounds = state.getShape(level, pos).bounds();
        Vec3 point = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double epsilon = 1.0E-7;
        if (!bounds.inflate(epsilon).contains(point)) return vanillaLimit;
        Direction face = hit.getDirection();
        Direction.Axis axis = face.getAxis();
        double edge = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? bounds.max(axis) : bounds.min(axis);
        if (Math.abs(point.get(axis) - edge) > epsilon) return vanillaLimit;
        double limit = vanillaLimit;
        for (Direction.Axis coordinate : Direction.Axis.values()) {
            limit = Math.max(limit, Math.max(Math.abs(bounds.min(coordinate) - .5),
                    Math.abs(bounds.max(coordinate) - .5)) + epsilon);
        }
        return limit;
    }
    InteractionResult toggleBlinds(BlockState state, Level level, BlockPos pos, Player player) {
        if (kind != Kind.BLINDS) return InteractionResult.PASS;
        return CabinetInteraction.toggle(state, level, pos, player,
                SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_TRAPDOOR_CLOSE);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggleBlinds(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggleBlinds(state, level, pos, player);
    }
    *///?}
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
