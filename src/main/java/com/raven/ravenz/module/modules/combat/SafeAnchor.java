package com.raven.ravenz.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SafeAnchor {

    public void executeMacro(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        int anchorSlot = findItem(client, Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findItem(client, Items.GLOWSTONE);

        if (anchorSlot == -1 || glowstoneSlot == -1) return;

        HitResult crosshairTarget = client.crosshairTarget;
        if (!(crosshairTarget instanceof BlockHitResult originalHit)) return;

        client.player.getInventory().selectedSlot = anchorSlot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, originalHit);

        client.player.getInventory().selectedSlot = glowstoneSlot;
        float originalPitch = client.player.getPitch();
        float originalYaw = client.player.getYaw();

        client.player.setPitch(80f);

        BlockPos feetPos = client.player.getBlockPos();
        BlockHitResult safeBlockHit = new BlockHitResult(
                new Vec3d(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5),
                Direction.UP,
                feetPos,
                false
        );

        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, safeBlockHit);

        client.player.setPitch(originalPitch);
        client.player.setYaw(originalYaw);
    }

    private int findItem(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }
}
