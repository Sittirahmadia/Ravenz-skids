package com.raven.ravenz.utils.mc.crystal;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

import static com.raven.ravenz.RavenZClient.mc;

public final class CrystalUtils {

    private CrystalUtils() {}

    public static boolean canPlaceCrystalClient(BlockPos block) {
        BlockState blockState = mc.world.getBlockState(block);
        if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK))
            return false;
        return canPlaceCrystalClientAssumeObsidian(block);
    }

    public static boolean canPlaceCrystalClientAssumeObsidian(BlockPos block) {
        BlockPos up = block.up();
        if (!mc.world.isAir(up)) return false;

        double d = up.getX(), e = up.getY(), f = up.getZ();
        List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0, e + 2.0, f + 1.0));
        return list.isEmpty();
    }
}
