package com.craftdistrict.menu;

import com.craftdistrict.block.SlidingEntranceBlock;
import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.registry.CraftDistrictDoors;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Native menu-button packets; no custom settings channel, storage slots or polling of unrelated blocks. */
public final class SlidingDoorMenu extends AbstractContainerMenu {
    private final Player owner;
    private final Level level;
    private final BlockPos root;
    private final InteractionHand hand;
    private final ItemStack wrench;
    private final boolean original;
    private final List<BlockPos> parts;
    private final List<BlockState> structure;
    private final DataSlot mode=DataSlot.standalone(),stale=DataSlot.standalone();
    public SlidingDoorMenu(int id,Inventory inventory,FriendlyByteBuf extra) {
        this(id,inventory,extra.readBlockPos(),extra.readBoolean()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,extra.readBoolean());
    }
    private SlidingDoorMenu(int id,Inventory inventory,BlockPos root,InteractionHand hand,boolean original) {
        super(CraftDistrictDoors.SETTINGS.get(),id);owner=inventory.player;level=owner.level();this.root=root.immutable();this.hand=hand;
        wrench=hand==null?ItemStack.EMPTY:owner.getItemInHand(hand);this.original=original;mode.set(original?1:0);
        BlockState state=level.getBlockState(root);
        parts=state.getBlock() instanceof SlidingEntranceBlock door?door.entranceParts(level,root):List.of();
        structure=parts.stream().map(level::getBlockState).toList();addDataSlot(mode);addDataSlot(stale);
    }
    private static boolean permitted(Level level,BlockPos root,Player player,InteractionHand hand) {
        if(player==null||player.level()!=level||!player.isAlive()||player.isSpectator()||!player.mayBuild()||hand==null
                ||!(player.getItemInHand(hand).getItem() instanceof WrenchItem)||!level.hasChunkAt(root)
                ||player.distanceToSqr(root.getX()+.5,root.getY()+1,root.getZ()+.5)>64)return false;
        if(!(level.getBlockState(root).getBlock() instanceof SlidingEntranceBlock door))return false;
        List<BlockPos> parts=door.entranceParts(level,root);if(parts.isEmpty())return false;
        for(BlockPos p:parts)if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!level.mayInteract(player,p))return false;
        return true;
    }
    public static InteractionResult open(Level level,BlockPos pos,Player player,InteractionHand hand) {
        if(!(level.getBlockState(pos).getBlock() instanceof SlidingEntranceBlock door))return InteractionResult.PASS;
        BlockPos root=door.controller(level,pos);
        if(!permitted(level,root,player,hand))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer server))return InteractionResult.PASS;
        boolean mode=level.getBlockState(root).getValue(SlidingEntranceBlock.AUTOMATIC);
        MenuRegistry.openExtendedMenu(server,new ExtendedMenuProvider() {
            @Override public Component getDisplayName(){return Component.translatable("screen.craftdistrict.sliding_door.title");}
            @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player ignored){return new SlidingDoorMenu(id,inventory,root,hand,mode);}
            @Override public void saveExtraData(FriendlyByteBuf buffer){buffer.writeBlockPos(root);buffer.writeBoolean(hand==InteractionHand.OFF_HAND);buffer.writeBoolean(mode);}
        });
        return InteractionResult.CONSUME;
    }
    public boolean automatic(){return mode.get()!=0;}
    public boolean stale(){return stale.get()!=0;}
    @Override public void broadcastChanges() {
        if(!level.isClientSide&&level.hasChunkAt(root)&&level.getBlockState(root).getBlock() instanceof SlidingEntranceBlock) {
            boolean current=level.getBlockState(root).getValue(SlidingEntranceBlock.AUTOMATIC);
            mode.set(current?1:0);if(current!=original)stale.set(1);
        }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) {
        if(player!=owner||hand==null||owner.getItemInHand(hand)!=wrench||wrench.isEmpty())return false;
        if(level.isClientSide)return true;
        if(!permitted(level,root,player,hand)||!(level.getBlockState(root).getBlock() instanceof SlidingEntranceBlock door)
                ||!parts.equals(door.entranceParts(level,root)))return false;
        for(int i=0;i<parts.size();i++) {
            BlockState a=structure.get(i),b=level.getBlockState(parts.get(i));
            if(a.getBlock()!=b.getBlock()||a.getValue(SlidingEntranceBlock.FACING)!=b.getValue(SlidingEntranceBlock.FACING)
                    ||a.getValue(SlidingEntranceBlock.HINGE)!=b.getValue(SlidingEntranceBlock.HINGE)
                    ||a.getValue(SlidingEntranceBlock.HALF)!=b.getValue(SlidingEntranceBlock.HALF)
                    ||a.getValue(SlidingEntranceBlock.PAIRED)!=b.getValue(SlidingEntranceBlock.PAIRED))return false;
        }
        return true;
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button<0||button>1||level.isClientSide||player.containerMenu!=this||!stillValid(player)||stale())return false;
        SlidingEntranceBlock door=(SlidingEntranceBlock)level.getBlockState(root).getBlock();
        for(BlockPos p:parts)if(level.getBlockState(p).getValue(SlidingEntranceBlock.AUTOMATIC)!=original){stale.set(1);broadcastChanges();return false;}
        boolean selected=button==1;
        if(selected!=original) {
            if(!door.configure(level,root,selected)){stale.set(1);broadcastChanges();return false;}
            WrenchItem.used(player,hand,wrench);
        }
        player.closeContainer();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
