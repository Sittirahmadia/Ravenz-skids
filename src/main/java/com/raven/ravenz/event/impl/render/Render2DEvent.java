package com.raven.ravenz.event.impl.render;

import com.raven.ravenz.event.types.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;

@Getter
@Setter
public class Render2DEvent implements Event {
    private DrawContext context;
    private int width;
    private int height;

    public Render2DEvent(DrawContext context, int width, int height) {
        this.context = context;
        this.width = width;
        this.height = height;
    }

}
