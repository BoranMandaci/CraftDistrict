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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static pool fixtures reuse the checked placement/removal transaction, without ticking. */
public class HotelPoolFixtureBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public enum Design implements StringRepresentable {
        WATERFALL(2, Shapes.or(box(2, 0, 10, 14, 1.5, 16), box(2.5, 1.5, 12, 13.5, 10, 14),
                box(2.5, 10, 9.5, 13.5, 15.5, 13), box(2.5, 14.75, 6.75, 13.5, 18.5, 11.5),
                box(2.5, 17, 1, 13.5, 20, 8), box(2.25, 15.5, -.75, 13.75, 18.5, 3)).optimize()),
        SHOWER(2, Shapes.or(box(4.5, 0, 8, 11.5, .75, 15), box(6.625, .75, 10.625, 9.375, 31.9375, 13.375),
                box(7.25, 31.25, 4.5, 8.75, 31.875, 12), box(3.75, 30.75, .75, 12.25, 31.75, 8.25),
                box(6.25, 14, 8.75, 9.75, 21, 10.75)).optimize()),
        UMBRELLA(3, Shapes.or(box(3.5, 0, 3.5, 12.5, 1.25, 12.5), box(7.375, 1.25, 7.375, 8.625, 41.5, 8.625),
                box(-10.125, 32.875, -10.125, 26.125, 35, 26.125), box(-6, 35, -6, 22, 37, 22),
                box(-2, 37, -2, 18, 39, 18), box(3, 39, 3, 13, 41, 13),
                box(7, 41, 7, 9, 42.25, 9)).optimize());

        private final int parts;
        private final VoxelShape[][] shapes;
        Design(int parts, VoxelShape whole) {
            this.parts = parts;
            shapes = new VoxelShape[parts][];
            for (int part = 0; part < parts; part++) shapes[part] = RestaurantKitchenBlock.rotations(
                    Shapes.join(whole.move(0, -part, 0), box(-16, 0, -16, 32, 16, 32), BooleanOp.AND).optimize());
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HotelPoolFixtureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelPoolFixtureBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelPoolFixtureBlock::new));
    //?}
    public HotelPoolFixtureBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override protected int partCount(BlockState state) { return design.parts; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) >= partCount(state)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        int partner = state.getValue(PART) + (side == Direction.UP ? 1 : side == Direction.DOWN ? -1 : 0);
        if (partner != state.getValue(PART) && partner >= 0 && partner < partCount(state)
                && neighbor != stateForPart(state, partner)) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        return part < design.parts ? design.shapes[part][RestaurantKitchenBlock.turn(state)] : Shapes.empty();
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
