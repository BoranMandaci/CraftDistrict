package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cached simple shapes, including four corner profiles; no geometry construction during targeting or collision. */
final class RestaurantSeatShapes {
    private static final VoxelShape[][][] SHAPES=build();
    private RestaurantSeatShapes() { }

    private static VoxelShape[][][] build() {
        var designs=RestaurantSeatBlock.Design.values();
        VoxelShape[][][] all=new VoxelShape[designs.length][][];
        for(var design:designs) {
            all[design.ordinal()]=new VoxelShape[design.connected()?ConnectedRestaurantSeatBlock.Connection.values().length:1][4];
            for(int c=0;c<all[design.ordinal()].length;c++) {
                double h=design.seatHeight*16;
                VoxelShape north;
                if(design.connected()) {
                    var conn=ConnectedRestaurantSeatBlock.Connection.values()[c];
                    var corner=conn.corner;
                    boolean bench=design==RestaurantSeatBlock.Design.BENCH;
                    if(bench) {
                        north=corner==ConnectedRestaurantSeatBlock.Corner.STRAIGHT ? Block.box(0,7.75,3,16,9,13) :
                            Shapes.or(Block.box(corner==ConnectedRestaurantSeatBlock.Corner.INNER_LEFT || corner==ConnectedRestaurantSeatBlock.Corner.OUTER_RIGHT ? 3:0,7.75,3,
                                               corner==ConnectedRestaurantSeatBlock.Corner.INNER_RIGHT || corner==ConnectedRestaurantSeatBlock.Corner.OUTER_LEFT ? 13:16,9,13),
                                      Block.box(3,7.75,corner==ConnectedRestaurantSeatBlock.Corner.INNER_LEFT || corner==ConnectedRestaurantSeatBlock.Corner.INNER_RIGHT ? 0:13,
                                                13,9,corner==ConnectedRestaurantSeatBlock.Corner.INNER_LEFT || corner==ConnectedRestaurantSeatBlock.Corner.INNER_RIGHT ? 3:16));
                        if(corner!=ConnectedRestaurantSeatBlock.Corner.STRAIGHT || conn.support)
                            north=Shapes.or(north,Block.box(7,0,4,9,7.75,12));
                    } else {
                        north=Shapes.or(Block.box(0,0,3,16,7.5,16),Block.box(0,7.5,0,16,h,16));
                        double x0=corner==ConnectedRestaurantSeatBlock.Corner.OUTER_RIGHT ? 12:0;
                        double x1=corner==ConnectedRestaurantSeatBlock.Corner.OUTER_LEFT ? 4:16;
                        north=Shapes.or(north,Block.box(x0,h,12,x1,18.5,16));
                        if(corner==ConnectedRestaurantSeatBlock.Corner.INNER_LEFT) north=Shapes.or(north,Block.box(0,h,0,4,18.5,12));
                        if(corner==ConnectedRestaurantSeatBlock.Corner.INNER_RIGHT) north=Shapes.or(north,Block.box(12,h,0,16,18.5,12));
                    }
                    VoxelShape left=bench ? Block.box(1.25,0,4,3.25,7.75,12) : Block.box(0,h,2,1,12,12);
                    VoxelShape right=bench ? Block.box(12.75,0,4,14.75,7.75,12) : Block.box(15,h,2,16,12,12);
                    int leftTurns=switch(corner) { case INNER_LEFT->1;case OUTER_RIGHT->3;default->0; };
                    int rightTurns=switch(corner) { case INNER_RIGHT->3;case OUTER_LEFT->1;default->0; };
                    if(!conn.left) north=Shapes.or(north,rotate(left,leftTurns));
                    if(!conn.right) north=Shapes.or(north,rotate(right,rightTurns));
                } else {
                    north=Block.box(2.5,h-1.5,2.5,13.5,h,13.5);
                    for(double x:new double[]{3,11}) for(double z:new double[]{3,11})
                        north=Shapes.or(north,Block.box(x,0,z,x+2,h-1.5,z+2));
                    if(design!=RestaurantSeatBlock.Design.BAR_STOOL) {
                        double top=switch(design) {case CUSHIONED->19.5; case BAR_CHAIR->23; default->18.5;};
                        north=Shapes.or(north,Block.box(2.75,h,11.5,13.25,top,13.5));
                    }
                }
                all[design.ordinal()][c][0]=north.optimize();
                for(int turn=1;turn<4;turn++) {
                    VoxelShape rotated=Shapes.empty();
                    for(AABB box:all[design.ordinal()][c][turn-1].toAabbs())
                        rotated=Shapes.or(rotated,Shapes.box(1-box.maxZ,box.minY,box.minX,1-box.minZ,box.maxY,box.maxX));
                    all[design.ordinal()][c][turn]=rotated.optimize();
                }
            }
        }
        return all;
    }

    private static VoxelShape rotate(VoxelShape shape, int turns) {
        for(int n=0;n<turns;n++) {
            VoxelShape next=Shapes.empty();
            for(AABB b:shape.toAabbs()) next=Shapes.or(next,Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));
            shape=next;
        }
        return shape;
    }

    static VoxelShape get(RestaurantSeatBlock.Design design, BlockState state) {
        int c=design.connected()?state.getValue(ConnectedRestaurantSeatBlock.CONNECTION).ordinal():0;
        int turn=switch(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        return SHAPES[design.ordinal()][c][turn];
    }
}
