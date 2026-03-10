package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.input.HandleInputEvent;
import com.raven.ravenz.event.impl.world.WorldChangeEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.modules.misc.Teams;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.utils.friend.FriendManager;
import com.raven.ravenz.utils.math.MathUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import com.raven.ravenz.utils.mc.CombatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public final class TriggerBot extends Module {

    // ── Timing ─────────────────────────────────────────────────────────────
    private final RangeSetting swordThreshold = new RangeSetting("Sword Threshold", 0.1, 1.0, 0.85, 0.95, 0.01);
    private final RangeSetting axeThreshold   = new RangeSetting("Axe Threshold",   0.1, 1.0, 0.85, 0.95, 0.01);
    private final RangeSetting axePostDelay   = new RangeSetting("Axe Post Delay",  0,   500, 80,   140,  0.5);
    private final RangeSetting reactionTime   = new RangeSetting("Reaction Time",   0,   350, 15,   80,   0.5);
    private final RangeSetting missChance     = new RangeSetting("Miss Chance %",   0,   100, 0,    0,    1);

    // ── Crit ───────────────────────────────────────────────────────────────
    private final ModeSetting critMode = new ModeSetting("Criticals", "Strict", "None", "Strict", "Prefer");

    // ── Target Filters ─────────────────────────────────────────────────────
    private final ModeSetting    targetFilter    = new ModeSetting("Target Filter",    "Players", "Players", "Mobs", "Animals", "All");
    private final BooleanSetting ignorePassive   = new BooleanSetting("No Passive",    true);
    private final BooleanSetting ignoreCrystals  = new BooleanSetting("No Crystals",   true);
    private final BooleanSetting ignoreInvis     = new BooleanSetting("No Invisible",  true);
    private final BooleanSetting skipTeammates   = new BooleanSetting("No Teammates",  true);
    private final BooleanSetting skipFriends     = new BooleanSetting("No Friends",    true);

    // ── Combat Options ─────────────────────────────────────────────────────
    private final BooleanSetting respectShields    = new BooleanSetting("Ignore Shielding",  false);
    private final BooleanSetting useOnlySwordOrAxe = new BooleanSetting("Only Sword or Axe", true);
    private final BooleanSetting requireLMB        = new BooleanSetting("Only Mouse Hold",   false);
    private final BooleanSetting sameTarget        = new BooleanSetting("Same Target Lock",  false);
    private final NumberSetting  sameTargetWindow  = new NumberSetting("Lock Window (s)",    1, 30, 5, 1);

    // ── Misc ───────────────────────────────────────────────────────────────
    private final BooleanSetting disableOnLoad = new BooleanSetting("Disable on Load", false);

    // ── State ──────────────────────────────────────────────────────────────
    private final TimerUtil reactionTimer = new TimerUtil();
    private final TimerUtil axeTimer      = new TimerUtil();
    private final TimerUtil lockTimer     = new TimerUtil();
    private final TimerUtil missTimer     = new TimerUtil();

    private boolean waitingForReaction = false;
    private boolean waitingForAxeDelay = false;
    private long    currentReactionMs  = 0;
    private float   cachedAxeThreshold = 0;
    private float   cachedAxePostDelay = 0;
    private float   cachedSwordThreshold = 0;
    private String  lockedTargetUUID   = null;
    private Entity  currentTarget      = null;

    // Crit prediction state
    private boolean critWindowOpen     = false;
    private long    critWindowStart    = 0;
    private static final long CRIT_WINDOW_MS = 80; // ms to wait for crit opportunity

    public boolean waitingForDelay = false; // compat

    public TriggerBot() {
        super("Trigger Bot", "Makes you automatically attack once aimed at a target", -1, Category.PLAYER);
        addSettings(
                swordThreshold, axeThreshold, axePostDelay,
                reactionTime, missChance,
                critMode,
                targetFilter, ignorePassive, ignoreCrystals, ignoreInvis,
                skipTeammates, skipFriends,
                respectShields, useOnlySwordOrAxe, requireLMB,
                sameTarget, sameTargetWindow,
                disableOnLoad
        );
    }

    // ── Events ─────────────────────────────────────────────────────────────

    @EventHandler
    private void onWorldChange(WorldChangeEvent event) {
        if (disableOnLoad.getValue() && isEnabled()) toggle();
    }

    @EventHandler
    private void onInput(HandleInputEvent event) {
        if (isNull() || mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;

        if (requireLMB.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            return;

        if (!passesWeaponFilter()) return;

        Entity target = getTarget();

        // No target — reset reaction but keep axe timing
        if (target == null) {
            if (waitingForReaction) {
                waitingForReaction = false;
                critWindowOpen = false;
            }
            currentTarget = null;
            return;
        }

        // Target changed — full reset
        if (target != currentTarget) {
            currentTarget      = target;
            waitingForReaction = false;
            waitingForAxeDelay = false;
            critWindowOpen     = false;
            cachedSwordThreshold = randomizeThreshold(swordThreshold);
        }

        // Same target lock
        if (sameTarget.getValue()) {
            long windowMs = (long) (sameTargetWindow.getValue() * 1000);
            if (lockedTargetUUID == null || lockTimer.hasElapsedTime(windowMs, false)) {
                lockedTargetUUID = target.getUuidAsString();
                lockTimer.reset();
            } else if (!target.getUuidAsString().equals(lockedTargetUUID)) {
                return;
            }
        }

        // Start reaction timer
        if (!waitingForReaction) {
            waitingForReaction = true;
            reactionTimer.reset();
            currentReactionMs = computeReactionDelay(target);
            return;
        }

        if (!reactionTimer.hasElapsedTime(currentReactionMs, false)) return;

        // ── Timing gate ──────────────────────────────────────────────────
        if (!timingOk()) return;

        // ── Crit logic ───────────────────────────────────────────────────
        String crit = critMode.getMode();
        if (!crit.equals("None") && !isInBadState()) {
            if (crit.equals("Strict")) {
                // Must already be in crit state — no waiting
                if (!canCrit()) return;
            } else if (crit.equals("Prefer")) {
                // Open a short window to catch a crit; attack if window expires
                if (canCrit()) {
                    // Already critting — fire immediately
                    critWindowOpen = false;
                } else if (!critWindowOpen) {
                    // Start waiting for crit window
                    critWindowOpen = true;
                    critWindowStart = System.currentTimeMillis();
                    return;
                } else if (System.currentTimeMillis() - critWindowStart < CRIT_WINDOW_MS) {
                    // Still inside window — keep waiting
                    return;
                }
                // Window expired — attack anyway
                critWindowOpen = false;
            }
        } else {
            critWindowOpen = false;
        }

        // ── Fire ─────────────────────────────────────────────────────────
        if (sameTarget.getValue() && target != null) {
            lockedTargetUUID = target.getUuidAsString();
            lockTimer.reset();
        }

        performAttack();
        waitingForReaction = false;
        waitingForDelay    = false;
        critWindowOpen     = false;
        // Re-randomize sword threshold for next hit
        cachedSwordThreshold = randomizeThreshold(swordThreshold);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Entity getTarget() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return null;
        Entity e = hit.getEntity();
        if (!isValidTarget(e)) return null;
        return e;
    }

    private boolean isValidTarget(Entity e) {
        if (e == null || e == mc.player || e == mc.cameraEntity) return false;
        if (!e.isAlive()) return false;
        if (e instanceof WindChargeEntity) return false;
        if (ignoreCrystals.getValue() && e instanceof EndCrystalEntity) return false;
        if (e instanceof Tameable) return false;
        if (ignorePassive.getValue() && e instanceof PassiveEntity) return false;
        if (ignoreInvis.getValue() && e.isInvisible()) return false;
        if (skipTeammates.getValue() && Teams.isTeammate(e)) return false;
        if (skipFriends.getValue() && e instanceof PlayerEntity p && FriendManager.isFriend(p.getUuid())) return false;

        if (e instanceof LivingEntity le) {
            boolean passes = switch (targetFilter.getMode()) {
                case "Players" -> le instanceof PlayerEntity;
                case "Mobs"    -> le instanceof MobEntity;
                case "Animals" -> le instanceof AnimalEntity;
                default        -> true;
            };
            if (!passes) return false;
        }

        if (respectShields.getValue() && e instanceof PlayerEntity pe
                && pe.isBlocking() && pe.isHolding(Items.SHIELD)
                && CombatUtil.isShieldFacingAway(pe)
                && mc.player.getMainHandStack().isIn(ItemTags.SWORDS))
            return false;

        return true;
    }

    private boolean passesWeaponFilter() {
        if (!useOnlySwordOrAxe.getValue()) return true;
        var stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.getItem() instanceof AxeItem;
    }

    private long computeReactionDelay(Entity target) {
        double raw = MathUtils.randomDoubleBetween(reactionTime.getMinValue(), reactionTime.getMaxValue());
        double dist = mc.player.distanceTo(target);
        // Closer targets feel faster — scale reaction slightly with distance
        if      (dist < 1.5) raw *= 0.65;
        else if (dist < 2.5) raw *= 0.80;
        else if (dist < 3.5) raw *= 0.92;
        return Math.max(0, (long) raw);
    }

    private boolean timingOk() {
        float cooldown = mc.player.getAttackCooldownProgress(0f);
        var item = mc.player.getMainHandStack().getItem();

        if (item instanceof AxeItem) {
            if (!waitingForAxeDelay) {
                cachedAxeThreshold = randomizeThreshold(axeThreshold);
                cachedAxePostDelay = (float) MathUtils.randomDoubleBetween(axePostDelay.getMinValue(), axePostDelay.getMaxValue());
                waitingForAxeDelay = true;
            }
            if (cooldown >= cachedAxeThreshold) {
                if (axeTimer.hasElapsedTime((long) cachedAxePostDelay, false)) {
                    waitingForAxeDelay = false;
                    return true;
                }
                // Post delay counting — don't reset axe timer
            } else {
                // Cooldown hasn't hit threshold yet — keep resetting post-delay timer
                axeTimer.reset();
            }
            return false;
        }

        // Sword / other — use per-target cached threshold so it doesn't re-roll mid cooldown
        return cooldown >= cachedSwordThreshold;
    }

    private float randomizeThreshold(RangeSetting setting) {
        return (float) MathUtils.randomDoubleBetween(setting.getMinValue(), setting.getMaxValue());
    }

    private boolean canCrit() {
        if (mc.player == null) return false;
        return !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && mc.player.fallDistance > 0.05f
                && mc.player.getVehicle() == null;
    }

    private boolean isInBadState() {
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isSubmergedInWater()) return true;
        if (mc.player.isClimbing()) return true;
        var state = mc.world.getBlockState(mc.player.getBlockPos());
        return state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.POWDER_SNOW);
    }

    private void performAttack() {
        double missRoll = MathUtils.randomDoubleBetween(missChance.getMinValue(), missChance.getMaxValue());
        if (missRoll > 0 && Math.random() * 100 < missRoll) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            ((MinecraftClientAccessor) mc).invokeDoAttack();
        }
    }

    // ── Compat ─────────────────────────────────────────────────────────────

    public void attack() {
        performAttack();
        waitingForDelay = false;
    }

    public boolean hasTarget(Entity en) {
        return isValidTarget(en);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

    private void resetState() {
        waitingForReaction   = false;
        waitingForAxeDelay   = false;
        waitingForDelay      = false;
        critWindowOpen       = false;
        currentTarget        = null;
        lockedTargetUUID     = null;
        cachedSwordThreshold = 0;
        reactionTimer.reset();
        axeTimer.reset();
        lockTimer.reset();
        missTimer.reset();
    }
}
