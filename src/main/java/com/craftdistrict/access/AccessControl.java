package com.craftdistrict.access;

import com.craftdistrict.access.AccessControlData.Permission;
import com.craftdistrict.access.AccessControlData.Reader;
import com.craftdistrict.block.BarDoorBlock;
import com.craftdistrict.block.ModularMovingDoorBlock;
import com.craftdistrict.block.OfficeBadgeReaderBlock;
import com.craftdistrict.block.RotaryEntranceBlock;
import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.menu.CardReaderMenu;
import com.craftdistrict.network.AccessControlNetworking;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Interaction permissions always use the server player, the held item and the current reader record. */
public final class AccessControl {
    private static final int LINK_TICKS = 30 * 20, MANAGEMENT_TICKS = 60 * 20;
    private static final double DISTANCE_SQUARED = 8 * 8;
    private static final DustParticleOptions GRANTED = new DustParticleOptions(new Vector3f(.18F, 1, .42F), .65F);
    private static final DustParticleOptions REJECTED = new DustParticleOptions(new Vector3f(1, .17F, .21F), .65F);
    private static final Map<ServerPlayer, PlayerAccess> SESSIONS = Collections.synchronizedMap(new WeakHashMap<>());
    private static ClientSelection clientSelection;

    private record Grant(ServerLevel level, Reader reader, UUID card, long expires) { }
    private record Selection(ServerLevel level, Reader reader, long expires) { }
    private record ClientSelection(WeakReference<Player> player, ResourceKey<Level> dimension, long expires) { }
    private static final class PlayerAccess { Grant grant; Selection selection; }

    private AccessControl() { }

    public static void register() { PlayerEvent.PLAYER_QUIT.register(SESSIONS::remove); }

    private static PlayerAccess sessions(ServerPlayer player) { return SESSIONS.computeIfAbsent(player, ignored -> new PlayerAccess()); }

    public static void present(ServerPlayer player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        if (!player.isShiftKeyDown() || player.getItemInHand(hand) != stack || !(stack.getItem() instanceof AccessCardItem)
                || !readerAvailable(player, level, pos, true)) return;
        Reader reader = AccessControlData.get(level).reader(pos);
        UUID id = AccessCardItem.initialize(stack);
        String name = AccessCardItem.name(stack);
        Permission existing = reader.permission(id);
        if (existing != null) {
            reader.refreshName(id, name);
            if (existing == Permission.ADMINISTRATOR) {
                renewManagement(player, level, reader, id);
                CardReaderMenu.open(player, level, reader, hand, id);
            } else message(player, "already_registered");
            return;
        }
        Permission added = reader.enroll(id, name, level.getGameTime());
        if (added == Permission.ADMINISTRATOR) {
            renewManagement(player, level, reader, id);
            message(player, "first_admin");
        } else message(player, added == Permission.DOOR_ACCESS ? "registered" : "registration_closed");
    }

    public static void swipe(ServerPlayer player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        if (player.isShiftKeyDown() || player.getItemInHand(hand) != stack || !(stack.getItem() instanceof AccessCardItem)
                || !readerAvailable(player, level, pos, true)) return;
        var facing = level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
        Reader reader = AccessControlData.get(level).reader(pos);
        UUID card = AccessCardItem.initialize(stack);
        String result;
        if (!reader.enabled()) result = "reader_disabled";
        else if (!reader.authorized(card)) result = "access_denied";
        else {
            reader.refreshName(card, AccessCardItem.name(stack));
            result = AccessDoorSecurity.authorize(player, reader) ? "access_granted" : "door_not_ready";
        }
        if (!reader.feedbackReady(level.getGameTime())) return;
        var right = facing.getClockWise();
        boolean granted = result.equals("access_granted");
        double lens = granted ? -.0703125 : .0703125;
        double x = pos.getX() + .5 - facing.getStepX() * .34 + right.getStepX() * lens;
        double z = pos.getZ() + .5 - facing.getStepZ() * .34 + right.getStepZ() * lens;
        level.sendParticles(granted ? GRANTED : REJECTED, x, pos.getY() + .5625, z, 3, .012, .012, .012, 0);
        level.playSound(null, pos, (granted ? SoundEvents.NOTE_BLOCK_BIT : SoundEvents.NOTE_BLOCK_BASS).value(),
                SoundSource.BLOCKS, .55F, granted ? 1.5F : .65F);
        message(player, result);
    }

    public static boolean canManage(ServerPlayer player, ServerLevel level, Reader reader, InteractionHand hand, UUID card) {
        return readerAvailable(player, level, reader.pos(), true) && AccessControlData.get(level).existing(reader.pos()) == reader
                && card.equals(AccessCardItem.identity(player.getItemInHand(hand))) && reader.administrator(card);
    }

    public static void renewManagement(ServerPlayer player, ServerLevel level, Reader reader, UUID card) {
        sessions(player).grant = new Grant(level, reader, card, level.getGameTime() + MANAGEMENT_TICKS);
    }

    private static boolean readerAvailable(ServerPlayer player, ServerLevel level, BlockPos pos, boolean nearby) {
        if (player.serverLevel() != level || !player.isAlive() || player.isSpectator()
                || nearby && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > DISTANCE_SQUARED
                || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)) return false;
        BlockState state = AccessDoorTarget.loadedState(level, pos);
        return state != null && state.getBlock() instanceof OfficeBadgeReaderBlock;
    }

    private static boolean mayLink(ServerPlayer player, ServerLevel level, Reader reader, PlayerAccess access) {
        if (!readerAvailable(player, level, reader.pos(), false) || AccessControlData.get(level).existing(reader.pos()) != reader) return false;
        if (!reader.configured()) return true;
        Grant grant = access.grant;
        if (grant == null || grant.level() != level || grant.reader() != reader || grant.expires() <= level.getGameTime()
                || !reader.administrator(grant.card())) return false;
        return AccessCardItem.isCarriedBy(player, grant.card());
    }

    public static InteractionResult useWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        var block = state.getBlock();
        if (block instanceof OfficeBadgeReaderBlock) {
            if (player instanceof ServerPlayer server) select(server, context.getClickedPos());
            else if (player.level().isClientSide) receiveSelection(player, LINK_TICKS);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        boolean supported = AccessDoorTarget.supports(block);
        if (!supported && !(block instanceof DoorBlock || block instanceof BarDoorBlock || block instanceof RotaryEntranceBlock
                || block instanceof ModularMovingDoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock))
            return InteractionResult.PASS;
        if (player.level().isClientSide) return clientHasSelection(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        PlayerAccess access = SESSIONS.get(server);
        Selection selection = currentSelection(server);
        if (selection == null) return InteractionResult.PASS;
        if (!supported) return message(server, "unsupported_door");
        ServerLevel level = server.serverLevel();
        Reader reader = selection.reader();
        if (!mayLink(server, level, reader, access)) {
            setSelection(server, access, null);
            return message(server, "admin_required");
        }
        AccessDoorTarget.Resolution resolution = AccessDoorTarget.resolve(level, context.getClickedPos());
        AccessDoorTarget target = resolution.target();
        if (target == null) return message(server, resolution.unloaded() ? "door_unloaded" : "door_incomplete");
        if (target.parts().stream().noneMatch(pos -> pos.distSqr(reader.pos()) <= DISTANCE_SQUARED)) return message(server, "door_too_far");
        for (BlockPos part : target.parts()) {
            if (!level.getWorldBorder().isWithinBounds(part) || !level.mayInteract(server, part)) return message(server, "door_unavailable");
        }
        if (reader.setDoor(target)) WrenchItem.used(server, context.getHand(), context.getItemInHand());
        setSelection(server, access, null);
        return message(server, "door_linked");
    }

    private static void select(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        PlayerAccess access = sessions(player);
        Selection current = currentSelection(player);
        if (!readerAvailable(player, level, pos, true)) {
            sendSelection(player, current);
            return;
        }
        Reader reader = AccessControlData.get(level).reader(pos);
        if (current != null && current.reader() == reader) {
            setSelection(player, access, null);
            message(player, "selection_cancelled");
        } else if (mayLink(player, level, reader, access)) {
            setSelection(player, access, new Selection(level, reader, level.getGameTime() + LINK_TICKS));
            message(player, "reader_selected");
        } else {
            sendSelection(player, current);
            message(player, "admin_required");
        }
    }

    public static InteractionResult unsupportedWrenchTarget(Player player) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && currentSelection(server) != null) return message(server, "unsupported_door");
        return player.level().isClientSide && clientHasSelection(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static Selection currentSelection(ServerPlayer player) {
        PlayerAccess access = SESSIONS.get(player);
        Selection selection = access == null ? null : access.selection;
        if (selection == null) return null;
        if (selection.level() == player.serverLevel() && selection.expires() > player.level().getGameTime()) return selection;
        setSelection(player, access, null);
        return null;
    }

    private static void setSelection(ServerPlayer player, PlayerAccess access, Selection selection) {
        access.selection = selection;
        sendSelection(player, selection);
    }

    private static void sendSelection(ServerPlayer player, Selection selection) {
        AccessControlNetworking.sendSelection(player, selection == null ? 0 : (int) (selection.expires() - player.level().getGameTime()));
    }

    public static void receiveSelection(Player player, int ticks) {
        if (!player.level().isClientSide) return;
        clientSelection = ticks == 0 ? null : new ClientSelection(new WeakReference<>(player), player.level().dimension(), player.level().getGameTime() + ticks);
    }

    private static boolean clientHasSelection(Player player) {
        return clientSelection != null && clientSelection.player().get() == player && clientSelection.dimension() == player.level().dimension()
                && clientSelection.expires() > player.level().getGameTime();
    }

    private static InteractionResult message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.craftdistrict.access_control." + key), true);
        return InteractionResult.CONSUME;
    }
}
