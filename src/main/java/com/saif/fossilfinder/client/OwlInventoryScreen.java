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
        "fossil_finder_owl", "textures/gui/fossil_finder_owl_gui_texture.png");
    private static final int TEXTURE_WIDTH = 2046;
    private static final int TEXTURE_HEIGHT = 1890;
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 236;

    public OwlInventoryScreen(OwlInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, imageWidth, imageHeight,
            0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (minecraft.level != null
            && minecraft.level.getEntity(menu.getOwlEntityId()) instanceof FossilFinderOwlEntity owl) {
            int previewX = leftPos + 64;
            int previewY = topPos + 102;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, previewX, previewY, 34, previewX - mouseX, previewY - mouseY, owl);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
