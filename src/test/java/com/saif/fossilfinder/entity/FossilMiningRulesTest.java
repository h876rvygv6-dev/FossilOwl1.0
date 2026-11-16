package com.saif.fossilfinder.entity;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FossilMiningRulesTest {
    @Test
    void pickaxeRouteMiningIsLimitedToSpecifiedStone() {
        assertTrue(FossilMiningRules.isPickaxeRouteBlock(blockId("stone")));
        assertTrue(FossilMiningRules.isPickaxeRouteBlock(blockId("granite")));
        assertTrue(FossilMiningRules.isPickaxeRouteBlock(blockId("deepslate")));
        assertTrue(FossilMiningRules.isPickaxeRouteBlock(blockId("tuff")));
        assertFalse(FossilMiningRules.isPickaxeRouteBlock(blockId("diamond_ore")));
        assertFalse(FossilMiningRules.isPickaxeRouteBlock(blockId("iron_ore")));
        assertFalse(FossilMiningRules.isPickaxeRouteBlock(blockId("andesite")));
    }

    @Test
    void shovelRouteMiningIsLimitedToSoilAndLooseBlocks() {
        assertTrue(FossilMiningRules.isShovelRouteBlock(blockId("sand")));
        assertTrue(FossilMiningRules.isShovelRouteBlock(blockId("gravel")));
        assertTrue(FossilMiningRules.isShovelRouteBlock(blockId("dirt")));
        assertTrue(FossilMiningRules.isShovelRouteBlock(blockId("grass_block")));
        assertFalse(FossilMiningRules.isShovelRouteBlock(blockId("stone")));
        assertFalse(FossilMiningRules.isShovelRouteBlock(blockId("coal_ore")));
    }

    private ResourceLocation blockId(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
