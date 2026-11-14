package com.saif.fossilfinder;

import com.saif.fossilfinder.registry.OwlEntities;
import com.saif.fossilfinder.registry.OwlItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FossilFinderOwl.MODID)
public class FossilFinderOwl {
    public static final String MODID = "fossil_finder_owl";

    public FossilFinderOwl() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        OwlEntities.ENTITIES.register(modBus);
        OwlItems.ITEMS.register(modBus);

        modBus.addListener(this::attributes);
        modBus.addListener(this::addCreativeTab);
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(() -> SpawnPlacements.register(
                OwlEntities.OWL.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules
            ))
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(OwlEntities.OWL.get(), com.saif.fossilfinder.entity.FossilFinderOwlEntity.createAttributes().build());
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(OwlItems.SPAWN_EGG);
        }
    }
}
