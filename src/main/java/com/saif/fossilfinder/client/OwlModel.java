package com.saif.fossilfinder.client;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OwlModel extends GeoModel<FossilFinderOwlEntity> {
    @Override
    public ResourceLocation getModelResource(FossilFinderOwlEntity entity) {
        return new ResourceLocation(FossilFinderOwl.MODID, "geo/owl.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FossilFinderOwlEntity entity) {
        return new ResourceLocation(FossilFinderOwl.MODID, "textures/entity/owl.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FossilFinderOwlEntity entity) {
        return new ResourceLocation(FossilFinderOwl.MODID, "animations/owl.animation.json");
    }
}
