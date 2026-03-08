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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class AutoHitCrystal extends Module {

    private final BooleanSetting workWithTotem  = new BooleanSetting("Work With Totem",      true);
    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation",    true);
    private final NumberSetting  placeDelay      = new NumberSetting("Obsidian Place Delay",  0, 10, 0, 1);
    private final NumberSetting  switchDelay     = new NumberSetting("Switch Delay",          0, 10, 0, 1);
    private final KeybindSetting activateKey     = new KeybindSetting("Activate Key", GLFW.GLFW_MOUSE_BUTTON_2, false);

    private int placeClock;
    private int switchClock;
    private boolean crystalling;
    private boolean selectedCrystal;
    private boolean activated;

    public AutoHitCrystal() {
        super("Auto Hit Crystal", "Places obsidian then automatically places and hits end crystals", -1, Category.COMBAT);
        addSettings(workWithTotem, clickSimulation, placeDelay, switchDelay, activateKey);
    }

    @Override
    public void onEnable() { resetState(); }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) { resetState(); return; }

        boolean keyDown = KeyUtils.isKeyPressed(activateKey.getKeyCode());
        if (!keyDown) { resetState(); return; }

        var mainHand = mc.player.getMainHandStack();

        // When bound to RMB, only activate when holding sword or totem
        if (activateKey.getKeyCode() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            boolean holdingValid = (mainHand.isIn(ItemTags.SWORDS))
                    || (workWithTotem.getValue() && mainHand.isOf(Items.TOTEM_OF_UNDYING));
            if (!holdingValid && !activated) return;
            activated = true;
        }

        // ── Phase 1: place obsidian ───────────────────────────────────────
        if (!crystalling) {
            if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;
            if (blockHit.getType() == HitResult.Type.MISS) return;
            if (mc.world.getBlockState(blockHit.getBlockPos()).getBlock() instanceof SignBlock) return;

            if (!mc.world.getBlockState(blockHit.getBlockPos()).isOf(Blocks.OBSIDIAN)) {
                mc.options.useKey.setPressed(false);

                if (!mainHand.isOf(Items.OBSIDIAN)) {
                    if (switchClock > 0) { switchClock--; return; }
                    if (!swapToItem(Items.OBSIDIAN)) return;
                    switchClock = switchDelay.getValueInt();
                }
                if (placeClock > 0) { placeClock--; return; }

                if (clickSimulation.getValue()) ClickSimulator.leftClick();

                ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);

                placeClock = placeDelay.getValueInt();
                crystalling = true;
                return;
            }
        }

        // ── Phase 2: on obsidian or crystal — place/hit crystals ──────────
        boolean onObsidian = (mc.crosshairTarget instanceof BlockHitResult bh)
                && mc.world.getBlockState(bh.getBlockPos()).isOf(Blocks.OBSIDIAN);
        boolean onCrystal = (mc.crosshairTarget instanceof EntityHitResult eh)
                && (eh.getEntity() instanceof EndCrystalEntity);

        if (crystalling || onObsidian || onCrystal) {
            crystalling = true;

            // Directly hit the crystal
            if (onCrystal) {
                ((MinecraftClientAccessor) mc).invokeDoAttack();
                resetState();
                return;
            }

            // Select end crystal before placing
            if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && !selectedCrystal) {
                if (switchClock > 0) { switchClock--; return; }
                selectedCrystal = swapToItem(Items.END_CRYSTAL);
                switchClock = switchDelay.getValueInt();
                if (!selectedCrystal) return;
            }

            // Place crystal on obsidian
            if (mc.crosshairTarget instanceof BlockHitResult bh) {
                ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bh);
                if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private boolean swapToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    private void resetState() {
        placeClock      = 0;
        switchClock     = 0;
        crystalling     = false;
        selectedCrystal = false;
        activated       = false;
    }
}
