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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive two-cell pharmacy fixtures on the shared atomic placement/removal lifecycle. */
public class PharmacyFixtureBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        SIGN, COUNTER, GLASS_DISPLAY, PREPARATION;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<PharmacyFixtureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((PharmacyFixtureBlock block) -> block.design),
            propertiesCodec()).apply(instance, PharmacyFixtureBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(box(.5, 4, 14.5, 16, 12.5, 16)),
                    RestaurantKitchenBlock.rotations(box(0, 4, 14.5, 15.5, 12.5, 16))},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1, 16, 14.25, 15),
                    box(0, 13.25, 0, 16, 15, 16), box(3.5, 14.25, 3, 14.5, 18.5, 15)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1, 16, 14.25, 15),
                            box(0, 13.25, 0, 16, 15, 16), box(2.75, 14.25, 7.875, 13.25, 24, 9.3125)).optimize())},
            {RestaurantKitchenBlock.rotations(box(0, 0, .5, 16, 16, 15.5)),
                    RestaurantKitchenBlock.rotations(box(0, 0, .5, 16, 16, 15.5))},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1, 16, 14.25, 15),
                    box(0, 13.25, 0, 16, 14.25, 16), box(1, 14.25, 12, 16, 20.25, 15),
                    box(1.5, 14.25, 3, 10.5, 19.25, 10.5)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1, 16, 14.25, 15),
                            box(0, 13.25, 0, 16, 14.25, 16), box(6, 14.25, 2, 15, 18.5, 13)).optimize())}};

    public PharmacyFixtureBlock(Design design, Properties properties) { super(properties); this.design = design; }

    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }

    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return design == Design.SIGN
                ? (context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null)
                : super.getPlacementFacing(context);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction side = design == Design.SIGN ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design == Design.SIGN ? pos.relative(side.getOpposite()) : pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, side);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction along = getExtensionDirection(state);
        Direction partner = state.getValue(PRIMARY) ? along : along.getOpposite();
        if (side == partner && neighbor != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        Direction support = design == Design.SIGN ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        return side == support && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(PRIMARY, !state.getValue(PRIMARY));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = super.getBlockSupportShape(state, level, pos);
        return design == Design.GLASS_DISPLAY ? DecorationSupport.withTop(shape) : shape;
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
