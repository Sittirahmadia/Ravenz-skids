package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.modules.misc.Teams;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.utils.friend.FriendManager;
import com.raven.ravenz.utils.keybinding.simulation.ClickSimulator;
import com.raven.ravenz.utils.math.MathUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import com.raven.ravenz.utils.mc.CombatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class TriggerBotV3 extends Module {

    // ── Mode ──────────────────────────────────────────────────────────────
    /**
     * Delay    — waits a random ms window (humanlike variance).
     * Cooldown — fires when vanilla attack cooldown hits the threshold.
     */
    private final ModeSetting attackMode = new ModeSetting("Attack Mode", "Delay", "Delay", "Cooldown");

    // ── Activation ────────────────────────────────────────────────────────
    private final BooleanSetting inScreen      = new BooleanSetting("Work In Screen",   false);
    private final BooleanSetting whileUse      = new BooleanSetting("While Use",        false);
    private final BooleanSetting onLeftClick   = new BooleanSetting("On Left Click",    false);
    private final BooleanSetting allItems      = new BooleanSetting("All Items",        false);

    // ── Timing — DELAY mode ───────────────────────────────────────────────
    private final RangeSetting   swordDelay    = new RangeSetting("Sword Delay",  0, 1000, 540, 550, 1);
    private final RangeSetting   axeDelay      = new RangeSetting("Axe Delay",    0, 1000, 780, 800, 1);

    // ── Timing — COOLDOWN mode ────────────────────────────────────────────
    /**
     * Attack fires when getAttackCooldownProgress() >= this threshold.
     * 1.0 = fully charged (same as vanilla). Lower = attack before full charge.
     * Range 0.1 – 1.0 in steps of 0.01.
     */
    private final NumberSetting  swordThreshold = new NumberSetting("Sword Threshold", 0.1, 1.0, 1.0, 0.01);
    private final NumberSetting  axeThreshold   = new NumberSetting("Axe Threshold",   0.1, 1.0, 1.0, 0.01);

    // ── Targeting ─────────────────────────────────────────────────────────
    private final BooleanSetting allEntities   = new BooleanSetting("All Entities",     false);
    private final BooleanSetting strayBypass   = new BooleanSetting("Stray Bypass",     false);
    private final BooleanSetting sticky        = new BooleanSetting("Same Player",      false);
    private final BooleanSetting checkShield   = new BooleanSetting("Check Shield",     false);

    // ── Crit ──────────────────────────────────────────────────────────────
    private final BooleanSetting onlyCritSword = new BooleanSetting("Only Crit Sword",  false);
    private final BooleanSetting onlyCritAxe   = new BooleanSetting("Only Crit Axe",    false);
    private final BooleanSetting whileAscend   = new BooleanSetting("While Ascending",  false);

    // ── Actions ───────────────────────────────────────────────────────────
    private final BooleanSetting swing         = new BooleanSetting("Swing Hand",       true);
    private final BooleanSetting clickSim      = new BooleanSetting("Click Simulation", false);
    private final BooleanSetting useShield     = new BooleanSetting("Use Shield",       false);
    private final NumberSetting  shieldTime    = new NumberSetting("Shield Time",       100, 1000, 350, 1);

    // ── State ─────────────────────────────────────────────────────────────
    private final TimerUtil timer = new TimerUtil();
    private long currentSwordDelay;
    private long currentAxeDelay;

    public TriggerBotV3() {
        super("Trigger Bot V3", "Automatically hits players for you", -1, Category.COMBAT);
        addSettings(
                attackMode,
                inScreen, whileUse, onLeftClick, allItems,
                swordDelay, axeDelay,
                swordThreshold, axeThreshold,
                allEntities, strayBypass, sticky, checkShield,
                onlyCritSword, onlyCritAxe, whileAscend,
                swing, clickSim, useShield, shieldTime
        );
    }

    @Override
    public void onEnable() {
        randomizeDelays();
        super.onEnable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;
        if (!inScreen.getValue() && mc.currentScreen != null) return;

        // LMB gate
        if (onLeftClick.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            return;

        // While-use gate
        if (!whileUse.getValue()) {
            var offhand = mc.player.getOffHandStack().getItem();
            boolean rightHeld = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            var offhandStack = mc.player.getOffHandStack();
            boolean isFood = offhandStack.getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD);
            if (rightHeld && (isFood || offhand instanceof ShieldItem)) return;
        }

        // Ascending gate
        if (!whileAscend.getValue()) {
            boolean ascending = !mc.player.isOnGround() && mc.player.getVelocity().y > 0;
            boolean noFall    = !mc.player.isOnGround() && mc.player.fallDistance <= 0.0f;
            if (ascending || noFall) return;
        }

        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        if (hit.getType() != HitResult.Type.ENTITY) return;

        Entity entity = hit.getEntity();
        if (entity == null || entity == mc.player) return;

        if (!isValidTarget(entity)) return;

        if (sticky.getValue() && mc.player.getAttacking() != null && entity != mc.player.getAttacking())
            return;

        if (checkShield.getValue() && entity instanceof PlayerEntity player
                && player.isBlocking() && !CombatUtil.isShieldFacingAway(player))
            return;

        var item   = mc.player.getMainHandStack().getItem();
        boolean isSword = mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
        boolean isAxe   = item instanceof AxeItem;

        if (!allItems.getValue() && !isSword && !isAxe) return;

        // Crit checks
        if (isSword && onlyCritSword.getValue() && mc.player.fallDistance <= 0.0f) return;
        if (isAxe  && onlyCritAxe.getValue()   && mc.player.fallDistance <= 0.0f) return;

        // ── Mode dispatch ──────────────────────────────────────────────────
        if (attackMode.isMode("Delay")) {
            tickDelayMode(entity, isSword, isAxe);
        } else {
            tickCooldownMode(entity, isSword, isAxe);
        }
    }

    // ── DELAY mode ─────────────────────────────────────────────────────────

    private void tickDelayMode(Entity entity, boolean isSword, boolean isAxe) {
        long delay = isAxe && !allItems.getValue() ? currentAxeDelay : currentSwordDelay;

        if (!timer.hasElapsedTime(delay, false)) {
            handleShieldHold();
            return;
        }

        dropShieldIfNeeded();
        performAttack(entity);
        timer.reset();
        randomizeDelays();
    }

    // ── COOLDOWN mode ──────────────────────────────────────────────────────

    /**
     * Attacks when the vanilla attack cooldown progress meets the threshold.
     * getAttackCooldownProgress(0f) returns 0.0 (just attacked) → 1.0 (fully ready).
     */
    private void tickCooldownMode(Entity entity, boolean isSword, boolean isAxe) {
        float cooldown  = mc.player.getAttackCooldownProgress(0f);
        float threshold = (float) (isAxe && !allItems.getValue()
                ? axeThreshold.getValue()
                : swordThreshold.getValue());

        if (cooldown < threshold) {
            handleShieldHold();
            return;
        }

        dropShieldIfNeeded();
        performAttack(entity);
        // No timer.reset() needed — the game resets its own cooldown on attack
    }

    // ── Shared attack logic ────────────────────────────────────────────────

    private void performAttack(Entity entity) {
        dropShieldIfNeeded();

        if (swing.getValue()) {
            ((MinecraftClientAccessor) mc).invokeDoAttack();
        } else {
            mc.player.attack(entity);
        }

        if (clickSim.getValue()) ClickSimulator.leftClick();
    }

    private void dropShieldIfNeeded() {
        if (useShield.getValue() && mc.player.getOffHandStack().getItem() instanceof ShieldItem
                && mc.player.isBlocking()) {
            simulateMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT, false);
        }
    }

    // Cancel bounce attack when LMB not held
    @EventHandler
    private void onAttack(AttackEvent event) {
        if (isNull()) return;
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            event.cancel();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity instanceof EndCrystalEntity) return false;
        if (Teams.isTeammate(entity)) return false;
        if (entity instanceof PlayerEntity p && FriendManager.isFriend(p.getUuid())) return false;

        if (allEntities.getValue()) return true;
        if (strayBypass.getValue() && entity instanceof ZombieEntity) return true;
        return entity instanceof PlayerEntity;
    }

    private void handleShieldHold() {
        if (!useShield.getValue()) return;
        if (mc.player.getOffHandStack().getItem() instanceof ShieldItem && !mc.player.isBlocking()) {
            simulateMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT, true);
        }
    }

    private void simulateMouse(int button, boolean press) {
        var key = net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(button);
        net.minecraft.client.option.KeyBinding.setKeyPressed(key, press);
        if (press) net.minecraft.client.option.KeyBinding.onKeyPressed(key);
    }

    private void randomizeDelays() {
        currentSwordDelay = (long) MathUtils.randomDoubleBetween(swordDelay.getMinValue(), swordDelay.getMaxValue());
        currentAxeDelay   = (long) MathUtils.randomDoubleBetween(axeDelay.getMinValue(),   axeDelay.getMaxValue());
    }
}
