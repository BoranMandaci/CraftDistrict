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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Decorative service equipment; there are no inventories, appliance timers, or ticking entities. */
public final class HotelServiceBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        HOUSEKEEPING_CART, LINEN_TROLLEY, WASHING_MACHINE, DRYER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[] OPEN_SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(0, 0, 0, 16, 1.25, 16), box(0, 1.25, 1, 1, 15.5, 16), box(15, 1.25, 1, 16, 15.5, 16),
            box(1, 1.25, 14, 15, 15.5, 16), box(0, 12.75, 0, 16, 16, 16),
            box(1.875, 2.5, -9.25, 3.75, 12, 1)).optimize());
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(1, 0, 1, 15, 16, 15.5)),
            RestaurantKitchenBlock.rotations(box(1, 0, 1, 15, 15.5, 15)),
            RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 16, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelServiceBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelServiceBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelServiceBlock::new));
    //?}
    public HotelServiceBlock(Design design, Properties properties) {
        super(properties); this.design = design; registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.OPEN);
    }
    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        return design.ordinal() < 2 ? InteractionResult.PASS : CabinetInteraction.toggle(state, level, pos, player,
                SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return toggle(state, level, pos, player); }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        return DecorationSupport.isPlaced(state, level, pos) || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return design.ordinal() >= 2 && state.getValue(BlockStateProperties.OPEN) ? OPEN_SHAPES[turn] : SHAPES[Math.min(design.ordinal(), 2)][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
