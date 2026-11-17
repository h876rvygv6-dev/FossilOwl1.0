package com.saif.fossilfinder.entity;

import com.saif.fossilfinder.menu.OwlInventoryMenu;
import com.saif.fossilfinder.item.OwlHatItem;
import com.saif.fossilfinder.fieldwork.FieldworkDialogue;
import com.saif.fossilfinder.museum.MuseumBoneClassifier;
import com.saif.fossilfinder.museum.MuseumDialogue;
import com.saif.fossilfinder.registry.OwlItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Server-authoritative fossil-finding companion. */
public class FossilFinderOwlEntity extends TamableAnimal implements GeoEntity {
    private static final RawAnimation IDLE_ANIMATION =
        RawAnimation.begin().thenLoop("animation.fossil_finder_owl.standing");
    private static final RawAnimation MOVING_ANIMATION =
        RawAnimation.begin().thenLoop("animation.fossil_finder_owl.moving");
    private static final RawAnimation FLYING_ANIMATION =
        RawAnimation.begin().thenLoop("animation.fossil_finder_owl.flying");
    private static final RawAnimation SITTING_ANIMATION =
        RawAnimation.begin().thenLoop("animation.fossil_finder_owl.sitting");
    private static final RawAnimation MINING_ANIMATION =
        RawAnimation.begin().thenLoop("animation.fossil_finder_owl.mining");
    private static final RawAnimation DANCE_ANIMATION =
        RawAnimation.begin().thenPlayXTimes("animation.fossil_finder_owl.dance", 2);

    private static final EntityDataAccessor<ItemStack> DATA_PICKAXE = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_ACTIVE_TOOL = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_LIGHT_ITEM = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_MUSEUM_MODE = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MINING = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Set<String> FOSSIL_BLOCKS = Set.of(
        "fossil:fossil_stone", "fossil:fossil_deepslate", "fossil:fossil_calcite",
        "fossil:fossil_dripstone", "fossil:fossil_sandstone", "fossil:fossil_red_sandstone",
        "fossil:fossil_tuff", "fossil:amber_ore", "fossil:fossil_ore");
    private static final Set<String> TAME_ITEMS = Set.of(
        "fossil:fossil_bio", "fossil:fossil_plant", "fossil:fossil_shale", "fossil:fossil_tar");
    private static final Set<String> HEALING_FOODS = Set.of(
        "minecraft:apple", "minecraft:carrot", "minecraft:wheat_seeds", "minecraft:beetroot_seeds",
        "minecraft:melon_seeds", "minecraft:pumpkin_seeds", "minecraft:torchflower_seeds", "minecraft:pitcher_pod",
        "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish",
        "minecraft:cooked_cod", "minecraft:cooked_salmon", "minecraft:beef", "minecraft:cooked_beef",
        "minecraft:porkchop", "minecraft:cooked_porkchop", "minecraft:mutton", "minecraft:cooked_mutton",
        "minecraft:chicken", "minecraft:cooked_chicken", "minecraft:rabbit", "minecraft:cooked_rabbit");
    private static final ResourceLocation HOOT_ID = new ResourceLocation("fossil_finder_owl", "fossil_owl_hoot");
    private static final int CARGO_SIZE = 15;
    private static final int MINING_SOUND_INTERVAL_TICKS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CargoContainer cargo = new CargoContainer(CARGO_SIZE);
    private NonNullList<ItemStack> pendingCargo = NonNullList.withSize(12, ItemStack.EMPTY);
    private final HatContainer hatContainer = new HatContainer();
    private final PickaxeContainer pickaxeContainer = new PickaxeContainer();
    private final ShovelContainer shovelContainer = new ShovelContainer();
    private final LightContainer lightContainer = new LightContainer();
    private ItemStack pickaxe = ItemStack.EMPTY;
    private ItemStack shovel = ItemStack.EMPTY;
    private ItemStack lightItem = ItemStack.EMPTY;
    private ItemStack museumBook = ItemStack.EMPTY;
    private boolean museumBookConsumed;
    private final Set<String> museumDiscoveredSpecies = new HashSet<>();
    private final Set<String> museumFactSpecies = new HashSet<>();
    private final Map<String, Integer> museumFactCounts = new HashMap<>();
    private final Queue<MuseumDialogue.Cue> museumDialogueQueue = new ArrayDeque<>();
    private final Queue<String> fieldworkDialogueQueue = new ArrayDeque<>();
    private BlockPos target;
    private BlockPos miningTarget;
    private int scanCooldown;
    private int digTicks;
    private int alarmCooldown;
    private int museumScanCooldown;
    private int museumSpeechCooldown;
    private int museumStatusCooldown;
    private MuseumDialogue.CueType museumStatusCue;
    private int fieldworkSpeechCooldown;
    private int fieldworkFollowTicks;
    private int fieldworkMiningCount;
    private int fieldworkMiningCommentCooldown;
    private int fieldworkShovelRequestCooldown;
    private int fieldworkDiscoveryCooldown;
    private int fieldworkQuietTicks;
    private int fieldworkQuietCooldown;
    private int upsetTicks;
    private boolean inventoryPreviewRendering;

    public FossilFinderOwlEntity(EntityType<FossilFinderOwlEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 18, true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_PICKAXE, ItemStack.EMPTY);
        entityData.define(DATA_ACTIVE_TOOL, ItemStack.EMPTY);
        entityData.define(DATA_LIGHT_ITEM, ItemStack.EMPTY);
        entityData.define(DATA_MUSEUM_MODE, false);
        entityData.define(DATA_MINING, false);
    }

    private void setPickaxe(ItemStack stack) {
        pickaxe = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        entityData.set(DATA_PICKAXE, pickaxe.copy());
        pickaxeContainer.setFromEntity(pickaxe);
    }

    private void setShovel(ItemStack stack) {
        shovel = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        shovelContainer.setFromEntity(shovel);
    }

    private void setLightItem(ItemStack stack) {
        lightItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        entityData.set(DATA_LIGHT_ITEM, lightItem.copy());
    }

    public ItemStack getMiningToolForRender() {
        ItemStack activeTool = entityData.get(DATA_ACTIVE_TOOL);
        return activeTool.isEmpty() ? entityData.get(DATA_PICKAXE) : activeTool;
    }
    public ItemStack getLightForRender() { return entityData.get(DATA_LIGHT_ITEM); }
    public boolean isMuseumMode() { return entityData.get(DATA_MUSEUM_MODE); }
    public boolean isInventoryPreviewRendering() { return inventoryPreviewRendering; }
    public void setInventoryPreviewRendering(boolean inventoryPreviewRendering) {
        this.inventoryPreviewRendering = inventoryPreviewRendering;
    }

    private void setMuseumMode(boolean museumMode) {
        entityData.set(DATA_MUSEUM_MODE, museumMode);
    }

    private boolean isMining() {
        return entityData.get(DATA_MINING);
    }

    private void setMining(boolean mining) {
        if (isMining() != mining) entityData.set(DATA_MINING, mining);
        if (!mining && !entityData.get(DATA_ACTIVE_TOOL).isEmpty()) {
            entityData.set(DATA_ACTIVE_TOOL, ItemStack.EMPTY);
        }
    }

    private void setActiveMiningTool(ItemStack tool) {
        ItemStack current = entityData.get(DATA_ACTIVE_TOOL);
        if (!ItemStack.matches(current, tool)) entityData.set(DATA_ACTIVE_TOOL, tool.copy());
    }

    private void resetMiningProgress() {
        setMining(false);
        miningTarget = null;
        digTicks = 0;
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

    @Override public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) { return false; }
    @Override protected SoundEvent getAmbientSound() { return sound(HOOT_ID); }
    @Override public int getAmbientSoundInterval() { return 120; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return sound(HOOT_ID); }
    @Override protected SoundEvent getDeathSound() { return sound(HOOT_ID); }
    private static SoundEvent sound(ResourceLocation id) { return ForgeRegistries.SOUND_EVENTS.getValue(id); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (target == null) resetMiningProgress();
        if (upsetTicks > 0) {
            upsetTicks--;
            if (getOwner() instanceof Player owner) turnAwayFrom(owner);
        }
        if (!isTame() || getOwner() == null) {
            resetMiningProgress();
            target = null;
            setNoGravity(false);
            return;
        }
        if (!isMuseumMode() && getOwner() instanceof Player owner) tickFieldworkDialogue(owner);
        if (isOrderedToSit()) {
            resetMiningProgress();
            target = null;
            getNavigation().stop();
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
        restorePendingCargo();
        if (isMuseumMode()) {
            resetMiningProgress();
            if (getOwner() instanceof Player owner) {
                tickMuseumDialogue(owner);
                tickMuseumMode(owner);
            }
            return;
        }
        collectNearbyDrops();
        if (!pickaxe.isEmpty() && target == null) {
            if (++fieldworkQuietTicks >= 6000 && fieldworkQuietCooldown <= 0) {
                queueFieldworkDialogue(FieldworkDialogue.Cue.QUIET_EXPEDITION);
                fieldworkQuietTicks = 0;
                fieldworkQuietCooldown = 6000;
            }
        } else {
            fieldworkQuietTicks = 0;
        }
        if (getOwner() instanceof Player owner && distanceToSqr(owner) > 64.0) {
            resetMiningProgress();
            recordFieldworkFollowing();
            target = null;
            if (distanceToSqr(owner) > 400.0) {
                teleportTo(owner.getX() + 1.0, owner.getY() + 1.0, owner.getZ() + 1.0);
                getNavigation().stop();
            } else if (getNavigation().isDone() || tickCount % 10 == 0) {
                getNavigation().moveTo(owner, 1.0);
            }
            return;
        }
        if (cargoFull()) {
            resetMiningProgress();
            target = null;
            returnToOwner();
            return;
        }
        if (pickaxe.isEmpty()) resetMiningProgress();
        if (!pickaxe.isEmpty()) {
            if (target == null || !isFossilBlock(level().getBlockState(target))) {
                if (scanCooldown-- <= 0) {
                    target = findFossil();
                    scanCooldown = 30;
                    resetMiningProgress();
                }
            }
            if (target != null) {
                investigateAndExcavate();
                return;
            }
        }
        if (getOwner() instanceof Player owner) {
            double distance = distanceToSqr(owner);
            if (distance > 30.0) {
                recordFieldworkFollowing();
                getNavigation().moveTo(owner, 1.0);
            } else if (distance < 7.0) {
                fieldworkFollowTicks = 0;
                getNavigation().stop();
            } else if (getNavigation().isDone()) {
                fieldworkFollowTicks = 0;
                getNavigation().moveTo(owner, 0.9);
            }
            if (distance > 400.0) {
                teleportTo(owner.getX() + 1.0, owner.getY() + 1.0, owner.getZ() + 1.0);
                getNavigation().stop();
            }
        }
    }

    private void tickMuseumMode(Player owner) {
        target = null;
        digTicks = 0;
        if (museumStatusCooldown > 0) museumStatusCooldown--;
        if (museumScanCooldown > 0) {
            museumScanCooldown--;
            return;
        }
        museumScanCooldown = 10;

        double ownerDistance = distanceToSqr(owner);
        if (ownerDistance > 64.0) {
            if (ownerDistance > 400.0) {
                teleportTo(owner.getX() + 1.0, owner.getY() + 1.0, owner.getZ() + 1.0);
                getNavigation().stop();
            } else if (getNavigation().isDone() || tickCount % 10 == 0) {
                getNavigation().moveTo(owner, 1.0);
            }
            return;
        }

        int cargoSlot = findMuseumBoneSlot();
        if (cargoSlot < 0) {
            reportMuseumStatus(MuseumDialogue.CueType.NEEDS_SPECIMENS);
            followOwner(owner);
            return;
        }

        String species = museumSpecies(cargo.getItem(cargoSlot));
        if (species == null) {
            followOwner(owner);
            return;
        }
        if (museumStatusCue == MuseumDialogue.CueType.NEEDS_SPECIMENS) clearMuseumStatus();
        if (museumDiscoveredSpecies.add(species)) {
            enqueueMuseumDialogue(MuseumDialogue.CueType.SPECIES_RECEIVED, species);
            reactHappy();
        }

        ItemFrame frame = findMuseumFrame(species);
        if (frame == null) {
            reportMuseumStatus(MuseumDialogue.CueType.NEEDS_FRAMES);
            followOwner(owner);
            return;
        }
        if (distanceToSqr(frame) > 4.0) {
            if (getNavigation().isDone() || tickCount % 10 == 0) {
                boolean pathFound = getNavigation().moveTo(frame, 0.9);
                if (!pathFound) {
                    reportMuseumStatus(MuseumDialogue.CueType.FRAME_UNREACHABLE);
                } else {
                    clearMuseumStatus();
                }
            }
            return;
        }

        clearMuseumStatus();
        getNavigation().stop();
        if (!frame.isAlive() || !frame.getItem().isEmpty()) return;

        ItemStack displayStack = cargo.getItem(cargoSlot).copyWithCount(1);
        frame.setItem(displayStack, true);
        if (ItemStack.isSameItemSameTags(frame.getItem(), displayStack)) {
            cargo.removeItem(cargoSlot, 1);
            if (museumFactSpecies.add(species)) {
                museumFactCounts.putIfAbsent(species, 0);
                enqueueMuseumDialogue(MuseumDialogue.CueType.SPECIES_DISPLAYED, species);
            }
            museumScanCooldown = 20;
            reactHappy();
            playHoot(false);
        }
    }

    private int findMuseumBoneSlot() {
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            if (museumSpecies(cargo.getItem(slot)) != null) return slot;
        }
        return -1;
    }

    private String museumSpecies(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : MuseumBoneClassifier.speciesFor(id.getNamespace(), id.getPath()).orElse(null);
    }

    private ItemFrame findMuseumFrame(String species) {
        var frames = level().getEntitiesOfClass(ItemFrame.class, getBoundingBox().inflate(8.0),
            frame -> frame.isAlive() && !frame.isInvisible() && frame.distanceToSqr(this) <= 64.0);
        var emptyFrames = frames.stream().filter(frame -> frame.getItem().isEmpty()).toList();
        if (emptyFrames.isEmpty()) return null;

        var matchingDisplays = frames.stream()
            .filter(frame -> species.equals(museumSpecies(frame.getItem())))
            .toList();
        return emptyFrames.stream()
            .min((first, second) -> {
                int groupDistance = Double.compare(
                    distanceToSpeciesDisplay(first, matchingDisplays),
                    distanceToSpeciesDisplay(second, matchingDisplays));
                if (groupDistance != 0) return groupDistance;
                int owlDistance = Double.compare(first.distanceToSqr(this), second.distanceToSqr(this));
                if (owlDistance != 0) return owlDistance;
                return Long.compare(first.blockPosition().asLong(), second.blockPosition().asLong());
            })
            .orElse(null);
    }

    private double distanceToSpeciesDisplay(ItemFrame frame, List<ItemFrame> matchingDisplays) {
        if (matchingDisplays.isEmpty()) return frame.distanceToSqr(this);
        return matchingDisplays.stream().mapToDouble(frame::distanceToSqr).min().orElse(Double.MAX_VALUE);
    }

    private void followOwner(Player owner) {
        double distance = distanceToSqr(owner);
        if (distance > 30.0) getNavigation().moveTo(owner, 1.0);
        else if (distance < 7.0) getNavigation().stop();
        else if (getNavigation().isDone()) getNavigation().moveTo(owner, 0.9);
    }

    private void tickMuseumDialogue(Player owner) {
        if (museumSpeechCooldown > 0) museumSpeechCooldown--;
        if (museumSpeechCooldown > 0 || museumDialogueQueue.isEmpty()) return;

        MuseumDialogue.Cue cue = museumDialogueQueue.poll();
        int factIndex = museumFactCounts.getOrDefault(cue.species(), 0);
        String line = MuseumDialogue.line(
            cue, ((ServerLevel) level()).getServer().getResourceManager(), random.nextInt(24), factIndex);
        sendDialogue(owner, line);
        museumSpeechCooldown = 80;
        if (cue.type() == MuseumDialogue.CueType.SPECIES_DISPLAYED) {
            museumFactCounts.put(cue.species(), factIndex + 1);
        }
    }

    private void tickFieldworkDialogue(Player owner) {
        if (fieldworkSpeechCooldown > 0) fieldworkSpeechCooldown--;
        if (fieldworkMiningCommentCooldown > 0) fieldworkMiningCommentCooldown--;
        if (fieldworkShovelRequestCooldown > 0) fieldworkShovelRequestCooldown--;
        if (fieldworkDiscoveryCooldown > 0) fieldworkDiscoveryCooldown--;
        if (fieldworkQuietCooldown > 0) fieldworkQuietCooldown--;
        if (fieldworkSpeechCooldown > 0 || fieldworkDialogueQueue.isEmpty()) return;

        sendDialogue(owner, fieldworkDialogueQueue.poll());
        fieldworkSpeechCooldown = 40;
    }

    private void sendDialogue(Player recipient, String line) {
        recipient.displayClientMessage(
            Component.empty().append(getName()).append(Component.literal(": " + line)), false);
    }

    private void queueFieldworkDialogue(FieldworkDialogue.Cue cue) {
        if (isMuseumMode() || fieldworkDialogueQueue.size() >= 10) return;
        String phrase = FieldworkDialogue.line(cue, random.nextInt(24));
        for (String line : FieldworkDialogue.sentences(phrase)) {
            if (fieldworkDialogueQueue.size() >= 10) break;
            if (!fieldworkDialogueQueue.contains(line)) fieldworkDialogueQueue.add(line);
        }
    }

    private void recordFieldworkFollowing() {
        if (++fieldworkFollowTicks >= 2400) {
            fieldworkFollowTicks = 0;
            queueFieldworkDialogue(FieldworkDialogue.Cue.FOLLOWING);
        }
    }

    public void reactToOwnerMining(Player player, BlockPos brokenPos, BlockState state) {
        if (level().isClientSide || !isTame() || !isOwnedBy(player) || isMuseumMode()
            || distanceToSqr(brokenPos.getX() + 0.5, brokenPos.getY() + 0.5, brokenPos.getZ() + 0.5) > 256.0) {
            return;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && (FOSSIL_BLOCKS.contains(id.toString())
            || (id.getNamespace().equals("fossil")
                && (id.getPath().contains("fossil") || id.getPath().contains("amber"))))) {
            fieldworkMiningCount = 0;
            reactHappy();
            if (fieldworkDiscoveryCooldown <= 0) {
                queueFieldworkDialogue(FieldworkDialogue.Cue.OWNER_FOUND_FOSSIL);
                fieldworkDiscoveryCooldown = 800;
            }
            return;
        }

        if (isCommonStone(state)) {
            fieldworkMiningCount++;
            if (fieldworkMiningCount >= 8 && fieldworkMiningCommentCooldown <= 0) {
                queueFieldworkDialogue(FieldworkDialogue.Cue.MINING_COMMENT);
                fieldworkMiningCount = 0;
                fieldworkMiningCommentCooldown = 2400;
            }
        }
    }

    private boolean isCommonStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
            || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE)
            || state.is(Blocks.ANDESITE) || state.is(Blocks.TUFF);
    }

    public void reactToHatGift() {
        if (!isTame()) return;
        reactHappy();
        if (isMuseumMode()) {
            enqueueMuseumDialogue(MuseumDialogue.CueType.GIFT_RECEIVED, "hat");
        } else {
            queueFieldworkDialogue(FieldworkDialogue.Cue.HAT_GIFT);
        }
    }

    public void reactToRejectedHat(Player player) {
        if (isTame() && isOwnedBy(player)) {
            reactUpset(player, FieldworkDialogue.Cue.REJECTED_HAT, "hat");
        }
    }

    private void returnGift(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
    }

    private void givePickaxe(Player player, ItemStack held) {
        ItemStack previousPickaxe = pickaxe.copy();
        setPickaxe(held.copyWithCount(1));
        if (!player.getAbilities().instabuild) held.shrink(1);
        returnGift(player, previousPickaxe);
        target = null;
        resetMiningProgress();
        reactHappy();
        queueFieldworkDialogue(FieldworkDialogue.Cue.PICKAXE);
    }

    private void giveShovel(Player player, ItemStack held) {
        ItemStack previousShovel = shovel.copy();
        setShovel(held.copyWithCount(1));
        if (!player.getAbilities().instabuild) held.shrink(1);
        returnGift(player, previousShovel);
        resetMiningProgress();
        fieldworkShovelRequestCooldown = 0;
        reactHappy();
        queueFieldworkDialogue(FieldworkDialogue.Cue.SHOVEL);
    }

    private void giveLight(Player player, ItemStack held) {
        ItemStack previousLight = lightItem.copy();
        setLightItem(held.copyWithCount(1));
        if (!player.getAbilities().instabuild) held.shrink(1);
        returnGift(player, previousLight);
        if (isMuseumMode()) {
            enqueueMuseumDialogue(MuseumDialogue.CueType.GIFT_RECEIVED, "light");
        } else {
            queueFieldworkDialogue(FieldworkDialogue.Cue.LIGHT);
        }
        reactHappy();
    }

    private void enqueueMuseumDialogue(MuseumDialogue.CueType type, String species) {
        MuseumDialogue.Cue cue = new MuseumDialogue.Cue(type, species);
        if (!museumDialogueQueue.contains(cue)) museumDialogueQueue.add(cue);
    }

    private void reportMuseumStatus(MuseumDialogue.CueType type) {
        if (museumStatusCue != type) {
            museumStatusCue = type;
            museumStatusCooldown = 0;
            museumDialogueQueue.removeIf(cue -> !isPriorityMuseumDialogue(cue));
        }
        if (museumStatusCooldown <= 0) {
            enqueueMuseumDialogue(type, "");
            museumStatusCooldown = 1200;
        }
    }

    private void clearMuseumStatus() {
        museumStatusCue = null;
        museumStatusCooldown = 0;
        museumDialogueQueue.removeIf(cue -> !isPriorityMuseumDialogue(cue));
    }

    private boolean isPriorityMuseumDialogue(MuseumDialogue.Cue cue) {
        return cue.type() == MuseumDialogue.CueType.SPECIES_RECEIVED
            || cue.type() == MuseumDialogue.CueType.SPECIES_DISPLAYED
            || cue.type() == MuseumDialogue.CueType.GIFT_RECEIVED
            || cue.type() == MuseumDialogue.CueType.GIFT_REJECTED;
    }

    private void reactHappy() {
        triggerAnim("action", "dance");
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.75, getZ(),
                7, 0.35, 0.35, 0.35, 0.02);
        }
    }

    private void reactUpset(Player player, FieldworkDialogue.Cue fieldworkCue, String museumGift) {
        upsetTicks = 40;
        turnAwayFrom(player);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.75, getZ(),
                7, 0.3, 0.35, 0.3, 0.02);
        }
        if (isMuseumMode()) {
            enqueueMuseumDialogue(MuseumDialogue.CueType.GIFT_REJECTED, museumGift);
        } else {
            queueFieldworkDialogue(fieldworkCue);
        }
    }

    private void turnAwayFrom(Player player) {
        float yaw = (float) (Mth.atan2(getZ() - player.getZ(), getX() - player.getX())
            * (180.0 / Math.PI)) - 90.0F;
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
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
        resetMiningProgress();
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
        if (target == null) {
            resetMiningProgress();
            return;
        }
        if (!level().hasChunkAt(target) || !isFossilBlock(level().getBlockState(target))) {
            resetMiningProgress();
            target = null;
            return;
        }
        double distance = distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        emitTargetLockParticles(target, distance);
        getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        BlockPos obstruction = findMiningObstruction(target);
        if (obstruction != null) {
            BlockState obstructionState = level().getBlockState(obstruction);
            if (isPickaxeTunnelBlock(obstructionState) && isPickaxe(pickaxe)
                && pickaxe.isCorrectToolForDrops(obstructionState)) {
                tickMiningBlock(obstruction, pickaxe, false);
                return;
            }
            if (isShovelBlock(obstructionState)) {
                if (isShovel(shovel) && shovel.isCorrectToolForDrops(obstructionState)) {
                    tickMiningBlock(obstruction, shovel, false);
                    return;
                }
                requestShovel();
            }
            resetMiningProgress();
            moveTowardFossil();
            return;
        }

        if (distance > 5.0) {
            resetMiningProgress();
            moveTowardFossil();
            return;
        }
        tickMiningBlock(target, pickaxe, true);
    }

    private BlockPos findMiningObstruction(BlockPos fossilTarget) {
        Vec3 start = new Vec3(getX(), getY() + getBbHeight() * 0.5, getZ());
        Vec3 end = Vec3.atCenterOf(fossilTarget);
        BlockHitResult hit = level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(fossilTarget)) {
            return hit.getBlockPos();
        }
        return null;
    }

    private void emitTargetLockParticles(BlockPos fossilTarget, double distanceToTargetSqr) {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 20 != 0) return;
        boolean nearby = distanceToTargetSqr <= 25.0;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            getX(), getY() + getBbHeight() * 0.7, getZ(),
            nearby ? 4 : 1, 0.25, 0.3, 0.25, 0.02);
        if (nearby) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                fossilTarget.getX() + 0.5, fossilTarget.getY() + 0.6, fossilTarget.getZ() + 0.5,
                5, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private void moveTowardFossil() {
        if (getNavigation().isDone() || tickCount % 20 == 0) {
            getNavigation().moveTo(target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, 1.05);
        }
    }

    private void tickMiningBlock(BlockPos pos, ItemStack tool, boolean fossil) {
        if (!level().hasChunkAt(pos)) {
            resetMiningProgress();
            return;
        }
        BlockState state = level().getBlockState(pos);
        if (fossil ? !isFossilBlock(state) : !isMineableRouteBlock(state, tool)) {
            resetMiningProgress();
            return;
        }
        if (!pos.equals(miningTarget)) {
            miningTarget = pos.immutable();
            digTicks = 0;
            setMining(false);
        }

        double distance = distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > 5.0) {
            setMining(false);
            if (getNavigation().isDone() || tickCount % 20 == 0) {
                getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.05);
            }
            return;
        }

        getNavigation().stop();
        getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        setActiveMiningTool(tool);
        setMining(true);
        if (++digTicks >= 55) {
            if (fossil) {
                excavateTarget();
                target = null;
            } else {
                excavateRouteBlock(pos, tool);
            }
            resetMiningProgress();
        } else if (digTicks % MINING_SOUND_INTERVAL_TICKS == 0) {
            playMiningSound(state, pos, false);
        }
    }

    private boolean isMineableRouteBlock(BlockState state, ItemStack tool) {
        return (isPickaxeTunnelBlock(state) && isPickaxe(tool) && tool.isCorrectToolForDrops(state))
            || (isShovelBlock(state) && isShovel(tool) && tool.isCorrectToolForDrops(state));
    }

    private boolean isPickaxeTunnelBlock(BlockState state) {
        return FossilMiningRules.isPickaxeRouteBlock(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private boolean isShovelBlock(BlockState state) {
        return FossilMiningRules.isShovelRouteBlock(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private void requestShovel() {
        if (fieldworkShovelRequestCooldown > 0) return;
        queueFieldworkDialogue(FieldworkDialogue.Cue.SHOVEL_REQUEST);
        fieldworkShovelRequestCooldown = 1200;
    }

    private void returnToOwner() {
        if (getOwner() instanceof Player owner) {
            if (distanceToSqr(owner) > 16.0) {
                recordFieldworkFollowing();
                getNavigation().moveTo(owner, 1.0);
            }
            else getNavigation().stop();
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
        if (!excavateBlock(minedPos, pickaxe)) return;
        if (fieldworkDiscoveryCooldown <= 0) {
            queueFieldworkDialogue(FieldworkDialogue.Cue.OWL_FOUND_FOSSIL);
            fieldworkDiscoveryCooldown = 800;
        }
        reactHappy();
        playHoot(false);
        if (cargoFull() || pickaxe.isEmpty()) returnToOwner();
    }

    private void excavateRouteBlock(BlockPos pos, ItemStack tool) {
        BlockState state = level().getBlockState(pos);
        if (isMineableRouteBlock(state, tool)) excavateBlock(pos, tool);
    }

    private boolean excavateBlock(BlockPos pos, ItemStack tool) {
        if (tool.isEmpty() || !level().hasChunkAt(pos) || !(level() instanceof ServerLevel serverLevel)) return false;
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) return false;

        playMiningSound(state, pos, true);
        var breaker = getOwner();
        if (breaker == null) breaker = this;
        for (ItemStack drop : Block.getDrops(state, serverLevel, pos, level().getBlockEntity(pos), breaker, tool)) {
            ItemStack remainder = addCargo(drop);
            if (!remainder.isEmpty()) {
                level().addFreshEntity(new ItemEntity(level(), pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, remainder));
            }
        }
        level().removeBlock(pos, false);
        damageMiningTool(tool);
        return true;
    }

    private void damageMiningTool(ItemStack tool) {
        if (tool == pickaxe) {
            tool.hurtAndBreak(1, this, entity -> setPickaxe(ItemStack.EMPTY));
            setPickaxe(pickaxe);
        } else if (tool == shovel) {
            tool.hurtAndBreak(1, this, entity -> setShovel(ItemStack.EMPTY));
            setShovel(shovel);
        }
    }

    private void playMiningSound(BlockState state, BlockPos pos, boolean breaking) {
        SoundType soundType = state.getSoundType();
        SoundEvent sound = breaking ? soundType.getBreakSound() : soundType.getHitSound();
        float volume = (soundType.getVolume() + 1.0F) / 2.0F;
        float pitch = soundType.getPitch() * (breaking ? 0.8F : 0.8F + random.nextFloat() * 0.4F);
        level().playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private void collectNearbyDrops() {
        for (ItemEntity itemEntity : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(1.25))) {
            if (!itemEntity.isAlive()) continue;
            ItemStack item = itemEntity.getItem();
            ItemStack remainder = addCargo(item);
            if (remainder.getCount() == item.getCount()) continue;
            itemEntity.setItem(remainder);
            if (remainder.isEmpty()) itemEntity.discard();
        }
    }

    private ItemStack addCargo(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < cargo.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = cargo.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(moved);
                cargo.setChanged();
                remaining.shrink(moved);
            }
        }
        for (int i = 0; i < cargo.getContainerSize() && !remaining.isEmpty(); i++) {
            if (cargo.getItem(i).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                cargo.setItem(i, remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }
        }
        return remaining;
    }

    private boolean cargoFull() {
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack stack = cargo.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return false;
        }
        return true;
    }

    private void restorePendingCargo() {
        for (int i = 0; i < pendingCargo.size(); i++) {
            ItemStack stack = pendingCargo.get(i);
            if (stack.isEmpty()) continue;
            ItemStack remaining = addCargo(stack);
            if (remaining.getCount() != stack.getCount()) pendingCargo.set(i, remaining);
        }
    }

    private void addPendingCargo(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remaining = stack.copy();
        for (int i = 0; i < pendingCargo.size() && !remaining.isEmpty(); i++) {
            ItemStack slot = pendingCargo.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(moved);
                remaining.shrink(moved);
            }
        }
        for (int i = 0; i < pendingCargo.size() && !remaining.isEmpty(); i++) {
            if (pendingCargo.get(i).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                pendingCargo.set(i, remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }
        }
        if (!remaining.isEmpty()) pendingCargo.add(remaining);
    }

    private void giveFieldNotes(Player player, ItemStack held) {
        if (isMuseumMode()) stopMuseumMode(player);
        ItemStack remaining = addCargo(held.copyWithCount(1));
        if (!remaining.isEmpty()) {
            player.displayClientMessage(Component.literal("My cargo is full; I cannot keep these field notes."), true);
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        reactHappy();
        queueFieldworkDialogue(FieldworkDialogue.Cue.CARGO);
    }

    private void openCargo(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, playerInventory, viewer) ->
                new OwlInventoryMenu(id, playerInventory, cargo, hatContainer, pickaxeContainer, shovelContainer,
                    lightContainer, getId()),
                Component.literal("Fossil Finder Owl")), buffer -> buffer.writeVarInt(getId()));
        }
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!isTame() && isTameGift(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            tame(player);
            setOrderedToSit(false);
            reactHappy();
            queueFieldworkDialogue(FieldworkDialogue.Cue.TAMED);
            return InteractionResult.CONSUME;
        }
        if (isTame() && isOwnedBy(player)) {
            if (held.is(OwlItems.FIELD_NOTES.get())) {
                giveFieldNotes(player, held);
                return InteractionResult.CONSUME;
            }
            if (held.is(OwlItems.MUSEUM_GUIDE.get())) {
                if (isMuseumMode()) {
                    player.displayClientMessage(Component.literal("Your owl is already curating a museum."), true);
                    return InteractionResult.CONSUME;
                }
                museumBook = held.copyWithCount(1);
                museumBookConsumed = !player.getAbilities().instabuild;
                setMuseumMode(true);
                fieldworkDialogueQueue.clear();
                fieldworkSpeechCooldown = 0;
                museumScanCooldown = 0;
                museumSpeechCooldown = 80;
                museumStatusCooldown = 0;
                museumStatusCue = null;
                setOrderedToSit(false);
                if (!player.getAbilities().instabuild) held.shrink(1);
                target = null;
                digTicks = 0;
                getNavigation().stop();
                sendDialogue(player, MuseumDialogue.line(
                    new MuseumDialogue.Cue(MuseumDialogue.CueType.MODE_STARTED, ""),
                    ((ServerLevel) level()).getServer().getResourceManager(), random.nextInt(12), 0));
                return InteractionResult.CONSUME;
            }
            if (isMuseumMode() && held.isEmpty() && player.isCrouching()) {
                stopMuseumMode(player);
                return InteractionResult.CONSUME;
            }
            if (isMuseumMode() && isPickaxe(held)) {
                ItemStack newPickaxe = held.copyWithCount(1);
                ItemStack previousPickaxe = pickaxe.copy();
                if (!player.getAbilities().instabuild) held.shrink(1);
                setPickaxe(newPickaxe);
                returnGift(player, previousPickaxe);
                reactHappy();
                stopMuseumMode(player);
                queueFieldworkDialogue(FieldworkDialogue.Cue.PICKAXE);
                return InteractionResult.CONSUME;
            }
            if (isHealingFood(held) && getHealth() < getMaxHealth()) {
                heal(4.0F);
                if (!player.getAbilities().instabuild) held.shrink(1);
                reactHappy();
                if (isMuseumMode()) {
                    enqueueMuseumDialogue(MuseumDialogue.CueType.GIFT_RECEIVED, "healing");
                } else {
                    queueFieldworkDialogue(FieldworkDialogue.Cue.HEALING);
                }
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.BONE)) {
                if (isMuseumMode()) {
                    sendDialogue(player,
                        "My wings are busy curating. I shall perch when the exhibits are ready.");
                    return InteractionResult.CONSUME;
                }
                setOrderedToSit(!isOrderedToSit());
                target = null;
                digTicks = 0;
                getNavigation().stop();
                fieldworkFollowTicks = 0;
                if (!player.getAbilities().instabuild) held.shrink(1);
                queueFieldworkDialogue(isOrderedToSit()
                    ? FieldworkDialogue.Cue.STAY : FieldworkDialogue.Cue.FOLLOW);
                return InteractionResult.CONSUME;
            }
            if (isRejectedPickaxe(held)) {
                reactUpset(player, FieldworkDialogue.Cue.REJECTED_PICKAXE, "pickaxe");
                return InteractionResult.CONSUME;
            }
            if (isPickaxe(held)) {
                givePickaxe(player, held);
                return InteractionResult.CONSUME;
            }
            if (isShovel(held)) {
                giveShovel(player, held);
                return InteractionResult.CONSUME;
            }
            if (isLightItem(held)) {
                giveLight(player, held);
                return InteractionResult.CONSUME;
            }
            if (held.isEmpty()) {
                if (!isMuseumMode()) queueFieldworkDialogue(FieldworkDialogue.Cue.CARGO);
                openCargo(player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    public void stopMuseumMode(Player player) {
        if (!isMuseumMode()) return;
        setMuseumMode(false);
        museumScanCooldown = 0;
        for (MuseumDialogue.Cue cue : museumDialogueQueue) {
            if (cue.type() == MuseumDialogue.CueType.SPECIES_RECEIVED) {
                museumDiscoveredSpecies.remove(cue.species());
            } else if (cue.type() == MuseumDialogue.CueType.SPECIES_DISPLAYED) {
                museumFactSpecies.remove(cue.species());
            }
        }
        museumDialogueQueue.clear();
        museumStatusCue = null;
        fieldworkFollowTicks = 0;
        target = null;
        digTicks = 0;
        getNavigation().stop();

        ItemStack book = museumBook;
        museumBook = ItemStack.EMPTY;
        if (!book.isEmpty() && museumBookConsumed && !player.getInventory().add(book)) player.drop(book, false);
        museumBookConsumed = false;
        queueFieldworkDialogue(FieldworkDialogue.Cue.FIELDWORK_RETURNED);
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        super.dropAllDeathLoot(source);
        if (!museumBook.isEmpty()) {
            if (museumBookConsumed) spawnAtLocation(museumBook);
            museumBook = ItemStack.EMPTY;
            museumBookConsumed = false;
        }
    }

    private boolean isRejectedPickaxe(ItemStack stack) {
        return stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) || stack.is(Items.GOLDEN_PICKAXE);
    }

    public static boolean isPickaxe(ItemStack stack) {
        return stack.is(Items.IRON_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
    }
    public static boolean isShovel(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem;
    }
    private boolean isHealingFood(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && HEALING_FOODS.contains(id.toString());
    }
    public static boolean isLightItem(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
    }
    private boolean isTameGift(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && TAME_ITEMS.contains(id.toString());
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        NonNullList<ItemStack> cargoItems = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < cargo.getContainerSize(); i++) cargoItems.set(i, cargo.getItem(i).copy());
        tag.put("Cargo", ContainerHelper.saveAllItems(new CompoundTag(), cargoItems));
        tag.put("PendingCargo", ContainerHelper.saveAllItems(new CompoundTag(), pendingCargo));
        if (!pickaxe.isEmpty()) tag.put("Pickaxe", pickaxe.save(new CompoundTag()));
        if (!shovel.isEmpty()) tag.put("Shovel", shovel.save(new CompoundTag()));
        if (!lightItem.isEmpty()) tag.put("LightItem", lightItem.save(new CompoundTag()));
        tag.putBoolean("MuseumMode", isMuseumMode());
        if (!museumBook.isEmpty()) tag.put("MuseumBook", museumBook.save(new CompoundTag()));
        tag.putBoolean("MuseumBookConsumed", museumBookConsumed);
        saveMuseumDialogueData(tag);
        saveFieldworkDialogueData(tag);
        if (target != null) tag.putLong("Target", target.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pendingCargo = NonNullList.withSize(12, ItemStack.EMPTY);
        if (tag.contains("PendingCargo")) {
            CompoundTag pendingTag = tag.getCompound("PendingCargo");
            ListTag items = pendingTag.getList("Items", Tag.TAG_COMPOUND);
            int pendingSize = 12;
            for (int i = 0; i < items.size(); i++) {
                pendingSize = Math.max(pendingSize, (items.getCompound(i).getByte("Slot") & 0xFF) + 1);
            }
            pendingCargo = NonNullList.withSize(pendingSize, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(pendingTag, pendingCargo);
        }
        cargo.setLoading(true);
        try {
            if (tag.contains("Cargo")) {
                NonNullList<ItemStack> cargoItems = NonNullList.withSize(27, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(tag.getCompound("Cargo"), cargoItems);
                for (int i = 0; i < cargo.getContainerSize(); i++) cargo.setItem(i, cargoItems.get(i));
                for (int i = cargo.getContainerSize(); i < cargoItems.size(); i++) addPendingCargo(cargoItems.get(i));
            }
            cargo.setChanged();
        } finally {
            cargo.setLoading(false);
        }
        setPickaxe(tag.contains("Pickaxe") ? ItemStack.of(tag.getCompound("Pickaxe")) : ItemStack.EMPTY);
        setShovel(tag.contains("Shovel") ? ItemStack.of(tag.getCompound("Shovel")) : ItemStack.EMPTY);
        setLightItem(tag.contains("LightItem") ? ItemStack.of(tag.getCompound("LightItem")) : ItemStack.EMPTY);
        museumBook = tag.contains("MuseumBook") ? ItemStack.of(tag.getCompound("MuseumBook")) : ItemStack.EMPTY;
        museumBookConsumed = !museumBook.isEmpty()
            && (!tag.contains("MuseumBookConsumed") || tag.getBoolean("MuseumBookConsumed"));
        setMuseumMode(tag.getBoolean("MuseumMode") && !museumBook.isEmpty());
        loadMuseumDialogueData(tag);
        loadFieldworkDialogueData(tag);
        if (tag.contains("Target")) target = BlockPos.of(tag.getLong("Target"));
    }

    private void saveFieldworkDialogueData(CompoundTag tag) {
        ListTag queuedDialogue = new ListTag();
        for (String line : fieldworkDialogueQueue) {
            queuedDialogue.add(net.minecraft.nbt.StringTag.valueOf(line));
        }
        tag.put("FieldworkDialogueQueue", queuedDialogue);
        tag.putInt("FieldworkSpeechCooldown", fieldworkSpeechCooldown);
        tag.putInt("FieldworkFollowTicks", fieldworkFollowTicks);
        tag.putInt("FieldworkMiningCount", fieldworkMiningCount);
        tag.putInt("FieldworkMiningCommentCooldown", fieldworkMiningCommentCooldown);
        tag.putInt("FieldworkShovelRequestCooldown", fieldworkShovelRequestCooldown);
        tag.putInt("FieldworkDiscoveryCooldown", fieldworkDiscoveryCooldown);
        tag.putInt("FieldworkQuietTicks", fieldworkQuietTicks);
        tag.putInt("FieldworkQuietCooldown", fieldworkQuietCooldown);
    }

    private void loadFieldworkDialogueData(CompoundTag tag) {
        fieldworkDialogueQueue.clear();
        ListTag queuedDialogue = tag.getList("FieldworkDialogueQueue", Tag.TAG_STRING);
        for (int i = 0; i < queuedDialogue.size() && fieldworkDialogueQueue.size() < 10; i++) {
            fieldworkDialogueQueue.add(queuedDialogue.getString(i));
        }
        fieldworkSpeechCooldown = tag.getInt("FieldworkSpeechCooldown");
        fieldworkFollowTicks = tag.getInt("FieldworkFollowTicks");
        fieldworkMiningCount = tag.getInt("FieldworkMiningCount");
        fieldworkMiningCommentCooldown = tag.getInt("FieldworkMiningCommentCooldown");
        fieldworkShovelRequestCooldown = tag.getInt("FieldworkShovelRequestCooldown");
        fieldworkDiscoveryCooldown = tag.getInt("FieldworkDiscoveryCooldown");
        fieldworkQuietTicks = tag.getInt("FieldworkQuietTicks");
        fieldworkQuietCooldown = tag.getInt("FieldworkQuietCooldown");
    }

    private void saveMuseumDialogueData(CompoundTag tag) {
        ListTag discovered = new ListTag();
        for (String species : museumDiscoveredSpecies) discovered.add(net.minecraft.nbt.StringTag.valueOf(species));
        tag.put("MuseumDiscoveredSpecies", discovered);

        ListTag facts = new ListTag();
        for (String species : museumFactSpecies) facts.add(net.minecraft.nbt.StringTag.valueOf(species));
        tag.put("MuseumFactSpecies", facts);

        CompoundTag factCounts = new CompoundTag();
        museumFactCounts.forEach(factCounts::putInt);
        tag.put("MuseumFactCounts", factCounts);

        ListTag queuedDialogue = new ListTag();
        for (MuseumDialogue.Cue cue : museumDialogueQueue) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Type", cue.type().name());
            entry.putString("Species", cue.species());
            queuedDialogue.add(entry);
        }
        tag.put("MuseumDialogueQueue", queuedDialogue);
        tag.putInt("MuseumSpeechCooldown", museumSpeechCooldown);
        tag.putInt("MuseumStatusCooldown", museumStatusCooldown);
        if (museumStatusCue != null) tag.putString("MuseumStatusCue", museumStatusCue.name());
    }

    private void loadMuseumDialogueData(CompoundTag tag) {
        museumDiscoveredSpecies.clear();
        ListTag discovered = tag.getList("MuseumDiscoveredSpecies", Tag.TAG_STRING);
        for (int i = 0; i < discovered.size(); i++) museumDiscoveredSpecies.add(discovered.getString(i));

        museumFactSpecies.clear();
        ListTag facts = tag.getList("MuseumFactSpecies", Tag.TAG_STRING);
        for (int i = 0; i < facts.size(); i++) museumFactSpecies.add(facts.getString(i));

        museumFactCounts.clear();
        CompoundTag factCounts = tag.getCompound("MuseumFactCounts");
        for (String species : factCounts.getAllKeys()) museumFactCounts.put(species, factCounts.getInt(species));

        museumDialogueQueue.clear();
        ListTag queuedDialogue = tag.getList("MuseumDialogueQueue", Tag.TAG_COMPOUND);
        for (int i = 0; i < queuedDialogue.size(); i++) {
            CompoundTag entry = queuedDialogue.getCompound(i);
            try {
                museumDialogueQueue.add(new MuseumDialogue.Cue(
                    MuseumDialogue.CueType.valueOf(entry.getString("Type")), entry.getString("Species")));
            } catch (IllegalArgumentException exception) {
                System.getLogger(getClass().getName()).log(
                    System.Logger.Level.WARNING, "Ignoring invalid saved Museum Mode dialogue cue", exception);
            }
        }
        museumSpeechCooldown = tag.getInt("MuseumSpeechCooldown");
        museumStatusCooldown = tag.getInt("MuseumStatusCooldown");
        museumStatusCue = null;
        if (tag.contains("MuseumStatusCue")) {
            try {
                museumStatusCue = MuseumDialogue.CueType.valueOf(tag.getString("MuseumStatusCue"));
            } catch (IllegalArgumentException exception) {
                System.getLogger(getClass().getName()).log(
                    System.Logger.Level.WARNING, "Ignoring invalid saved Museum Mode status", exception);
            }
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
            state.setAndContinue(isOrderedToSit() ? SITTING_ANIMATION
                : state.isMoving() ? MOVING_ANIMATION : IDLE_ANIMATION)));
        controllers.add(new AnimationController<>(this, "flight", 0, state ->
            !isOrderedToSit() && state.isMoving()
                ? state.setAndContinue(FLYING_ANIMATION) : PlayState.STOP));
        controllers.add(new AnimationController<>(this, "mining", 0, state ->
            isMining() ? state.setAndContinue(MINING_ANIMATION) : PlayState.STOP));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
            .triggerableAnim("dance", DANCE_ANIMATION));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob mate) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }

    private final class HatContainer extends SimpleContainer {
        private HatContainer() {
            super(1);
        }

        @Override public boolean isEmpty() {
            return getItem(0).isEmpty();
        }

        @Override public ItemStack getItem(int slot) {
            return slot == 0 ? getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && stack.getItem() instanceof OwlHatItem;
        }

        @Override public void setItem(int slot, ItemStack stack) {
            if (slot != 0 || (!stack.isEmpty() && !canPlaceItem(slot, stack))) return;
            setItemSlot(EquipmentSlot.HEAD, stack.copy());
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
            ItemStack current = getItem(slot);
            if (current.isEmpty()) return ItemStack.EMPTY;

            ItemStack removed = current.copyWithCount(Math.min(amount, current.getCount()));
            ItemStack remaining = current.copy();
            remaining.shrink(removed.getCount());
            setItem(slot, remaining);
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            if (slot != 0) return ItemStack.EMPTY;
            ItemStack removed = getItem(slot).copy();
            setItem(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override public void clearContent() {
            setItem(0, ItemStack.EMPTY);
        }
    }

    private final class CargoContainer extends SimpleContainer {
        private boolean loading;

        private CargoContainer(int size) {
            super(size);
        }

        private void setLoading(boolean loading) {
            this.loading = loading;
        }

        @Override public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (!loading && !level().isClientSide && isMuseumMode()
                && !stack.isEmpty() && stack.is(OwlItems.FIELD_NOTES.get())
                && getOwner() instanceof Player owner) {
                stopMuseumMode(owner);
                reactHappy();
            }
        }
    }

    private final class LightContainer extends SimpleContainer {
        private LightContainer() {
            super(1);
        }

        @Override public boolean isEmpty() {
            return lightItem.isEmpty();
        }

        @Override public ItemStack getItem(int slot) {
            return slot == 0 ? lightItem : ItemStack.EMPTY;
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && isLightItem(stack);
        }

        @Override public void setItem(int slot, ItemStack stack) {
            if (slot != 0 || (!stack.isEmpty() && !canPlaceItem(slot, stack))) return;
            setLightItem(stack);
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
            ItemStack current = getItem(slot);
            if (current.isEmpty()) return ItemStack.EMPTY;

            ItemStack removed = current.copyWithCount(Math.min(amount, current.getCount()));
            ItemStack remaining = current.copy();
            remaining.shrink(removed.getCount());
            setItem(slot, remaining);
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            if (slot != 0) return ItemStack.EMPTY;
            ItemStack removed = getItem(slot).copy();
            setItem(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override public void clearContent() {
            setItem(0, ItemStack.EMPTY);
        }
    }

    private final class PickaxeContainer extends SimpleContainer {
        private boolean updatingFromEntity;

        private PickaxeContainer() {
            super(1);
        }

        private void setFromEntity(ItemStack stack) {
            updatingFromEntity = true;
            super.setItem(0, stack.copy());
            updatingFromEntity = false;
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && isPickaxe(stack);
        }

        @Override public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (slot == 0 && !updatingFromEntity) setPickaxe(super.getItem(0));
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = super.removeItem(slot, amount);
            if (slot == 0) setPickaxe(super.getItem(0));
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = super.removeItemNoUpdate(slot);
            if (slot == 0) setPickaxe(super.getItem(0));
            return removed;
        }

        @Override public void clearContent() {
            super.clearContent();
            setPickaxe(ItemStack.EMPTY);
        }
    }

    private final class ShovelContainer extends SimpleContainer {
        private boolean updatingFromEntity;

        private ShovelContainer() {
            super(1);
        }

        private void setFromEntity(ItemStack stack) {
            updatingFromEntity = true;
            super.setItem(0, stack.copy());
            updatingFromEntity = false;
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && isShovel(stack);
        }

        @Override public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (slot == 0 && !updatingFromEntity) setShovel(super.getItem(0));
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = super.removeItem(slot, amount);
            if (slot == 0) setShovel(super.getItem(0));
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = super.removeItemNoUpdate(slot);
            if (slot == 0) setShovel(super.getItem(0));
            return removed;
        }

        @Override public void clearContent() {
            super.clearContent();
            setShovel(ItemStack.EMPTY);
        }
    }
}
