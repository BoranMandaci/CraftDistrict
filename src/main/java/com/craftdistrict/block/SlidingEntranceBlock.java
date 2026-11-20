package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.menu.SlidingDoorMenu;
import com.craftdistrict.door.SlidingDoorSensors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Standard two-half ownership/pairing, with translating leaves and outer jambs only. */
public final class SlidingEntranceBlock extends PairedDoorBlock implements EntityBlock {
    public static final BooleanProperty AUTOMATIC=BooleanProperty.create("automatic");
    public enum Design implements StringRepresentable {
        GLASS, HOSPITAL;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final int SHAPE_STEPS=32;
    private static final VoxelShape[][][][][] SHAPES = new VoxelShape[2][2][2][SHAPE_STEPS+1][];
    private static final VoxelShape[][][][][] LEAVES = new VoxelShape[2][2][2][SHAPE_STEPS+1][];
    static {
        for (int paired=0; paired<2; paired++) for (int upper=0; upper<2; upper++)
            for (int right=0; right<2; right++) {
                double x0 = paired == 1 && right == 1 ? 0 : .65;
                double x1 = paired == 1 && right == 0 ? 16 : 15.35;
                VoxelShape frame = paired==0 ? Shapes.or(box(0,0,0,.65,16,1.75),box(15.35,0,0,16,16,1.75))
                        : box(right==0?0:15.35,0,0,right==0?.65:16,16,1.75);
                if (upper==1) frame=Shapes.or(frame,box(0,14.25,-1.75,16,16,1.75));
                for(int step=0;step<=SHAPE_STEPS;step++) {
                    double progress=(double)step/SHAPE_STEPS,shift=(right==0?-16:16)*progress,depth=-1.75*progress;
                    VoxelShape leaf=box(x0+shift,upper==0?.6:0,.25+depth,x1+shift,upper==0?16:14.25,1.5+depth);
                    LEAVES[paired][upper][right][step]=RestaurantKitchenBlock.rotations(leaf);
                    SHAPES[paired][upper][right][step]=RestaurantKitchenBlock.rotations(Shapes.or(frame,leaf).optimize());
                }
            }
    }
    //? if >=1.21 {
    public static final MapCodec<SlidingEntranceBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((SlidingEntranceBlock b)->b.design),
            propertiesCodec()).apply(instance,SlidingEntranceBlock::new));
    @Override public MapCodec<? extends DoorBlock> codec() { return CODEC; }
    //?}
    public SlidingEntranceBlock(Design design, Properties properties) {
        super(BlockSetType.IRON,properties,1.5,true);this.design=design;
        registerDefaultState(defaultBlockState().setValue(AUTOMATIC,false));
    }
    public Design design(){return design;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?new SlidingDoorBlockEntity(pos,state):null;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(AUTOMATIC);}
    @Override protected BlockState pairedSettings(BlockState own,BlockState other){return own.setValue(AUTOMATIC,own.getValue(AUTOMATIC)||other.getValue(AUTOMATIC));}
    @Override protected boolean redstoneOpen(Level level,BlockPos root,boolean powered) {
        BlockState state=level.getBlockState(root);
        return powered||state.getValue(AUTOMATIC)&&state.getValue(OPEN);
    }
    @Override protected void playDoorSounds(Entity source,Level level,BlockPos[] positions,BlockState[] before,boolean open) {
        boolean changed=false;
        for(int i=0;i<positions.length;i+=2)changed|=before[i].getValue(OPEN)!=open;
        if(!changed)return;
        boolean automatic=before[0].getValue(AUTOMATIC);
        if(automatic&&level.isClientSide)return;
        double x=positions[0].getX()+.5,z=positions[0].getZ()+.5;
        if(positions.length==4){x=(positions[0].getX()+positions[2].getX())*.5+.5;z=(positions[0].getZ()+positions[2].getZ())*.5+.5;}
        // One short, subdued motor stroke for the whole entrance, retaining vanilla predicted-player sound ownership.
        level.playSound(!automatic&&source instanceof Player player?player:null,x,positions[0].getY()+1,z,
                open?SoundEvents.PISTON_EXTEND:SoundEvents.PISTON_CONTRACT,SoundSource.BLOCKS,
                design==Design.GLASS?.16F:.22F,design==Design.GLASS?(open?1.55F:1.45F):(open?1.2F:1.08F));
    }
    public List<BlockPos> entranceParts(Level level,BlockPos clicked) {
        BlockState state=level.getBlockState(clicked);
        if(!state.is(this))return List.of();
        BlockPos root=bottom(state,clicked);
        if(!complete(level,root))return List.of();
        BlockState lower=level.getBlockState(root);BlockPos other=partner(level,root,lower);
        if(lower.getValue(PAIRED)&&other==null)return List.of();
        return other==null?List.of(root,root.above()):List.of(root,root.above(),other,other.above());
    }
    public BlockPos controller(Level level,BlockPos pos) {
        BlockState state=level.getBlockState(pos);if(!state.is(this))return pos.immutable();
        BlockPos root=bottom(state,pos);if(!complete(level,root))return root;
        BlockState lower=level.getBlockState(root);BlockPos other=partner(level,root,lower);
        return other!=null&&lower.getValue(HINGE)==DoorHingeSide.RIGHT?other:root;
    }
    public AABB entranceArea(Level level,BlockPos root,double range) {
        BlockState state=level.getBlockState(root);BlockPos other=partner(level,root,state);
        double x=root.getX()+.5,z=root.getZ()+.5;
        if(other!=null){x=(x+other.getX()+.5)*.5;z=(z+other.getZ()+.5)*.5;}
        Direction f=state.getValue(FACING);x+=f.getStepX()*.4453125;z+=f.getStepZ()*.4453125;
        return new AABB(x-range,root.getY(),z-range,x+range,root.getY()+2,z+range);
    }
    public boolean near(Level level,BlockPos root,Player player,double range) {
        AABB area=entranceArea(level,root,range);
        double dx=player.getX()-(area.minX+area.maxX)*.5,dz=player.getZ()-(area.minZ+area.maxZ)*.5;
        return !player.isSpectator()&&player.isAlive()&&player.getBoundingBox().maxY>area.minY
                &&player.getBoundingBox().minY<area.maxY&&dx*dx+dz*dz<=range*range;
    }
    public boolean occupied(Level level,BlockPos root) {
        return !level.getEntitiesOfClass(Player.class,entranceArea(level,root,3),p->near(level,root,p,3)).isEmpty();
    }
    public boolean configure(Level level,BlockPos pos,boolean automatic) {
        if(!(level instanceof ServerLevel))return false;
        List<BlockPos> parts=entranceParts(level,pos);if(parts.isEmpty())return false;
        BlockPos[] positions=parts.toArray(BlockPos[]::new);BlockState[] before=new BlockState[positions.length],after=before.clone();
        for(int i=0;i<positions.length;i++){before[i]=level.getBlockState(positions[i]);after[i]=before[i].setValue(AUTOMATIC,automatic);}
        if(!DoorStateUpdates.commit(level,positions,before,after))return false;
        for(BlockPos p:positions)if(level instanceof ServerLevel server&&level.getBlockState(p).getValue(HALF)==DoubleBlockHalf.LOWER)
            SlidingDoorSensors.changed(server,p);
        doorChanged(level,positions[0]);return true;
    }
    @Override protected void doorChanged(Level level,BlockPos root) {
        if(!(level instanceof ServerLevel server))return;
        for(BlockPos part:entranceParts(level,root))if(level.getBlockState(part).getValue(HALF)==DoubleBlockHalf.LOWER)SlidingDoorSensors.changed(server,part);
        BlockState state=level.getBlockState(root);
        if(state.is(this)&&state.getValue(AUTOMATIC)&&state.getValue(OPEN))SlidingDoorSensors.arm(server,controller(level,root));
        synchronizeMotion(server,root);
    }
    private void synchronizeMotion(ServerLevel level,BlockPos clicked) {
        List<BlockPos> parts=entranceParts(level,clicked);if(parts.isEmpty())return;
        BlockPos root=controller(level,clicked);
        if(!(level.getBlockEntity(root) instanceof SlidingDoorBlockEntity motion))return;
        boolean changed=motion.transition(level.getBlockState(root).getValue(OPEN),level.getGameTime());
        for(BlockPos part:parts)if(level.getBlockState(part).getValue(HALF)==DoubleBlockHalf.LOWER) {
            boolean updated=part.equals(root)?changed:level.getBlockEntity(part) instanceof SlidingDoorBlockEntity peer&&peer.copy(motion);
            if(updated){BlockState state=level.getBlockState(part);level.sendBlockUpdated(part,state,state,Block.UPDATE_CLIENTS);}
        }
        if(!level.getBlockState(root).getValue(OPEN)&&motion.moving(level.getGameTime()))SlidingDoorSensors.watchClosing(level,root);
    }
    private static SlidingDoorBlockEntity motion(BlockGetter level,BlockPos pos,BlockState state) {
        BlockPos lower=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;
        return level.getBlockEntity(lower) instanceof SlidingDoorBlockEntity motion?motion:null;
    }
    private static float progress(BlockGetter level,BlockPos pos,BlockState state) {
        SlidingDoorBlockEntity motion=motion(level,pos,state);
        return motion!=null&&motion.getLevel()!=null?motion.progress(motion.getLevel().getGameTime()):state.getValue(OPEN)?1:0;
    }
    private static int step(float progress){return net.minecraft.util.Mth.clamp(Math.round(progress*SHAPE_STEPS),0,SHAPE_STEPS);}
    private static VoxelShape leaf(BlockState state,int step) {
        return LEAVES[state.getValue(PAIRED)?1:0][state.getValue(HALF)==DoubleBlockHalf.UPPER?1:0]
                [state.getValue(HINGE)==DoorHingeSide.RIGHT?1:0][step][RestaurantKitchenBlock.turn(state)];
    }
    /** Only active closing entrances perform this small swept-doorway safety check. */
    public boolean guardClosing(ServerLevel level,BlockPos root) {
        BlockState state=level.getBlockState(root);
        if(!state.is(this)||state.getValue(OPEN))return false;
        List<BlockPos> parts=entranceParts(level,root);if(parts.isEmpty())return false;
        SlidingDoorBlockEntity motion=motion(level,root,state);if(motion==null)return false;
        for(BlockPos part:parts)if(level.getBlockState(part).getValue(HALF)==DoubleBlockHalf.LOWER) {
            BlockState lower=level.getBlockState(part);
            AABB swept=leaf(lower,0).bounds().minmax(leaf(lower,step(progress(level,part,lower))).bounds())
                    .move(part).expandTowards(0,1,0).inflate(.08,0,.08);
            if(!level.getEntitiesOfClass(Player.class,swept,p->!p.isSpectator()&&p.isAlive()).isEmpty()) {
                setOpen(null,level,state,root,true);return false;
            }
        }
        return motion.moving(level.getGameTime());
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);doorChanged(level,pos);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        super.neighborChanged(state,level,pos,block,from,moving);
        if(!level.isClientSide&&level.getBlockState(pos).is(this))doorChanged(level,bottom(level.getBlockState(pos),pos));
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(level instanceof ServerLevel server&&state.getValue(HALF)==DoubleBlockHalf.LOWER&&!next.is(this))SlidingDoorSensors.removed(server,pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){SlidingDoorSensors.closeTick(level,this,bottom(state,pos));}
    private InteractionResult interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand) {
        if(WrenchItem.isHeldBy(player))return SlidingDoorMenu.open(level,pos,player,WrenchItem.hand(player));
        if(entranceParts(level,pos).isEmpty())return InteractionResult.PASS;
        BlockPos root=bottom(state,pos);
        // Proximity can depend on another player's server position; do not predict a rejected close locally.
        if(state.getValue(AUTOMATIC)&&level.isClientSide)return InteractionResult.SUCCESS;
        if(state.getValue(AUTOMATIC)&&state.getValue(OPEN)&&occupied(level,controller(level,root)))return InteractionResult.sidedSuccess(level.isClientSide);
        //? if >=1.21 {
        return super.useWithoutItem(state,level,pos,player,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        //?} else {
        /*return super.use(state,level,pos,player,hand,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        *///?}
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player u,BlockHitResult h){return interact(s,l,p,u,InteractionHand.MAIN_HAND);}
    //?} else {
    /*@Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player u,InteractionHand hand,BlockHitResult h){return interact(s,l,p,u,hand);}
    *///?}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state=super.getStateForPlacement(context);
        return state==null?null:state.setValue(FACING,state.getValue(FACING).getOpposite());
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PAIRED)?1:0][state.getValue(HALF)==DoubleBlockHalf.UPPER?1:0]
                [state.getValue(HINGE)==DoorHingeSide.RIGHT?1:0][step(progress(level,pos,state))][RestaurantKitchenBlock.turn(state)];
    }
    /** Visit only cells along the cursor ray and their immediate neighbors, never a world-wide door list. */
    public static BlockHitResult pickMovedPanel(BlockGetter level,Vec3 start,Vec3 end,double nearestSquared) {
        double lengthSquared=start.distanceToSqr(end);
        if(lengthSquared<1e-12||nearestSquared<1e-12)return null;
        Vec3 scanEnd=nearestSquared<lengthSquared?start.lerp(end,Math.sqrt(nearestSquared/lengthSquared)):end;
        if (level instanceof Level world && !mayHaveSlidingDoor(world, start, scanEnd)) return null;

        final class Ray {
            final LongOpenHashSet visited=new LongOpenHashSet();
            final BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();
            BlockHitResult hit;
            double nearest=nearestSquared;
            void visit(BlockPos cell) {
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
                    cursor.set(cell.getX()+x,cell.getY(),cell.getZ()+z);
                    if(!visited.add(cursor.asLong()))continue;
                    BlockState state=level.getBlockState(cursor);
                    if(!(state.getBlock() instanceof SlidingEntranceBlock))continue;
                    VoxelShape leaf=leaf(state,step(progress(level,cursor,state)));
                    // BlockHitResult is queued for interaction: never retain the traversal's mutable cursor.
                    BlockHitResult candidate=leaf.clip(start,end,cursor.immutable());
                    if(candidate==null)continue;
                    double distance=start.distanceToSqr(candidate.getLocation());
                    if(distance<nearest-1e-8){nearest=distance;hit=candidate;}
                }
            }
        }
        Ray ray=new Ray();
        return BlockGetter.traverseBlocks(start,scanEnd,ray,(query,pos)->{query.visit(pos);return null;},query->query.hit);
    }

    private static boolean mayHaveSlidingDoor(Level level, Vec3 start, Vec3 end) {
        // Include the ray traversal's endpoint nudge and the neighboring cells a leaf can slide into.
        double padding = 1e-5;
        int minX = SectionPos.posToSectionCoord(Math.min(start.x, end.x) - 1 - padding);
        int maxX = SectionPos.posToSectionCoord(Math.max(start.x, end.x) + 1 + padding);
        int minY = SectionPos.posToSectionCoord(Math.min(start.y, end.y) - padding);
        int maxY = SectionPos.posToSectionCoord(Math.max(start.y, end.y) + padding);
        int minZ = SectionPos.posToSectionCoord(Math.min(start.z, end.z) - 1 - padding);
        int maxZ = SectionPos.posToSectionCoord(Math.max(start.z, end.z) + 1 + padding);
        // Bound the palette check to eight sections even with extended interaction reach.
        if (maxX - minX > 1 || maxY - minY > 1 || maxZ - minZ > 1) return true;

        int firstSectionIndex = level.getSectionIndexFromSectionY(Math.max(minY, level.getMinSection()));
        int lastSectionIndex = level.getSectionIndexFromSectionY(Math.min(maxY, level.getMaxSection() - 1));
        if (firstSectionIndex > lastSectionIndex) return false;
        var chunks = level.getChunkSource();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = chunks.getChunkNow(x, z);
                if (chunk == null) continue;
                var sections = chunk.getSections();
                for (int y = firstSectionIndex; y <= lastSectionIndex; y++) {
                    var section = sections[y];
                    if (!section.hasOnlyAir()
                            && section.maybeHas(state -> state.getBlock() instanceof SlidingEntranceBlock)) return true;
                }
            }
        }
        return false;
    }

    /** Permit hits on the actual moved leaf; vanilla distance and permission checks still apply. */
    public static double interactionHitLimit(BlockGetter level, BlockHitResult hit, double original) {
        BlockPos pos=hit.getBlockPos(); BlockState state=level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlidingEntranceBlock)) return original;
        Vec3 point=hit.getLocation().subtract(pos.getX(),pos.getY(),pos.getZ());
        Direction.Axis axis=hit.getDirection().getAxis(); double epsilon=1e-7;
        for (var box:state.getShape(level,pos).toAabbs()) {
            double edge=hit.getDirection().getAxisDirection()==Direction.AxisDirection.POSITIVE?box.max(axis):box.min(axis);
            if (!box.inflate(epsilon).contains(point) || !hit.isInside()&&Math.abs(point.get(axis)-edge)>epsilon) continue;
            double limit=original;
            for (Direction.Axis coordinate:Direction.Axis.values())
                limit=Math.max(limit,Math.max(Math.abs(box.min(coordinate)-.5),Math.abs(box.max(coordinate)-.5))+epsilon);
            return limit;
        }
        // A click can arrive a few ticks after the panel was picked. Accept only the real slide corridor,
        // for the short transition/latency window; vanilla reach, permissions and occlusion remain intact.
        SlidingDoorBlockEntity motion=motion(level,pos,state);
        if(motion!=null&&motion.getLevel()!=null&&motion.recent(motion.getLevel().getGameTime()))
            for(int step=0;step<=SHAPE_STEPS;step++)for(var box:leaf(state,step).toAabbs())
                if(box.inflate(epsilon).contains(point)) {
                    double limit=original;
                    for(Direction.Axis coordinate:Direction.Axis.values())
                        limit=Math.max(limit,Math.max(Math.abs(box.min(coordinate)-.5),Math.abs(box.max(coordinate)-.5))+epsilon);
                    return limit;
                }
        return original;
    }
}
