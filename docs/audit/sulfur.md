# sulfur — Chaos Cubed (sulfur cube, sulfur caves, potent sulfur/geysers, speleothems, noxious gas, cinnabar)

**Summary:** 45 findings: gameplay 20, audio 10, visual 9, data 3, cosmetic 3.
**Covered:**
- Sulfur cube entity: AbstractCubeMob, SulfurCube, archetypes plus their data and tags, bounce/friction attribute mixins, bucket item and dispenser, loot, damage type and tags, entity-type tags.
- Potent sulfur block, state and block entity (geyser timings, launch, nausea).
- SpeleothemBlock vs native SpeleothemBlock/SulfurSpikeBlock; block properties of the sulfur and cinnabar families.
- Client: models, renderer, inner/outer layers, all 7 particles plus options and JSONs, sounds.json, every sulfur .ogg (SHA1 vs 26.2 asset index 32), textures (pixel compare), blockstates/models, lang.
- Worldgen: speleothem/cluster/root-system features, configured/placed features, biome JSON and builder, climate points, surface rules, noise, lake mixin, biome tags, the 10 spring NBTs (decoded and compared).

**Not checked:**
- TerraBlender region and surface-rule internals (jar not in the trees).
- Runtime behaviour: everything here comes from reading code, nothing was run.
- VB `TemptGoal.ForNonPathfinders`, shared with the happy ghast, is left to that area's audit.

**Reference trees added later (coordinator update), re-checked:**
- **sounds.json:** every sulfur, cinnabar, geyser and noxious entry was compared mechanically against `<mc-26.2-assets>/minecraft/sounds.json`, including volume, pitch, weight, stream, attenuation, type and subtitle. Its SHA1 9ac006d5… is identical to the copy the client pass used. The only differences are findings 27–31; everything else matches.
- **src-platform:** biome placement checked in `src-platform/com/blackgear/platform/core/mixin/common/OverworldBiomeBuilderMixin.java` (TAIL of `addBiomes`) and in `common/worldgen/placement/parameters/*`. Continentalness COAST.min..INLAND.max = [-0.19, 0.55], Erosion EROSION_5..6 = [0.45, 1.0] and Depth.UNDERGROUND = [0.2, 0.9] equal native OverworldBiomeBuilder:880-889.
- **Finding 19:** confirmed by `src-platform/.../core/helper/BlockRegistry.java:69-70` (`() -> factory.apply(properties)`, no copy). Platform `SoundRegistry.soundType` uses volume and pitch 1.0/1.0 (`SoundRegistry.java:56`).
- **src-mc263:** not needed. Every feature in this area exists in 26.2.

Config keys (CommonConfig, section "Chaos Cubed"): `has_sulfur_caves`, `has_sulfur_springs`, `do_geysers_erupt`, `has_sulfur_cubes`, `do_sulfur_cubes_explode`, `do_sulfur_cubes_damage`, `do_merchant_trade_chaos_cubed_contents`.

## Coverage

| VB class / data | native counterpart | result |
|---|---|---|
| entities/mob/monster/sulfur_cube/AbstractCubeMob | world/entity/monster/cubemob/AbstractCubeMob | findings (01, 02, 03). `dealDamage` pitch `/0.2F` vs `*0.2F` is unreachable because SulfurCube.isDealsDamage()=false |
| entities/mob/monster/sulfur_cube/SulfurCube | world/entity/monster/cubemob/SulfurCube | findings (04–09, 14, 15, 18) |
| SulfurCubeArchetype / SulfurCubeArchetypes (hardcoded) | world/entity/SulfurCubeArchetype(s) + data/minecraft/sulfur_cube_archetype/*.json | values match exactly (all 12: modifiers, buoyant, explosion 3/false/120, contact damage, knockback, sound settings); finding 16 (modifier ids) |
| core/mixin/common/attributes/EntityMixin + LivingEntityMixin (bounciness/friction/air drag) | Entity.restituteMovementAfterCollisions, LivingEntity.travelInAir/getAirDrag | finding 13; ground/air friction maths match |
| ModAttributes AIR_DRAG_MODIFIER/BOUNCINESS/FRICTION_MODIFIER | Attributes | match (ranges/defaults/syncable) |
| ModEntityTypes SULFUR_CUBE | EntityTypes.SULFUR_CUBE | match (0.49×0.49, eye 0.175, spawnDimensionsScale 2, tracking 10, MONSTER) |
| MobIntegrations spawn placement / attributes | SpawnPlacements:133 / DefaultAttributes:139 | match (TEMPT_RANGE 8 via setTemptRange(8)) |
| items/SulfurCubeBucketItem | MobBucketItem (content EMPTY) + SulfurCubeContent tooltip | finding 17; the tooltip key `entity.minecraft.sulfur_cube.content` matches |
| dispenser/SulfurCubeBucketDispenseBehavior | DispenseItemBehavior filledBucketBehavior | match |
| (none) | SulfurCubeBlockDispenseItemBehavior + DispenserBlock.getDefaultDispenseMethod + TNT behavior | finding 09 |
| loot_table/entities/sulfur_cube.json, blocks/*sulfur*, *cinnabar* | same | match (only `random_sequence` / `add:false` / `bonus_rolls:0` defaults differ) |
| damage_type/sulfur_cube_hot.json | same | match |
| tags/damage_type/sulfur_cube_with_block_immune_to | same | finding 10 |
| damage-type tag memberships of sulfur_cube_hot | burn_from_stepping, panic_environmental_causes | finding 11 |
| tags/entity_type (not_scary_for_pufferfish, monsters_that_spawn_on_peaceful) | non_controlling_rider, cannot_be_pushed_onto_boats | finding 12. Boats are covered by VB BoatMixin. VB's peaceful tag is unused in code, so it has no effect |
| tags/item sulfur_cube_archetype/*, swallowable, food | same | match after resolving. VB uses `#c:concrete(_powders)`/`#c:glazed_terracotta`/`#minecraft:logs`/`#wool` where native lists vanilla entries, so VB also accepts modded members. No overlaps |
| blocks/PotentSulfurBlock | world/level/block/PotentSulfurBlock | finding 25 (client tickers). The rest matches: validBlockState, onPlace, animateTick, triggerEvent |
| blocks/PotentSulfurState / ModBlockStateProperties | properties/PotentSulfurState | match |
| block_entities/PotentSulfurBlockEntity | block/entity/PotentSulfurBlockEntity | findings 25, 26. Countdown formulas, salt, 10/20/40-tick cadences, nausea 80 t amplifier 0 ambient+visible, ranges: match |
| tags/block causes_*_geyser_eruptions, entity_type not_affected_by_geysers | same | match |
| blocks/SpeleothemBlock (sulfur_spike) | SpeleothemBlock + SulfurSpikeBlock | findings 21–24 (+ 31 for sound) |
| ModBlocks / SharedBlockProperties sulfur + cinnabar families | Blocks:5055-5099, 5343-5356 | findings 19, 20, 30. Hardness 1.5/6, tool, instrument and map colours of the others match |
| TradeIntegrations sulfur spike | villager_trade/wandering_trader/emerald_sulfur_spike.json | match (1 emerald → 2, 5 uses, 0.05) |
| ModValueProviders / value_providers/TrapezoidInt | util/valueproviders/TrapezoidInt | match |
| client models / renderer / layers / particles / sounds / textures | see rows below | findings 27–37 |
| worldgen features / biome / surface / noise / mixins / structures | see rows below | findings 38–45 |
| client/level/model/entity/sulfur_cube/SulfurCubeModel, SmallSulfurCubeModel | client/model/monster/slime/same | match |
| client/level/renderer/entity/mob/SulfurCubeRenderer | renderer/entity/SulfurCubeRenderer | finding 37. Scale chain, squish, fuse swell/flash and shadow match |
| client/level/layer/SulfurCubeInnerLayer | layers/SulfurCubeInnerLayer + BlockModelRenderState | finding 36 |
| client/level/layer/SulfurCubeOuterLayer | LivingEntityRenderer main-model path | finding 37 |
| ModModelLayers / EntityRendering sulfur entries | ModelLayers / LayerDefinitions | match |
| particle/GeyserBaseParticle | client/particle/GeyserBaseParticle | finding 33 |
| particle/GeyserEruptionParticle, GeyserPlumeParticle, NoxiousGasCloudParticle, NoxiousGasParticle, particleoptions/* | same | match |
| particle/SulfurBubbleParticle | client/particle/SulfurBubbleParticle | finding 34 |
| particle/SulfurCubeParticle | BreakingItemParticle.SulfurCubeProvider | finding 35 |
| ModParticles sulfur entries, particles/*.json | ParticleTypes / same JSONs | match |
| ModSoundEvents / sounds.json sulfur entries | SoundEvents / 26.2 sounds.json | findings 27, 28, 29, 31 |
| ModSoundTypes SULFUR/POTENT_SULFUR/CINNABAR | SoundType | match (1.0/1.0); SULFUR_SPIKE missing → 30 |
| sounds/**/*.ogg (302 files) | asset index 32 | finding 32; all others byte-identical |
| textures (35 binary_diff PNGs) | same | match (pixel-identical) |
| blockstates / models sulfur*, cinnabar*, sulfur_spike* | same | match |
| features/SpeleothemCluster{Feature,Configuration}, Speleothem{Feature,Configuration}, SpeleothemUtils | levelgen/feature/same | match (JSON key names differ, values/defaults equal) |
| features/SulfurRootSystem{Feature,Configuration} | RootSystemFeature (`minecraft:root_system`) | finding 39 (air predicate); otherwise match |
| worldgen/features/ChaosCubedFeatures + configured_feature JSONs | CaveFeatures / MiscOverworldFeatures | findings 38, 39 |
| placements/ChaosCubedPlacements + placed_feature JSONs | CavePlacements / MiscOverworldPlacements | finding 39 |
| biomes/ChaosCubedBiomes + biome/sulfur_caves.json, ChaosCubedFeatureManager | OverworldBiomes.sulfurCaves, BiomeDefaultFeatures | findings 40–43 |
| integrations/worldgen/BiomeGeneration (climate point) via src-platform BiomePlacement + OverworldBiomeBuilderMixin (addBiomes TAIL) | OverworldBiomeBuilder.addUndergroundBiomes:880-889 | match: T/H full, C [-0.19,0.55], E [0.45,1.0], depth [0.2,0.9], W [-1.1,-0.85], offset 0. The point is appended after the vanilla list; no exact tie with other points |
| compat/terrablender/OverworldRegion, VanillaBackportTerrablender | (TerraBlender) | not verified |
| worldgen/surface/ModSurfaceRuleData + mixin NoiseBasedChunkGeneratorMixin | SurfaceRuleData.overworldLike | findings 44, 45 |
| level/worldgen/surface/* + SurfaceContextMixin | SurfaceRules noise3d condition | match |
| ModNoises / noise/sulfur_cave_gradient.json | NoiseData / same | match (-5, [1,0,1]) |
| mixin LakeFeatureMixin | LakeFeature origin offset | match (feature behaviour → 38) |
| tags/block/sulfur_spike_replaceable | sulfur_spike_replaceable_blocks | match (only the id differs) |
| biome tags with sulfur_caves | BiomeTagsProvider | match |
| structure/spring/sulfur_spring_*.nbt (10) | same | match (only DataVersion differs) |

---

## Sulfur cube entity

### SULFUR-01  Split children don't inherit effects, absorption, team, silence, gravity, fire, tags
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/AbstractCubeMob.java:184-194 (remove)
  ```java
  AbstractCubeMob cube = (AbstractCubeMob)this.getType().create(this.level());
  if (this.isPersistenceRequired()) cube.setPersistenceRequired();
  cube.setCustomName(this.getCustomName()); cube.setNoAi(this.isNoAi()); cube.setInvulnerable(this.isInvulnerable());
  this.setUpSplitCube(cube, halfSize, x, z); this.level().addFreshEntity(cube);
  ```
- native: src-mc/net/minecraft/world/entity/monster/cubemob/AbstractCubeMob.java:199-204
  ```java
  this.convertTo(this.getType(), new ConversionParams(ConversionType.SPLIT_ON_DEATH, false, false, team), EntitySpawnReason.TRIGGERED, cubeMob -> this.setUpSplitCube(...))
  ```
  ConversionType.java:64-79 drops the leash and dismounts the passenger, then convertCommon (:94-) copies:
  - absorption and every active MobEffect
  - baby flag and age (setAge/forcedAge)
  - ANGRY_AT, left-handedness, noAi, persistence
  - customNameVisible, on-fire flag, invulnerable, noGravity, portal cooldown, silent
  - entity tags, CUSTOM_NAME/CUSTOM_DATA, scoreboard team
- difference: In VB, a dying size-2 cube's two babies don't inherit potion effects (e.g. slowness, levitation), silence, no-gravity, glowing/visible name, team or command tags. In native they do.
- fix_kind: mixin-vb (AbstractCubeMob#remove)
- fix: `@Redirect(method="remove", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))`. Before calling the original, copy onto `child` from `this`:
  - `setAbsorptionAmount`
  - each `getActiveEffects()` as a `new MobEffectInstance(e)`
  - `setLeftHanded`, `setCustomNameVisible`, `setSharedFlagOnFire(isOnFire())`, `setNoGravity`, `setPortalCooldown`, `setSilent`
  - `getTags().forEach(child::addTag)`
  - if `getTeam()!=null`, `level.getScoreboard().addPlayerToTeam(child.getStringUUID(), team)`

  Do not copy age: native copies it in convertCommon, but the later afterConversion callback (setUpSplitCube → `setBaby(true)` → `setAge(-24000)`) overwrites it, so the end state equals VB's. Before the loop, also `dropLeash(true,true)` and `if (getFirstPassenger()!=null) getFirstPassenger().stopRiding()`.
- config_gate: has_sulfur_cubes

### SULFUR-02  Split offset: VB uses float division for the z row
- severity: gameplay
- vb: AbstractCubeMob.java:182-183 `float x = (i % 2.0F - 0.5F) * xzSpawnOffset; float z = (i / 2.0F - 0.5F) * xzSpawnOffset;`
- native: AbstractCubeMob.java:197-198 `float xd = (i % 2 - 0.5F) * xzCubeSpawnOffset; float zd = (i / 2 - 0.5F) * xzCubeSpawnOffset;` (integer `i / 2`)
- difference: For the 2 sulfur-cube children (i=0,1), native z offsets are (-0.5, -0.5)·w/2. VB's are (-0.5, 0.0)·w/2: the second baby is shifted +0.245 blocks in z for a size-2 cube (w=0.98).
- fix_kind: mixin-vb (AbstractCubeMob#remove)
- fix: `@ModifyArg(method="remove", at=@At(value="INVOKE", target="...AbstractCubeMob;setUpSplitCube(L...AbstractCubeMob;IFF)V"), index=3)`. Recompute: capture `i` with `@Local(ordinal=?)` (the loop int) and the offset, and return `((i / 2) - 0.5F) * xzSpawnOffset`.
- config_gate: has_sulfur_cubes

### SULFUR-03  Spawn size is chosen before AgeableMob may make the cube a baby (size-2 babies)
- severity: gameplay
- vb: AbstractCubeMob.java:292-297 `this.setSpawnSize(level, difficulty); return super.finalizeSpawn(...)`. SulfurCube.java:686-688 `this.setSize(this.isBaby() ? 1 : 2, true)`.
- native: AbstractCubeMob.java:314-316 `SpawnGroupData data = super.finalizeSpawn(...); this.setSpawnSize(level, difficulty); return data;`. SulfurCube.java:668-674 then sees the baby flag.
- difference: For the 2nd–4th cube of a natural group (2–4), AgeableMob.finalizeSpawn makes a baby with chance 0.05. In VB the size was already 2, so these spawn as babies with size-2 hitbox and health (8 HP, 0.98 box, small model). Native gives them size 1 (4 HP, 0.49). The random-call order also differs: native consumes the Mob/AgeableMob randoms first.
- fix_kind: mixin-vb (AbstractCubeMob#finalizeSpawn)
- fix: `@Inject(method="finalizeSpawn", at=@At("RETURN"))`: `if ((Object)this instanceof SulfurCube sc && sc.isBaby() && sc.getSize() != 1) sc.setSize(1, true);`. For strict RNG order, @Redirect the `setSpawnSize` call to a no-op and call `setSpawnSize` in that RETURN inject. SulfurCube's override uses no random.
- config_gate: has_sulfur_cubes

### SULFUR-04  Sprint/Knockback-enchant hits apply the full archetype knockback a second time
- severity: gameplay
- vb: SulfurCube.java:841-879 `knockback(double power, double xd, double zd)` uses `float powerMultiplier = Mth.sqrt(damage);` (:867) for every call. `pendingKnockbackSource/Damage` is set once in `hurt` (:256-257) and never cleared. In 1.21.1, Player.attack calls `knockback(f4 * 0.5F, …)` a second time (src-121 Player.java:1292-1299) when sprinting or holding a Knockback weapon, and Mob.doHurtTarget does the same.
- native: SulfurCube.java:830-860 `knockback(..., DamageSource source, float damage, boolean comesFromEffect)` with `float powerMultiplier = Mth.sqrt(damage) * (comesFromEffect ? (float)power * 0.25F : 1.0F);`. The extra hit comes from Player.java:981 / LivingEntity:2920 `causeExtraKnockback(..., getKnockback + (sprint?0.5:0), ..., true)`.
- difference: Native's second impulse is scaled by power×0.25 (sprint: 0.5×0.25 = 0.125; Knockback II sword: (2/2+0.5)×0.25 = 0.375). VB's second impulse is as strong as the first, so a sprint hit launches a block-holding cube about 2× as far. Stale `pendingKnockbackSource` also makes later non-hurt knockback calls use the archetype path.
- fix_kind: mixin-vb (SulfurCube#hurt, SulfurCube#knockback)
- fix:
  - Add `@Unique int vbp$kbCalls;`.
  - `@Inject(method="hurt", at=@At("HEAD"))` sets `vbp$kbCalls = 0`.
  - `@ModifyVariable(method="knockback", at=@At(value="STORE"), name="powerMultiplier")` returns `vbp$kbCalls++ == 0 ? v : v * (float)power * 0.25F`, with `power` captured via `@Local(argsOnly=true, ordinal=0) double power`.
  - Clear `pendingKnockbackSource` in a `tick` HEAD inject so stale sources are not reused.
- config_gate: has_sulfur_cubes

### SULFUR-05  Fuse-prime and baby-eat sounds use volume 1 / pitch 1 instead of the mob voice
- severity: audio
- vb:
  - SulfurCube.java:384 `this.playSound(SoundEvents.TNT_PRIMED);`
  - SulfurCube.java:482 `this.playSound(ModSoundEvents.SULFUR_CUBE_SMALL_EAT.get());`
  - Both use Entity.playSound(sound) → volume 1.0, pitch 1.0.
- native:
  - SulfurCube.java:347 `this.makeSound(SoundEvents.TNT_PRIMED);`
  - :446/:533 `this.makeSound(SoundEvents.SULFUR_CUBE_SMALL_EAT)`
  - LivingEntity.java:1441-1444 `playSound(sound, this.getSoundVolume(), this.getVoicePitch())`
- difference: Native volume is 0.4×size: 0.8 for a primed size-2 cube, 0.4 for an eating baby. Pitch is getVoicePitch, `(r-r)*0.2+1.0` for adults and `+1.5` for babies. VB always plays both at 1.0/1.0.
- fix_kind: mixin-vb (SulfurCube#primeTime, SulfurCube#mobInteract)
- fix: `@Redirect` both `playSound(Lnet/minecraft/sounds/SoundEvent;)V` calls in `primeTime` and `mobInteract` to `this.playSound(sound, this.getSoundVolume(), this.getVoicePitch())` (LivingEntity#makeSound exists in 1.21.1 as well).
- config_gate: none

### SULFUR-06  Releasing a bucketed cube that holds a block plays the absorb sound
- severity: audio
- vb: SulfurCube.java:214-218 (loadFromBucketTag) `ItemStack absorbedBlock = ItemStack.parse(...); if (!absorbedBlock.isEmpty()) this.equipItem(absorbedBlock);`. equipItem :540 `this.playSound(this.getAbsorbSound());`
- native: the block comes back via the SULFUR_CUBE_CONTENT component. SulfurCube.java:894-910 `applyImplicitComponent → setSulfurCubeContent → this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, ...)`, with no sound.
- difference: VB plays `entity.sulfur_cube.absorb` each time a filled bucket is emptied; native is silent apart from the bucket-empty sound.
- fix_kind: mixin-vb (SulfurCube#loadFromBucketTag)
- fix: `@Redirect(method="loadFromBucketTag", at=@At(value="INVOKE", target="...SulfurCube;equipItem(Lnet/minecraft/world/item/ItemStack;)Z"))` → `self.setItemSlotAndDropWhenKilled(EquipmentSlot.CHEST, stack.copyWithCount(1)); return true;` (CHEST is VB's slot).
- config_gate: none

### SULFUR-07  Ejected/swapped block drops 1.5×height up instead of at the passenger attachment
- severity: gameplay
- vb: SulfurCube.java:536 `this.spawnAtLocation(this.getItemBySlot(EquipmentSlot.CHEST), this.getBbHeight() * 1.5F);` and :643 `this.spawnAtLocation(itemStackToShear, this.getBbHeight() * 1.5F);`
- native: SulfurCube.java:501-503 and :607-610 `Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER); this.spawnAtLocation(level, stack, equipmentSpawnOffset);`. PASSENGER falls back to AT_HEIGHT (EntityAttachment.java:7), giving `(0, bbHeight, 0)`.
- difference: For a size-2 cube (height 0.98), native drops the block at y+0.98; VB drops it at y+1.47.
- fix_kind: mixin-vb (SulfurCube#equipItem, SulfurCube#shear)
- fix: `@ModifyArg(method={"equipItem","shear"}, at=@At(value="INVOKE", target="...SulfurCube;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"), index=1)` → `return this.getBbHeight();`
- config_gate: none

### SULFUR-08  Named cubes are always persistent in VB
- severity: gameplay
- vb: SulfurCube.java:584-586 `public boolean isPersistenceRequired() { return super.isPersistenceRequired() || this.hasCustomName(); }`
- native: no override. Only `requiresCustomPersistence()` (hasBodyItem || fromBucket) is extended (:233-236).
- difference: A cube named without a name tag (e.g. `/summon … {CustomName:…}` or a renamed spawn egg) never despawns in VB. It also forces `PersistenceRequired` on split children and into bucket NBT (VB :186-188). Native lets such a cube despawn and doesn't propagate persistence.
- fix_kind: mixin-vb (SulfurCube#isPersistenceRequired)
- fix: `@Inject(method="isPersistenceRequired", at=@At("HEAD"), cancellable=true)` → `cir.setReturnValue(((MobAccessor)this).vbp$persistenceRequired())`, which reads the Mob field `persistenceRequired`.
- config_gate: none

### SULFUR-09  Dispensers can't feed blocks into sulfur cubes (and TNT ignores cubes)
- severity: gameplay
- vb: no equivalent. VB only registers `SulfurCubeBucketDispenseBehavior` (BlockIntegrations.java:182). In 1.21.1, DispenserBlock.getDispenseMethod (src-121 DispenserBlock.java:113-115) returns `DISPENSER_REGISTRY.get(item)`, whose default is DEFAULT_BEHAVIOR (drop the item).
- native:
  - DispenserBlock.java:117-121: `else if (itemStack.is(ItemTags.SULFUR_CUBE_SWALLOWABLE)) return SulfurCubeBlockDispenseItemBehavior.INSTANCE;`
  - SulfurCubeBlockDispenseItemBehavior.java:20-28: `for (SulfurCube entity : level.getEntitiesOfClass(SulfurCube.class, new AABB(pos))) if (entity.equipItem(dispensed)) { dispensed.shrink(1); return true; }`, otherwise it falls back to dropping the item.
  - DispenseItemBehavior.java:207: the TNT behavior first tries `SulfurCubeBlockDispenseItemBehavior.dispenseBlock` before priming.
- difference: In native, a dispenser facing a sulfur cube feeds it one swallowable block (and swaps an existing one). Dispensed TNT is swallowed instead of primed. In VB, blocks are dropped as items and TNT always primes.
- fix_kind: mixin-mc (net.minecraft.world.level.block.DispenserBlock#getDispenseMethod) + mixin-vb (SulfurCube#equipItem must be made callable: it is private, so add an @Invoker)
- fix:
  - Add `class VbpSulfurCubeDispense extends DefaultDispenseItemBehavior { execute(src, stack) { BlockPos p = src.pos().relative(src.state().getValue(FACING)); for (SulfurCube c : src.level().getEntitiesOfClass(SulfurCube.class, new AABB(p))) if (((SulfurCubeInvoker)c).vbp$equipItem(stack)) { stack.shrink(1); return stack; } return super.execute(src, stack); } }`.
  - `@ModifyReturnValue(method="getDispenseMethod", at=@At("RETURN"))`: if `ret == DispenserBlock.DEFAULT_BEHAVIOR` (access via AT) and `!stack.has(DataComponents.EQUIPPABLE…)`, which in 1.21.1 means `!(stack.getItem() instanceof Equipable)`, and `stack.is(ModItemTags.SULFUR_CUBE_SWALLOWABLE)`, return the new behaviour.
  - For TNT, wrap the registered TNT behavior in `CommonSetup`: `DispenserBlock.registerBehavior(Blocks.TNT, (src, st) -> fed(src, st) ? st : old.dispense(src, st))`.
- config_gate: has_sulfur_cubes

### SULFUR-10  Immunity tag misses mace_smash, spit, fireworks, bad_respawn_point
- severity: gameplay
- vb: src-vb/data/minecraft/tags/damage_type/sulfur_cube_with_block_immune_to.json contains `"minecraft:explosion", "minecraft:player_explosion"` and no `mace_smash`, `spit` or `#is_explosion`.
- native: src-mc/data/minecraft/tags/damage_type/sulfur_cube_with_block_immune_to.json contains `"minecraft:mace_smash", "minecraft:spear", "minecraft:spit", …, "#minecraft:is_explosion"`. In 1.21.1, is_explosion = fireworks, explosion, player_explosion, bad_respawn_point (src-121 DamageTypeTagsProvider.java:77).
- difference: A cube holding a block takes damage in VB from mace smash, llama spit, firework explosions and bed/anchor explosions. In native it is immune and only knocked back (SulfurCube.hurtServer :255-260). `spear` has no 1.21.1/VB damage type.
- fix_kind: data-override (data/minecraft/tags/damage_type/sulfur_cube_with_block_immune_to.json)
- fix: `{"replace":false,"values":["minecraft:mace_smash","minecraft:spit","#minecraft:is_explosion",{"id":"minecraft:spear","required":false}]}`
- config_gate: none

### SULFUR-11  Hot cube damage isn't in burn_from_stepping / panic_environmental_causes
- severity: gameplay
- vb: src-vb/data/minecraft/tags/damage_type/ has no membership for `minecraft:sulfur_cube_hot` besides the immune tag.
- native: src-mc/data/minecraft/tags/damage_type/burn_from_stepping.json and panic_environmental_causes.json both list `"minecraft:sulfur_cube_hot"`.
- difference: Native Frost Walker boots, via the enchantment's `damage_immunity` on `#burn_from_stepping` (src-121 Enchantments.java:389), make the wearer immune to hot-cube contact damage, and animals (wolves, pandas, polar bears, armadillos) panic from it. Neither happens in VB. Both tags exist in 1.21.1.
- fix_kind: data-override (data/minecraft/tags/damage_type/burn_from_stepping.json, data/minecraft/tags/damage_type/panic_environmental_causes.json)
- fix: each file `{"replace":false,"values":["minecraft:sulfur_cube_hot"]}`
- config_gate: do_sulfur_cubes_damage (only matters when contact damage is on)

### SULFUR-12  sulfur_cube missing from non_controlling_rider
- severity: gameplay
- vb: src-vb/data/minecraft/tags/entity_type/ has no `non_controlling_rider.json`. The 1.21.1 vanilla tag is `slime, magma_cube` (src-121 EntityTypeTagsProvider.java:117).
- native: src-mc/data/minecraft/tags/entity_type/non_controlling_rider.json `["minecraft:slime","minecraft:magma_cube","minecraft:sulfur_cube"]`. It is used by Entity.java:2689 `canControlVehicle() { return !this.is(NON_CONTROLLING_RIDER); }`.
- difference: In VB a sulfur cube riding a mob (e.g. a strider or another mount) can become its controlling passenger; in native it can't.
- fix_kind: data-override (data/minecraft/tags/entity_type/non_controlling_rider.json)
- fix: `{"replace":false,"values":["minecraft:sulfur_cube"]}`
- config_gate: none

### SULFUR-13  Bounce physics: slime/bed bounciness is ignored, ground friction damps vertical bounce, block suppression also kills wall bounces
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/core/mixin/common/attributes/EntityMixin.java
  - :114-115 `boolean suppressBlock = effectState.is(ModBlockTags.SUPPRESSES_BOUNCE); if (!suppressEntity && !suppressBlock) {`
  - :134-139 `validImpact = !this.verticalCollisionBelow || -currentMovement.y > g; … newY = (gravityCompensation - currentMovement.y) * effectiveDrag * restitution;` with restitution being the entity attribute only
  - :163-173 vb$getAirDrag for omnidirectional movers: `float friction = living.onGround() ? vb$computeFriction(blockFriction, FRICTION_MODIFIER) : 1.0F; return friction * vb$computeFriction(0.91F, airDragModifier);`
  - :96 the vanilla horizontal velocity reset is skipped for every bouncy entity
- native: src-mc/net/minecraft/world/entity/Entity.java:813-853
  - `restitution = isSuppressingBounce() ? 0 : getEntityBounciness();` applies to x/z
  - floor: `restitution = !(-y < g) && !isSuppressingBounce() && !effectState.is(SUPPRESSES_BOUNCE) ? Math.max(restitution, getBlockBounciness(block)) : 0.0;`
  - `effectiveDrag = Mth.lerp(portion, 1.0, getAirDrag())`
  - LivingEntity.java:2501-2503 `getAirDrag() = computeModifiedFriction(omni ? 0.91F : 0.98F, AIR_DRAG_MODIFIER)`, with no ground friction
  - Blocks: slime `bounceRestitution(1.0F)` (Blocks.java:2926), beds `bounceRestitution(0.75F)` (Blocks.java:684)
- difference:
  - (a) On slime or bed, a block-holding cube bounces with max(attribute, 1.0 / 0.75) natively. In VB it uses its attribute (0.0–1.0): e.g. regular 0.5, sticky 0. VB also overwrites the vanilla slime/bed `updateEntityAfterFallOn` result.
  - (b) VB multiplies the floor-bounce drag by the ground friction term. For a regular cube on stone that is 1-(0.4×0.3)=0.88, so bounces are lower than native.
  - (c) On honey (SUPPRESSES_BOUNCE), VB skips the whole restitution and has also skipped the vanilla x/z reset, so the cube keeps pushing into walls. Native still bounces horizontally and zeroes only the floor bounce.
  - (d) Edge case: VB uses `>` where native uses `>=`.
- fix_kind: mixin-vb (core.mixin.common.attributes.EntityMixin#vb$restituteMovementAfterCollisions, #vb$getAirDrag)
- fix: Replace the body of `vb$restituteMovementAfterCollisions` (`@Inject` HEAD on the VB handler via `@TargetHandler`, or re-implement in our own `@Inject` at the same point and cancel VB's by making `vb$canBounce` false):
  ```java
  double r = suppressEntity ? 0 : attr;
  Vec3 c = velocityBeforeCollide;
  double nx = xCol ? -c.x*r : c.x, nz = zCol ? -c.z*r : c.z, ny = c.y;
  boolean b = r > 0 && (xCol || zCol);
  if (verticalCollision) {
     if (verticalCollisionBelow) {
        double br = block == SLIME_BLOCK ? 1.0 : block instanceof BedBlock ? 0.75 : 0.0;
        r = (-c.y >= g && !suppressEntity && !effectState.is(SUPPRESSES_BOUNCE)) ? Math.max(r, br) : 0;
     }
     if (r > 0) {
        double p = movement.y / c.y;
        ny = (p*g - c.y) * Mth.lerp(p, 1, computeFriction(omni ? 0.91F : 0.98F, airDragMod)) * r;
        b = true;
     } else ny = 0;
  }
  setDeltaMovement(nx, ny, nz);
  if (b) gameEvent(HIT_GROUND);
  ```
- config_gate: has_sulfur_cubes

### SULFUR-14  Shearing a cube doesn't fire the player_sheared_equipment trigger
- severity: data
- vb: SulfurCube.java:504-512 (mobInteract shear) `this.shear(SoundSource.PLAYERS); this.gameEvent(GameEvent.SHEAR, player); heldItem.hurtAndBreak(...)`. There is no criterion.
- native: SulfurCube.java:460 `CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayer)player, itemStackToShear, this);`
- difference: Native advancement triggers fire when a block is sheared off; VB fires none. The trigger type does not exist in 1.21.1 (src-121 CriteriaTriggers has no PLAYER_SHEARED_EQUIPMENT).
- fix_kind: engine
- fix: Not cleanly fixable. The closest faithful fix is to register an add-on criterion `minecraft:player_sheared_equipment` with the 26.2 codec (player, item, entity predicates) and fire it from an @Inject after `shear` in `SulfurCube#mobInteract`. Only worth it if 26.2 advancements are ported too.
- config_gate: none

### SULFUR-15  Explosion gate is a VB config, not the tntExplodes gamerule; different disabled message
- severity: cosmetic
- vb:
  - SulfurCube.java:340 and :377 `VanillaBackport.COMMON_CONFIG.doSulfurCubesExplode.get()`
  - :490-491 `player.displayClientMessage(Component.translatable("entity.minecraft.sulfur_cube.explosion_disabled"), true);`
- native:
  - SulfurCube.java:302 and :340 `level.getGameRules().get(GameRules.TNT_EXPLODES)`
  - :475-476 `player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));`
- difference: 1.21.1 has no `tntExplodes` gamerule, so VB substitutes a config key with its own message string. Behaviour matches when the config is on.
- fix_kind: engine
- fix: none needed for parity while `do_sulfur_cubes_explode=true`. If the add-on backports a `tntExplodes` gamerule for TNT, also redirect these two config reads to it and the message key to `block.minecraft.tnt.disabled`.
- config_gate: do_sulfur_cubes_explode

### SULFUR-16  Archetype attribute-modifier ids include the namespace
- severity: cosmetic
- vb: SulfurCubeArchetype.java:67 and :82 `archetype.location().getPath() + "_add_" + attributeLocation.getNamespace() + "_" + attributeLocation.getPath()`, giving e.g. `minecraft:regular_add_minecraft_knockback_resistance`.
- native: SulfurCubeArchetype.java (AttributeEntry.add/multiply) `archetype.identifier().getPath() + "_add_" + attribute.unwrapKey().get().identifier().getPath()`; data/minecraft/sulfur_cube_archetype/regular.json `"id":"minecraft:regular_add_knockback_resistance"`.
- difference: Same amounts and operations, but the modifier ids differ. They are visible in `/attribute … modifier` and in saved transient state, and break commands or datapacks that target the native ids.
- fix_kind: mixin-vb (SulfurCubeArchetype$AttributeEntry#add, #multiply)
- fix: `@ModifyArg` on the `ResourceLocation.withDefaultNamespace(String)` call in both methods → `s.replace("_add_minecraft_","_add_").replace("_mul_minecraft_","_mul_")`
- config_gate: none

### SULFUR-17  Emptying the sulfur-cube bucket fires placed_block; creative doesn't add an empty bucket
- severity: data
- vb: SulfurCubeBucketItem.java:62-67 `if (player instanceof ServerPlayer serverPlayer) { CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, stack); } … return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(stack, player), …)`
- native: BucketItem.java:59-64 `if (player instanceof ServerPlayer serverPlayer && this.content != Fluids.EMPTY) CriteriaTriggers.PLACED_BLOCK.trigger(...)`; `ItemUtils.createFilledResult(itemStack, player, getEmptySuccessItem(itemStack, player))`
- difference: VB fires `placed_block` for a mob bucket with no fluid; native doesn't. In creative, native also puts an empty bucket into the inventory (createFilledResult) when none is present; VB doesn't.
- fix_kind: mixin-vb (SulfurCubeBucketItem#use)
- fix: `@Redirect` the `PlacedBlockTrigger.trigger` call in `use` to a no-op. `@ModifyArg` on `InteractionResultHolder.sidedSuccess` arg 0 → `ItemUtils.createFilledResult(stack, player, original)`.
- config_gate: none

### SULFUR-18  Swallowed block is stored in CHEST instead of BODY
- severity: cosmetic
- vb: SulfurCube.java:305 `return !this.getItemBySlot(EquipmentSlot.CHEST).isEmpty();` (every slot access uses CHEST)
- native: SulfurCube.java:268 `EquipmentSlot.BODY`, plus `canUseSlot(BODY)` = alive && !baby (:630-632), `isEquippableInSlot`, `getEquipmentSlotForItem` (:542-549), `canDispenserEquipIntoSlot(BODY)` (:635-637)
- difference: The block shows up as `armor.chest` / `ArmorItems[2]` instead of `armor.body` / `body_armor_item`. `/item replace entity @e armor.body` does nothing on a VB cube. Armour-slot mechanics see a chest item. The NBT layout is incompatible with native saves. Gameplay is otherwise the same.
- fix_kind: engine
- fix: Not worth fixing in an add-on: every VB access would need redirecting and existing VB saves migrating. Document only.
- config_gate: none

## Blocks

### SULFUR-19  Every sulfur building block plays potent-sulfur sounds (shared Properties mutated)
- severity: audio
- vb:
  - src-vb/com/blackgear/vanillabackport/common/level/blocks/properties/SharedBlockProperties.java:36-41 `public static final Properties SULFUR = Properties.of().sound(ModSoundTypes.SULFUR)…`
  - src-vb/com/blackgear/vanillabackport/common/registries/blocks/ModBlocks.java:566-567 `REGISTRIES.register("potent_sulfur", PotentSulfurBlock::new, SharedBlockProperties.SULFUR.sound(ModSoundTypes.POTENT_SULFUR))`
  - 1.21.1 `Properties.sound` mutates and returns `this` (src-121 BlockBehaviour.java:1185-1187).
  - Platform BlockRegistry.register(String, Function, Properties) defers construction and passes the same instance (checked with javap on Platform-neoforge-1.21.1-1.4.2.jar).
  - The block copies `soundType` at construction (src-121 BlockBehaviour.java:113).
- native: Blocks.java:5055-5078. SULFUR `.sound(SoundType.SULFUR)`; POTENT_SULFUR `ofFullCopy(SULFUR).sound(SoundType.POTENT_SULFUR)`; polished/bricks/chiseled/slab/stairs/wall copy SULFUR's SoundType.SULFUR.
- difference: ModBlocks' static initializer runs the `.sound(POTENT_SULFUR)` call before any block is built. All 13 blocks using SharedBlockProperties.SULFUR then get POTENT_SULFUR sounds (block/potent_sulfur/*): sulfur, its slab/stairs/wall, polished sulfur ×4, sulfur bricks ×4 and chiseled sulfur. Native plays block/sulfur/*. Cinnabar is unaffected.
- fix_kind: mixin-vb (ModBlocks static init)
- fix: `@Redirect(method="<clinit>", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;sound(Lnet/minecraft/world/level/block/SoundType;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;", ordinal=N))` targeting the potent-sulfur call. Return `BlockBehaviour.Properties.ofFullCopy`-equivalent copy instead: build a fresh `Properties.of().sound(ModSoundTypes.POTENT_SULFUR).mapColor(MapColor.GOLD).instrument(BASEDRUM).requiresCorrectToolForDrops().strength(1.5F,6.0F)`, which also fixes finding 20. Fallback if the ordinal is fragile: in FMLCommonSetup reflectively set `BlockBehaviour.soundType` (AT) of those 13 blocks to `ModSoundTypes.SULFUR`.
- config_gate: none

### SULFUR-20  Potent sulfur map colour is yellow instead of gold
- severity: visual
- vb: ModBlocks.java:566-567 uses `SharedBlockProperties.SULFUR` (`.mapColor(MapColor.COLOR_YELLOW)`, SharedBlockProperties.java:38) with no override.
- native: Blocks.java:5064-5065 `ofFullCopy(SULFUR).sound(SoundType.POTENT_SULFUR).mapColor(MapColor.GOLD)`
- difference: On maps potent sulfur shows as COLOR_YELLOW (id 18); native uses GOLD (id 30).
- fix_kind: mixin-vb (ModBlocks static init), handled by the same redirect as finding 19
- fix: see 19 (`.mapColor(MapColor.GOLD)` on the fresh Properties). Alternatively @Inject into `PotentSulfurBlock.<init>` TAIL setting the `BlockBehaviour.properties.mapColor` via AT, but map colour is read from the state cache at construction, so prefer the Properties fix.
- config_gate: none

### SULFUR-21  Sulfur spikes grow up to 7 long; native caps growth at 2
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/level/blocks/SpeleothemBlock.java:261 `BlockPos stalactiteTipPos = findTip(state, level, pos, 7, false);`
- native: SulfurSpikeBlock.java:9 `private static int MAX_GROWING_LENGTH = 2;`, :29-31 `getMaxGrowthLength() { return MAX_GROWING_LENGTH; }`; SpeleothemBlock.java:306 `findTip(stalactiteStartState, level, stalactiteStartPos, this.getMaxGrowthLength(), false)`
- difference: With maxSteps 2, the native search loop (`for i=1; i<maxSteps`) only looks 1 block below the start, so a sulfur stalactite stops growing once it is 2 long. VB searches 6 blocks, so it keeps growing to 7, as dripstone does.
- fix_kind: mixin-vb (SpeleothemBlock#tryGrowStalactiteOrStalagmiteIfPossible)
- fix: `@ModifyConstant(method="tryGrowStalactiteOrStalagmiteIfPossible", constant=@Constant(intValue=7))` → `return 2;` (VB's SpeleothemBlock is only used by sulfur_spike).
- config_gate: none

### SULFUR-22  Stalagmite growth scan stops after one block (inverted condition)
- severity: gameplay
- vb: SpeleothemBlock.java:384-386 `if (!this.blocksStalagmiteScan(level, pos, state)) { return; }` with `blocksStalagmiteScan` returning `false` (:390-392)
- native: SpeleothemBlock.java:435-437 `if (this.blocksStalagmiteScan(level, pos, state)) { return; }` (base also returns false, so the loop runs 10 steps)
- difference: Native looks up to 10 blocks below a dripping sulfur stalactite for floor or a stalagmite tip. VB returns after the first block, so stalagmites only grow when the floor is directly 2 blocks below the tip.
- fix_kind: mixin-vb (SpeleothemBlock#growStalagmiteBelow)
- fix: `@Redirect(method="growStalagmiteBelow", at=@At(value="INVOKE", target="…SpeleothemBlock;blocksStalagmiteScan(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))` → `return true;`. The negated value is then false, so the loop continues.
- config_gate: none

### SULFUR-23  Tridents break sulfur spikes even when projectilesCanBreakBlocks is off
- severity: gameplay
- vb: SpeleothemBlock.java:161 `if (projectile.mayInteract(level, blockPos) && projectile instanceof ThrownTrident && projectile.getDeltaMovement().length() > 0.6)`
- native: SpeleothemBlock.java:190-194 `… && projectile.mayInteract(serverLevel, blockPos) && projectile.mayBreak(serverLevel) && projectile instanceof ThrownTrident && …`. The same check is in 1.21.1 PointedDripstoneBlock (src-121 :143-146).
- difference: VB omits `mayBreak`, which checks `#impact_projectiles` and the `projectilesCanBreakBlocks` gamerule.
- fix_kind: mixin-vb (SpeleothemBlock#onProjectileHit)
- fix: `@Inject(method="onProjectileHit", at=@At("HEAD"), cancellable=true)` → `if (!projectile.mayBreak(level)) ci.cancel();`
- config_gate: none

### SULFUR-24  Spike tips merge with an opposing pointed dripstone tip
- severity: visual
- vb: SpeleothemBlock.java:130-144 `if (!isSpeleothemWithDirection(inFrontState, direction)) {...} else { return !merge && … ? TIP : TIP_MERGE; }`, where isSpeleothemWithDirection is tag `#speleothems` (dripstone + spike)
- native: SpeleothemBlock.java:159-162 `if (isSpeleothemWithDirection(inFrontState, baseDirection) && inFrontState.is(this)) { … TIP_MERGE }`
- difference: A sulfur spike facing an opposite dripstone tip becomes TIP_MERGE in VB. Native keeps it TIP, since it merges only with the same block.
- fix_kind: mixin-vb (SpeleothemBlock#calculateSpeleothemThickness)
- fix: `@Inject(method="calculateSpeleothemThickness", at=@At("HEAD"), cancellable=true)`: `BlockState f = level.getBlockState(pos.relative(tip)); if (f.is(ModBlockTags.SPELEOTHEMS) && f.getValue(TIP_DIRECTION) == tip.getOpposite() && !f.is((Block)(Object)this)) cir.setReturnValue(DripstoneThickness.TIP);`. The native fall-through for an opposite-direction neighbour is `!isSpeleothemWithDirection(front, tip)` → TIP.
- config_gate: none

### SULFUR-25  Geyser launch: server-only, uses hurtMarked, and launches the root vehicle for passengers
- severity: gameplay
- vb:
  - PotentSulfurBlockEntity.java:125-134:
    ```java
    Entity entityToBeLaunched = target.isPassenger() ? target.getRootVehicle() : target; … entityToBeLaunched.addDeltaMovement(new Vec3(0.0, 0.2F, 0.0)); entityToBeLaunched.hurtMarked = true;
    ```
  - PotentSulfurBlock.java:157-165: the client tickers for ERUPTING/CONTINUOUS are only `CLIENT_GEYSER_PLUME_TICKER`; `SERVER_LAUNCH_ENTITY_TICKER` runs on the server only.
- native:
  - PotentSulfurBlockEntity.java:122-131:
    ```java
    for (Entity entityToBeLaunched : …) { … if (entityToBeLaunched.canSimulateMovement() && … && !entityToBeLaunched.isPassenger() && …) { addDeltaMovement(0,0.2F,0); entityToBeLaunched.needsSync = true; } }
    ```
  - PotentSulfurBlock.java:159-168: the client runs `CLIENT_GEYSER_PLUME_TICKER.apply(..).andThen(LAUNCH_ENTITY_TICKER)`.
  - needsSync is 1.21.1 `hasImpulse` (26.2 ServerEntity:126/178 vs src-121 ServerEntity:121/174).
- difference:
  - Native predicts the launch on the client for entities it simulates (the local player and player-driven vehicles) and syncs others with a normal impulse.
  - VB applies the launch only on the server and forces a SetEntityMotion packet to every player inside each tick (hurtMarked). That overwrites the client's own velocity with the server's stale value, so launches are laggy and jittery.
  - VB launches a boat or minecart when only its passenger is in the column, and launches it once per rider plus once for itself in the same tick. Native launches only entities that are not passengers and are inside the column.
- fix_kind: mixin-vb (PotentSulfurBlockEntity SERVER_LAUNCH_ENTITY_TICKER lambda, PotentSulfurBlock#getTicker)
- fix:
  - (1) `@Redirect` the `Entity.getRootVehicle()` call in the launch lambda → return `target`. The `!isPassenger()` check then skips riders as native does.
  - (2) `@Redirect(PUTFIELD hurtMarked)` → `e.hasImpulse = true`.
  - (3) Skip on the server when `e.isControlledByLocalInstance()` is false for the server: `e instanceof Player` is fine to keep, since native Player.canSimulateMovement is true on the server; skip `e.getControllingPassenger() instanceof Player`.
  - (4) `@ModifyReturnValue(method="getTicker")`: when `level.isClientSide()` and the state is ERUPTING or CONTINUOUS, return `PotentSulfurBlockEntity.sequence(ret, VBP_CLIENT_LAUNCH)`. VBP_CLIENT_LAUNCH copies the launch loop and only applies to `e.isControlledByLocalInstance()` (the 1.21.1 analogue of canSimulateMovement on the client).
- config_gate: do_geysers_erupt

### SULFUR-26  Noxious-gas line of sight uses the VISUAL shape (reaches through glass, ice, slime)
- severity: gameplay
- vb: PotentSulfurBlockEntity.java:258 `level.clip(new ClipContext(from, to, Block.VISUAL, Fluid.NONE, CollisionContext.empty()))`
- native: PotentSulfurBlockEntity.java:249 `level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))`
- difference: Glass, ice, slime and honey blocks (HalfTransparentBlock/TransparentBlock) have an empty visual shape. In VB nausea therefore passes through them under water; native blocks it.
- fix_kind: mixin-vb (PotentSulfurBlockEntity#haveLineOfSight)
- fix: `@ModifyArg(method="haveLineOfSight", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/ClipContext;<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;Lnet/minecraft/world/phys/shapes/CollisionContext;)V"), index=2)` → `ClipContext.Block.COLLIDER`
- config_gate: none

## Client: sounds, particles, rendering

The native 26.2 `sounds.json` and asset hashes come from asset index 32. Its SHA1 c12254a5… matches the `assetIndex.sha1` of `.minecraft/versions/26.2/26.2.json`, and sounds.json's SHA1 9ac006d5… matches the index.

### SULFUR-27  Continuous-geyser start sound has no sounds.json entry (silent)
- severity: audio
- vb:
  - src-vb/com/blackgear/vanillabackport/client/registries/ModSoundEvents.java:70 `GEYSER_CONTINUOUS_START = REGISTRIES.soundEvent("block.potent_sulfur.geyser_continuous_eruption_start")`, played by PotentSulfurBlock.java:101.
  - src-vb/assets/minecraft/sounds.json:1962 only defines `"block.potent_sulfur.geyser_continuous_eruption"`.
- native: src-mc/net/minecraft/sounds/SoundEvents.java:1861 `GEYSER_CONTINUOUS_START = register("block.potent_sulfur.geyser_continuous_eruption")`. 26.2 sounds.json: `{"sounds":["block/potent_sulfur/geyser_continuous/eruption1".."eruption6"],"subtitle":"subtitles.block.potent_sulfur.geyser_eruption"}`
- difference: When a continuous eruption starts, VB plays nothing and shows no subtitle (unknown sound event). Native plays one of 6 eruption clips.
- fix_kind: asset-override (assets/minecraft/sounds.json)
- fix: add `"block.potent_sulfur.geyser_continuous_eruption_start": {"sounds":["block/potent_sulfur/geyser_continuous/eruption1","block/potent_sulfur/geyser_continuous/eruption2","block/potent_sulfur/geyser_continuous/eruption3","block/potent_sulfur/geyser_continuous/eruption4","block/potent_sulfur/geyser_continuous/eruption5","block/potent_sulfur/geyser_continuous/eruption6"],"subtitle":"subtitles.block.potent_sulfur.geyser_eruption"}`. sounds.json files merge, and the .ogg files already ship in VB.
- config_gate: none

### SULFUR-28  Periodic geyser eruption_active loop at volume 1.0 instead of 2.0
- severity: audio
- vb: src-vb/assets/minecraft/sounds.json:1950 `"block.potent_sulfur.geyser_eruption_active": {"sounds": ["block/potent_sulfur/sulfur_spring/eruption_active1", … "eruption_active8"]}` (plain strings, volume 1.0)
- native: 26.2 sounds.json `"block.potent_sulfur.geyser_eruption_active": {"sounds":[{"name":"block/potent_sulfur/sulfur_spring/eruption_active1","volume":2.0}, … ×8]}`
- difference: VB uses half the gain, and an attenuation range of 16 blocks instead of 32 (16×max(vol,1)). The continuous variant matches.
- fix_kind: asset-override (assets/minecraft/sounds.json)
- fix: redefine the entry with `"replace": true` and the 8 sounds as `{"name":"block/potent_sulfur/sulfur_spring/eruption_activeN","volume":2.0}`.
- config_gate: none

### SULFUR-29  Fall sounds of sulfur, potent sulfur, cinnabar have no subtitle
- severity: audio
- vb: src-vb/assets/minecraft/sounds.json:1835 `block.sulfur.fall`, :1895 `block.potent_sulfur.fall`, :2042 `block.cinnabar.fall`. Each has only `"sounds":[…/hitN]`, with no `subtitle`.
- native: 26.2 sounds.json, same three events: `…,"subtitle":"subtitles.block.generic.fall"`
- difference: No "Something falls" subtitle in VB. The sound lists are identical.
- fix_kind: asset-override (assets/minecraft/sounds.json)
- fix: redefine the three with `"replace": true` plus `"subtitle":"subtitles.block.generic.fall"` (sound lists hit1–hit7 of block/sulfur, block/potent_sulfur, block/cinnabar).
- config_gate: none

### SULFUR-30  Sulfur spike uses dripstone sounds instead of SoundType.SULFUR_SPIKE
- severity: audio
- vb: src-vb/com/blackgear/vanillabackport/common/registries/blocks/ModBlocks.java:578 `.sound(SoundType.POINTED_DRIPSTONE)`. No `block.sulfur_spike.*` events are registered.
- native: Blocks.java:5351 `.sound(SoundType.SULFUR_SPIKE)`; SoundType.java:832 `new SoundType(1.0F, 1.0F, SULFUR_SPIKE_BREAK, SULFUR_SPIKE_STEP, SULFUR_SPIKE_PLACE, SULFUR_SPIKE_HIT, SULFUR_SPIKE_FALL)`. Per 26.2 sounds.json:
  - break → block/sulfur/break1–7
  - step → block/sulfur/step1–7
  - place → block/sulfur/place1–8
  - hit and fall → block/sulfur/hit1–7
  - subtitles: generic break/footsteps/place/hit/fall
- difference: VB plays block/dripstone/*; native plays the sulfur sound set.
- fix_kind: mixin-vb + asset-override (assets/minecraft/sounds.json)
- fix: Register `block.sulfur_spike.{break,step,place,hit,fall}` in the add-on and add their sounds.json entries as above. Build `new SoundType(1F,1F,break,step,place,hit,fall)`. Then either @ModifyArg the `.sound(...)` call in the ModBlocks SULFUR_SPIKE supplier lambda, or override `getSoundType` via a mixin into VB SpeleothemBlock that returns it.
- config_gate: none

### SULFUR-31  Falling sulfur spike plays the dripstone landing sound
- severity: audio
- vb: src-vb/com/blackgear/vanillabackport/common/level/blocks/SpeleothemBlock.java:212 `level.levelEvent(1045, pos, 0);` (1045 = POINTED_DRIPSTONE_LAND)
- native: SulfurSpikeBlock.java:24-26 `getStalactiteLandingSound() { return 1052; }`; LevelEventHandler.java:236 `case 1052: playLocalSound(pos, SoundEvents.SULFUR_SPIKE_LAND, BLOCKS, 2.0F, random.nextFloat() * 0.1F + 0.9F, false)`; sounds.json `block.sulfur_spike.land` = block/sulfur_spike/land1–7, subtitle `subtitles.block.generic.break`.
- difference: VB uses the dripstone clips. Native has 7 dedicated clips that are not in VB's jar (asset hashes caceb7f5…, 7c43ce22…, bb328a37…, b788155b…, 9d3dd032…, 26901c1c…, 77e8f9b3…). Volume and pitch are the same.
- fix_kind: mixin-vb + asset-override (sounds/block/sulfur_spike/land1–7.ogg, sounds.json)
- fix: @Redirect the `levelEvent(1045, …)` in VB SpeleothemBlock#onBrokenAfterFall. Play `block.sulfur_spike.land` at 2.0F volume and `rand*0.1+0.9` pitch on BLOCKS, server-side with `level.playSound(null, pos, …)`. Ship the 7 .ogg files plus the sounds.json entry; the files come from Mojang's asset store, so check redistribution or fetch them at runtime.
- config_gate: none

### SULFUR-32  bouncy_hit1–4.ogg are different recordings
- severity: audio
- vb: src-vb/assets/minecraft/sounds/mob/sulfur_cube/ball/bouncy_hit{1..4}.ogg. Sizes are 8524/9299/7803/7876 B; sha1 10816080…, 5261d4d7…, 7e9ae9fe…, d4548054…. They are used by sounds.json:1231 `entity.sulfur_cube.bouncy.hit`.
- native: asset index 32: bouncy_hit1 0a0478aa… (7959 B), bouncy_hit2 17caf22c… (8404 B), bouncy_hit3 e1f69140… (7342 B), bouncy_hit4 da2a0a45… (7213 B)
- difference: The hit clips for a cube holding a bouncy block (planks/logs) differ, probably an older cut. The other 298 VB sulfur-related .ogg files are byte-identical to 26.2.
- fix_kind: asset-override (assets/minecraft/sounds/mob/sulfur_cube/ball/bouncy_hit1–4.ogg)
- fix: replace the four files with the 26.2 objects for those hashes (same redistribution caveat as 31).
- config_gate: none

### SULFUR-33  Geyser base/poof burst impulse formula
- severity: visual
- vb: src-vb/com/blackgear/vanillabackport/client/level/particle/GeyserBaseParticle.java:37 `float burstImpulse = type.burstImpulseBase() + 0.25F + type.waterBlocks();`
- native: src-mc/net/minecraft/client/particle/GeyserBaseParticle.java:28 `float burstImpulse = burstImpulseBase + 0.25F * waterBlocks;`
- difference: VB computes base + 0.25 + w; native computes base + 0.25·w. For w=1, base particles get 2.75 vs 1.75 and poof gets 3.25 vs 2.25. For w=4, base gets 5.75 vs 2.5. The value scales the random initial velocity on all axes, so the base and poof clouds spread far too wide. Size (3 + 0.125·w) matches.
- fix_kind: mixin-vb (GeyserBaseParticle$Provider#createParticle)
- fix: `@ModifyVariable(method="createParticle", at=@At("STORE"), ordinal=0)` on `burstImpulse` → `type.burstImpulseBase() + 0.25F * type.waterBlocks()`.
- config_gate: none

### SULFUR-34  Sulfur bubbles start with a random velocity
- severity: visual
- vb: src-vb/com/blackgear/vanillabackport/client/level/particle/SulfurBubbleParticle.java:23 `super(level, x, y, z, xSpeed, ySpeed, zSpeed);`. That is the 7-arg Particle ctor, which randomizes xd/yd/zd (src-121 Particle.java:63-72): yd ≈ norm·d0·0.4 + 0.1, roughly in [-0.08, 0.28].
- native: src-mc/net/minecraft/client/particle/SulfurBubbleParticle.java:23 `super(level, x, y, z, sprite);` (4-arg base, so xd=yd=zd=0 before the explicit xd/zd)
- difference: Native bubbles start at yd=0 and rise from gravity -0.04 alone. VB bubbles start with up to about 0.28 upward speed. Those with yd < -0.04 move down on tick 1 and are removed by the `y <= yPrev` check.
- fix_kind: mixin-vb (SulfurBubbleParticle#<init>)
- fix: `@Inject(method="<init>", at=@At("TAIL"))` → `this.yd = 0.0;` (Particle accessor if needed; xd and zd are already overwritten).
- config_gate: none

### SULFUR-35  Sulfur cube goo particles move at a tenth of native speed
- severity: visual
- vb: src-vb/com/blackgear/vanillabackport/client/level/particle/SulfurCubeParticle.java:59 `new SulfurCubeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed)`. Its ctor (from :20) does `xd *= 0.1F; yd *= 0.1F; zd *= 0.1F; xd += xSpeed; …`.
- native: src-mc/net/minecraft/client/particle/BreakingItemParticle.java:167 `SulfurCubeProvider → new BreakingItemParticle(level, x, y, z, this.sprites.first())`. That is the 5-arg ctor (:42, `super(level,x,y,z,0,0,0,sprite)`) with no 0.1 damping, like slime particles.
- difference: Native goo flies with the full random Particle velocity (about 0.06–0.18 horizontal, 0.1 ± 0.18 vertical). VB's is scaled ×0.1, so the squish splash barely moves. VB's opaque layer is irrelevant because the texture alpha is only 0 or 255.
- fix_kind: mixin-vb (SulfurCubeParticle$Provider#createParticle)
- fix: @Redirect the 7-arg `new SulfurCubeParticle(...)` to the public 4-arg ctor `new SulfurCubeParticle(level, x, y, z)` (VB :23), keeping `pickSprite(sprites)`.
- config_gate: none

### SULFUR-36  Swallowed block ignores its light emission and BLOCK_STATE component
- severity: visual
- vb: src-vb/com/blackgear/vanillabackport/client/level/layer/SulfurCubeInnerLayer.java:
  - :70 `Block.byItem(containedBlock.getItem()).defaultBlockState()`
  - :95 `RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)`
  - :101 `packedLight`
- native:
  - src-mc/net/minecraft/client/renderer/entity/SulfurCubeRenderer.java:74-76 applies `BlockItemStateProperties` to `Block.byItem(...).defaultBlockState()`
  - BlockModelResolver.java:21 `blockLightCoords = emissiveRendering() ? 15728880 : pack(getLightEmission(), 0)`
  - BlockModelRenderState.java:81/109 `LightCoordsUtil.max(externalLightCoords, blockLightCoords)`
- difference: Native renders a swallowed light source (glowstone, sea lantern, shroomlight, froglights, jack o'lantern, crying obsidian; magma is fully emissive) at least as bright as its own emission. VB uses the cube's light, so these look dark in caves. VB also drops the stack's `block_state` component.
- fix_kind: mixin-vb (SulfurCubeInnerLayer#render)
- fix:
  - @ModifyArg the light passed to `renderSingleBlock` → `LightTexture.pack(Math.max(LightTexture.block(packedLight), state.getLightEmission()), LightTexture.sky(packedLight))`, or `0xF000F0` when `state.emissiveRendering(level,pos)`.
  - @Redirect `defaultBlockState()` → `stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(defaultState)`.
- config_gate: none

### SULFUR-37  Invisible cubes show no translucent ghost to spectators/teammates
- severity: visual
- vb:
  - src-vb/com/blackgear/vanillabackport/client/level/layer/SulfurCubeOuterLayer.java:50 `if (!cube.isInvisible() || glowing)`
  - :55 `RenderType.outline(this.getTextureLocation(cube))`, where the parent texture is the INNER one (SulfurCubeRenderer.java:57)
- native: src-mc/net/minecraft/client/renderer/entity/LivingEntityRenderer.java:89-96 `forceTransparent = !isBodyVisible && !state.isInvisibleToPlayer; … baseColor = forceTransparent ? 654311423 : -1`; :123-131 `entityTranslucentCullItemTarget(texture)` / `outline(texture)` with texture = outer (SulfurCubeRenderer.java:62)
- difference: Native draws an invisible cube's outer shell at 15% alpha (0x26FFFFFF) for spectators and see-friendly-invisibles teammates; VB draws nothing. When the cube is glowing and invisible, VB's outline uses the inner texture's alpha.
- fix_kind: mixin-vb (SulfurCubeOuterLayer#render)
- fix: @Inject HEAD. When `cube.isInvisible() && !cube.isInvisibleTo(Minecraft.getInstance().player)`, render the chosen model with `RenderType.itemEntityTranslucentCull(outerTex)` and colour `0x26FFFFFF`, then cancel. Redirect the outline texture argument to the outer texture (small variant for babies).
- config_gate: none

## Worldgen

### SULFUR-38  Sulfur pool can cut through sulfur spikes (missing can_place_feature)
- severity: gameplay
- vb: src-vb/data/minecraft/worldgen/configured_feature/sulfur_pool.json has only `"fluid"` and `"barrier"`. The 1.21.1 LakeFeature has no can-place predicate (src-121 …/feature/LakeFeature.java shell loop).
- native: src-mc/net/minecraft/data/worldgen/features/MiscOverworldFeatures.java:120 `BlockPredicate.not(BlockPredicate.matchesBlocks(Blocks.SULFUR_SPIKE))` as `can_place_feature`; src-mc/…/feature/LakeFeature.java:84 `if (!config.canPlaceFeature.test(level, offsetPos)) { return false; }`
- difference: Native drops the whole pool if any shell position is a sulfur_spike. VB always places it, cutting through spikes.
- fix_kind: mixin-mc (net.minecraft.world.level.levelgen.feature.LakeFeature#place)
- fix: `@WrapOperation(method="place", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/WorldGenLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", ordinal=0))` with `@Local(argsOnly=true) FeaturePlaceContext<LakeFeature.Configuration> ctx`. Call the original. If `ctx.config().barrier()` is a SimpleStateProvider of `ModBlocks.SULFUR` (identifies sulfur_pool) and the result `is(ModBlocks.SULFUR_SPIKE)`, return `Blocks.LAVA.defaultBlockState()`. That makes both existing shell checks return false, exactly native's outcome.
- config_gate: has_sulfur_caves

### SULFUR-39  Air predicates match only minecraft:air, not #air (cave_air from carvers)
- severity: gameplay
- vb:
  - src-vb/com/blackgear/vanillabackport/common/worldgen/features/ChaosCubedFeatures.java:222 `BlockPredicate.ONLY_IN_AIR_PREDICATE`; :263 and :279 `ONLY_IN_AIR_OR_WATER_PREDICATE`
  - placements/ChaosCubedPlacements.java:33 `scanningFor(Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, 32)` and :46
  - In 1.21.1 these are `matchesBlocks(Blocks.AIR)` (src-121 …/blockpredicates/BlockPredicate.java:21). The data has `{"type":"minecraft:matching_blocks","blocks":"minecraft:air"}`.
- native: src-mc/…/blockpredicates/BlockPredicate.java:23 `ONLY_IN_AIR_PREDICATE = matchesTag(BlockTags.AIR)`. The data has `{"type":"minecraft:matching_block_tag","tag":"minecraft:air"}` (spikes: `any_of[#air, water]`).
- difference: 1.21.1 carvers fill with CAVE_AIR (src-121 WorldCarver.java:35), and the biome uses the cave, cave_extra_underground and canyon carvers. In VB these stop or fail in carved caves; native passes through cave_air:
  - the rooted spring: allowed_tree_position and the 12-step up scan
  - the sulfur_pool target scan (32 up)
  - both sulfur_spike scans
- fix_kind: data-override
- fix:
  - `configured_feature/rooted_sulfur_spring.json`: `allowed_tree_position: {"type":"minecraft:matching_block_tag","tag":"minecraft:air"}`.
  - `placed_feature/rooted_sulfur_spring.json`: `environment_scan.allowed_search_condition` set to the same predicate.
  - `placed_feature/sulfur_pool.json`: `environment_scan.target_condition` set to the same predicate.
  - `configured_feature/sulfur_spike.json`: both inner placements' `allowed_search_condition: {"type":"minecraft:any_of","predicates":[{"type":"minecraft:matching_block_tag","tag":"minecraft:air"},{"type":"minecraft:matching_blocks","blocks":"minecraft:water"}]}`.
  - Keep all other fields (VB's `#minecraft:sulfur_spike_replaceable` id). The 1.21.1 tag `minecraft:air` exists (src-121 BlockTags.java:191).
- config_gate: has_sulfur_caves (has_sulfur_springs for rooted_sulfur_spring)

### SULFUR-40  LAKES step order: rooted_sulfur_spring is placed after sulfur_pool
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/worldgen/ChaosCubedFeatureManager.java:23 `writer.addFeature(Decoration.LAKES, ChaosCubedPlacements.ROOTED_SULFUR_SPRING)`. As a biome modifier this is appended after the JSON list `["lake_lava_underground","lake_lava_surface","sulfur_pool"]`.
- native: src-mc/net/minecraft/data/worldgen/BiomeDefaultFeatures.java:179-180 `addFeature(LAKES, CavePlacements.ROOTED_SULFUR_SPRING); addFeature(LAKES, MiscOverworldPlacements.SULFUR_POOL);`, giving `[lake_lava_underground, lake_lava_surface, rooted_sulfur_spring, sulfur_pool]`.
- difference: Native places springs first, then pools. In VB the springs see pools already placed, and the per-feature decoration seed indices are swapped, so the layouts differ.
- fix_kind: data-override (add-on NeoForge biome modifier)
- fix: BiomeModifier for `minecraft:sulfur_caves` in `Phase.AFTER_EVERYTHING`. In `getGenerationSettings().getFeatures(LAKES)`, if both `minecraft:rooted_sulfur_spring` and `minecraft:sulfur_pool` are present and the pool comes first, move `sulfur_pool` to the end.
- config_gate: has_sulfur_springs

### SULFUR-41  Monster spawns: zombie_villager weight 1 instead of 5; sulfur_cube entry last
- severity: data
- vb: src-vb/com/blackgear/vanillabackport/common/worldgen/biomes/ChaosCubedBiomes.java:37 `new SpawnerData(EntityType.ZOMBIE_VILLAGER, 1, 1, 1)`; ChaosCubedFeatureManager.java:28 `writer.addSpawn(MONSTER, new SpawnerData(SULFUR_CUBE, 100, 2, 4))` (appended last)
- native: src-mc/net/minecraft/data/worldgen/biome/OverworldBiomes.java:898 `mobs.addSpawn(MobCategory.MONSTER, 5, new SpawnerData(EntityTypes.ZOMBIE_VILLAGER, 1, 1))`; :890 sulfur_cube (100, 2-4) is the first MONSTER entry
- difference: Zombie villager weight is 1 of 307 in VB vs 5 of 311 in native (about 5× rarer). The entry order changes which mob a given roll picks; the distribution is otherwise the same.
- fix_kind: data-override (data/minecraft/worldgen/biome/sulfur_caves.json + biome modifier)
- fix: set the monster entry `{"type":"minecraft:zombie_villager","weight":5,"minCount":1,"maxCount":1}`. To get the order right while keeping the has_sulfur_cubes gate, use the AFTER_EVERYTHING modifier from 40 to move the sulfur_cube spawner to index 0 of MONSTER.
- config_gate: none (weight); has_sulfur_cubes (order)

### SULFUR-42  Biome fog colour is default overworld blue, not sulfur green
- severity: visual
- vb: ChaosCubedBiomes.java:57 `.fogColor(12638463)` (#C0D8FF); src-vb/data/minecraft/worldgen/biome/sulfur_caves.json:11 `"fog_color": 12638463`
- native: OverworldBiomes.java:906 `.setAttribute(EnvironmentAttributes.FOG_COLOR, -7555023)` (#8CB831); data `"minecraft:visual/fog_color": "#8cb831"`
- difference: VB fog is pale blue #C0D8FF; native is olive green #8CB831.
- fix_kind: data-override (data/minecraft/worldgen/biome/sulfur_caves.json)
- fix: `effects.fog_color` = `9222193`.
- config_gate: has_sulfur_caves

### SULFUR-43  Biome has no cave mood sound
- severity: audio
- vb: ChaosCubedBiomes.java:52-60 `new BiomeSpecialEffects.Builder()...backgroundMusic(...)` has no `.ambientMoodSound`; the biome JSON `effects` has no `mood_sound`
- native: src-mc/net/minecraft/data/worldgen/DimensionTypes.java:43 `.set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)` (overworld-wide, so it covers sulfur_caves). In 1.21.1 every vanilla biome sets it (src-121 OverworldBiomes.java:67 `.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)`).
- difference: Native plays `ambient.cave` mood sounds in sulfur caves (6000-tick delay, extent 8, offset 2.0); VB never does.
- fix_kind: data-override (data/minecraft/worldgen/biome/sulfur_caves.json)
- fix: add `"mood_sound": {"sound":"minecraft:ambient.cave","tick_delay":6000,"block_search_extent":8,"offset":2.0}` to `effects`.
- config_gate: has_sulfur_caves

### SULFUR-44  Sulfur surface rules are only injected for minecraft:overworld noise settings
- severity: gameplay
- vb: src-vb/…/core/mixin/common/worldgen/sulfur_caves/NoiseBasedChunkGeneratorMixin.java:30 `return this.settings.is(NoiseGeneratorSettings.OVERWORLD) ? ModSurfaceRuleData.merge(...) : original.call(...)`
- native: src-mc/net/minecraft/world/level/levelgen/NoiseGeneratorSettings.java:78-80. OVERWORLD, LARGE_BIOMES and AMPLIFIED all use `overworld(...)`, whose rule (:125) is `SurfaceRuleData.overworld(...)` with the sulfur rules (SurfaceRuleData:100, :295). The `caves` preset also uses overworldLike.
- difference: Without TerraBlender, Large Biomes and Amplified worlds still generate sulfur_caves (same multi-noise preset) but with plain stone/deepslate, no sulfur or cinnabar banding.
- fix_kind: mixin-mc (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator#buildSurface)
- fix: same `@WrapOperation` target as VB (`NoiseGeneratorSettings.surfaceRule()` inside `buildSurface(ChunkAccess, WorldGenerationContext, RandomState, StructureManager, BiomeManager, Registry, Blender)`). When `settings.is(LARGE_BIOMES) || settings.is(AMPLIFIED) || settings.is(CAVES)`, return the same merged rule (or the 45 structure). Skip when TerraBlender is loaded.
- config_gate: none (VB's injection is ungated); has_sulfur_caves if desired

### SULFUR-45  Surface rule placement/order: extra "bedrock" gradient, STONE fallback, band precedence
- severity: visual
- vb: src-vb/…/common/worldgen/surface/ModSurfaceRuleData.java:
  - :61 `SurfaceRules.verticalGradient("bedrock", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5))`
  - :35-39 `sequence(bedrock(), ifTrue(noise3d(-0.4,-0.1), cinnabar), ifTrue(noise3d(0.0,0.4), sulfur), ifTrue(noise3d(0.4), cinnabar), ifTrue(abovePreliminarySurface(), stone))`
  - :67-69 `merge` prepends this before the whole original overworld rule
- native: src-mc/net/minecraft/data/worldgen/SurfaceRuleData.java:
  - :285 `verticalGradient("bedrock_floor", bottom(), aboveBottom(5))` comes first.
  - :293 `ifTrue(abovePreliminarySurface(), mainRuleCloseToSurface)` reaches `:100 ifTrue(isBiome(SULFUR_CAVES), sequence(sulfurCaveBands, STONE))` only through two paths:
    - ON_FLOOR + `waterBlockCheck(-1,0)`
    - `waterStartCheck(-6,-1)` + UNDER_FLOOR
  - Deep-water floors fall to `ON_FLOOR → gravelOrStoneIfCeiling`.
  - Then :295 `ifTrue(isBiome(SULFUR_CAVES), sulfurCaveBands)` and :296 the deepslate gradient 0..8.
- difference:
  - (a) At y -64..-60 in sulfur caves, VB's extra gradient is named "bedrock", so its positional random differs from "bedrock_floor". The bedrock pattern differs, and band blocks can replace bedrock-floor positions.
  - (b) Above the preliminary surface, VB returns STONE for every gap between bands. Native returns STONE only on floor/under-floor positions and deepslate (y<8) or the default block elsewhere.
  - (c) Under deep water (more than 6 blocks of water above), native floors are GRAVEL (STONE on ceilings); VB places sulfur, cinnabar or stone.
- fix_kind: mixin-vb (com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData#merge, #makeRules)
- fix:
  - Make `makeRules()` return only the dappled-forest rule.
  - In `merge(original, rules)` (`@Inject` HEAD, cancellable), unwrap `original` as SurfaceRules.SequenceRuleSource (accessor/AT for `sequence()`). In the 1.21.1 overworld it is `[bedrock_floor, ifTrue(abovePrelim, main), deepslate]`.
  - Rebuild it as:
    ```java
    sequence(rules..., seq[0],
       ifTrue(abovePreliminarySurface(), ifTrue(isBiome(SULFUR_CAVES), sequence(
          ifTrue(ON_FLOOR, ifTrue(waterBlockCheck(-1,0), sequence(bands, STONE))),
          ifTrue(waterStartCheck(-6,-1), ifTrue(UNDER_FLOOR, sequence(bands, STONE)))))),
       seq[1], ifTrue(isBiome(SULFUR_CAVES), bands), seq[2..])
    ```
  - `bands = sequence(ifTrue(noise3d(SULFUR_CAVE_GRADIENT,-0.4,-0.1),CINNABAR), ifTrue(noise3d(..,0.0,0.4),SULFUR), ifTrue(noise3d(..,0.4),CINNABAR))`, built with VB's SpatialNoiseThresholdConditionSource. No separate bedrock rule.
  - The TerraBlender path (`SurfaceRuleManager.addToDefaultSurfaceRulesAtStage(OVERWORLD, BEFORE_BEDROCK, 5, makeRules())`) would need the same two sulfur rules at AFTER_BEDROCK; not verified.
- config_gate: none (VB's injection is ungated)
