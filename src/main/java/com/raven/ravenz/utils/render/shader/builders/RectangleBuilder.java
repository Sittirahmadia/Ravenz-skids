package com.raven.ravenz.utils.render.shader.builders;

import com.raven.ravenz.utils.render.shader.AbstractBuilder;
import com.raven.ravenz.utils.render.shader.renderers.BuiltRectangle;
import com.raven.ravenz.utils.render.shader.states.QuadColorState;
import com.raven.ravenz.utils.render.shader.states.QuadRadiusState;
import com.raven.ravenz.utils.render.shader.states.SizeState;

public final class RectangleBuilder extends AbstractBuilder<BuiltRectangle> {
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;

    public RectangleBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public RectangleBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public RectangleBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public RectangleBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    @Override
    protected BuiltRectangle _build() {
        return new BuiltRectangle(this.size, this.radius, this.color, this.smoothness);
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.smoothness = 1.0f;
    }
}
