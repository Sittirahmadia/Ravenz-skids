package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import com.raven.ravenz.utils.keybinding.simulation.ClickSimulator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class AutoHitCrystal extends Module {

    private final BooleanSetting workWithTotem   = new BooleanSetting("Work With Totem",     true);
    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation",    true);
    private final NumberSetting  placeDelay      = new NumberSetting("Obsidian Place Delay", 0, 10, 0, 1);
    private final NumberSetting  switchDelay     = new NumberSetting("Switch Delay",         0, 10, 0, 1);
    private final KeybindSetting activateKey     = new KeybindSetting("Activate Key", GLFW.GLFW_MOUSE_BUTTON_2, false);

    // ── State ──────────────────────────────────────────────────────────────
    private enum Phase { IDLE, PLACE_OBSIDIAN, SELECT_CRYSTAL, PLACE_CRYSTAL, HIT_CRYSTAL }

    private Phase phase         = Phase.IDLE;
    private int   placeClock    = 0;
    private int   switchClock   = 0;
    private int   prevSlot      = -1;
    private boolean activated   = false;

    public AutoHitCrystal() {
        super("Auto Hit Crystal", "Places obsidian then automatically places and hits end crystals", -1, Category.COMBAT);
        addSettings(workWithTotem, clickSimulation, placeDelay, switchDelay, activateKey);
    }

    @Override
    public void onEnable() { resetState(); }

    @Override
    public void onDisable() { restoreSlot(); resetState(); }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) { restoreSlot(); resetState(); return; }

        boolean keyDown = KeyUtils.isKeyPressed(activateKey.getKeyCode());

        // RMB mode — only activate when holding sword or totem
        if (activateKey.getKeyCode() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            var main = mc.player.getMainHandStack();
            boolean validItem = main.isIn(ItemTags.SWORDS)
                    || (workWithTotem.getValue() && main.isOf(Items.TOTEM_OF_UNDYING));
            if (!keyDown || (!validItem && !activated)) { restoreSlot(); resetState(); return; }
            activated = true;
        } else {
            if (!keyDown) { restoreSlot(); resetState(); return; }
        }

        // Suppress vanilla RMB use while we're active
        mc.options.useKey.setPressed(false);

        switch (phase) {

            case IDLE -> {
                // Decide starting phase based on what we're looking at
                if (mc.crosshairTarget instanceof EntityHitResult eh
                        && eh.getEntity() instanceof EndCrystalEntity) {
                    phase = Phase.HIT_CRYSTAL;
                } else if (mc.crosshairTarget instanceof BlockHitResult bh
                        && bh.getType() != HitResult.Type.MISS) {
                    var block = mc.world.getBlockState(bh.getBlockPos()).getBlock();
                    if (block instanceof SignBlock) return;
                    if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                        phase = Phase.SELECT_CRYSTAL;
                    } else {
                        phase = Phase.PLACE_OBSIDIAN;
                    }
                }
            }

            case PLACE_OBSIDIAN -> {
                if (!(mc.crosshairTarget instanceof BlockHitResult bh)
                        || bh.getType() == HitResult.Type.MISS) {
                    resetState(); return;
                }

                var block = mc.world.getBlockState(bh.getBlockPos()).getBlock();
                if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                    // Obsidian placed — move on
                    phase = Phase.SELECT_CRYSTAL;
                    return;
                }

                // Need obsidian in hand
                if (!mc.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
                    if (switchClock > 0) { switchClock--; return; }
                    if (!swapToItem(Items.OBSIDIAN)) return;
                    switchClock = switchDelay.getValueInt();
                    return;
                }

                if (placeClock > 0) { placeClock--; return; }

                if (clickSimulation.getValue()) ClickSimulator.leftClick();
                ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bh);
                if (res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);

                placeClock = placeDelay.getValueInt();
                phase = Phase.SELECT_CRYSTAL;
            }

            case SELECT_CRYSTAL -> {
                if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
                    if (switchClock > 0) { switchClock--; return; }
                    if (!swapToItem(Items.END_CRYSTAL)) { resetState(); return; }
                    switchClock = switchDelay.getValueInt();
                    return;
                }
                phase = Phase.PLACE_CRYSTAL;
            }

            case PLACE_CRYSTAL -> {
                // If a crystal appeared under crosshair, skip to hit immediately
                if (mc.crosshairTarget instanceof EntityHitResult eh
                        && eh.getEntity() instanceof EndCrystalEntity) {
                    phase = Phase.HIT_CRYSTAL;
                    return;
                }

                if (!(mc.crosshairTarget instanceof BlockHitResult bh)
                        || bh.getType() == HitResult.Type.MISS) {
                    resetState(); return;
                }

                // Validate placement surface
                BlockPos pos = bh.getBlockPos();
                var block = mc.world.getBlockState(pos).getBlock();
                if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
                    phase = Phase.IDLE; return;
                }

                ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bh);
                if (res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
                phase = Phase.HIT_CRYSTAL;
            }

            case HIT_CRYSTAL -> {
                // Confirm crystal is actually there before hitting
                if (mc.crosshairTarget instanceof EntityHitResult eh
                        && eh.getEntity() instanceof EndCrystalEntity crystal) {

                    // Client-side instant removal before server confirms
                    crystal.setRemoved(net.minecraft.entity.Entity.RemovalReason.KILLED);
                    crystal.onRemoved();

                    ((MinecraftClientAccessor) mc).invokeDoAttack();
                    restoreSlot();
                    resetState();
                } else {
                    // Crystal not in crosshair — reset and let player re-aim
                    restoreSlot();
                    resetState();
                }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean swapToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                if (prevSlot == -1) prevSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                return true;
            }
        }
        return false;
    }

    private void restoreSlot() {
        if (prevSlot != -1 && !isNull()) {
            mc.player.getInventory().selectedSlot = prevSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }
        prevSlot = -1;
    }

    private void resetState() {
        phase         = Phase.IDLE;
        placeClock    = 0;
        switchClock   = 0;
        activated     = false;
    }
}
