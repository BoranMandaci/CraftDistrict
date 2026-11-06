package com.craftdistrict.item;

import com.craftdistrict.block.OverbedTableBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A non-consumable utility tool; block-specific mode changes stay server authoritative. */
public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        return state.getBlock() instanceof OverbedTableBlock table
                ? table.cycleDesign(state, context.getLevel(), context.getClickedPos(), context.getPlayer()) : InteractionResult.PASS;
    }
}
