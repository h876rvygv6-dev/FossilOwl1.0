package com.saif.fossilfinder.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FossilMiningRulesTest {
    @Test
    void pickaxeRouteMiningIsLimitedToSpecifiedStone() {
        Set<Block> allowed = Set.of(Blocks.STONE, Blocks.GRANITE, Blocks.DEEPSLATE, Blocks.TUFF);
        for (Block block : allowed) {
            assertTrue(FossilMiningRules.isPickaxeRouteBlock(block.defaultBlockState()));
        }

        assertFalse(FossilMiningRules.isPickaxeRouteBlock(Blocks.DIAMOND_ORE.defaultBlockState()));
        assertFalse(FossilMiningRules.isPickaxeRouteBlock(Blocks.IRON_ORE.defaultBlockState()));
        assertFalse(FossilMiningRules.isPickaxeRouteBlock(Blocks.ANDESITE.defaultBlockState()));
    }

    @Test
    void shovelRouteMiningIsLimitedToSoilAndLooseBlocks() {
        assertTrue(FossilMiningRules.isShovelRouteBlock(Blocks.SAND.defaultBlockState()));
        assertTrue(FossilMiningRules.isShovelRouteBlock(Blocks.GRAVEL.defaultBlockState()));
        assertTrue(FossilMiningRules.isShovelRouteBlock(Blocks.DIRT.defaultBlockState()));
        assertTrue(FossilMiningRules.isShovelRouteBlock(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertFalse(FossilMiningRules.isShovelRouteBlock(Blocks.STONE.defaultBlockState()));
        assertFalse(FossilMiningRules.isShovelRouteBlock(Blocks.COAL_ORE.defaultBlockState()));
    }
}
