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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ordinary paired block states provide decorative doors without inventories or block entities. */
public final class PharmacyCabinetBlock extends HospitalEquipmentBlock {
    public enum Style implements StringRepresentable {
        REFRIGERATOR, SECURED;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final Style style;
    //? if >=1.21 {
    public static final MapCodec<PharmacyCabinetBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Style::values).fieldOf("style").forGetter((PharmacyCabinetBlock block) -> block.style),
            propertiesCodec()).apply(instance, PharmacyCabinetBlock::new));
    //?}
    private static final VoxelShape[] CLOSED = RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 16, 16));
    private static final VoxelShape[][] OPEN_SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1.5, 16, 16, 16),
                    box(0, 1.25, -13.125, .75, 16, 1.375)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1.5, 16, 16, 16),
                    box(0, 0, -13.125, .75, 14.25, 1.375)).optimize())};

    public PharmacyCabinetBlock(Style style, Properties properties) {
        super(Design.STERILE_CABINET, properties);
        this.style = style;
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(OPEN);
    }
    private InteractionResult toggle(Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return state.getValue(OPEN) ? OPEN_SHAPES[state.getValue(PRIMARY) ? 0 : 1][turn] : CLOSED[turn];
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = super.getBlockSupportShape(state, level, pos);
        return state.getValue(PRIMARY) ? shape : DecorationSupport.withTop(shape);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
