package me.aleksilassila.litematica.printer.v1_20.guides.placement;

import me.aleksilassila.litematica.printer.v1_20.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.v1_20.config.PrinterConfig;
import me.aleksilassila.litematica.printer.v1_20.implementation.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.v1_20.SchematicBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * This is the placement guide that most blocks will use.
 * It will try to predict the correct player state for producing the right blockState
 * by brute forcing the correct hit vector and look direction.
 */
public class GuesserGuide extends GeneralPlacementGuide {
    private PrinterPlacementContext contextCache = null;
    private final MinecraftClient mc = MinecraftClient.getInstance();

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
        if (contextCache != null && !LitematicaMixinMod.DEBUG && !PrinterConfig.NO_PLACEMENT_CACHE.getBooleanValue() && contextCache.isRaytrace == PrinterConfig.RAYCAST.getBooleanValue()) return contextCache;

        ItemStack requiredItem = getRequiredItem(player).stream().findFirst().orElse(ItemStack.EMPTY);
        int slot = getRequiredItemStackSlot(player);

        if (slot == -1) return null;

        Vec3d[] hitVecsToTryArray = PrinterConfig.CARPET_MODE.getBooleanValue() ? carpetHitVecsToTry : hitVecsToTry;

        Vec3d playerEyePos = player.getEyePos();

        for (Direction lookDirection : directionsToTry) {
            for (Direction side : directionsToTry) {
                BlockPos neighborPos = state.blockPos.offset(side);

                // Check if the block face is visible. Prevents the printer from trying to place blocks on the backside of other blocks
                if (PrinterConfig.STRICT_BLOCK_FACE_CHECK.getBooleanValue()) {
                    if (!canSeeBlockFace(player, new BlockHitResult(Vec3d.ofCenter(state.blockPos), side.getOpposite(), neighborPos, false))) {
                        continue;
                    }
                }

                BlockState neighborState = state.world.getBlockState(neighborPos);
                boolean requiresShift = getRequiresExplicitShift() || isInteractive(neighborState.getBlock());

                if (!canBeClicked(state.world, neighborPos) || // Handle unclickable grass for example
                        neighborState.isReplaceable())
                    continue;

                Vec3d hitVec = Vec3d.ofCenter(state.blockPos)
                        .add(Vec3d.of(side.getVector()).multiply(0.5)); // Center of the block side face we are placing on

                // Now we bring on the big guns, brute force the hit vector until we find a solution that directly hits the neighbor block without obstruction
                for (Vec3d hitVecToTry : hitVecsToTryArray) {
                    Vec3d multiplier = Vec3d.of(side.getVector());
                    multiplier = new Vec3d(
                            multiplier.x == 0 ? 1 : 0,
                            multiplier.y == 0 ? 1 : 0,
                            multiplier.z == 0 ? 1 : 0); // Offset from the Center of the block side face we are placing on by pre calculated values. This samples different points on that face.

                    Vec3d blockHit = hitVec.add(hitVecToTry.multiply(multiplier));

                    if (playerEyePos.distanceTo(blockHit) > LitematicaMixinMod.PRINTING_RANGE.getDoubleValue()) // Check if the hit vector is in range
                        continue;

                    if (PrinterConfig.RAYCAST.getBooleanValue() && mc.world != null && mc.player != null) {
                        Vec3d lookVec = blockHit.subtract(playerEyePos).normalize(); // Look vector from the player's eye to the block hit vector
                        Vec3d raycastEnd = playerEyePos.add(lookVec.multiply(5)); // 5 block max distance
                        RaycastContext raycastContext = new RaycastContext(playerEyePos, raycastEnd, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player);
                        BlockHitResult result = mc.world.raycast(raycastContext);
                        if (result.getType() != HitResult.Type.BLOCK) { // If we didn't hit a block, skip
                            continue;
                        }

                        if (result.getBlockPos().equals(neighborPos)) {
                            if (PrinterConfig.RAYCAST_STRICT_BLOCK_HIT.getBooleanValue()) { // Check if the right side was hit
                                Direction hitSide = result.getSide();
                                if (hitSide.getOpposite() != side) {
                                    continue;
                                }
                            }
                            if (result.getPos().distanceTo(playerEyePos) > LitematicaMixinMod.PRINTING_RANGE.getDoubleValue()) { // Check if the hit result is in range
                                continue;
                            }
                            BlockHitResult hitResult = new BlockHitResult(blockHit, side.getOpposite(), neighborPos, false);
                            PrinterPlacementContext rayTraceContext = new PrinterPlacementContext(player, hitResult, requiredItem, slot, lookDirection, requiresShift);
                            rayTraceContext.canStealth = true;
                            rayTraceContext.isRaytrace = true;
                            BlockState resultState = getRequiredItemAsBlock(player)
                                    .orElse(targetState.getBlock())
                                    .getPlacementState(rayTraceContext);

                            if (resultState != null && (statesEqual(resultState, targetState) || correctChestPlacement(targetState, resultState))) {
                                contextCache = rayTraceContext;
                                return rayTraceContext;
                            }
                        }
                        continue;
                    }

                    BlockHitResult hitResult = new BlockHitResult(blockHit, side.getOpposite(), neighborPos, false);
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

        Direction side = hitResult.getSide();
        BlockPos blockPos = hitResult.getBlockPos();
        Vec3d playerEyePos = player.getEyePos();
        switch (side) {
            case UP:
                if (blockPos.getY() + 1 > playerEyePos.getY()) return false;
                break;
            case DOWN:
                if (blockPos.getY() < playerEyePos.getY()) return false;
                break;
            case NORTH:
                if (blockPos.getZ() < playerEyePos.getZ()) return false;
                break;
            case SOUTH:
                if (blockPos.getZ() + 1 > playerEyePos.getZ()) return false;
                break;
            case EAST:
                if (blockPos.getX() + 1 > playerEyePos.getX()) return false;
                break;
            case WEST:
                if (blockPos.getX() < playerEyePos.getX()) return false;
                break;
        }
        return true;

//        Direction side = hitResult.getSide(); // The side of the block we are placing on
//        BlockPos pos = hitResult.getBlockPos(); // Neighbor block pos
//
//        Vec3d hitLocation = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5)); // Center of the block side face we are placing on
//
//        Vec3d lookVector = hitLocation.subtract(player.getEyePos());
//        return lookVector.dotProduct(Vec3d.of(side.getVector())) > PrinterConfig.MIN_BLOCK_HIT_ANGLE.getDoubleValue(); // If the dot product is negative, the block side is not visible
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
