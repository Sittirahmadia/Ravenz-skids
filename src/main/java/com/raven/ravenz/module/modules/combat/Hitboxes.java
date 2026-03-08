package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;

public class Hitboxes extends Module {

    private final ModeSetting mode = new ModeSetting(
            "Expand Mode", "Blatant", "Blatant", "Legit");

    // ── Target selection ───────────────────────────────────────────────────
    private final BooleanSetting onlyWeapon     = new BooleanSetting("Only Weapon",    false);
    private final BooleanSetting targetCrystals = new BooleanSetting("Target Crystals", false);

    // ── Per-weapon expansion ───────────────────────────────────────────────
    private final NumberSetting swordExpand    = new NumberSetting("Sword Expand",    0.0, 1.0, 0.1, 0.01);
    private final NumberSetting axeExpand      = new NumberSetting("Axe Expand",      0.0, 1.0, 0.1, 0.01);
    private final NumberSetting maceExpand     = new NumberSetting("Mace Expand",     0.0, 1.0, 0.1, 0.01);
    private final NumberSetting tridentExpand  = new NumberSetting("Trident Expand",  0.0, 1.0, 0.1, 0.01);
    private final NumberSetting expandAmount   = new NumberSetting("Expand Amount",   0.0, 1.0, 0.1, 0.01);
    private final NumberSetting crystalExpand  = new NumberSetting("Crystal Expand",  0.0, 1.0, 0.3, 0.01);

    // ── Reach ──────────────────────────────────────────────────────────────
    private final BooleanSetting reachEnabled  = new BooleanSetting("Reach",          false);
    private final NumberSetting  reachExpand   = new NumberSetting("Reach Expand",    0.0, 6.0, 0.5, 0.05);

    public Hitboxes() {
        super("Hitboxes", "Expands the hitboxes of other players and end crystals", -1, Category.COMBAT);
        addSettings(mode, onlyWeapon, targetCrystals,
                swordExpand, axeExpand, maceExpand, tridentExpand, expandAmount, crystalExpand,
                reachEnabled, reachExpand);
    }

    /**
     * Returns the hitbox expansion to apply for {@code entity}.
     * Returns 0 if no expansion should be applied.
     * Called from EntityMixin on getBoundingBox.
     */
    public double getExpansionFor(Entity entity) {
        if (mc.player == null) return 0;
        if (entity == mc.player) return 0;

        // End crystal expansion
        if (entity instanceof EndCrystalEntity) {
            return targetCrystals.getValue() ? crystalExpand.getValue() : 0;
        }

        if (!(entity instanceof PlayerEntity)) return 0;

        var stack = mc.player.getMainHandStack();
        Item held = stack.getItem();

        if (stack.isIn(ItemTags.SWORDS)) return swordExpand.getValue();
        if (held instanceof AxeItem)     return axeExpand.getValue();
        if (held instanceof MaceItem)    return maceExpand.getValue();
        if (held instanceof TridentItem) return tridentExpand.getValue();

        if (onlyWeapon.getValue()) return 0;
        return expandAmount.getValue();
    }

    /** True when Blatant mode: expands all three axes uniformly. */
    public boolean isBlatant() {
        return mode.isMode("Blatant");
    }

    /** Returns extra reach to add when Reach is enabled, or 0 if disabled. */
    public double getReachExpand() {
        if (!isEnabled() || !reachEnabled.getValue()) return 0;
        return reachExpand.getValue();
    }

    /** Convenience accessor for EntityMixin. */
    public static Hitboxes getInstance() {
        if (RavenZClient.INSTANCE == null) return null;
        return RavenZClient.INSTANCE.getModuleManager()
                .getModule(Hitboxes.class)
                .orElse(null);
    }
}
