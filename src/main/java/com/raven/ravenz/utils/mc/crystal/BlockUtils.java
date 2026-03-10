package com.raven.ravenz.utils.mc.crystal;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.stream.Stream;

import static com.raven.ravenz.RavenZClient.mc;

public final class BlockUtils {

    private BlockUtils() {}

    public static boolean isBlock(BlockPos pos, Block block) {
        return mc.world.getBlockState(pos).getBlock() == block;
    }

    public static Stream<BlockPos> getAllInBoxStream(BlockPos from, BlockPos to) {
        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));

        int limit = (max.getX() - min.getX() + 1)
                  * (max.getY() - min.getY() + 1)
                  * (max.getZ() - min.getZ() + 1);

        Stream<BlockPos> stream = Stream.iterate(min, pos -> {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            if (++x > max.getX()) { x = min.getX(); y++; }
            if (y > max.getY())   { y = min.getY(); z++; }
            return new BlockPos(x, y, z);
        });

        return stream.limit(limit);
    }
}
