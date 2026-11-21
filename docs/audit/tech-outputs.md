# Technical audit — axis `outputs` (everything a farm collects)

Summary
- 7 findings (3 `farm`, 4 `data`) plus 3 engine-drift notes. The farm findings are all loot pools that native 26.2 added to tables VB never touches: the red mushroom from zombies riding zombie horses, the rabbit foot from husks riding camel husks, and the larger zombie-horse rotten flesh roll.
- Checked against VB 1.2.2 with the add-on's merged fixes: all 229 effective VB/add-on loot tables (225 match native after normalising format, including 89 tables checked against 26.3), the code-side loot modifiers (`LootIntegrations` and `GhastParity`), the XP paths, egg laying, creaking resin, copper golem outputs and transport, iron-golem flower gifts, sulfur cube intake/split/shear/bucket, happy ghast harness drops, dried ghast hydration, every bone-meal / harvest path, composter chances, fuel, smelting, wandering-trader trades, item entities, and copper chest / shelf container behaviour.
- First-round fixes re-verified as correct where they touch outputs: GARDEN-01 (resin), VARIANTS-18 (egg laying), FLORA-04 (cactus ageing and flower roll), COPPER-15 / WILD-27 / WILD-31 (fuel), WILD-19 (straw bed compost), GHAST-01 (harness drop), GHAST-30 (barter order), COPPER-22 (flower gift), MISC-35 (lodestone pool), SULFUR-01/02/07 (split and drop point).
- Not checked: hopper / hopper-minecart engine drift between 1.21.1 and 26.2 for vanilla containers (only VB containers were compared), and tag membership (another axis covers it). The 26.3 loot tables were compared through a format translator (`modifier`→`functions`, `condition`→`conditions`, `match_block`→`block_state_property`, `tool/can_shear`→`match_tool shears`).

## Coverage

| Mechanic | Native reference | Result |
|---|---|---|
| Entity loot tables: copper_golem, creaking, happy_ghast, sulfur_cube, camel_husk, parched, zombie_nautilus | `src-mc/data/minecraft/loot_table/entities/*.json` | match (chances and counts identical; parched tipped arrow `limit 1`, zombie nautilus `killed_by_player` 0–3 + looting 0–1, copper golem 1–3 + looting 0–1) |
| Nautilus loot (shell 5%, +1%/looting level above first, base 6%, killed_by_player) | `entities/nautilus.json` | match (VB has the conditions on the entry, native on the pool; with `rolls 1` and one entry the result and random calls are the same) |
| Zombie table (VB adds Lava Chicken disc pool) | `entities/zombie.json` | disc pool matches (same last position and conditions); **OUT-01** (red mushroom pool missing) |
| Husk table | `entities/husk.json` | **OUT-02** |
| Zombie horse table | `entities/zombie_horse.json` | **OUT-03** |
| Ghast table (VB adds Tears disc pool) | `entities/ghast.json` pool 2 | match (fireball + is_projectile damage source, killed_by_player, pool-level set_count 1) |
| Piglin bartering (dried ghast w10 ×1) | `gameplay/piglin_bartering.json` index 7 | match (GHAST-30 fix moves it after the ender pearl) |
| chicken_lay gift table | `gameplay/chicken_lay.json` | match (VB uses an NBT `variant` predicate, native a component predicate; same three outcomes) |
| Block loot (136 tables vs 26.2, 89 vs 26.3): silk touch, shears, fortune `table_bonus` [0.05,0.0625,0.083333,0.1] / sticks [0.02,0.022222,0.025,0.033333,0.1], explosion decay, slab doubles, door / bed halves, shelf mushroom age 1 → 2 | `loot_table/blocks/*` | match |
| Loot table `random_sequence` | every native table | **OUT-07** (165 tables) |
| Chest additions: nautilus armour pools (empty 148 / copper 20 / iron 10 / gold 5 / diamond 2), bundle pools (1:2), mansion resin w50 ×2–4, mineshaft Bounce disc w10 in sulfur caves, buried-treasure iron spear, weaponsmith spears w5/w7, end city diamond spear w3 lvl 20–39, bastion diamond spear 2×w6, copper horse armour weights 15/1/1/5/1/1/15, lodestone (MISC-35 fix) | `loot_table/chests/*` | weights and pools match; **OUT-04** (ruins spear type), **OUT-06** (entry positions) |
| Camp barrel / common / secret chests (26.3) | `src-mc263 .../barrels`, `chests` | pools, rolls and weights match; map entries are covered by first-round WILD-10..13 |
| Creaking resin: trigger (player-blamed hit on a bound creaking, `emitter <= 0`, AWAKE heart), clumps `nextIntBetweenInclusive(2,3)`, BFS depth 2 / 64 nodes over `#pale_oak_logs`, neighbour shuffle then face shuffle, stop at first face, air or water-source targets, RESIN_PLACE 1.0/1.0 + BLOCK_PLACE, 100-tick emitter | `CreakingHeartBlockEntity.creakingHurt/spreadResin`, `BlockPos.breadthFirstTraversal`, `Creaking.hurtServer` | match (GARDEN-01 fix correct) |
| Creaking heart XP 20–24 (natural heart, non-creative, block-destroying explosion), comparator | `CreakingHeartBlock.tryAwardExperience` | match |
| Egg laying: `--eggTime <= 0` on server, alive, adult, not a jockey; gift table; sound only on a drop; `nextInt(6000)+6000` | `Chicken.aiStep` | match (VARIANTS-18 fix correct) |
| Thrown egg: 1/8, 1/32 → 4, variant from the item component; blue/brown eggs in dispensers | `ThrownEgg.onHit`, `DispenseItemBehavior` | match |
| Mob XP: creaking 0, copper golem 0, parched 5 + 1–3 per equipped item, Animal 1–3 (happy ghast, nautilus, zombie nautilus, camel husk), sulfur cube adult `1+nextInt(2)`, baby 0 | `Mob/Animal/SulfurCube.getBaseExperienceReward` | match in values; **ED-2** (random source) |
| Breeding XP `nextInt(7)+1`; happy ghast `canFallInLove false`; nautilus offspring inherits tame owner; sulfur cube offspring size 1 | `Animal.finalizeSpawnChildFromBreeding`, `Nautilus/SulfurCube.getBreedOffspring` | match |
| Baby drops: no loot when `isBaby()` | `LivingEntity.shouldDropLoot` | match (1.21.1 checks the gamerule in `dropAllDeathLoot` instead; same result) |
| Copper golem: shearing antenna drop at 1.5, held transport stack guaranteed drop, `dropPreservedEquipment` on statue conversion, weathering 504000–552000 ticks, statue chance 0.0058 on air with a random pose, leash drop gated by entityDrops | `CopperGolem` | match (antenna slot HEAD vs SADDLE is first-round COPPER-25; the poppy is guaranteed in both) |
| Copper golem transport: takes `min(count,16)` from the first non-empty slot, fills the first empty slot or same-item stacks, 140-tick idle cooldown, 60-tick interaction | `TransportItemsBetweenContainers.pickupItemFromContainer/addItemsToContainer` | match (`golem_item_carry_amount` default 16) |
| Iron golem flower: daylight, `nextInt(8000)`, 6-block range, nearest of villager / `#accepts_iron_golem_gift`, 400 ticks, poppy guaranteed on the copper golem | `OfferFlowerGoal` | match (COPPER-22 fix correct: one goal, one roll) |
| Sulfur cube: swallow tag, one item, pickup timer 100 after shear, 8-block item search, baby food tag, split into 2 babies of half size, guaranteed drop of the swallowed block, bucket item | `SulfurCube`, `AbstractCubeMob.remove` | match apart from first-round SULFUR-14/18 (engine); native's `canAgeUp()` age-lock check has no 1.21.1 counterpart |
| Happy ghast harness drop | `Equippable` guaranteed drop | match (GHAST-01 fix: drop chance 2.0) |
| Dried ghast: random tick schedules 5000-tick tick when waterlogged or hydration > 0; +1 per tick in water up to 3, then a baby ghastling facing the block; −1 when dry | `DriedGhastBlock.tick/randomTick/spawnGhastling` | match |
| Bone meal: bush, firefly bush, tall dry grass (horizontal shuffled neighbour spread), short → tall dry grass, pale hanging moss (tip down), pale moss (`pale_moss_patch_bonemeal` feature, identical JSON), mossy carpet topper, wildflowers (+1 amount, pop item at 4), shelf mushroom age 0→1; leaf litter and cactus flower are not bone-mealable in either | `BushBlock`, `FireflyBushBlock`, `TallDryGrassBlock`, `ShortDryGrassBlock`, `HangingMossBlock`, `BonemealableFeaturePlacerBlock`, `MossyCarpetBlock`, `FlowerBedBlock`, 26.3 `ShelfMushroomBlock` | match (GARDEN-18 is first-round) |
| Cactus flower growth: age 8 top, 10% (height < 3) / 25% (height ≥ 3) `nextDouble`, ages in the same tick, 3+ tall tops age to 15 | `CactusBlock.randomTick` | match (FLORA-04 fix correct) |
| Composter chances (0.3 / 0.65 / 0.85) for all 21 VB items | 26.2 `ComposterBlock.bootStrap`; 26.3 `Items` `compostable(...)` | match (WILD-19 fixed); **ED-3** |
| Furnace fuel | 26.2 `FuelValues.vanillaBurnTimes`; 26.3 `cooking/*` providers | dry grass / leaf litter 100, shelves 300, wool stairs 100, wool slabs and cushions 50 match; **OUT-05** (wooden spear) |
| Smelting / blasting: leaf litter (#leaves, 0.1 xp), resin brick (0.1), copper / iron / gold nuggets from tools and armour (0.1, 200 / 100 ticks) | `recipe/*.json` | match (spear blasting time is first-round MAYHEM-42) |
| Wandering trader: pale oak log 1→8 ×4, eyeblossom 1→1 ×7, pale oak sapling 5→1 ×8, hanging moss 1→3 ×4, pale moss 1→2 ×5, wildflowers / tall dry grass 1→1 ×12, firefly bush 3→1 ×12, sulfur spike 1→2 ×5, poplar log 1→8 ×4, poplar sapling 5→1 ×8, shelf mushroom 1→3 ×12; all xp 1, multiplier 0.05 | `villager_trade/wandering_trader/*.json` (codec defaults `max_uses 4`, `xp 1`) | match in values; **ED-1** (pool structure) |
| Villager profession trades with VB items | 26.2 `villager_trade/<profession>` | none reference VB items |
| Item entities: lifetime 6000, merge box 0.5/0/0.5, merge rate 2/40 ticks, pickup delays; fire resistance of netherite spear / horse armour / nautilus armour | `ItemEntity`, `Items` | match |
| Copper chest as container: `ChestBlock` subclass, vanilla chest block entity, comparator from `getContainer`, double-chest pairing | 26.2 `CopperChestBlock` | match (pairing on placement is first-round COPPER-06) |
| Shelf as container: 3 slots, `ListBackedContainer` defaults (canPlaceItem, removeItemNoUpdate, limit to max stack), comparator bits 1/2/4 from the back face only, single swap / powered hotbar swap slot formula `9 - (n - i) * 3 + slot` | `ShelfBlockEntity`, `ListBackedContainer`, `ShelfBlock` | match (COPPER-13 fixed) |

## Findings

### OUT-01  Zombies riding zombie horses never drop red mushrooms
- severity: farm
- vb: `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/common/integrations/LootIntegrations.java:87-101` (modify) — the only change VB makes to `entities/zombie` is to append the Lava Chicken pool:
  `if (key.equals(EntityType.ZOMBIE.getDefaultLootTable()) && ...hasLavaChickenMusicDisc.get()) { context.addPool(LootPool.lootPool().add(LootItem.lootTableItem(ModItems.MUSIC_DISC_LAVA_CHICKEN.get())).when(killedByPlayer()).when(... setIsBaby(true) ... vehicle(... of(EntityType.CHICKEN))));`
  1.21.1 base table, `<mc-1.21.1>/net/minecraft/data/loot/packs/VanillaEntityLoot.java:1035-1055`: rotten flesh pool, then the iron ingot / carrot / potato pool. There is no add-on override (`src/main/resources/data/minecraft/loot_table/entities/` has none for zombie). Meanwhile VB makes every naturally spawned zombie horse carry a zombie: `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/core/mixin/common/zombie_horses/ZombieHorseMixin.java:153-159` (`Zombie zombie = EntityType.ZOMBIE.create(...); ... zombie.startRiding(this, false)`).
- native: `<mc-26.2>/data/minecraft/loot_table/entities/zombie.json` pool index 1 (between the flesh pool and the rare-drop pool):
  `{"entries":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"minecraft:vehicle":{"minecraft:entity_type":"minecraft:zombie_horse"}}}],"functions":[{"count":{"type":"minecraft:uniform","max":1.0,"min":0.0},"function":"minecraft:set_count"},{"count":{"type":"minecraft:uniform","max":1.0,"min":0.0},"enchantment":"minecraft:looting","function":"minecraft:enchanted_count_increase"}],"name":"minecraft:red_mushroom"}],"rolls":1.0}`
  The riders come from `<mc-26.2>/net/minecraft/world/entity/animal/equine/ZombieHorse.java:137-144` (every NATURAL zombie horse spawns a zombie rider).
- difference: in native, a zombie killed while riding a zombie horse drops 0–1 red mushroom, +0–1 per Looting level (uniform 0..1 per level, so Looting III gives 0–4). In VB it drops none. The pool also uses two random draws (set_count, then looting) before the iron/carrot/potato pool, so the rare-drop roll comes from a different point in the sequence. Zombies that aren't riding a horse are unaffected (the pool has no candidate entries and draws nothing).
- fix_kind: data-override (runtime pool insertion)
- fix: in the add-on's game-bus `LootTableLoadEvent` handler (same place as `GhastParity.GameEvents.onLootTableLoad`, priority LOW so that it runs after VB/Platform), when `event.getName().equals(EntityType.ZOMBIE.getDefaultLootTable().location())`, insert at pool index 1 (`((com.blackgear.platform.core.mixin.neoforge.access.LootTableAccessor) event.getTable()).getPools().add(1, pool)`, the accessor `GhastParity` already uses):
  ```java
  LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
      .add(LootItem.lootTableItem(Items.RED_MUSHROOM)
          .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
              EntityPredicate.Builder.entity().vehicle(EntityPredicate.Builder.entity().of(EntityType.ZOMBIE_HORSE))))
          .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
          .apply(EnchantedCountIncreaseFunction.lootingMultiplier(event.getRegistries(), UniformGenerator.between(0.0F, 1.0F))))
      .build()
  ```
  If the table already has more than one pool, insert at index 1, so the Lava Chicken pool VB appends stays last, as it is natively.
- config_gate: `has_zombie_horses`

### OUT-02  Husks riding camel husks never drop rabbit's feet
- severity: farm
- vb: no change to `entities/husk` anywhere (`LootIntegrations.java` has no HUSK branch, and VB data has no `entities/husk.json`), so the 1.21.1 table applies: `<mc-1.21.1>/net/minecraft/data/loot/packs/VanillaEntityLoot.java:396-416` — `HUSK` = rotten flesh 0–2 (+0–1 looting), then the iron ingot / carrot / potato pool (killed_by_player, 0.025 + 0.01/level). Camel-husk jockeys exist (`ZombieJockeyMixin` / GHAST-43 path, `has_camel_husks`).
- native: `<mc-26.2>/data/minecraft/loot_table/entities/husk.json` pool index 1:
  `{"entries":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"minecraft:vehicle":{"minecraft:entity_type":"minecraft:camel_husk"}}}],"functions":[{"count":{"type":"minecraft:uniform","max":1.0,"min":0.0},"function":"minecraft:set_count"},{"count":{"type":"minecraft:uniform","max":1.0,"min":0.0},"enchantment":"minecraft:looting","function":"minecraft:enchanted_count_increase"}],"name":"minecraft:rabbit_foot"}],"rolls":1.0}`; the riders come from `<mc-26.2>/net/minecraft/world/entity/monster/zombie/Husk.java:106-127` (10% `nextFloat() < 0.1F` once per natural group).
- difference: native camel-husk riders drop 0–1 rabbit's foot (+0–1 per Looting level); VB riders drop none. The same random-order shift as OUT-01 applies to the rare-drop pool.
- fix_kind: data-override (runtime pool insertion)
- fix: same handler as OUT-01, for `EntityType.HUSK.getDefaultLootTable()`: insert at index 1 a pool with `LootItem.lootTableItem(Items.RABBIT_FOOT)`, condition vehicle `of(ModEntityTypes.CAMEL_HUSK.get())`, `setCount(between(0,1))`, `lootingMultiplier(between(0,1))`.
- config_gate: `has_camel_husks`

### OUT-03  Zombie horses drop 0–2 rotten flesh instead of 2–3
- severity: farm
- vb: no override; the 1.21.1 table applies, `<mc-1.21.1>/net/minecraft/data/loot/packs/VanillaEntityLoot.java:1056-1067`:
  `EntityType.ZOMBIE_HORSE ... LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries, UniformGenerator.between(0.0F, 1.0F)))`
  VB makes zombie horses spawn naturally as monsters (`MobIntegrations.java:51`, `zombie_horses/EntityTypeMixin.java:24-25`).
- native: `<mc-26.2>/data/minecraft/loot_table/entities/zombie_horse.json`: `"count":{"type":"minecraft:uniform","max":3.0,"min":2.0}` + looting `uniform 0..1`.
- difference: mean flesh per horse without Looting is 1.0 in VB and 2.5 natively (uniform floats rounded down: VB 0/1/2, native 2/3). A zombie-horse mob farm collects 2.5 times less flesh.
- fix_kind: data-override `data/minecraft/loot_table/entities/zombie_horse.json`
- fix:
  ```json
  {"type":"minecraft:entity","pools":[{"rolls":1.0,"bonus_rolls":0.0,"entries":[{"type":"minecraft:item","name":"minecraft:rotten_flesh","functions":[
    {"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":2.0,"max":3.0},"add":false},
    {"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting","count":{"type":"minecraft:uniform","min":0.0,"max":1.0}}]}]}],
   "random_sequence":"minecraft:entities/zombie_horse"}
  ```
- config_gate: none (the native table applies whatever the spawn setting; gate it on `has_zombie_horses` only if 1.21.1 zombie horses should keep their old drops when VB's zombie horses are off)

### OUT-04  Underwater ruins hold an iron spear where native has a stone spear
- severity: data
- vb: `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/common/integrations/LootIntegrations.java:263-268`
  `if (key.equals(BuiltInLootTables.UNDERWATER_RUIN_BIG) || key.equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL)) { context.addToPool(new ...[]{ LootItem.lootTableItem((ItemLike)ModItems.IRON_SPEAR.get()).setWeight(2) }); }` (pool 0, appended last)
- native: `<mc-26.2>/data/minecraft/loot_table/chests/underwater_ruin_big.json` pool 0 `[coal 10, gold_nugget 10, emerald 1, stone_spear 2, wheat 10]`; `underwater_ruin_small.json` pool 0 `[coal 10, stone_axe 2, stone_spear 2, rotten_flesh 5, emerald 1, wheat 10]`.
- difference: the spear entry has the right weight (2 of 33 big / 30 small, rolls 2–8) but the wrong item, iron instead of stone, and it sits last instead of at index 3 (big) / 2 (small).
- fix_kind: data-override (runtime entry replacement)
- fix: in the add-on's LOW-priority `LootTableLoadEvent` handler, for `chests/underwater_ruin_big` and `chests/underwater_ruin_small`, remove the `iron_spear` entry from pool 0 and insert `LootItem.lootTableItem(ModItems.STONE_SPEAR.get()).setWeight(2).build()` at index 3 (big) / 2 (small), using the same entry-list access as `GhastParity.onLootTableLoad`.
- config_gate: `has_spears`

### OUT-05  Wooden spear is not furnace fuel
- severity: data
- vb: no fuel entry. `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/common/integrations/BlockIntegrations.java:17-24` registers only `SHORT_DRY_GRASS 100, TALL_DRY_GRASS 100, LEAF_LITTER 100, #wool_stairs 100, #wool_slabs 100, #cushions 200`. The item is `ModItems.java:185-186` `"wooden_spear", properties -> new SpearItem(Tiers.WOOD, ...)`, with no `getBurnTime`. NeoForge 21.1.223's `data/neoforge/data_maps/item/furnace_fuels.json` lists only vanilla ids and tags. The add-on's `SharedFuel` handles only cushions and wool slabs.
- native: `<mc-26.2>/net/minecraft/world/level/block/entity/FuelValues.java:79` `.add(Items.WOODEN_SPEAR, baseUnit)` with `baseUnit = 200`.
- difference: native smelts 1 item per wooden spear (200 ticks); in VB the spear can't go in the fuel slot.
- fix_kind: data-override `data/neoforge/data_maps/item/furnace_fuels.json` (or the `SharedFuel` event)
- fix: add `"minecraft:wooden_spear": {"burn_time": 200}` (merge with the wild area's entries in the same file), or in `SharedFuel.furnaceFuel`: `if (stack.is(ModItems.WOODEN_SPEAR.get())) event.setBurnTime(200);`.
- config_gate: `has_spears`

### OUT-06  Chest-loot entries VB adds sit at the end of each pool, not at their native positions
- severity: data
- vb: `<platform-1.4.2>/com/blackgear/platform/common/data/LootModifier.java:36-37,50-51` — `addToPool(content)` = `addToPool(0, content)`, appended after the existing entries. Used by `LootIntegrations.java:152-206` (copper horse armour), `:263-283` (spears), `:286-305` (diamond spear).
- native (`<mc-26.2>/data/minecraft/loot_table/chests/*.json`, pool 0 index / size): simple_dungeon copper_horse_armor 8/12; desert_pyramid copper_horse_armor 8/16 (before `empty`); end_city_treasure copper_horse_armor 6/25, diamond_spear 11/25; jungle_temple 8/13; nether_bridge 9/13; stronghold_corridor 15/21; village_weaponsmith iron_spear 7, copper_spear 8, copper_horse_armor 16 of 20 (VB adds horse armour first, then iron spear, then copper spear, all at the end); bastion_treasure diamond_spear (damaged + enchanted) 5/18 and plain 11/18.
- difference: weights and totals are identical, so the odds of each item are the same. But a roll picks by walking the entries in order, so for a given world seed a structure chest holds different items than natively.
- fix_kind: data-override (runtime reorder)
- fix: in the LOW-priority `LootTableLoadEvent` handler, for each table above, move the VB-added `LootItem` entries in pool 0 to the native indices listed (same remove/insert technique as `GhastParity.onLootTableLoad`, keyed on the item).
- config_gate: `has_copper_horse_armor_loot`, `has_spears`

### OUT-07  165 VB loot tables lack `random_sequence`, so their drops draw from the level RNG
- severity: data
- vb: for example `<vanillabackport-1.2.2>/data/minecraft/loot_table/blocks/leaf_litter.json`, `blocks/pale_oak_leaves.json`, `blocks/resin_clump.json`, `entities/nautilus.json`, `entities/parched.json`, `entities/zombie_nautilus.json`, `entities/camel_husk.json` have no `"random_sequence"` key. All 165 effective tables without it: 156 `blocks/*` (every pale oak, poplar, resin, sulfur and cinnabar block, eyeblossoms, creaking heart, bush, firefly bush, cactus flower, dry grass, leaf litter, wildflowers, shelf mushroom, straw bed, red shrub, wool and concrete slabs and stairs), `entities/{camel_husk,creaking,nautilus,parched,sulfur_cube,zombie_nautilus}`, `chests/abandoned_camp_{common,secret}_chest` and `barrels/abandoned_camp_barrel` (the add-on overrides of the two camp chests also lack it). 1.21.1 picks the random source in `LootContext.Builder.create(Optional<ResourceLocation> sequence)`: the named world sequence if the table has one, otherwise `level.getRandom()`.
- native: every one of these tables carries `"random_sequence": "minecraft:<path>"`, for example `src-mc/data/minecraft/loot_table/entities/nautilus.json` `"random_sequence": "minecraft:entities/nautilus"`.
- difference: odds are unchanged, but natively each table draws from its own seeded per-world stream. In VB these drops draw from, and advance, the shared level RNG: a leaf-litter or pale-oak-leaf farm shifts every other level-random user, and a given world seed gives different drop sequences. First-round COPPER-33 and GHAST-29 fixed this only for the copper and ghast tables.
- fix_kind: data-override (each listed path under `data/minecraft/loot_table/`)
- fix: ship add-on copies of each listed table with `"random_sequence": "minecraft:<table path without .json>"` added at the top level (no other change). Generate them mechanically from the VB JSON. For the two add-on camp chests, add the key to the existing override.
- config_gate: none

## Engine drift

### ED-1  Wandering trader offer structure
- 1.21.1: `<mc-1.21.1>/net/minecraft/world/entity/npc/WanderingTrader.java:138-148` — 5 offers from the level-1 list (`addOffersFromItemListings(..., 5)`) plus 1 random offer from the level-2 list. VB and the add-on append their trades to the end of those lists (`TradeIntegrations.java`, `WildTrades.java`: `getGenericTrades().add`, `getRareTrades().add`).
- native: `<mc-26.2>/data/minecraft/trade_set/wandering_trader/{buying,common,uncommon}.json` — `amount` 2 / 5 / 2 drawn from tags `#wandering_trader/buying` (6), `common` (76), `uncommon` (15). Each VB item sits at a fixed position in those tags (for example `emerald_open_eyeblossom` after lily of the valley, `emerald_pale_oak_log` index 12 of 15 in uncommon).
- farm effect: trade prices, uses and xp match exactly. What differs is how often an item is offered: a pale oak log is 1 rare pick out of the 1.21.1 rare list versus 2 picks out of 15 in native's uncommon list. The per-trader sequence also differs, because the list order differs.
- fix: none scoped. Changing how often items are offered means replacing the 1.21.1 trader generation for every trade, not just VB's.

### ED-2  Animal XP draws from the level RNG
- 1.21.1: `<mc-1.21.1>/net/minecraft/world/entity/animal/Animal.java:126-128` `return 1 + this.level().random.nextInt(3);`
- native: `<mc-26.2>/net/minecraft/world/entity/animal/Animal.java:134-136` `return 1 + this.random.nextInt(3);`
- farm effect: same 1–3 distribution. For VB's Animal subclasses (happy ghast, nautilus, zombie nautilus, camel husk), each kill draws from the shared level RNG instead of the mob's own.
- fix (scoped): `@Mixin({HappyGhast.class, AbstractNautilus.class, CamelHusk.class})` adding `@Override protected int getBaseExperienceReward() { return 1 + this.random.nextInt(3); }` (vanilla animals keep 1.21.1 behaviour).

### ED-3  26.3 compost rolls use a weighted list
- 1.21.1: `<mc-1.21.1>/net/minecraft/world/level/block/ComposterBlock.java:307` (addItem) — `if ((i != 0 || !(f > 0.0F)) && !(level.getRandom().nextDouble() < (double)f))` (after the empty-composter guarantee).
- native 26.3 (straw bed, poplar leaves, poplar sapling, red shrub, shelf mushroom): `<mc-26.3-pre-1>/net/minecraft/world/level/storage/loot/providers/number/ints/ContextIntProviders.java:75-79,108-123` `compostable(blocks, n)` = level 0 → 1, else weighted list `{1: n, 0: 100-n}` (an integer roll out of 100).
- farm effect: same probabilities (0.30 / 0.65 / 0.85); a different random call. This applies only to the five Wilderness Bound items; 26.2 items use the 1.21.1-style `nextDouble`, as in 26.2.
- fix: none worth scoping (it would need a per-item composter mixin for a call-shape difference only).
