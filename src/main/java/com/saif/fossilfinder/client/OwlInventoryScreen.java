package com.saif.fossilfinder.client;

import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import com.saif.fossilfinder.menu.OwlInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class OwlInventoryScreen extends AbstractContainerScreen<OwlInventoryMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "fossil_finder_owl", "textures/gui/owl_inventory.png");

    public OwlInventoryScreen(OwlInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 162;
        imageHeight = 152;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (minecraft.level != null
            && minecraft.level.getEntity(menu.getOwlEntityId()) instanceof FossilFinderOwlEntity owl) {
            int previewX = leftPos + 47;
            int previewY = topPos + 37;
            owl.setInventoryPreviewRendering(true);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, previewX, previewY, 28, previewX - mouseX, previewY - mouseY, owl);
            } finally {
                owl.setInventoryPreviewRendering(false);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
