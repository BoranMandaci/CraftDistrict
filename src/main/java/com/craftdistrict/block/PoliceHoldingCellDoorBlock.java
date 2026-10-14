package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla two-half door lifecycle, hand opening and metal sounds; no block entity. */
public final class PoliceHoldingCellDoorBlock extends DoorBlock {
    private static final BlockSetType TYPE = new BlockSetType("craftdistrict:holding_cell", true,
            //? if >=1.21 {
            false, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
            //?}
            SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);
    //? if >=1.21 {
    public static final MapCodec<PoliceHoldingCellDoorBlock> CODEC = simpleCodec(PoliceHoldingCellDoorBlock::new);
    //?}

    public PoliceHoldingCellDoorBlock(Properties properties) {
        //? if >=1.21 {
        super(TYPE, properties);
        //?} else {
        /*super(properties, TYPE);
        *///?}
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Filled door plane is easy to target without selecting the entire cube.
        return super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Use vanilla's filled edge plane for both closed and swung-open states.
        return super.getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }
    //?}
}
