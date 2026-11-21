package com.craftdistrict.menu;

import com.craftdistrict.access.AccessControl;
import com.craftdistrict.access.AccessControlData;
import com.craftdistrict.access.AccessControlData.Card;
import com.craftdistrict.access.AccessControlData.LinkStatus;
import com.craftdistrict.access.AccessControlData.Permission;
import com.craftdistrict.access.AccessControlData.Reader;
import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.network.AccessControlNetworking;
import com.craftdistrict.network.AccessControlNetworking.Action;
import com.craftdistrict.registry.CraftDistrictAccessControl;
import com.craftdistrict.registry.CraftDistrictBlocks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** The client sees only the current page. Each displayed row is bound to a server-side snapshot. */
public final class CardReaderMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 4;

    public record Row(String name, boolean administrator, boolean protectedCard) { }
    public record View(int version, int page, int pages, int count, boolean configured, boolean enabled, int seconds,
                       int enrollmentTicks, LinkStatus linkStatus, ResourceLocation door, List<Row> rows) {
        public static View read(FriendlyByteBuf buffer) {
            int version = buffer.readVarInt(), page = buffer.readVarInt(), pages = buffer.readVarInt(), count = buffer.readVarInt();
            boolean configured = buffer.readBoolean(), enabled = buffer.readBoolean();
            int duration = buffer.readUnsignedByte(), enrollment = buffer.readVarInt();
            LinkStatus status = buffer.readEnum(LinkStatus.class);
            ResourceLocation door = buffer.readBoolean() ? ResourceLocation.tryParse(buffer.readUtf(256)) : null;
            int rowCount = buffer.readUnsignedByte();
            if (pages < 1 || page < 0 || page >= pages || count < 0 || rowCount > PAGE_SIZE
                    || !AccessControlData.validDuration(duration) || enrollment < 0 || enrollment > AccessControlData.ENROLLMENT_TICKS)
                throw new IllegalArgumentException("Invalid card reader view");
            List<Row> rows = new ArrayList<>(rowCount);
            for (int i = 0; i < rowCount; i++) rows.add(new Row(buffer.readUtf(AccessCardItem.MAX_NAME), buffer.readBoolean(), buffer.readBoolean()));
            return new View(version, page, pages, count, configured, enabled, duration, enrollment, status, door, List.copyOf(rows));
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(version).writeVarInt(page).writeVarInt(pages).writeVarInt(count);
            buffer.writeBoolean(configured);
            buffer.writeBoolean(enabled);
            buffer.writeByte(seconds);
            buffer.writeVarInt(enrollmentTicks).writeEnum(linkStatus);
            buffer.writeBoolean(door != null);
            if (door != null) buffer.writeUtf(door.toString(), 256);
            buffer.writeByte(rows.size());
            for (Row row : rows) {
                buffer.writeUtf(row.name(), AccessCardItem.MAX_NAME);
                buffer.writeBoolean(row.administrator());
                buffer.writeBoolean(row.protectedCard());
            }
        }
    }

    private final Player owner;
    private final ServerLevel level;
    private final Reader reader;
    private final InteractionHand hand;
    private final UUID credential;
    private final long session;
    private View view;
    private List<Card> visibleCards;
    private long seenRevision, receivedAt;

    public CardReaderMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readLong(), View.read(buffer), List.of(), null, null, null, null);
    }

    private CardReaderMenu(int id, Inventory inventory, long session, View view, List<Card> cards, ServerLevel level,
                           Reader reader, InteractionHand hand, UUID credential) {
        super(CraftDistrictAccessControl.CARD_READER.get(), id);
        owner = inventory.player;
        this.session = session;
        this.level = level;
        this.reader = reader;
        this.hand = hand;
        this.credential = credential;
        visibleCards = cards;
        if (reader != null) seenRevision = reader.revision();
        receive(view);
    }

    public static void open(ServerPlayer player, ServerLevel level, Reader reader, InteractionHand hand, UUID credential) {
        if (!AccessControl.canManage(player, level, reader, hand, credential)) return;
        long session = ThreadLocalRandom.current().nextLong();
        List<Card> cards = reader.page(0, PAGE_SIZE);
        View initial = snapshot(reader, level, 0, 0, cards);
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override public Component getDisplayName() { return CraftDistrictBlocks.Office.EMPLOYEE_BADGE_READER.get().getName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new CardReaderMenu(id, inventory, session, initial, cards, level, reader, hand, credential);
            }
            @Override public void saveExtraData(FriendlyByteBuf buffer) { buffer.writeLong(session); initial.write(buffer); }
        });
    }

    private static int pageCount(Reader reader) { return Math.max(1, (reader.cardCount() - 1) / PAGE_SIZE + 1); }

    private static View snapshot(Reader reader, ServerLevel level, int version, int page, List<Card> cards) {
        LinkStatus status = reader.linkStatus(level);
        List<Row> rows = cards.stream().map(card -> {
            boolean admin = card.permission() == Permission.ADMINISTRATOR;
            return new Row(card.name(), admin, admin && reader.administratorCount() == 1);
        }).toList();
        return new View(version, page, pageCount(reader), reader.cardCount(), reader.configured(), reader.enabled(), reader.seconds(),
                reader.enrollmentRemaining(level.getGameTime()), status, reader.door() == null ? null : reader.door().blockId(), rows);
    }

    public long session() { return session; }
    public View view() { return view; }
    public int enrollmentTicks() {
        return (int) Math.max(0, view.enrollmentTicks() - Math.max(0, owner.level().getGameTime() - receivedAt));
    }

    public void receive(View view) {
        this.view = view;
        receivedAt = owner.level().getGameTime();
    }

    private void publish(ServerPlayer player, int requestedPage) {
        int page = Math.min(requestedPage, pageCount(reader) - 1);
        visibleCards = reader.page(page * PAGE_SIZE, PAGE_SIZE);
        view = snapshot(reader, level, view.version() + 1, page, visibleCards);
        seenRevision = reader.revision();
        AccessControlNetworking.sendView(player, this, view);
    }

    public void handle(ServerPlayer player, long submitted, int version, Action action, int value) {
        if (player.containerMenu != this || submitted != session || !stillValid(player)) return;
        int page = view.page();
        // An absolute duration edit does not depend on the displayed card page or reader revision.
        if (action == Action.DURATION || (version == view.version() && seenRevision == reader.revision())) {
            switch (action) {
                case PAGE -> { if (value >= 0 && value < view.pages()) page = value; }
                case ENROLLMENT -> { if (value == 0 || value == 1) reader.setEnrollment(value == 1, level.getGameTime()); }
                case ENABLED -> { if (value == 0 || value == 1) reader.setEnabled(value == 1); }
                case DURATION -> reader.setDuration(value);
                case UNLINK -> { if (value == 0) reader.setDoor(null); }
                case ADMINISTRATOR, DOOR_ACCESS, REMOVE -> updateCard(action, value);
                case SAVE_CARD -> { return; }
            }
            if (!stillValid(player)) { player.closeContainer(); return; }
            AccessControl.renewManagement(player, level, reader, credential);
        }
        publish(player, page);
    }

    private void updateCard(Action action, int index) {
        if (index < 0 || index >= visibleCards.size()) return;
        UUID id = visibleCards.get(index).id();
        if (action == Action.REMOVE) reader.removeCard(id);
        else reader.setPermission(id, action == Action.ADMINISTRATOR ? Permission.ADMINISTRATOR : Permission.DOOR_ACCESS);
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (reader != null && reader.revision() != seenRevision && owner instanceof ServerPlayer player
                && player.containerMenu == this && stillValid(player)) publish(player, view.page());
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && (player.level().isClientSide || player instanceof ServerPlayer server
                && reader != null && AccessControl.canManage(server, level, reader, hand, credential));
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
