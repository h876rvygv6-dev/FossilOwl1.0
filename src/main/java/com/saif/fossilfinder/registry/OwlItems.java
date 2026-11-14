package com.saif.fossilfinder.registry;

import com.saif.fossilfinder.FossilFinderOwl;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OwlItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FossilFinderOwl.MODID);
    public static final RegistryObject<Item> SPAWN_EGG = ITEMS.register(
        "fossil_finder_owl_spawn_egg",
        () -> new ForgeSpawnEggItem(OwlEntities.OWL, 0xA99A7B, 0xE8DCC2, new Item.Properties())
    );

    private OwlItems() {}
}
