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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The shared linked lifecycle owns placement, rollback, removal and single-item loot. */
public final class TallLightBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 2);
    public enum Fixture implements StringRepresentable {
        FLOOR(false, new VoxelShape[]{
                Shapes.or(Block.box(4, 0, 5, 12, 1, 13), Block.box(7.25, 1, 10.25, 8.75, 16, 11.75)),
                Shapes.or(Block.box(7.25, 0, 10.25, 8.75, 12.5, 11.75), Block.box(4.25, 8, 1, 11.75, 14.5, 10.25))}),
        CHANDELIER(true, new VoxelShape[]{Block.box(3, 0, 3, 13, 16, 13), Block.box(2, 2, 2, 14, 16, 14)}),
        STREET(false, new VoxelShape[]{
                Shapes.or(Block.box(3.75, 0, 3.75, 12.25, 1, 12.25), Block.box(5.5, 1, 5.5, 10.5, 16, 10.5)),
                Block.box(6.5, 0, 6.5, 9.5, 16, 9.5),
                Shapes.or(Block.box(6.5, 0, 6.5, 9.5, 6, 9.5), Block.box(4.25, 5.75, 4.25, 11.75, 16, 11.75))});
        final boolean hanging;
        final VoxelShape[][] shapes;
        Fixture(boolean hanging, VoxelShape[] shapes) {
            this.hanging = hanging;
            this.shapes = new VoxelShape[shapes.length][];
            for (int i = 0; i < shapes.length; i++) this.shapes[i] = RestaurantKitchenBlock.rotations(shapes[i]);
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<TallLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture").forGetter((TallLightBlock b) -> b.fixture),
            propertiesCodec()).apply(instance, TallLightBlock::new));
    //?}
    private final Fixture fixture;

    public TallLightBlock(Fixture fixture, Properties properties) { super(properties); this.fixture = fixture; }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(SEGMENT); }
    @Override protected int partCount() { return fixture.shapes.length; }
    @Override protected int partIndex(BlockState state) { return state.getValue(SEGMENT); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(SEGMENT, index); }
    @Override protected Direction getExtensionDirection(BlockState state) { return fixture.hanging ? Direction.DOWN : Direction.UP; }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        if (context.getClickedFace() != (fixture.hanging ? Direction.DOWN : Direction.UP)) return null;
        return super.getPlacementState(context);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (partIndex(state) >= partCount()) return false;
        Direction support = getExtensionDirection(state).getOpposite();
        BlockPos anchor = pos.relative(support, partIndex(state) + 1);
        return level.getBlockState(anchor).isFaceSturdy(level, anchor, support.getOpposite());
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        Direction extension = getExtensionDirection(state);
        int adjacent = partIndex(state) + (side == extension ? 1 : side == extension.getOpposite() ? -1 : 0);
        if (adjacent != partIndex(state) && adjacent >= 0 && adjacent < partCount()
                && neighbor != stateForPart(state, adjacent)) return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fixture.shapes[Math.min(partIndex(state), partCount() - 1)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
