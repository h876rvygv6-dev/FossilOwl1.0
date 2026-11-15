package com.saif.fossilfinder.client;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OwlModel extends GeoModel<FossilFinderOwlEntity> {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation(FossilFinderOwl.MODID, "textures/entity/owl.png");
    private static final ResourceLocation BLATHERS_TEXTURE =
        new ResourceLocation(FossilFinderOwl.MODID, "textures/entity/blathers.png");
    private static final ResourceLocation CELESTE_TEXTURE =
        new ResourceLocation(FossilFinderOwl.MODID, "textures/entity/celeste.png");

    @Override
    public ResourceLocation getModelResource(FossilFinderOwlEntity entity) {
        return new ResourceLocation(FossilFinderOwl.MODID, "geo/owl.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FossilFinderOwlEntity entity) {
        if (entity.hasCustomName()) {
            String name = entity.getCustomName().getString();
            if ("Blathers".equalsIgnoreCase(name)) return BLATHERS_TEXTURE;
            if ("Celeste".equalsIgnoreCase(name)) return CELESTE_TEXTURE;
        }
        return DEFAULT_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FossilFinderOwlEntity entity) {
        return new ResourceLocation(FossilFinderOwl.MODID, "animations/owl.animation.json");
    }
}
