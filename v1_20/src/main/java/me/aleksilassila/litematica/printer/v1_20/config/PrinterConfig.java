package me.aleksilassila.litematica.printer.v1_20.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import javax.annotation.Nullable;
import java.util.List;

public class PrinterConfig {
    private PrinterConfig() {}
    @Nullable
    public static PrinterConfig INSTANCE;

    public static PrinterConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PrinterConfig();
        }
        return INSTANCE;
    }
    public static final ConfigInteger TICK_DELAY = new ConfigInteger("printerTickDelay", 8, 0, 100, "Tick delay between actions. 0 = no delay.");
    public static final ConfigInteger BLOCK_TIMEOUT = new ConfigInteger("printerBlockTimeout", 10, 0, 100, "How many ticks to wait before trying to place the same block again.");
    public static final ConfigBoolean ROTATE_PLAYER = new ConfigBoolean("printerRotatePlayer", false, "Rotate the player to face the block to place.");
    public static final ConfigBoolean SNAP_BACK = new ConfigBoolean("printerSnapBask", false, "Snap back to the view direction after placing a block.");
    public static final ConfigBoolean STOP_ON_MOVEMENT = new ConfigBoolean("printerStopOnMovement", true, "Stop the printer if the player velocity is to high.");
    public static final ConfigBoolean INTERPOLATE_LOOK = new ConfigBoolean("printerInterpolateLook", true, "Interpolate the player look direction packets.");
    public static final ConfigDouble INTERPOLATE_LOOK_MAX_ANGLE = new ConfigDouble("printerInterpolateLookMaxAngle", 10, "Interpolate the player look direction packets.");
    public static final ConfigBoolean LENIENT_STEALTH = new ConfigBoolean("printerLeaneantStealth", true, "Lenient stealth mode. Good enough for grim and offers better placement speed.");
    public static final ConfigBoolean CARPET_MODE = new ConfigBoolean("printerCarpetMode", false, "Carpet mode. For placing carpets on the top of blocks.");
    public static final ConfigInteger INACTIVE_SNAP_BACK = new ConfigInteger("printerInactiveSnapBack", 10, "Snap back to the view direction after placing a block.");
    public static final ConfigDouble MIN_BLOCK_HIT_ANGLE = new ConfigDouble("printerMaxBlockHitAngle", 1, 0, 30, "The maximum angle between the player look direction and the block hit direction.");
    public static final ConfigInteger INVENTORY_DELAY = new ConfigInteger("printerInventoryDelay", 2, 0, 100, "The delay between each inventory action. 0 = no delay.");
    public static final ConfigBoolean RAYCAST = new ConfigBoolean("printerRaycast", true, "Raycast the block to place to check if it is visible.");
    public static final ConfigBoolean NO_PLACEMENT_CACHE = new ConfigBoolean("printerNoPlacementCache", false, "Disable the placement cache. This will make the printer slower but more accurate.");
    public static final ConfigBoolean RAYCAST_STRICT_BLOCK_HIT = new ConfigBoolean("printerRaycastStrictBlockHit", true, "Check if the right side of the block is hit.");
    public static final ConfigBoolean PREVENT_DOUBLE_TAP_SPRINTING = new ConfigBoolean("printerPreventDoubleTapSprinting", true, "Prevent double tap sprinting when the printer is active.");
    public static final ConfigBoolean FREE_LOOK = new ConfigBoolean("printerFreeLook", false, "Free look mode. Allows you to look around while the printer is active.");
    public static final ConfigHotkey FREE_LOOK_TOGGLE = new ConfigHotkey("printerFreeLook", "LEFT_ALT", KeybindSettings.MODIFIER_INGAME, "Free look mode. Allows you to look around while the printer is active.");
    public static final ConfigBoolean STRICT_BLOCK_FACE_CHECK = new ConfigBoolean("printerStrictBlockFaceCheck", true, "Places only against block faces that are facing the player.");

    public ImmutableList<IConfigBase> getOptions() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
        list.add(TICK_DELAY);
        list.add(ROTATE_PLAYER);
        list.add(STOP_ON_MOVEMENT);
        list.add(CARPET_MODE);
        list.add(MIN_BLOCK_HIT_ANGLE);
        list.add(INVENTORY_DELAY);
        list.add(RAYCAST);
        list.add(NO_PLACEMENT_CACHE);
        list.add(RAYCAST_STRICT_BLOCK_HIT);
        list.add(NO_PLACEMENT_CACHE);
        list.add(STRICT_BLOCK_FACE_CHECK);
        return ImmutableList.copyOf(list);
    }
}
