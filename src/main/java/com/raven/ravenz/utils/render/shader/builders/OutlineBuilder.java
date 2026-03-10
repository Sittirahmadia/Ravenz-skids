package com.raven.ravenz.utils.render.shader.builders;

import com.raven.ravenz.utils.render.shader.AbstractBuilder;
import com.raven.ravenz.utils.render.shader.renderers.BuiltOutline;
import com.raven.ravenz.utils.render.shader.states.QuadColorState;
import com.raven.ravenz.utils.render.shader.states.QuadRadiusState;
import com.raven.ravenz.utils.render.shader.states.SizeState;

public final class OutlineBuilder extends AbstractBuilder<BuiltOutline> {
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float thickness;
    private float smoothness;

    public OutlineBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public OutlineBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public OutlineBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public OutlineBuilder thickness(float thickness) {
        this.thickness = thickness;
        return this;
    }

    public OutlineBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    @Override
    protected BuiltOutline _build() {
        return new BuiltOutline(this.size, this.radius, this.color, this.thickness, this.smoothness);
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.thickness = 1.0f;
        this.smoothness = 1.0f;
    }
}
