package com.raven.ravenz.utils.render.font.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;

public final class BufferUtils {
    private BufferUtils() {
    }

    public static void draw(BufferBuilder builder) {
        try (BuiltBuffer builtBuffer = builder.end()) {
            try {
                selectDefaultLayer(builtBuffer.getDrawParameters().mode()).draw(builtBuffer);
            } catch (Throwable ignored) {
                try {
                    Class<?> bufferRenderer = Class.forName("net.minecraft.client.render.BufferRenderer");
                    bufferRenderer.getMethod("drawWithGlobalProgram", BuiltBuffer.class).invoke(null, builtBuffer);
                } catch (Throwable ignored2) {
                }
            }
        }
    }

    public static void draw(BufferBuilder builder, RenderLayer layer) {
        try (BuiltBuffer builtBuffer = builder.end()) {
            layer.draw(builtBuffer);
        }
    }

    private static RenderLayer selectDefaultLayer(VertexFormat.DrawMode mode) {
        return switch (mode) {
            case DEBUG_LINES -> RenderLayers.linesTranslucent();
            case TRIANGLES -> RenderLayers.debugFilledBox();
            case TRIANGLE_FAN -> RenderLayers.debugTriangleFan();
            case QUADS -> RenderLayers.debugQuads();
            default -> RenderLayers.debugQuads();
        };
    }
}
