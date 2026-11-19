package com.craftdistrict.client;

import com.craftdistrict.menu.SlidingDoorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SlidingDoorScreen extends AbstractContainerScreen<SlidingDoorMenu> {
    private boolean automatic,pending;
    private Button manual,auto,done;
    public SlidingDoorScreen(SlidingDoorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);automatic=menu.automatic();imageWidth=248;imageHeight=158;}
    private static Component text(String key){return Component.translatable("screen.craftdistrict.sliding_door."+key);}
    @Override protected void init() {
        super.init();
        manual=addRenderableWidget(new ModeButton(leftPos+14,topPos+48,false));
        auto=addRenderableWidget(new ModeButton(leftPos+128,topPos+48,true));
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL,b->onClose()).bounds(leftPos+14,topPos+124,106,20).build());
        done=addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,b->{
            if(pending||menu.stale()||minecraft==null||minecraft.gameMode==null)return;
            pending=true;manual.active=auto.active=done.active=false;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,automatic?1:0);
        }).bounds(leftPos+128,topPos+124,106,20).build());
    }
    @Override protected void containerTick(){super.containerTick();manual.active=auto.active=done.active=!pending&&!menu.stale();}
    @Override protected void renderBg(GuiGraphics g,float delta,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff405461);g.fill(leftPos+1,topPos+1,leftPos+imageWidth-1,topPos+imageHeight-1,0xff17212a);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {
        g.drawCenteredString(font,title,imageWidth/2,13,0xffeff5f5);
        g.drawCenteredString(font,Component.translatable("screen.craftdistrict.sliding_door.current",text(menu.automatic()?"automatic":"manual")),imageWidth/2,30,0xff99b5bd);
        var lines=font.split(text(menu.stale()?"stale":automatic?"automatic_hint":"manual_hint"),imageWidth-28);
        for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(font,lines.get(i),14,82+i*10,menu.stale()?0xffffb0a0:0xffb3c6cb,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta) {
        //? if <1.21 {
        /*renderBackground(g);
        *///?}
        super.render(g,x,y,delta);
    }
    private final class ModeButton extends Button {
        private final boolean choice;
        ModeButton(int x,int y,boolean choice){super(x,y,106,24,text(choice?"automatic":"manual"),b->automatic=choice,DEFAULT_NARRATION);this.choice=choice;}
        @Override protected void renderWidget(GuiGraphics g,int x,int y,float delta) {
            boolean selected=automatic==choice;
            g.fill(getX(),getY(),getX()+width,getY()+height,selected?0xff7dd8ca:0xff405461);
            g.fill(getX()+1,getY()+1,getX()+width-1,getY()+height-1,selected?0xff234a4d:isHoveredOrFocused()?0xff304652:0xff253540);
            renderScrollingString(g,font,8,active?0xffedf4f4:0xff738c97);
        }
    }
}
