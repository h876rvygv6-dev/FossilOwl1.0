package com.saif.fossilfinder.client;

import com.saif.fossilfinder.menu.OwlInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OwlInventoryScreen extends AbstractContainerScreen<OwlInventoryMenu> {
    public OwlInventoryScreen(OwlInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = 168;
        inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF202020);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, 0xFFC6C6C6);
        graphics.fill(left + 3, top + 3, left + imageWidth - 3, top + 73, 0xFF8B8B8B);
        graphics.fill(left + 4, top + 4, left + imageWidth - 4, top + 72, 0xFFC6C6C6);
    }
}
