package com.saif.fossilfinder.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

/** Server-authoritative fossil-finding companion. */
public class FossilFinderOwlEntity extends TamableAnimal implements GeoEntity {
    private static final Set<String> FOSSIL_BLOCKS = Set.of(
        "fossil:stone_fossil", "fossil:deepslate_fossil", "fossil:calcite_fossil",
        "fossil:dripstone_fossil", "fossil:sandstone_fossil", "fossil:red_sandstone_fossil",
        "fossil:tuff_fossil", "fossil:amber_ore", "fossil:fossil_ore");
    private static final Set<String> TAME_ITEMS = Set.of(
        "fossil:fossil_bio", "fossil:fossil_plant", "fossil:fossil_shale", "fossil:fossil_tar");
    private static final ResourceLocation HOOT_ID = new ResourceLocation("fossil_finder_owl", "fossil_owl_hoot");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> cargo = NonNullList.withSize(27, ItemStack.EMPTY);
    private ItemStack pickaxe = ItemStack.EMPTY;
    private ItemStack lightItem = ItemStack.EMPTY;
    private BlockPos target;
    private int scanCooldown;
    private int digTicks;
    private int alarmCooldown;
    private int phraseIndex;
    private boolean excavating;

    public FossilFinderOwlEntity(EntityType<FossilFinderOwlEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 18, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 14.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FLYING_SPEED, 0.32)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override protected SoundEvent getAmbientSound() { return sound(HOOT_ID); }
    @Override protected int getAmbientSoundInterval() { return 120; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return sound(HOOT_ID); }
    @Override protected SoundEvent getDeathSound() { return sound(HOOT_ID); }
    private static SoundEvent sound(ResourceLocation id) { return ForgeRegistries.SOUND_EVENTS.getValue(id); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!isTame() || isOrderedToSit() || getOwner() == null) {
            setNoGravity(false);
            return;
        }
        Monster threat = findNearbyHostile();
        if (threat != null) {
            handleThreat(threat);
            return;
        }
        setNoGravity(true);
        if (alarmCooldown > 0) alarmCooldown--;
        if (cargoFull()) {
            target = null;
            returnToOwner();
            return;
        }
        if (!pickaxe.isEmpty()) {
            if (target == null || !isFossilBlock(level().getBlockState(target))) {
                if (scanCooldown-- <= 0) {
                    target = findFossil();
                    scanCooldown = 30;
                    digTicks = 0;
                }
            }
            if (target != null) {
                investigateAndExcavate();
                return;
            }
        }
        if (getOwner() instanceof Player owner) {
            double distance = distanceToSqr(owner);
            if (distance > 30.0) getNavigation().moveTo(owner, 1.0);
            else if (distance < 7.0) getNavigation().stop();
            else if (getNavigation().isDone()) getNavigation().moveTo(owner, 0.9);
            if (distance > 400.0) {
                teleportTo(owner.getX() + 1.0, owner.getY() + 1.0, owner.getZ() + 1.0);
                getNavigation().stop();
            }
        }
    }

    private Monster findNearbyHostile() {
        return level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(12.0, 8.0, 12.0),
            mob -> mob.isAlive() && mob.getTarget() != this).stream()
            .min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b))).orElse(null);
    }

    private void handleThreat(Monster threat) {
        if (alarmCooldown <= 0) {
            playHoot(true);
            SoundEvent mimic = threat instanceof Zombie ? SoundEvents.PARROT_IMITATE_ZOMBIE
                : threat instanceof Skeleton ? SoundEvents.PARROT_IMITATE_SKELETON
                : threat instanceof Creeper ? SoundEvents.PARROT_IMITATE_CREEPER
                : threat instanceof Spider ? SoundEvents.PARROT_IMITATE_SPIDER : null;
            if (mimic != null) playSound(mimic, 0.8F, 1.1F);
            alarmCooldown = 65;
        }
        excavating = false;
        target = null;
        setNoGravity(true);
        if (tickCount % 12 == 0 || getNavigation().isDone()) {
            double dx = getX() - threat.getX();
            double dz = getZ() - threat.getZ();
            double length = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            getNavigation().moveTo(getX() + dx / length * 7.0,
                Math.max(getY() + 5.0, threat.getY() + 6.0), getZ() + dz / length * 7.0, 1.35);
        }
    }

    private void playHoot(boolean alarm) {
        SoundEvent hoot = sound(HOOT_ID);
        if (hoot != null) playSound(hoot, alarm ? 1.5F : 0.8F, alarm ? 1.35F : 0.95F + random.nextFloat() * 0.15F);
    }

    private void investigateAndExcavate() {
        if (target == null) return;
        if (!level().hasChunkAt(target) || !isFossilBlock(level().getBlockState(target))) {
            target = null;
            excavating = false;
            return;
        }
        double distance = distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distance > 5.0) {
            excavating = false;
            if (getNavigation().isDone() || tickCount % 20 == 0)
                getNavigation().moveTo(target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, 1.05);
            return;
        }
        getNavigation().stop();
        excavating = true;
        if (++digTicks >= 55) {
            excavateTarget();
            digTicks = 0;
            target = null;
            excavating = false;
        }
    }

    private void returnToOwner() {
        if (getOwner() instanceof Player owner) {
            if (distanceToSqr(owner) > 8.0) getNavigation().moveTo(owner, 1.0);
        }
    }

    private BlockPos findFossil() {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -6, -12), origin.offset(12, 6, 12))) {
            if (!level().hasChunkAt(pos) || !isFossilBlock(level().getBlockState(pos))) continue;
            double distance = pos.distToCenterSqr(getX(), getY(), getZ());
            if (distance < bestDistance && distance < 220.0) { best = pos.immutable(); bestDistance = distance; }
        }
        return best;
    }

    private boolean isFossilBlock(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        boolean fossil = FOSSIL_BLOCKS.contains(id.toString())
            || (id.getNamespace().equals("fossil") && (id.getPath().contains("fossil") || id.getPath().contains("amber")));
        return fossil && !pickaxe.isEmpty() && pickaxe.isCorrectToolForDrops(state);
    }

    private void excavateTarget() {
        if (target == null || pickaxe.isEmpty() || !level().hasChunkAt(target) || !(level() instanceof ServerLevel serverLevel)) return;
        BlockState state = level().getBlockState(target);
        if (!isFossilBlock(state)) return;
        BlockPos minedPos = target;
        for (ItemStack drop : Block.getDrops(state, serverLevel, minedPos, level().getBlockEntity(minedPos), this, pickaxe)) {
            ItemStack remainder = addCargo(drop);
            if (!remainder.isEmpty()) {
                level().addFreshEntity(new ItemEntity(level(), minedPos.getX() + 0.5, minedPos.getY() + 0.5, minedPos.getZ() + 0.5, remainder));
            }
        }
        level().removeBlock(minedPos, false);
        pickaxe.hurtAndBreak(1, this, entity -> pickaxe = ItemStack.EMPTY);
        playHoot(false);
        if (cargoFull() || pickaxe.isEmpty()) returnToOwner();
    }

    private ItemStack addCargo(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < cargo.size() && !remaining.isEmpty(); i++) {
            ItemStack slot = cargo.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(moved);
                remaining.shrink(moved);
            }
        }
        for (int i = 0; i < cargo.size() && !remaining.isEmpty(); i++) {
            if (cargo.get(i).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                cargo.set(i, remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }
        }
        return remaining;
    }

    private boolean cargoFull() { return cargo.stream().allMatch(stack -> !stack.isEmpty() && stack.getCount() >= stack.getMaxStackSize()); }

    private void openCargo(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SimpleContainer inventory = new SimpleContainer(27);
            for (int i = 0; i < cargo.size(); i++) inventory.setItem(i, cargo.get(i));
            serverPlayer.openMenu(new SimpleMenuProvider((id, playerInventory, viewer) ->
                ChestMenu.threeRows(id, playerInventory, inventory), Component.literal("Owl Cargo")));
            // Copy changes made in the screen back into the entity when the menu is closed.
            serverPlayer.containerMenu.addSlotListener((menu, slot, stack) -> {
                if (menu == serverPlayer.containerMenu) {
                    for (int i = 0; i < cargo.size(); i++) cargo.set(i, inventory.getItem(i).copy());
                }
            });
        }
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!isTame() && isTameGift(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            tame(player);
            setOrderedToSit(false);
            level().broadcastEntityEvent(this, (byte) 7);
            player.displayClientMessage(Component.literal("The owl accepts your fossil and joins your expedition!"), true);
            return InteractionResult.CONSUME;
        }
        if (isTame() && isOwnedBy(player)) {
            if (held.is(Items.BONE)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                setOrderedToSit(!isOrderedToSit());
                target = null;
                player.displayClientMessage(Component.literal(isOrderedToSit() ? "Your owl will perch here." : "Your owl will follow your expedition."), true);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.WOODEN_PICKAXE) || held.is(Items.STONE_PICKAXE) || held.is(Items.IRON_PICKAXE)
                || held.is(Items.GOLDEN_PICKAXE) || held.is(Items.DIAMOND_PICKAXE) || held.is(Items.NETHERITE_PICKAXE)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                pickaxe = held.copyWithCount(1);
                target = null;
                player.displayClientMessage(Component.literal("Your owl is ready for its fossil expedition!"), true);
                return InteractionResult.CONSUME;
            }
            if (isLight(held)) {
                lightItem = held.copyWithCount(1);
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("Your owl takes the light. (Held-item rendering and dynamic lighting require model/render support.)"), true);
                return InteractionResult.CONSUME;
            }
            if (held.isEmpty()) {
                openCargo(player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    private boolean isLight(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
    }
    private boolean isTameGift(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && TAME_ITEMS.contains(id.toString());
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Cargo", ContainerHelper.saveAllItems(new CompoundTag(), cargo));
        if (!pickaxe.isEmpty()) tag.put("Pickaxe", pickaxe.save(new CompoundTag()));
        if (!lightItem.isEmpty()) tag.put("LightItem", lightItem.save(new CompoundTag()));
        if (target != null) tag.putLong("Target", target.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Cargo")) ContainerHelper.loadAllItems(tag.getCompound("Cargo"), cargo);
        if (tag.contains("Pickaxe")) pickaxe = ItemStack.of(tag.getCompound("Pickaxe"));
        if (tag.contains("LightItem")) lightItem = ItemStack.of(tag.getCompound("LightItem"));
        if (tag.contains("Target")) target = BlockPos.of(tag.getLong("Target"));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
            state.setAndContinue(state.isMoving()
                ? RawAnimation.begin().thenLoop("animation.owl.walk")
                : RawAnimation.begin().thenLoop("animation.owl.idle"))));
        controllers.add(new AnimationController<>(this, "excavation", 0, state -> excavating
            ? state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.dig")) : PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob mate) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }
}
