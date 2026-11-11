package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import com.craftdistrict.menu.RoomNumberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;

public final class RoomNumberPlaqueBlock extends HotelCorridorWallBlock implements EntityBlock {
    public static final BooleanProperty CONFIGURED = BooleanProperty.create("configured");
    //? if >=1.21 {
    public static final MapCodec<RoomNumberPlaqueBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).optionalFieldOf("design", Design.PLAQUE).forGetter((RoomNumberPlaqueBlock b) -> b.design),
            propertiesCodec()).apply(instance, RoomNumberPlaqueBlock::new));
    //?}
    public RoomNumberPlaqueBlock(Properties properties) { this(Design.PLAQUE, properties); }
    public RoomNumberPlaqueBlock(Design design, Properties properties) {
        super(design, properties);
        if (design != Design.PLAQUE && design != Design.INDICATOR && design != Design.DIRECTION_SIGN) throw new IllegalArgumentException("Unsupported configurable sign");
        registerDefaultState(defaultBlockState().setValue(CONFIGURED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(CONFIGURED);
    }
    public int kind() { return design == Design.INDICATOR ? 1 : design == Design.DIRECTION_SIGN ? 2 : 0; }
    public String defaultNumber() { return kind() == 1 ? "03" : kind() == 2 ? "101-116" : "101"; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RoomNumberPlaqueBlockEntity(pos, state); }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return RoomNumberMenu.open(level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? RoomNumberMenu.open(level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
