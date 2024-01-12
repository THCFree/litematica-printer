package me.aleksilassila.litematica.printer.v1_20;

import me.aleksilassila.litematica.printer.v1_20.config.PrinterConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;

public class InventoryManager {
    int delay = 0;
    int serverResponseDelay = 0;
    /**
     * The queue of items to pull from the inventory
     */
    private final ArrayDeque<Item> pullQueue = new ArrayDeque<>();
    private final ArrayList<SlotInfo> hotbarSlots = new ArrayList<>(9);

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ArrayDeque<Integer> rollingSlots = new ArrayDeque<>();
    private static InventoryManager instance;
    private InventoryManager() {
        // Probably add a way to configure this
        for (int i = 3; i < 9; i++) {
            rollingSlots.add(i);
        }
        for (int i = 0; i < 9; i++) {
            hotbarSlots.add(new SlotInfo());
        }
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public void reset() {
        pullQueue.clear();
        hotbarSlots.forEach((info) -> info.ticksLocked = 0);
    }

    public boolean tick() {
        if (mc.player == null) {
            return false;
        }
        PlayerInventory inventory = mc.player.getInventory();
        for (int i = 0; i < hotbarSlots.size(); i++) {
            SlotInfo info = hotbarSlots.get(i);
            info.ticksLocked = Math.max(0, info.ticksLocked - 1);
            if (info.ticksLocked == 0) {
                if (info.waitingForItem != null && PrinterConfig.INVENTORY_NO_ASYNC.getBooleanValue()) {
                    delay = PrinterConfig.INVENTORY_DELAY.getIntegerValue();
                }
                info.waitingForItem = null;
            }
            if (info.waitingForItem == inventory.getStack(i).getItem()) {
                info.ticksLocked = Math.min(2, info.ticksLocked);
            }
        }

        delay = Math.max(0, delay - 1);

        if (!pullQueue.isEmpty() && delay == 0 && mc.player != null && mc.interactionManager != null && mc.getNetworkHandler() != null) {
            if (PrinterConfig.INVENTORY_NO_ASYNC.getBooleanValue()) {
                if (hotbarSlots.stream().anyMatch((info) -> info.ticksLocked > 0)) {
                    return false;
                }
            }
            Item item = pullQueue.poll();
            if (getHotbarSlotWithItem(mc.player, new ItemStack(item)) != -1) {
                return true;
            }
            int slot = getBestInventorySlotWithItem(mc.player, new ItemStack(item));
            int nextSlot = nextHotbarSlot();
            hotbarSlots.get(nextSlot).addTicksLocked(10);
            hotbarSlots.get(nextSlot).waitingForItem = item;
            // int currentHotbarSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = nextSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(nextSlot));
            System.out.println("Picking item from inventory: " + slot + " -> " + nextSlot);
            mc.interactionManager.pickFromInventory(slot);
            delay += PrinterConfig.INVENTORY_DELAY.getIntegerValue();
            return true;
            // mc.player.getInventory().selectedSlot = currentHotbarSlot;
        }
        return false;
    }

    private boolean requestStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!canRequest(stack)) {
            return false;
        }
        pullQueue.addLast(stack.getItem());

        return true;
    }

    public boolean select(ItemStack itemStack) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) {
            return false;
        }
        if (itemStack != null) {
            PlayerInventory inventory = player.getInventory();

            if (PrinterConfig.INVENTORY_PAUSE_PLACEMENT.getBooleanValue()) {
                if (hotbarSlots.stream().anyMatch((slotInfo) -> slotInfo.waitingForItem != null)) {
                    return false;
                }
            }

            // This thing is straight from MinecraftClient#doItemPick()
            if (player.getAbilities().creativeMode) {
                inventory.addPickBlock(itemStack);
                mc.interactionManager.clickCreativeStack(player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
                return true;
            } else {
                int hotbarSlot = getHotbarSlotWithItem(player, itemStack);
                if (hotbarSlot != -1 && InventoryManager.getInstance().isSlotFree(hotbarSlot)) {
                    System.out.println("Selecting slot " + (hotbarSlot));
                    if (hotbarSlot < 0 || hotbarSlot > 8) {
                        Error trace = new Error();
                        System.out.println("Inventory slot out of bounds: " + hotbarSlot);
                        trace.printStackTrace();
                        return false;
                    }
                    player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                    inventory.selectedSlot = hotbarSlot;
                    return true;
                } else {
                    requestStack(itemStack);
                    return false;
                }
            }
        }
        return false;
    }

    public boolean canRequest(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (hotbarSlots.stream().anyMatch((slotInfo) -> slotInfo.waitingForItem == item)) {
            return false;
        }
        return !pullQueue.contains(item);
    }

    public boolean canSelect(ItemStack itemStack) {
        if (mc.player == null) return false;
        return getHotbarSlotWithItem(mc.player, itemStack) != -1;
    }

    public void onPacketReceived(Packet<?> packet, CallbackInfo callbackInfo) {
        if (packet instanceof UpdateSelectedSlotC2SPacket packet1) {
            if (LitematicaMixinMod.DEBUG) mc.inGameHud.getChatHud().addMessage(Text.literal("[Printer] Server selected slot: " + packet1.getSelectedSlot()));
        } else if (packet instanceof ScreenHandlerSlotUpdateS2CPacket packet1) {
            if (mc.player != null && packet1.getSyncId() == mc.player.currentScreenHandler.syncId) {
                serverResponseDelay = 0;
            }
        }
    }

    /**
     * Check slots 0-8 if they are free to use
     * @param slot The slot to check
     * @return If the slot is free
     */
    public boolean isSlotFree(int slot) {
        return hotbarSlots.get(slot).isFree();
    }

    /**
     * Returns a list of empty hotbar slots that are not locked. Range from 0-8
     * @return A list of empty hotbar slots. Range from 0-8
     */
    private List<Integer> emptyValidHotbarSlots() {
        if (mc.player == null) return new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().main.get(i).isEmpty()) {
                if (!hotbarSlots.get(i).isFree()) continue;
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * Returns the next available slot for a new item. Returns the slot to the end of the queue after returning.
     * Range from 0-8
     * @return The next available slot
     */
    private int nextHotbarSlot() {
        List<Integer> emptyHotbarSlots = emptyValidHotbarSlots();
        if (!emptyHotbarSlots.isEmpty()) {
            int hotbarSlot = emptyHotbarSlots.get(0);
            if (rollingSlots.contains(hotbarSlot)) {
                rollingSlots.remove(hotbarSlot);
                rollingSlots.addLast(hotbarSlot);
            }
            return hotbarSlot;
        }
        if (!rollingSlots.isEmpty()) {
            int slot = rollingSlots.pollFirst();
            rollingSlots.addLast(slot);
            return slot;
        }
        return 8;
    }

    /**
     * Returns the first slot with the given item. Returns -1 if no slot is found.
     * @param player The player to check
     * @param itemStack The item to check for
     * @return The first slot with the given item
     */
    private static int getBestInventorySlotWithItem(ClientPlayerEntity player, ItemStack itemStack) {
        PlayerInventory inventory = player.getInventory();

        if (itemStack.isEmpty()) return -1;

        int lowestCount = 0;
        int lowestSlot = -1;
        for(int i = 9; i < inventory.main.size(); ++i) {
            if (!(inventory.main.get(i)).isEmpty() && ItemStack.canCombine(itemStack, inventory.main.get(i))) {
                if (inventory.main.get(i).getCount() < lowestCount || lowestSlot == -1) {
                    lowestCount = inventory.main.get(i).getCount();
                    lowestSlot = i;
                }
            }
        }

        return lowestSlot;
    }

    public int getHotbarSlotWithItem(ClientPlayerEntity player, ItemStack itemStack) {
        PlayerInventory inventory = player.getInventory();

        if (itemStack.isEmpty()) return -1;

        for (int i = 0; i < 9; ++i) {
            if (!inventory.main.get(i).isEmpty() && ItemStack.areItemsEqual(inventory.main.get(i), itemStack)) {
                if (hotbarSlots.get(i).ticksLocked == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    public static boolean isInventorySlotInHotbar(int slot) {
        return slot >= 0 && slot <= 8;
    }

    static class PendingItemInfo {
        int slot;
        int ticks;
        PendingItemInfo(int slot, int ticks) {
            this.slot = slot;
            this.ticks = ticks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PendingItemInfo that = (PendingItemInfo) o;

            return slot == that.slot;
        }
    }

    static class RequestedItemInfo {
        Item item;
        int ticks;
        RequestedItemInfo(Item item, int ticks) {
            this.item = item;
            this.ticks = ticks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RequestedItemInfo that = (RequestedItemInfo) o;

            return Objects.equals(item, that.item);
        }
    }

    static class SlotInfo {
        private int ticksLocked = 0;
        @Nullable
        public Item waitingForItem = null;
        private boolean canUse = true;

        public void addTicksLocked(int ticks) {
            ticksLocked += ticks;
        }

        public void decrementTicksLocked() {
            ticksLocked--;
        }

        public void setCanUse(boolean canUse) {
            this.canUse = canUse;
        }

        public boolean isCanUse() {
            return canUse;
        }

        public boolean isFree() {
            return ticksLocked == 0 && canUse;
        }
    }
}
