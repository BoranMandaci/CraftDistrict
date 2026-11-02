package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla door placement, hinges, use, partner removal and Creative/Survival drops. */
public final class OfficeGlassDoorBlock extends DoorBlock {
    private static final BlockSetType TYPE = new BlockSetType("craftdistrict:office_glass", true,
            //? if >=1.21 {
            false, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
            //?}
            SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);
    //? if >=1.21 {
    public static final MapCodec<OfficeGlassDoorBlock> CODEC = simpleCodec(OfficeGlassDoorBlock::new);
    //?}
    public OfficeGlassDoorBlock(Properties properties) {
        //? if >=1.21 {
        super(TYPE, properties);
        //?} else {
        /*super(properties, TYPE);
        *///?}
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && DecorationSupport.isPlaced(state, level, pos)
                || super.canSurvive(state, level, pos);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction corrected = context.getHorizontalDirection().getOpposite();
        Direction aligned = null;
        // A legacy row may face the other way. Follow its actual plane, not the
        // player's approach, while retaining the corrected standalone direction.
        for (Direction side : new Direction[]{corrected.getClockWise(), corrected.getCounterClockWise()}) {
            BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(side));
            if (!(neighbor.getBlock() instanceof OfficeGlassPartitionBlock) && !neighbor.is(this)) continue;
            Direction row = neighbor.getValue(FACING);
            if (row.getAxis() != corrected.getAxis()) continue;
            if (row == corrected) { aligned = row; break; }
            if (aligned == null) aligned = row;
        }
        Direction facing = aligned == null ? corrected : aligned;
        // Vanilla computes the hinge using the same final direction as the leaf.
        return super.getStateForPlacement(new BlockPlaceContext(context) {
            @Override public Direction getHorizontalDirection() {
                return facing;
            }
        });
    }
    //? if >=1.21 {
    @Override public MapCodec<? extends DoorBlock> codec() { return CODEC; }
    //?}
}
