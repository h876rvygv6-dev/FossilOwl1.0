package com.saif.fossilfinder.client;

import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** Renders the owl's synced tools at the hand and head attachment bones. */
public class OwlHeldItemsLayer extends BlockAndItemGeoLayer<FossilFinderOwlEntity> {
    public OwlHeldItemsLayer(GeoRenderer<FossilFinderOwlEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, FossilFinderOwlEntity owl) {
        return switch (bone.getName()) {
            case "right_hand" -> owl.getToolForRender();
            case "left_hand" -> owl.getLightForRender();
            case "head_item" -> owl.getItemBySlot(EquipmentSlot.HEAD);
            default -> null;
        };
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, FossilFinderOwlEntity owl) {
        return switch (bone.getName()) {
            case "right_hand" -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case "left_hand" -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            case "head_item" -> ItemDisplayContext.HEAD;
            default -> ItemDisplayContext.NONE;
        };
    }
}
