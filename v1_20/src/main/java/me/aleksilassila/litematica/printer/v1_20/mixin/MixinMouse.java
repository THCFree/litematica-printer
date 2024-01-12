package me.aleksilassila.litematica.printer.v1_20.mixin;

import me.aleksilassila.litematica.printer.v1_20.FreeLook;
import me.aleksilassila.litematica.printer.v1_20.LitematicaMixinMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class)
public class MixinMouse {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Shadow @Final private MinecraftClient client;
    @Unique
    MinecraftClient mc = MinecraftClient.getInstance();
    @Inject(method = "updateMouse",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/Mouse;cursorDeltaX:D",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE
            ), cancellable = true
    )
    private void updateMouseChangeLookDirection(CallbackInfo ci) {
        FreeLook freeLook = LitematicaMixinMod.freeLook;
        if (freeLook.isEnabled()) {
            double f = this.client.options.getMouseSensitivity().getValue() * 0.6000000238418579 + 0.20000000298023224;
            double g = f * f * f;
            double h = g * 8.0;
            double k = this.cursorDeltaX * h;
            double l = this.cursorDeltaY * h;
            int m = 1;
            if (this.client.options.getInvertYMouse().getValue()) {
                m = -1;
            }
            if (mc.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
            freeLook.setCameraYaw((float) (freeLook.getCameraYaw() + k * 0.15F));
            float pitch = MathHelper.clamp((float) (freeLook.getCameraPitch() + (l * (double) m) * 0.15F), -90.0F, 90.0F);
            freeLook.setCameraPitch(pitch);
            if (Math.abs(freeLook.getCameraPitch()) > 90.0F) freeLook.setCameraYaw(freeLook.getCameraPitch() > 0.0F ? 90.0F : -90.0F);
            cursorDeltaX = 0.0;
            cursorDeltaY = 0.0;
            ci.cancel();
        }
    }
}