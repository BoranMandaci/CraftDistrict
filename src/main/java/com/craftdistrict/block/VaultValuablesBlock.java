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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small vault props use cached envelopes, with no containers, entities, or ticking. */
public class VaultValuablesBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        BANKNOTES, BAGS, GOLD_CRATE, LOCKBOX;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(1.5, 0, 2, 14.5, 3.5, 14)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2.8, 0, 1, 13.2, 6.5, 15),
                    box(2.45, 3.6, 3.9, 3.2, 7.9, 12.1), box(12.8, 3.6, 3.9, 13.55, 7.9, 12.1)).optimize()),
            RestaurantKitchenBlock.rotations(box(.75, 0, .75, 15.25, 8, 15.25)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 3, 14, 6.5, 13), box(5, 6.5, 6.5, 11, 8, 9.5)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<VaultValuablesBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((VaultValuablesBlock b) -> b.design),
            propertiesCodec()).apply(instance, VaultValuablesBlock::new));
    //?}
    public VaultValuablesBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos below = pos.below();
        return level.hasChunkAt(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos other) {
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
