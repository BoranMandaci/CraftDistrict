package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** All Restaurant seating shares the same interaction and cached-shape path. */
public class RestaurantSeatBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        WOODEN(9), MODERN(9), CUSHIONED(9.5), DINER(9.25), BAR_STOOL(14), BAR_CHAIR(14.5),
        DINER_BOOTH(9.25), MODERN_BOOTH(9.5), BENCH(9);
        public final double seatHeight;
        Design(double units) { seatHeight = units / 16; }
        public boolean connected() { return ordinal() >= DINER_BOOTH.ordinal(); }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    protected final Design design;
    //? if >=1.21 {
    public static final MapCodec<RestaurantSeatBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((RestaurantSeatBlock b) -> b.design),
            propertiesCodec()).apply(instance, RestaurantSeatBlock::new));
    //?}

    public RestaurantSeatBlock(Design design, Properties properties) { super(properties); this.design = design; }

    public double seatHeight() { return design.seatHeight; }
    public double forwardOffset() { return design == Design.BENCH || design == Design.BAR_STOOL ? 0 : 1.0 / 16; }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    //? if >=1.21 {
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Seating.sit(state, level, pos, player);
    }
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? Seating.sit(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING)) Seating.removeAt(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RestaurantSeatShapes.get(design, state);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
