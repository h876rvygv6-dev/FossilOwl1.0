package com.saif.fossilfinder.menu;

import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static com.saif.fossilfinder.registry.OwlMenus.OWL_INVENTORY;

public class OwlInventoryMenu extends AbstractContainerMenu {
    private static final int CARGO_SLOTS = 15;
    private static final int TOOL_SLOT = CARGO_SLOTS;
    private static final int PLAYER_INVENTORY_START = TOOL_SLOT + 1;
    private final int owlEntityId;

    public OwlInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, new SimpleContainer(CARGO_SLOTS), new SimpleContainer(1), buffer.readVarInt());
    }

    public OwlInventoryMenu(int containerId, Inventory playerInventory, Container cargo, Container tool, int owlEntityId) {
        super(OWL_INVENTORY.get(), containerId);
        this.owlEntityId = owlEntityId;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                addSlot(new Slot(cargo, row * 5 + col, 73 + col * 17, 13 + row * 17));
            }
        }

        addSlot(new Slot(tool, 0, 5, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FossilFinderOwlEntity.isPickaxe(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 5 + col * 17, 74 + row * 17));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 5 + col * 17, 126));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack carried = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            carried = stack.copy();
            boolean moved;

            if (index < CARGO_SLOTS || index == TOOL_SLOT) {
                moved = this.moveItemStackTo(stack, PLAYER_INVENTORY_START, this.slots.size(), true);
            } else {
                moved = false;
                Slot toolSlot = this.slots.get(TOOL_SLOT);
                if (FossilFinderOwlEntity.isPickaxe(stack) && !toolSlot.hasItem()) {
                    moved = this.moveItemStackTo(stack, TOOL_SLOT, TOOL_SLOT + 1, false);
                }
                if (!stack.isEmpty()) {
                    moved |= this.moveItemStackTo(stack, 0, CARGO_SLOTS, false);
                }
            }

            if (!moved) return ItemStack.EMPTY;
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
        if (player.level().isClientSide) return true;
        var entity = player.level().getEntity(owlEntityId);
        return entity instanceof FossilFinderOwlEntity owl
            && owl.isAlive()
            && owl.isOwnedBy(player)
            && owl.distanceToSqr(player) <= 64.0;
    }

    public int getOwlEntityId() {
        return owlEntityId;
    }
}
