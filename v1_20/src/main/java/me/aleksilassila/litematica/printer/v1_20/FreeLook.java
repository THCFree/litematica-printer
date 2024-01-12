package me.aleksilassila.litematica.printer.v1_20;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.v1_20.config.PrinterConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

import java.util.function.Consumer;

public class FreeLook {
    float cameraYaw;
    float cameraPitch;
    Perspective prevPerspective;
    MinecraftClient mc = MinecraftClient.getInstance();
    boolean enabled = false;

//    BooleanSetting changePers = addBooleanSetting("Change Perspective",true);
//    EnumSetting<Mode> cameraMode = addEnumSetting("Camera Mode",Mode.CAMERA);
//    FloatSetting sensitivity = addFloatSetting("Sensitivity",8,0,10);
    public FreeLook() {
        enabled = PrinterConfig.FREE_LOOK.getBooleanValue();
        PrinterConfig.FREE_LOOK.setValueChangeCallback(this::setEnabled);
    }

    private void setEnabled(ConfigBoolean configBoolean) {
        this.enabled = configBoolean.getBooleanValue();
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    void onEnable() {
        if(mc.player == null) {
            return;
        }
        this.enabled = true;

        cameraPitch = mc.player.getPitch();
        cameraYaw = mc.player.getYaw();
        prevPerspective = mc.options.getPerspective();

        if (prevPerspective != Perspective.THIRD_PERSON_BACK /*&& changePers.getValue()*/) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    void onDisable() {
        this.enabled = false;
        if (mc.options.getPerspective() != prevPerspective /*&& changePers.getValue()*/) mc.options.setPerspective(prevPerspective);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public void setCameraYaw(float v) {
        cameraYaw = v;
    }

    public void setCameraPitch(float v) {
        cameraPitch = v;
    }

    public void setPrevPerspective(Perspective perspective) {
        prevPerspective = perspective;
    }

    public Perspective getPrevPerspective() {
        return prevPerspective;
    }

    public enum Mode{
        CAMERA,
        PLAYER
    }
}