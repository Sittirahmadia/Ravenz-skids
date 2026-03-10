package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AirAnchor extends Module {

    private BlockPos lastPos;
    private int extraPacketCount;

    public AirAnchor() {
        super("Air Anchor", "Sends an extra interact packet when detonating a charged respawn anchor", -1, Category.COMBAT);
    }

    @Override
    public void onEnable() {
        lastPos = null;
        extraPacketCount = 0;
    }

    @EventHandler
    private void onHandleInput(HandleInputEvent event) {
        if (isNull() || mc.currentScreen != null) return;
        if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;

        BlockPos pos = hit.getBlockPos();
        var state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof RespawnAnchorBlock)) return;

        int charges = state.get(RespawnAnchorBlock.CHARGES);
        if (charges <= 0) return; // not yet charged, nothing to detonate

        if (!pos.equals(lastPos)) {
            lastPos = pos;
            extraPacketCount = 0;
        }

        // Allow only one extra packet per anchor position per press
        if (extraPacketCount >= 1) return;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        extraPacketCount++;
    }
}
