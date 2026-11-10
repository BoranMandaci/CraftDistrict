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

/** One item places either two wall-mounted sections or two supported media-furniture sections. */
public final class HotelMediaBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        WALL_TV, STANDING_TV, CONSOLE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(box(1.5, .5, 13.125, 16, 15.5, 16)),
                    RestaurantKitchenBlock.rotations(box(0, .5, 13.125, 14.5, 15.5, 16))},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(3, 2.25, 8.625, 16, 16, 10.5), box(11, 0, 6.75, 16, 2.25, 12.75)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(0, 2.25, 8.625, 13, 16, 10.5), box(0, 0, 6.75, 5, 2.25, 12.75)).optimize())}};
    //? if >=1.21 {
    public static final MapCodec<HotelMediaBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelMediaBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelMediaBlock::new));
    //?}
    public HotelMediaBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return design == Design.WALL_TV ? context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null
                : super.getPlacementFacing(context);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design == Design.WALL_TV ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design == Design.WALL_TV ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? getExtensionDirection(state) : getExtensionDirection(state).getOpposite();
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(PRIMARY, !state.getValue(PRIMARY));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design == Design.CONSOLE ? Shapes.block() : SHAPES[design.ordinal()][state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
