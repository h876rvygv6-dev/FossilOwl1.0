package com.saif.fossilfinder.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static com.saif.fossilfinder.registry.OwlMenus.OWL_INVENTORY;

public class OwlInventoryMenu extends AbstractContainerMenu {
    private final Container cargo;

    public OwlInventoryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }

    public OwlInventoryMenu(int containerId, Inventory playerInventory, Container cargo) {
        super(OWL_INVENTORY.get(), containerId);
        this.cargo = cargo;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(cargo, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack carried = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            carried = stack.copy();

            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return carried;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Container getCargo() {
        return cargo;
    }
}
