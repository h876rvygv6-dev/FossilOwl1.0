package com.saif.fossilfinder.entity;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class FossilMiningRules {
    private static final Set<String> PICKAXE_ROUTE_BLOCKS = Set.of(
        "minecraft:stone", "minecraft:granite", "minecraft:deepslate", "minecraft:tuff");
    private static final Set<String> SHOVEL_ROUTE_BLOCKS = Set.of(
        "minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:dirt",
        "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
        "minecraft:podzol", "minecraft:mycelium", "minecraft:dirt_path");

    private FossilMiningRules() {}

    public static boolean isPickaxeRouteBlock(ResourceLocation blockId) {
        return blockId != null && PICKAXE_ROUTE_BLOCKS.contains(blockId.toString());
    }

    public static boolean isShovelRouteBlock(ResourceLocation blockId) {
        return blockId != null && SHOVEL_ROUTE_BLOCKS.contains(blockId.toString());
    }
}
