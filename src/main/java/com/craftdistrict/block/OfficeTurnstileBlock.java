package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One passage controller per lane, with a stationary pedestal and a player-specific waist bar. */
public final class OfficeTurnstileBlock extends OfficeTurnstileFixtureBlock implements EntityBlock {
    //? if >=1.21 {
    public static final MapCodec<OfficeTurnstileBlock> CODEC = simpleCodec(OfficeTurnstileBlock::new);
    //?}
    public static final double HUB_X = 6.875 / 16, HUB_Y = 9.625 / 16, HUB_Z = .5;
    public static final float AXLE_X = .8F, AXLE_Y = -.6F;
    public static final int APPROACH_LOOKAHEAD = 2;
    private static final double MAX_APPROACH_CLEARANCE = .25, SERVER_POSITION_TOLERANCE = .125, CONTACT_TOLERANCE = .025;
    private static final double ARM_THICKNESS = .375 / 16;
    private static final VoxelShape PEDESTAL = Shapes.or(
            box(1, 0, 2.5, 5.5, 1, 13.5), box(1, 1, 3, 5.5, 15, 13),
            box(1, 15, 3.25, 5.5, 15.5, 12.75), box(1.625, 15.5, 4, 4.875, 15.75, 7),
            box(2, 15.5, 10, 4.5, 15.625, 10.5));
    private static final VoxelShape BEARING = Shapes.or(box(5.5, 8.5, 7, 6.5, 10.75, 9),
            box(6.5, 8.75, 7.125, 7.25, 10.5, 8.875));
    private static final VoxelShape WAIST_BAR = box(7.25, 9.25, 7.625, 14.5, 10, 8.375);
    private static final double WAIST_BAR_TOP = WAIST_BAR.max(Direction.Axis.Y);
    private static final VoxelShape RESTING_GATE = Shapes.or(BEARING, WAIST_BAR);
    private static final VoxelShape RAIL_MOUNT = OfficeTurnstileSideBarrierBlock.rails(0, 7.25, 1, 8.75);
    private static final VoxelShape[][] BODY = mountedShapes(PEDESTAL, RAIL_MOUNT);
    private static final VoxelShape[][] BLOCKED = mountedShapes(Shapes.or(PEDESTAL, RESTING_GATE), RAIL_MOUNT);
    private static final VoxelShape[][] WALKING_BLOCKED = mountedShapes(closedWalkingShape(), RAIL_MOUNT);
    private static final VoxelShape[][] OUTLINE = mountedShapes(restingOutline(), RAIL_MOUNT);
    private static final AABB[] BAR_BOUNDS = new AABB[4];

    static {
        VoxelShape[] bars = RestaurantKitchenBlock.rotations(WAIST_BAR);
        for (int i = 0; i < bars.length; i++) BAR_BOUNDS[i] = bars[i].bounds();
    }

    public OfficeTurnstileBlock(Properties properties) { super(properties); }

    @Override Direction barrierSide(BlockState state) { return state.getValue(FACING).getCounterClockWise(); }

    private static VoxelShape closedWalkingShape() {
        VoxelShape shape = PEDESTAL;
        // A closed gate stops forward movement without giving vanilla a step-height ledge.
        for (AABB part : RESTING_GATE.toAabbs()) {
            shape = Shapes.or(shape, Shapes.box(part.minX, part.minY, part.minZ, part.maxX, 1.25, part.maxZ));
        }
        return shape;
    }

    private static VoxelShape restingOutline() {
        VoxelShape shape = Shapes.or(PEDESTAL, RESTING_GATE);
        // The two downward arms are included only for block selection.
        double x = 1.5 * AXLE_X * AXLE_X - .5;
        double y = 1.5 * AXLE_X * AXLE_Y;
        double z = -AXLE_Y * Math.sqrt(3) / 2;
        for (int side : new int[]{-1, 1}) {
            for (int segment = 0; segment < 4; segment++) {
                double a = .375 + 7.25 * segment / 4, b = a + 7.25 / 4;
                double z1 = 8 + side * z * a, z2 = 8 + side * z * b;
                shape = Shapes.or(shape, box(6.875 + x * a - .4, 9.625 + y * b - .4, Math.min(z1, z2) - .4,
                        6.875 + x * b + .4, 9.625 + y * a + .4, Math.max(z1, z2) + .4));
            }
        }
        return shape;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OfficeTurnstileBlockEntity(pos, state);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fixtureShape(OUTLINE, state);
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player player) {
            // Above the real bar there is no extended surface to land on.
            if (player.getBoundingBox().minY >= pos.getY() + WAIST_BAR_TOP) return fixtureShape(BODY, state);
            // A player straddling a returning bar can leave in either direction.
            // The shallow bearing is also cleared during passage so a row has a usable shoulder width.
            if (overlapsBar(state, pos, player)
                    || nearLane(state, pos, player) && level.getBlockEntity(pos) instanceof OfficeTurnstileBlockEntity turnstile
                    && turnstile.allowsPassage(player, player.level().getGameTime())) return fixtureShape(BODY, state);
            return fixtureShape(WALKING_BLOCKED, state);
        }
        return fixtureShape(BLOCKED, state);
    }

    public static boolean canPush(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.getAbilities().flying;
    }

    /** Signed distance from the stationary arm along the lane, in the block's model coordinates. */
    static double depth(BlockState state, BlockPos pos, Player player) {
        Direction facing = state.getValue(FACING);
        return -(player.getX() - pos.getX() - .5) * facing.getStepX()
                - (player.getZ() - pos.getZ() - .5) * facing.getStepZ();
    }

    private static double across(BlockState state, BlockPos pos, Player player) {
        Direction right = state.getValue(FACING).getClockWise();
        return .5 + (player.getX() - pos.getX() - .5) * right.getStepX()
                + (player.getZ() - pos.getZ() - .5) * right.getStepZ();
    }

    static boolean nearLane(BlockState state, BlockPos pos, Player player) {
        return nearLane(pos, player, across(state, pos, player));
    }

    private static boolean nearLane(BlockPos pos, Player player, double across) {
        var box = player.getBoundingBox();
        return across >= 5.5 / 16 + player.getBbWidth() * .5 - CONTACT_TOLERANCE && across < 1
                && box.maxY > pos.getY() + 9.25 / 16 && box.minY < pos.getY() + WAIST_BAR_TOP;
    }

    static boolean overlapsBar(BlockState state, BlockPos pos, Player player) {
        AABB bar = BAR_BOUNDS[RestaurantKitchenBlock.turn(state)];
        double x = pos.getX(), y = pos.getY(), z = pos.getZ(), tolerance = 1e-5;
        return player.getBoundingBox().intersects(x + bar.minX + tolerance, y + bar.minY, z + bar.minZ + tolerance,
                x + bar.maxX - tolerance, y + bar.maxY, z + bar.maxZ - tolerance);
    }

    /** Position validation is shared by the client movement request and the server. */
    public int approach(BlockState state, BlockPos pos, Player player) {
        return approachDirection(state, pos, player, null);
    }

    public int pushDirection(BlockState state, BlockPos pos, Player player, Vec3 movement) {
        return approachDirection(state, pos, player, movement);
    }

    private static int approachDirection(BlockState state, BlockPos pos, Player player, Vec3 movement) {
        double across = across(state, pos, player);
        if (!nearLane(pos, player, across)) return 0;
        double offset = depth(state, pos, player);
        int direction = offset < 0 ? 1 : -1;
        double radius = player.getBbWidth() * .5;
        // Near the cabinet the bearing is the first surface the player reaches.
        double innerEdge = across - radius;
        double contact = innerEdge < 6.5 / 16 ? 1 / 16.0 : innerEdge < 7.25 / 16 ? .875 / 16 : ARM_THICKNESS;
        double clearance = Math.abs(offset) - radius - contact;
        // A movement update may reach the server just after the push request.
        double reach = movement == null ? MAX_APPROACH_CLEARANCE + SERVER_POSITION_TOLERANCE : MAX_APPROACH_CLEARANCE;
        if (clearance < -CONTACT_TOLERANCE || clearance > reach) return 0;
        if (movement == null) return direction;

        Direction facing = state.getValue(FACING);
        double forward = -(movement.x * facing.getStepX() + movement.z * facing.getStepZ()) * direction;
        return forward > 1e-4 && forward * APPROACH_LOOKAHEAD + CONTACT_TOLERANCE >= clearance ? direction : 0;
    }

    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof Player player) || !canPush(player) || !nearLane(state, pos, player))
            return;
        if (level.getBlockEntity(pos) instanceof OfficeTurnstileBlockEntity turnstile)
            turnstile.observePassage(player, depth(state, pos, player));
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof OfficeTurnstileBlockEntity turnstile)
            turnstile.finishOrWait(level);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
