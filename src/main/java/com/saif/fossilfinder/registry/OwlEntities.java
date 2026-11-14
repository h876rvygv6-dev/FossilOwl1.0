package com.saif.fossilfinder.registry;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OwlEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FossilFinderOwl.MODID);
    public static final RegistryObject<EntityType<FossilFinderOwlEntity>> OWL = ENTITIES.register("fossil_finder_owl", () -> EntityType.Builder.of(FossilFinderOwlEntity::new, MobCategory.CREATURE).sized(0.55f, 0.95f).clientTrackingRange(8).build("fossil_finder_owl:fossil_finder_owl"));
    private OwlEntities() {}
}
