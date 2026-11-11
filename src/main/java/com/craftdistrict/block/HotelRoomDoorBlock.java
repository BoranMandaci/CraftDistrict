package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Vanilla edge hinges, two-half lifecycle, hand interaction, and survival drops. */
public final class HotelRoomDoorBlock extends DoorBlock {
    private static final VoxelShape[][][][] SHAPES = new VoxelShape[2][2][2][];
    static {
        for (int upper = 0; upper < 2; upper++) for (int open = 0; open < 2; open++) for (int right = 0; right < 2; right++) {
            VoxelShape leaf = open == 0 ? box(0, 0, 13, 16, 16, 16) : box(right == 0 ? 0 : 13, 0, 0, right == 0 ? 3 : 16, 16, 16);
            VoxelShape frame = Shapes.or(box(0, 0, 13.25, .375, 16, 15.75), box(15.625, 0, 13.25, 16, 16, 15.75));
            if (upper == 1) frame = Shapes.or(frame, box(.375, 15.5, 13.25, 15.625, 16, 15.75));
            SHAPES[upper][open][right] = RestaurantKitchenBlock.rotations(Shapes.or(leaf, frame).optimize());
        }
    }
    //? if >=1.21 {
    public static final MapCodec<HotelRoomDoorBlock> CODEC = simpleCodec(HotelRoomDoorBlock::new);
    //?}
    public HotelRoomDoorBlock(Properties properties) {
        //? if >=1.21 {
        super(BlockSetType.OAK, properties);
        //?} else {
        /*super(properties, BlockSetType.OAK);
        *///?}
    }
    //? if >=1.21 {
    @Override public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        var result = super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide && result.consumesAction() && level.getBlockState(pos) != state)
            com.craftdistrict.item.WrenchItem.usedIfHeld(player);
        return result;
    }
    //?} else {
    /*@Override public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var result = super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide && result.consumesAction() && level.getBlockState(pos) != state)
            com.craftdistrict.item.WrenchItem.used(player, hand, player.getItemInHand(hand));
        return result;
    }
    *///?}
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && DecorationSupport.isPlaced(state, level, pos)
                || super.canSurvive(state, level, pos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(HALF) == DoubleBlockHalf.UPPER ? 1 : 0][state.getValue(OPEN) ? 1 : 0]
                [state.getValue(HINGE) == DoorHingeSide.RIGHT ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override public MapCodec<? extends DoorBlock> codec() { return CODEC; }
    //?}
}
