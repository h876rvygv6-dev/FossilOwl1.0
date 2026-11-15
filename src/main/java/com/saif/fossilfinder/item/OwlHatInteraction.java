package com.saif.fossilfinder.item;

import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lets the owl wear one hat in its head equipment slot. */
@Mod.EventBusSubscriber(modid = FossilFinderOwl.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OwlHatInteraction {
    private OwlHatInteraction() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof FossilFinderOwlEntity owl)) return;
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack wornHat = owl.getItemBySlot(EquipmentSlot.HEAD);

        if (owl.isTame() && owl.isOwnedBy(player) && owl.isMuseumMode()
            && player.isCrouching() && event.getItemStack().isEmpty()) {
            owl.stopMuseumMode(player);
            cancel(event, InteractionResult.CONSUME);
            return;
        }

        if (player.isCrouching() && event.getItemStack().isEmpty() && wornHat.getItem() instanceof OwlHatItem) {
            owl.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            if (!player.getInventory().add(wornHat.copy())) player.drop(wornHat.copy(), false);
            cancel(event, InteractionResult.CONSUME);
            return;
        }

        ItemStack offered = event.getItemStack();
        if (!(offered.getItem() instanceof OwlHatItem) || !owl.isTame() || !owl.isOwnedBy(player)) return;
        if (!wornHat.isEmpty()) {
            owl.reactToRejectedHat(player);
            cancel(event, InteractionResult.CONSUME);
            return;
        }

        owl.setItemSlot(EquipmentSlot.HEAD, offered.copyWithCount(1));
        if (!player.getAbilities().instabuild) offered.shrink(1);
        if (owl.isMuseumMode()) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("A new hat! I feel extra brave already!"), true);
        } else {
            owl.reactToHatGift();
        }
        cancel(event, InteractionResult.CONSUME);
    }

    private static void cancel(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
