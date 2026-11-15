package com.saif.fossilfinder.client;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.item.OwlHatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FossilFinderOwl.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OwlHatRenderEvents {
    private OwlHatRenderEvents() {}

    @SubscribeEvent
    public static void renderPlayerHat(RenderPlayerEvent.Post event) {
        ItemStack hat = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
        if (!(hat.getItem() instanceof OwlHatItem)) return;

        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            event.getRenderer().getModel().head.translateAndRotate(poseStack);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                event.getEntity(),
                hat,
                ItemDisplayContext.HEAD,
                false,
                poseStack,
                event.getMultiBufferSource(),
                event.getEntity().level(),
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                event.getEntity().getId()
            );
        }
        finally {
            poseStack.popPose();
        }
    }
}
