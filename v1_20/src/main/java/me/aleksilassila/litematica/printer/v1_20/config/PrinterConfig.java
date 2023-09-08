package me.aleksilassila.litematica.printer.v1_20.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;

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
    public static final ConfigBoolean DEBUG_MODE = new ConfigBoolean("printerDebugMode", false, "Debug mode. Prints debug messages to the console and adds additional debug information.");
    public static final ConfigBoolean SNAP_BASK = new ConfigBoolean("printerSnapBask", false, "Snap back to the view direction after placing a block.");
    public static final ConfigBoolean STOP_ON_MOVEMENT = new ConfigBoolean("printerStopOnMovement", true, "Stop the printer if the player velocity is to high.");
    public static final ConfigBoolean INTERPOLATE_LOOK = new ConfigBoolean("printerInterpolateLook", true, "Interpolate the player look direction packets.");
    public static final ConfigDouble INTERPOLATE_LOOK_MAX_ANGLE = new ConfigDouble("printerInterpolateLookMaxAngle", 10, "Interpolate the player look direction packets.");
    public ImmutableList<IConfigBase> getOptions() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
        list.add(DEBUG_MODE);
        list.add(SNAP_BASK);
        list.add(STOP_ON_MOVEMENT);
        list.add(INTERPOLATE_LOOK);
        list.add(INTERPOLATE_LOOK_MAX_ANGLE);
        return ImmutableList.copyOf(list);
    }
}
