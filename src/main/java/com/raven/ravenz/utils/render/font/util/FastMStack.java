package com.raven.ravenz.utils.render.font.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class FastMStack extends MatrixStack {
    private static final MethodHandle MATRIXSTACK_ENTRY_CTOR;

    static {
        MethodHandle ctor = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(MatrixStack.Entry.class, MethodHandles.lookup());
            ctor = lookup.findConstructor(
                    MatrixStack.Entry.class,
                    MethodType.methodType(void.class, Matrix4f.class, Matrix3f.class)
            );
        } catch (Throwable ignored) {
        }
        MATRIXSTACK_ENTRY_CTOR = ctor;
    }

    private final boolean fastPath;
    private final ObjectArrayList<Entry> entries;
    private Entry top;

    public FastMStack() {
        super();
        this.fastPath = MATRIXSTACK_ENTRY_CTOR != null;
        this.entries = new ObjectArrayList<>(8);

        if (fastPath) {
            this.top = new Entry(new Matrix4f(), new Matrix3f());
            this.entries.add(this.top);
        }
    }

    @Override
    public void translate(float x, float y, float z) {
        if (!fastPath) {
            super.translate(x, y, z);
            return;
        }
        top.positionMatrix.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        if (!fastPath) {
            super.scale(x, y, z);
            return;
        }

        top.positionMatrix.scale(x, y, z);
        if (x == y && y == z) {
            if (x != 0.0f) {
                top.normalMatrix.scale(Math.signum(x));
            }
            return;
        }

        float inverseX = 1.0f / x;
        float inverseY = 1.0f / y;
        float inverseZ = 1.0f / z;
        float scalar = (float) (1.0f / Math.cbrt(inverseX * inverseY * inverseZ));
        top.normalMatrix.scale(scalar * inverseX, scalar * inverseY, scalar * inverseZ);
    }

    public void multiply(Quaternionf quaternion) {
        if (!fastPath) {
            super.multiply(quaternion);
            return;
        }
        top.positionMatrix.rotate(quaternion);
        top.normalMatrix.rotate(quaternion);
    }

    public void multiply(Quaternionf quaternion, float originX, float originY, float originZ) {
        if (!fastPath) {
            super.multiply(quaternion, originX, originY, originZ);
            return;
        }
        top.positionMatrix.rotateAround(quaternion, originX, originY, originZ);
        top.normalMatrix.rotate(quaternion);
    }

    public void multiplyPositionMatrix(Matrix4f matrix) {
        if (!fastPath) {
            super.multiplyPositionMatrix(matrix);
            return;
        }
        top.positionMatrix.mul(matrix);
    }

    @Override
    public void push() {
        if (!fastPath) {
            super.push();
            return;
        }
        top = new Entry(new Matrix4f(top.positionMatrix), new Matrix3f(top.normalMatrix));
        entries.add(top);
    }

    @Override
    public void pop() {
        if (!fastPath) {
            super.pop();
            return;
        }

        if (entries.size() == 1) {
            throw new IllegalStateException("Trying to pop an empty stack");
        }
        entries.pop();
        top = entries.top();
    }

    @Override
    public MatrixStack.Entry peek() {
        if (!fastPath) {
            return super.peek();
        }

        try {
            return (MatrixStack.Entry) MATRIXSTACK_ENTRY_CTOR.invoke(top.positionMatrix, top.normalMatrix);
        } catch (Throwable e) {
            return super.peek();
        }
    }

    @Override
    public boolean isEmpty() {
        if (!fastPath) {
            return super.isEmpty();
        }
        return entries.size() == 1;
    }

    @Override
    public void loadIdentity() {
        if (!fastPath) {
            super.loadIdentity();
            return;
        }
        top.positionMatrix.identity();
        top.normalMatrix.identity();
    }

    private record Entry(Matrix4f positionMatrix, Matrix3f normalMatrix) {
    }
}
