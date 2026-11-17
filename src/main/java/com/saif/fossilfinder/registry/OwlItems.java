package com.saif.fossilfinder.registry;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.item.OwlHatItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OwlItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FossilFinderOwl.MODID);

    public static final RegistryObject<Item> SPAWN_EGG = ITEMS.register(
        "fossil_finder_owl_spawn_egg",
        () -> new ForgeSpawnEggItem(OwlEntities.OWL, 0xA99A7B, 0xE8DCC2, new Item.Properties()));

    public static final RegistryObject<Item> MUSEUM_GUIDE = ITEMS.register(
        "museum_guide", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIELD_NOTES = ITEMS.register(
        "field_notes", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PALEO_COWBOY_HAT = hat("paleo_cowboy_hat", 6.0);
    public static final RegistryObject<Item> PALEONTOLOGIST_EXPLORER_HAT = hat("paleontologist_explorer_hat", 6.0);
    public static final RegistryObject<Item> TRICERATOPS_PLUSHIE_HAT = hat("dino_plushie_hat_triceratops", 10.0);
    public static final RegistryObject<Item> T_REX_PLUSHIE_HAT = hat("dino_plushie_hat_t_rex", 10.0);
    public static final RegistryObject<Item> DINO_BASEBALL_CAP = hat("dino_baseball_cap", 6.0);
    public static final RegistryObject<Item> DINO_TRUCKER_CAP = hat("dino_trucker_cap", 6.0);

    private static RegistryObject<Item> hat(String id, double bonusHealth) {
        return ITEMS.register(id, () -> new OwlHatItem(id, bonusHealth, new Item.Properties()));
    }

    private OwlItems() {}
}
