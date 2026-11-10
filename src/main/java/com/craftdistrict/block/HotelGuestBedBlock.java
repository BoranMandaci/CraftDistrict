package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import com.craftdistrict.entity.MedicalResting;
import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.lighting.LightControls;
import com.craftdistrict.lighting.LightDyeData;
import com.craftdistrict.menu.LightColorMenu;
import com.craftdistrict.network.LightDyeNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.21 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;

/** One shared footprint; each column has its own decorative resting anchor. Colors use the existing palette. */
public final class HotelGuestBedBlock extends MultiBlockDecorationBlock implements LyingBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    // Missing property in old saves defaults to the original head-origin, two-row layout.
    public static final BooleanProperty FOOT_ORIGIN = BooleanProperty.create("foot_origin");
    private final int width;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 0, 16, 11.5, 16),
                    box(0, 0, 0, 16, 12.5, 2.5), box(.75, 11.5, 3.5, 15.25, 12.5, 11)).optimize()),
            RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 11.25, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelGuestBedBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 3).fieldOf("width").forGetter((HotelGuestBedBlock b) -> b.width),
            propertiesCodec()).apply(instance, HotelGuestBedBlock::new));
    //?}

    public HotelGuestBedBlock(int width, Properties properties) {
        super(properties); this.width = width;
        registerDefaultState(defaultBlockState().setValue(FOOT_ORIGIN, false));
    }
    public boolean dyeable() { return width < 3; }
    private int length(BlockState state) { return width == 3 && state.getValue(FOOT_ORIGIN) ? 3 : 2; }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, FOOT_ORIGIN); }
    @Override protected int partCount() { return width * (width == 3 ? 3 : 2); }
    @Override protected int partCount(BlockState state) { return width * length(state); }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index % width)
                .relative(state.getValue(FOOT_ORIGIN) ? state.getValue(FACING) : state.getValue(FACING).getOpposite(), index / width);
    }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) { return context.getHorizontalDirection(); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(FOOT_ORIGIN, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (partIndex(state) >= partCount(state)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos below = pos.below();
        return level.hasChunkAt(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < partCount(state); part++)
            if (otherPos.equals(origin.offset(partOffset(state, part))) && neighbor != stateForPart(state, part))
                return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockPos lyingHead(BlockState state, BlockPos pos) {
        return headRowOrigin(state, pos).relative(state.getValue(FACING).getClockWise(), partIndex(state) % width);
    }
    private BlockPos headRowOrigin(BlockState state, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos);
        return state.getValue(FOOT_ORIGIN) ? origin.relative(state.getValue(FACING), length(state) - 1) : origin;
    }
    @Override public double lyingHeight() { return 11.5 / 16; }
    @Override public AABB lyingClearance(BlockState state, BlockPos head) { return new AABB(head.above()).deflate(.25, 0, .25); }

    /** One paid dye per whole bed. Every part must exist, agree, and be editable before the palette changes. */
    public boolean color(Level level, BlockPos pos, Player player, int selected) {
        if (!(level instanceof ServerLevel server) || !dyeable() || selected < 0 || selected > 15
                || !player.isAlive() || player.isSpectator() || !player.mayBuild() || player.level() != level
                || player.distanceToSqr(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5) > 64) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return false;
        List<BlockPos> parts = structureParts(state, pos);
        if (!LightControls.complete(level, state, parts)) return false;
        LightDyeData data = LightDyeData.get(server); int previous = data.color(parts.get(0));
        for (BlockPos part : parts)
            if (!level.getWorldBorder().isWithinBounds(part) || !level.mayInteract(player, part) || data.color(part) != previous) return false;
        if (previous == selected) return true;
        if (player.getAbilities().instabuild) {
            for (BlockPos part : parts) data.putUnpaid(part, selected);
        } else {
            LightDyeData.Payment payment = data.payment(player, parts, selected, false, true);
            if (payment == null) return false;
            payment.commit(player);
        }
        LightDyeNetworking.changed(server, parts);
        return true;
    }
    public InteractionResult useColor(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!dyeable()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack alternative = player.getItemInHand(other);
            if (alternative.getItem() instanceof DyeItem || alternative.getItem() instanceof WrenchItem) { held = alternative; hand = other; }
        }
        if (held.getItem() instanceof WrenchItem) return LightColorMenu.open(state, level, pos, player, hand);
        if (!(held.getItem() instanceof DyeItem dye)) return InteractionResult.PASS;
        if (!level.isClientSide) color(level, pos, player, dye.getDyeColor().getId());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return MedicalResting.lie(state, level, pos, player);
    }
    @Override public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                   Player player, InteractionHand hand, BlockHitResult hit) {
        return useColor(state, level, pos, player, hand).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useColor(state, level, pos, player, hand);
        return result.consumesAction() ? result : hand == InteractionHand.MAIN_HAND ? MedicalResting.lie(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level instanceof ServerLevel server) {
            if (!isRemovingLinkedStructure(state, level, pos)) {
                BlockPos origin = headRowOrigin(state, pos);
                for (int column = 0; column < width; column++)
                    MedicalResting.removeAt(level, origin.relative(state.getValue(FACING).getClockWise(), column), state);
                for (var player : server.players())
                    if (player.containerMenu instanceof LightColorMenu menu && menu.targets(pos)) player.closeContainer();
            }
            if (dyeable() && LightDyeData.get(server).put(pos, -1)) LightDyeNetworking.changed(server, List.of(pos));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror); int part = partIndex(state);
        return mirror == Mirror.NONE || part >= partCount(state) ? result
                : result.setValue(PART, part / width * width + width - 1 - part % width);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean head = partIndex(state) / width == (state.getValue(FOOT_ORIGIN) ? length(state) - 1 : 0);
        return SHAPES[head ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
