package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static frame and cached client-rendered wings; the server synchronizes only real target changes. */
public final class BarDoorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN,REVERSED=BooleanProperty.create("reversed");
    public static final IntegerProperty SWING=IntegerProperty.create("swing",0,8);
    private static final int CLOSED=4,HOLD_TICKS=10;
    private static final VoxelShape[] POSTS=RestaurantKitchenBlock.rotations(Shapes.or(
            box(0,8,13,1,24,16),box(15,8,13,16,24,16)));
    private static final VoxelShape[][] SHAPES=new VoxelShape[9][],LEAVES=new VoxelShape[9][];
    static {
        for(int pose=0;pose<9;pose++) {
            double angle=Math.toRadians((pose-CLOSED)*22.5);
            VoxelShape wings=Shapes.or(wing(1,7.9,1,angle),wing(8.1,15,15,-angle)).optimize();
            LEAVES[pose]=RestaurantKitchenBlock.rotations(wings);
            SHAPES[pose]=RestaurantKitchenBlock.rotations(Shapes.or(POSTS[0],wings).optimize());
        }
    }
    private static VoxelShape wing(double from,double to,double hinge,double angle) {
        VoxelShape shape=Shapes.empty();double sin=Math.sin(angle),cos=Math.cos(angle);
        // Narrow slices follow the angled wing instead of selecting a large empty triangle.
        for(int i=0;i<8;i++) {
            double x0=from+(to-from)*i/8,x1=from+(to-from)*(i+1)/8;
            double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
            for(double x:new double[]{x0,x1})for(double z:new double[]{13.5,15}) {
                double rotatedX=hinge+(x-hinge)*cos-(z-14.25)*sin;
                double rotatedZ=14.25+(x-hinge)*sin+(z-14.25)*cos;
                minX=Math.min(minX,rotatedX);maxX=Math.max(maxX,rotatedX);
                minZ=Math.min(minZ,rotatedZ);maxZ=Math.max(maxZ,rotatedZ);
            }
            shape=Shapes.or(shape,box(minX,9.5,minZ,maxX,23,maxZ));
        }
        return shape;
    }
    //? if >=1.21 {
    public static final MapCodec<BarDoorBlock> CODEC=simpleCodec(BarDoorBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    //?}
    public BarDoorBlock(Properties properties) {
        super(properties);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH)
                .setValue(OPEN,false).setValue(REVERSED,false).setValue(SWING,CLOSED));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACING,OPEN,REVERSED,SWING);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BarDoorBlockEntity(pos,state);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        // One owning block with raised wings, not two copies of the saloon panels.
        BlockPos above=c.getClickedPos().above();
        if(c.getLevel().isOutsideBuildHeight(above)||!c.getLevel().getBlockState(above).isAir())return null;
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {
        int pose=s.getValue(SWING);
        if(l.getBlockEntity(p) instanceof BarDoorBlockEntity bar&&bar.getLevel()!=null)
            pose=net.minecraft.util.Mth.clamp(Math.round(bar.angle(bar.getLevel().getGameTime())/22.5F)+CLOSED,0,8);
        return SHAPES[pose][RestaurantKitchenBlock.turn(s)];
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {
        // The player pushes the spring-loaded wings instead of being stopped before entityInside.
        if(c instanceof EntityCollisionContext entity&&entity.getEntity() instanceof Player)return POSTS[RestaurantKitchenBlock.turn(s)];
        return getShape(s,l,p,c);
    }
    private AABB doorway(BlockState state,BlockPos pos) {
        return LEAVES[CLOSED][RestaurantKitchenBlock.turn(state)].bounds().move(pos).inflate(.2,0,.2);
    }
    private Player occupant(Level level,BlockPos pos,BlockState state) {
        int turn=RestaurantKitchenBlock.turn(state),end=state.getValue(REVERSED)?0:8;
        AABB swept=LEAVES[CLOSED][turn].bounds().minmax(LEAVES[end][turn].bounds())
                .minmax(LEAVES[state.getValue(SWING)][turn].bounds()).move(pos).inflate(.15,0,.15);
        var players=level.getEntitiesOfClass(Player.class,swept,p->!p.isSpectator());
        return players.isEmpty()?null:players.get(0);
    }
    private void schedule(Level level,BlockPos pos,int delay) {
        pos=pos.immutable();
        if(!level.isClientSide&&!level.getBlockTicks().hasScheduledTick(pos,this))level.scheduleTick(pos,this,delay);
    }
    private static int target(BlockState state){return state.getValue(OPEN)?state.getValue(REVERSED)?0:8:CLOSED;}
    private boolean write(Level level,BlockPos pos,BlockState state,BlockState next,Player player) {
        // Entity collision callbacks reuse a MutableBlockPos; queued packets/ticks must own a snapshot.
        pos=pos.immutable();
        if(level.isClientSide||!state.is(this)||level.getBlockState(pos)!=state||state==next)return false;
        // A single, unpredicted owner needs only vanilla's batched block update, not a bundled transaction.
        if(!level.setBlock(pos,next,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))return false;
        if(state.getValue(OPEN)!=next.getValue(OPEN)) {
            boolean open=next.getValue(OPEN);
            level.playSound(null,pos,open?SoundEvents.WOODEN_DOOR_OPEN:SoundEvents.WOODEN_DOOR_CLOSE,SoundSource.BLOCKS,.55F,1.2F);
            level.gameEvent(player,open?GameEvent.BLOCK_OPEN:GameEvent.BLOCK_CLOSE,pos);
        }
        return true;
    }
    private boolean request(Level level,BlockPos pos,BlockState state,boolean open,Player player) {
        BlockState next=state.setValue(OPEN,open);
        if(open&&player!=null&&!state.getValue(OPEN)) {
            Direction front=state.getValue(FACING);
            AABB plane=LEAVES[CLOSED][RestaurantKitchenBlock.turn(state)].bounds().move(pos);
            double side=(player.getX()-(plane.minX+plane.maxX)/2)*front.getStepX()
                    +(player.getZ()-(plane.minZ+plane.maxZ)/2)*front.getStepZ();
            next=next.setValue(REVERSED,side<0);
        }
        next=next.setValue(SWING,target(next));
        if(!(level.getBlockEntity(pos) instanceof BarDoorBlockEntity bar))return false;
        float from=bar.angle(level.getGameTime());
        if(!write(level,pos,state,next,player))return false;
        bar.begin(from,(next.getValue(SWING)-CLOSED)*22.5F,level.getGameTime());
        level.sendBlockUpdated(pos.immutable(),next,next,Block.UPDATE_CLIENTS);
        schedule(level,pos,open?BarDoorBlockEntity.DURATION:2);
        return true;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide&&entity instanceof Player player&&!player.isSpectator()
                &&player.getBoundingBox().intersects(doorway(state,pos))) {
            if(!state.getValue(OPEN))request(level,pos,state,true,player);
            else {
                int remaining=level.getBlockEntity(pos) instanceof BarDoorBlockEntity bar?bar.remaining(level.getGameTime()):0;
                schedule(level,pos,remaining>0?remaining:HOLD_TICKS);
            }
        }
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        int remaining=level.getBlockEntity(pos) instanceof BarDoorBlockEntity bar?bar.remaining(level.getGameTime()):0;
        if(!state.getValue(OPEN)) {
            if(remaining==0&&state.getValue(SWING)==CLOSED)return;
            Player player=occupant(level,pos,state);
            if(player!=null){request(level,pos,state,true,player);return;}
        }
        if(remaining>0){schedule(level,pos,state.getValue(OPEN)?remaining:Math.min(2,remaining));return;}
        if(state.getValue(SWING)!=target(state)) {
            // Resume a resting pose saved by the earlier stepped implementation.
            request(level,pos,state,state.getValue(OPEN),null);
        } else if(state.getValue(OPEN)) {
            // Only a used doorway gets this bounded occupancy check; no idle/world-wide polling.
            if(occupant(level,pos,state)!=null)schedule(level,pos,HOLD_TICKS);
            else request(level,pos,state,false,null);
        }
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState before,boolean moving) {
        super.onPlace(state,level,pos,before,moving);
        if(!before.is(this)&&(state.getValue(OPEN)||state.getValue(SWING)!=CLOSED))schedule(level,pos,2);
    }
}
