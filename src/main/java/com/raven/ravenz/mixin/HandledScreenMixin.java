package com.raven.ravenz.mixin;

import com.raven.ravenz.RavenZClient;
import com.raven.ravenz.module.modules.render.ContainerSlots;
import com.raven.ravenz.utils.render.font.FontManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.raven.ravenz.RavenZClient.mc;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Inject(method = "drawSlot", at = @At("TAIL"))
    public void postDrawSlot(DrawContext context, Slot slot, int x, int y, CallbackInfo ci) {
        if (!RavenZClient.INSTANCE.moduleManager.getModule(ContainerSlots.class).get().isEnabled()) return;

        if (ContainerSlots.highlightTotem.getValue() && slot.hasStack()) {
            if (slot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, ContainerSlots.highlightColor.getValue().getRGB());
            }
        }

        if (ContainerSlots.disableText.getValue()) return;

        if (ContainerSlots.fontMode.isMode("Inter")) {
            RavenZClient.INSTANCE.fontManager
                    .getSize(10, FontManager.Type.Inter)
                    .drawString(new MatrixStack(), String.valueOf(slot.getIndex()), slot.x, slot.y, ContainerSlots.color.getValue());
        } else {
            context.drawText(
                    mc.textRenderer,
                    String.valueOf(slot.getIndex()),
                    slot.x,
                    slot.y,
                    ContainerSlots.color.getValue().getRGB(),
                    false
            );
        }
    }
}
