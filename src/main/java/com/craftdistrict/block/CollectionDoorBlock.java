package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** The eight Doors Collection leaves share the standard hinged-door pairing lifecycle. */
public final class CollectionDoorBlock extends PairedDoorBlock {
    public enum Design implements StringRepresentable {
        FIRE_EXIT, VENTILATED_UTILITY, MESH_SECURITY, ORNATE_WOODEN,
        COLD_STORAGE, KITCHEN_SERVICE, INDUSTRIAL_RIVETED, RUSTIC_WOODEN;
        boolean wooden() { return this == ORNATE_WOODEN || this == RUSTIC_WOODEN; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private static final BlockSetType METAL = new BlockSetType("craftdistrict:hinged_metal", true,
            //? if >=1.21 {
            false, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
            //?}
            SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<CollectionDoorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CollectionDoorBlock b) -> b.design),
            propertiesCodec()).apply(instance, CollectionDoorBlock::new));
    @Override public MapCodec<? extends DoorBlock> codec() { return CODEC; }
    //?}

    public CollectionDoorBlock(Design design, Properties properties) {
        super(design.wooden() ? BlockSetType.OAK : METAL, properties,
                design == Design.COLD_STORAGE ? 3.5 : 2.5, design == Design.COLD_STORAGE);
        this.design = design;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (design != Design.COLD_STORAGE) return super.getStateForPlacement(context);
        Direction front = context.getHorizontalDirection().getOpposite();
        // The freezer's external latch is on its north/front face. Compute the
        // vanilla hinge and facing together so that face points toward the placer.
        return super.getStateForPlacement(new BlockPlaceContext(context) {
            @Override public Direction getHorizontalDirection() { return front; }
        });
    }
}
