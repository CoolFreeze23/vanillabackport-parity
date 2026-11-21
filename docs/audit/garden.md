# garden — The Garden Awakens (creaking, creaking heart, pale garden, eyeblossom, pale moss, resin, pale oak)

Summary
- 24 findings: gameplay 11, audio 5, visual 4, data 2, cosmetic 2.
- The biggest ones: resin spreading places far too many clumps per hit (GARDEN-01); in 1.21.1 the creaking's player sensor only reaches 16 blocks instead of 32, so freeze and deactivate happen at the wrong distance (GARDEN-02); FIGHT erases the attack target every time the creaking freezes, and it never drops a target it can no longer see (GARDEN-03/04); a heart removed by anything other than a player or an explosion leaves its creaking to die the normal way (GARDEN-05); creakings can be pushed into boats (GARDEN-06).
- Covered: every class and data file in scope, block and item registration properties, flammability, compost, strip, trades, woodland mansion resin loot, tags (searched by script), loot tables, recipes, advancements, worldgen JSON, biome JSON, textures (compared pixel by pixel), animation keyframes (compared by script; they match), model, renderer, emissive layer, trail particle.
- Not checked: sounds.json and .ogg files (native sounds are not in the jar). Pale garden climate placement (`BiomeGeneration` uses `Placement`/`Depth`/`Temperature` from the blackgear platform library, which is not in src-vb). Native puts pale garden in `OverworldBiomeBuilder.PLATEAU_BIOMES[2][4]` (src-mc …/biome/OverworldBiomeBuilder.java:93), so VB's MID/HIGH/PEAK slice points need checking against that table in a tree that has the library. Pale oak shelf, leaf-litter particles, falling-leaf particles and boat entities are left to their own areas.

## Coverage

| VB class / data | native counterpart | result |
|---|---|---|
| creaking/Creaking.java | world/entity/monster/creaking/Creaking.java | findings (05,06,07,08,09,10) |
| creaking/CreakingAi.java | world/entity/monster/creaking/CreakingAi.java | findings (02,03,04) |
| creaking/CreakingMeleeAttack.java | ai/behavior/MeleeAttack.java | match |
| blocks/CreakingHeartBlock.java | level/block/CreakingHeartBlock.java | match (day/night via `do_creaking_hearts_work_on_day` / isNaturalNight = CREAKING_ACTIVE timeline 12600..23400), except removal (05) |
| blocks/CreakingHeartState.java | state/properties/CreakingHeartState.java | match |
| block_entities/CreakingHeartBlockEntity.java | level/block/entity/CreakingHeartBlockEntity.java | findings (01,05,08); detection 32, too-far 34, spawn 5/16/8, grace 30, emitter 100/50/10, comparator math, particle colours all match |
| blocks/EyeblossomBlock.java | level/block/EyeblossomBlock.java | findings (11,14,15) |
| blocks/EyeblossomPotBlock.java | FlowerPotBlock (eyeblossom part) | match (VB checks `natural()`, which has the same effect as TriState DEFAULT) |
| blocks/HangingMossBlock.java | level/block/HangingMossBlock.java | findings (11,18) |
| blocks/MossyCarpetBlock.java | level/block/MossyCarpetBlock.java | match (logic); sound in 12 |
| blocks/PaleMossBlock.java | BonemealableFeaturePlacerBlock | finding (17) |
| blocks/ResinClumpBlock.java | MultifaceBlock (26.2, waterlogged built in) | match |
| tree decorators CreakingHeartDecorator / PaleMossDecorator | treedecorators/* | match |
| worldgen TheGardenAwakensFeatures/Placements + data JSON | data/minecraft/worldgen/* | match in meaning (tree `dirt_provider` = `below_trunk_provider` for these trees, the patch is equivalent to the `pale_forest_flower` placement); mixin finding 19 |
| mixin pale_garden/SimpleBlockFeatureMixin | SimpleBlockFeature (schedule_tick, MossyCarpet.placeAt) | finding (19) |
| worldgen biome pale_garden.json / TheGardenAwakensBiomes | worldgen/biome/pale_garden.json | colours, spawns, carvers, downfall and temperature match; findings 20, 22 |
| client CreakingModel / CreakingAnimation / CreakingRenderer / LivingEntityEmissiveLayer | client/model/monster/creaking, animation/definitions, renderer | match (keyframes identical by script; the eyes layer uses the full mesh but the eyes texture only has the 9 head texels, so nothing visible changes; overlay and death-flip suppression are equivalent because VB never advances deathTime) |
| TrailParticle / TrailParticleOption | client/particle/TrailParticle, core/particles/TrailParticleOption | match |
| emissive (EmissiveModelHandler/Wrapper, open_eyeblossom_emissive) | cross_emissive models | match (the base texture keeps the eye texels and the emissive quad is drawn over them) |
| ModBlocks entries | Blocks.java | finding (13); carpet sound 12; everything else matches (strength, sound, instrument, push, flammability, compost, strippables) |
| ModItems / ModEntityTypes / ModWoodTypes / ModBlockSetTypes / ModTreeGrowers / ModSoundTypes / ModSoundEvents | Items / EntityTypes / WoodType / BlockSetType / TreeGrower / SoundType / SoundEvents | match except 24 (spawn egg); all 35 sound ids are present |
| loot tables (blocks/*, entities/creaking, woodland_mansion resin) | same paths | match (only random_sequence and default-field format differences) |
| recipes / recipe advancements | same paths | match in meaning (`chain` vs `iron_chain` and the `category` default are engine and format differences) |
| advancements heart_transplanter / kill_a_mob / kill_all_mobs / adventuring_time | same | finding (21) |
| tags | tags/* | findings 15/16 (bee tags); `dirt`/`sniffer_diggable`/`enderman_holdable` reach the same result in VB through the 1.21.1 structure; boat tag in 06 |
| wandering trader trades (`pale_trades`) | villager_trade/wandering_trader/* | match (prices, counts, max uses, 0.05) |
| textures | same paths | finding 23 (boat items); open_eyeblossom extra texels are intentional (see emissive row) |

---

### GARDEN-01  Resin spreading keeps traversing after it places a clump (many clumps per hit instead of 2–3)
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/level/block_entities/CreakingHeartBlockEntity.java:245-277 (spreadResin)
  ```java
  BlockPos.breadthFirstTraversal(this.worldPosition, 2, 64, (pos, acceptor) -> {...}, pos -> {
     if (level.getBlockState(pos).is(ModBlockTags.CREAKING_HEART_HOLDERS)) {
        for (Direction dir : Util.shuffledCopy(Direction.values(), random)) { ...
           if (neighbourState.is(ModBlocks.RESIN_CLUMP.get()) && !MultifaceBlock.hasFace(neighbourState, opposite)) {
              level.setBlockAndUpdate(neighbourPos, ...); placedResin.setValue(neighbourPos);
              return false;      // 1.21.1 Predicate: false = skip this node, keep traversing
        } } }
     return true; });
  ```
  The 1.21.1 traversal (src-121/net/minecraft/core/BlockPos.java:517) does `if (longset.add(k) && predicate.test(blockpos)) {...}`, so returning `false` only skips that node.
- native: src-mc/net/minecraft/world/level/block/entity/CreakingHeartBlockEntity.java:276-283
  ```java
  level.setBlock(neightbourPos, neighbourState.setValue(MultifaceBlock.getFaceProperty(opposite), true), 3);
  placedResin.setValue(neightbourPos);
  return BlockPos.TraversalNodeStatus.STOP;   // breaks out of the whole traversal (BlockPos.java:504)
  ```
- difference: Native places exactly one resin face per `spreadResin` call, so 2–3 per creaking hit (`nextIntBetweenInclusive(2, 3)`). VB places one face on every pale-oak-log node that has a free neighbour (up to 64 nodes, depth 2), so it yields several times more resin per hit. Only the last position gets the place sound and game event.
- fix_kind: mixin-vb (CreakingHeartBlockEntity.spreadResin)
- fix: `@Mixin(CreakingHeartBlockEntity.class)` with `@Inject(method="spreadResin", at=@At("HEAD"), cancellable=true)`, then `cir.setReturnValue(addon$spread(level))`, where:
  ```java
  private Optional<BlockPos> addon$spread(ServerLevel level) {
     RandomSource random = level.getRandom();
     BlockPos start = ((BlockEntity)(Object)this).getBlockPos();
     ArrayDeque<Pair<BlockPos,Integer>> q = new ArrayDeque<>(); LongSet seen = new LongOpenHashSet();
     q.add(Pair.of(start, 0)); int count = 0;
     while (!q.isEmpty()) {
        Pair<BlockPos,Integer> n = q.poll(); BlockPos pos = n.getFirst(); int depth = n.getSecond();
        if (!seen.add(pos.asLong())) continue;
        if (level.getBlockState(pos).is(ModBlockTags.CREAKING_HEART_HOLDERS)) {
           for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
              BlockPos np = pos.relative(dir); BlockState ns = level.getBlockState(np); Direction opp = dir.getOpposite();
              if (ns.isAir()) ns = ModBlocks.RESIN_CLUMP.get().defaultBlockState();
              else if (ns.is(Blocks.WATER) && ns.getFluidState().isSource()) ns = ModBlocks.RESIN_CLUMP.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
              if (ns.is(ModBlocks.RESIN_CLUMP.get()) && !MultifaceBlock.hasFace(ns, opp)) {
                 level.setBlock(np, ns.setValue(MultifaceBlock.getFaceProperty(opp), true), 3);
                 return Optional.of(np);                       // STOP
              }
           }
        }
        if (++count >= 64) break;
        if (depth < 2) for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
           BlockPos np = pos.relative(dir);
           if (level.getBlockState(np).is(ModBlockTags.CREAKING_HEART_HOLDERS)) q.add(Pair.of(np, depth + 1));
        }
     }
     return Optional.empty();
  }
  ```
  This keeps native RNG order: the processor's shuffle runs first, then the neighbour-provider shuffle, as in 26.2 BlockPos.java:501-514.
- config_gate: `The Garden Awakens.resin`

### GARDEN-02  The creaking's player sensor reaches 16 blocks instead of its 32-block follow range (freeze, deactivation and targeting distance)
- severity: gameplay
- vb: VB uses the vanilla 1.21.1 sensor (CreakingAi.java:29-31 `SensorType.NEAREST_PLAYERS`); src-121/net/minecraft/world/entity/ai/sensing/PlayerSensor.java:28 `.filter(p -> entity.closerThan(p, 16.0))`, and Sensor.java:18 `ATTACK_TARGET_CONDITIONS = TargetingConditions.forCombat().range(16.0)`. Creaking.java:465 `checkCanMove` reads `MemoryModuleType.NEAREST_PLAYERS`.
- native: src-mc/net/minecraft/world/entity/ai/sensing/PlayerSensor.java:32 `.filter(player -> body.closerThan(player, this.getFollowDistance(body)))` (FOLLOW_RANGE = 32 for the creaking, Creaking.java:133), and Sensor.java:53-59 `updateTargetingConditionRanges` sets all TargetingConditions to the follow range.
- difference: Natively, any player within 32 blocks who looks at an active creaking freezes it, and the creaking deactivates only when no player is within 32. In VB a player 16–32 blocks away cannot freeze it (it keeps walking while being watched), it deactivates once players the first round6 blocks, and the visible/attackable player memory (used by StartAttacking) is limited to 16 blocks. Activation (<12 blocks, 144 sq) is unchanged.
- fix_kind: mixin-mc (net.minecraft.world.entity.ai.sensing.PlayerSensor.doTick), scoped to the VB creaking
- fix: `@Mixin(PlayerSensor.class)` with `@Inject(method="doTick", at=@At("HEAD"), cancellable=true)`. When `entity instanceof com.blackgear...creaking.Creaking c`, run the native body with `double r = c.getAttributeValue(Attributes.FOLLOW_RANGE);`: players = `level.players()` filtered by NO_SPECTATORS and `closerThan(p, r)`, sorted by distance, set NEAREST_PLAYERS. Visible = those passing `TargetingConditions.forNonCombat().range(r)` (or `.ignoreInvisibilityTesting()` when the player is the current ATTACK_TARGET); set NEAREST_VISIBLE_PLAYER to the first. Attackable = those passing `TargetingConditions.forCombat().range(r)` (the same ATTACK_TARGET exception applies); set NEAREST_VISIBLE_ATTACKABLE_PLAYER to the first. Also store the full attackable list (needed by GARDEN-04) in a field on the creaking through an accessor interface, for example `addon$setVisibleAttackablePlayers(List<Player>)`. Then `ci.cancel()`. Build new TargetingConditions per call; never mutate the static ones.
- config_gate: `The Garden Awakens.creaking`

### GARDEN-03  FIGHT activity erases ATTACK_TARGET every time the creaking freezes
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/level/entities/mob/monster/creaking/CreakingAi.java:69-78
  ```java
  brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(
     SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F), CreakingMeleeAttack.create(Creaking::canMove, 40),
     StopAttackingIfTargetInvalid.create()), MemoryModuleType.ATTACK_TARGET);
  ```
- native: src-mc/net/minecraft/world/entity/monster/creaking/CreakingAi.java:54-64: `ActivityData.create(Activity.FIGHT, 10, ImmutableList.of(...), ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)))`. This is a conditions-only overload; `memoriesToEraseWhenStopped` stays empty (ActivityData.java:56-62).
- difference: `updateActivity` (both sides) calls `useDefaultActivity()` (IDLE) on every freeze. In VB that erases ATTACK_TARGET, so after each unfreeze the creaking has to pick a target again through IDLE's `StartAttacking(NEAREST_VISIBLE_ATTACKABLE_PLAYER)`, one tick later and possibly a different, nearer player. Natively the target the creaking activated on is kept across freezes.
- fix_kind: mixin-vb (CreakingAi.initFightActivity)
- fix: `@Inject(method="initFightActivity", at=@At("HEAD"), cancellable=true)` that registers
  ```java
  brain.addActivityWithConditions(Activity.FIGHT, ImmutableList.of(
     Pair.of(10, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F)),
     Pair.of(11, CreakingMeleeAttack.create(Creaking::canMove, 40)),
     Pair.of(12, StopAttackingIfTargetInvalid.<Creaking>create(t -> !addon$stillReachable(brain, t)))),   // see GARDEN-04
     ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)));
  ```
  then `ci.cancel()`. `Brain.addActivityWithConditions` exists in 1.21.1 (src-121 Brain.java:353).
- config_gate: `The Garden Awakens.creaking`

### GARDEN-04  The creaking never drops a target that left line of sight or range
- severity: gameplay
- vb: CreakingAi.java:75 `StopAttackingIfTargetInvalid.create()`. The stop predicate is `p -> false`, so the target is erased only if it can't be attacked, is dead, is in another level, or has been unreachable for 200 ticks.
- native: CreakingAi.java:61 `StopAttackingIfTargetInvalid.create((level, target) -> !isAttackTargetStillReachable(body, target))`, and CreakingAi.java:67-70 `getMemory(NEAREST_VISIBLE_ATTACKABLE_PLAYERS).map(players -> target instanceof Player player && players.contains(player)).orElse(false)`.
- difference: Natively the creaking stops pursuing as soon as its target is not in the sensor's visible and attackable player list (out of line of sight, beyond 32 blocks, invisible, creative). VB keeps chasing a player hidden behind walls through pathfinding.
- fix_kind: mixin-vb (CreakingAi.initFightActivity); 1.21.1 lacks the plural memory (engine)
- fix: use the predicate from GARDEN-03: `addon$stillReachable(brain, t) = t instanceof Player p && creaking.addon$getVisibleAttackablePlayers().contains(p)`, filled by the PlayerSensor mixin in GARDEN-02. If GARDEN-02 is not applied, approximate with `Sensor.isEntityAttackable(creaking, p) && creaking.closerThan(p, creaking.getAttributeValue(Attributes.FOLLOW_RANGE))`.
- config_gate: `The Garden Awakens.creaking`

### GARDEN-05  Heart removed by anything other than a player or explosion does not tear down its creaking
- severity: gameplay
- vb: src-vb/.../blocks/CreakingHeartBlock.java:155-161 (onRemove): `Containers.dropContentsOnDestroy(...); if (!state.is(newState.getBlock())) { super.onRemove(...); level.updateNeighbourForOutputSignal(...); }`. There is no protector cleanup; only `playerWillDestroy` (:177) and `onExplosionHit` (:164) call `removeProtector`.
- native: src-mc/.../block/entity/CreakingHeartBlockEntity.java:312-315
  ```java
  @Override public void preRemoveSideEffects(final BlockPos pos, final BlockState state) { this.removeProtector(null); }
  ```
- difference: When a heart is removed by /setblock, /fill, structure or mod code, or replaced by another block, native calls `tearDown()` immediately: 100+10 crumble particles, death sound, discard. In VB the creaking only notices on its next tick (`Creaking.tick` :245-249 `setHealth(0)`) and then plays the vanilla 20-tick death: tip-over, red overlay, poof particles, no crumble.
- fix_kind: mixin-vb (CreakingHeartBlock.onRemove)
- fix: `@Inject(method="onRemove", at=@At("HEAD"))`: `if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart) heart.removeProtector(null);`. This is a no-op after `playerWillDestroy` and explosions because `clearCreakingInfo` has already run.
- config_gate: `The Garden Awakens.creaking`

### GARDEN-06  Creakings get pushed into boats
- severity: gameplay
- vb: no handling. 1.21.1 src-121/net/minecraft/world/entity/vehicle/Boat.java:354-361 boards any `LivingEntity` that is not a WaterAnimal or Player and fits `hasEnoughSpaceFor` (0.9 < 1.375).
- native: src-mc/.../vehicle/boat/AbstractBoat.java:283-288 `&& !entity.is(EntityTypeTags.CANNOT_BE_PUSHED_ONTO_BOATS)`; data/minecraft/tags/entity_type/cannot_be_pushed_onto_boats.json contains `"minecraft:creaking"`.
- difference: In VB, pushing a boat into a creaking traps it in the boat, a cheap trap that native prevents.
- fix_kind: mixin-vb (Creaking) + mixin-mc fallback
- fix: in the VB `Creaking` class add `@Override public boolean startRiding(Entity vehicle, boolean force)` through `@Inject(method="startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at=@At("HEAD"), cancellable=true)` on `Entity`, filtered to `this instanceof Creaking && vehicle instanceof Boat && !force` → `cir.setReturnValue(false)`. This covers vanilla Boat and VB CustomBoat, which extends Boat. Alternatively `@WrapOperation` the `instanceof` chain in `Boat.tick` with a check for a datapack tag `minecraft:cannot_be_pushed_onto_boats` shipped with `["minecraft:creaking"]`.
- config_gate: `The Garden Awakens.creaking`

### GARDEN-07  A heart-bound creaking refuses all passengers (native allows them)
- severity: gameplay
- vb: Creaking.java:347-359
  ```java
  protected boolean canAddPassenger(Entity p) { return !this.isHeartBound() && super.canAddPassenger(p); }
  protected boolean couldAcceptPassenger() { return !this.isHeartBound() && super.couldAcceptPassenger(); }
  protected void addPassenger(Entity p) { if (!this.isHeartBound()) super.addPassenger(p); }
  ```
- native: src-mc/.../creaking/Creaking.java has no passenger overrides (only `canUsePortal`, :348-351).
- difference: Natively mobs or players can be put on a heart-bound creaking (/ride, jockey-style mounting). VB blocks it.
- fix_kind: mixin-vb (Creaking.canAddPassenger / couldAcceptPassenger / addPassenger)
- fix: three `@Inject(at=@At("HEAD"), cancellable=true)` that return `super` behaviour: `cir.setReturnValue(addon$superCanAdd(p))`. Simpler: `@Redirect` the `isHeartBound()` call inside each of the three methods to return `false`.
- config_gate: none

### GARDEN-08  Creaking and heart voice sounds play at fixed pitch 1.0 (native uses the random voice pitch)
- severity: audio
- vb: Creaking.java:229 `this.playSound(ModSoundEvents.CREAKING_UNFREEZE.get())`, :232 `CREAKING_FREEZE`, :321 `this.playSound(this.getDeathSound())` (tearDown), :328 `CREAKING_TWITCH`, :504 `CREAKING_ACTIVATE`, :511 `CREAKING_DEACTIVATE`; CreakingHeartBlockEntity.java:113 `creaking.playSound(ModSoundEvents.CREAKING_SPAWN.get())`. `Entity.playSound(SoundEvent)` → volume 1, pitch 1 (src-121 Entity.java:1144-1147).
- native: Creaking.java:230/233/320/327/499/506 and CreakingHeartBlockEntity.java:119 use `this.makeSound(...)`, which is `playSound(sound, getSoundVolume(), getVoicePitch())` (LivingEntity.java:1441-1445); the voice pitch for adults is `(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F`.
- difference: Native pitch is triangular in 0.8–1.2 for freeze, unfreeze, activate, deactivate, twitch, tear-down death and spawn. VB is always 1.0.
- fix_kind: mixin-vb (Creaking.aiStep/tearDown/creakingDeathEffects/activate/deactivate; CreakingHeartBlockEntity.serverTick)
- fix: `@Redirect(method={"aiStep","tearDown","creakingDeathEffects","activate","deactivate"}, at=@At(value="INVOKE", target="Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/creaking/Creaking;playSound(Lnet/minecraft/sounds/SoundEvent;)V"))` → `c.playSound(s, 1.0F, c.getVoicePitch())` (`getSoundVolume()` is 1 and protected; use 1.0F). The same redirect in `CreakingHeartBlockEntity.serverTick` on the `Creaking.playSound(SoundEvent)` call.
- config_gate: none

### GARDEN-09  Attack sound is a client-local, pitch-1 sound fired on every swing (native: server sound only on a successful hit)
- severity: audio
- vb: Creaking.java:432-434 `this.level().playLocalSound(getX(), getY(), getZ(), ModSoundEvents.CREAKING_ATTACK.get(), getSoundSource(), 1.0F, 1.0F, false)`, called from `handleEntityEvent(4)` (:335-337). Event 4 is broadcast in `doHurtTarget` (:138) before `super.doHurtTarget`. On the server, `Mob.doHurtTarget` calls it too, but `ServerLevel.playLocalSound` does nothing.
- native: Creaking.java:420-422 `this.makeSound(SoundEvents.CREAKING_ATTACK)`. On the client (event 4) this is `level.playSound(null, …)`, which ClientLevel ignores (ClientLevel.java:695 `if (except == this.minecraft.player)`). The audible copy comes from the server in `Mob.doHurtTarget` (Mob.java:1417), only when the hit lands, with voice pitch 0.8–1.2.
- difference: VB plays the attack sound even when the hit fails (for example against an invulnerable target), always at pitch 1.0, as a non-entity-bound sound at the swing position.
- fix_kind: mixin-vb (Creaking.playAttackSound)
- fix: `@Inject(method="playAttackSound", at=@At("HEAD"), cancellable=true)`: `if (!level().isClientSide()) playSound(ModSoundEvents.CREAKING_ATTACK.get(), 1.0F, getVoicePitch()); ci.cancel();`. On the client do nothing. Keep `attackAnimationRemainingTicks = 15` in `handleEntityEvent`, which is untouched.
- config_gate: none

### GARDEN-10  Tear-down uses `block` particles instead of `block_crumble`
- severity: visual
- vb: Creaking.java:293-318 `new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.PALE_OAK_WOOD...)` ×100 and `(ParticleTypes.BLOCK, CREAKING_HEART[state=awake])` ×10, spread 0.3×bbox, speed 0.
- native: Creaking.java:294-317 `ParticleTypes.BLOCK_CRUMBLE`, same counts and spreads. Its provider (client/particle/TerrainParticle.java:98-118): `createTerrainParticle(...)`, then `setParticleSpeed(0,0,0)` and `setLifetime(random.nextInt(10) + 1)`.
- difference: VB particles get the terrain particle's random initial velocity and a long life (about 4/(r*0.9+0.1) ticks, up to ~40). Native crumble particles start with zero velocity and live 1–10 ticks.
- fix_kind: engine (1.21.1 has no BLOCK_CRUMBLE); fixable by adding a particle type
- fix: register `addon:block_crumble` as `ParticleType<BlockParticleOption>` with the `BlockParticleOption::codec`/`streamCodec` factory. Client provider: `TerrainParticle p = new TerrainParticle(level, x, y, z, 0, 0, 0, opt.getState(), opt.getPos()); p.setParticleSpeed(0,0,0); p.setLifetime(level.random.nextInt(10) + 1)`. The constructor is protected, so subclass it, or go through `ParticleProvider` of `ParticleTypes.BLOCK` and adjust. Then `@ModifyArg` / `@Redirect` the two `new BlockParticleOption(ParticleTypes.BLOCK, …)` in `Creaking.tearDown` so they use the new type.
- config_gate: none

### GARDEN-11  Eyeblossom and pale hanging moss idle sounds use the BLOCKS category (native AMBIENT)
- severity: audio
- vb: EyeblossomBlock.java:48 `playLocalSound(..., ModSoundEvents.EYEBLOSSOM_IDLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false)`; HangingMossBlock.java:46 `... PALE_HANGING_MOSS_IDLE.get(), SoundSource.BLOCKS, ...`
- native: EyeblossomBlock.java:56 `SoundEvents.EYEBLOSSOM_IDLE, SoundSource.AMBIENT`; HangingMossBlock.java:49 `SoundEvents.PALE_HANGING_MOSS_IDLE, SoundSource.AMBIENT`
- difference: Both idle sounds follow the Blocks volume slider instead of the Ambient/Environment slider. Chance (1/700, 1/500), volume and pitch match.
- fix_kind: mixin-vb (EyeblossomBlock.animateTick, HangingMossBlock.animateTick)
- fix: `@ModifyArg(method="animateTick", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"), index=4)` → `SoundSource.AMBIENT`, in both classes.
- config_gate: none

### GARDEN-12  Pale moss carpet uses the MOSS sound type (native MOSS_CARPET)
- severity: audio
- vb: ModBlocks.java:170-174 `PALE_MOSS_CARPET ... .strength(0.1F).sound(SoundType.MOSS).pushReaction(PushReaction.DESTROY).noOcclusion()`
- native: Blocks.java:5649-5658 `PALE_MOSS_CARPET ... .strength(0.1F).sound(SoundType.MOSS_CARPET)`
- difference: Break, step, place, hit and fall play `block.moss.*` instead of `block.moss_carpet.*` (both types are 1.0/1.0).
- fix_kind: mixin-vb, or set the field after registration
- fix: at common setup: `((BlockBehaviourAccessor) ModBlocks.PALE_MOSS_CARPET.get()).setSoundType(SoundType.MOSS_CARPET)`, using an accessor on `BlockBehaviour.soundType` (`@Mutable @Accessor`). Or override `getSoundType(BlockState, LevelReader, BlockPos, Entity)` (NeoForge `IBlockExtension`) with a `@Mixin(MossyCarpetBlock.class)` returning `SoundType.MOSS_CARPET`.
- config_gate: none

### GARDEN-13  Map colours of pale oak leaves, pale oak sapling and closed eyeblossom
- severity: visual
- vb: ModBlocks.java:179 `PALE_OAK_LEAVES .mapColor(MapColor.TERRACOTTA_GREEN)`; :194 `PALE_OAK_SAPLING .mapColor(MapColor.COLOR_LIGHT_GRAY)`; :203 `CLOSED_EYEBLOSSOM .mapColor(MapColor.COLOR_LIGHT_GRAY)`
- native: Blocks.java:599 `PALE_OAK_LEAVES .mapColor(MapColor.METAL)`; :257 `PALE_OAK_SAPLING .mapColor(MapColor.METAL)`; :5686 `CLOSED_EYEBLOSSOM .mapColor(PALE_OAK_LEAVES.defaultMapColor())`, which is METAL
- difference: Pale gardens show green-brown (TERRACOTTA_GREEN) canopies on maps instead of METAL light grey. The sapling and closed eyeblossom use COLOR_LIGHT_GRAY instead of METAL.
- fix_kind: mixin-mc (BlockBehaviour.BlockStateBase.getMapColor) or a properties accessor
- fix: `@Inject(method="getMapColor", at=@At("HEAD"), cancellable=true)` on `BlockBehaviour.BlockStateBase`: `if (this.is(ModBlocks.PALE_OAK_LEAVES.get()) || this.is(ModBlocks.PALE_OAK_SAPLING.get()) || this.is(ModBlocks.CLOSED_EYEBLOSSOM.get())) cir.setReturnValue(MapColor.METAL);`. The state caches mapColor at construction, so an `@Inject` into `getMapColor` is the reliable point.
- config_gate: none

### GARDEN-14  Eyeblossoms outside the overworld cycle (open ones close); native leaves them alone
- severity: gameplay
- vb: EyeblossomBlock.java:70 `boolean shouldBeOpen = WorldUtilities.EnvironmentUtils.isNaturalNight(level);`. `isNaturalNight` returns `false` when `!level.dimensionType().natural()` (WorldUtilities.java EnvironmentUtils).
- native: EyeblossomBlock.java:80 `level.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, pos).toBoolean(this.type.open)`. The attribute defaults to `TriState.DEFAULT` (EnvironmentAttributes.java:129-131); only the overworld `day` timeline sets it (true at 12600, false at 23401), so elsewhere it returns the current state.
- difference: An open eyeblossom placed in the Nether or End closes on its next random tick in VB (with the close_long sound and particles). Natively it never changes there. VB's pot (EyeblossomPotBlock) already guards with `natural()`, so only the flower differs.
- fix_kind: mixin-vb (EyeblossomBlock.tryChangingState)
- fix: `@Inject(method="tryChangingState", at=@At("HEAD"), cancellable=true)`: `if (!level.dimensionType().natural()) cir.setReturnValue(false);`
- config_gate: none

### GARDEN-15  A closed eyeblossom poisons bees (native: only bee-attractive flowers, so only the open one)
- severity: gameplay
- vb: EyeblossomBlock.java:90-93 `if (!level.isClientSide() && level.getDifficulty() != Difficulty.PEACEFUL && entity instanceof Bee bee && !bee.hasEffect(MobEffects.POISON)) bee.addEffect(new MobEffectInstance(MobEffects.POISON, 25));`
- native: EyeblossomBlock.java:104-110 `... && entity instanceof Bee bee && Bee.attractsBees(state) && !bee.hasEffect(MobEffects.POISON)`. `attractsBees` requires `#minecraft:bee_attractive` (Bee.java:672-682), which lists `minecraft:open_eyeblossom` only.
- difference: Bees flying through closed eyeblossoms get Poison (25 ticks) in VB, but not natively. Bees inside a waterlogged bee-attractive block are excluded natively too.
- fix_kind: mixin-vb (EyeblossomBlock.entityInside)
- fix: `@Inject(method="entityInside", at=@At("HEAD"), cancellable=true)`: `if (!state.is(ModBlocks.OPEN_EYEBLOSSOM.get())) ci.cancel();`
- config_gate: none

### GARDEN-16  A closed eyeblossom counts as bee food and as a pollination flower
- severity: gameplay
- vb: src-vb/data/minecraft/tags/block/small_flowers.json and tags/item/small_flowers.json add both `open_eyeblossom` and `closed_eyeblossom`. In 1.21.1, bee food is `#minecraft:flowers` (src-121 VanillaItemTagsProvider.java:438 `tag(BEE_FOOD).addTag(ItemTags.FLOWERS)`), and bee flower search uses `BlockTags.FLOWERS` (src-121 Bee.java:580, :1078).
- native: data/minecraft/tags/item/bee_food.json and tags/block/bee_attractive.json list `minecraft:open_eyeblossom` only. `small_flowers` still holds both.
- difference: In VB a closed eyeblossom breeds and tempts bees and is a valid pollination target. Natively it is neither.
- fix_kind: mixin-mc (net.minecraft.world.entity.animal.Bee)
- fix: `@Inject(method="isFood", at=@At("HEAD"), cancellable=true)` → `if (stack.is(ModBlocks.CLOSED_EYEBLOSSOM.get().asItem())) cir.setReturnValue(false);`. `@WrapOperation` the `BlockState.is(TagKey)` calls with `BlockTags.FLOWERS` in `Bee.isFlowerValid` and `Bee.BeePollinateGoal` (the `findNearbyFlower` predicate) so they also require `!state.is(ModBlocks.CLOSED_EYEBLOSSOM.get())`. The TemptGoal (src-121 Bee.java:165) uses `ItemTags.BEE_FOOD` directly; wrap that lambda the same way.
- config_gate: none

### GARDEN-17  Bone meal on pale moss shows grower particles instead of the neighbour-spreader burst
- severity: visual
- vb: PaleMossBlock.java:17-40 implements `BonemealableBlock` without overriding `getType()`, so it defaults to `GROWER` (src-121 BonemealableBlock.java:24-26).
- native: BonemealableFeaturePlacerBlock.java:52-54 `public BonemealableBlock.Type getType() { return BonemealableBlock.Type.NEIGHBOR_SPREADER; }`
- difference: The particle position moves from the block itself to `pos.above()`, and the count and spread change from `spawnParticleInBlock(data)` to `spawnParticles(data*3, spread 3.0)` (src-121 BoneMealItem.java:159-165). Vanilla 1.21.1 moss already uses NEIGHBOR_SPREADER.
- fix_kind: mixin-vb (PaleMossBlock)
- fix: `@Mixin(PaleMossBlock.class) implements BonemealableBlock` with `@Override public BonemealableBlock.Type getType() { return BonemealableBlock.Type.NEIGHBOR_SPREADER; }`. Mixin can add the interface default override because the method is not declared in the target.
- config_gate: none

### GARDEN-18  Pale hanging moss accepts bone meal when its tip is at the world bottom
- severity: gameplay
- vb: HangingMossBlock.java:83-85 `return this.canGrowInto(level.getBlockState(this.getTip(level, pos).below()));`
- native: HangingMossBlock.java:101-104 `BlockPos growPos = this.getTip(level, pos).below(); return this.canGrowInto(level.getBlockState(growPos)) && level.isInsideBuildHeight(growPos);`
- difference: Below min-Y, `getBlockState` returns VOID_AIR, which counts as air. VB consumes bone meal and shows particles without growing anything; native refuses.
- fix_kind: mixin-vb (HangingMossBlock.isValidBonemealTarget)
- fix: `@Inject(method="isValidBonemealTarget", at=@At("RETURN"), cancellable=true)`: `if (cir.getReturnValueZ() && level instanceof LevelHeightAccessor h && h.isOutsideBuildHeight(((HangingMossBlock)(Object)this).getTip(level, pos).below())) cir.setReturnValue(false);`
- config_gate: none

### GARDEN-19  SimpleBlockFeatureMixin samples the state provider twice; pale moss patches get extra bare carpets and less grass
- severity: visual
- vb: src-vb/.../mixin/common/worldgen/pale_garden/SimpleBlockFeatureMixin.java:24-41 is a HEAD inject: `BlockState state = config.toPlace().getState(context.random(), origin);`. It returns early only for MossyCarpet and Eyeblossom. Otherwise the original 1.21.1 body (src-121 SimpleBlockFeature.java:20) samples `getState(p_160341_.random(), blockpos)` again.
- native: SimpleBlockFeature.java (26.2) samples once: `config.toPlace().getOptionalState(level, context.random(), origin)`. A MossyCarpet goes through `MossyCarpetBlock.placeAt(level, origin, level.getRandom(), 2)`; `schedule_tick` is honoured.
- difference: For `pale_moss_vegetation` (weighted: carpet 25 / short grass 25 / tall grass 10), native gives carpet with placeAt (sides and topper) 41.7%, short grass 41.7%, tall grass 16.7%. VB gives carpet with placeAt 41.7%, plain default-state carpet (no wall sides or topper, set by `setBlock`) 24.3%, short grass 24.3%, tall grass 9.7%. Every other weighted simple_block feature also draws one extra random value, which shifts the rest of that feature's sequence (its distribution is unchanged).
- fix_kind: mixin-mc (SimpleBlockFeature.place), which supersedes VB's injector
- fix: `@WrapMethod(method="place")` (MixinExtras). The wrapper runs outside all injectors, so VB's HEAD inject never fires when the original is not called. Body:
  ```java
  SimpleBlockConfiguration cfg = ctx.config(); WorldGenLevel level = ctx.level(); BlockPos o = ctx.origin();
  BlockState s = cfg.toPlace().getState(ctx.random(), o);
  if (!s.canSurvive(level, o)) return false;
  if (s.getBlock() instanceof DoublePlantBlock) { if (!level.isEmptyBlock(o.above())) return false; DoublePlantBlock.placeAt(level, s, o, 2); }
  else if (s.getBlock() instanceof MossyCarpetBlock) MossyCarpetBlock.placeAt(level, o, level.getRandom(), 2);
  else level.setBlock(o, s, 2);
  if (s.getBlock() instanceof EyeblossomBlock) level.scheduleTick(o, level.getBlockState(o).getBlock(), 1);   // = native schedule_tick:true on flower_pale_garden / pale_forest_flower
  return true;
  ```
  Do not call `original.call(...)`.
- config_gate: `The Garden Awakens.pale_garden`

### GARDEN-20  Pale garden music silencing is binary at the player's block, not blended at the camera
- severity: audio
- vb: client/api/modules/music_fade/MusicFadeManager.java `isSilentBiome()` = `player.level().getBiome(player.blockPosition()).is(ModBiomes.PALE_GARDEN)`; target volume `isSilentBiome() ? 0.0F : 1.0F`. MusicManagerMixin cancels the tick (and any new song) whenever this is true, even while a screen has its own background music.
- native: pale_garden.json `"minecraft:audio/music_volume": 0.0`, `"minecraft:audio/background_music": {}`; Minecraft.java:2649-2653 `getMusicVolume()` returns 1.0 when the open screen has background music, otherwise `mainCamera().attributeProbe().getValue(MUSIC_VOLUME, 1.0F)`. The probe Gaussian-samples biomes around the camera (EnvironmentAttributeProbe.java:34-37), so the volume falls off gradually across a pale-garden border.
- difference: Natively music fades towards an intermediate volume near pale-garden edges. It is sampled at the camera, not the feet, and screen music still plays. VB is 0 or 1, by the biome at the feet. The fade constants (0.97/0.03, 5e-4..0.005, 1e-4) match.
- fix_kind: engine (no environment attributes in 1.21.1); approximate fix mixin-vb (MusicFadeManager.getTargetVolume / isSilentBiome)
- fix: replace `getTargetVolume()` with: `if (mc.screen != null && mc.screen.getBackgroundMusic() != null) return 1.0F;` (1.21.1 `Screen.getBackgroundMusic()` exists). Otherwise sample a 5×5×5 grid of quart positions around `camera.getPosition().scale(0.25)` with Gaussian weights (σ≈1 quart, as `GaussianSampler`), using `level.getBiomeManager().getNoiseBiomeAtQuart`, and return `1 - weightedFraction(pale_garden)`. Also make `isSilentBiome()` return `getTargetVolume() <= 1e-4F`.
- config_gate: `The Garden Awakens.pale_garden`

### GARDEN-21  Creaking and pale garden missing from the vanilla mob and biome advancements
- severity: data
- vb: src-vb/data/minecraft/advancement/adventure/ holds only `heart_transplanter.json` (matches native apart from icon count) and `spear_many_mobs.json`. There are no overrides of kill_a_mob, kill_all_mobs or adventuring_time, and no code adds the criteria.
- native: data/minecraft/advancement/adventure/kill_a_mob.json:74-86 and kill_all_mobs.json:74-86 have criterion `"minecraft:creaking": {"trigger":"minecraft:player_killed_entity","conditions":{"entity":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"minecraft:entity_type":"minecraft:creaking"}}]}}`. That criterion sits in the requirements (kill_a_mob: one OR group, line 597; kill_all_mobs: its own `["minecraft:creaking"]` line, 608). adventuring_time.json:532-545 has `"minecraft:pale_garden": {"trigger":"minecraft:location","conditions":{"player":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"minecraft:location":{"biomes":"minecraft:pale_garden"}}}]}}` plus requirement `["minecraft:pale_garden"]` (line 989).
- difference: Killing a creaking (heart break with player blame → `die` → `player_killed_entity`) does not count towards Monster Hunter or Monsters Hunted, and visiting a pale garden does not count towards Adventuring Time.
- fix_kind: data-override (data/minecraft/advancement/adventure/kill_a_mob.json, kill_all_mobs.json, adventuring_time.json)
- fix: ship the full 1.21.1 vanilla files, with the criteria above added and the requirement entries extended: kill_a_mob appends `"minecraft:creaking"` to its single OR array; kill_all_mobs adds `["minecraft:creaking"]`; adventuring_time adds `["minecraft:pale_garden"]`. Coordinate with other areas that add mobs or biomes to the same files (one merged override). The pale_garden criterion only works when `pale_garden` is enabled; otherwise the advancement can't be completed. Prefer adding it through a NeoForge condition (`neoforge:conditions` with a mod-config condition), or only when the config is on.
- config_gate: `The Garden Awakens.creaking` / `The Garden Awakens.pale_garden`

### GARDEN-22  Pale garden vegetal feature order (pumpkin vs sugar cane)
- severity: data
- vb: src-vb/data/minecraft/worldgen/biome/pale_garden.json step 9: `..., "minecraft:patch_grass_forest", "minecraft:patch_sugar_cane", "minecraft:patch_pumpkin"` (firefly bushes are appended by the biome modifier through `#is_forest`)
- native: pale_garden.json step 9: `..., "minecraft:patch_grass_forest", "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water"`
- difference: Per-feature decoration seeds come from the global sorted feature index, so the order changes which chunks get pumpkins and sugar cane compared with native. Densities are identical.
- fix_kind: engine (not cleanly fixable)
- fix: none. Every 1.21.1 vanilla biome uses `patch_sugar_cane` before `patch_pumpkin` (`addDefaultExtraVegetation`). Reordering only pale_garden creates a FeatureSorter cycle ("Feature order cycle found") and crashes world load. Leave as is.
- config_gate: `The Garden Awakens.pale_garden`

### GARDEN-23  Pale oak boat and chest boat item textures are the old palette
- severity: cosmetic
- vb: src-vb/assets/minecraft/textures/item/pale_oak_boat.png and pale_oak_chest_boat.png: 38 pixels differ from native each. For example (5,2) VB `(231,227,225)` vs native `(250,239,238)` and (4,3) VB `(250,239,238)` vs native `(231,227,225)` (highlight and shade swapped); the chest boat at (2,7) is VB `(48,42,42)` vs native `(66,47,11)`.
- native: src-mc/assets/minecraft/textures/item/pale_oak_boat.png, pale_oak_chest_boat.png
- difference: The 26.2 item icons have redrawn shading. All other garden textures are pixel-identical; `open_eyeblossom.png`'s 4 extra eye texels are intentional for VB's emissive overlay.
- fix_kind: asset-override (assets/minecraft/textures/item/pale_oak_boat.png, assets/minecraft/textures/item/pale_oak_chest_boat.png)
- fix: copy both PNGs from src-mc unchanged.
- config_gate: none

### GARDEN-24  Creaking spawn egg uses the tinted template, not the 26.2 texture
- severity: cosmetic
- vb: ModItems.java:100-101 `ItemRegistry.createSpawnEgg(ModEntityTypes.CREAKING, 6250335, 16545810, new Properties())`; assets/minecraft/models/item/creaking_spawn_egg.json `{"parent":"minecraft:item/template_spawn_egg"}`
- native: assets/minecraft/models/item/creaking_spawn_egg.json `{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/creaking_spawn_egg"}}` (the texture is already present and identical in VB)
- difference: VB shows the old two-tone egg, native the bespoke creaking egg.
- fix_kind: asset-override (assets/minecraft/models/item/creaking_spawn_egg.json) + client mixin
- fix: write a model with explicit elements and no `tintindex`. Do not use `item/generated`, because 1.21.1 assigns `tintindex 0` to layer0 and `ItemColors` would multiply it by 0x5F5F5F. In practice: `{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/creaking_spawn_egg"}}`, plus a client mixin on the spawn-egg `ItemColor` lambda (or `RegisterColorHandlersEvent.Item` registered after VB) returning `-1` for `ModItems.CREAKING_SPAWN_EGG`. Coordinate with whichever area handles all VB spawn eggs.
- config_gate: none
