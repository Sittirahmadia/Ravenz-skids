package com.raven.ravenz.module.modules.misc;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.KeybindSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.keybinding.KeyUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * SafeAnchorAuto — places a shield block beside the anchor before exploding.
 * Phases: IDLE → PLACE_ANCHOR → PLACE_PROTECTION → CHARGE_ANCHOR → EXPLODE → DONE
 */
public final class SafeAnchorAuto extends Module {

    private final NumberSetting  range           = new NumberSetting("Range",          1, 6, 4.5, 0.1);
    private final NumberSetting  minHealth       = new NumberSetting("Min Health",      0, 20, 0, 1);
    private final BooleanSetting autoExplode     = new BooleanSetting("Auto Explode",   true);
    private final NumberSetting  placeDelay      = new NumberSetting("Place Delay",     0, 10, 0, 1);
    private final NumberSetting  stageDelay      = new NumberSetting("Stage Delay",     0, 10, 1, 1);
    private final NumberSetting  chargeCount     = new NumberSetting("Charge Count",    1, 4, 4, 1);
    private final BooleanSetting silentSwing     = new BooleanSetting("Silent Swing",   true);
    private final BooleanSetting smartProtection = new BooleanSetting("Smart Protect",  true);
    private final BooleanSetting autoSwitchBack  = new BooleanSetting("Switch Back",    true);
    private final ModeSetting    blockMode       = new ModeSetting("Block Menu",
            "Glowstone", "Glowstone", "Obsidian", "EnderChest", "Off");
    private final BooleanSetting useActivateKey  = new BooleanSetting("Use Activation Key", false);
    private final KeybindSetting activateKey     = new KeybindSetting("Activation Key", GLFW.GLFW_KEY_UNKNOWN, false);

    private Phase  currentPhase  = Phase.IDLE;
    private int    delayClock    = 0;
    private int    chargesPlaced = 0;
    private BlockPos anchorPos   = null;
    private int    originalSlot  = -1;

    public SafeAnchorAuto() {
        super("Safe Anchor Auto", "Places shield block before exploding respawn anchors", -1, Category.MISC);
        addSettings(range, minHealth, autoExplode, placeDelay, stageDelay, chargeCount,
                silentSwing, smartProtection, autoSwitchBack,
                blockMode, useActivateKey, activateKey);
    }

    @Override public void onEnable()  { reset(); super.onEnable(); }
    @Override public void onDisable() { reset(); super.onDisable(); }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        if (useActivateKey.getValue()) {
            int key = activateKey.getKeyCode();
            if (key == GLFW.GLFW_KEY_UNKNOWN || !KeyUtils.isKeyPressed(key)) return;
        }

        if (currentPhase == Phase.IDLE) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp < minHealth.getValue() * 2f) { reset(); setEnabled(false); return; }

            if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
            if (hit.getType() == HitResult.Type.MISS) return;

            BlockPos pos = hit.getBlockPos();
            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.getValue() * range.getValue()) return;

            originalSlot = mc.player.getInventory().getSelectedSlot();

            var state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof RespawnAnchorBlock) {
                anchorPos = pos;
                currentPhase = Phase.PLACE_PROTECTION;
            } else {
                BlockPos candidate = pos.offset(hit.getSide());
                if (isReplaceable(candidate)) {
                    anchorPos = candidate;
                    currentPhase = Phase.PLACE_ANCHOR;
                }
            }
            return;
        }

        if (delayClock > 0) { delayClock--; return; }

        switch (currentPhase) {
            case PLACE_ANCHOR -> {
                int slot = findHotbar(Items.RESPAWN_ANCHOR);
                if (slot == -1) { reset(); return; }
                selectSlot(slot);
                placeAt(anchorPos);
                delayClock = stageDelay.getValueInt();
                currentPhase = Phase.PLACE_PROTECTION;
            }
            case PLACE_PROTECTION -> {
                if (blockMode.getMode().equals("Off")) {
                    chargesPlaced = 0;
                    currentPhase = Phase.CHARGE_ANCHOR;
                    return;
                }
                BlockPos protect = getProtectionPos(anchorPos);
                if (protect != null && isReplaceable(protect)) {
                    net.minecraft.item.Item blockItem = switch (blockMode.getMode()) {
                        case "Obsidian"   -> Items.OBSIDIAN;
                        case "EnderChest" -> Items.ENDER_CHEST;
                        default           -> Items.GLOWSTONE;
                    };
                    int slot = findHotbar(blockItem);
                    if (slot != -1) { selectSlot(slot); placeAt(protect); }
                }
                delayClock = stageDelay.getValueInt();
                chargesPlaced = 0;
                currentPhase = Phase.CHARGE_ANCHOR;
            }
            case CHARGE_ANCHOR -> {
                int glowSlot = findHotbar(Items.GLOWSTONE);
                if (glowSlot == -1) {
                    delayClock = stageDelay.getValueInt();
                    currentPhase = autoExplode.getValue() ? Phase.EXPLODE : Phase.DONE;
                    return;
                }
                selectSlot(glowSlot);
                interactAt(anchorPos);
                chargesPlaced++;
                if (chargesPlaced >= chargeCount.getValueInt()) {
                    delayClock = stageDelay.getValueInt();
                    currentPhase = autoExplode.getValue() ? Phase.EXPLODE : Phase.DONE;
                } else {
                    delayClock = placeDelay.getValueInt();
                }
            }
            case EXPLODE -> {
                // Switch to any non-anchor/non-glowstone slot to trigger explosion
                for (int i = 0; i < 9; i++) {
                    var stack = mc.player.getInventory().getStack(i);
                    if (stack.isEmpty() || (!stack.isOf(Items.GLOWSTONE) && !stack.isOf(Items.RESPAWN_ANCHOR))) {
                        selectSlot(i);
                        break;
                    }
                }
                delayClock = 1;
                interactAt(anchorPos);
                currentPhase = Phase.DONE;
            }
            case DONE -> {
                if (autoSwitchBack.getValue() && originalSlot != -1)
                    mc.player.getInventory().setSelectedSlot(originalSlot);
                reset();
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private BlockPos getProtectionPos(BlockPos anchor) {
        if (anchor == null) return null;
        Vec3d eye = mc.player.getEyePos();
        double dx = eye.x - (anchor.getX() + 0.5);
        double dz = eye.z - (anchor.getZ() + 0.5);
        int xOff = 0, zOff = 0;
        if (Math.abs(dx) > Math.abs(dz)) xOff = dx > 0 ? 1 : -1;
        else                             zOff = dz > 0 ? 1 : -1;

        BlockPos primary = anchor.add(xOff, 0, zOff);
        if (!smartProtection.getValue()) return primary;
        if (!isReplaceable(primary)) {
            // try orthogonal axis
            if (xOff != 0) { xOff = 0; zOff = dz > 0 ? 1 : -1; }
            else            { xOff = dx > 0 ? 1 : -1; zOff = 0; }
            BlockPos secondary = anchor.add(xOff, 0, zOff);
            if (isReplaceable(secondary)) return secondary;
        }
        return primary;
    }

    private boolean isReplaceable(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable();
    }

    private void placeAt(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (!silentSwing.getValue() && res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void interactAt(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (!silentSwing.getValue() && res.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void selectSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private int findHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private void reset() {
        currentPhase  = Phase.IDLE;
        delayClock    = 0;
        chargesPlaced = 0;
        anchorPos     = null;
        originalSlot  = -1;
    }

    private enum Phase { IDLE, PLACE_ANCHOR, PLACE_PROTECTION, CHARGE_ANCHOR, EXPLODE, DONE }
}
