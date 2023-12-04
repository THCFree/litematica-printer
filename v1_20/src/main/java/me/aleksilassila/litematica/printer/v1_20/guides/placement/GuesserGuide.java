package me.aleksilassila.litematica.printer.v1_20.guides.placement;

import me.aleksilassila.litematica.printer.v1_20.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.v1_20.config.PrinterConfig;
import me.aleksilassila.litematica.printer.v1_20.implementation.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.v1_20.SchematicBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * This is the placement guide that most blocks will use.
 * It will try to predict the correct player state for producing the right blockState
 * by brute forcing the correct hit vector and look direction.
 */
public class GuesserGuide extends GeneralPlacementGuide {
    private PrinterPlacementContext contextCache = null;

    protected static Direction[] directionsToTry = new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };
    protected static Vec3d[] hitVecsToTry = new Vec3d[]{
            new Vec3d(-0.25, -0.25, -0.25),
            new Vec3d(+0.25, -0.25, -0.25),
            new Vec3d(-0.25, +0.25, -0.25),
            new Vec3d(-0.25, -0.25, +0.25),
            new Vec3d(+0.25, +0.25, -0.25),
            new Vec3d(-0.25, +0.25, +0.25),
            new Vec3d(+0.25, -0.25, +0.25),
            new Vec3d(+0.25, +0.25, +0.25)
    };

    protected static Vec3d[] carpetHitVecsToTry = new Vec3d[]{
            new Vec3d(-0.25, -0.25, -0.25),
            new Vec3d(+0.25, -0.25, -0.25),
            new Vec3d(-0.25, +0.25, -0.25),
            new Vec3d(-0.25, -0.25, +0.25),
            new Vec3d(+0.25, +0.25, -0.25),
            new Vec3d(-0.25, +0.25, +0.25),
            new Vec3d(+0.25, -0.25, +0.25),
            new Vec3d(+0.25, +0.25, +0.25),
            new Vec3d(-0.25, -0.49, -0.25), // 1/4 Just above the lower edge of a block. For carpets for instance as they are very thin
            new Vec3d(+0.25, -0.49, -0.25), // 2/4
            new Vec3d(-0.25, -0.49, +0.25), // 3/4
            new Vec3d(+0.25, -0.49, +0.25) // 4/4
    };

    public GuesserGuide(SchematicBlockState state) {
        super(state);
    }

    @Nullable
    @Override
    public PrinterPlacementContext getPlacementContext(ClientPlayerEntity player) {
        if (contextCache != null && !LitematicaMixinMod.DEBUG) return contextCache;

        ItemStack requiredItem = getRequiredItem(player).stream().findFirst().orElse(ItemStack.EMPTY);
        int slot = getRequiredItemStackSlot(player);

        if (slot == -1) return null;

        for (Direction lookDirection : directionsToTry) {
            for (Direction side : directionsToTry) {
                BlockPos neighborPos = state.blockPos.offset(side);
                BlockState neighborState = state.world.getBlockState(neighborPos);
                boolean requiresShift = getRequiresExplicitShift() || isInteractive(neighborState.getBlock());

                if (!canBeClicked(state.world, neighborPos) || // Handle unclickable grass for example
                        neighborState.isReplaceable())
                    continue;

                if (!canSeeBlockFace(player, new BlockHitResult(Vec3d.ofCenter(state.blockPos), side, neighborPos, false)))
                    continue;

                Vec3d hitVec = Vec3d.ofCenter(state.blockPos)
                        .add(Vec3d.of(side.getVector()).multiply(0.5));

                // If we can see the face of the blocks its enough for lenient stealth
                if (PrinterConfig.LENIENT_STEALTH.getBooleanValue()) {
                    BlockHitResult hitResult = new BlockHitResult(hitVec.add(Vec3d.of(side.getVector()).multiply(0.5)), side.getOpposite(), neighborPos, false);
                    return new PrinterPlacementContext(player, hitResult, requiredItem, slot, lookDirection, requiresShift);
                }

                Vec3d[] hitVecsToTryArray = hitVecsToTry;
                if (PrinterConfig.CARPET_MODE.getBooleanValue()) {
                    hitVecsToTryArray = carpetHitVecsToTry;
                }

                // Now we bring on the big guns, brute force the hit vector until we find a solution that directly hits the neighbor block without obstruction
                for (Vec3d hitVecToTry : hitVecsToTryArray) {
                    Vec3d multiplier = Vec3d.of(side.getVector());
                    multiplier = new Vec3d(
                            multiplier.x == 0 ? 1 : 0,
                            multiplier.y == 0 ? 1 : 0,
                            multiplier.z == 0 ? 1 : 0);

                    BlockHitResult hitResult = new BlockHitResult(hitVec.add(hitVecToTry.multiply(multiplier)), side.getOpposite(), neighborPos, false);
                    PrinterPlacementContext context = new PrinterPlacementContext(player, hitResult, requiredItem, slot, lookDirection, requiresShift);
                    context.canStealth = true;
                    BlockState result = getRequiredItemAsBlock(player)
                            .orElse(targetState.getBlock())
                            .getPlacementState(context); // FIXME torch shift clicks another torch and getPlacementState is the clicked block, which is true

                    if (result != null && (statesEqual(result, targetState) || correctChestPlacement(targetState, result))) {
                        contextCache = context;
                        return context;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean canExecute(ClientPlayerEntity player) {
        if (targetState.getBlock() instanceof SlabBlock) return false; // Slabs are a special case

        return super.canExecute(player);
    }

    private boolean canSeeBlockFace(ClientPlayerEntity player, BlockHitResult hitResult) {
        // Draw a line between the player pos and the block pos and check if the block side is visible
        // BlockPos targetPos = state.blockPos;

        Vec3d sideOffset = Vec3d.of(hitResult.getSide().getVector()).multiply(0.5);

        Vec3d hitLocation = hitResult.getPos().add(sideOffset);
        Vec3d vecToHit = hitLocation.subtract(player.getEyePos());
        Vec3d vecSideSurface = new Vec3d(hitResult.getSide().getVector().getX(), hitResult.getSide().getVector().getY(), hitResult.getSide().getVector().getZ());
        return vecToHit.dotProduct(vecSideSurface) > PrinterConfig.MIN_BLOCK_HIT_ANGLE.getDoubleValue(); // If the dot product is negative, the block side is not visible
    }

    private boolean correctChestPlacement(BlockState targetState, BlockState result) {
        if (targetState.contains(ChestBlock.CHEST_TYPE) && result.contains(ChestBlock.CHEST_TYPE) && result.get(ChestBlock.FACING) == targetState.get(ChestBlock.FACING)) {
            ChestType targetChestType = targetState.get(ChestBlock.CHEST_TYPE);
            ChestType resultChestType = result.get(ChestBlock.CHEST_TYPE);

            return targetChestType != ChestType.SINGLE && resultChestType == ChestType.SINGLE;
        }

        return false;
    }
}
