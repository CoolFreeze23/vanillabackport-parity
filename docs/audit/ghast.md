# ghast (Chase the Skies: happy ghast, ghastling, dried ghast, harness, leash rework, controllable-mount mixins)

**Summary**
- 45 findings: 25 gameplay, 3 audio, 9 visual, 6 data, 2 cosmetic.
- I covered every VB class, mixin and data/asset folder in scope: HappyGhast, HappyGhastAi, TemptGoal, GhastlingFollowAdult, DriedGhastBlock, HarnessItem and GhastHarnessInteraction, the happy_ghast/controllable_mounts/leash_behavior mixins, the leash API (LeashPhysics, LeashableCallback, LeashFeatureRenderer, InterpolationHandler), the renderer, models and layers, the registrations, and all ghast/harness/dried-ghast JSON and PNG files. All textures match native pixel for pixel.
- I could not check `sounds.json` volume, pitch or attenuation for the ghast, harness and dried-ghast events, because 26.2 keeps sounds outside the jar. Every event ID matches.
- Several native interactions depend on 26.2-only systems: the `Equippable` component, `canBeCollidedWith(Entity)`, the `CAMERA_DISTANCE`/`TEMPT_RANGE` attributes, `EntityTracker.targetEyeHeight`, `requiredPathLength`, and the new `item.lead.*` and `item.shears.snip` sounds. Each one below has its closest 1.21.1 fix, or is marked `engine`.
- Config gates: `dried_ghasts` (fossils and bartering), `happy_ghast_speed_modifier` (default 1.0, which matches native), `modern_leashes`, `leash_drop_connections`, `has_nautilus`, `zombie_nautilus_spawn_chance`, `can_mounts_float_while_ridden`, `has_spears`.

## Coverage

| VB class / data | Native counterpart (26.2) | Result |
|---|---|---|
| common/level/entities/mob/animal/happy_ghast/HappyGhast.java | world/entity/animal/happyghast/HappyGhast.java, monster/Ghast.java (GhastMoveControl, RandomFloatAroundGoal, faceMovementDirection) | findings (01-11, 17) |
| HappyGhast.GhastMoveControl / RandomFloatAroundGoal / GhastFloatGoal / LookControl / BodyRotationControl | Ghast.GhastMoveControl, Ghast.RandomFloatAroundGoal, HappyGhast inner classes | match (every constant and branch checked) |
| HappyGhast still-timeout logic (setServerStillTimeout, tick, addPassenger/removePassenger, scanPlayerAboveGhast) | same methods | match (only difference is the 1.21.1 teleport packet vs EntityPositionSync, which is engine and behaves the same) |
| HappyGhast attributes | createAttributes | match (TEMPT_RANGE 16 is emulated through setTemptRange(16); CAMERA_DISTANCE is emulated by CameraMixin, see 18) |
| HappyGhast getRiddenInput / tickRidden / travel | same | match at the default config value |
| HappyGhastAi.java, GhastlingFollowAdult.java | HappyGhastAi.java, ai/behavior/BabyFollowAdult.java, FollowTemptation, AnimalPanic | findings (12-15) |
| ai/sensor_types/AdultSensorAnyType + tag followable_friendly_mobs | ai/sensing/AdultSensorAnyType | findings (16) |
| happy_ghast/TemptGoal.java | ai/goal/TemptGoal.java | match |
| common/level/blocks/DriedGhastBlock.java + ModBlocks DRIED_GHAST | level/block/DriedGhastBlock.java, Blocks.DRIED_GHAST | findings (21-24) |
| ModSoundTypes.DRIED_GHAST | SoundType.DRIED_GHAST | match |
| assets models/block/dried_ghast*.json | same paths | findings (25, 26) |
| blockstates/dried_ghast.json, dried ghast textures, item model | same | match |
| common/level/items/HarnessItem.java, ModItems *_HARNESS, GhastHarnessInteraction | Items harness + Equippable.harness, Mob.shearItem | findings (01-05) |
| client HappyGhastRenderer / HappyGhastModel / HappyGhastHarnessModel / RopesLayer / GhastEquipmentManager | client/renderer/entity/HappyGhastRenderer, model/animal/ghast/*, layers/RopesLayer | findings (19, 20); geometry, tentacle animation and goggles all match |
| equipment/ghast/*.json + textures/entity/ghast/harness/*.png | equipment/*_harness.json + textures/entity/equipment/happy_ghast_body/*.png | match (pixel-identical) |
| ModEntityTypes HAPPY_GHAST | EntityTypes.HAPPY_GHAST | findings (07, 10) |
| ModSoundEvents ghast / ghastling / harness / dried_ghast | SoundEvents | match (IDs) |
| core/mixin/common/happy_ghast/ServerGamePacketListenerImplMixin | isFlyingVehicle() in ServerGamePacketListenerImpl | match |
| core/mixin/common/happy_ghast/ServerEntityMixin | ServerEntity.sendChanges (requiresPrecisePosition) | match (packet types are engine) |
| core/mixin/common/happy_ghast/NetherFossilPieceMixin | NetherFossilPieces.placeDriedGhast | findings (31) |
| core/mixin/client/controllable_mount_effects/CameraMixin | Camera.setup (CAMERA_DISTANCE) | findings (18) |
| core/mixin/client/controllable_mount_effects/LocalPlayerMixin + client/level/sound/RidingEntitySoundInstance | LocalPlayer.startRiding, RidingEntitySoundInstance | match |
| core/mixin/common/controllable_mounts/CamelPanicMixin, RunAroundLikeCrazyGoalMixin | CamelAi.CamelPanic, RunAroundLikeCrazyGoal | match |
| core/mixin/common/controllable_mounts/PanicGoalMixin | PanicGoal (native has no check) | match in effect (no native mob that uses PanicGoal is mob-controlled) |
| core/mixin/common/controllable_mounts/DrownedMixin | monster/zombie/Drowned.finalizeSpawn | findings (42) |
| core/mixin/common/controllable_mounts/HuskMixin | monster/zombie/Husk.finalizeSpawn | findings (43) |
| core/mixin/common/controllable_mounts/LivingEntityMixin | LivingEntity.floatInWaterWhileRidden | findings (44) |
| core/mixin/common/controllable_mounts/ServerPlayerMixin, api/extensions/entity/mounts/* | ServerPlayer.openNautilusInventory | no native counterpart in this area (nautilus menu) |
| api/modules/leash_behavior/LeashPhysics + LeashableTickMixin + LeashDataMixin | Leashable.java | findings (35-38), physics math matches |
| api/modules/leash_behavior/LeashableCallback + EntityMixin | Leashable defaults, Entity quad-holder defaults, per-mob overrides | findings (35-37) |
| common/integrations/interactions/LeashInteraction | Entity.interact, Entity.shearOffAllLeashConnections | findings (32) |
| core/mixin/common/leash_behavior/FireworkRocketItemMixin | FireworkRocketItem.use | findings (32) |
| LeashFenceKnotEntity / LeadItem (VB does not touch them) | decoration/LeashFenceKnotEntity, item/LeadItem | findings (33, 34) |
| api/modules/leash_behavior/LeashFeatureRenderer + LeashState + core/mixin/client/leash_behavior/EntityRendererMixin | client/renderer/feature/LeashFeatureRenderer, EntityRenderer.extractRenderState | findings (39, 40) |
| api/modules/leash_behavior/InterpolationHandler + BoatMixin | world/entity/InterpolationHandler, AbstractBoat | findings (41) |
| loot_table entities/happy_ghast, blocks/dried_ghast | same | findings (29) |
| recipe *_harness, dye_*_harness, dried_ghast | same | findings (27, 28) |
| advancement recipes/* (VB transportation / building_blocks vs native combat) | same content | match (only the advancement IDs differ) |
| advancement husbandry/place_dried_ghast_in_water | same | findings (45) |
| tags item happy_ghast_food, happy_ghast_tempt_items, harnesses; block happy_ghast_avoids, triggers_ambient_dried_ghast_block_sounds | same | match (VB's harnesses tag only adds an optional extra tag) |
| LootIntegrations piglin bartering (dried ghast) | loot_table/gameplay/piglin_bartering.json | findings (30) |
| models/item/happy_ghast_spawn_egg.json | same | match (VB's SpawnEggRenderer uses the modern texture unless `use_legacy_spawn_eggs` is on) |

---

### GHAST-01  Harness has no guaranteed drop when the ghast dies
- severity: gameplay
- vb: common/integrations/interactions/GhastHarnessInteraction.java:25 (onInteract): `ghast.setItemSlot(EquipmentSlot.CHEST, new ItemStack(stack.getItem()));`. No drop chance is set, so the 1.21.1 default applies: `Arrays.fill(this.armorDropChances, 0.085F)` (src-121 Mob.java:148). In Mob.dropCustomDeathLoot the item then drops only if `(recentlyHit || flag) && this.random.nextFloat() < f`.
- native: item/equipment/Equippable.java:163-166 (equipOnTarget): `target.setItemSlot(this.slot, itemStack.split(1)); if (target instanceof Mob mob) { mob.setGuaranteedDrop(this.slot); }`
- difference: Native always drops the harness when a harnessed happy ghast dies, from any cause. VB drops it 8.5% of the time, and only when a player killed the ghast.
- fix_kind: mixin-vb
- fix: Use `@Mixin(HappyGhast.class)` (VB) and add `@Override protected float getEquipmentDropChance(EquipmentSlot slot) { return slot == EquipmentSlot.CHEST && this.isHarnessed() ? 2.0F : super.getEquipmentDropChance(slot); }`. This also fixes ghasts that were harnessed before the fix. Also add `ghast.setGuaranteedDrop(EquipmentSlot.CHEST)` after line 25 of GhastHarnessInteraction (TAIL inject or @Redirect of setItemSlot).
- config_gate: none

### GHAST-02  Equipping a harness discards the item's components
- severity: gameplay
- vb: GhastHarnessInteraction.java:25: `new ItemStack(stack.getItem())`, then `stack.shrink(1)` when not in creative.
- native: Equippable.java:163: `target.setItemSlot(this.slot, itemStack.split(1));` Creative restores the count through Player.interactOn.
- difference: VB equips a fresh, component-less harness, so its custom name, enchantments (for example Curse of Binding) and other components are lost. Native moves the actual stack.
- fix_kind: mixin-vb
- fix: Redirect `GhastHarnessInteraction.onInteract` → `ItemStack.<init>(ItemLike)` to return `stack.copyWithCount(1)`. Keep the existing creative/shrink logic.
- config_gate: none

### GHAST-03  Harness equip sound is half volume, plays when silent, and emits an extra UNEQUIP game event
- severity: audio
- vb: HappyGhast.java:267-269 (equipHarness): `this.level().playSound(null, this, ModSoundEvents.HARNESS_EQUIP.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);`. This is called from GhastHarnessInteraction.java:24, which then calls setItemSlot. Because the VB harness is not `Equipable`, 1.21.1 LivingEntity.onEquipItem (src-121 LivingEntity.java:690-691) emits `GameEvent.UNEQUIP`, and VB then adds `GameEvent.EQUIP` (line 26).
- native: LivingEntity.java:707-723 (onEquipItem): `if (!this.isSilent() && equippable != null && slot == equippable.slot()) playSeededSound(null, getX(), getY(), getZ(), getEquipSound(...), getSoundSource(), 1.0F, 1.0F, random.nextLong())` followed by `gameEvent(EQUIP)`.
- difference: The volume is 0.5 instead of 1.0, VB ignores `isSilent()`, and sculk sensors receive UNEQUIP+EQUIP instead of a single EQUIP. Native also plays the sound for every equip path (dispenser, commands); VB only plays it on right-click.
- fix_kind: mixin-vb
- fix: In HappyGhast.equipHarness, @ModifyArg the volume (index 4 of `Level.playSound(Player,Entity,SoundEvent,SoundSource,float,float)`) to 1.0F, and HEAD-cancel when `isSilent()`. To fix the game event, @Inject HEAD into `LivingEntity.onEquipItem`: when `this instanceof HappyGhast && slot == CHEST && newItem.is(HARNESSES)`, run native logic (sound at 1.0 if not silent, `gameEvent(EQUIP)`) and cancel. Then remove VB's explicit gameEvent (@Redirect `Level.gameEvent` in GhastHarnessInteraction to a no-op) and its playSound in equipHarness.
- config_gate: none

### GHAST-04  Dispensers cannot harness a happy ghast
- severity: gameplay
- vb: Nothing. HarnessItem.java is a plain `Item`, and no DispenseItemBehavior is registered for harnesses (grep finds none).
- native: HappyGhast.java:277-280 `protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) { return slot == EquipmentSlot.BODY; }`, with `Equippable.harness(...)` dispensable by default (Equippable.Builder `dispensable = true`).
- difference: Native dispensers equip a harness onto an adjacent adult, unharnessed, living happy ghast. In VB the dispenser drops the item instead.
- fix_kind: mixin-vb (registration)
- fix: For each `ModItems.*_HARNESS`, register an `OptionalDispenseItemBehavior` (DispenserBlock.registerBehavior) whose execute does the following: `BlockPos p = src.pos().relative(src.state().getValue(DispenserBlock.FACING)); List<HappyGhast> l = level.getEntitiesOfClass(HappyGhast.class, new AABB(p), g -> g.canBeHarnessed() && !g.isHarnessed());`. If one is found, `g.setItemSlot(CHEST, stack.split(1)); g.setGuaranteedDrop(CHEST); setSuccess(true);`, and return the stack. Play the equip sound through the GHAST-03 path.
- config_gate: none

### GHAST-05  Shearing off the harness: no sneak or binding check, wrong drop height, missing game event, and a creative-mode bug
- severity: gameplay
- vb: HappyGhast.java:291-307 (mobInteract): `if (!stack.is(Items.SHEARS) || this.isVehicle() || !this.isHarnessed() && !player.isCreative()) {...} else { stack.hurtAndBreak(1, player, getSlotForHand(hand)); this.playSound(ModSoundEvents.HARNESS_UNEQUIP.get()); ... this.spawnAtLocation(harness, this.getBbHeight() + 0.5F); return InteractionResult.SUCCESS; }`
- native: Entity.java:2309-2313 (interact): `this instanceof Mob target && heldItem.is(Items.SHEARS) && target.canShearEquipment(player) && !player.isSecondaryUseActive() && target.attemptToShearEquipment(...)`. Then Mob.java:580-604: `equippable.canBeSheared() && (!EnchantmentHelper.has(itemStack, PREVENT_ARMOR_CHANGE) || player.isCreative())` → `heldItem.hurtAndBreak(1, ...)`, `Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER)`, `setItemSlot(slot, EMPTY)`, `gameEvent(GameEvent.SHEAR, player)`, `spawnAtLocation(serverLevel, itemStackToShear, equipmentSpawnOffset)`, `playSound(equippable.shearingSound())`.
- difference: (a) Native ignores shears while the player sneaks; VB shears anyway. (b) Native refuses to remove a harness with Curse of Binding outside creative; VB removes it. (c) The drop spawns at the average passenger attachment (y +4.0, x/z 0) in native vs y +4.5 in VB. (d) VB emits no `GameEvent.SHEAR`. (e) With shears on an unharnessed adult in creative, VB plays HARNESS_UNEQUIP and returns SUCCESS without removing anything; native returns PASS.
- fix_kind: mixin-vb
- fix: Replace the shears branch with @Inject(HEAD, cancellable) on HappyGhast.mobInteract, placed after the `stack.interactLivingEntity` call or by reimplementing the whole method (see GHAST-06). Condition: `!isBaby() && stack.is(SHEARS) && !isVehicle() && !player.isSecondaryUseActive() && isHarnessed() && (!EnchantmentHelper.has(harness, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative())`. Body: `stack.hurtAndBreak(1, player, getSlotForHand(hand)); setItemSlot(CHEST, EMPTY); gameEvent(GameEvent.SHEAR, player); if (!level().isClientSide) spawnAtLocation(harness, 4.0F /* avg passenger attachment y */); playSound(HARNESS_UNEQUIP); return SUCCESS`. Otherwise fall through without the creative branch.
- config_gate: none

### GHAST-06  Sneak-right-click mounts a harnessed ghast instead of passing through
- severity: gameplay
- vb: HappyGhast.java:291-297: `if (this.isHarnessed()) { if (!this.level().isClientSide()) player.startRiding(this); return InteractionResult.sidedSuccess(...); }`
- native: HappyGhast.java:295-299: `if (this.isWearingBodyArmor() && !player.isSecondaryUseActive()) { this.doPlayerRide(player); return InteractionResult.SUCCESS; } else { return super.mobInteract(player, hand); }`
- difference: While sneaking, native skips mounting and falls through to Animal.mobInteract (for example leash interactions and feeding). VB always mounts.
- fix_kind: mixin-vb
- fix: In HappyGhast.mobInteract, use @ModifyExpressionValue on the second `isHarnessed()` call (ordinal 1, in the ride branch) and return `original && !player.isSecondaryUseActive()`. Capture `player` with @Local.
- config_gate: none

### GHAST-07  Passenger seat offsets are 1.8 instead of 1.7
- severity: gameplay
- vb: common/registries/entities/ModEntityTypes.java:37: `.passengerAttachments(new Vec3[]{new Vec3(0.0, 4.0, 1.8), new Vec3(-1.8, 4.0, 0.0), new Vec3(0.0, 4.0, -1.8), new Vec3(1.8, 4.0, 0.0)})`
- native: world/entity/EntityTypes.java:488: `.passengerAttachments(new Vec3(0.0, 4.0, 1.7), new Vec3(-1.7, 4.0, 0.0), new Vec3(0.0, 4.0, -1.7), new Vec3(1.7, 4.0, 0.0))`
- difference: All four seats sit 0.1 blocks further from the centre in VB. This also shifts the shear-drop average used in GHAST-05 (it averages to 0, so no change there).
- fix_kind: mixin-vb
- fix: In `ModEntityTypes.<clinit>`, @ModifyArg on `EntityType$Builder.passengerAttachments([Lnet/minecraft/world/phys/Vec3;)` with ordinal 0 (it is the only Vec3[] overload call in the class). Return `new Vec3[]{new Vec3(0,4,1.7), new Vec3(-1.7,4,0), new Vec3(0,4,-1.7), new Vec3(1.7,4,0)}`.
- config_gate: none

### GHAST-08  Dismount location is not the top of the ghast
- severity: gameplay
- vb: Not overridden, so the 1.21.1 LivingEntity.getDismountLocationForPassenger default applies (it searches sideways around the vehicle).
- native: HappyGhast.java:596-599: `public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ()); }`
- difference: Native puts dismounting riders on top of the ghast's back. VB places them beside the 4x4 body, often in mid-air, so they fall.
- fix_kind: mixin-vb
- fix: Add to the `@Mixin(HappyGhast.class)` (VB): `@Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return new Vec3(getX(), getBoundingBox().maxY, getZ()); }`
- config_gate: none

### GHAST-09  Collision rules: a ridden ghast blocks every entity, and the client-side "player above" rule is missing
- severity: gameplay
- vb: HappyGhast.java:578-580: `public boolean canBeCollidedWith() { return !this.isBaby() && this.isAlive() ? this.isVehicle() || this.isOnStillTimeout() : false; }`
- native: HappyGhast.java:579-589: `if (!isBaby() && isAlive()) { if (level().isClientSide() && other instanceof Player && other.position().y >= getBoundingBox().maxY) return true; else return this.isVehicle() && other instanceof HappyGhast ? true : this.isOnStillTimeout(); }`
- difference: When ridden, native ghasts are solid only to other happy ghasts. VB's are solid to players and all mobs. Native also makes the ghast solid client-side to any player at or above its top face, which lets players land on it before the STAYS_STILL flag syncs. VB lacks this, so players can briefly sink in. 1.21.1 `canBeCollidedWith()` has no `other` parameter.
- fix_kind: mixin-mc
- fix: Use `@Mixin(Entity.class)` and @Inject HEAD cancellable into `canCollideWith(Entity entity)`. `if (entity instanceof HappyGhast g) { boolean c = !g.isBaby() && g.isAlive() && ((g.level().isClientSide() && (Object)this instanceof Player && ((Entity)(Object)this).position().y >= g.getBoundingBox().maxY) || (g.isVehicle() && (Object)this instanceof HappyGhast) || g.isOnStillTimeout()); cir.setReturnValue(c && !((Entity)(Object)this).isPassengerOfSameVehicle(entity)); }`. Do the same in `Boat.canCollideWith`/`AbstractMinecart.canCollideWith`, which use `canVehicleCollide`.
- config_gate: none

### GHAST-10  Ghastling eye height is 0.6175 instead of 0.46875
- severity: gameplay
- vb: ModEntityTypes.java:35-36 `.sized(4.0F, 4.0F).eyeHeight(2.6F)` and HappyGhast.java:259-261 `getAgeScale() → 0.2375F`. getDefaultDimensions is not overridden, so the baby eye height is 2.6 × 0.2375 = 0.6175.
- native: HappyGhast.java:80 `BABY_DIMENSIONS = EntityTypes.HAPPY_GHAST.getDimensions().scale(0.2375F).withEyeHeight(0.46875F)`, and :247-249 `getDefaultDimensions → isBaby() ? BABY_DIMENSIONS : super...`
- difference: The ghastling eye sits 0.14375 blocks higher in VB. This affects line of sight, sensors, look and leash attach points, and the breathing-in-fluid check.
- fix_kind: mixin-vb
- fix: Add `@Override protected EntityDimensions getDefaultDimensions(Pose pose) { return isBaby() ? getType().getDimensions().scale(0.2375F).withEyeHeight(0.46875F) : super.getDefaultDimensions(pose); }`
- config_gate: none

### GHAST-11  Ghastling pathfinding range: 48 required path length is missing
- severity: gameplay
- vb: HappyGhast.java:582-587 BabyFlyingPathNavigation: `setCanOpenDoors(false); setCanFloat(true);`. Path range = FOLLOW_RANGE 16, max visited nodes = 16×16 = 256.
- native: HappyGhast.java:602-607: `... this.setRequiredPathLength(48.0F);`. Then PathNavigation.java:71-82: `maxVisitedNodes = floor(max(FOLLOW_RANGE, requiredPathLength) * 16)` = 768, and createPath uses `getMaxPathLength()` = 48.
- difference: Native ghastlings can path up to 48 blocks with 768 nodes; VB stops at 16 blocks with 256 nodes. This hurts following adults and players, panic, and strolling around terrain.
- fix_kind: mixin-vb
- fix: Use `@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast$BabyFlyingPathNavigation")` and add `@Override protected PathFinder createPathFinder(int n) { return super.createPathFinder(Math.max(n, 768)); }` and `@Override protected Path createPath(Set<BlockPos> t, int r, boolean up, int acc) { return createPath(t, r, up, acc, Math.max((float)mob.getAttributeValue(Attributes.FOLLOW_RANGE), 48.0F)); }`
- config_gate: none

### GHAST-12  Ghastlings are tempted from 10 blocks instead of 16
- severity: gameplay
- vb: common/registries/entities/ModSensorTypes.java:14: `new TemptingSensor(HappyGhast.IS_FOOD)`. 1.21.1 TemptingSensor hardcodes `TargetingConditions...range(10.0)` and `entity.closerThan(p, 10.0)`.
- native: HappyGhast.java:155 `.add(Attributes.TEMPT_RANGE, 16.0)`, and TemptingSensor.java:39 `TEMPT_TARGETING.copy().range((float)body.getAttributeValue(Attributes.TEMPT_RANGE))`
- difference: Ghastlings (brain AI) notice a player holding a snowball at 16 blocks in native vs 10 in VB. Adults are already correct through `setTemptRange(16)`.
- fix_kind: mixin-mc
- fix: Use `@Mixin(TemptingSensor.class)` and @Inject HEAD cancellable into `doTick(ServerLevel, PathfinderMob)` when `entity instanceof HappyGhast`. Re-run the filter with `TargetingConditions.forNonCombat().ignoreLineOfSight().range(16.0)` and `closerThan(p, 16.0)`, set or erase TEMPTING_PLAYER, then cancel.
- config_gate: none

### GHAST-13  Ghastling close-enough and adult-follow distances are 5 instead of 3
- severity: gameplay
- vb: HappyGhastAi.java:29-30 `CLOSE_ENOUGH_DIST = 5.0; ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);` and :86 `new FollowTemptation(entity -> 1.25F, entity -> 5.0)`
- native: HappyGhastAi.java:27-28 `BABY_GHAST_CLOSE_ENOUGH_DIST = 3.0; ADULT_FOLLOW_RANGE = UniformInt.of(3, 16);` and :52 `new FollowTemptation(mob -> 1.25F, mob -> 3.0, true)`
- difference: A tempted ghastling stops 5 blocks from the player in VB vs 3 in native. Adult-following starts beyond 5 instead of 3 blocks, with a walk-target closeness of 4 instead of 2 (`min - 1`).
- fix_kind: mixin-vb
- fix: In `HappyGhastAi.<clinit>`, @Redirect `UniformInt.of(II)` → `UniformInt.of(3, 16)`. In `initIdleActivity`, use @ModifyConstant(doubleValue = 5.0) → 3.0. The 5.0 sits in a lambda (`lambda$initIdleActivity$1`), so alternatively @Redirect the `FollowTemptation.<init>` NEW to pass `e -> 3.0`.
- config_gate: none

### GHAST-14  Ghastling walk targets aim at the feet instead of the eyes of the player or adult
- severity: gameplay
- vb: GhastlingFollowAdult.java:28 `new WalkTarget(new EntityTracker(leader, false), ...)`. 1.21.1 FollowTemptation.tick uses `new WalkTarget(new EntityTracker(player, false), speed, 2)`.
- native: BabyFollowAdult.create(..., targetEye = true) → `new WalkTarget(new EntityTracker(adult, targetEye, targetEye), ...)`. FollowTemptation with `lookInTheEyes = true` → `new EntityTracker(player, true, true)`. EntityTracker.java:31-33 `currentBlockPosition() → targetEyeHeight ? BlockPos.containing(entity.getEyePosition()) : entity.blockPosition()`.
- difference: Native ghastlings fly toward the eye position of the player or adult (for an adult happy ghast that is y +2.6). VB ones go to the feet block, so they hover low and under adults.
- fix_kind: engine (1.21.1 EntityTracker has no targetEyeHeight); closest fix is mixin-vb plus mixin-mc
- fix: Add a `PositionTracker` implementation `EyeTracker(Entity e)` with `currentPosition() = e.getEyePosition()` and `currentBlockPosition() = BlockPos.containing(e.getEyePosition())`, delegating `isVisibleBy` like EntityTracker. @Redirect the `EntityTracker.<init>(Entity,Z)` call for WalkTarget in GhastlingFollowAdult's lambda to `new EyeTracker(leader)`. Then @Redirect `WalkTarget.<init>(PositionTracker,FI)` in `FollowTemptation.tick` to use `EyeTracker(player)` when `body instanceof HappyGhast`.
- config_gate: none

### GHAST-15  Ghastling panic picks land positions instead of air or water positions
- severity: gameplay
- vb: HappyGhastAi.java:74 `new AnimalPanic(2.0F)`. In 1.21.1 AnimalPanic.getPanicPos this becomes `LandRandomPos.getPos(pathfinder, 5, 4)`.
- native: HappyGhastAi.java:40 `new AnimalPanic(2.0F, 0)`, and AnimalPanic.java:39-45 `AirAndWaterRandomPos.getPos(mob, 5, 4, flyHeight=0, mob.getViewVector(0.0F).x, mob.getViewVector(0.0F).z, (float)(Math.PI / 2))`
- difference: A hurt ghastling flees to a random air or water spot ahead of it (±90°) in native. In VB it picks ground-reachable positions, or none at all when flying over the void.
- fix_kind: mixin-mc
- fix: Use `@Mixin(AnimalPanic.class)` and @Redirect `LandRandomPos.getPos(PathfinderMob,II)` inside `getPanicPos`. If `mob instanceof HappyGhast`, return `AirAndWaterRandomPos.getPos(mob, 5, 4, 0, mob.getViewVector(0).x, mob.getViewVector(0).z, Math.PI / 2)`; otherwise call the original.
- config_gate: none

### GHAST-16  followable_friendly_mobs tag is missing armadillo
- severity: data
- vb: data/minecraft/tags/entity_type/followable_friendly_mobs.json: `["minecraft:bee", "minecraft:camel", ...]` (24 entries, no armadillo)
- native: data/minecraft/tags/entity_type/followable_friendly_mobs.json: `["minecraft:armadillo", "minecraft:bee", ...]` (25 entries)
- difference: Native ghastlings (AdultSensorAnyType) also follow adult armadillos; VB ones don't.
- fix_kind: data-override (data/minecraft/tags/entity_type/followable_friendly_mobs.json)
- fix: `{"values":["minecraft:armadillo","minecraft:bee","minecraft:camel","minecraft:cat","minecraft:chicken","minecraft:cow","minecraft:donkey","minecraft:fox","minecraft:goat","minecraft:happy_ghast","minecraft:horse","minecraft:skeleton_horse","minecraft:llama","minecraft:mule","minecraft:ocelot","minecraft:panda","minecraft:parrot","minecraft:pig","minecraft:polar_bear","minecraft:rabbit","minecraft:sheep","minecraft:sniffer","minecraft:strider","minecraft:villager","minecraft:wolf"]}`. Armadillo is an AgeableMob, so VB's cast in AdultSensorAnyType stays safe.
- config_gate: none

### GHAST-17  Fast-heal cloud band is 192 instead of 192.33, and precipitation healing requires a "natural" dimension
- severity: gameplay
- vb: HappyGhast.java:466-470 `server.dimensionType().natural() && (WorldUtilities.EnvironmentUtils.isInClouds(this) || precipitationAt(...) != NONE)`, and WorldUtilities.java:192-200 `if (natural()) { int cloudHeight = 192; if (getY() + getBbHeight() < cloudHeight) return false; ... return getY() <= cloudHeight + 4; }`
- native: HappyGhast.java:466 `isFastHealing = this.isInClouds() || level.precipitationAt(blockPosition()) != NONE`, and Entity.java:1635-1646, where cloud height is `EnvironmentAttributes.CLOUD_HEIGHT` (overworld 192.33, DimensionDefaults.java:12) with `cloudTop = cloudBottom + 4.0F`. Dimensions whose cloud alpha is 0 are excluded.
- difference: The fast-heal band is y ∈ [188.33, 196.33] in native vs [188, 196] in VB. VB also blocks rain healing in non-natural custom dimensions that have weather and sky. It matches in vanilla dimensions.
- fix_kind: mixin-vb
- fix: In WorldUtilities.EnvironmentUtils.isInClouds, use @Inject HEAD cancellable: `if (!natural) return false; float b = 192.33F; return !(e.getY() + e.getBbHeight() < b) && e.getY() <= b + 4.0F;`. In HappyGhast.continuousHeal, @Redirect `DimensionType.natural()` to return true and move the natural() check into isInClouds only.
- config_gate: none

### GHAST-18  Riding camera distance is fixed at 8 and ignores scale
- severity: visual
- vb: core/mixin/client/controllable_mount_effects/CameraMixin.java:30-33: `if (entity.isPassenger() && entity.getVehicle() instanceof HappyGhast) { this.move(-this.getMaxZoom(8.0F), 0.0F, 0.0F); ci.cancel(); }`
- native: Camera.java:277-290: `cameraScale = living.getScale(); cameraDistance = CAMERA_DISTANCE (player 4)`, `mountScale = mount.getScale(); mountDistance = mount CAMERA_DISTANCE (8)` → `move(-getMaxZoom(Math.max(cameraScale * cameraDistance, mountScale * mountDistance)), 0, 0)`
- difference: Native uses max(playerScale × 4, ghastScale × 8). VB always uses 8: it ignores the ghast's SCALE attribute (scale 0.5 should give max(4, 4) = 4) and the player's scale (scale 3 should give 12). It also bypasses NeoForge's detached-camera-distance hook.
- fix_kind: mixin-vb
- fix: In CameraMixin.onCameraSetup, @ModifyArg the `getMaxZoom(F)` argument to `Math.max(ClientHooks.getDetachedCameraDistance(camera, thirdPersonReverse, ((LivingEntity)entity).getScale(), 4.0F) * ((LivingEntity)entity).getScale(), ghast.getScale() * 8.0F)`.
- config_gate: none

### GHAST-19  Shadow radius is 1.5 instead of 2.0
- severity: visual
- vb: client/level/renderer/entity/mob/HappyGhastRenderer.java:26: `super(context, new HappyGhastModel(...), 1.5F);`
- native: client/renderer/entity/HappyGhastRenderer.java: `super(context, new HappyGhastModel(...HAPPY_GHAST), new HappyGhastModel(...HAPPY_GHAST_BABY), 2.0F);`. MobRenderer scales by ageScale.
- difference: The adult shadow is 1.5 vs 2.0 blocks, and the baby shadow is 0.356 vs 0.475.
- fix_kind: mixin-vb
- fix: Use @ModifyArg on `MobRenderer.<init>(Context, EntityModel, F)` (index 2) in `HappyGhastRenderer.<init>` and return 2.0F.
- config_gate: none

### GHAST-20  Harness "body squeeze" scale is never reset, so it leaks to unharnessed ghasts
- severity: visual
- vb: client/level/model/entity/happy_ghast/HappyGhastModel.java:99-103: `if (entity.hasItemInSlot(EquipmentSlot.CHEST)) { this.body.xScale = 0.9375F; this.body.yScale = 0.9375F; this.body.zScale = 0.9375F; }`. There is no else branch, and the 1.21.1 HierarchicalModel does not reset poses between renders.
- native: HappyGhastModel.setupAnim: `super.setupAnim(state)` (EntityModel resets the pose of every part to its initial pose each frame), then `if (!state.bodyItem.isEmpty()) { body.xScale = body.yScale = body.zScale = 0.9375F; }`
- difference: The model instance is shared by every happy ghast. Once one harnessed ghast renders, every unharnessed ghast and ghastling drawn later also renders at 93.75% body scale, until restart.
- fix_kind: mixin-vb
- fix: In `HappyGhastModel.setupAnim`, @Inject HEAD and set `body.xScale = body.yScale = body.zScale = 1.0F` (body is a @Shadow field).
- config_gate: none

### GHAST-21  Dried ghast is missing forceSolidOn
- severity: gameplay
- vb: common/registries/blocks/ModBlocks.java:325-328: `Properties.of().mapColor(MapColor.COLOR_GRAY).instabreak().sound(ModSoundTypes.DRIED_GHAST).noOcclusion().randomTicks()`
- native: Blocks.java:3862: `BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).forceSolidOn().instabreak().sound(SoundType.DRIED_GHAST).noOcclusion().randomTicks()`
- difference: Native's dried ghast is "legacy solid" (`isSolid()`/`blocksMotion()` true). VB's 10×10×10 shape computes to non-solid. This changes which blocks can be placed or attached on top, fluid-flow blocking checks, and anything that tests `blocksMotion`.
- fix_kind: mixin-vb
- fix: Use @ModifyArg on `HorizontalDirectionalBlock.<init>(Properties)` in `DriedGhastBlock.<init>` and return `properties.forceSolidOn()`.
- config_gate: none

### GHAST-22  Dried ghast ambient sounds play on the AMBIENT channel instead of BLOCKS
- severity: audio
- vb: DriedGhastBlock.java:142 `playLocalSound(x, y, z, DRIED_GHAST_AMBIENT.get(), SoundSource.AMBIENT, 1.0F, 1.0F, false)` and :150 `DRIED_GHAST_AMBIENT_WATER ... SoundSource.AMBIENT`
- native: DriedGhastBlock.java:136 `... SoundEvents.DRIED_GHAST_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false` and :144 `DRIED_GHAST_AMBIENT_WATER, SoundSource.BLOCKS`
- difference: The "Blocks" slider controls these sounds natively; in VB the "Ambient/Environment" slider does.
- fix_kind: mixin-vb
- fix: Use @ModifyArg (index 4, SoundSource) on both `Level.playLocalSound(DDDLSoundEvent;LSoundSource;FFZ)` calls in `DriedGhastBlock.animateTick` and return `SoundSource.BLOCKS`.
- config_gate: none

### GHAST-23  Dry-state particle is SMOKE instead of WHITE_SMOKE
- severity: visual
- vb: DriedGhastBlock.java:146 `level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.02, 0.0);`
- native: DriedGhastBlock.java:140 `level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.02, 0.0);`
- difference: VB shows dark grey smoke where native shows white smoke. WHITE_SMOKE exists in 1.21.1.
- fix_kind: mixin-vb
- fix: @Redirect the GETSTATIC `ParticleTypes.SMOKE` in `DriedGhastBlock.animateTick` to return `ParticleTypes.WHITE_SMOKE`.
- config_gate: none

### GHAST-24  Placing a dried ghast in falling flowing water waterlogs it
- severity: gameplay
- vb: DriedGhastBlock.java:176 `boolean isWaterlogged = state.is(FluidTags.WATER) && state.getAmount() == 8;`
- native: DriedGhastBlock.java:171 `boolean isWaterSource = replacedFluidState.is(Fluids.WATER);` (`Fluids.WATER` is the source fluid only)
- difference: Flowing water with amount 8 (falling columns) waterlogs the block in VB, so it starts hydrating. Native waterlogs only on a source block.
- fix_kind: mixin-vb
- fix: Use @Inject RETURN on `getStateForPlacement` and set `WATERLOGGED` to `ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER)` (FluidState.is(Fluid) compares the exact source type).
- config_gate: none

### GHAST-25  Dried ghast models swap the east and west textures
- severity: visual
- vb: assets/minecraft/models/block/dried_ghast_hydration_{0..3}.json: `"east": "minecraft:block/dried_ghast_hydration_N_west", "west": "minecraft:block/dried_ghast_hydration_N_east"`
- native: same files: `"east": "minecraft:block/dried_ghast_hydration_N_east", "west": "minecraft:block/dried_ghast_hydration_N_west"`
- difference: The left and right side textures are mirrored on all four hydration stages. The texture files themselves are identical.
- fix_kind: asset-override (assets/minecraft/models/block/dried_ghast_hydration_0.json … _3.json)
- fix: For N in 0..3: `{"parent":"minecraft:block/dried_ghast","textures":{"bottom":"minecraft:block/dried_ghast_hydration_N_bottom","east":"minecraft:block/dried_ghast_hydration_N_east","north":"minecraft:block/dried_ghast_hydration_N_north","particle":"minecraft:block/dried_ghast_hydration_N_north","south":"minecraft:block/dried_ghast_hydration_N_south","tentacles":"minecraft:block/dried_ghast_hydration_N_tentacles","top":"minecraft:block/dried_ghast_hydration_N_top","west":"minecraft:block/dried_ghast_hydration_N_west"}}`
- config_gate: none

### GHAST-26  Dried ghast base model is missing six tentacle cullfaces
- severity: visual
- vb: assets/minecraft/models/block/dried_ghast.json: `left_tent_1`/`left_tent_2` west faces, `right_tent_1`/`right_tent_2` east faces, and the two back tentacles' south faces have no `cullface`.
- native: same file: `"west": {..., "cullface": "west"}` (left tentacles), `"east": {..., "cullface": "east"}` (right), `"south": {..., "cullface": "south"}` (back).
- difference: The outer tentacle tips touch the block edge. Native culls those faces against an adjacent full block; VB always draws them, so you get z-fighting and overdraw.
- fix_kind: asset-override (assets/minecraft/models/block/dried_ghast.json)
- fix: Copy src-mc/assets/minecraft/models/block/dried_ghast.json verbatim. The format is 1.21.1-compatible, and the diff is only the six cullface entries.
- config_gate: none

### GHAST-27  Harness dye recipes accept a harness of the same colour
- severity: data
- vb: data/minecraft/recipe/dye_red_harness.json: `"ingredients": [{"item": "minecraft:red_dye"}, {"tag": "minecraft:harnesses"}]`
- native: data/minecraft/recipe/dye_red_harness.json: `"ingredients": ["minecraft:red_dye", ["minecraft:white_harness", ... the 15 other colours, red excluded]]`
- difference: In VB, red dye plus a red harness crafts a red harness and wastes the dye; the recipe book also lists that combination. Native excludes the target colour. This applies to all 16 dye_*_harness recipes.
- fix_kind: data-override (data/minecraft/recipe/dye_<color>_harness.json ×16)
- fix: For each colour C: `{"type":"minecraft:crafting_shapeless","category":"equipment","group":"harness_dye","ingredients":[{"item":"minecraft:C_dye"},[{"item":"minecraft:white_harness"},… every colour except C …]],"result":{"count":1,"id":"minecraft:C_harness"}}`. 1.21.1 accepts an ingredient array of `{"item":…}` objects.
- config_gate: none

### GHAST-28  Recipe-book category for harness recipes is wrong
- severity: data
- vb: recipe/red_harness.json `"category": "misc"`; recipe/dye_red_harness.json `"category": "building"`
- native: both `"category": "equipment"`
- difference: VB lists harnesses under the Misc and Building tabs of the recipe book instead of Equipment. This covers all 32 harness recipes; `equipment` is a valid CraftingBookCategory in 1.21.1. (dried_ghast.json `building` matches.)
- fix_kind: data-override (data/minecraft/recipe/<color>_harness.json, dye_<color>_harness.json)
- fix: Set `"category": "equipment"` in all 32 files and keep the rest of VB's JSON (or use GHAST-27's JSON for the dye recipes).
- config_gate: none

### GHAST-29  Loot tables are missing random_sequence
- severity: data
- vb: loot_table/entities/happy_ghast.json `{"type":"minecraft:entity"}`; loot_table/blocks/dried_ghast.json has no `random_sequence`
- native: `"random_sequence": "minecraft:entities/happy_ghast"` and `"random_sequence": "minecraft:blocks/dried_ghast"`
- difference: Without a random sequence the drop RNG is not seeded per world and table, so outcomes are not reproducible the way native ones are. The happy ghast table has no pools, so it only matters for dried_ghast. `random_sequence` exists in 1.21.1.
- fix_kind: data-override (data/minecraft/loot_table/blocks/dried_ghast.json, entities/happy_ghast.json)
- fix: Use VB's JSON plus `"random_sequence": "minecraft:blocks/dried_ghast"` and `"random_sequence": "minecraft:entities/happy_ghast"`.
- config_gate: none

### GHAST-30  Dried ghast is appended at the end of the piglin barter pool instead of after ender pearls
- severity: data
- vb: common/integrations/LootIntegrations.java:79-84: `context.addToPool(LootItem.lootTableItem(DRIED_GHAST).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))`, which appends it as the last entry.
- native: loot_table/gameplay/piglin_bartering.json lists the entries in order `... iron_nugget(10), ender_pearl(10), dried_ghast(10), string(20), quartz(20), ...`
- difference: The probabilities are identical (weight 10/469). The entry order differs, so the same RNG roll maps to different items, and bartering is not 1:1 seed-reproducible with native.
- fix_kind: data-override (data/minecraft/loot_table/gameplay/piglin_bartering.json)
- fix: Ship the native 26.2 piglin_bartering.json, converted to 1.21.1 syntax (identical structure), with dried_ghast at index 7, and suppress VB's addToPool. To keep the `dried_ghasts` gate, use a NeoForge loot modifier or condition instead: wrap the entry with `{"condition":"neoforge:mod_loaded"...}`, or cancel VB's injection when the config is true and the override is present.
- config_gate: dried_ghasts

### GHAST-31  Nether fossil dried-ghast placement adds a nether-biome requirement
- severity: data
- vb: core/mixin/common/happy_ghast/NetherFossilPieceMixin.java:53 `if (hasDriedGhasts && level.getBiome(origin).is(BiomeTags.IS_NETHER)) {`
- native: NetherFossilPieces.java placeDriedGhast has no biome check: `RandomSource positionalRandom = RandomSource.createThreadLocalInstance(level.getSeed()).forkPositional().at(fossilBB.getCenter()); if (positionalRandom.nextFloat() < 0.5F) {...}`
- difference: The same 50% chance and position RNG apply, which is equivalent (LegacyRandomSource vs SingleThreadedRandomSource produce the same sequence). But fossils placed outside nether biomes, by datapacks or custom dimensions, never get a dried ghast in VB.
- fix_kind: mixin-vb
- fix: In `placeDriedGhast`, @Redirect `Holder.is(TagKey)` to return true.
- config_gate: dried_ghasts

### GHAST-32  Lead sounds: VB uses the old leash-knot and sheep-shear sounds
- severity: audio
- vb: LeashInteraction.java:42 `entity.playSound(SoundEvents.LEASH_KNOT_PLACE)` (leash-to-leash), :58 `LEASH_KNOT_BREAK` (unleash), :75 `LEASH_KNOT_PLACE` (lead attach), :92 `SoundEvents.SHEEP_SHEAR` (shearing connections); LeashableTickMixin.java:50 `playSound(null, holder.blockPosition(), LEASH_KNOT_BREAK, NEUTRAL, 1, 1)` (snap); FireworkRocketItemMixin.java:29 `LEASH_KNOT_BREAK`
- native: Entity.java:2299 `LEAD_TIED`, :2327 `LEAD_UNTIED`, :2346 `LEAD_TIED`, :2354 `SoundEvents.SHEARS_SNIP`; Leashable.java tickLeash `level.playSound(null, leashHolder.getX(), leashHolder.getY(), leashHolder.getZ(), SoundEvents.LEAD_BREAK, NEUTRAL, 1, 1)`; FireworkRocketItem.java:63 `LEAD_BREAK`; LeashFenceKnotEntity :63 `LEAD_UNTIED`, :109 and :152 `LEAD_TIED`
- difference: Native added the `item.lead.tied`, `item.lead.untied`, `item.lead.break` and `item.shears.snip` events with new audio. VB plays the knot or sheep sounds. The snap sound also plays at the block centre (`blockPosition()`) instead of the holder's exact position.
- fix_kind: engine (the .ogg files are not in the 1.21.1 asset index)
- fix: Register `item.lead.tied`/`untied`/`break` and `item.shears.snip` SoundEvents with a resource pack that supplies the 26.2 audio, then @Redirect the GETSTATICs listed above. Without the audio, the only fixable part is the snap position: @Redirect `Level.playSound(Player,BlockPos,...)` in LeashableTickMixin to `playSound(null, holder.getX(), holder.getY(), holder.getZ(), ...)`.
- config_gate: modern_leashes (tick, interactions), leash_drop_connections (firework)

### GHAST-33  Fence knot rework is missing (taking leashes back, auto-removing knots, shears)
- severity: gameplay
- vb: VB does not touch LeashFenceKnotEntity, so 1.21.1 applies (src-121 LeashFenceKnotEntity.java:80-118). Clicking a knot with no leashed mobs in hand runs `this.discard()`; in creative it also drops the knot's leashes. There is no notifyLeasheeRemoved.
- native: LeashFenceKnotEntity.java:75-120: shears first run `super.interact` (shear all connections). The player's leashed mobs are then attached when `canHaveALeashAttachedTo(this)`. Otherwise, when not sneaking, the knot's mobs move back to the player (`mob.setLeashedTo(player, true)`). The method plays `LEAD_TIED` and returns SUCCESS. `notifyLeasheeRemoved → if (leashableLeashedTo(this).isEmpty()) discard()`.
- difference: In native, right-clicking a knot hands its mobs back to you; in VB it destroys the knot and the leads drop. Native removes a knot automatically once nothing is tied to it; in VB empty knots stay until clicked. Shears on a knot drop all its connections in native.
- fix_kind: mixin-mc
- fix: Use `@Mixin(LeashFenceKnotEntity.class)` and @Inject HEAD cancellable into `interact(Player, InteractionHand)` with the 26.2 body above, using `LeashPhysics.leashableLeashedTo` and `LeashPhysics.canAttachLeash`, and the `LeashInteraction` shears path for shears. To get auto-discard, @Inject TAIL into `Leashable.dropLeash(Entity,boolean,boolean)` (static) and `Leashable.setLeashedTo(...)`: if the old holder is a LeashFenceKnotEntity with no remaining leashees, discard it.
- config_gate: modern_leashes

### GHAST-34  Tying mobs to a fence uses the old 7-block box with no distance check
- severity: gameplay
- vb: VB does not touch LeadItem, so 1.21.1 applies: `leashableInArea(level, pos, ...)` with `new AABB(i - 7.0, j - 7.0, k - 7.0, i + 7.0, j + 7.0, k + 7.0)`, which is anchored at the block corner. Every mob found is tied, with no distance check.
- native: LeadItem.java bindPlayerMobs: `Leashable.leashableInArea(level, Vec3.atCenterOf(pos), ...)` (32×32×32 box centred on the block), and each one is tied only `if (leashable.canHaveALeashAttachedTo(activeKnot))` (bbox-centre distance ≤ leashSnapDistance: 12, or 16 for happy ghasts). The knot is discarded if nothing got tied.
- difference: Native ties mobs up to 12 or 16 blocks away; VB misses mobs 7 to 16 blocks away. The VB box is also off-centre by 0.5. Native won't create an orphan knot.
- fix_kind: mixin-mc
- fix: Use `@Mixin(LeadItem.class)` and @Inject HEAD cancellable into `bindPlayerMobs(Player, Level, BlockPos)` with the 26.2 body: `LeashPhysics.leashableInArea(level, Vec3.atCenterOf(pos), l -> l.getLeashHolder() == player)`, plus `LeashFenceKnotEntity.getOrCreateKnot` and `LeashPhysics.canAttachLeash`.
- config_gate: modern_leashes

### GHAST-35  Llamas and trader llamas use four-rope quad leashes
- severity: gameplay
- vb: common/api/modules/leash_behavior/LeashPhysics.java:48 `entity -> entity instanceof AbstractChestedHorse` → quad offsets. LeashableCallback.vb$supportsQuadLeash returns true for any matching predicate, so it applies to llamas.
- native: animal/equine/Llama.java:424-427 `public boolean supportQuadLeash() { return false; }`
- difference: A llama leashed to a happy ghast gets four ropes and the shared-quad spring physics (force ×0.25 over four points) in VB. Native uses a single rope with single-point physics.
- fix_kind: mixin-mc
- fix: Use `@Mixin(Llama.class)` and add `public boolean vb$supportsQuadLeash() { return false; }`. This overrides the LeashableCallback default injected through VB's EntityMixin.
- config_gate: modern_leashes

### GHAST-36  Zombie horse uses the horse quad offsets instead of the chested-horse ones
- severity: visual
- vb: LeashPhysics.java:52 `entity instanceof AbstractHorse → createQuadOffsets(entity, 0.04, 0.52, 0.23, 0.87)`, which ZombieHorse falls into.
- native: animal/equine/ZombieHorse.java:193-194 `getQuadLeashOffsets() → Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73)`
- difference: On a zombie horse the four rope attachment points sit wider (0.23 vs 0.18 × width), longer (0.52 vs 0.41) and higher (0.87 vs 0.73 × height) than native. This only affects rendering, because physics uses the shared attachment points.
- fix_kind: mixin-mc
- fix: Use `@Mixin(ZombieHorse.class)` and add `public Vec3[] vb$getQuadLeashOffsets() { return LeashPhysics.createQuadOffsets((Entity)(Object)this, 0.04, 0.41, 0.18, 0.73); }`
- config_gate: modern_leashes

### GHAST-37  Elastic leash pull no longer stops horses eating or stands camels up
- severity: gameplay
- vb: LeashableCallback.java:42-44 `default void vb$onElasticLeashPull() { ((Entity)this).checkSlowFallDistance(); }`. Only HappyGhast overrides it. VB's tick replaces 1.21.1 tickLeash, which skips the old `handleLeashAtDistance` hooks (1.21.1 AbstractHorse:228 `if (distance > 6.0F && isEating()) setEating(false)`, Camel:374 `if (distance > 6.0F && isCamelSitting() && !isInPoseTransition() && canCamelChangePose()) standUp()`).
- native: AbstractHorse.java:193-199 `onElasticLeashPull() { super...; if (this.isEating()) this.setEating(false); }`; Camel.java:403-409 `onElasticLeashPull() { super...; if (isCamelSitting() && !isInPoseTransition() && canCamelChangePose()) standUp(); }`
- difference: With modern leashes, a sitting camel stays sitting and is dragged, and a grazing horse keeps eating while pulled. Both react in native and in vanilla 1.21.1.
- fix_kind: mixin-mc
- fix: Use `@Mixin(AbstractHorse.class)` and add `public void vb$onElasticLeashPull() { checkSlowFallDistance(); if (isEating()) setEating(false); }`. Use `@Mixin(Camel.class)` and add `public void vb$onElasticLeashPull() { checkSlowFallDistance(); if (isEating()) setEating(false); if (isCamelSitting() && !isInPoseTransition() && canCamelChangePose()) standUp(); }`.
- config_gate: modern_leashes

### GHAST-38  Leash is kept when the holder becomes a spectator
- severity: gameplay
- vb: LeashableTickMixin.java:40 `if (!entity.isAlive() || !data.leashHolder.isAlive()) entity.dropLeash(true, doEntityDrops)`
- native: Leashable.java tickLeash `if (!entity.canInteractWithLevel() || !leashData.leashHolder.canInteractWithLevel())`, where Entity.java:351 `canInteractWithLevel() { return isAlive() && !isRemoved() && !isSpectator(); }`
- difference: A player who switches to spectator mode keeps dragging leashed mobs in VB; native drops the leash.
- fix_kind: mixin-vb
- fix: In `LeashableTickMixin.vb$tickLeash`, @Redirect the second `Entity.isAlive()` call (ordinal 1) to `holder.isAlive() && !holder.isRemoved() && !holder.isSpectator()`. Apply the same to ordinal 0 for the leashee.
- config_gate: modern_leashes

### GHAST-39  Leash ribbon back pass uses the wrong fudge, so the cross-shaped strip is lost
- severity: visual
- vb: common/api/modules/leash_behavior/LeashFeatureRenderer.java:93-97: the forward loop calls `addVertexPair(builder, matrices, dx, dy, dz, 0.05F, dxOff, dzOff, segment, false, state)` and the backward loop calls `addVertexPair(builder, matrices, dx, dy, dz, 0.05F, dxOff, dzOff, segment, true, state)`
- native: client/renderer/feature/LeashFeatureRenderer.java prepare: forward `addVertexPair(..., 0.05F, ...)`, backward `addVertexPair(builder, pose, dx, dy, dz, 0.0F, dxOff, dzOff, k, true, leashState)`
- difference: Native's second strip uses fudge 0, which flips the vertical vertex order and gives two crossing diagonal ribbons (an X cross-section). VB draws the same diagonal twice, so leashes look like one flat strip that vanishes edge-on.
- fix_kind: mixin-vb
- fix: In `LeashFeatureRenderer.renderLeash`, @ModifyArg on `addVertexPair(...)` with ordinal 1 and index 5 (fudge), returning 0.0F.
- config_gate: modern_leashes

### GHAST-40  Non-quad leashed entities get two leashes drawn
- severity: visual
- vb: core/mixin/client/leash_behavior/EntityRendererMixin.java:46-50 `renderAdditional` (HEAD of render) always calls `this.leashRenderer.render(entity, ...)`. For non-quad entities, LeashFeatureRenderer.setupLeashRendering's else branch builds a single 0.05-wide leash state. In addition, :86-88 still calls the vanilla `original.call(...)` when `!vb$supportsQuadLeash()`, which is the 1.21.1 renderLeash with width 0.025 (src-121 EntityRenderer.java:105-139).
- native: one leash per connection, built in EntityRenderer.extractRenderState (:185-234) and drawn by LeashFeatureRenderer with width 0.05.
- difference: Every ordinary leashed mob (dog, cow, boat leashed to a player, and so on) renders a 0.05 modern leash plus a 0.025 legacy leash with different sag maths. The result is a double or z-fighting rope.
- fix_kind: mixin-mc
- fix: Use `@Mixin(value = EntityRenderer.class, priority = 1100)` and @Inject HEAD cancellable into `renderLeash`, cancelling when `VanillaBackport.COMMON_CONFIG.hasModernLeashes.get() && !ModChecker.SABLE`.
- config_gate: modern_leashes

### GHAST-41  Boat interpolation restarts on duplicate targets
- severity: cosmetic
- vb: common/api/modules/leash_behavior/InterpolationHandler.java:26-36 interpolateTo always sets `this.data.steps = this.interpolationSteps; ...`
- native: world/entity/InterpolationHandler.java interpolateTo: `else if (!hasActiveInterpolation() || !Objects.equals(yRot(), yRot) || !Objects.equals(xRot(), xRot) || !Objects.equals(position(), position)) { ...restart... }`
- difference: When the same target arrives again, VB restarts the 3-step lerp and native ignores it. Remote boats look slightly laggier in VB.
- fix_kind: mixin-vb
- fix: In `InterpolationHandler.interpolateTo`, @Inject HEAD cancellable: `if (this.interpolationSteps != 0 && hasActiveInterpolation() && data.position.equals(position) && data.yRot == y && data.xRot == x) ci.cancel();`
- config_gate: modern_leashes

### GHAST-42  Drowned nautilus jockeys: the check runs before the trident is equipped, and RNG order differs
- severity: gameplay
- vb: core/mixin/common/controllable_mounts/DrownedMixin.java:28-33: `hasNautilus && level.getRandom().nextFloat() < zombieNautilusSpawnChance && (reason == NATURAL || reason == STRUCTURE) && this.getMainHandItem().is(Items.TRIDENT) && !isBaby() && !biome.is(MORE_FREQUENT_DROWNED_SPAWNS)`. This runs from MobMixin.java:42-55 at `@At("RETURN")` of **Mob**.finalizeSpawn, which is inside Zombie.finalizeSpawn's `super` call. Zombie only calls `populateDefaultEquipmentSlots` (where Drowned receives its trident) after super returns (src-121 Zombie.finalizeSpawn).
- native: Drowned.java:116-131, at the end of Drowned.finalizeSpawn after all equipment: `(spawnReason == NATURAL || spawnReason == STRUCTURE) && getMainHandItem().is(Items.TRIDENT) && level.getRandom().nextFloat() < 0.5F && !isBaby() && !biome.is(MORE_FREQUENT_DROWNED_SPAWNS)` → `EntityTypes.ZOMBIE_NAUTILUS.create(level, JOCKEY)`
- difference: In VB, `getMainHandItem().is(TRIDENT)` is evaluated before Drowned gets its trident, so it is false for natural spawns and natural zombie-nautilus jockeys effectively never appear (please confirm in game). VB also draws the 0.5 random float for every drowned before the reason and trident checks, while native draws it only for trident-holding NATURAL or STRUCTURE drowned. That shifts the RNG for every later roll in the spawn.
- fix_kind: mixin-mc
- fix: Disable VB's hook. Use `@Mixin(value = Drowned.class, priority = 1500)` so it applies after VB's DrownedMixin has merged `vb$finalizeSpawn` into Drowned, then @Inject HEAD cancellable into `vb$finalizeSpawn` and `cir.setReturnValue(data)`. Then use `@Mixin(Drowned.class)` @Inject RETURN on `finalizeSpawn` with the native order: `if (hasNautilus && (reason==NATURAL||reason==STRUCTURE) && getMainHandItem().is(TRIDENT) && level.getRandom().nextFloat() < zombieNautilusSpawnChance && !isBaby() && !level.getBiome(blockPosition()).is(MORE_FREQUENT_DROWNED_SPAWNS)) { ...VB body (create, setPersistenceRequired if STRUCTURE, moveTo, finalizeSpawn(JOCKEY? VB uses reason), startRiding(n,false), addFreshEntity)... }`
- config_gate: has_nautilus, zombie_nautilus_spawn_chance

### GHAST-43  Camel-husk jockeys: the first husk of each pack can never get one, and the spear can be overwritten
- severity: gameplay
- vb: HuskMixin.java:37-48 runs at Mob.finalizeSpawn RETURN (MobMixin.java:42-55): `if (data != null) { data = new CamelHusk.HuskGroupData((ZombieGroupData)data); ... }`. At that point `data` is still the incoming group data, which is null for the first mob of a natural pack because Zombie creates its ZombieGroupData only after super returns. VB then sets `MAINHAND = IRON_SPEAR` before Zombie.populateDefaultEquipmentSlots runs.
- native: Husk.java finalizeSpawn: `groupData = super.finalizeSpawn(...)` (Zombie has already created ZombieGroupData and equipment) → `if (groupData != null) groupData = new HuskGroupData(...)` → 10% chance → `setItemSlot(MAINHAND, IRON_SPEAR)` after Zombie's equipment.
- difference: (a) In VB the first husk of every natural pack never rolls for a camel husk; native rolls for each one, so VB spawns about 25% fewer for 4-packs. (b) VB's spear can be replaced by Zombie's random sword or shovel roll, and its enchantment pass runs on the spear. (c) The RNG order differs: native's `setCanPickUpLoot` roll comes first.
- fix_kind: mixin-mc
- fix: Cancel VB's `Husk.vb$finalizeSpawn` from a `@Mixin(value = Husk.class, priority = 1500)` (HEAD, cancellable, return data unchanged) and @Inject RETURN of `Husk.finalizeSpawn` with VB's body applied to `cir.getReturnValue()`: wrap into HuskGroupData, roll `random.nextFloat() < 0.1F`, set the spear (or iron sword when `has_spears` is off), and spawn the camel husk plus parched. Then `cir.setReturnValue(huskData)`.
- config_gate: has_spears (held item)

### GHAST-44  Ridden-float boost is applied after jump-out-of-fluid instead of before
- severity: gameplay
- vb: core/mixin/common/controllable_mounts/LivingEntityMixin.java:19-25 postTravelInFluid adds `0.04F` to deltaMovement.y. It is invoked by core/mixin/common/attributes/LivingEntityMixin.java:41-55 at `INVOKE calculateEntityAnimation` in `travel`, after the 1.21.1 water branch has already done `if (horizontalCollision && isFree(...)) setDeltaMovement(x, 0.3F, z)`.
- native: LivingEntity.java:2509-2511 travelInFluid: `travelInWater(...); this.floatInWaterWhileRidden();` then :2565 `this.jumpOutOfFluid(oldY)` (sets y = 0.3).
- difference: When a ridden horse or camel hits a bank in water, native ends at y-velocity 0.3; VB ends at 0.34, giving a slightly higher hop out of water.
- fix_kind: mixin-mc
- fix: Move the boost into 1.21.1 `LivingEntity.travel`. @Inject before the first `this.horizontalCollision` read inside the `isInWater()` branch, using `@At(value="FIELD", target="horizontalCollision", opcode=GETFIELD, ordinal=0)`, and call the same tag-gated `+0.04` there. Then make `LivingEntityMixin.postTravelInFluid` (controllable_mounts) a no-op.
- config_gate: can_mounts_float_while_ridden

### GHAST-45  Dried-ghast advancement criterion has a different name
- severity: cosmetic
- vb: data/minecraft/advancement/husbandry/place_dried_ghast_in_water.json: `"criteria": {"place_dried_ghast_on_water": {...}}, "requirements": [["place_dried_ghast_on_water"]]`
- native: `"criteria": {"place_dried_ghast_in_water": {...}}, "requirements": [["place_dried_ghast_in_water"]]`
- difference: The criterion key differs (`on_water` vs `in_water`). Any `/advancement grant ... only husbandry/place_dried_ghast_in_water place_dried_ghast_in_water` command or datapack reference fails on VB. The trigger and conditions are identical.
- fix_kind: data-override (data/minecraft/advancement/husbandry/place_dried_ghast_in_water.json)
- fix: Use VB's JSON with both occurrences renamed to `place_dried_ghast_in_water`. Existing player progress under the old key is lost unless both keys are kept with OR: `"requirements": [["place_dried_ghast_in_water", "place_dried_ghast_on_water"]]`, with both criteria defined.
- config_gate: none
