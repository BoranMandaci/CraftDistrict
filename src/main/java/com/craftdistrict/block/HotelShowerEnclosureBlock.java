package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Local corner ownership and reciprocal door pairs use saved states, never entities or ticks. */
public class HotelShowerEnclosureBlock extends MultiBlockDecorationBlock {
    public enum Corner implements StringRepresentable {
        NONE, JOIN, OWNER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public enum Pair implements StringRepresentable {
        SINGLE, LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final EnumProperty<Corner> LEFT_CORNER = EnumProperty.create("left_corner", Corner.class);
    public static final EnumProperty<Corner> RIGHT_CORNER = EnumProperty.create("right_corner", Corner.class);
    public static final EnumProperty<Pair> PAIR = EnumProperty.create("pair", Pair.class);
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[][][][][] SINGLE_SHAPES = new VoxelShape[2][2][2][12][];
    private static final VoxelShape[][][][][] PAIR_SHAPES = new VoxelShape[2][2][2][4][];
    static {
        for (int style = 0; style < 2; style++) for (int upper = 0; upper < 2; upper++)
            for (int open = 0; open < 2; open++) {
                for (int left = 0; left < 4; left++) for (int right = 0; right < 3; right++) {
                    if (style == 1 && open == 1) {
                        SINGLE_SHAPES[style][upper][open][left * 3 + right] = SINGLE_SHAPES[style][upper][0][left * 3 + right];
                    } else if (left == 3) {
                        SINGLE_SHAPES[style][upper][open][left * 3 + right] = SINGLE_SHAPES[style][upper][open][6 + right];
                    } else if (right == 2) {
                        SINGLE_SHAPES[style][upper][open][left * 3 + right] = SINGLE_SHAPES[style][upper][open][left * 3 + 1];
                    } else {
                        VoxelShape shape = Shapes.or(body(style == 0, Pair.SINGLE, upper, open == 1),
                                end(false, left, upper), end(true, right == 0 ? 0 : right + 1, upper));
                        SINGLE_SHAPES[style][upper][open][left * 3 + right] = RestaurantKitchenBlock.rotations(shape.optimize());
                    }
                }
                if (style == 0) for (int outer = 0; outer < 4; outer++) {
                    PAIR_SHAPES[0][upper][open][outer] = outer == 3 ? PAIR_SHAPES[0][upper][open][2]
                            : RestaurantKitchenBlock.rotations(Shapes.or(body(true, Pair.LEFT, upper, open == 1), end(false, outer, upper)).optimize());
                    if (outer < 3) PAIR_SHAPES[1][upper][open][outer] = outer == 2 ? PAIR_SHAPES[1][upper][open][1]
                            : RestaurantKitchenBlock.rotations(Shapes.or(body(true, Pair.RIGHT, upper, open == 1), end(true, outer == 0 ? 0 : outer + 1, upper)).optimize());
                }
            }
    }
    private static VoxelShape body(boolean sliding, Pair pair, int upper, boolean open) {
        double railStart = pair == Pair.RIGHT ? 0 : .5, railEnd = pair == Pair.LEFT ? 16 : 15.5;
        VoxelShape track = upper == 0 ? box(railStart, 0, 14.25, railEnd, .5, 16.375)
                : Shapes.or(box(railStart, 15, 15.625, railEnd, 16, 16.375),
                        box(railStart, 15.125, 14.25, railEnd, 15.875, 15.625));
        double x0 = .5, x1 = 15.5, y0 = upper == 0 ? .5 : 0, y1 = upper == 0 ? 16 : 15;
        if (sliding) {
            if (open) { x0 = pair == Pair.RIGHT ? 10 : .5; x1 = pair == Pair.RIGHT ? 15.5 : pair == Pair.LEFT ? 6 : 5.5; }
            else if (pair == Pair.LEFT) x1 = 16;
            else if (pair == Pair.RIGHT) x0 = 0;
        }
        VoxelShape panels = box(x0, y0, sliding ? 14.5 : 15.875, x1, y1, sliding ? 16.25 : 16.125);
        if (sliding) {
            double handle = pair == Pair.SINGLE ? 13.25 : 14.5;
            if (open) handle -= 10;
            if (pair == Pair.RIGHT) handle = 16 - handle - .375;
            panels = Shapes.or(panels, box(handle, upper == 0 ? 13.25 : 0, 13.875,
                    handle + .375, upper == 0 ? 16 : 2.75, 14.625));
        }
        return Shapes.or(track, panels);
    }
    /** 0: outer post, 1: straight join, 2/3: precise meeting face of a shared corner post. */
    private static VoxelShape end(boolean right, int kind, int upper) {
        double x0 = right ? 15.5 : 0, x1 = right ? 16 : .5;
        double y0 = upper == 0 ? .5 : 0, y1 = upper == 0 ? 16 : 15;
        if (kind == 0) return box(x0, 0, 14.25, x1, 16, 16.375);
        if (kind == 1) return Shapes.or(box(x0, y0, 15.875, x1, y1, 16.125),
                upper == 0 ? box(x0, 0, 14.25, x1, .5, 16.375) : box(x0, 15, 14.25, x1, 16, 16.375));
        double edge0 = right ? 15.5 : .375, edge1 = right ? 15.625 : .5;
        VoxelShape result = Shapes.or(box(edge0, y0, 14.5, edge1, y1, 16.125),
                upper == 0 ? box(edge0, 0, 14.25, edge1, .5, 16.375)
                        : Shapes.or(box(edge0, 15, 15.625, edge1, 16, 16.375), box(edge0, 15.125, 14.25, edge1, 15.875, 15.625)));
        // Both components can select/collide with the visible shared post; only its owner renders it.
        result = Shapes.or(result, box(right ? 15.625 : -.375, 0, 15.625, right ? 16.375 : .375, 16, 16.375));
        return result;
    }
    //? if >=1.21 {
    public static final MapCodec<HotelShowerEnclosureBlock> CODEC = simpleCodec(HotelShowerEnclosureBlock::new);
    //?}
    protected boolean sliding() { return true; }
    public HotelShowerEnclosureBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(BlockStateProperties.OPEN, false).setValue(LEFT, false)
                .setValue(LEFT_CORNER, Corner.NONE).setValue(RIGHT_CORNER, Corner.NONE);
        if (sliding()) state = state.setValue(PAIR, Pair.SINGLE);
        registerDefaultState(state);
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(BlockStateProperties.OPEN, LEFT, LEFT_CORNER, RIGHT_CORNER);
        if (sliding()) builder.add(PAIR);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    private static boolean complete(LevelReader level, BlockPos origin, Direction facing) {
        if (!level.hasChunkAt(origin) || !level.hasChunkAt(origin.above())) return false;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        return lower.getBlock() instanceof HotelShowerEnclosureBlock && upper.is(lower.getBlock())
                && lower.getValue(PRIMARY) && !upper.getValue(PRIMARY)
                && lower.getValue(FACING) == facing && upper.getValue(FACING) == facing;
    }
    private static boolean localLoaded(LevelReader level, BlockPos origin) {
        for (int x = -1; x <= 1; x += 2) for (int z = -1; z <= 1; z += 2)
            if (!level.hasChunkAt(origin.offset(x, 0, z))) return false;
        return true;
    }
    private static BlockPos vertex(BlockPos origin, Direction facing, boolean right) {
        return switch (facing) {
            case NORTH -> origin.offset(right ? 1 : 0, 0, 1);
            case EAST -> origin.offset(0, 0, right ? 1 : 0);
            case SOUTH -> origin.offset(right ? 0 : 1, 0, 0);
            case WEST -> origin.offset(1, 0, right ? 0 : 1);
            default -> throw new IllegalArgumentException("Horizontal shower facing required");
        };
    }
    private static boolean straight(LevelReader level, BlockPos origin, Direction facing, boolean right) {
        return complete(level, origin.relative(right ? facing.getClockWise() : facing.getCounterClockWise()), facing);
    }
    private static Corner corner(LevelReader level, BlockPos origin, Direction facing, boolean right) {
        if (straight(level, origin, facing, right)) return Corner.NONE;
        BlockPos point = vertex(origin, facing, right), owner = origin;
        boolean perpendicular = false;
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) {
            BlockPos candidate = point.offset(x, 0, z);
            if (!level.hasChunkAt(candidate)) continue;
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof HotelShowerEnclosureBlock)) continue;
            Direction otherFacing = state.getValue(FACING);
            if (!complete(level, candidate, otherFacing)) continue;
            boolean otherRight = vertex(candidate, otherFacing, true).equals(point);
            if (!otherRight && !vertex(candidate, otherFacing, false).equals(point)) continue;
            if (straight(level, candidate, otherFacing, otherRight)) continue;
            perpendicular |= otherFacing.getAxis() != facing.getAxis();
            if (candidate.compareTo(owner) < 0) owner = candidate;
        }
        return !perpendicular ? Corner.NONE : owner.equals(origin) ? Corner.OWNER : Corner.JOIN;
    }
    private static BlockState layout(LevelReader level, BlockPos origin, BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, straight(level, origin, facing, false))
                .setValue(LEFT_CORNER, corner(level, origin, facing, false))
                .setValue(RIGHT_CORNER, corner(level, origin, facing, true));
    }
    private static Pair role(BlockState state) { return state.hasProperty(PAIR) ? state.getValue(PAIR) : Pair.SINGLE; }
    private static BlockPos partnerPos(BlockState state, BlockPos origin) {
        return origin.relative(role(state) == Pair.LEFT ? state.getValue(FACING).getClockWise() : state.getValue(FACING).getCounterClockWise());
    }
    private static boolean validPair(LevelReader level, BlockPos origin, BlockState state) {
        Pair role = role(state);
        if (role == Pair.SINGLE) return false;
        BlockPos partner = partnerPos(state, origin);
        if (!complete(level, partner, state.getValue(FACING))) return false;
        BlockState other = level.getBlockState(partner);
        return other.is(state.getBlock()) && role(other) == (role == Pair.LEFT ? Pair.RIGHT : Pair.LEFT);
    }
    /** Fixed-size writes follow the same ownership/rollback rules as linked cabinet toggles. */
    private static boolean commit(Level level, BlockPos[] positions, BlockState[] before, BlockState[] after) {
        int written = 0;
        boolean valid = true;
        for (int i = 0; i < positions.length; i++) {
            if (level.getBlockState(positions[i]) != before[i]
                    || before[i] != after[i] && !level.setBlock(positions[i], after[i], FLAGS)) { valid = false; break; }
            written++;
            for (int j = 0; j < written; j++) if (level.getBlockState(positions[j]) != after[j]) valid = false;
            if (!valid) break;
        }
        if (valid) return true;
        for (int i = written - 1; i >= 0; i--) if (level.getBlockState(positions[i]) == after[i]) level.setBlock(positions[i], before[i], FLAGS);
        return false;
    }
    private static boolean column(Level level, BlockPos origin, BlockState joined) {
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        HotelShowerEnclosureBlock owner = (HotelShowerEnclosureBlock) joined.getBlock();
        BlockState nextUpper = owner.structurePartState(joined, 1);
        return lower == joined && upper == nextUpper || commit(level, new BlockPos[]{origin, origin.above()},
                new BlockState[]{lower, upper}, new BlockState[]{joined, nextUpper});
    }
    private static boolean makePair(Level level, BlockPos leftPos, BlockPos rightPos, Direction facing) {
        if (!complete(level, leftPos, facing) || !complete(level, rightPos, facing)) return false;
        BlockState left = level.getBlockState(leftPos), right = level.getBlockState(rightPos);
        if (!left.is(right.getBlock()) || !((HotelShowerEnclosureBlock) left.getBlock()).sliding()
                || role(left) != Pair.SINGLE || role(right) != Pair.SINGLE) return false;
        HotelShowerEnclosureBlock owner = (HotelShowerEnclosureBlock) left.getBlock();
        BlockState leftUpper = level.getBlockState(leftPos.above()), rightUpper = level.getBlockState(rightPos.above());
        if (leftUpper != owner.structurePartState(left, 1) || rightUpper != owner.structurePartState(right, 1)) return false;
        boolean open = left.getValue(BlockStateProperties.OPEN) || right.getValue(BlockStateProperties.OPEN);
        BlockState newLeft = layout(level, leftPos, left).setValue(PAIR, Pair.LEFT).setValue(BlockStateProperties.OPEN, open);
        BlockState newRight = layout(level, rightPos, right).setValue(PAIR, Pair.RIGHT)
                .setValue(BlockStateProperties.OPEN, open);
        return commit(level, new BlockPos[]{leftPos, leftPos.above(), rightPos, rightPos.above()},
                new BlockState[]{left, leftUpper, right, rightUpper},
                new BlockState[]{newLeft, owner.structurePartState(newLeft, 1), newRight, owner.structurePartState(newRight, 1)});
    }
    private static void refresh(Level level, BlockPos origin) {
        if (!level.hasChunkAt(origin) || !localLoaded(level, origin)) return;
        BlockState state = level.getBlockState(origin);
        if (!(state.getBlock() instanceof HotelShowerEnclosureBlock owner) || !complete(level, origin, state.getValue(FACING))) return;
        if (role(state) != Pair.SINGLE && !validPair(level, origin, state)) state = state.setValue(PAIR, Pair.SINGLE);
        if (!column(level, origin, layout(level, origin, state))) return;
        state = level.getBlockState(origin);
        if (validPair(level, origin, state)) {
            BlockPos partner = partnerPos(state, origin);
            BlockPos leftPos = role(state) == Pair.LEFT ? origin : partner;
            BlockPos rightPos = role(state) == Pair.RIGHT ? origin : partner;
            BlockState left = level.getBlockState(leftPos), right = level.getBlockState(rightPos);
            if (left.getValue(BlockStateProperties.OPEN) != right.getValue(BlockStateProperties.OPEN))
                column(level, rightPos, right.setValue(BlockStateProperties.OPEN, left.getValue(BlockStateProperties.OPEN)));
        }
        if (owner.sliding() && role(state) == Pair.SINGLE) {
            Direction facing = state.getValue(FACING);
            if (!makePair(level, origin.relative(facing.getCounterClockWise()), origin, facing))
                makePair(level, origin, origin.relative(facing.getClockWise()), facing);
        }
    }
    private static void refreshArea(Level level, BlockPos origin) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) refresh(level, origin.offset(x, 0, z));
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY)) refreshArea(level, pos);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!level.isClientSide && previous.is(this) && previous.getValue(FACING) != state.getValue(FACING))
            refreshArea(level, structureOrigin(state, pos));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos)) refresh(level, structureOrigin(state, pos));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = structureOrigin(state, pos).below();
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshArea(level, origin);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(LEFT, false)
                .setValue(LEFT_CORNER, state.getValue(RIGHT_CORNER)).setValue(RIGHT_CORNER, state.getValue(LEFT_CORNER));
        if (sliding() && role(state) != Pair.SINGLE) result = result.setValue(PAIR, role(state) == Pair.LEFT ? Pair.RIGHT : Pair.LEFT);
        return mirror == Mirror.NONE ? state : result;
    }
    private InteractionResult toggle(Level level, BlockPos clickedPos, Player player) {
        if (!sliding()) return InteractionResult.PASS;
        BlockState clicked = level.getBlockState(clickedPos);
        if (!clicked.is(this)) return InteractionResult.PASS;
        BlockPos origin = structureOrigin(clicked, clickedPos);
        if (!level.isClientSide) refresh(level, origin);
        BlockState primary = level.getBlockState(origin);
        if (!primary.is(this)) return InteractionResult.PASS;
        if (role(primary) == Pair.SINGLE)
            return CabinetInteraction.toggleLinked(this, level, clickedPos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
        if (!validPair(level, origin, primary)) return InteractionResult.PASS;
        BlockPos otherPos = partnerPos(primary, origin); BlockState other = level.getBlockState(otherPos);
        BlockState upper = level.getBlockState(origin.above()), otherUpper = level.getBlockState(otherPos.above());
        if (upper != structurePartState(primary, 1) || otherUpper != structurePartState(other, 1)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean open = !primary.getValue(BlockStateProperties.OPEN);
        BlockState next = primary.setValue(BlockStateProperties.OPEN, open), nextOther = other.setValue(BlockStateProperties.OPEN, open);
        if (!commit(level, new BlockPos[]{origin, origin.above(), otherPos, otherPos.above()},
                new BlockState[]{primary, upper, other, otherUpper},
                new BlockState[]{next, structurePartState(next, 1), nextOther, structurePartState(nextOther, 1)})) return InteractionResult.PASS;
        com.craftdistrict.item.WrenchItem.usedIfHeld(player);
        level.playSound(null, origin, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, .6F, 1.1F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, origin);
        return InteractionResult.CONSUME;
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return toggle(level, pos, player); }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int upper = state.getValue(PRIMARY) ? 0 : 1, open = state.getValue(BlockStateProperties.OPEN) ? 1 : 0;
        int left = state.getValue(LEFT_CORNER) == Corner.NONE ? state.getValue(LEFT) ? 1 : 0 : state.getValue(LEFT_CORNER).ordinal() + 1;
        int right = state.getValue(RIGHT_CORNER).ordinal(), turn = RestaurantKitchenBlock.turn(state);
        return role(state) == Pair.LEFT ? PAIR_SHAPES[0][upper][open][left][turn]
                : role(state) == Pair.RIGHT ? PAIR_SHAPES[1][upper][open][right][turn]
                : SINGLE_SHAPES[sliding() ? 0 : 1][upper][open][left * 3 + right][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
