package com.saif.fossilfinder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlRenderer extends GeoEntityRenderer<FossilFinderOwlEntity> {
    private BlathersMesh blathersMesh;

    public OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new OwlModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new OwlHeldItemsLayer(this));
    }

    @Override
    public void render(FossilFinderOwlEntity owl, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (blathersMesh == null) {
            blathersMesh = BlathersMesh.load();
        }
        poseStack.pushPose();
        try {
            applyRotations(owl, poseStack, owl.tickCount + partialTick, entityYaw, partialTick);
            blathersMesh.render(owl, partialTick, poseStack, bufferSource, packedLight);
        }
        finally {
            poseStack.popPose();
        }
        super.render(owl, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
