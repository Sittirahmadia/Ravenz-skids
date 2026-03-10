package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.AttackEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.HitResult;

public final class AntiMiss extends Module {
    public AntiMiss() {
        super("Anti Miss", "Makes you not miss", -1, Category.COMBAT);
    }

    @EventHandler
    private void onAttackEvent(AttackEvent event) {
        if (isNull()) return;

        assert mc.crosshairTarget != null;
        if (mc.crosshairTarget.getType().equals(HitResult.Type.MISS)) {
            event.cancel();
        }
    }
}
