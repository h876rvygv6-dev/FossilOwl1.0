package com.saif.fossilfinder.registry;

import com.saif.fossilfinder.FossilFinderOwl;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OwlMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FossilFinderOwl.MODID);
    public static final RegistryObject<MenuType<com.saif.fossilfinder.menu.OwlInventoryMenu>> OWL_INVENTORY = MENUS.register(
        "owl_inventory",
        () -> IForgeMenuType.create(com.saif.fossilfinder.menu.OwlInventoryMenu::new)
    );

    private OwlMenus() {}
}
