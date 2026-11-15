package com.saif.fossilfinder.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.saif.fossilfinder.FossilFinderOwl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A durable wearable hat that grants extra maximum health while worn. */
public class OwlHatItem extends ArmorItem {
    private static final ArmorMaterial MATERIAL = new HatMaterial();
    private final double bonusHealth;
    private final UUID healthModifierId;

    public OwlHatItem(String id, double bonusHealth, Properties properties) {
        super(MATERIAL, Type.HELMET, properties);
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

    private static final class HatMaterial implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> 407;
                case CHESTPLATE -> 592;
                case LEGGINGS -> 555;
                case BOOTS -> 481;
            };
        }

        @Override public int getDefenseForType(Type type) { return 0; }
        @Override public int getEnchantmentValue() { return ArmorMaterials.NETHERITE.getEnchantmentValue(); }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.STRING); }
        @Override public String getName() { return FossilFinderOwl.MODID + ":owl_hat"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    }
}
