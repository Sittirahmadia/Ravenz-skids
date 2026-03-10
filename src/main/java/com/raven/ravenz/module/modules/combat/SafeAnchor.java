package com.raven.ravenz.module.modules.combat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SafeAnchor {

    /**
     * Executes the safe anchor macro:
     * 1. Validates crosshair target is a real solid block (not air, not out of world).
     * 2. Validates the block above the target is air so the anchor won't float.
     * 3. Places the anchor only if validation passes.
     * 4. Locates the freshly placed anchor in the world before charging with glowstone.
     * 5. Only charges if a RespawnAnchorBlock is actually present at the expected position.
     */
    public void executeMacro(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        int anchorSlot    = findItem(mc, Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findItem(mc, Items.GLOWSTONE);
        if (anchorSlot == -1 || glowstoneSlot == -1) return;

        // ── 1. Crosshair must be hitting a block ──────────────────────────────
        HitResult target = mc.crosshairTarget;
        if (!(target instanceof BlockHitResult originalHit)) return;
        if (originalHit.getType() == HitResult.Type.MISS)   return;

        BlockPos basePos   = originalHit.getBlockPos();               // block we're clicking ON
        BlockPos anchorPos = basePos.offset(originalHit.getSide());   // block that will receive the anchor

        // ── 2. Base block must be solid / have a full top face ────────────────
        BlockState baseState = mc.world.getBlockState(basePos);
        if (baseState.isAir()) return;                                 // nothing to place on
        if (!baseState.isSolidBlock(mc.world, basePos)) return;       // e.g. slab, slab-top only — skip

        // ── 3. Destination block (anchorPos) must be completely empty ─────────
        BlockState destState = mc.world.getBlockState(anchorPos);
        if (!destState.isAir()) return;                               // already occupied

        // ── 4. The block ABOVE the anchor must also be clear (anchors are 1-tall
        //       but we still need head clearance for the explosion to be useful) ─
        BlockPos aboveAnchor = anchorPos.up();
        if (!mc.world.getBlockState(aboveAnchor).isAir()) return;

        // ── 5. The anchor position must be inside valid world bounds ──────────
        if (!mc.world.isInBuildLimit(anchorPos)) return;

        // ── 6. Place the anchor ───────────────────────────────────────────────
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = anchorSlot;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, originalHit);
        mc.player.swingHand(Hand.MAIN_HAND);

        // ── 7. Confirm the anchor actually appeared in the world ──────────────
        //       (server may reject the placement)
        BlockState placedState = mc.world.getBlockState(anchorPos);
        if (!(placedState.getBlock() instanceof RespawnAnchorBlock)) {
            // Placement was rejected — restore slot and abort
            mc.player.getInventory().selectedSlot = prevSlot;
            return;
        }

        // ── 8. Charge the anchor with glowstone ───────────────────────────────
        mc.player.getInventory().selectedSlot = glowstoneSlot;

        // Build a hit result aimed at the TOP face of the placed anchor
        Vec3d anchorCenter = Vec3d.ofCenter(anchorPos);
        BlockHitResult anchorHit = new BlockHitResult(
                new Vec3d(anchorCenter.x, anchorPos.getY() + 1.0, anchorCenter.z),
                Direction.UP,
                anchorPos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, anchorHit);
        mc.player.swingHand(Hand.MAIN_HAND);

        // ── 9. Restore original hotbar slot ───────────────────────────────────
        mc.player.getInventory().selectedSlot = prevSlot;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the hotbar slot (0-8) that holds {@code item}, or -1 if not found. */
    private int findItem(MinecraftClient mc, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
