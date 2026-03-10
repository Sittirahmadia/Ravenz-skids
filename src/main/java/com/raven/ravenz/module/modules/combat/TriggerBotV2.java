package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.mixin.MinecraftClientAccessor;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.utils.friend.FriendManager;
import com.raven.ravenz.utils.keybinding.simulation.ClickSimulator;
import com.raven.ravenz.utils.math.MathUtils;
import com.raven.ravenz.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * TriggerBotV2 — crosshair-based auto-attacker.
 *
 * Modes:        Cooldown | Delay | Instant
 * Target filter: Players | Mobs | Animals | All
 * Weapon filter: Any | Sword | Axe | SwordOrAxe | Mace | Trident
 */
public final class TriggerBotV2 extends Module {

    // ── Modes ──────────────────────────────────────────────────────────────
    private final ModeSetting attackMode   = new ModeSetting("Attack Mode",   "Cooldown", "Cooldown", "Delay", "Instant");
    private final ModeSetting targetFilter = new ModeSetting("Target Filter", "Players",  "Players", "Mobs", "Animals", "All");
    private final ModeSetting weaponFilter = new ModeSetting("Weapon Filter", "Any",      "Any", "Sword", "Axe", "SwordOrAxe", "Mace", "Trident");

    // ── Timing ─────────────────────────────────────────────────────────────
    private final NumberSetting cooldownPct = new NumberSetting("Cooldown %",      50, 100, 95, 1);
    private final RangeSetting  attackDelay = new RangeSetting("Attack Delay (ms)", 0, 1000, 1, 150, 50);

    // ── Filters ────────────────────────────────────────────────────────────
    private final BooleanSetting critOnly        = new BooleanSetting("Crit Only",      false);
    private final BooleanSetting skipShielding   = new BooleanSetting("Skip Shielding", true);
    private final BooleanSetting skipFriends     = new BooleanSetting("Skip Friends",   true);
    private final BooleanSetting requireClick    = new BooleanSetting("Require Click",  false);
    private final BooleanSetting healthGate      = new BooleanSetting("Health Gate",    false);
    private final NumberSetting  healthThreshold = new NumberSetting("HP Threshold",    1, 40, 20, 1);

    // ── Behaviour ──────────────────────────────────────────────────────────
    private final BooleanSetting simulateClick = new BooleanSetting("Simulate Click", false);
    private final BooleanSetting noBounce      = new BooleanSetting("No Bounce",      true);

    // ── State ──────────────────────────────────────────────────────────────
    private final TimerUtil hitTimer = new TimerUtil();
    private boolean attackedThisTick = false;

    public TriggerBotV2() {
        super("Trigger Bot V2", "Auto-attacks any entity on your crosshair", -1, Category.COMBAT);
        addSettings(
                attackMode, cooldownPct, attackDelay,
                targetFilter, weaponFilter,
                critOnly, skipShielding, skipFriends,
                requireClick, healthGate, healthThreshold,
                simulateClick, noBounce
        );
    }

    @Override
    public void onEnable() {
        hitTimer.reset();
        attackedThisTick = false;
        super.onEnable();
    }

    // ── Filter helpers ─────────────────────────────────────────────────────

    private boolean passesWeaponFilter() {
        var stack = mc.player.getMainHandStack();
        var item = stack.getItem();
        return switch (weaponFilter.getMode()) {
            case "Sword"      -> stack.isIn(ItemTags.SWORDS);
            case "Axe"        -> item instanceof AxeItem;
            case "SwordOrAxe" -> stack.isIn(ItemTags.SWORDS) || item instanceof AxeItem;
            case "Mace"       -> item instanceof MaceItem;
            case "Trident"    -> item instanceof TridentItem;
            default           -> true; // Any
        };
    }

    private boolean passesTargetFilter(LivingEntity entity) {
        return switch (targetFilter.getMode()) {
            case "Players" -> entity instanceof PlayerEntity;
            case "Mobs"    -> entity instanceof MobEntity;
            case "Animals" -> entity instanceof AnimalEntity;
            default        -> true; // All
        };
    }

    private boolean canCrit() {
        return mc.player.fallDistance > 0.0f
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && mc.player.getAttackCooldownProgress(0.5f) > 0.9f;
    }

    private boolean timingOk() {
        return switch (attackMode.getMode()) {
            case "Cooldown" -> mc.player.getAttackCooldownProgress(0f) >= cooldownPct.getValue() * 0.01f;
            case "Delay"    -> hitTimer.hasElapsedTime(
                    (long) MathUtils.randomDoubleBetween(attackDelay.getMinValue(), attackDelay.getMaxValue()));
            default         -> true; // Instant
        };
    }

    // ── Tick ───────────────────────────────────────────────────────────────

    @EventHandler
    private void onTick(TickEvent event) {
        attackedThisTick = false;

        if (isNull() || mc.currentScreen != null) return;

        if (requireClick.getValue()
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            return;

        if (!passesWeaponFilter()) return;

        if (!(mc.crosshairTarget instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;
        if (target.isDead()) return;
        if (!passesTargetFilter(target)) return;

        if (skipFriends.getValue() && target instanceof PlayerEntity player
                && FriendManager.isFriend(player.getUuid())) return;

        if (skipShielding.getValue() && target instanceof PlayerEntity player
                && player.isBlocking() && player.isHolding(Items.SHIELD)) return;

        if (critOnly.getValue() && !canCrit()) return;
        if (healthGate.getValue() && target.getHealth() > healthThreshold.getValue()) return;
        if (!timingOk()) return;
        if (attackedThisTick) return;

        if (simulateClick.getValue()) ClickSimulator.leftClick();
        ((MinecraftClientAccessor) mc).invokeDoAttack();
        hitTimer.reset();
        attackedThisTick = true;
    }

    // ── No Bounce ──────────────────────────────────────────────────────────

    @EventHandler
    private void onAttack(AttackEvent event) {
        if (mc.player == null) return;
        if (noBounce.getValue()
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            event.cancel();
        }
    }
}
