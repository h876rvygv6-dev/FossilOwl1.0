package com.saif.fossilfinder.entity;

import com.saif.fossilfinder.registry.OwlEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

/**
 * Server-authoritative expedition companion. Navigation is deliberately coordinated in one
 * place: danger avoidance > fossil investigation > owner following. This avoids competing
 * FollowOwner/stroll/mining goals repeatedly overwriting each other's paths.
 */
public class FossilFinderOwlEntity extends TamableAnimal implements GeoEntity {
    private static final Set<String> FOSSIL_BLOCKS = Set.of(
        "fossil:stone_fossil", "fossil:deepslate_fossil", "fossil:calcite_fossil",
        "fossil:dripstone_fossil", "fossil:sandstone_fossil", "fossil:red_sandstone_fossil",
        "fossil:tuff_fossil", "fossil:amber_ore");
    private static final Set<String> TAME_ITEMS = Set.of(
        "fossil:fossil_bio", "fossil:fossil_plant", "fossil:fossil_shale", "fossil:fossil_tar");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> cargo = NonNullList.withSize(12, ItemStack.EMPTY);
    private ItemStack pickaxe = ItemStack.EMPTY;
    private ItemStack lightItem = ItemStack.EMPTY;
    private BlockPos target;
    private int scanCooldown;
    private int digTicks;
    private int alarmCooldown;
    private int hootCooldown;
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

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        // No FollowOwnerGoal or stroll goal: the server tick below arbitrates all movement.
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.SitWhenOrderedToSitGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
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

        // Resume normal flight after the alarm state ends.
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

        // Follow at a comfortable offset; stop navigating once close enough.
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
        if (hootCooldown > 0) hootCooldown--;
        else if (random.nextInt(900) == 0) { playHoot(false); hootCooldown = 500; }
    }

    private Monster findNearbyHostile() {
        AABB range = getBoundingBox().inflate(12.0, 8.0, 12.0);
        return level().getEntitiesOfClass(Monster.class, range,
            mob -> mob.isAlive() && mob.getTarget() != this).stream()
            .min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b))).orElse(null);
    }

    private void handleThreat(Monster threat) {
        if (alarmCooldown <= 0) {
            playHoot(true);
            imitateThreat(threat);
            alarmCooldown = 65;
        } else alarmCooldown--;
        excavating = false;
        target = null;
        setNoGravity(true);
        // Climb above the threat and move away from it. Repath periodically, not every tick.
        if (tickCount % 12 == 0 || getNavigation().isDone()) {
            double dx = getX() - threat.getX();
            double dz = getZ() - threat.getZ();
            double length = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            double escapeX = getX() + dx / length * 7.0;
            double escapeZ = getZ() + dz / length * 7.0;
            double escapeY = Math.max(getY() + 5.0, threat.getY() + 6.0);
            getNavigation().moveTo(escapeX, escapeY, escapeZ, 1.35);
        }
    }

    private void imitateThreat(Monster threat) {
        SoundEvent sound = null;
        if (threat instanceof Zombie) sound = SoundEvents.PARROT_IMITATE_ZOMBIE;
        else if (threat instanceof Skeleton) sound = SoundEvents.PARROT_IMITATE_SKELETON;
        else if (threat instanceof Creeper) sound = SoundEvents.PARROT_IMITATE_CREEPER;
        else if (threat instanceof Spider) sound = SoundEvents.PARROT_IMITATE_SPIDER;
        if (sound != null) playSound(sound, 0.8F, 1.15F + random.nextFloat() * 0.2F);
    }

    private void playHoot(boolean alarm) {
        // The existing custom hoot asset is also used for the alarm call; alarm calls are louder/faster.
        playSound(ForgeRegistries.SOUND_EVENTS.getValue(
            new ResourceLocation("fossil_finder_owl", "fossil_owl_hoot")), alarm ? 1.5F : 0.8F,
            alarm ? 1.45F : 0.95F + random.nextFloat() * 0.15F);
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
        if (!excavating) {
            excavating = true;
            playHoot(false);
        }
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
            else transferCargo(owner);
        }
    }

    private BlockPos findFossil() {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-12, -6, -12), origin.offset(12, 6, 12))) {
            if (!level().hasChunkAt(p) || !isFossilBlock(level().getBlockState(p))) continue;
            double d = p.distToCenterSqr(getX(), getY(), getZ());
            if (d < bestDistance && d < 220.0) { best = p.immutable(); bestDistance = d; }
        }
        return best;
    }

    private boolean isFossilBlock(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && FOSSIL_BLOCKS.contains(id.toString()) && pickaxe.isCorrectToolForDrops(state);
    }

    private void excavateTarget() {
        if (target == null || pickaxe.isEmpty() || !level().hasChunkAt(target)) return;
        BlockState state = level().getBlockState(target);
        if (!isFossilBlock(state)) return;
        Block.dropResources(state, level(), target, level().getBlockEntity(target), this, pickaxe);
        level().removeBlock(target, false);
        pickaxe.hurtAndBreak(1, this, e -> { pickaxe = ItemStack.EMPTY; });
        for (ItemEntity drop : level().getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(1.5))) {
            ItemStack stack = drop.getItem().copy();
            addCargo(stack);
            if (stack.isEmpty()) drop.discard(); else drop.setItem(stack);
        }
        if (cargoFull() || pickaxe.isEmpty()) returnToOwner();
    }

    private void addCargo(ItemStack stack) {
        for (int i = 0; i < cargo.size(); i++) {
            if (cargo.get(i).isEmpty()) { cargo.set(i, stack.copy()); stack.setCount(0); return; }
            if (ItemStack.isSameItemSameTags(cargo.get(i), stack) && cargo.get(i).getCount() < cargo.get(i).getMaxStackSize()) {
                int n = Math.min(stack.getCount(), cargo.get(i).getMaxStackSize() - cargo.get(i).getCount());
                cargo.get(i).grow(n); stack.shrink(n); if (stack.isEmpty()) return;
            }
        }
    }

    private boolean cargoFull() {
        return cargo.stream().allMatch(s -> !s.isEmpty() && s.getCount() >= s.getMaxStackSize());
    }

    private void transferCargo(Player player) {
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack stack = cargo.get(i);
            if (!stack.isEmpty() && player.getInventory().add(stack)) cargo.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!isTame() && isTameGift(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            tame(player); setOrderedToSit(false);
            level().broadcastEntityEvent(this, (byte) 7);
            player.displayClientMessage(Component.literal("The owl accepts your fossil and joins your expedition!"), true);
            return InteractionResult.CONSUME;
        }
        if (isTame() && isOwnedBy(player)) {
            if (held.is(Items.BONE)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                setOrderedToSit(!isOrderedToSit()); target = null;
                player.displayClientMessage(Component.literal(isOrderedToSit() ? "Your owl will perch here." : "Your owl will follow your expedition."), true);
                return InteractionResult.CONSUME;
            }
            if (isLight(held)) {
                lightItem = held.copy(); lightItem.setCount(1);
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("Your owl takes the light carefully. (Visual attachment and dynamic cave lighting are not yet implemented.)"), true);
                return InteractionResult.CONSUME;
            }
            if (held.getItem() instanceof net.minecraft.world.item.TieredItem && held.is(Items.IRON_PICKAXE)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                pickaxe = held.copy(); pickaxe.setCount(1); target = null;
                player.displayClientMessage(Component.literal("Your owl is ready for its fossil expedition!"), true);
                return InteractionResult.CONSUME;
            }
            if (held.isEmpty()) {
                transferCargo(player);
                String[] phrases = {"Hoo! Ready for fossils?", "I spotted something interesting!", "Hoo-hoo! Stay close, explorer!", "The cave smells of ancient history!", "I'll keep watch from above!"};
                player.displayClientMessage(Component.literal(phrases[phraseIndex++ % phrases.length]), true);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Cargo", ContainerHelper.saveAllItems(new CompoundTag(), cargo));
        if (!pickaxe.isEmpty()) tag.put("Pickaxe", pickaxe.save(new CompoundTag()));
        if (!lightItem.isEmpty()) tag.put("LightItem", lightItem.save(new CompoundTag()));
        if (target != null) tag.putLong("Target", target.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Cargo")) ContainerHelper.loadAllItems(tag.getCompound("Cargo"), cargo);
        if (tag.contains("Pickaxe")) pickaxe = ItemStack.of(tag.getCompound("Pickaxe"));
        if (tag.contains("LightItem")) lightItem = ItemStack.of(tag.getCompound("LightItem"));
        if (tag.contains("Target")) target = BlockPos.of(tag.getLong("Target"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
            state.setAndContinue(state.isMoving()
                ? RawAnimation.begin().thenLoop("animation.owl.walk")
                : RawAnimation.begin().thenLoop("animation.owl.idle"))));
        controllers.add(new AnimationController<>(this, "excavation", 0, state ->
            excavating ? state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.dig")) : state.stop()));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public net.minecraft.world.entity.AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob mate) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }
}
