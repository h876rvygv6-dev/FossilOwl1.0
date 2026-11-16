package com.saif.fossilfinder.entity;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class FossilMiningRules {
    private FossilMiningRules() {}

    public static boolean isPickaxeRouteBlock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRANITE)
            || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF);
    }

    public static boolean isShovelRouteBlock(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
            || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
            || state.is(Blocks.DIRT_PATH);
    }
}
