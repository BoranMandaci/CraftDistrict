package com.craftdistrict.item;

import com.craftdistrict.block.OverbedTableBlock;
import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.ControlledLight;
import com.craftdistrict.block.KitchenExhaustDuctBlock;
import com.craftdistrict.block.ModularDiningTableBlock;
import com.craftdistrict.block.ConferenceTableBlock;
import com.craftdistrict.block.PoliceWaitingBenchBlock;
import com.craftdistrict.block.SittableBlock;
import com.craftdistrict.block.LyingBlock;
import com.craftdistrict.lighting.LightControls;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** A non-consumable utility tool; block-specific mode changes stay server authoritative. */
public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) { super(properties); }
    public static boolean isHeldBy(Player player) {
        return player != null && (player.getMainHandItem().getItem() instanceof WrenchItem
                || player.getOffhandItem().getItem() instanceof WrenchItem);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (block instanceof ModularDiningTableBlock table) return table.toggleSupport(state, level, pos, player);
        if (block instanceof ConferenceTableBlock table) return table.toggleSupport(state, level, pos, player);
        if (block instanceof PoliceWaitingBenchBlock bench) return bench.toggleSupport(state, level, pos, player);
        if (block instanceof KitchenExhaustDuctBlock duct)
            return duct.toggleConnection(state, level, pos, player, context.getClickedFace(), context.getClickLocation());
        if (block instanceof OverbedTableBlock table) return table.cycleDesign(state, level, pos, player);
        if (block instanceof ControlledLight) return LightControls.use(state, level, pos, player, context.getHand());
        // Invoke only existing open/close actions, never general furniture, beds, or GUIs.
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !CraftDistrict.MOD_ID.equals(id.getNamespace()) || !state.hasProperty(BlockStateProperties.OPEN)
                || block instanceof SittableBlock || block instanceof LyingBlock || block instanceof BedBlock)
            return InteractionResult.PASS;
        var hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
        //? if >=1.21 {
        return state.useWithoutItem(level, player, hit);
        //?} else {
        /*return state.use(level, player, context.getHand(), hit);
        *///?}
    }
}
