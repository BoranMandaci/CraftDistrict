package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static lobby equipment using the existing two-part placement/removal transaction. */
public final class HotelTallBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        LUGGAGE_CART, BROCHURE_STAND, LUGGAGE_CART_LOADED;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Shapes.or(box(1, 0, 1, 15, 4.5, 15),
                    box(2.25, 4.5, 2.25, 3.5, 16, 13.75), box(12.5, 4.5, 2.25, 13.75, 16, 13.75),
                    box(2.5, 15.25, 12.25, 13.5, 16, 14.625)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(2.25, 0, 2.25, 3.5, 9, 13.75),
                            box(12.5, 0, 2.25, 13.75, 9, 13.75), box(2.25, 9, 2.25, 13.75, 12.5, 13.75),
                            box(2.5, 0, 12.25, 13.5, 1.8125, 14.625)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 3.75, 14, 1, 13),
                    box(7.25, 1, 10.625, 8.75, 10, 11.75), box(2.25, 9, 6.25, 13.75, 16, 11.75)).optimize()),
                    RestaurantKitchenBlock.rotations(box(2.25, 0, 6.25, 13.75, 12, 11.75))}};
    private static final VoxelShape[][] LOADED_SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(SHAPES[0][0][0], box(3.75, 4.25, 3.75, 12.25, 16, 12.25)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(SHAPES[0][1][0], box(3.75, 0, 4, 8, 1.5, 9.5)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<HotelTallBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelTallBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelTallBlock::new));
    //?}
    public HotelTallBlock(Design design, Properties properties) { super(properties); this.design = design; }
    /** Saved narrow carts keep their original collision without allocating another set of shapes. */
    static VoxelShape legacyCartShape(boolean loaded, boolean lower, int turn) {
        return (loaded ? LOADED_SHAPES : SHAPES[0])[lower ? 0 : 1][turn];
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PRIMARY) ? 0 : 1;
        return (design == Design.LUGGAGE_CART_LOADED ? LOADED_SHAPES : SHAPES[design.ordinal()])
                [part][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
