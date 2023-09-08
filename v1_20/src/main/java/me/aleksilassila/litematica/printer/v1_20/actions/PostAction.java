package me.aleksilassila.litematica.printer.v1_20.actions;

import me.aleksilassila.litematica.printer.v1_20.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.v1_20.config.PrinterConfig;
import me.aleksilassila.litematica.printer.v1_20.implementation.PrinterPlacementContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class PostAction extends PrepareAction {
    public PostAction(PrinterPlacementContext context, ClientPlayerEntity player) {
        super(context);
        this.pitch = player.getPitch();
        this.yaw = player.getYaw();
    }

    @Override
    public void send(MinecraftClient client, ClientPlayerEntity player) {
        if (context.canStealth) {
            PlayerMoveC2SPacket.Full packet = new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(), 0, 0, player.isOnGround());

            if (PrinterConfig.DEBUG_MODE.getBooleanValue()) {
                player.setYaw(this.yaw);
                player.setPitch(this.pitch);
            }

            player.networkHandler.sendPacket(packet);
        }
    }
}
