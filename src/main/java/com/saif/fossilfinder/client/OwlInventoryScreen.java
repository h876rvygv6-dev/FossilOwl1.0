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

    public OwlInventoryScreen(OwlInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 162;
        imageHeight = 150;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 1400, 1293);
        graphics.fill(leftPos + 4, topPos + 46, leftPos + 22, topPos + 64, 0xFF38251A);
        graphics.fill(leftPos + 5, topPos + 47, leftPos + 21, topPos + 63, 0xFF9A795A);
        graphics.fill(leftPos + 6, topPos + 48, leftPos + 20, topPos + 62, 0xFF493322);

        if (minecraft.level != null
            && minecraft.level.getEntity(menu.getOwlEntityId()) instanceof FossilFinderOwlEntity owl) {
            int previewX = leftPos + 47;
            int previewY = topPos + 37;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, previewX, previewY, 28, previewX - mouseX, previewY - mouseY, owl);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
