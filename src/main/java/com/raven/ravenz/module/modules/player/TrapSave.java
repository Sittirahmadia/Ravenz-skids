package com.raven.ravenz.module.modules.player;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.event.impl.render.Render2DEvent;
import com.raven.ravenz.event.impl.render.Render3DEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.utils.math.TimerUtil;
import com.raven.ravenz.utils.mc.ChatUtil;
import com.raven.ravenz.utils.render.Render3DEngine;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class TrapSave extends Module {
    private static final int SCAN_INTERVAL_MS = 500;
    private static final int WARNING_DURATION_MS = 3000;
    private static final String CHAT_PREFIX = "[TrapSave] ";

    private static final Box REDSTONE_WIRE_BOUNDS = new Box(0, 0, 0, 1, 0.0625, 1);
    private static final Box TORCH_BOUNDS = new Box(0.375, 0, 0.375, 0.625, 0.625, 0.625);
    private static final Box LEVER_BOUNDS = new Box(0.25, 0, 0.25, 0.75, 0.6, 0.75);
    private static final Box PRESSURE_PLATE_BOUNDS = new Box(0.0625, 0, 0.0625, 0.9375, 0.0625, 0.9375);
    private static final Box TRIPWIRE_HOOK_BOUNDS = new Box(0.375, 0, 0.125, 0.625, 0.625, 0.875);
    private static final Box THIN_BLOCK_BOUNDS = new Box(0, 0, 0, 1, 0.125, 1);
    private static final Box FULL_BLOCK_BOUNDS = new Box(0, 0, 0, 1, 1, 1);

    private final NumberSetting scanRadius = new NumberSetting("Scan Radius", 3, 20, 8, 1);
    private final NumberSetting scanHeight = new NumberSetting("Scan Height", 0, 30, 8, 1);
    private final NumberSetting maxExpansion = new NumberSetting("Max Expansion", 0, 100, 20, 1);
    private final BooleanSetting soundAlert = new BooleanSetting("Sound Alert", true);
    private final BooleanSetting showWarning = new BooleanSetting("Show Warning", true);
    private final NumberSetting outlineWidth = new NumberSetting("Outline Width", 1, 5, 2, 1);
    private final NumberSetting tntColor = new NumberSetting("TNT Color", 0, 16777215, 0xFFA500, 1);
    private final NumberSetting redstoneColor = new NumberSetting("Redstone Color", 0, 16777215, 0xFF0000, 1);
    private final NumberSetting pistonColor = new NumberSetting("Piston Color", 0, 16777215, 0x8B4513, 1);
    private final NumberSetting leverColor = new NumberSetting("Lever Color", 0, 16777215, 0xFFFF00, 1);
    private final NumberSetting armorStandColor = new NumberSetting("Armor Stand Color", 0, 16777215, 0x00FFFF, 1);

    private final TimerUtil scanTimer = new TimerUtil();
    private final TimerUtil warningTimer = new TimerUtil();
    private final List<TrapCluster> detectedTraps = new ArrayList<>();

    private boolean trapDetected;
    private String detectedTrapType = "";
    private int trapCount;
    private BlockPos lastScanPosition;
    private int lastScanRadius;
    private int lastScanHeight;

    public TrapSave() {
        super("Trap Save", "Detects trap blocks and armor stands around the player", -1, Category.PLAYER);
        addSettings(scanRadius, scanHeight, maxExpansion, soundAlert, showWarning, outlineWidth,
                tntColor, redstoneColor, pistonColor, leverColor, armorStandColor);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (isInvalidGameState() || detectedTraps.isEmpty()) {
            return;
        }
        renderTrapOutlines(event.getMatrixStack());
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (isInvalidGameState() || !showWarning.getValue() || !trapDetected) {
            return;
        }
        renderWarningOverlay(event);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isInvalidGameState()) {
            return;
        }

        if (scanTimer.hasElapsedTime(SCAN_INTERVAL_MS)) {
            performTrapScan();
            scanTimer.reset();
        }

        if (trapDetected && warningTimer.hasElapsedTime(WARNING_DURATION_MS)) {
            resetTrapDetection();
        }
    }

    private boolean isInvalidGameState() {
        return mc.player == null || mc.world == null || !isEnabled();
    }

    private void performTrapScan() {
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = scanRadius.getValueInt();
        int height = scanHeight.getValueInt();

        if (shouldSkipScan(playerPos, radius, height)) {
            return;
        }

        List<TrapCluster> foundTraps = scanForTrapClusters(playerPos, radius, height);
        detectedTraps.clear();
        detectedTraps.addAll(foundTraps);

        updateScanCache(playerPos, radius, height);

        if (!foundTraps.isEmpty() && !trapDetected) {
            handleTrapDetection(foundTraps);
        }
    }

    private boolean shouldSkipScan(BlockPos currentPos, int currentRadius, int currentHeight) {
        return lastScanPosition != null
                && lastScanPosition.equals(currentPos)
                && lastScanRadius == currentRadius
                && lastScanHeight == currentHeight;
    }

    private void updateScanCache(BlockPos position, int radius, int height) {
        lastScanPosition = position;
        lastScanRadius = radius;
        lastScanHeight = height;
    }

    private List<TrapCluster> scanForTrapClusters(BlockPos playerPos, int radius, int height) {
        Set<BlockPos> scannedBlocks = new HashSet<>();
        List<TrapCluster> clusters = new ArrayList<>();
        int radiusSquared = radius * radius;

        boolean infiniteHeight = height == 0;
        int minY = infiniteHeight ? mc.world.getBottomY() : playerPos.getY() - height;
        int maxY = infiniteHeight
                ? mc.world.getBottomY() + mc.world.getHeight() - 1
                : playerPos.getY() + height;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSquared) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    BlockPos scanPos = new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z);
                    if (scannedBlocks.contains(scanPos)) {
                        continue;
                    }

                    Block block = mc.world.getBlockState(scanPos).getBlock();
                    TrapType trapType = getTrapType(block);
                    if (trapType == TrapType.NONE) {
                        continue;
                    }

                    TrapCluster cluster = expandTrapCluster(scanPos, trapType, scannedBlocks);
                    if (!cluster.isEmpty()) {
                        clusters.add(cluster);
                    }
                }
            }
        }

        scanArmorStands(playerPos, radius, height, clusters);
        return clusters;
    }

    private TrapCluster expandTrapCluster(BlockPos startPos, TrapType startType, Set<BlockPos> globalScanned) {
        TrapCluster cluster = new TrapCluster(startType);
        Queue<BlockPos> toExpand = new ArrayDeque<>();
        Set<BlockPos> clusterScanned = new HashSet<>();

        int maxBlocks = maxExpansion.getValueInt();
        boolean infiniteExpansion = maxBlocks == 0;
        int safetyLimit = 1000;

        toExpand.offer(startPos);

        while (!toExpand.isEmpty() && (infiniteExpansion ? cluster.size() < safetyLimit : cluster.size() < maxBlocks)) {
            BlockPos current = toExpand.poll();
            if (current == null || clusterScanned.contains(current) || globalScanned.contains(current)) {
                continue;
            }

            clusterScanned.add(current);
            globalScanned.add(current);

            Block block = mc.world.getBlockState(current).getBlock();
            TrapType trapType = getTrapType(block);

            if (trapType != TrapType.NONE && isRelatedTrapType(startType, trapType)) {
                cluster.addBlock(current, trapType);
                for (BlockPos neighbor : getNeighbors(current)) {
                    if (!clusterScanned.contains(neighbor)) {
                        toExpand.offer(neighbor);
                    }
                }
            }
        }

        return cluster;
    }

    private boolean isRelatedTrapType(TrapType original, TrapType candidate) {
        if (original == candidate) {
            return true;
        }

        Set<TrapType> redstoneGroup = EnumSet.of(TrapType.REDSTONE, TrapType.LEVER, TrapType.PRESSURE_PLATE, TrapType.TRIPWIRE);
        Set<TrapType> mechanicalGroup = EnumSet.of(TrapType.PISTON, TrapType.DISPENSER, TrapType.TNT);

        if (redstoneGroup.contains(original) && redstoneGroup.contains(candidate)) {
            return true;
        }
        if (mechanicalGroup.contains(original) && mechanicalGroup.contains(candidate)) {
            return true;
        }
        if (redstoneGroup.contains(original) && mechanicalGroup.contains(candidate)) {
            return true;
        }
        return mechanicalGroup.contains(original) && redstoneGroup.contains(candidate);
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(
                pos.north(), pos.south(), pos.east(), pos.west(),
                pos.up(), pos.down(),
                pos.north().up(), pos.south().up(), pos.east().up(), pos.west().up(),
                pos.north().down(), pos.south().down(), pos.east().down(), pos.west().down()
        );
    }

    private void scanArmorStands(BlockPos playerPos, int radius, int height, List<TrapCluster> clusters) {
        boolean infiniteHeight = height == 0;
        int minY = infiniteHeight ? mc.world.getBottomY() : playerPos.getY() - height;
        int maxY = infiniteHeight
                ? mc.world.getBottomY() + mc.world.getHeight() - 1
                : playerPos.getY() + height;

        Box area = new Box(
                playerPos.getX() - radius,
                minY,
                playerPos.getZ() - radius,
                playerPos.getX() + radius,
                maxY,
                playerPos.getZ() + radius
        );

        List<ArmorStandEntity> armorStands = mc.world.getEntitiesByClass(
                ArmorStandEntity.class,
                area,
                armorStand -> armorStand != null && armorStand.isAlive()
        );

        if (!armorStands.isEmpty()) {
            TrapCluster armorStandCluster = new TrapCluster(TrapType.ARMOR_STAND);
            for (ArmorStandEntity armorStand : armorStands) {
                armorStandCluster.addArmorStand(armorStand);
            }
            clusters.add(armorStandCluster);
        }
    }

    private TrapType getTrapType(Block block) {
        if (block == Blocks.TNT) {
            return TrapType.TNT;
        }
        if (block == Blocks.LEVER) {
            return TrapType.LEVER;
        }
        if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
            return TrapType.PISTON;
        }
        if (isRedstoneBlock(block)) {
            return TrapType.REDSTONE;
        }
        if (isPressurePlate(block)) {
            return TrapType.PRESSURE_PLATE;
        }
        if (block == Blocks.TRIPWIRE_HOOK) {
            return TrapType.TRIPWIRE;
        }
        if (block == Blocks.DISPENSER || block == Blocks.DROPPER) {
            return TrapType.DISPENSER;
        }
        return TrapType.NONE;
    }

    private boolean isRedstoneBlock(Block block) {
        return block == Blocks.REDSTONE_WIRE
                || block == Blocks.REDSTONE_TORCH
                || block == Blocks.REDSTONE_WALL_TORCH
                || block == Blocks.REDSTONE_BLOCK
                || block == Blocks.OBSERVER
                || block == Blocks.REPEATER
                || block == Blocks.COMPARATOR;
    }

    private boolean isPressurePlate(Block block) {
        return block == Blocks.STONE_PRESSURE_PLATE
                || block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE
                || block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE;
    }

    private void handleTrapDetection(List<TrapCluster> traps) {
        trapDetected = true;
        detectedTrapType = traps.get(0).getPrimaryType().getDisplayName();
        trapCount = traps.size();
        warningTimer.reset();

        if (soundAlert.getValue()) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 2.0f));
        }

        ChatUtil.addChatMessage(CHAT_PREFIX + "Trap detected: " + detectedTrapType + " (" + trapCount + " traps)");
    }

    private void renderWarningOverlay(Render2DEvent event) {
        int centerX = event.getWidth() / 2;
        int warningY = 50;
        int warningWidth = 210;
        int warningHeight = 40;

        int backgroundX = centerX - warningWidth / 2;
        String warningText = "TRAP DETECTED!";
        String detailText = detectedTrapType + " (" + trapCount + " traps)";

        event.getContext().fill(backgroundX - 10, warningY - 10, backgroundX + warningWidth, warningY + warningHeight, 0x80FF0000);
        event.getContext().drawText(mc.textRenderer, Text.literal(warningText), backgroundX, warningY, 0xFFFFFFFF, true);
        event.getContext().drawText(mc.textRenderer, Text.literal(detailText), backgroundX, warningY + 15, 0xFFFFFFFF, true);
    }

    private void renderTrapOutlines(MatrixStack matrices) {
        Render3DEngine.setupThroughWalls();
        GL11.glLineWidth(outlineWidth.getValueFloat());

        for (TrapCluster cluster : detectedTraps) {
            renderTrapCluster(matrices, cluster);
        }

        Render3DEngine.end();
    }

    private void renderTrapCluster(MatrixStack matrices, TrapCluster cluster) {
        Color clusterColor = getCustomColor(cluster.getPrimaryType());

        for (Map.Entry<BlockPos, TrapType> entry : cluster.getBlocks().entrySet()) {
            BlockPos blockPos = entry.getKey();
            Block block = mc.world.getBlockState(blockPos).getBlock();
            Box bounds = getBlockBounds(block).offset(blockPos);
            Render3DEngine.drawOutlineBox(matrices, bounds, clusterColor);
        }

        for (ArmorStandEntity armorStand : cluster.getArmorStands()) {
            if (armorStand.isAlive()) {
                Render3DEngine.drawOutlineBox(matrices, armorStand.getBoundingBox(), clusterColor);
            }
        }
    }

    private Color getCustomColor(TrapType trapType) {
        int alpha = 200;
        return switch (trapType) {
            case TNT -> new Color(tntColor.getValueInt() | (alpha << 24), true);
            case REDSTONE -> new Color(redstoneColor.getValueInt() | (alpha << 24), true);
            case PISTON -> new Color(pistonColor.getValueInt() | (alpha << 24), true);
            case LEVER, PRESSURE_PLATE -> new Color(leverColor.getValueInt() | (alpha << 24), true);
            case ARMOR_STAND -> new Color(armorStandColor.getValueInt() | (alpha << 24), true);
            case TRIPWIRE -> new Color(0x800080 | (alpha << 24), true);
            case DISPENSER -> new Color(0x696969 | (alpha << 24), true);
            default -> new Color(0xFFFFFF | (alpha << 24), true);
        };
    }

    private Box getBlockBounds(Block block) {
        if (block == Blocks.REDSTONE_WIRE) {
            return REDSTONE_WIRE_BOUNDS;
        }
        if (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH) {
            return TORCH_BOUNDS;
        }
        if (block == Blocks.LEVER) {
            return LEVER_BOUNDS;
        }
        if (isPressurePlate(block)) {
            return PRESSURE_PLATE_BOUNDS;
        }
        if (block == Blocks.TRIPWIRE_HOOK) {
            return TRIPWIRE_HOOK_BOUNDS;
        }
        if (block == Blocks.REPEATER || block == Blocks.COMPARATOR) {
            return THIN_BLOCK_BOUNDS;
        }
        return FULL_BLOCK_BOUNDS;
    }

    private void resetTrapDetection() {
        trapDetected = false;
        detectedTrapType = "";
        trapCount = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        detectedTraps.clear();
        resetTrapDetection();
        lastScanPosition = null;
    }

    @Override
    public void onDisable() {
        resetTrapDetection();
        detectedTraps.clear();
        lastScanPosition = null;
        super.onDisable();
    }

    private enum TrapType {
        NONE(""),
        TNT("TNT"),
        LEVER("Lever"),
        PISTON("Piston"),
        REDSTONE("Redstone"),
        PRESSURE_PLATE("Pressure Plate"),
        TRIPWIRE("Tripwire"),
        DISPENSER("Dispenser"),
        ARMOR_STAND("Armor Stand");

        private final String displayName;

        TrapType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static class TrapCluster {
        private final TrapType primaryType;
        private final Map<BlockPos, TrapType> blocks = new HashMap<>();
        private final List<ArmorStandEntity> armorStands = new ArrayList<>();

        TrapCluster(TrapType primaryType) {
            this.primaryType = primaryType;
        }

        public TrapType getPrimaryType() {
            return primaryType;
        }

        public Map<BlockPos, TrapType> getBlocks() {
            return blocks;
        }

        public List<ArmorStandEntity> getArmorStands() {
            return armorStands;
        }

        public void addBlock(BlockPos pos, TrapType type) {
            blocks.put(pos, type);
        }

        public void addArmorStand(ArmorStandEntity armorStand) {
            armorStands.add(armorStand);
        }

        public boolean isEmpty() {
            return blocks.isEmpty() && armorStands.isEmpty();
        }

        public int size() {
            return blocks.size() + armorStands.size();
        }
    }
}
