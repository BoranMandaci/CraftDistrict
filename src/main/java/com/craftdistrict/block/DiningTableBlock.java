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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Independent, rotationally symmetric tables; no connection or seating behavior. */
public final class DiningTableBlock extends Block {
    public enum Design implements StringRepresentable {
        SQUARE, CAFE, BAR;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<DiningTableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design")
                    .forGetter((DiningTableBlock block) -> block.design),
            propertiesCodec()).apply(instance, DiningTableBlock::new));
    //?}

    private static final VoxelShape SQUARE = Shapes.or(
            box(1, 13.25, 1, 15, 14.5, 15),
            box(2, 0, 2, 4, 13.25, 4), box(12, 0, 2, 14, 13.25, 4),
            box(2, 0, 12, 4, 13.25, 14), box(12, 0, 12, 14, 13.25, 14)).optimize();
    private static final VoxelShape CAFE = Shapes.or(
            box(5, 13.25, 1, 11, 14.5, 15), box(3, 13.25, 2, 13, 14.5, 14),
            box(1.5, 13.25, 4, 14.5, 14.5, 12),
            box(6.5, 1.25, 6.5, 9.5, 13.25, 9.5),
            box(3, 0, 6, 13, 1.25, 10), box(6, 0, 3, 10, 1.25, 13)).optimize();
    private static final VoxelShape BAR = Shapes.or(
            box(1.5, 17.5, 1.5, 14.5, 19, 14.5),
            box(6.5, 1, 6.5, 9.5, 17.5, 9.5),
            box(2, 0, 6, 14, 1, 10), box(6, 0, 2, 10, 1, 14),
            box(4, 4, 4, 12, 5, 12)).optimize();
    private final Design design;

    public DiningTableBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (design) { case SQUARE -> SQUARE; case CAFE -> CAFE; case BAR -> BAR; };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        if (design != Design.BAR) return true;
        // The bar top extends three units above its cell; leave that space clear.
        VoxelShape above = level.getBlockState(pos.above()).getCollisionShape(level, pos.above());
        return above.isEmpty() || above.min(Direction.Axis.Y) >= 3.0 / 16.0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacent,
                                  LevelAccessor level, BlockPos pos, BlockPos adjacentPos) {
        return (direction == Direction.DOWN || design == Design.BAR && direction == Direction.UP)
                && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}
}
