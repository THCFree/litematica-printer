package me.aleksilassila.litematica.printer.v1_20.actions;

import me.aleksilassila.litematica.printer.v1_20.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.v1_20.Printer;
import me.aleksilassila.litematica.printer.v1_20.implementation.PrinterPlacementContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

abstract public class InteractAction extends Action {
    public final PrinterPlacementContext context;

    public InteractAction(PrinterPlacementContext context) {
        this.context = context;
        isSync = false;
    }

    protected abstract ActionResult interact(MinecraftClient client, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult);

    @Override
    public void send(MinecraftClient client, ClientPlayerEntity player) {
        ActionResult result = interact(client, player, Hand.MAIN_HAND, context.hitResult);

        if (LitematicaMixinMod.DEBUG)
            System.out.println("InteractAction.send: Blockpos: " + context.getBlockPos() + " Side: " + context.getSide() + " HitPos: " + context.getHitPos());
        if (result.isAccepted()) {
            Printer.addTimeout(context.getBlockPos());
        }
    }

    @Override
    public String toString() {
        return "InteractAction{" +
                "context=" + context +
                '}';
    }
}
