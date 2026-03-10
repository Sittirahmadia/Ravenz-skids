package com.raven.ravenz.module.modules.render;

import com.raven.ravenz.event.impl.render.Render2DEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ColorSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.render.W2SUtil;
import com.raven.ravenz.utils.render.nanovg.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Trajectories extends Module {

    private final BooleanSetting showLandingPoint = new BooleanSetting("Show Landing Point", true);
    private final BooleanSetting trackThrown = new BooleanSetting("Track Thrown", true);
    private final NumberSetting trackDuration = new NumberSetting("Track Duration (s)", 1.0, 10.0, 3.0, 0.5);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.0, 5.0, 2.0, 0.5);
    private final NumberSetting pointSize = new NumberSetting("Point Size", 2.0, 10.0, 5.0, 0.5);
    private final ColorSetting lineColor = new ColorSetting("Line Color", new Color(255, 255, 255, 200), true);
    private final ColorSetting hitColor = new ColorSetting("Hit Color", new Color(255, 50, 50, 255), true);
    private final ColorSetting thrownColor = new ColorSetting("Thrown Color", new Color(100, 200, 255, 200), true);
    private final NumberSetting maxPoints = new NumberSetting("Max Points", 50, 500, 200, 10);

    private final Map<Entity, ProjectileTracker> trackedProjectiles = new HashMap<>();

    public Trajectories() {
        super("Trajectories", "Shows projectile trajectory path", -1, Category.RENDER);
        addSettings(showLandingPoint, trackThrown, trackDuration, lineWidth, pointSize, lineColor, hitColor,
                thrownColor, maxPoints);
    }

    private static class ProjectileTracker {
        final List<Vec3d> path = new ArrayList<>();
        final long startTime = System.currentTimeMillis();

        boolean isExpired(double durationSeconds) {
            return (System.currentTimeMillis() - startTime) / 1000.0 > durationSeconds;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (isNull() || mc.player == null || mc.world == null) {
            return;
        }

        if (trackThrown.getValue()) {
            updateTrackedProjectiles();
        } else {
            trackedProjectiles.clear();
        }

        if (!NanoVGRenderer.beginFrame()) {
            return;
        }

        ItemStack heldItem = mc.player.getMainHandStack();
        if (isProjectile(heldItem) && isUsingItem(heldItem)) {
            List<Vec3d> trajectory = calculateTrajectory(heldItem);
            if (!trajectory.isEmpty()) {
                renderTrajectoryLines(trajectory, lineColor.getValue(), false);
            }
        }

        if (trackThrown.getValue()) {
            for (ProjectileTracker tracker : trackedProjectiles.values()) {
                if (!tracker.path.isEmpty()) {
                    renderTrajectoryLines(tracker.path, thrownColor.getValue(), true);
                }
            }
        }

        NanoVGRenderer.endFrame();
    }

    private boolean isUsingItem(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof BowItem) {
            return mc.player.isUsingItem() && mc.player.getItemUseTime() > 0;
        }

        if (item instanceof CrossbowItem) {
            return CrossbowItem.isCharged(stack);
        }

        if (item instanceof TridentItem) {
            return mc.player.isUsingItem() && mc.player.getItemUseTime() > 0;
        }

        if (item instanceof SnowballItem || item instanceof EggItem
                || item instanceof EnderPearlItem || item instanceof ExperienceBottleItem
                || item instanceof PotionItem) {
            return true;
        }

        if (item instanceof FishingRodItem) {
            return mc.player.isUsingItem();
        }

        return false;
    }

    private void updateTrackedProjectiles() {
        trackedProjectiles.entrySet().removeIf(entry -> entry.getValue().isExpired(trackDuration.getValue())
                || !entry.getKey().isAlive()
                || entry.getKey().isRemoved());

        for (Entity entity : mc.world.getEntities()) {
            if (isTrackableProjectile(entity) && !trackedProjectiles.containsKey(entity)) {
                if (entity.age < 5 && entity.squaredDistanceTo(mc.player) < 100.0) {
                    trackedProjectiles.put(entity, new ProjectileTracker());
                }
            }
        }

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        for (Map.Entry<Entity, ProjectileTracker> entry : trackedProjectiles.entrySet()) {
            Entity entity = entry.getKey();
            ProjectileTracker tracker = entry.getValue();
            Vec3d pos = entity.getLerpedPos(tickDelta);

            if (tracker.path.isEmpty() || tracker.path.get(tracker.path.size() - 1).squaredDistanceTo(pos) > 0.01) {
                tracker.path.add(pos);

                if (tracker.path.size() > maxPoints.getValueInt()) {
                    tracker.path.remove(0);
                }
            }
        }
    }

    private boolean isTrackableProjectile(Entity entity) {
        return entity instanceof ArrowEntity
                || entity instanceof SpectralArrowEntity
                || entity instanceof TridentEntity
                || entity instanceof SnowballEntity
                || entity instanceof EggEntity
                || entity instanceof EnderPearlEntity
                || entity instanceof ExperienceBottleEntity
                || entity instanceof PotionEntity
                || entity instanceof FishingBobberEntity;
    }

    private boolean isProjectile(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof SnowballItem
                || item instanceof EggItem
                || item instanceof EnderPearlItem
                || item instanceof ExperienceBottleItem
                || item instanceof PotionItem
                || item instanceof FishingRodItem;
    }

    private List<Vec3d> calculateTrajectory(ItemStack stack) {
        List<Vec3d> points = new ArrayList<>();
        Item item = stack.getItem();

        Vec3d pos = mc.player.getEyePos();
        Vec3d velocity = getRotationVector(mc.player.getPitch(), mc.player.getYaw());

        float power = getProjectilePower(item, stack);
        velocity = velocity.multiply(power);

        float gravity = getGravity(item);
        float drag = getDrag(item);

        int maxIterations = maxPoints.getValueInt();
        for (int i = 0; i < maxIterations; i++) {
            points.add(pos);

            Vec3d nextPos = pos.add(velocity);
            HitResult hitResult = raycast(pos, nextPos);
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                points.add(hitResult.getPos());
                break;
            }

            pos = nextPos;
            velocity = velocity.multiply(drag).add(0.0, -gravity, 0.0);

            if (velocity.lengthSquared() < 0.001) {
                break;
            }
        }

        return points;
    }

    private Vec3d getRotationVector(float pitch, float yaw) {
        float pitchRad = pitch * 0.017453292F;
        float yawRad = -yaw * 0.017453292F;
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        return new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    private float getProjectilePower(Item item, ItemStack stack) {
        if (item instanceof BowItem) {
            int useTicks = mc.player.getItemUseTime();
            float charge = BowItem.getPullProgress(useTicks);
            return charge * 3.0f;
        }
        if (item instanceof CrossbowItem) {
            return 3.15f;
        }
        if (item instanceof TridentItem) {
            return 2.5f;
        }
        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem) {
            return 1.5f;
        }
        if (item instanceof ExperienceBottleItem || item instanceof PotionItem) {
            return 1.0f;
        }
        if (item instanceof FishingRodItem) {
            return 1.5f;
        }
        return 1.5f;
    }

    private float getGravity(Item item) {
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) {
            return 0.05f;
        }
        if (item instanceof FishingRodItem) {
            return 0.04f;
        }
        return 0.03f;
    }

    private float getDrag(Item item) {
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) {
            return 0.99f;
        }
        return 0.99f;
    }

    private HitResult raycast(Vec3d start, Vec3d end) {
        BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        Box box = new Box(start, end).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                mc.world,
                mc.player,
                start,
                end,
                box,
                entity -> !entity.isSpectator() && entity != mc.player,
                0.0f
        );

        if (entityHit == null) {
            return blockHit;
        }

        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) {
            return entityHit;
        }

        double entityDistance = start.squaredDistanceTo(entityHit.getPos());
        double blockDistance = start.squaredDistanceTo(blockHit.getPos());
        return entityDistance <= blockDistance ? entityHit : blockHit;
    }

    private void renderTrajectoryLines(List<Vec3d> points, Color color, boolean isThrown) {
        if (points.size() < 2) {
            return;
        }

        float width = lineWidth.getValueFloat();

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3d start = points.get(i);
            Vec3d end = points.get(i + 1);

            Vec3d screenStart = W2SUtil.getCoords(start);
            Vec3d screenEnd = W2SUtil.getCoords(end);

            if (screenStart != null && screenEnd != null
                    && screenStart.z >= 0.0 && screenStart.z < 1.0
                    && screenEnd.z >= 0.0 && screenEnd.z < 1.0) {
                Color drawColor = color;
                if (isThrown) {
                    float alpha = (float) i / points.size();
                    drawColor = new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (color.getAlpha() * alpha)
                    );
                }

                NanoVGRenderer.drawLine(
                        (float) screenStart.x,
                        (float) screenStart.y,
                        (float) screenEnd.x,
                        (float) screenEnd.y,
                        width,
                        drawColor
                );
            }
        }

        if (showLandingPoint.getValue() && !isThrown && !points.isEmpty()) {
            Vec3d lastPoint = points.get(points.size() - 1);
            Vec3d screenPos = W2SUtil.getCoords(lastPoint);

            if (screenPos != null && screenPos.z >= 0.0 && screenPos.z < 1.0) {
                Color hitCol = hitColor.getValue();
                float size = pointSize.getValueFloat();

                NanoVGRenderer.drawCircle((float) screenPos.x, (float) screenPos.y, size + 2.0f,
                        new Color(0, 0, 0, 150));
                NanoVGRenderer.drawCircle((float) screenPos.x, (float) screenPos.y, size, hitCol);
            }
        }
    }
}
