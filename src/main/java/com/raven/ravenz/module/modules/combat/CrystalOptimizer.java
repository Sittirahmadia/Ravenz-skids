package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.event.impl.player.DoAttackEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class CrystalOptimizer extends Module {

    public CrystalOptimizer() {
        super("Crystal Optimizer", "Makes crystals disappear faster client-side for quicker placement.", -1, Category.COMBAT);
    }

    @EventHandler
    private void onAttackEvent(DoAttackEvent event) {
        if (isNull()) return;
        if (mc.crosshairTarget == null) return;

        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;

        Entity target = hit.getEntity();
        if (!(target instanceof EndCrystalEntity crystal)) return;

        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        ItemStack mainHand = mc.player.getMainHandStack();

        boolean canAttack =
                (weakness == null)
                        || (strength != null && strength.getAmplifier() > weakness.getAmplifier())
                        || mainHand.isIn(ItemTags.SWORDS)
                        || mainHand.isIn(ItemTags.AXES)
                        || mainHand.isIn(ItemTags.PICKAXES)
                        || mainHand.isIn(ItemTags.SHOVELS)
                        || mainHand.isIn(ItemTags.HOES);

        if (!canAttack) return;

        crystal.setRemoved(Entity.RemovalReason.KILLED);
        crystal.onRemoved();
    }
}
