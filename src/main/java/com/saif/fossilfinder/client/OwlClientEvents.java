package com.saif.fossilfinder.client;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.registry.OwlEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=FossilFinderOwl.MODID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class OwlClientEvents {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(OwlEntities.OWL.get(), OwlRenderer::new); }
}
