package com.saif.fossilfinder.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A durable wearable hat that grants extra maximum health while worn. */
public class OwlHatItem extends ArmorItem {
    private final double bonusHealth;
    private final UUID healthModifierId;

    public OwlHatItem(String id, double bonusHealth, Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.HELMET, properties);
        this.bonusHealth = bonusHealth;
        this.healthModifierId = UUID.nameUUIDFromBytes(("fossil_finder_owl:" + id + ":bonus_health").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> defaults = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.HEAD) return defaults;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> modifiers = ImmutableMultimap.builder();
        modifiers.putAll(defaults);
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
            healthModifierId, "Hat bonus health", bonusHealth, AttributeModifier.Operation.ADDITION));
        return modifiers.build();
    }
}
