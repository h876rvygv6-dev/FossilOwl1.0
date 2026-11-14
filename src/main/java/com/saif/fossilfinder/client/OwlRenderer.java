package com.saif.fossilfinder.client;

import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlRenderer extends GeoEntityRenderer<FossilFinderOwlEntity> {
    public OwlRenderer(EntityRendererProvider.Context context) { super(context, new OwlModel()); this.shadowRadius = 0.25f; }
}
