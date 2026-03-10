package com.raven.ravenz.utils.render.blur;

import java.awt.Color;

public final class BlurBuilder {
    private SizeState size = SizeState.NONE;
    private QuadRadiusState radius = QuadRadiusState.NO_ROUND;
    private QuadColorState color = QuadColorState.WHITE;
    private float smoothness = 1.0f;
    private float blurRadius = 0.0f;

    public BlurBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public BlurBuilder size(float width, float height) {
        this.size = new SizeState(width, height);
        return this;
    }

    public BlurBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public BlurBuilder radius(float radius) {
        this.radius = new QuadRadiusState(radius);
        return this;
    }

    public BlurBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public BlurBuilder color(Color color) {
        this.color = new QuadColorState(color);
        return this;
    }

    public BlurBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public BlurBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }

    public BuiltBlur build() {
        BuiltBlur blur = new BuiltBlur(size, radius, color, smoothness, blurRadius);
        reset();
        return blur;
    }

    private void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0f;
        this.blurRadius = 0.0f;
    }

    public static BlurBuilder create() {
        return new BlurBuilder();
    }
}
