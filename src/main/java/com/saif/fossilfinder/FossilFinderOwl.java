package com.saif.fossilfinder;

import com.saif.fossilfinder.registry.OwlEntities;
import com.saif.fossilfinder.registry.OwlItems;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.MobCategory;

@Mod(FossilFinderOwl.MODID)
public class FossilFinderOwl {
    public static final String MODID = "fossil_finder_owl";
    public FossilFinderOwl() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        OwlEntities.ENTITIES.register(modBus);
        OwlItems.ITEMS.register(modBus);
        modBus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.register(this);
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() ->
            SpawnPlacements.register(OwlEntities.OWL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules)));
    }
    private void attributes(EntityAttributeCreationEvent e) { e.put(OwlEntities.OWL.get(), com.saif.fossilfinder.entity.FossilFinderOwlEntity.createAttributes().build()); }

    @SubscribeEvent
    public void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(OwlItems.SPAWN_EGG);
    }

    @SubscribeEvent
    public void addBiomeSpawns(BiomeLoadingEvent event) {
        Biome.BiomeCategory c = event.getCategory();
        if (c == Biome.BiomeCategory.FOREST || c == Biome.BiomeCategory.PLAINS) {
            event.getSpawns().getSpawner(MobCategory.CREATURE).add(new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(OwlEntities.OWL.get(), 5, 1, 2));
        }
    }
}
