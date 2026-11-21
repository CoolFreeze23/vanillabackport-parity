# copper — The Copper Age (VB 1.2.2 vs native 26.2)

**Summary.** 32 findings: 15 gameplay, 4 visual, 2 audio, 5 data, 6 cosmetic.
**Covered:** copper golem (entity, brain, transport behaviour, navigation, container plumbing, animations, model, renderer, statue conversion), the iron-golem flower gift, copper chest, golem statue, shelves (block, BE, renderer, side-chain), bars/chain/lantern/torch, weathering lightning rods and the lightning mixins, copper tools/armor/horse armor/spear/nugget, trims, spawn equipment, plus every copper_*/shelf data and asset file (loot, recipes, tags, advancements, blockstates, models, textures pixel by pixel, sounds.json, lang).
**Audio:** I checked native `assets-26.2/files/minecraft/sounds.json` (sha1 9ac006d5…, the same file as asset index 32) against VB's `sounds.json` and .ogg sha1s. Volume, pitch and weight match for every copper-area event. src-platform confirms that `SoundRegistry.soundType` uses 1.0/1.0, and that `BlockRegistry.register(name, factory, props)` is `() -> factory.apply(properties)`, with no copy (relevant to COPPER-14).
**Not checked:** how the .ogg files actually sound beyond their hashes. Spear mechanics apart from copper's own constants (the spear area). Nautilus copper armor (the nautilus area). Spawn-egg art (VB keeps it in a separate config-gated model). Wool stair/slab shear speeds (VB-only blocks, no native counterpart).
**Default config:** every number below assumes VB's default config. `golem_weathering_tick_from/to` (504000/552000) and `golem_item_carry_amount` (16) match native at their defaults.

## Coverage

| VB class / data | Native counterpart | Result |
|---|---|---|
| copper_golem/CopperGolem | world/entity/animal/golem/CopperGolem | findings 01, 07, 25 |
| copper_golem/CopperGolemAi | golem/CopperGolemAi | findings 04 (predicates), otherwise match |
| copper_golem/TransportItemsBetweenContainers | ai/behavior/TransportItemsBetweenContainers | finding 04; timings (60-tick interaction, sound at tick 9, 140 cooldown, 6000 memory, 10/50 caps, 32×8 search, 3.0/2.0/1.0/0.5 distances) match |
| copper_golem/CopperGolemPathNavigation | ai/navigation/GroundPathNavigation (+PathNavigation.requiredPathLength) | findings 01, 02 |
| copper_golem/CopperGolemOxidationLevel(s), CopperGolemState | same names | match (texture files renamed, pixels identical) |
| copper_golem/container/* (ContainerManager, adapters, ContainerUser) | entity/ContainerUser, block/entity/ContainerOpenersCounter | finding 03 |
| ai/goal/OfferCopperGolemFlowerGoal | ai/goal/OfferFlowerGoal | finding 22 |
| mixin copper_golem/CarvedPumpkinBlockMixin | block/CarvedPumpkinBlock | match (spawn pattern, weather state); finding 05 is in the chest mapping it calls |
| blocks/CopperChestBlock, WeatheringCopperChestBlock | block/CopperChestBlock, WeatheringCopperChestBlock, ChestBlock | findings 05, 06, 18 |
| block_entities/CopperChestBlockEntity | block/entity/ChestBlockEntity (playSound) | match (0.5 vol, 0.9–1.0 pitch, LEFT half silent) |
| blocks/CopperGolemStatueBlock, WeatheringCopperGolemStatueBlock | same | finding 12; poses, axe/honeycomb flow, shape match |
| block_entities/CopperGolemStatueBlockEntity | same | match |
| blocks/ShelfBlock, SelectableSlotContainer, SideChainPart(Block) | same | findings 13, 14, 15 |
| block_entities/ShelfBlockEntity, ListBackedContainer | same | match (ItemOwner is engine-only, no gameplay effect) |
| blocks/WeatheringCopperBars/Chain/Lantern/LightningRodBlock, WeatheringCopperBlocks | same | match (block properties identical) |
| blocks/LazyTorchBlock, LazyWallTorchBlock | TorchBlock / WallTorchBlock (COPPER_TORCH) | match; lang finding 28 |
| mixin weathering_lightning_rods/* , ModPoiTypes | LightningBolt, ServerLevel.findLightningRod, PoiTypes.LIGHTNING_ROD, LightningRodBlock | findings 08, 09, 10, 11 |
| mixin equipment/MobMixin + CommonSetup copper spawn tiers | Mob.populateDefaultEquipmentSlots / getEquipmentForSlot | finding 19 |
| mixin tool_efficiency/ShearsItemMixin | ShearsItem.createToolProperties | no native counterpart (wool stairs/slabs are VB-only) |
| mixin client armor_trims/SpriteSourceListMixin, ModTrimMaterials | TrimMaterials, MaterialAssetGroup | finding 20 |
| ModArmorMaterials | equipment/ArmorMaterials.COPPER | match (11 dur., 1/3/4/2/4, ench 8, 0 toughness, 0 KB res) |
| ModToolMaterials | ToolMaterial.COPPER | match (190, 5.0, 1.0, 13) |
| ModItems copper tools/armor/horse armor/nugget | Items | match (sword 3/-2.4, shovel 1.5/-3, pick 1/-2.8, axe 7/-3.2, hoe -1/-2) |
| ModItems COPPER_SPEAR | Items.COPPER_SPEAR | finding 21 |
| ModItems/ModBlocks statue item | Items.COPPER_GOLEM_STATUE | finding 29 |
| ModMemoryModuleTypes / sensors for golem | MemoryModuleType / CopperGolem BRAIN_PROVIDER | match |
| client CopperGolemAnimation | client/animation/definitions/CopperGolemAnimation | match (token-level identical) |
| client CopperGolemModel | client/model/animal/golem/CopperGolemModel | match except antenna offset (finding 25) |
| client CopperGolemRenderer, BlockDecorationLayer | entity/CopperGolemRenderer, layers/BlockDecorationLayer | finding 25 |
| client CopperGolemStatueModel/Renderer/ItemRenderer | statue model, CopperGolemStatueBlockRenderer, CopperGolemStatueSpecialRenderer | finding 26; rotation math is equivalent |
| client CopperChestRenderer, CopperChestItemRenderer | ChestRenderer, Sheets.CHEST_COPPER | match |
| client ShelfRenderer | blockentity/ShelfRenderer | finding 27 |
| data loot_table/blocks/* (copper, shelves), entities/copper_golem | same paths | match in meaning; finding 33 (random_sequence) |
| data recipes (copper_*, shelves, waxed_*_from_honeycomb, VanillaDynamicRecipes) | same | findings 16, 30, 31 |
| data tags (block/item) | same | findings 04, 18; the rest match after nested resolution |
| data advancements wax_on/wax_off | same | finding 17 |
| loot injections (copper horse armor) | chests/* | match (pool 0, weights 15/1/1/5/1/1/15) |
| assets blockstates/models/textures | same | match (waxed blockstates just point at copies); textures pixel-identical |
| assets sounds.json + .ogg | asset index 32 | findings 23, 24 |
| fuel (BlockIntegrations) | FuelValues | finding 15 |

---

### COPPER-01  Golem pathfinding is capped at 16 blocks (native 48)
- severity: gameplay
- vb: `common/level/entities/mob/animal/golem/copper_golem/CopperGolem.java:93-97` (createNavigation): `CopperGolemPathNavigation navigation = new CopperGolemPathNavigation(this, level); navigation.setCanOpenDoors(true); navigation.setCanPassDoors(true);`. The attributes at `:101` add no FOLLOW_RANGE, so the default is 16.
- native: `world/entity/animal/golem/CopperGolem.java:93` (ctor): `this.getNavigation().setRequiredPathLength(48.0F);`. Also `ai/navigation/PathNavigation.java:72,81-82`: `maxVisitedNodes = floor(getMaxPathLength()*16)`, `getMaxPathLength() = max(FOLLOW_RANGE, requiredPathLength)`.
- difference: Native paths use a max distance of 48 and 768 visited nodes. VB uses 16 and 256 (1.21.1 `PathNavigation` reads FOLLOW_RANGE=16). The golem looks for chests up to 32 blocks away horizontally, but in VB any chest more than about 16 blocks of path away fails or gets a partial path. It is then marked unreachable, so the golem sorts far less of the area than native.
- fix_kind: mixin-vb (`CopperGolemPathNavigation`)
- fix: add two overriding methods to `CopperGolemPathNavigation` through a mixin; a mixin may add methods that override the superclass.
  ```java
  @Mixin(CopperGolemPathNavigation.class)
  abstract class CopperGolemNavParity extends GroundPathNavigation {
     CopperGolemNavParity(Mob m, Level l){ super(m,l); }
     @Override protected PathFinder createPathFinder(int maxVisitedNodes) {   // called from PathNavigation.<init>
        return super.createPathFinder(Math.max(maxVisitedNodes, Mth.floor(48.0F * 16.0F)));  // 768
     }
     @Override protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
        return this.createPath(targets, regionOffset, offsetUpward, accuracy,
           Math.max((float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), 48.0F));
     }
  }
  ```
- config_gate: has_copper_golems

### COPPER-02  findSurfacePosition never climbs out of solid blocks (`>=` typo)
- severity: gameplay
- vb: `copper_golem/CopperGolemPathNavigation.java:61` (findSurfacePosition): `while (columnPos.getY() >= this.level.getMaxBuildHeight() && chunk.getBlockState(columnPos).isSolid())`
- native: `ai/navigation/GroundPathNavigation.java:83`: `while (columnPos.getY() <= this.level.getMaxY() && chunk.getBlockState(columnPos).isSolid())`
- difference: When the target block is solid, native walks up through every solid block to the first non-solid one. VB's loop condition is always false, so it returns `pos.above()` even when that is still inside solid blocks. This applies whenever the golem paths without the below-surface flag, which includes ordinary walk targets.
- fix_kind: mixin-vb (`CopperGolemPathNavigation.findSurfacePosition`)
- fix: `@Redirect`/`@ModifyExpressionValue` on the `getMaxBuildHeight()` comparison is fragile. Overwrite instead: `@Overwrite private BlockPos findSurfacePosition(LevelChunk chunk, BlockPos pos)` with the native body from `GroundPathNavigation.java:57-88`, using `level.getMinBuildHeight()` for `getMinY()` and `level.getMaxBuildHeight()` for `getMaxY()`. The last loop must read `columnPos.getY() <= this.level.getMaxBuildHeight()`.
- config_gate: has_copper_golems

### COPPER-03  Golem-opened chests never re-check their openers (lid stuck open, counts drift)
- severity: gameplay
- vb: `copper_golem/container/ContainerOpenersCounterAdapter.java:100-111` (incrementOpeners): `int previous = counter.openCount++; if (previous == 0) { ...callOnOpen(level, pos, state); level.gameEvent(entity, GameEvent.CONTAINER_OPEN, pos); }`, with no recheck scheduled. 1.21.1 `ContainerOpenersCounter.recheckOpeners` counts only `Player`s (`getPlayersWithContainerOpen` → `EntityTypeTest.forClass(Player.class)`).
- native: `block/entity/ContainerOpenersCounter.java` incrementOpeners: `if (previous == 0) { this.onOpen(...); level.gameEvent(...); scheduleRecheck(level, pos, blockState); }`. `recheckOpeners` uses `getEntitiesWithContainerOpen`, which counts every `ContainerUser`, golems included.
- difference: Native re-validates the chest every 5 ticks. A golem that stops mid-interaction (hurt or panic, leashed, killed, unloaded) clears `openedChestPos`, and within 5 ticks the chest closes, plays its close sound and drops the trapped-chest signal. In VB the chest stays open indefinitely. If a player opens the same chest, the 1.21.1 recheck resets the count to players only, so the golem's later `decrementOpeners` can close the lid on the player or push the count negative.
- fix_kind: mixin-mc (`net.minecraft.world.level.block.entity.ContainerOpenersCounter`) + mixin-vb (`ContainerOpenersCounterAdapter.incrementOpeners`)
- fix:
  1. `@Inject(method="incrementOpeners", at=@At("TAIL"))` in the VB adapter: `if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) level.scheduleTick(pos, state.getBlock(), 5);`
  2. `@Inject(method="recheckOpeners", at=@At("HEAD"), cancellable=true)` on `ContainerOpenersCounter`. Reimplement the 26.2 body with `List<LivingEntity> users = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(maxInteractionRange + 4.0), e -> !e.isSpectator() && (e instanceof Player p ? isOwnContainer(p) : e instanceof ContainerUser u && u.hasContainerOpen(self, pos)))`. Set `maxInteractionRange = max over users of (player ? blockInteractionRange() : ((ContainerUser)e).getContainerInteractionRange())`. Then apply the native open/close/gameEvent/`openerCountChanged`/reschedule logic (`ContainerOpenersCounter.java` 26.2 `recheckOpeners`). Use `@Shadow` for `openCount`, `maxInteractionRange`, `onOpen`, `onClose`, `openerCountChanged`, `isOwnContainer`, and `scheduleRecheck`.
- config_gate: has_copper_golems

### COPPER-04  Golem will deliver to any modded "c:chests" container
- severity: gameplay
- vb: `copper_golem/CopperGolemAi.java:38-39,100`: source = `#minecraft:transport_item_source_blocks` (= `#copper_chests`); destination = `TRANSPORT_ITEM_DESTINATION_BLOCK.test(state) && !SOURCE.test(state)`, where `data/minecraft/tags/block/transport_item_destination_blocks.json` = `["minecraft:chest","minecraft:trapped_chest","#c:chests"]`. `TransportItemsBetweenContainers.java:285`: `if (potentialTarget instanceof Container)`.
- native: `golem/CopperGolemAi.java:43-44`: `block.is(BlockTags.COPPER_CHESTS)` / `block.is(Blocks.CHEST) || block.is(Blocks.TRAPPED_CHEST)`. `TransportItemsBetweenContainers.java:287`: `if (potentialTarget instanceof ChestBlockEntity chestBlockEntity)`.
- difference: Native golems only take from copper chests and only deliver to vanilla chests and trapped chests. VB also delivers to any block in `c:chests` that has a `Container` block entity (Iron Chests, Quark chests, and other modded chests) and scans every `Container` BE. Vanilla-only worlds behave the same.
- fix_kind: data-override + mixin-vb
- fix: `data/minecraft/tags/block/transport_item_destination_blocks.json` → `{"replace": true, "values": ["minecraft:chest", "minecraft:trapped_chest"]}`. Optionally add `@ModifyConstant`/`@WrapOperation` on the `instanceof Container` test in `TransportItemsBetweenContainers.getTransportTarget` so it requires `ChestBlockEntity`.
- config_gate: has_copper_golems

### COPPER-05  A golem built on waxed copper leaves an unwaxed chest
- severity: gameplay
- vb: `blocks/CopperChestBlock.java` `COPPER_TO_COPPER_CHEST_MAPPING`: `Blocks.WAXED_COPPER_BLOCK, ModBlocks.COPPER_CHEST, Blocks.WAXED_EXPOSED_COPPER, ModBlocks.EXPOSED_COPPER_CHEST, ... Blocks.WAXED_OXIDIZED_COPPER, ModBlocks.OXIDIZED_COPPER_CHEST`
- native: `block/CopperChestBlock.java` `WeatheringCopperCollection.zipApply(Blocks.COPPER_BLOCK, Blocks.COPPER_CHEST, result::put)`. `WeatheringCopperCollection.java:105-107` zips `weathering→weathering` **and** `waxed→waxed`.
- difference: Native maps waxed copper blocks to the matching waxed copper chest. VB maps them to unwaxed chests, which then keep oxidizing.
- fix_kind: mixin-vb (`CopperChestBlock.getFromCopperBlock`)
- fix: `@ModifyVariable(method="getFromCopperBlock", at=@At(value="STORE"), ordinal=0)` on the local `block`. If `copperBlock` is a waxed copper block (`HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(copperBlock)`), return the waxed chest: `WAXED_COPPER_BLOCK→ModBlocks.WAXED_COPPER_CHEST`, `WAXED_EXPOSED_COPPER→WAXED_EXPOSED_COPPER_CHEST`, `WAXED_WEATHERED_COPPER→WAXED_WEATHERED_COPPER_CHEST`, `WAXED_OXIDIZED_COPPER→WAXED_OXIDIZED_COPPER_CHEST`.
- config_gate: has_copper_golems

### COPPER-06  Copper chests of different oxidation or wax state do not pair when placed
- severity: gameplay
- vb: `blocks/CopperChestBlock.java:70-72` (getStateForPlacement): `BlockState state = super.getStateForPlacement(context); return getLeastOxidizedChestOfConnectedBlocks(...)`. The 1.21.1 super (`ChestBlock.java:234-236`) uses `candidatePartnerFacing(...)`: `return blockstate.is(this) && blockstate.getValue(TYPE) == ChestType.SINGLE ? ... : null;`
- native: `block/ChestBlock.java:216-236,251-254`: getStateForPlacement uses `candidatePartnerFacing(level,pos,dir)`: `this.chestCanConnectTo(state) && state.getValue(TYPE) == ChestType.SINGLE`, where `CopperChestBlock.chestCanConnectTo` = `is(BlockTags.COPPER_CHESTS)`.
- difference: Natively, a copper chest placed beside any single copper chest (any oxidation, waxed or not) joins it, and both halves become the least-oxidized variant. In VB the halves join only if they are exactly the same block. VB's `getLeastOxidizedChestOfConnectedBlocks` never runs for mixed placements. Shift-placing against the side has the same limit.
- fix_kind: mixin-vb (`CopperChestBlock.getStateForPlacement`)
- fix: `@Inject(method="getStateForPlacement", at=@At("HEAD"), cancellable=true)`, then reimplement native `ChestBlock.getStateForPlacement` (26.2 lines 216-236) using VB's private `candidatePartnerFacing(Level, BlockPos, Direction)` and `getChestType(Level, BlockPos, Direction)` (via `@Invoker`). Pass the result through `getLeastOxidizedChestOfConnectedBlocks(state, level, pos)` (`@Invoker`, static) and add `.setValue(WATERLOGGED, fluid.is(Fluids.WATER))`.
- config_gate: none

### COPPER-07  Waxing a golem plays the wax sound twice
- severity: audio
- vb: `CopperGolem.java:224-225` (mobInteract): `level.levelEvent(null, 3003, pos, 0); level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);`
- native: `CopperGolem.java:232-234`: `level.levelEvent(this, 3003, this.blockPosition(), 0); this.nextWeatheringTick = -2L; this.usePlayerItem(player, hand, itemStack);`. Level event 3003 already plays `HONEYCOMB_WAX_ON` (BLOCKS, 1.0, 1.0) on the client (`LevelEventHandler.java:422-424`; the same in 1.21.1 `LevelRenderer:3324-3326`).
- difference: VB plays the wax-on sound twice, once from the server and once from the client level event. Native plays it once.
- fix_kind: mixin-vb (`CopperGolem.mobInteract`)
- fix: `@WrapOperation` on the `Level.playSound(Player, BlockPos, SoundEvent, SoundSource, float, float)` call whose SoundEvent is `HONEYCOMB_WAX_ON` inside `mobInteract`, as a no-op.
- config_gate: has_copper_golems

### COPPER-08  The plain lightning rod stops attracting lightning
- severity: gameplay
- vb: `core/mixin/common/weathering_lightning_rods/ServerLevelMixin.java:44-57`: `@Inject(method="findLightningRod", at=HEAD, cancellable=true) ... findClosest(p -> p.is(ModPoiTypes.LIGHTNING_RODS), ...) ; cir.setReturnValue(...)`. `ModPoiTypes.getLightningRodBlocks()` lists only `EXPOSED/WEATHERED/OXIDIZED_LIGHTNING_ROD` and the four `WAXED_*`; `Blocks.LIGHTNING_ROD` stays in `PoiTypes.LIGHTNING_ROD`.
- native: `server/level/ServerLevel.java:610-619`: `findClosest(p -> p.is(PoiTypes.LIGHTNING_ROD), ...)`, where `PoiTypes.LIGHTNING_RODS = Blocks.LIGHTNING_ROD.asList()` (all 8 rods).
- difference: VB's cancelling HEAD inject drops the vanilla POI, so an unweathered `minecraft:lightning_rod` never redirects lightning (128-block search). Only the seven VB variants do. Natively all 8 rods attract lightning.
- fix_kind: mixin-mc (`ServerLevel.findLightningRod`)
- fix: register a mixin with priority 500 (applied before VB's 1000, so its HEAD callback runs first and cancels):
  ```java
  @Inject(method="findLightningRod", at=@At("HEAD"), cancellable=true)
  private void parity$allRods(BlockPos center, CallbackInfoReturnable<Optional<BlockPos>> cir) {
     ServerLevel self = (ServerLevel)(Object)this;
     cir.setReturnValue(self.getPoiManager().findClosest(
        h -> h.is(PoiTypes.LIGHTNING_ROD) || h.is(ModPoiTypes.LIGHTNING_RODS),
        p -> p.getY() == self.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ()) - 1,
        center, 128, PoiManager.Occupancy.ANY).map(BlockPos::above));
  }
  ```
  Check the callback order in a dev run. If VB's handler still wins, cancel it with MixinSquared (`@TargetHandler(mixin="…weathering_lightning_rods.ServerLevelMixin", name="findLightningRods")`).
- config_gate: has_weathering_lightning_rods (the variants only exist when it is on, but the regression for the plain rod occurs regardless)

### COPPER-09  A lightning strike on a rod cleans the attached block instead of the rod; waxed targets do nothing
- severity: gameplay
- vb: the engine `LightningBolt.clearCopperOnLightningStrike` (1.21.1): `if (blockstate.getBlock() instanceof LightningRodBlock) { blockpos = pos.relative(FACING.getOpposite()); ... } if (blockstate1.getBlock() instanceof WeatheringCopper) { setBlockAndUpdate(blockpos, getFirst(...)); ...walk }`. VB adds `LightningBoltMixin.java:51-53`: `return block instanceof WeatheringCopper && WAX_OFF_BY_BLOCK.containsKey(block) ? null : block;` (waxed rods, which VB makes `WeatheringCopper`, become "no block").
- native: `world/entity/LightningBolt.java:191-208`: `boolean isWaxed = WAX_OFF_BY_BLOCK.get(struck) != null; boolean isWeatheringCopper = struck instanceof WeatheringCopper; if (isWeatheringCopper || isWaxed) { if (isWeatheringCopper) setBlockAndUpdate(struckPos, WeatheringCopper.getFirst(...)); strikes = random.nextInt(3)+3; ... randomWalkCleaningCopper(level, struckPos, workPos, random.nextInt(8)+1) }`
- difference: Natively, a struck rod is reset to unaffected itself, and the 3–5 random walks of 1–8 steps start from the rod. A struck waxed rod or waxed copper block still runs the random walks without resetting itself. In VB the rod keeps its oxidation, and the block the rod is attached to is fully reset when it is weathering copper. Waxed rods and waxed copper blocks trigger no cleaning at all.
- fix_kind: mixin-mc (`LightningBolt.clearCopperOnLightningStrike`)
- fix: `@Inject(method="clearCopperOnLightningStrike", at=@At("HEAD"), cancellable=true)` (static):
  ```java
  BlockState s = level.getBlockState(pos); Block b = s.getBlock();
  boolean waxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(b) || DataMapHooks.getBlockUnwaxed(b) != null;
  boolean weathering = b instanceof WeatheringCopper && !waxed;
  if (weathering || waxed) {
     if (weathering) level.setBlockAndUpdate(pos, WeatheringCopper.getFirst(level.getBlockState(pos)));
     BlockPos.MutableBlockPos work = pos.mutable();
     int strikes = level.random.nextInt(3) + 3;
     for (int i = 0; i < strikes; i++) LightningBoltInvoker.callRandomWalkCleaningCopper(level, pos, work, level.random.nextInt(8) + 1);
  }
  ci.cancel();
  ```
  `randomWalkCleaningCopper` is private static, so call it through an `@Invoker`.
- config_gate: has_weathering_lightning_rods

### COPPER-10  Struck rods fire onLightningStrike twice (double spark burst)
- severity: visual
- vb: `LightningBoltMixin.java:33-42`: `@Inject(method="powerLightningRod", at=HEAD) ... if (struckState.is(ModBlockTags.LIGHTNING_RODS) && struckState.getBlock() instanceof LightningRodBlock rod) rod.onLightningStrike(...)`. NeoForge 1.21.1 `powerLightningRod` already calls `onLightningStrike` for every `instanceof LightningRodBlock` (`LightningBolt.java:73-75`).
- native: `LightningBolt.java:69-75`: a single `lightningRodBlock.onLightningStrike(stateBelow, level, strikePosition)`.
- difference: In VB every rod gets two `setBlock(POWERED)` calls, two neighbour updates, two `scheduleTick(8)` calls and two `levelEvent(3002)` calls, which doubles the electric-spark particles. Native fires once.
- fix_kind: mixin-vb (`core.mixin.common.weathering_lightning_rods.LightningBoltMixin.shouldPowerLightningRod`)
- fix: cancel VB's handler with MixinSquared (`@TargetHandler(mixin=".../LightningBoltMixin", name="shouldPowerLightningRod")` + `@Inject(at=HEAD, cancellable=true)` → `ci.cancel()`). The engine call already covers every rod.
- config_gate: has_weathering_lightning_rods

### COPPER-11  Channeling on weathered or waxed rods uses a different bolt position and sound source
- severity: gameplay
- vb: `LightningRodBlockMixin.java:54-73` (vb$onProjectileHit, only for non-plain rods): `lightningBolt.moveTo(Vec3.atBottomCenterOf(pos.above())); ... level.playSound(null, pos, TRIDENT_THUNDER, SoundSource.WEATHER, 5.0F, 1.0F);`. The plain rod uses vanilla 1.21.1 `enchantment/channeling.json` (`location_check block = minecraft:lightning_rod`).
- native: `data/minecraft/enchantment/channeling.json` hit_block: `location_check {"block":{"blocks":"#minecraft:lightning_rods"},"can_see_sky":true}`, `summon_entity lightning_bolt` at the clamped hit position (`ThrownTrident.java:155`, `SummonEntityEffect.java:33-47`), and `play_sound item.trident.thunder` volume 5, pitch 1, with the source set to `entity.getSoundSource()` and skipped if the trident is silent.
- difference: For the seven VB rods, the bolt spawns at the bottom centre of the block above the rod rather than at the hit point. The thunder plays on the WEATHER channel at the block corner instead of the trident's sound source at the hit point, and it ignores silent tridents.
- fix_kind: data-override + mixin-vb
- fix: override `data/minecraft/enchantment/channeling.json` with the vanilla 1.21.1 file, changing only `location_check.predicate.block.blocks` from `"minecraft:lightning_rod"` to `"#minecraft:lightning_rods"`. Then cancel `LightningRodBlockMixin.vb$onProjectileHit` (MixinSquared `@TargetHandler`, or `@Inject` HEAD cancel on the VB `BlockExtension` default method target).
- config_gate: has_weathering_lightning_rods

### COPPER-12  Statue comparator output is one level too low and is not refreshed on removal
- severity: gameplay
- vb: `blocks/CopperGolemStatueBlock.java:134-135`: `return ((Pose)state.getValue(POSE)).ordinal();`. `onRemove` only removes the BE and never calls `updateNeighbourForOutputSignal`.
- native: `block/CopperGolemStatueBlock.java:147-148`: `return state.getValue(POSE).ordinal() + 1;`, plus `affectNeighborsAfterRemoval → level.updateNeighbourForOutputSignal(pos, state.getBlock())`.
- difference: Natively the poses give 1/2/3/4 (standing/running/sitting/star). VB gives 0/1/2/3, so a standing statue gives no signal. Comparators reading the statue through a block are not updated when it is removed.
- fix_kind: mixin-vb (`CopperGolemStatueBlock.getAnalogOutputSignal`, `onRemove`)
- fix: `@ModifyReturnValue(method="getAnalogOutputSignal", at=@At("RETURN")) int f(int v){ return v + 1; }`, and `@Inject(method="onRemove", at=@At("TAIL"))`: `if (!state.is(newState.getBlock())) level.updateNeighbourForOutputSignal(pos, state.getBlock());`
- config_gate: none

### COPPER-13  Shelves output to comparators on every side (native: back only)
- severity: gameplay
- vb: `blocks/ShelfBlock.java:287-290`: `if (level.isClientSide()) return 0; else if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {...bits...}`
- native: `block/ShelfBlock.java:318-321`: `... } else if (direction != state.getValue(FACING).getOpposite()) { return 0; } else ...`. `direction` is the side facing the comparator (`ComparatorBlock.java:103,110` pass `direction.getOpposite()`).
- difference: Natively only a comparator directly behind the shelf, or reading through a block behind it, gets the item-presence or POWERED signal. VB outputs to all four sides. 1.21.1 has no direction argument, so the fix has to go on the comparator.
- fix_kind: mixin-mc (`ComparatorBlock.getInputSignal`)
- fix: `@WrapOperation` on both `BlockState.getAnalogOutputSignal(Level, BlockPos)` calls in `ComparatorBlock.getInputSignal`: `Direction cmpFacing = comparatorState.getValue(FACING); if (target.getBlock() instanceof com.blackgear.vanillabackport.common.level.blocks.ShelfBlock && cmpFacing.getOpposite() != target.getValue(ShelfBlock.FACING).getOpposite()) return 0; return original.call(target, level, pos);`. This is equivalent to requiring `cmpFacing == shelfFacing`.
- config_gate: none

### COPPER-14  Every shelf has the map colour COLOR_LIGHT_GRAY (shared mutable Properties)
- severity: visual
- vb: `common/registries/blocks/ModBlocks.java:402-437,658-660`: `REGISTRIES.register("acacia_shelf", ShelfBlock::new, SharedBlockProperties.SHELF.mapColor(Blocks.ACACIA_PLANKS.defaultMapColor()).ignitedByLava())` ... `"poplar_shelf", ShelfBlock::new, SharedBlockProperties.SHELF.mapColor(MapColor.COLOR_LIGHT_GRAY)`. `SharedBlockProperties.java:35` holds one static `Properties`. `Properties.mapColor` mutates and returns `this` (1.21.1 `BlockBehaviour.java:1149-1151`). Blocks are built later by the NeoForge `DeferredRegister` (Platform `CoreRegistryImpl` → `DeferredRegister`), and `BlockStateBase` reads `properties.mapColor` at construction (`BlockBehaviour.java:520`).
- native: `block/Blocks.java:1071-1180`: a fresh `Properties.of()` per shelf, with `mapColor` set to ACACIA_PLANKS / BAMBOO_PLANKS / BIRCH_PLANKS / CHERRY_PLANKS default, `MapColor.CRIMSON_STEM`, DARK_OAK_LOG / JUNGLE_LOG / MANGROVE_LOG default, OAK_PLANKS, PALE_OAK_PLANKS, SPRUCE_LOG default, and `MapColor.WARPED_STEM`.
- difference: All 12 vanilla-wood shelves (and poplar) are constructed after the last `mapColor(COLOR_LIGHT_GRAY)` call, so every shelf shows light gray on maps. `ignitedByLava` leaks the same way to crimson and warped, which happens to match native, where all shelves are `ignitedByLava`.
- fix_kind: mixin-vb (`ShelfBlock.<init>`)
- fix: `@ModifyVariable(method="<init>", at=@At("HEAD"), argsOnly=true)` on `ShelfBlock(Properties)` cannot tell which block is being built. Instead mixin `BlockBehaviour.BlockStateBase#getMapColor(BlockGetter, BlockPos)`: if `getBlock() instanceof com.blackgear...ShelfBlock`, return a static map from the block's registry path to colour. Use acacia→`MapColor.COLOR_ORANGE`, bamboo→`COLOR_YELLOW`, birch→`SAND`, cherry→`TERRACOTTA_WHITE`, crimson→`CRIMSON_STEM`, dark_oak→`COLOR_BROWN`, jungle→`DIRT`, mangrove→`COLOR_RED`, oak→`WOOD`, pale_oak→`QUARTZ`, spruce→`PODZOL` and warped→`WARPED_STEM` (the native values resolved). The simplest equivalent is an `@Inject` at the TAIL of `ShelfBlock.<init>` that re-runs the state colour cache: for each state in `getStateDefinition().getPossibleStates()`, set `((BlockStateBaseAccessor)state).setMapColor(color)`, keyed by `BuiltInRegistries.BLOCK.getKey(this)` inside a `RegisterEvent` callback after registration.
- config_gate: none

### COPPER-15  Shelves are not furnace fuel (native 300 ticks)
- severity: gameplay
- vb: `common/integrations/BlockIntegrations.java:17-24` (registerFuel) lists dry grass, leaf litter, wool stairs/slabs and cushions, but no shelves. VB ships no `data/neoforge/data_maps/item/furnace_fuels.json`.
- native: `block/entity/FuelValues.java:59`: `.add(ItemTags.WOODEN_SHELVES, baseUnit * 3 / 2)` (base 200, so 300 ticks), with `:108` `.remove(ItemTags.NON_FLAMMABLE_WOOD)`, whose item tag includes `crimson_shelf` and `warped_shelf`.
- difference: Natively the ten overworld-wood shelves burn for 300 ticks (1.5 items) and crimson/warped shelves are not fuel. In VB no shelf is fuel.
- fix_kind: data-override (`data/neoforge/data_maps/item/furnace_fuels.json`)
- fix:
  ```json
  {"replace": false, "values": {
    "minecraft:acacia_shelf": {"burn_time": 300}, "minecraft:bamboo_shelf": {"burn_time": 300},
    "minecraft:birch_shelf": {"burn_time": 300}, "minecraft:cherry_shelf": {"burn_time": 300},
    "minecraft:dark_oak_shelf": {"burn_time": 300}, "minecraft:jungle_shelf": {"burn_time": 300},
    "minecraft:mangrove_shelf": {"burn_time": 300}, "minecraft:oak_shelf": {"burn_time": 300},
    "minecraft:pale_oak_shelf": {"burn_time": 300}, "minecraft:spruce_shelf": {"burn_time": 300} }}
  ```
  Also add `crimson_shelf` and `warped_shelf` to `data/minecraft/tags/item/non_flammable_wood.json` (`replace:false`).
- config_gate: none

### COPPER-16  Honeycomb crafting recipes are missing for copper bars, chains and lanterns
- severity: data
- vb: `data/minecraft/recipe/` contains `waxed_*_copper_chest_from_honeycomb`, `waxed_*_copper_golem_statue_from_honeycomb` and `waxed_*_lightning_rod_from_honeycomb`, but no `waxed_*copper_bars_from_honeycomb`, `waxed_*copper_chain_from_honeycomb` or `waxed_*copper_lantern_from_honeycomb`. VanillaDynamicRecipes doesn't generate them either.
- native: 12 recipes, for example `recipe/waxed_copper_bars_from_honeycomb.json`: `{"type":"minecraft:crafting_shapeless","category":"building","group":"waxed_copper_bar","ingredients":["minecraft:copper_bars","minecraft:honeycomb"],"result":{"id":"minecraft:waxed_copper_bars"}}`, plus matching `advancement/recipes/building_blocks/*.json`.
- difference: In VB these 12 waxed variants can only be made by using honeycomb on a placed block, not in a crafting grid.
- fix_kind: data-override (`data/minecraft/recipe/` + `data/minecraft/advancement/recipes/building_blocks/`)
- fix: add 12 shapeless recipes in 1.21.1 format: `{"type":"minecraft:crafting_shapeless","category":"building","group":"<G>","ingredients":[{"item":"minecraft:<S>"},{"item":"minecraft:honeycomb"}],"result":{"count":1,"id":"minecraft:waxed_<S>"}}`. `<S>` ranges over `copper_bars, exposed_copper_bars, weathered_copper_bars, oxidized_copper_bars, copper_chain, exposed_…, copper_lantern, exposed_…, weathered_…, oxidized_…`. `<G>` is `waxed_copper_bar` for all bars, `waxed_copper_chain` for chains and `waxed_copper_lantern` for lanterns, as in native. The advancements copy native `waxed_copper_bars_from_honeycomb.json` (criteria `has_<S>` inventory_changed + `has_the_recipe`).
- config_gate: none

### COPPER-17  Wax On / Wax Off advancements ignore the new copper blocks
- severity: data
- vb: VB has no override of `data/minecraft/advancement/husbandry/wax_on.json` or `wax_off.json`, so vanilla 1.21.1 lists only 1.21.1 copper blocks.
- native: the `husbandry/wax_on.json` `item_used_on_block` location_check `blocks` also lists `copper_bars…oxidized_copper_bars, copper_chest…, copper_golem_statue…, lightning_rod, exposed/weathered/oxidized_lightning_rod, copper_lantern…, copper_chain…`. `wax_off.json` lists the 24 `waxed_*` counterparts.
- difference: Waxing or scraping VB copper chests, statues, bars, chains, lanterns or weathered rods does not grant "Wax On" or "Wax Off".
- fix_kind: data-override (`data/minecraft/advancement/husbandry/wax_on.json`, `wax_off.json`)
- fix: copy the native 26.2 files verbatim. Their format (criteria `minecraft:item_used_on_block`, `location` list with `location_check` + `match_tool`) is valid in 1.21.1, and every block id in them exists with VB loaded.
- config_gate: none

### COPPER-18  Piglins are not angered by copper chests
- severity: data
- vb: VB ships no `data/minecraft/tags/block/guarded_by_piglins.json`.
- native: `tags/block/guarded_by_piglins.json`: `["#minecraft:copper_chests","minecraft:gold_block","minecraft:barrel","minecraft:chest",…]`
- difference: Natively, opening or breaking a copper chest near piglins angers them. In VB it does not.
- fix_kind: data-override (`data/minecraft/tags/block/guarded_by_piglins.json`)
- fix: `{"replace": false, "values": ["#minecraft:copper_chests"]}`
- config_gate: none

### COPPER-19  Mob armor tier roll and slot order differ; copper is not its own tier
- severity: gameplay
- vb: `core/mixin/common/equipment/MobMixin.java` wraps `getEquipmentForSlot`. `CommonSetup.java:76-79`: `SpawnEquipmentRegistry.register(EquipmentSlot.HEAD, 1, 0.4F, ModItems.COPPER_HELMET)` (and the same for CHEST, LEGS, FEET). That is a per-UUID 40% swap of tier 1 (gold) to copper inside the 1.21.1 roll `int i = random.nextInt(2); 3× if (nextFloat() < 0.095F) i++;`, which iterates `EquipmentSlot.values()` (FEET→HEAD).
- native: `world/entity/Mob.java:975-1003`: `int armorType = random.nextInt(3); for (i=1..3) if (random.nextFloat() < 0.1087F) armorType++;` iterating `EQUIPMENT_POPULATION_ORDER = HEAD, CHEST, LEGS, FEET`. `getEquipmentForSlot` gives 0 leather, 1 copper, 2 gold, 3 chain, 4 iron, 5 diamond.
- difference: The resulting per-mob tier distributions are leather/copper/gold/chain/iron/diamond = native 23.6/32.2/33.3/9.7/1.1/0.04 % against VB 37.1/19.5/29.2/12.9/1.3/0.04 %. The partial-set cutoff also starts from the helmet natively and from the boots in VB.
- fix_kind: mixin-mc (`Mob.populateDefaultEquipmentSlots`)
- fix: `@Inject(method="populateDefaultEquipmentSlots", at=@At("HEAD"), cancellable=true)` with the native body (lines 975-1003). Use the slot order `List.of(HEAD, CHEST, LEGS, FEET)` and a local `getEquipmentForSlot26(slot, type)` returning leather/`ModItems.COPPER_*`/golden/chainmail/iron/diamond for types 0–5. When `has_copper_toolset` is false, fall back to the original call. Leave VB's `SpawnEquipmentRegistry` unused for armor (its tier-1 swap is then never reached because the mixin cancels).
- config_gate: has_copper_toolset

### COPPER-20  Copper trim on copper armor does not use the darker palette
- severity: visual
- vb: `assets/minecraft/models/item/copper_helmet_copper_trim.json`: `"layer1": "minecraft:trims/items/helmet_trim_copper"` (and the same for chestplate, leggings and boots). The vanilla 1.21.1 `trim_material/copper.json` has no `override_armor_materials` for VB's `minecraft:copper` armor material. `SpriteSourceListMixin` adds only the `resin` permutation, and VB ships no `trims/color_palettes/copper_darker.png`.
- native: `item/equipment/trim/MaterialAssetGroup.java:40`: `COPPER = create("copper", Map.of(EquipmentAssets.COPPER, "copper_darker"))`. `models/item/copper_helmet_copper_trim.json`: `"layer1": "minecraft:trims/items/helmet_trim_copper_darker"`. The palette is `textures/trims/color_palettes/copper_darker.png`.
- difference: Natively, a copper trim on copper armor uses the darker copper palette, both on the item icon and on the worn layer, so it stays visible. In VB it uses the normal copper palette and blends into the armor.
- fix_kind: data-override + asset-override
- fix:
  1. `data/minecraft/trim_material/copper.json` = `{"asset_name":"copper","description":{"color":"#B4684D","translate":"trim_material.minecraft.copper"},"ingredient":"minecraft:copper_ingot","item_model_index":0.5,"override_armor_materials":{"minecraft:copper":"copper_darker"}}`. The 11823181 colour is #B4684D.
  2. Add `assets/minecraft/textures/trims/color_palettes/copper_darker.png` (from 26.2).
  3. `assets/minecraft/atlases/armor_trims.json` and `atlases/blocks.json`: add a `paletted_permutations` source with the same `textures` lists as vanilla (`trims/models/armor/*` and `trims/items/*_trim`), `palette_key: "minecraft:trims/color_palettes/trim_palette"` and `permutations: {"copper_darker": "minecraft:trims/color_palettes/copper_darker"}`. 1.21.1 merges atlas files from every pack.
  4. In the 4 `models/item/copper_*_copper_trim.json`, set `layer1` to `minecraft:trims/items/<piece>_trim_copper_darker`.
- config_gate: has_copper_toolset

### COPPER-21  Copper spear dismount threshold is 9.0 (native 12.0)
- severity: gameplay
- vb: `common/registries/items/ModItems.java:192`: `new SpearItem(ModToolMaterials.COPPER, 0.85F, 0.82F, 0.65F, 4.0F, 9.0F, 8.25F, 5.1F, 12.5F, 4.6F, properties)`
- native: `world/item/Items.java:1666`: `.spear(ToolMaterial.COPPER, 0.85F, 0.82F, 0.65F, 4.0F, 12.0F, 8.25F, 5.1F, 12.5F, 4.6F)`. Parameter 5 is `dismountThreshold`, giving `KineticWeapon.Condition.ofAttackerSpeed((int)(dismountTime*20), dismountThreshold)` (`Item.java:497,512`).
- difference: The speed needed for a copper-spear charge to dismount a rider is 9.0 in VB and 12.0 natively. Stone, iron, gold, diamond and netherite are off by the same +3 (wood matches). That belongs to the spear area.
- fix_kind: mixin-vb (`ModItems` static init / `SpearItem.<init>`)
- fix: `@ModifyArg` on the `SpearItem` constructor call for `copper_spear` (index 5 → `12.0F`). The alternative is a mixin on `SpearItem.<init>` that corrects arg 5 when `tier == ModToolMaterials.COPPER`.
- config_gate: has_spears + has_copper_toolset

### COPPER-22  The iron-golem flower gift runs as two goals with independent rolls
- severity: gameplay
- vb: `common/integrations/MobIntegrations.java:75`: `event.registerGoal(EntityType.IRON_GOLEM, 5, mob -> new OfferCopperGolemFlowerGoal((IronGolem)mob));` (targets `#accepts_iron_golem_gift` only, own `nextInt(8000)`), next to the vanilla 1.21.1 `OfferFlowerGoal` (villagers only, own `nextInt(8000)`).
- native: `ai/goal/OfferFlowerGoal.java`: a single goal, `getNearestEntity(EntityTypeTags.CANDIDATE_FOR_IRON_GOLEM_GIFT, …)` where `candidate_for_iron_golem_gift = ["minecraft:villager","#minecraft:accepts_iron_golem_gift"]`, with one `nextInt(8000)` roll.
- difference: With villagers and copper golems both in range, VB rolls twice per tick, so offers happen about twice as often. Native offers to whichever candidate is nearest, so a villager closer than the golem gets the flower and the golem does not. The 400-tick duration, 6-block range and poppy-into-antenna behaviour match.
- fix_kind: mixin-mc (`OfferFlowerGoal.canUse`) + mixin-vb (`OfferCopperGolemFlowerGoal.canUse`)
- fix: in `OfferFlowerGoal.canUse`, `@WrapOperation` the `Level.getNearestEntity(Class<Villager>, …)` call to a nearest search over `LivingEntity` filtered to `type == VILLAGER || type.is(ModEntityTypeTags.ACCEPTS_IRON_GOLEM_GIFT)` (same `OFFER_TARGET_CONTEXT`, AABB inflate 6/2/6). Port VB's `stop()` antenna logic into `OfferFlowerGoal.stop` (`@Inject` TAIL before `entity = null`; use `@Shadow entity`). Make `OfferCopperGolemFlowerGoal.canUse` return false.
- config_gate: has_copper_golems

### COPPER-23  Copper golem (unaffected/exposed) footstep audio is the older recording
- severity: audio
- vb: `assets/minecraft/sounds/mob/coppergolem/regular/step1.ogg` … `step9.ogg`. All 9 sha1s differ from the 26.2 asset index (the other 119 copper-area .ogg files match byte for byte, and the `sounds.json` event definitions match).
- native: asset index 32 `minecraft/sounds/mob/coppergolem/regular/step{1..9}.ogg` (event `entity.copper_golem.step`)
- difference: The step sounds for unaffected and exposed golems are a different (pre-26.x) take. The weathered and oxidized steps are identical to native.
- fix_kind: asset-override (`assets/minecraft/sounds/mob/coppergolem/regular/step1-9.ogg`)
- fix: ship the 9 files from the 26.2 asset index (look up each hash in index 32 under `minecraft/sounds/mob/coppergolem/regular/stepN.ogg`). No `sounds.json` change is needed.
- config_gate: has_copper_golems

### COPPER-24  Missing fall subtitles for statues and shelves
- severity: cosmetic
- vb: `assets/minecraft/sounds.json` `"block.copper_golem_statue.fall": {"sounds":[hit1..hit4]}` and `"block.shelf.fall": {"sounds":[chiseled_bookshelf/step1..5]}`, both without a `subtitle`.
- native: the same events with `"subtitle": "subtitles.block.generic.fall"`.
- difference: Falling onto a statue or a shelf shows no subtitle in VB.
- fix_kind: asset-override (`assets/minecraft/sounds.json`)
- fix: `{"block.copper_golem_statue.fall":{"replace":true,"sounds":["block/copper_statue/hit1","block/copper_statue/hit2","block/copper_statue/hit3","block/copper_statue/hit4"],"subtitle":"subtitles.block.generic.fall"},"block.shelf.fall":{"replace":true,"sounds":["block/chiseled_bookshelf/step1","block/chiseled_bookshelf/step2","block/chiseled_bookshelf/step3","block/chiseled_bookshelf/step4","block/chiseled_bookshelf/step5"],"subtitle":"subtitles.block.generic.fall"}}`
- config_gate: none

### COPPER-25  Antenna: wrong slot, a small render offset, and no head layer
- severity: cosmetic
- vb: `CopperGolem.java:81`: `EQUIPMENT_SLOT_ANTENNA = EquipmentSlot.HEAD`. `client/level/renderer/entity/mob/CopperGolemRenderer.java:52`: `poseStack.translate(0.0, 1.5, 0.0)`. `CopperGolemModel.java:290`: `poseStack.translate(0.0, -2.25, 0.0)`. `client/level/layer/BlockDecorationLayer.java:49-51`: `mulPose(ZP 180); translate(-0.5F, -1.45F, -0.5F)`, rendering `block.getBlock().defaultBlockState()`. There is no `CustomHeadLayer`.
- native: `CopperGolem.java:89`: `EQUIPMENT_SLOT_ANTENNA = EquipmentSlot.SADDLE`. `CopperGolemModel` root `translated(0,24,0)` and `applyBlockOnAntennaTransform … translate(0.0, -1.75, 0.0)`. `BlockDecorationLayer.java:17-19`: `translation(-0.5,0,-0.5).rotateAround(ZP 180, .5,.5,.5)`, using the item's `BLOCK_STATE` component. `CopperGolemRenderer` adds `CustomHeadLayer`, and `translateToHead` translates 0.125 and scales 1.0625.
- difference: The antenna block (poppy) renders 0.05 block (0.8 px) higher in VB, net y −0.75+0.45 = −0.30 against native −0.25. VB also ignores the item's block-state component. The head slot is the antenna in VB, so a carved pumpkin or skull on a golem renders on the antenna instead of as a head item. 1.21.1 has no SADDLE slot (engine).
- fix_kind: mixin-vb (`client.level.model.entity.CopperGolemModel.applyBlockOnAntennaTransform`) + engine (slot)
- fix: `@ModifyConstant(method="applyBlockOnAntennaTransform", constant=@Constant(doubleValue=-2.25))` → `-2.2` (gives net −0.25). In `BlockDecorationLayer` the block supplier could apply `stack.get(DataComponents.BLOCK_STATE).apply(defaultState)`. Moving the antenna off HEAD is not cleanly possible on 1.21.1.
- config_gate: has_copper_golems

### COPPER-26  Statue renders without backface culling
- severity: visual
- vb: `client/level/renderer/block_entity/CopperGolemStatueRenderer.java:40`: `RenderType.entityCutoutNoCull(...)`. `CopperGolemStatueModel.java:17`: `super(RenderType::entityCutoutNoCull)`.
- native: `client/model/object/statue/CopperGolemStatueModel.java`: `super(root, RenderTypes::entityCutout)`. The renderer submits with the model's render type.
- difference: VB draws the back faces of the statue cubes, so inner faces show through gaps and z-fighting is possible. Native culls them.
- fix_kind: mixin-vb (`CopperGolemStatueRenderer.render`)
- fix: `@Redirect` the `RenderType.entityCutoutNoCull(ResourceLocation)` call to `RenderType.entityCutout(id)`.
- config_gate: none

### COPPER-27  Shelf item placement uses FIXED plus a scale heuristic, not the on_shelf context
- severity: cosmetic
- vb: `client/level/renderer/block_entity/ShelfRenderer.java:49-53,68-71`: `mulPose(YP yRot); mulPose(YP 180); translate(-slotOffsetX, align ? -0.25F : 0.0F, 0.27F); scale(0.25F)`, with `renderStatic(stack, ItemDisplayContext.FIXED, …)` and `calculateOptimalScale` (up to ×2).
- native: `blockentity/ShelfRenderer.java` submitItem: `translate(itemSlotPosition, align ? -0.25 : 0.0, -0.25); scale(0.25F)`, with `ItemDisplayContext.ON_SHELF` (the per-model `on_shelf` transform, falling back to `fixedFromBottom`).
- difference: Items sit 0.02 block further out (z 0.27 against 0.25). Plain generated items and full blocks end up with the same size and rotation as native. Items with a custom `on_shelf` transform (heads, shields, beds, anvils, fences, walls, copper golem statues) and builtin-entity items are sized by VB's bounding-box heuristic instead of their native transform. 1.21.1 has no ON_SHELF context (engine).
- fix_kind: mixin-vb (`ShelfRenderer.render`) + engine
- fix: `@ModifyConstant(method="render", constant=@Constant(floatValue=0.27F))` → `0.25F`. Exact `on_shelf` transforms are not reproducible without a new display context.
- config_gate: none

### COPPER-28  Copper wall torch shows a raw translation key
- severity: cosmetic
- vb: `ModBlocks.java:939`: `new LazyWallTorchBlock(particle, properties.dropsLike(torch.get()))`. The 1.21.1 `dropsLike` does not copy the description, and VB's lang has no `block.minecraft.copper_wall_torch`.
- native: `Blocks.java:2070-2073` `wallVariant(COPPER_TORCH, true)` → `overrideDescription(standingBlock.getDescriptionId())` (shows "Copper Torch").
- difference: Block-name displays (WAILA/Jade, `/setblock` feedback, the hovered-block HUD) show `block.minecraft.copper_wall_torch` in VB and "Copper Torch" natively.
- fix_kind: asset-override (`assets/minecraft/lang/en_us.json`)
- fix: `{"block.minecraft.copper_wall_torch": "Copper Torch"}`
- config_gate: none

### COPPER-29  Statue items have no default pose component, so creative and dropped statues don't stack
- severity: data
- vb: `ModBlocks.java` statues are registered via `REGISTRIES.register("copper_golem_statue", () -> new WeatheringCopperGolemStatueBlock(...))`, which gives a default `BlockItem` with plain `new Item.Properties()`. The loot table (`loot_table/blocks/copper_golem_statue.json`) adds `copy_state copper_golem_pose`.
- native: `Items.java:2048-2056`: `new Item.Properties().component(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, Pose.STANDING))`
- difference: Natively every statue item carries `{copper_golem_pose: standing}` by default, so a creative or crafted statue stacks with a mined standing statue. In VB they have different components and do not stack.
- fix_kind: mixin-mc (NeoForge `ModifyDefaultComponentsEvent`, add-on side)
- fix: in the add-on, `ModifyDefaultComponentsEvent` → for the 8 `*copper_golem_statue` items, `builder.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.STANDING))`.
- config_gate: none

### COPPER-30  Recipe-book groups for waxed chest/statue/rod recipes differ
- severity: cosmetic
- vb: for example `data/minecraft/recipe/waxed_exposed_copper_chest_from_honeycomb.json` `"group": "waxed_exposed_copper_chest"`, and likewise `waxed_weathered_*` and `waxed_oxidized_*` for chests, statues and lightning rods (9 files).
- native: `"group": "waxed_copper_chest"`, `"waxed_copper_golem_statue"` and `"waxed_lightning_rod"` for all four oxidation stages.
- difference: Natively the recipe book shows one stacked entry per family. VB shows separate entries.
- fix_kind: data-override (those 9 recipe files)
- fix: copy each VB file and change `group` to `waxed_copper_chest`, `waxed_copper_golem_statue` or `waxed_lightning_rod` respectively.
- config_gate: none

### COPPER-31  Copper chain, lantern and torch recipes accept any `#c:nuggets/copper`
- severity: data
- vb: `recipe/copper_chain.json` `"N": {"tag":"c:nuggets/copper"}`, `copper_lantern.json` `"X": {"tag":"c:nuggets/copper"}` and `copper_torch.json` `"C": {"tag":"c:nuggets/copper"}`. The advancements `recipes/decorations/copper_chain|copper_torch.json` use the `#c:nuggets/copper` trigger.
- native: `"minecraft:copper_nugget"` in all three, and the advancement triggers on `minecraft:copper_nugget`.
- difference: With other mods' copper nuggets present, VB accepts them and native does not. Vanilla-only play is identical.
- fix_kind: data-override (the 3 recipes + 2 advancements)
- fix: replace the tag ingredient with `{"item":"minecraft:copper_nugget"}` and the advancement item predicate with `{"items":"minecraft:copper_nugget"}`.
- config_gate: has_copper_nuggets

### COPPER-32  Golem readyForShearing also requires isAlive
- severity: cosmetic
- vb: `CopperGolem.java:429-431`: `return this.isAlive() && this.getItemBySlot(EQUIPMENT_SLOT_ANTENNA).is(ModItemTags.SHEARABLE_FROM_COPPER_GOLEM);`
- native: `CopperGolem.java:437-440`: `return this.getItemBySlot(EQUIPMENT_SLOT_ANTENNA).is(ItemTags.SHEARABLE_FROM_COPPER_GOLEM);`
- difference: This only matters during the death animation, when a dispenser or player can still shear the poppy natively but not in VB. VB's tag file also lists `minecraft:poppy` twice (harmless).
- fix_kind: mixin-vb (`CopperGolem.readyForShearing`)
- fix: `@ModifyExpressionValue(method="readyForShearing", at=@At(value="INVOKE", target="…CopperGolem;isAlive()Z")) boolean f(boolean b){ return true; }`
- config_gate: has_copper_golems

### COPPER-33  Area loot tables have no `random_sequence`
- severity: cosmetic
- vb: for example `loot_table/blocks/copper_chest.json` and `entities/copper_golem.json`, which have no `"random_sequence"` key (affects every copper_*/shelf/lightning-rod/statue block table and the golem).
- native: `"random_sequence": "minecraft:blocks/copper_chest"`, `"minecraft:entities/copper_golem"`, and so on.
- difference: Drop counts (golem 1–3 copper ingots plus looting 0–1) and conditions are identical. Only the RNG stream differs: native drops use a seeded per-table sequence, so the same seed gives the same drops.
- fix_kind: data-override (each table)
- fix: add `"random_sequence": "minecraft:<blocks|entities>/<id>"` to each VB table in this area (the content is otherwise unchanged).
- config_gate: none
