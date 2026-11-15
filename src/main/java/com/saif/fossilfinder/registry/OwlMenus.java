package com.saif.fossilfinder.registry;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.menu.OwlInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OwlMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FossilFinderOwl.MODID);
    public static final RegistryObject<MenuType<OwlInventoryMenu>> OWL_INVENTORY = MENUS.register(
        "owl_inventory",
        () -> IForgeMenuType.create((windowId, inv, data) -> new OwlInventoryMenu(windowId, inv, data))
    );

    private OwlMenus() {}
}
