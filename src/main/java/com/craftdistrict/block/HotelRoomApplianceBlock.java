package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Decorative minibar and safe doors reuse the existing Wrench-compatible cabinet action. */
public final class HotelRoomApplianceBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        MINI_FRIDGE, SAFE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(box(1, 0, .5, 15, 15.5, 15)),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(1, 0, 1.5, 15, 15.5, 15), box(-.25, .5, -11.5, 1.25, 15, 2)).optimize())},
            {RestaurantKitchenBlock.rotations(box(2, 0, 2.25, 14, 10.5, 14)),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 3, 14, 10.5, 14), box(1.5, .75, -7.75, 2.5, 10, 3.25)).optimize())}};
    //? if >=1.21 {
    public static final MapCodec<HotelRoomApplianceBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelRoomApplianceBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelRoomApplianceBlock::new));
    //?}
    public HotelRoomApplianceBlock(Design design, Properties properties) {
        super(properties); this.design = design; registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.OPEN);
    }
    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggle(state, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return toggle(state, level, pos, player); }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][state.getValue(BlockStateProperties.OPEN) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
