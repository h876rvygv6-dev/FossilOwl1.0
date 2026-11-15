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
    private static final int TEXTURE_WIDTH = 1400;
    private static final int TEXTURE_HEIGHT = 1293;
    private static final int TEXTURE_SCALE = 6;
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "fossil_finder_owl", "textures/gui/fossil_finder_owl_gui_texture.png");

    public OwlInventoryScreen(OwlInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = (TEXTURE_WIDTH + TEXTURE_SCALE - 1) / TEXTURE_SCALE;
        imageHeight = (TEXTURE_HEIGHT + TEXTURE_SCALE - 1) / TEXTURE_SCALE;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, imageWidth, imageHeight, 0, 0,
            TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (minecraft.level != null
            && minecraft.level.getEntity(menu.getOwlEntityId()) instanceof FossilFinderOwlEntity owl) {
            int previewX = leftPos + 94;
            int previewY = topPos + 105;
            owl.setInventoryPreviewRendering(true);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, previewX, previewY, 30, previewX - mouseX, previewY - mouseY, owl);
            } finally {
                owl.setInventoryPreviewRendering(false);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
