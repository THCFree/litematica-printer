package me.aleksilassila.litematica.printer.v1_20;

import me.aleksilassila.litematica.printer.v1_20.config.PrinterConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class InventoryManager {
    int delay = 0;
    ArrayDeque<Item> itemQueue = new ArrayDeque<>();
    ArrayList<PendingItemInfo> pendingSlots = new ArrayList<>();
    ArrayList<PendingItemInfo> pendingItems = new ArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ArrayDeque<Integer> rollingSlots = new ArrayDeque<>();
    private static InventoryManager instance;
    private InventoryManager() {
        // Probably add a way to configure this
        for (int i = 39; i < 45; i++) {
            rollingSlots.add(i);
        }
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public void tick() {
        pendingItems.forEach((info) -> info.ticks--);
        pendingSlots.forEach((info) -> info.ticks--);
        pendingItems.removeIf((info) -> info.ticks < 1);
        pendingSlots.removeIf((info) -> info.ticks < 1);
        if (delay > PrinterConfig.INVENTORY_DELAY.getIntegerValue()) {
            delay = 0;
        }
        delay++;

        if (!itemQueue.isEmpty() && mc.player != null && mc.interactionManager != null && mc.getNetworkHandler() != null) {
            Item item = itemQueue.poll();
            int slot = getSlotWithItem(mc.player, new ItemStack(item));
            int nextSlot = nextSlot();
            pendingSlots.add(new PendingItemInfo(nextSlot, 50));
            // int currentHotbarSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = toHotbarSlot(nextSlot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(toHotbarSlot(nextSlot)));
            mc.interactionManager.pickFromInventory(slot);
            // mc.player.getInventory().selectedSlot = currentHotbarSlot;
        }
    }

    public boolean requestStack(ItemStack stack) {
        Item item = stack.getItem();
        int hash = item.hashCode();
        PendingItemInfo info = new PendingItemInfo(hash, 50);
        if (pendingItems.contains(info)) {
            return false;
        }
        pendingItems.add(info);
        itemQueue.add(item);

        return true;
    }

    public int toHotbarSlot(int slot) {
        return slot - 36;
    }

    public boolean isSlotFree(int slot) {
        PendingItemInfo info = new PendingItemInfo(slot, 50);
        return !pendingSlots.contains(info);
    }

    private void equipItem(Item item) {

    }

    /**
     * Returns the next available slot for the given item. Returns the slot to the end of the queue after returning.
     * @return The next available slot
     */
    private int nextSlot() {
        if (!rollingSlots.isEmpty()) {
            int slot = rollingSlots.pollFirst();
            rollingSlots.addLast(slot);
            return slot;
        }
        return 37;
    }

    public static int getSlotWithItem(ClientPlayerEntity player, ItemStack itemStack) {
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.main.size(); ++i) {
            if (itemStack.isEmpty() && inventory.main.get(i).isOf(itemStack.getItem())) return i;
            if (!inventory.main.get(i).isEmpty() && ItemStack.areItemsEqual(inventory.main.get(i), itemStack)) {
                return i;
            }
        }

        return -1;
    }

    static class PendingItemInfo {
        int hash;
        int ticks;
        PendingItemInfo(int hash, int ticks) {
            this.hash = hash;
            this.ticks = ticks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PendingItemInfo that = (PendingItemInfo) o;

            return hash == that.hash;
        }
    }
}
