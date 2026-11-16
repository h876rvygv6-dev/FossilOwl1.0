package com.saif.fossilfinder.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class FossilMiningRules {
    private static final Set<Block> PICKAXE_ROUTE_BLOCKS = Set.of(
        Blocks.STONE, Blocks.GRANITE, Blocks.DEEPSLATE, Blocks.TUFF);
    private static final Set<Block> SHOVEL_ROUTE_BLOCKS = Set.of(
        Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.DIRT, Blocks.GRASS_BLOCK,
        Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.DIRT_PATH);

    private FossilMiningRules() {}

    public static boolean isPickaxeRouteBlock(BlockState state) {
        return PICKAXE_ROUTE_BLOCKS.contains(state.getBlock());
    }

    public static boolean isShovelRouteBlock(BlockState state) {
        return SHOVEL_ROUTE_BLOCKS.contains(state.getBlock());
    }
}
