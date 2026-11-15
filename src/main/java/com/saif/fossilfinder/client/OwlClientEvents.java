package com.saif.fossilfinder.client;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.registry.OwlEntities;
import com.saif.fossilfinder.registry.OwlItems;
import com.saif.fossilfinder.registry.OwlMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=FossilFinderOwl.MODID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class OwlClientEvents {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(OwlEntities.OWL.get(), OwlRenderer::new); }
    @SubscribeEvent public static void registerItemColors(RegisterColorHandlersEvent.Item event) { event.register((stack, tintIndex) -> 0xFFFFFF, OwlItems.SPAWN_EGG.get()); }
    @SubscribeEvent public static void registerMenuScreens(RegisterMenuScreensEvent event) { event.register(OwlMenus.OWL_INVENTORY.get(), OwlInventoryScreen::new); }
}
