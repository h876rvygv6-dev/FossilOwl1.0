package com.saif.fossilfinder.menu;

import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import com.saif.fossilfinder.item.OwlHatItem;
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
    private static final int HAT_SLOT = CARGO_SLOTS;
    private static final int PICKAXE_SLOT = HAT_SLOT + 1;
    private static final int SHOVEL_SLOT = PICKAXE_SLOT + 1;
    private static final int LIGHT_SLOT = SHOVEL_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = LIGHT_SLOT + 1;
    private static final int[] CARGO_COLUMN_X = {141, 161, 182, 202, 223};
    private static final int[] CARGO_ROW_Y = {29, 55, 80};
    private final int owlEntityId;

    public OwlInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, new SimpleContainer(CARGO_SLOTS), new SimpleContainer(1),
            new SimpleContainer(1), new SimpleContainer(1), new SimpleContainer(1), buffer.readVarInt());
    }

    public OwlInventoryMenu(int containerId, Inventory playerInventory, Container cargo, Container hat,
                            Container pickaxe, Container shovel, Container light, int owlEntityId) {
        super(OWL_INVENTORY.get(), containerId);
        this.owlEntityId = owlEntityId;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                addSlot(new Slot(cargo, row * 5 + col, CARGO_COLUMN_X[col], CARGO_ROW_Y[row]));
            }
        }

        addSlot(new Slot(hat, 0, 54, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof OwlHatItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new Slot(pickaxe, 0, 14, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FossilFinderOwlEntity.isPickaxe(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new Slot(shovel, 0, 14, 77) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FossilFinderOwlEntity.isShovel(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new Slot(light, 0, 14, 99) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FossilFinderOwlEntity.isLightItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 47 + col * 18, 128 + row * 25));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 47 + col * 18, 202));
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

            if (index < PLAYER_INVENTORY_START) {
                moved = this.moveItemStackTo(stack, PLAYER_INVENTORY_START, this.slots.size(), true);
            } else {
                moved = false;
                Slot hatSlot = this.slots.get(HAT_SLOT);
                Slot pickaxeSlot = this.slots.get(PICKAXE_SLOT);
                Slot shovelSlot = this.slots.get(SHOVEL_SLOT);
                Slot lightSlot = this.slots.get(LIGHT_SLOT);
                if (stack.getItem() instanceof OwlHatItem && !hatSlot.hasItem()) {
                    moved = this.moveItemStackTo(stack, HAT_SLOT, HAT_SLOT + 1, false);
                }
                if (!stack.isEmpty() && FossilFinderOwlEntity.isPickaxe(stack) && !pickaxeSlot.hasItem()) {
                    moved = this.moveItemStackTo(stack, PICKAXE_SLOT, PICKAXE_SLOT + 1, false);
                }
                if (!stack.isEmpty() && FossilFinderOwlEntity.isShovel(stack) && !shovelSlot.hasItem()) {
                    moved = this.moveItemStackTo(stack, SHOVEL_SLOT, SHOVEL_SLOT + 1, false);
                }
                if (!stack.isEmpty() && FossilFinderOwlEntity.isLightItem(stack) && !lightSlot.hasItem()) {
                    moved = this.moveItemStackTo(stack, LIGHT_SLOT, LIGHT_SLOT + 1, false);
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
