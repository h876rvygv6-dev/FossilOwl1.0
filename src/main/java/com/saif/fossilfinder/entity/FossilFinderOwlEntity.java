package com.saif.fossilfinder.entity;

import com.saif.fossilfinder.menu.OwlInventoryMenu;
import com.saif.fossilfinder.item.OwlHatItem;
import com.saif.fossilfinder.museum.MuseumBoneClassifier;
import com.saif.fossilfinder.registry.OwlItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private static final RawAnimation DANCE_ANIMATION =
        RawAnimation.begin().thenPlayXTimes("animation.fossil_finder_owl.dance", 2);
    private static final RawAnimation MINING_ANIMATION =
        RawAnimation.begin().thenPlay("animation.fossil_finder_owl.mining");
    private static final RawAnimation SHOVELING_ANIMATION =
        RawAnimation.begin().thenPlay("animation.fossil_finder_owl.shoveling");

    private static final EntityDataAccessor<ItemStack> DATA_TOOL = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_LIGHT_ITEM = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_FIELD_WORK_MODE = SynchedEntityData.defineId(FossilFinderOwlEntity.class, EntityDataSerializers.BOOLEAN);
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
    private static final String[] MUSEUM_DIALOGUE = {
        "dialogue.fossil_finder_owl.museum.1",
        "dialogue.fossil_finder_owl.museum.2",
        "dialogue.fossil_finder_owl.museum.3",
        "dialogue.fossil_finder_owl.museum.4"
    };
    private static final String[] FIELDWORK_DIALOGUE = {
        "dialogue.fossil_finder_owl.fieldwork.1",
        "dialogue.fossil_finder_owl.fieldwork.2",
        "dialogue.fossil_finder_owl.fieldwork.3",
        "dialogue.fossil_finder_owl.fieldwork.4"
    };
    private static final String[] TAMING_DIALOGUE = {
        "dialogue.fossil_finder_owl.tamed.1",
        "dialogue.fossil_finder_owl.tamed.2"
    };
    private static final String[] FEEDING_DIALOGUE = {
        "dialogue.fossil_finder_owl.feeding.1",
        "dialogue.fossil_finder_owl.feeding.2"
    };
    private static final String[] MODE_DIALOGUE = {
        "dialogue.fossil_finder_owl.mode.1",
        "dialogue.fossil_finder_owl.mode.2"
    };
    private static final String[] MINING_DIALOGUE = {
        "dialogue.fossil_finder_owl.mining.1",
        "dialogue.fossil_finder_owl.mining.2",
        "dialogue.fossil_finder_owl.mining.3"
    };
    private static final String[] HAT_DIALOGUE = {
        "dialogue.fossil_finder_owl.hat.1",
        "dialogue.fossil_finder_owl.hat.2"
    };
    private static final String[] FOLLOW_DIALOGUE = {
        "dialogue.fossil_finder_owl.follow.1",
        "dialogue.fossil_finder_owl.follow.2"
    };
    private static final String[] STAY_DIALOGUE = {
        "dialogue.fossil_finder_owl.stay.1",
        "dialogue.fossil_finder_owl.stay.2"
    };
    private static final ResourceLocation ADVANCEMENT_EVENT = new ResourceLocation("fossil_finder_owl", "event");
    private static final ResourceLocation HOOT_ID = new ResourceLocation("fossil_finder_owl", "fossil_owl_hoot");
    private static final int CARGO_SIZE = 15;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lastFieldNotesCount;
    private int lastMuseumGuideCount;
    private boolean suppressCargoModeChanges;
    private final CargoContainer cargo = new CargoContainer();
    private NonNullList<ItemStack> pendingCargo = NonNullList.withSize(12, ItemStack.EMPTY);
    private final HatContainer hatContainer = new HatContainer();
    private final ToolContainer toolContainer = new ToolContainer();
    private final LightContainer lightContainer = new LightContainer();
    private ItemStack excavationTool = ItemStack.EMPTY;
    private ItemStack lightItem = ItemStack.EMPTY;
    private BlockPos target;
    private int scanCooldown;
    private int digTicks;
    private int alarmCooldown;
    private int nextDialogueTick;
    private int museumScanCooldown;
    private int museumStatusCooldown;
    private String museumStatusKey;

    public FossilFinderOwlEntity(EntityType<FossilFinderOwlEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 18, true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_TOOL, ItemStack.EMPTY);
        entityData.define(DATA_LIGHT_ITEM, ItemStack.EMPTY);
        entityData.define(DATA_FIELD_WORK_MODE, false);
    }

    private void setTool(ItemStack stack) {
        excavationTool = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        entityData.set(DATA_TOOL, excavationTool.copy());
        toolContainer.setFromEntity(excavationTool);
    }

    private void setLightItem(ItemStack stack) {
        lightItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        entityData.set(DATA_LIGHT_ITEM, lightItem.copy());
        lightContainer.setFromEntity(lightItem);
    }

    public ItemStack getToolForRender() { return entityData.get(DATA_TOOL); }
    public ItemStack getLightForRender() { return entityData.get(DATA_LIGHT_ITEM); }
    public boolean isFieldWorkMode() { return entityData.get(DATA_FIELD_WORK_MODE); }

    private void setFieldWorkMode(boolean fieldWorkMode) {
        if (isFieldWorkMode() != fieldWorkMode) entityData.set(DATA_FIELD_WORK_MODE, fieldWorkMode);
        if (!fieldWorkMode) {
            target = null;
            digTicks = 0;
            scanCooldown = 0;
            getNavigation().stop();
        }
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
        if (!isTame() || getOwner() == null) {
            target = null;
            setNoGravity(false);
            return;
        }
        if (getOwner() instanceof Player owner) tickAmbientDialogue(owner);
        if (isOrderedToSit()) {
            target = null;
            digTicks = 0;
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
        if (isFieldWorkMode()) collectNearbyDrops();
        if (getOwner() instanceof Player owner && distanceToSqr(owner) > 64.0) {
            target = null;
            digTicks = 0;
            if (distanceToSqr(owner) > 400.0) {
                teleportTo(owner.getX() + 1.0, owner.getY() + 1.0, owner.getZ() + 1.0);
                getNavigation().stop();
            } else if (getNavigation().isDone() || tickCount % 10 == 0) {
                getNavigation().moveTo(owner, 1.0);
            }
            return;
        }
        if (!isFieldWorkMode()) {
            if (getOwner() instanceof Player owner) tickMuseumMode(owner);
            return;
        }
        if (isFieldWorkMode() && cargoFull()) {
            target = null;
            returnToOwner();
            return;
        }
        if (isFieldWorkMode() && !excavationTool.isEmpty()) {
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
            if (getOwner() instanceof Player owner) say(owner,
                "dialogue.fossil_finder_owl.alert.1", "dialogue.fossil_finder_owl.alert.2");
            SoundEvent mimic = threat instanceof Zombie ? SoundEvents.PARROT_IMITATE_ZOMBIE
                : threat instanceof Skeleton ? SoundEvents.PARROT_IMITATE_SKELETON
                : threat instanceof Creeper ? SoundEvents.PARROT_IMITATE_CREEPER
                : threat instanceof Spider ? SoundEvents.PARROT_IMITATE_SPIDER : null;
            if (mimic != null) playSound(mimic, 0.8F, 1.1F);
            alarmCooldown = 65;
        }
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
        if (!level().hasChunkAt(target)) {
            target = null;
            return;
        }
        BlockState state = level().getBlockState(target);
        ItemStack tool = toolForBlock(state);
        if (tool.isEmpty()) {
            target = null;
            return;
        }
        double distance = distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distance > 5.0) {
            if (getNavigation().isDone() || tickCount % 20 == 0)
                getNavigation().moveTo(target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, 1.05);
            return;
        }
        getNavigation().stop();
        if (digTicks % 14 == 0) {
            triggerAnim("action", isShovel(tool) ? "shovel" : "mine");
            playSound(state.getSoundType().getHitSound(), 0.65F, 0.9F + random.nextFloat() * 0.2F);
        }
        if (++digTicks >= 55) {
            excavateTarget();
            digTicks = 0;
            target = null;
        }
    }

    private void tickMuseumMode(Player owner) {
        if (museumScanCooldown > 0) {
            museumScanCooldown--;
            return;
        }
        museumScanCooldown = 20;

        List<SignBlockEntity> signs = findNearbySigns();
        if (signs.isEmpty()) {
            reportMuseumStatus(owner, "dialogue.fossil_finder_owl.museum.needs_sign");
            returnToOwner();
            return;
        }

        List<MuseumExhibit> exhibits = new ArrayList<>();
        boolean hasFrameGrid = false;
        for (SignBlockEntity sign : signs) {
            List<ItemFrame> frames = findMuseumGrid(sign);
            if (frames.isEmpty()) continue;
            hasFrameGrid = true;
            String species = speciesOnSign(sign);
            if (species == null && signHasText(sign)) continue;
            exhibits.add(new MuseumExhibit(sign, frames, species));
        }
        if (!hasFrameGrid) {
            reportMuseumStatus(owner, "dialogue.fossil_finder_owl.museum.needs_frames");
            returnToOwner();
            return;
        }
        if (exhibits.isEmpty()) {
            reportMuseumStatus(owner, "dialogue.fossil_finder_owl.museum.needs_species_label");
            returnToOwner();
            return;
        }
        exhibits.sort(Comparator.comparingDouble(exhibit -> {
            BlockPos signPos = exhibit.sign().getBlockPos();
            return distanceToSqr(signPos.getX() + 0.5, signPos.getY() + 0.5, signPos.getZ() + 0.5);
        }));

        MuseumPlacement placement = findMuseumPlacement(exhibits);
        if (placement == null) {
            reportMuseumStatus(owner, "dialogue.fossil_finder_owl.museum.needs_specimens");
            returnToOwner();
            return;
        }
        museumStatusKey = null;
        museumStatusCooldown = 0;

        int placedCount = countDisplayedBones(placement.exhibit().frames(), placement.species());
        int expectedCount = MuseumBoneClassifier.expectedBoneParts(placement.species()).size();
        updateMuseumSign(placement.exhibit().sign(), placement.species(), placedCount, expectedCount, null);

        ItemFrame frame = placement.frame();
        if (distanceToSqr(frame) > 4.0) {
            if (getNavigation().isDone() || tickCount % 10 == 0) getNavigation().moveTo(frame, 0.9);
            return;
        }
        getNavigation().stop();
        if (!frame.isAlive() || !frame.getItem().isEmpty()) return;

        ItemStack specimen = cargo.getItem(placement.cargoSlot()).copyWithCount(1);
        frame.setItem(specimen, true);
        if (!ItemStack.isSameItemSameTags(frame.getItem(), specimen)) return;
        cargo.removeItem(placement.cargoSlot(), 1);
        placedCount = countDisplayedBones(placement.exhibit().frames(), placement.species());
        boolean complete = placedCount >= expectedCount;
        List<String> facts = complete ? loadMuseumFacts(placement.species()) : List.of();
        updateMuseumSign(placement.exhibit().sign(), placement.species(), placedCount, expectedCount, facts);
        reactHappy();
        playHoot(false);

        if (complete) {
            awardAdvancement(owner, "opening_day");
            String details = facts.stream().limit(2).reduce((first, next) -> first + " " + next)
                .orElse("Every specimen tells a piece of this ancient story.");
            sayLine(owner, "Our " + MuseumBoneClassifier.displayName(placement.species())
                + " exhibit is complete! Did you know? " + details);
        } else {
            sayLine(owner, "One more " + MuseumBoneClassifier.displayName(placement.species())
                + " specimen displayed. " + placedCount + " of " + expectedCount + " pieces are in place.");
        }
    }

    private List<SignBlockEntity> findNearbySigns() {
        List<SignBlockEntity> signs = new ArrayList<>();
        BlockPos origin = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
            origin.offset(-10, -5, -10), origin.offset(10, 5, 10))) {
            if (level().getBlockEntity(pos) instanceof SignBlockEntity sign) signs.add(sign);
        }
        return signs;
    }

    private List<ItemFrame> findMuseumGrid(SignBlockEntity sign) {
        BlockPos signPos = sign.getBlockPos();
        AABB bounds = new AABB(signPos).inflate(8.0, 4.0, 8.0);
        List<ItemFrame> nearbyFrames = level().getEntitiesOfClass(ItemFrame.class, bounds,
            frame -> frame.isAlive() && !frame.isInvisible()
                && frame.distanceToSqr(signPos.getX() + 0.5, signPos.getY() + 0.5, signPos.getZ() + 0.5) <= 100.0);
        List<ItemFrame> bestGrid = List.of();
        double bestDistance = Double.MAX_VALUE;

        for (ItemFrame seed : nearbyFrames) {
            Direction facing = seed.getDirection();
            if (facing.getAxis() != Direction.Axis.X && facing.getAxis() != Direction.Axis.Z) continue;
            BlockPos seedPos = seed.blockPosition();
            int seedColumn = horizontalCoordinate(seedPos, facing);
            for (int columnOffset = 0; columnOffset < 4; columnOffset++) {
                for (int rowOffset = 0; rowOffset < 2; rowOffset++) {
                    int firstColumn = seedColumn - columnOffset;
                    int topY = seedPos.getY() + rowOffset;
                    List<ItemFrame> cells = new ArrayList<>(8);
                    boolean completeGrid = true;
                    for (int row = 0; row < 2 && completeGrid; row++) {
                        for (int column = 0; column < 4; column++) {
                            BlockPos cellPos = gridCell(seedPos, facing, firstColumn + column, topY - row);
                            ItemFrame cell = nearbyFrames.stream()
                                .filter(candidate -> candidate.getDirection() == facing
                                    && candidate.blockPosition().equals(cellPos))
                                .findFirst().orElse(null);
                            if (cell == null) {
                                completeGrid = false;
                                break;
                            }
                            cells.add(cell);
                        }
                    }
                    if (!completeGrid) continue;
                    double gridDistance = cells.stream().mapToDouble(frame ->
                        frame.distanceToSqr(signPos.getX() + 0.5, signPos.getY() + 0.5, signPos.getZ() + 0.5))
                        .average().orElse(Double.MAX_VALUE);
                    if (gridDistance < bestDistance) {
                        bestDistance = gridDistance;
                        bestGrid = cells;
                    }
                }
            }
        }
        return bestGrid;
    }

    private static int horizontalCoordinate(BlockPos pos, Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? pos.getX() : pos.getZ();
    }

    private static BlockPos gridCell(BlockPos reference, Direction facing, int horizontal, int y) {
        return facing.getAxis() == Direction.Axis.Z
            ? new BlockPos(horizontal, y, reference.getZ())
            : new BlockPos(reference.getX(), y, horizontal);
    }

    private String speciesOnSign(SignBlockEntity sign) {
        for (int line = 0; line < 4; line++) {
            Optional<String> species = MuseumBoneClassifier.speciesFromLabel(
                sign.getFrontText().getMessage(line, false).getString());
            if (species.isPresent()) return species.get();
        }
        return null;
    }

    private boolean signHasText(SignBlockEntity sign) {
        for (int line = 0; line < 4; line++) {
            if (!sign.getFrontText().getMessage(line, false).getString().isBlank()) return true;
        }
        return false;
    }

    private MuseumPlacement findMuseumPlacement(List<MuseumExhibit> exhibits) {
        for (MuseumExhibit exhibit : exhibits) {
            String species = exhibit.species();
            if (species == null) {
                for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
                    Optional<MuseumBone> bone = museumBone(cargo.getItem(slot));
                    if (bone.isPresent()) {
                        species = bone.get().species();
                        break;
                    }
                }
            }
            if (species == null) continue;

            List<String> expectedParts = MuseumBoneClassifier.expectedBoneParts(species);
            for (int index = 0; index < expectedParts.size(); index++) {
                String part = expectedParts.get(index);
                ItemFrame frame = exhibit.frames().get(MuseumBoneClassifier.BONE_PARTS.indexOf(part));
                Optional<MuseumBone> displayed = museumBone(frame.getItem());
                if (displayed.isPresent() && displayed.get().matches(species, part)) continue;
                if (!frame.getItem().isEmpty()) continue;
                int cargoSlot = findCargoBone(species, part);
                if (cargoSlot >= 0) return new MuseumPlacement(exhibit, species, part, frame, cargoSlot);
            }
        }
        return null;
    }

    private int findCargoBone(String species, String part) {
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            Optional<MuseumBone> bone = museumBone(cargo.getItem(slot));
            if (bone.isPresent() && bone.get().matches(species, part)) return slot;
        }
        return -1;
    }

    private Optional<MuseumBone> museumBone(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return Optional.empty();
        Optional<String> species = MuseumBoneClassifier.speciesFor(id.getNamespace(), id.getPath());
        Optional<String> part = MuseumBoneClassifier.bonePartFor(id.getPath());
        return species.isPresent() && part.isPresent()
            ? Optional.of(new MuseumBone(species.get(), part.get())) : Optional.empty();
    }

    private int countDisplayedBones(List<ItemFrame> frames, String species) {
        int count = 0;
        List<String> expectedParts = MuseumBoneClassifier.expectedBoneParts(species);
        for (String part : expectedParts) {
            int frameIndex = MuseumBoneClassifier.BONE_PARTS.indexOf(part);
            Optional<MuseumBone> bone = museumBone(frames.get(frameIndex).getItem());
            if (bone.isPresent() && bone.get().matches(species, part)) count++;
        }
        return count;
    }

    private void updateMuseumSign(SignBlockEntity sign, String species, int placed, int expected, List<String> facts) {
        String[] lines = new String[4];
        List<String> title = wrapForSign(MuseumBoneClassifier.displayName(species), 15, 2);
        for (int i = 0; i < title.size(); i++) lines[i] = title.get(i);

        if (facts != null) {
            int line = title.size();
            if (line < lines.length) lines[line++] = "Complete!";
            String factText = facts.stream().limit(2).reduce((first, next) -> first + " " + next).orElse("");
            List<String> wrappedFacts = wrapForSign(factText, 15, lines.length - line);
            for (String wrapped : wrappedFacts) lines[line++] = wrapped;
        } else {
            int line = title.size();
            if (line < lines.length) lines[line++] = "Bones " + placed + "/" + expected;
            if (line < lines.length) lines[line] = "Museum exhibit";
        }

        SignText text = sign.getFrontText();
        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i];
            if (!text.getMessage(i, false).getString().equals(line)) {
                text = text.setMessage(i, Component.literal(line));
                changed = true;
            }
        }
        if (!changed) return;
        SignText updatedText = text;
        sign.updateText(current -> updatedText, true);
        sign.setChanged();
        BlockState state = level().getBlockState(sign.getBlockPos());
        level().sendBlockUpdated(sign.getBlockPos(), state, state, 3);
    }

    private static List<String> wrapForSign(String text, int maxCharacters, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (maxLines <= 0 || text.isBlank()) return lines;
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (word.length() > maxCharacters) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                    if (lines.size() >= maxLines) return lines;
                }
                lines.add(word.substring(0, maxCharacters));
                word = word.substring(maxCharacters);
                if (lines.size() >= maxLines) return lines;
            }
            if (current.length() > 0 && current.length() + 1 + word.length() > maxCharacters) {
                lines.add(current.toString());
                current.setLength(0);
                if (lines.size() >= maxLines) return lines;
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0 && lines.size() < maxLines) lines.add(current.toString());
        return lines;
    }

    private List<String> loadMuseumFacts(String species) {
        ResourceLocation location = new ResourceLocation(
            "fossil_finder_owl", "dinopedia/" + species + ".txt");
        Optional<net.minecraft.server.packs.resources.Resource> resource =
            ((ServerLevel) level()).getServer().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            System.getLogger(getClass().getName()).log(
                System.Logger.Level.WARNING, "No Dinopedia entry found for " + species);
            return List.of();
        }
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            String text = String.join(" ", reader.lines().toList());
            return java.util.Arrays.stream(text.trim().split("(?<=[.!?])\\s+"))
                .map(String::trim).filter(sentence -> !sentence.isEmpty()).toList();
        } catch (IOException exception) {
            System.getLogger(getClass().getName()).log(
                System.Logger.Level.WARNING, "Could not read Dinopedia entry for " + species, exception);
            return List.of();
        }
    }

    private void reportMuseumStatus(Player owner, String dialogueKey) {
        if (!dialogueKey.equals(museumStatusKey)) {
            museumStatusKey = dialogueKey;
            museumStatusCooldown = 0;
        }
        if (museumStatusCooldown > 0) return;
        say(owner, dialogueKey);
        museumStatusCooldown = 1200;
    }

    private void returnToOwner() {
        if (getOwner() instanceof Player owner) {
            if (distanceToSqr(owner) > 16.0) getNavigation().moveTo(owner, 1.0);
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
            || (id.getNamespace().equals("fossil") && (id.getPath().contains("fossil")
                || id.getPath().contains("amber") || state.is(BlockTags.MINEABLE_WITH_SHOVEL)));
        return fossil && !toolForBlock(state).isEmpty();
    }

    private ItemStack toolForBlock(BlockState state) {
        if (isShovel(excavationTool) && state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return excavationTool;
        if (isPickaxe(excavationTool) && excavationTool.isCorrectToolForDrops(state)) return excavationTool;
        return ItemStack.EMPTY;
    }

    private void excavateTarget() {
        if (target == null || excavationTool.isEmpty() || !level().hasChunkAt(target)
            || !(level() instanceof ServerLevel serverLevel)) return;
        BlockState state = level().getBlockState(target);
        ItemStack tool = toolForBlock(state);
        if (tool.isEmpty()) return;
        BlockPos minedPos = target;
        var breaker = getOwner();
        if (breaker == null) breaker = this;
        for (ItemStack drop : Block.getDrops(state, serverLevel, minedPos, level().getBlockEntity(minedPos), breaker, tool)) {
            ItemStack remainder = addCargo(drop);
            if (!remainder.isEmpty()) level().addFreshEntity(new ItemEntity(level(), minedPos.getX() + 0.5, minedPos.getY() + 0.5, minedPos.getZ() + 0.5, remainder));
        }
        playSound(state.getSoundType().getBreakSound(), 0.8F, 0.9F + random.nextFloat() * 0.2F);
        level().removeBlock(minedPos, false);
        reactHappy();
        Player owner = getOwner() instanceof Player playerOwner ? playerOwner : null;
        boolean firstFossil = owner != null && awardAdvancement(owner, "digging_up_the_past");
        if (firstFossil || random.nextInt(12) == 0) {
            if (owner != null) say(owner, MINING_DIALOGUE);
        }
        tool.hurtAndBreak(1, this, entity -> setTool(ItemStack.EMPTY));
        setTool(tool);
        playHoot(false);
        if (cargoFull() || excavationTool.isEmpty()) returnToOwner();
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

    private void openCargo(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, playerInventory, viewer) ->
                new OwlInventoryMenu(id, playerInventory, cargo, hatContainer, toolContainer, lightContainer, getId()),
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
            nextDialogueTick = tickCount + 1200 + random.nextInt(1800);
            reactHappy();
            awardAdvancement(player, "a_friend_with_feathers");
            say(player, TAMING_DIALOGUE);
            player.displayClientMessage(Component.literal("The owl accepts your fossil and joins your expedition!"), true);
            return InteractionResult.CONSUME;
        }
        if (isTame() && isOwnedBy(player)) {
            if (!isFieldWorkMode() && museumBone(held).isPresent()) {
                if (!addCargo(held.copyWithCount(1)).isEmpty()) {
                    say(player, "dialogue.fossil_finder_owl.museum.cargo_full");
                    return InteractionResult.CONSUME;
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
                reactHappy();
                say(player, "dialogue.fossil_finder_owl.museum.bone_received");
                return InteractionResult.CONSUME;
            }
            if (held.is(OwlItems.FIELD_NOTES.get())) {
                selectMode(player, true);
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("Your owl heads out for field work."), true);
                return InteractionResult.CONSUME;
            }
            if (held.is(OwlItems.MUSEUM_GUIDE.get())) {
                selectMode(player, false);
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("Your owl returns to museum mode."), true);
                return InteractionResult.CONSUME;
            }
            if (isHealingFood(held) && getHealth() < getMaxHealth()) {
                heal(4.0F);
                if (!player.getAbilities().instabuild) held.shrink(1);
                reactHappy();
                awardAdvancement(player, "a_snack_for_thought");
                say(player, FEEDING_DIALOGUE);
                player.displayClientMessage(Component.literal("Your owl looks healthier."), true);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.BONE)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                setOrderedToSit(!isOrderedToSit());
                target = null;
                digTicks = 0;
                getNavigation().stop();
                player.displayClientMessage(Component.literal(isOrderedToSit() ? "Your owl will perch here." : "Your owl will follow your expedition."), true);
                say(player, isOrderedToSit() ? STAY_DIALOGUE : FOLLOW_DIALOGUE);
                return InteractionResult.CONSUME;
            }
            if (isRejectedExcavationTool(held)) {
                player.displayClientMessage(Component.literal("That tool is too flimsy for fossil work. I need a sturdier pickaxe or shovel!"), true);
                say(player, "dialogue.fossil_finder_owl.tool_rejected");
                return InteractionResult.CONSUME;
            }
            if (isExcavationTool(held)) {
                setTool(held.copyWithCount(1));
                if (!player.getAbilities().instabuild) held.shrink(1);
                target = null;
                reactHappy();
                say(player, "dialogue.fossil_finder_owl.tool");
                player.displayClientMessage(Component.literal("Your owl is ready for its fossil expedition!"), true);
                return InteractionResult.CONSUME;
            }
            if (isLightItem(held)) {
                setLightItem(held.copyWithCount(1));
                if (!player.getAbilities().instabuild) held.shrink(1);
                reactHappy();
                say(player, "dialogue.fossil_finder_owl.light");
                player.displayClientMessage(Component.literal("Your owl takes the light."), true);
                return InteractionResult.CONSUME;
            }
            if (held.isEmpty()) {
                openCargo(player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    private boolean isRejectedExcavationTool(ItemStack stack) {
        return stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) || stack.is(Items.GOLDEN_PICKAXE)
            || stack.is(Items.WOODEN_SHOVEL) || stack.is(Items.STONE_SHOVEL) || stack.is(Items.GOLDEN_SHOVEL);
    }

    public static boolean isPickaxe(ItemStack stack) {
        return stack.is(Items.IRON_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
    }
    public static boolean isShovel(ItemStack stack) {
        return stack.is(Items.IRON_SHOVEL) || stack.is(Items.DIAMOND_SHOVEL) || stack.is(Items.NETHERITE_SHOVEL);
    }
    public static boolean isExcavationTool(ItemStack stack) {
        return isPickaxe(stack) || isShovel(stack);
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

    private void tickAmbientDialogue(Player owner) {
        if (tickCount < nextDialogueTick) return;
        if (distanceToSqr(owner) <= 144.0) {
            say(owner, isFieldWorkMode() ? FIELDWORK_DIALOGUE : MUSEUM_DIALOGUE);
            nextDialogueTick = tickCount + 2400 + random.nextInt(2401);
        } else {
            nextDialogueTick = tickCount + 200;
        }
    }

    private void selectMode(Player player, boolean fieldWorkMode) {
        setFieldWorkMode(fieldWorkMode);
        reactHappy();
        awardAdvancement(player, fieldWorkMode ? "field_trip" : "the_little_curator");
        say(player, MODE_DIALOGUE);
    }

    private void say(Player player, String... dialogueKeys) {
        if (dialogueKeys.length == 0) return;
        sayLine(player, Component.translatable(dialogueKeys[random.nextInt(dialogueKeys.length)]));
    }

    private void sayLine(Player player, String line) {
        sayLine(player, Component.literal(line));
    }

    private void sayLine(Player player, Component line) {
        player.displayClientMessage(
            Component.empty().append(getName()).append(Component.literal(": ")).append(line), false);
    }

    private void reactHappy() {
        triggerAnim("action", "dance");
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                getX(), getY() + getBbHeight() * 0.75, getZ(),
                7, 0.35, 0.35, 0.35, 0.02);
        }
    }

    private boolean awardAdvancement(Player player, String advancementId) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level() instanceof ServerLevel serverLevel)) return false;
        ResourceLocation id = new ResourceLocation("fossil_finder_owl", advancementId);
        Advancement advancement = serverLevel.getServer().getAdvancements().getAdvancement(id);
        if (advancement == null) {
            System.getLogger(getClass().getName()).log(
                System.Logger.Level.WARNING, "Missing advancement " + id);
            return false;
        }
        return serverPlayer.getAdvancements().award(advancement, ADVANCEMENT_EVENT.getPath());
    }

    public void celebrateHatGift(Player player) {
        reactHappy();
        awardAdvancement(player, "a_dapper_companion");
        say(player, HAT_DIALOGUE);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        NonNullList<ItemStack> cargoItems = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < cargo.getContainerSize(); i++) cargoItems.set(i, cargo.getItem(i).copy());
        tag.put("Cargo", ContainerHelper.saveAllItems(new CompoundTag(), cargoItems));
        tag.put("PendingCargo", ContainerHelper.saveAllItems(new CompoundTag(), pendingCargo));
        if (!excavationTool.isEmpty()) tag.put("Tool", excavationTool.save(new CompoundTag()));
        if (!lightItem.isEmpty()) tag.put("LightItem", lightItem.save(new CompoundTag()));
        if (target != null) tag.putLong("Target", target.asLong());
        tag.putBoolean("FieldWorkMode", isFieldWorkMode());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        suppressCargoModeChanges = true;
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
        if (tag.contains("Cargo")) {
            NonNullList<ItemStack> cargoItems = NonNullList.withSize(27, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Cargo"), cargoItems);
            for (int i = 0; i < cargo.getContainerSize(); i++) cargo.setItem(i, cargoItems.get(i));
            for (int i = cargo.getContainerSize(); i < cargoItems.size(); i++) addPendingCargo(cargoItems.get(i));
        }
        cargo.setChanged();
        ItemStack storedTool = tag.contains("Tool") ? ItemStack.of(tag.getCompound("Tool"))
            : tag.contains("Pickaxe") ? ItemStack.of(tag.getCompound("Pickaxe")) : ItemStack.EMPTY;
        setTool(storedTool);
        setLightItem(tag.contains("LightItem") ? ItemStack.of(tag.getCompound("LightItem")) : ItemStack.EMPTY);
        if (tag.contains("Target")) target = BlockPos.of(tag.getLong("Target"));
        suppressCargoModeChanges = false;
        setFieldWorkMode(tag.getBoolean("FieldWorkMode"));
        nextDialogueTick = tickCount + 1200 + random.nextInt(1800);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
            state.setAndContinue(isOrderedToSit() ? SITTING_ANIMATION
                : state.isMoving() ? MOVING_ANIMATION : IDLE_ANIMATION)));
        controllers.add(new AnimationController<>(this, "flight", 0, state ->
            !isOrderedToSit() && state.isMoving()
                ? state.setAndContinue(FLYING_ANIMATION) : PlayState.STOP));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
            .triggerableAnim("dance", DANCE_ANIMATION)
            .triggerableAnim("mine", MINING_ANIMATION)
            .triggerableAnim("shovel", SHOVELING_ANIMATION));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob mate) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }

    private int countCargo(Item item) {
        int count = 0;
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private void onCargoChanged() {
        int fieldNotesCount = countCargo(OwlItems.FIELD_NOTES.get());
        int museumGuideCount = countCargo(OwlItems.MUSEUM_GUIDE.get());
        boolean fieldNotesAdded = fieldNotesCount > lastFieldNotesCount;
        boolean museumGuideAdded = museumGuideCount > lastMuseumGuideCount;
        lastFieldNotesCount = fieldNotesCount;
        lastMuseumGuideCount = museumGuideCount;

        if (suppressCargoModeChanges) return;
        if (fieldNotesAdded) {
            setFieldWorkMode(true);
            if (getOwner() instanceof Player owner) {
                reactHappy();
                awardAdvancement(owner, "field_trip");
                say(owner, MODE_DIALOGUE);
            }
        } else if (museumGuideAdded) {
            setFieldWorkMode(false);
            if (getOwner() instanceof Player owner) {
                reactHappy();
                awardAdvancement(owner, "the_little_curator");
                say(owner, MODE_DIALOGUE);
            }
        }
    }

    private final class CargoContainer extends SimpleContainer {
        private boolean initialized;

        private CargoContainer() {
            super(CARGO_SIZE);
            initialized = true;
        }

        @Override public void setChanged() {
            super.setChanged();
            if (initialized) onCargoChanged();
        }
    }

    private record MuseumBone(String species, String part) {
        private boolean matches(String expectedSpecies, String expectedPart) {
            return species.equals(expectedSpecies) && part.equals(expectedPart);
        }
    }

    private record MuseumExhibit(SignBlockEntity sign, List<ItemFrame> frames, String species) {}

    private record MuseumPlacement(
        MuseumExhibit exhibit, String species, String part, ItemFrame frame, int cargoSlot) {}

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

    private final class ToolContainer extends SimpleContainer {
        private boolean updatingFromEntity;

        private ToolContainer() {
            super(1);
        }

        private void setFromEntity(ItemStack stack) {
            updatingFromEntity = true;
            super.setItem(0, stack.copy());
            updatingFromEntity = false;
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && isExcavationTool(stack);
        }

        @Override public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (slot == 0 && !updatingFromEntity) setTool(super.getItem(0));
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = super.removeItem(slot, amount);
            if (slot == 0) setTool(super.getItem(0));
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = super.removeItemNoUpdate(slot);
            if (slot == 0) setTool(super.getItem(0));
            return removed;
        }

        @Override public void clearContent() {
            super.clearContent();
            setTool(ItemStack.EMPTY);
        }
    }

    private final class LightContainer extends SimpleContainer {
        private boolean updatingFromEntity;

        private LightContainer() {
            super(1);
        }

        private void setFromEntity(ItemStack stack) {
            updatingFromEntity = true;
            super.setItem(0, stack.copy());
            updatingFromEntity = false;
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && isLightItem(stack);
        }

        @Override public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (slot == 0 && !updatingFromEntity) setLightItem(super.getItem(0));
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = super.removeItem(slot, amount);
            if (slot == 0) setLightItem(super.getItem(0));
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = super.removeItemNoUpdate(slot);
            if (slot == 0) setLightItem(super.getItem(0));
            return removed;
        }

        @Override public void clearContent() {
            super.clearContent();
            setLightItem(ItemStack.EMPTY);
        }
    }
}
