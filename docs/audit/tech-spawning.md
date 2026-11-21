# tech-spawning: VB 1.2.2 + add-on vs native 26.2 (spawning, despawning and special spawners)

**Summary.**
- 12 findings: 5 farm, 3 gameplay, 3 cosmetic (RNG order only), 1 data. Engine drift adds 4 more (1 farm, 2 gameplay, 1 cosmetic) and 5 notes without a finding.
- Scope: every VB mob (creaking, happy ghast/ghastling, copper golem, parched, camel husk, nautilus, zombie nautilus, sulfur cube, zombie horse), the farm-animal, wolf, cat and frog variants, sheep, the zombie, husk and drowned spear and jockey logic, and camel spawns. For each mob the audit checked:
  - the effective biome spawn lists, MobCategory, spawn costs and creature probability;
  - SpawnPlacements, mob caps, structure overrides and chunk-generation spawns;
  - `finalizeSpawn` per spawn reason, with random draws in order;
  - despawning and persistence;
  - creaking-heart, copper-golem, ghastling, sulfur-cube-split and trial-spawner paths;
  - conversions;
  - NaturalSpawner engine drift.
- Method for biome lists: every overworld, nether and end `addSpawn` in 1.21.1 `BiomeDefaultFeatures`/`OverworldBiomes` was diffed against 26.2, and the effective VB lists were rebuilt for every biome VB touches. Apart from the findings below, all other biomes match entry for entry.
- Effective behaviour = VB 1.2.2 plus the add-on's merged fixes. First-round fixes re-verified as correct: GHAST-42, GHAST-43, MAYHEM-07/24/25/28/30, SULFUR-01/02/03/08/41, VARIANTS-20 (chance roll), MISC-31 and COPPER-19. Not re-reported.
- Not checked: Second-round work in progress, and anything that needs a running game (spawn rates were derived from code and data, not measured).

## Coverage

| Mechanic | Result |
|---|---|
| **Biome spawn lists** | |
| Parched in `#has_structure/desert_pyramid` (= desert): weight 50, 4–4, MONSTER, last entry | match for the entry itself; the desert skeleton weight is **SPAWN-01** |
| Zombie horse in plains, sunflower_plains (MAYHEM-28 fix), snowy_plains, `#is_savanna`: weight 5, 1–1, MONSTER | entry and biome set match; zombie weight and list position: **SPAWN-02** |
| Ice spikes: no zombie horse, zombie 95 | match |
| Nautilus: cold, deep_cold, frozen and deep_frozen oceans weight 2; ocean, deep_ocean, lukewarm, deep_lukewarm and warm_ocean weight 5; 1–1, WATER_CREATURE | match; warm-ocean entry order: **SPAWN-05** |
| Camel in desert (`#spawns_camels` = `#c:desert`/`#c:is_desert`, which resolves to desert in vanilla): weight 1, 1–1, CREATURE | entry matches; the desert rabbit weight is **SPAWN-04**. Modded biomes in `c:is_desert` also get camels, which native would not. |
| Badlands, eroded and wooded badlands creature list | **SPAWN-03** (no farm animals) |
| Sulfur caves (add-on JSON + VB modifier, SULFUR-41 fix): sulfur_cube 100 at index 0, zombie_villager 5 | match |
| Pale garden (VB JSON): monsters, bat and glow squid only, no creatures | match |
| Dappled forest (VB JSON vs 26.3): farm animals, rabbit 4 2–4, fox 4 2–4, common monsters | match |
| Spawn costs (charge/energy budget) in VB-touched biomes | match (none in either) |
| `creature_spawn_probability` (badlands 0.03 / wooded 0.04; others default 0.1) | match |
| Structure `spawn_overrides` (fortress, monument, outpost, swamp hut, ancient city, trial chambers; VB and 26.3 abandoned camps are empty) | match (no VB mob in any; camps `{}` in both) |
| Trial spawner configs (`trial_spawner/trial_chamber/**`) and `equipment/trial_chamber*` | match (no VB mob; zombies and husks roll VB spears through `finalizeSpawn`; see slice-B rows) |
| **SpawnPlacements** | |
| creaking ON_GROUND / MOTION_BLOCKING_NO_LEAVES / monster rules | match (native has no natural creaking list) |
| camel ON_GROUND, `camels_spawnable_on` and raw brightness > 8 (Platform `OR`, but 1.21.1 has no camel entry, so the predicate is used on its own) | behaviour matches; tag id: **SPAWN-12** |
| sulfur cube ON_GROUND, `return true` | match |
| parched and camel husk ON_GROUND, surface-monster rules | match off Peaceful; Peaceful: **SPAWN-06** |
| nautilus IN_WATER, y in [sea−25, sea−5], water below, water block above; `getWalkTargetValue` 0; `checkSpawnObstruction` = `isUnobstructed` | match |
| zombie horse ON_GROUND REPLACE with monster rules | match off Peaceful; Peaceful: **SPAWN-06** |
| happy ghast ON_GROUND / `Animal::checkAnimalSpawnRules` | **SPAWN-07** (missing) |
| copper golem (none in either) | match |
| `SpawnPlacementTypes.ON_GROUND`/`IN_WATER`, `Monster.isDarkEnoughToSpawn`, `Animal.checkAnimalSpawnRules`, `animals_spawnable_on`, `prevent_mob_spawning_inside` | match (1.21.1 = 26.2) |
| Spawn-surface properties of VB blocks (pale oak / poplar leaves `ocelotOrParrot`, pale oak / poplar trapdoors `never`, all others default; no full-face block with light ≥ 14) | match |
| Mob categories: creaking, parched, camel husk, zombie nautilus, sulfur cube and zombie horse (VB `EntityTypeMixin`) MONSTER; happy ghast CREATURE; nautilus WATER_CREATURE; copper golem MISC | match |
| `spawnDimensionsScale(2.0)` on the sulfur cube | match |
| **Spawn-time logic** | |
| Zombie family, husk camel jockey, drowned zombie-nautilus jockey, zombified piglin, parched, camel husk, zombie horse, zombie nautilus `finalizeSpawn` per reason | see the slice-B rows below; **SPAWN-10**, **SPAWN-13**, **SPAWN-14** |
| Cow, pig and chicken variant + sound variant draw order | **SPAWN-11** |
| Cat (sound after super), wolf (variant by biome, no draw), frog, sheep colour (VARIANTS-20 fix) | match |
| Camel `finalizeSpawn` (CamelAi memories, pose reset, AbstractHorse 0.2 baby group, `randomizeAttributes`) | match |
| Nautilus `finalizeSpawn` (`NautilusAi.initMemories`, then AgeableMob) | match |
| Sulfur cube `finalizeSpawn`: size after the AgeableMob baby roll (SULFUR-03 fix), adult 2, baby 1 | match |
| Copper golem `finalizeSpawn` (spawn sound only) and `spawn(weatherState)` | match |
| Happy ghast `finalizeSpawn` (none, AgeableMob default) | match |
| **Despawning** | |
| `Mob.checkDespawn` (128 instant, 32 + `noActionTime > 600` + 1/800) | match; Peaceful handled by MISC-31 |
| `removeWhenFarAway`: camel, happy ghast, copper golem false; nautilus, zombie horse, camel husk, creaking, sulfur cube true | match |
| `requiresCustomPersistence`: nautilus tame; sulfur cube body item or from bucket; jockey riders while riding | match; leashed: **SPAWN-09** |
| Persistence on name tag; on player interaction | name tag: match; interaction or lead: **SPAWN-09** |
| **Special spawners** | |
| Creaking heart: ticker `nextInt(5) + 20`; state from night 12600–23400 (native `CREAKING_ACTIVE` keyframes 12600 → 23401); `spawn_mobs` (+ `spawn_monsters`, which 1.21.1 lacks) and not Peaceful; nearest player within 32; `SpawnUtil` 5 attempts, ±16 XZ, Y 8 → −8, ON_TOP_OF_COLLIDER_NO_LEAVES, collision check, `checkSpawnRules`/obstruction; distance 34 release; reason SPAWNER | match (sounds: GARDEN-08, fixed) |
| Heart-bound creaking: transient home, dies without its heart, counts toward the monster cap, despawns like a monster | match |
| Copper golem creation: pattern, oxidation and wax state, spawn position +0.05, chest swap, `SUMMONED_ENTITY` trigger | match; **pattern priority: SPAWN-08** |
| Iron golem summoning by villagers (`SpawnUtil` LEGACY_IRON_GOLEM) | not touched by VB; nothing to compare |
| Dried ghast → ghastling: 5000-tick scheduled steps, hydration 0..3, baby, rotation from FACING, `removeBlock` first | match |
| Sulfur cube split: count 2 (0 when primed), size/2, offsets (SULFUR-02 fix), copied state (SULFUR-01 fix), yaw `nextFloat()*360` | match |
| Sulfur cube bucket release | covered by the sulfur area (SULFUR-17) |
| **Conversions** | |
| Zombie → drowned, husk → zombie: spear, baby, persistence and vehicle kept | match |
| Skeleton → stray: no VB part; parched never converts | match |
| Villager → zombie villager | **SPAWN-13** (engine) |
| **Engine drift** | |
| Global mob cap evaluation | **SPAWN-15** |
| Chunk-generation creature spawns and the `spawn_mobs` gamerule | **SPAWN-16** |
| WeightedList selection (`nextInt(total)`, cumulative in list order), MobCategory caps and distances, cluster/group sizes, spawn-attempt loop | match |
| `requiresCustomPersistence` and `isLeashed` | folded into SPAWN-09 for VB mobs |
| Mob.checkMobSpawnRules TRIAL_SPAWNER bypass; spawn-point dimension check | noted (N4, N5) |

Slice-B rows (zombie family; audited in this pass):

| Mechanic | Result |
|---|---|
| Zombie `finalizeSpawn` draw order: loot (0.55×), baby 0.05, chicken jockey 0.05/0.05, doors, equipment, enchantments, Halloween 0.25/0.1, attributes | match apart from drift (B03/B04 → SPAWN-13/14) |
| Zombie weapon `nextInt(6)`: sword 0, spear 1 (MAYHEM-07 fix, only when `has_spears` is on), shovel 2–5 | match |
| Spear enchant pool at spawn, mob armour tier roll (COPPER-19 fix) | match |
| `Mob.finalizeSpawn` (follow-range triangle, then leftHanded 0.05) | match |
| REINFORCEMENT: no jockeys | match (the reinforcement type is note N2) |
| Husk camel jockey (GHAST-43 fix): second loot roll, group data per husk, noCollision box, 0.1, spear after enchantments, rider order; only NATURAL rolls | match |
| Drowned: trident `>0.9` then `nextInt(16)<10`, shell 0.03, jockey order reason → trident → 0.5 → !baby → biome (GHAST-42 fix) | match (RNG offset: SPAWN-14) |
| Zombie-nautilus jockey: persistent if STRUCTURE, variant select, AI memories, AgeableMob, Mob | match |
| Zombified piglin golden spear `nextInt(20)==0` | match (one draw; VB gives it even with `has_spears` off, and native always does) |
| Parched: AbstractSkeleton chain, no sun burn, Weakness immunity (MAYHEM-30) | match |
| Camel husk: Camel memories, pose reset, AbstractHorse 0.2 + attributes; jockey never a baby | match |
| Zombie horse NATURAL rider (iron spear, unenchanted) | outcome matches; draw order: **SPAWN-10** |
| Zombie horse SPAWN_EGG/COMMAND/SPAWNER/BREEDING: no rider; babies never age (MAYHEM-25) | match |
| Zombie horse and zombie nautilus daylight burning (`nextInt(2)` armour wear, 8 s fire) | match |
| Jockey mounts despawn normally; riders persist while riding | match |

---

### SPAWN-01  Desert skeleton weight stays 100 when parched are added (native 50)
- severity: farm
- vb:
  - `src-vb/com/blackgear/vanillabackport/common/worldgen/MountsOfMayhemFeatureManager.java:20-24`: `this.addIf(hasParchedSkeletons, (context, writer) -> this.getOrCreateBiomeBuilder(BiomeTags.HAS_DESERT_PYRAMID).add(() -> writer.addSpawn(MobCategory.MONSTER, new SpawnerData(ModEntityTypes.PARCHED.get(), 50, 4, 4))))`. It only appends.
  - The base list is 1.21.1's `src-121/net/minecraft/data/worldgen/BiomeDefaultFeatures.java:450-455` `desertSpawns`: `monsters(builder, 19, 1, 100, false); ... HUSK, 80, 4, 4`. Skeleton weight is 100.
- native: `src-mc/net/minecraft/data/worldgen/BiomeDefaultFeatures.java:498-505`: `monsters(builder, 19, 1, 0, 50, false); builder.addSpawn(MONSTER, 80, HUSK 4, 4); builder.addSpawn(MONSTER, 50, PARCHED 4, 4);`. In `data/minecraft/worldgen/biome/desert.json` the skeleton entry is `"weight": 50`.
- difference: the desert MONSTER table totals 565 in VB and 515 natively.
  - Skeleton packs: VB 17.70% (100/565), native 9.71% (50/515).
  - Parched: VB 8.85%, native 9.71%.
  - Husk: VB 14.16%, native 15.53%.
  - Spider, creeper and slime: VB 17.70% each, native 19.42% each.
  - Zombie: VB 3.36%, native 3.69%.
  - A desert skeleton or parched farm gets about 1.8× as many skeletons per parched as native: 2.0 skeletons per parched, against 1.0.
- fix_kind: data-override via a NeoForge biome modifier (the same pattern as `sulfur/SulfurCavesOrderModifier`)
- fix: add a `vbparity:mayhem_monster_weights` `BiomeModifier`. At `Phase.AFTER_EVERYTHING`, for a biome whose MONSTER list contains a `SpawnerData` of type `ModEntityTypes.PARCHED` that VB added (`biome.is(BiomeTags.HAS_DESERT_PYRAMID)` and `hasParchedSkeletons`), replace the `SpawnerData(EntityType.SKELETON, 100, 4, 4)` entry with `new SpawnerData(EntityType.SKELETON, 50, 4, 4)` at the same index. Touch only an entry with exactly weight 100 and counts 4/4, so lists changed by other mods are left alone.
- config_gate: `has_parched_skeletons`

### SPAWN-02  Zombie-horse biomes keep zombie weight 95 (native 90), and the zombie horse is the last entry (native index 3)
- severity: farm
- vb: `MountsOfMayhemFeatureManager.java:25-29`: `getOrCreateBiomeBuilder(ModBiomeTags.SPAWNS_ZOMBIE_HORSES).add(() -> writer.addSpawn(MobCategory.MONSTER, new SpawnerData(EntityType.ZOMBIE_HORSE, 5, 1, 1)))`. Platform `BiomeManagerImpl.java:132-133` appends with `builder.getMobSpawnSettings().addSpawn(category, data)`. The base lists are 1.21.1 `plainsSpawns`/`snowySpawns` → `monsters(builder, 95, 5, 100|20, false)` (`BiomeDefaultFeatures.java:436-448, 465-474`) and savanna `commonSpawns` (`OverworldBiomes.java:317`).
- native: `BiomeDefaultFeatures.java:454-457`: `commonSpawnWithZombieHorse → monsters(builder, 90, 5, 5, 100, false)`; `:490-496`: `snowySpawns(..., spawnZombieHorse)` → `monsters(builder, spawnZombieHorse ? 90 : 95, 5, spawnZombieHorse ? 5 : 0, 20, false)`; `:514-534`: `monsters` adds spider, zombie, zombie_villager, **zombie_horse**, skeleton, ... in that order. `OverworldBiomes.java:306` savanna uses `commonSpawnWithZombieHorse`. In `plains.json` the order is spider, zombie 90, zombie_villager 5, zombie_horse 5, skeleton 100, ...
- difference: in plains, sunflower_plains, snowy_plains, savanna, savanna_plateau and windswept_savanna, the MONSTER total is 520 in VB and 515 natively.
  - Zombie packs: VB 18.27% (95/520), native 17.48% (90/515), so VB has 4.5% more zombies.
  - Zombie horse: VB 0.962%, native 0.971%.
  - Every other monster: VB ×0.990 of native.
  - The zombie horse entry is also last in VB, so the `nextInt(total)` value that picks it natively (index 3) picks a different mob in VB. The distribution is the same, but seeded or replayed spawns diverge.
- fix_kind: data-override (biome modifier)
- fix: in the same AFTER_EVERYTHING modifier, for biomes whose MONSTER list holds VB's `SpawnerData(ZOMBIE_HORSE, 5, 1, 1)` (only when `hasZombieHorses`):
  1. Replace `SpawnerData(ZOMBIE, 95, 4, 4)` with `SpawnerData(ZOMBIE, 90, 4, 4)`.
  2. Move the zombie-horse entry to just after the `ZOMBIE_VILLAGER` entry (index 3 in vanilla lists).
  Only act on these exact weights.
- config_gate: `has_zombie_horses`

### SPAWN-03  Badlands have no cow, pig, chicken or sheep spawns, so warm farm-animal variants never spawn there naturally
- severity: farm
- vb: VB adds nothing to the badlands spawn lists. 1.21.1 `OverworldBiomes.java:330-338` `badlands(...)`: `BiomeDefaultFeatures.commonSpawns(builder); addSpawn(CREATURE, ARMADILLO, 6, 1, 2); creatureGenerationProbability(0.03F); if (trees) { addSpawn(CREATURE, WOLF, 2, 4, 8); creatureGenerationProbability(0.04F); }`. Meanwhile VB's (and the add-on's) `data/minecraft/tags/worldgen/biome/spawns_warm_variant_farm_animals.json` lists `#minecraft:is_badlands` (badlands, eroded_badlands, wooded_badlands).
- native: `OverworldBiomes.java:319-328`: `BiomeDefaultFeatures.farmAnimals(mobs); BiomeDefaultFeatures.commonSpawns(mobs); mobs.addSpawn(CREATURE, 6, ARMADILLO 1, 2); ...`. In `badlands.json` the creature list is `sheep 12 4–4, pig 10 4–4, chicken 10 4–4, cow 8 4–4, armadillo 6 1–2` (wooded adds `wolf 2 4–8`).
- difference: the native badlands CREATURE table totals 46 (wooded 48), 87% of it farm animals; VB's totals 6 (wooded 8), with no farm animals.
  - On grass in badlands (wooded-badlands plateaus, river edges), native spawns warm cows, pigs and chickens and badlands sheep, both naturally and at chunk generation (probability 0.03/0.04). VB spawns only armadillos (and wolves).
  - The warm-variant tag has no effect in these biomes except on bred or egg-spawned animals.
  - Armadillo share: VB 100% (wooded 75%), native 13.0% (wooded 12.5%). So badlands armadillo farms produce about 7.7× more per creature spawn cycle in VB.
- fix_kind: data-override (biome modifier)
- fix: an AFTER_EVERYTHING modifier for `#minecraft:is_badlands`, when `hasFarmAnimalVariants`. Insert at CREATURE indices 0–3: `SpawnerData(SHEEP, 12, 4, 4)`, `SpawnerData(PIG, 10, 4, 4)`, `SpawnerData(CHICKEN, 10, 4, 4)`, `SpawnerData(COW, 8, 4, 4)`, unless a SHEEP entry is already present. These trees do not show which drop moved farm animals into badlands; the gate keeps 1.21.1 lists when the variant feature is off.
- config_gate: `farm_animal_variants`

### SPAWN-04  Desert rabbit weight stays 4 next to VB's camel (native 12), so camels are 2.6× too common
- severity: farm
- vb: `SpringToLifeFeatureManager.java:21-25`: `getOrCreateBiomeBuilder(ModBiomeTags.SPAWNS_CAMELS).add(() -> writer.addSpawn(MobCategory.CREATURE, new SpawnerData(EntityType.CAMEL, 1, 1, 1)))` on top of 1.21.1 `BiomeDefaultFeatures.java:450-451`: `addSpawn(CREATURE, new SpawnerData(EntityType.RABBIT, 4, 2, 3))`.
- native: `BiomeDefaultFeatures.java:498-500`: `builder.addSpawn(MobCategory.CREATURE, 12, new SpawnerData(EntityTypes.RABBIT, 2, 3)); builder.addSpawn(MobCategory.CREATURE, 1, new SpawnerData(EntityTypes.CAMEL, 1, 1));`. In `desert.json`: `rabbit weight 12`, `camel weight 1`.
- difference: in VB the desert CREATURE draw picks a camel 1/5 = 20.0% of the time; natively it is 1/13 = 7.69%. Rabbits fall from 92.3% to 80.0% of packs, about 0.87× native per creature cycle. This covers natural spawns and chunk generation (`creature_spawn_probability` 0.1). A camel collector or desert rabbit farm sees 2.6× more camels, or 0.87× the rabbits, than native.
- fix_kind: data-override (biome modifier)
- fix: an AFTER_EVERYTHING modifier for biomes where VB added the camel entry (`biome.is(ModBiomeTags.SPAWNS_CAMELS)` and `hasCamelSpawns`): replace `SpawnerData(RABBIT, 4, 2, 3)` with `SpawnerData(RABBIT, 12, 2, 3)` at the same index. This is a vanilla data change that sits beside the native desert camel line; gate it on the camel option so the unmodified 1.21.1 desert stays as it was.
- config_gate: `camel_spawns`

### SPAWN-05  Warm ocean: the nautilus entry is last (native first)
- severity: cosmetic (RNG order only)
- vb: `MountsOfMayhemFeatureManager.java:36`: `addSpawn(MobCategory.WATER_CREATURE, new SpawnerData(NAUTILUS, 5, 1, 1))`, appended after 1.21.1 `warmOceanSpawns` (`OverworldBiomes.java:439-442`: pufferfish, then squid 10, dolphin 2). The effective list is squid, dolphin, nautilus.
- native: `OverworldBiomes.java:421-425`: `new MobSpawnSettings.Builder().addSpawn(WATER_AMBIENT, 15, PUFFERFISH).addSpawn(WATER_CREATURE, 5, NAUTILUS 1, 1); BiomeDefaultFeatures.warmOceanSpawns(mobs, 10, 4);`. In `warm_ocean.json` the water_creature list is nautilus, squid, dolphin.
- difference: weights are equal (total 17, nautilus 29.4%). The same `nextInt(17)` value selects nautilus for 0–4 natively and for 12–16 in VB. Seeded spawns differ; rates do not. The other eight ocean biomes already have native order.
- fix_kind: data-override (biome modifier)
- fix: in the modifier from SPAWN-02, for `minecraft:warm_ocean`, move VB's nautilus `SpawnerData` to WATER_CREATURE index 0.
- config_gate: `has_nautilus`

### SPAWN-06  On Peaceful, zombie horses (and spawner camel husks) never spawn; natively they do
- severity: gameplay
- vb:
  - `src-vb/.../common/integrations/MobSpawns.java:19-23`: `checkMonsterSpawnRules(...) { return level.getDifficulty() != Difficulty.PEACEFUL && (MobSpawnType.ignoresLightRequirements(spawnType) || Monster.isDarkEnoughToSpawn(level, pos, random)) && Mob.checkMobSpawnRules(...); }`. `checkSurfaceMonstersSpawnRules` (`:25-29`) calls it.
  - `MobIntegrations.java:50-56` registers it for `EntityType.ZOMBIE_HORSE` with `SpawnPlacementStrategy.REPLACE`; `:46-48` registers it for `CAMEL_HUSK`.
  - The add-on (MISC-31) makes Peaceful run the monster category (`misc/mixin/MinecraftServerPeacefulMixin`) and lists `minecraft:zombie_horse` and `camel_husk` in `monsters_that_spawn_on_peaceful`. VB's predicate still refuses them.
- native:
  - `SpawnPlacements.java:168`: `register(ZOMBIE_HORSE, ON_GROUND, MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules)`.
  - `Monster.java:100-105`: `checkMonsterSpawnRules(...) { return (EntitySpawnReason.ignoresLightRequirements(spawnReason) || isDarkEnoughToSpawn(level, pos, random)) && checkMobSpawnRules(...); }`. There is no difficulty test; Peaceful is decided per type in `SpawnPlacements.java:77-85` (`!type.isAllowedInPeaceful() && PEACEFUL → false`).
  - `EntityTypes.java:1099-1106`: ZOMBIE_HORSE has no `.notInPeaceful()`; CAMEL_HUSK (`:257-258`) has none either.
- difference: in a Peaceful world, native plains, sunflower plains, snowy plains and savannas spawn zombie horses at night: 5/515 of monster packs, the same rate as on other difficulties. In VB + add-on they never spawn on Peaceful, and spawners holding zombie horses or camel husks stay idle. On other difficulties the two predicates give the same result and use the same random draws.
- fix_kind: mixin-vb (`MobSpawns.checkMonsterSpawnRules`)
- fix: `@Mixin(value = MobSpawns.class, remap = false) @WrapOperation(method = "checkMonsterSpawnRules", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getDifficulty()Lnet/minecraft/world/Difficulty;"))`: return `Difficulty.EASY` (any non-Peaceful value) when `type.is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL)`. Otherwise call the original. Types not allowed in Peaceful are already filtered earlier by the add-on (`NaturalSpawnerPeacefulMixin`, `EntityTypePeacefulMixin`), so creakings and parched are unaffected.
- config_gate: `has_zombie_horses` (spawns); the Peaceful behaviour itself is the add-on's MISC-31 option

### SPAWN-07  Happy ghast has no spawn placement: spawners spawn it anywhere
- severity: gameplay
- vb: `MobIntegrations.java:39-57` `registerPlacements` registers creaking, camel, sulfur cube, parched, camel husk, nautilus and zombie horse, but not `HAPPY_GHAST` (only its attributes, `:61`). 1.21.1 `SpawnPlacements.java:78-84`: `boolean vanillaResult = data == null || data.predicate.test(...)`, so a missing entry passes.
- native: `SpawnPlacements.java:124`: `register(EntityTypes.HAPPY_GHAST, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules)`. `Animal.java:112-117`: SPAWNER does not ignore light, so the block below must be `#animals_spawnable_on` (grass_block) and raw brightness must be > 8.
- difference: a monster spawner set to happy ghast (or ghastling) in VB spawns on any block at any light level; `BaseSpawner` uses `SpawnPlacements.checkSpawnRules(..., SPAWNER, ...)` when there are no custom rules. Natively it spawns only onto grass blocks with light ≥ 9 (one block below each candidate position). Spawner ghast farms built on stone work in VB and do nothing natively. No natural biome list contains the happy ghast in either version, so natural spawning is unaffected.
- fix_kind: mixin-vb or a mod-bus event in the add-on
- fix: on `RegisterSpawnPlacementsEvent`, call `event.register(ModEntityTypes.HAPPY_GHAST.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)`. `HappyGhast extends Animal` in VB, so the generic bound fits.
- config_gate: none (`dried_ghasts` gates only how you obtain them)

### SPAWN-08  The copper-golem pattern is checked before snow- and iron-golem patterns (native: last)
- severity: gameplay
- vb: `core/mixin/common/copper_golem/CarvedPumpkinBlockMixin.java:66-82`: `@Inject(method = "trySpawnGolem", at = @At("HEAD")) onTrySpawnGolem(...) { if (hasCopperGolems) { BlockPatternMatch copperGolemMatch = getOrCreateCopperGolemFull().find(level, pos); if (copperGolemMatch != null) { ... spawnGolemInWorld(...); replaceCopperBlockWithChest(...); golem.spawn(...); } } }`. It runs before the vanilla snow and iron checks (1.21.1 `CarvedPumpkinBlock.java:61-78`). Those checks then find the pumpkin gone.
- native: `CarvedPumpkinBlock.java:71-100`: snow golem first (`return`), then iron golem (`return`), and only then `copperGolemMatch = getOrCreateCopperGolemFull().find(level, topPos)`.
- difference: `BlockPattern.find` tries all 24 orientations (`BlockPattern.java:84-102`), so the 1×2 copper pattern matches whenever any copper block (`#copper`, the 8 full copper blocks) touches the placed pumpkin on any side, above included.
  - Snow golem: a pumpkin on two snow blocks with a copper block beside it builds a copper golem and a copper chest in VB, and leaves the snow column. Native builds the snow golem.
  - Iron golem: the same happens when a copper block touches the pumpkin on the axis not used by the arms, or from above.
  - Golem builders driven by dispensers next to copper (decoration, copper-golem storage setups) produce the wrong golem in VB.
- fix_kind: mixin-vb (`CarvedPumpkinBlockMixin.onTrySpawnGolem`)
- fix:
  1. Cancel VB's HEAD hook: `@Mixin(value = CarvedPumpkinBlock.class, priority = 1500)` `@TargetHandler(mixin = "...copper_golem.CarvedPumpkinBlockMixin", name = "onTrySpawnGolem") @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true) → ci.cancel()`. Alternatively, `@WrapOperation` its `BlockPattern.find` call to return null.
  2. Re-run the same body at the end of `trySpawnGolem`: `@Inject(method = "trySpawnGolem", at = @At("TAIL"))`. Both vanilla branches end without returning when nothing spawned, so TAIL is reached only when neither snow nor iron matched. The pumpkin is still in place, so `find` succeeds exactly as natively.
- config_gate: `has_copper_golems`

### SPAWN-09  Leashing an untamed zombie horse, camel husk, nautilus or zombie nautilus does not make it persistent, so it despawns
- severity: farm
- vb:
  - `src-vb/.../core/mixin/common/zombie_horses/ZombieHorseMixin.java:86-87`: `public InteractionResult mobInteract(Player player, InteractionHand hand) { this.setPersistenceRequired(); ...` and `:114-116` `removeWhenFarAway(...) { return true; }`.
  - `src-vb/.../animal/camel/CamelHusk.java:40-41`: `mobInteract(...) { this.setPersistenceRequired();`, and `:31` `removeWhenFarAway → true`.
  - `src-vb/.../animal/nautilus/AbstractNautilus.java:408-409`: `mobInteract(...) { this.setPersistenceRequired();`, `:474` `removeWhenFarAway → true`, and `:576-577` `requiresCustomPersistence() { return super.requiresCustomPersistence() || this.isTame(); }`.
  - In 1.21.1, `mobInteract` is the last stage of `Mob.interact` (`src-121 .../Mob.java:1247-1256`). VB's lead handling also runs earlier, from the NeoForge `EntityInteract` event (`src-platform .../neoforge/MobIntegrationImpl.java:30-37`; VB `LeashInteraction.java:64-74` `setLeashedTo(player, true)` → SUCCESS). So attaching a lead never reaches `mobInteract`.
  - 1.21.1 `Mob.requiresCustomPersistence` (`src-121 .../Mob.java:736-738`) is `return this.isPassenger();`.
- native: `animal/equine/ZombieHorse.java:58-61`: `interact(...) { this.setPersistenceRequired(); return super.interact(player, hand, location); }`. `CamelHusk.java:40-42` and `AbstractNautilus.java:394-396` do the same, before any lead handling. `Mob.java:693-695`: `requiresCustomPersistence() { return this.isPassenger() || this.isLeashed(); }`.
- difference: natively, any player interaction (a lead included) makes these mobs persistent, and a leashed mob is never despawned. In VB a wild one that is only leashed (to the player or to a fence) is removed at once beyond 128 blocks. Beyond 32 blocks, once `noActionTime > 600`, it has a 1/800 chance per tick, so it is gone about 40 s after the player walks off. Riding goes through `mobInteract` and is not affected. Fence pens of zombie horses or camel husks, and nautiluses moved on leads, empty themselves in VB.
- fix_kind: mixin-mc (`Mob.requiresCustomPersistence`, `Player.interactOn`)
- fix:
  1. `@Mixin(Mob.class) @Inject(method = "requiresCustomPersistence", at = @At("RETURN"), cancellable = true)`: if `!cir.getReturnValueZ() && isLeashed()` and `this` is a `ZombieHorse`, `CamelHusk` or `AbstractNautilus`, return true.
  2. `@Mixin(Player.class) @Inject(method = "interactOn", at = @At("HEAD"))`: server side, not spectator, entity is one of those types → `mob.setPersistenceRequired()`. This runs before the NeoForge `EntityInteract` event, like native `interact()`'s first line.
- config_gate: `has_zombie_horses`, `has_nautilus` (camel husk: none)

### SPAWN-10  Zombie-horse jockey: the rider is rolled after the horse's attributes instead of before
- severity: cosmetic (RNG order only)
- vb: `ZombieHorseMixin.java:150-163` `vb$finalizeSpawn`: `if (reason == NATURAL) { Zombie zombie = ZOMBIE.create(...); zombie.moveTo(...); zombie.finalizeSpawn(level, difficulty, reason, null); zombie.setItemSlot(MAINHAND, spear); zombie.startRiding(this, false); }`. It is called from `core/mixin/common/extension/entity/MobMixin.java:42-55` at `@At("RETURN")` of `Mob.finalizeSpawn`, which is after AbstractHorse `randomizeAttributes` (`src-121 .../AbstractHorse.java:1149`, 6 `nextDouble`), the AgeableMob baby check and the Mob draws.
- native: `animal/equine/ZombieHorse.java:134-147`: the zombie is created (JOCKEY), positioned, `finalizeSpawn(level, difficulty, spawnReason, null)`, given `IRON_SPEAR` and mounted **before** `return super.finalizeSpawn(...)`.
- difference: about 10–20 level-RNG draws for the zombie come after the horse's 6 attribute doubles and 3 Mob draws instead of before. Every rate is unchanged (spear always, baby zombie 5%, horse speed and jump distributions). Only seeded or replayed spawns diverge.
- fix_kind: mixin-mc (`AbstractHorse.finalizeSpawn`)
- fix: cancel VB's hook (`@Mixin(value = ZombieHorse.class, priority = 1500) @Inject(method = "vb$finalizeSpawn", at = @At("HEAD"), cancellable = true, remap = false) → cir.setReturnValue(data)`, the same pattern as `HuskJockeyMixin`). Then run its body at `@Inject(method = "finalizeSpawn", at = @At("HEAD"))` on `AbstractHorse`, guarded by `this instanceof ZombieHorse && reason == NATURAL`.
- config_gate: `has_zombie_horses`, `has_spears`

### SPAWN-11  Cow, pig and chicken pick their variant and sound variant after the AgeableMob and Mob draws (native: before)
- severity: cosmetic (RNG order only)
- vb:
  - `core/mixin/common/mob_variants/CowMixin.java:55-60` (and the Pig/Chicken twins): `vb$finalizeSpawn(...) { VariantUtils.selectVariantToSpawn(SpawnContext.create(level, blockPosition()), CowVariants.REGISTRIES, VariantSpawner.FARM_ANIMALS).ifPresent(this::setVariantData); return spawnData; }`. It runs from `extension/entity/MobMixin.java:42-55` at `Mob.finalizeSpawn` RETURN.
  - The add-on's sound pick, `variants/mixin/AgeableMobMixin.java:29-38` (`@Inject(method = "finalizeSpawn", at = @At("RETURN")) ... family.pickRandom(this, level.getRandom())` → `nextInt(size)`), runs later still.
  - The effective order is: AgeableMob baby `nextFloat` (`src-121 .../AgeableMob.java:34-36`) → Mob follow-range triangle and leftHanded `nextFloat` → variant → sound `nextInt`.
- native: `animal/cow/Cow.java:83-89`: `VariantUtils.selectVariantToSpawn(...).ifPresent(this::setVariant); this.setSoundVariant(CowSoundVariants.pickRandomSoundVariant(registryAccess(), level.getRandom())); return super.finalizeSpawn(...)`. `Pig.java:316-322` and `Chicken.java:186-192` are the same. The order is: variant → sound `nextInt` → baby → triangle → leftHanded.
- difference: the same draws occur in a different order. VARIANTS-20 removed the extra chance roll but left this ordering, and the add-on's new sound pick follows VB's late placement. Variant, sound, baby (5% in groups) and handedness rates are unchanged. Seeded or replayed spawns (spawn eggs in GameTests, `/random` replays) give a different sound variant and baby/handedness pattern than native.
- fix_kind: mixin-mc (`Cow`/`Pig`/`Chicken.finalizeSpawn`)
- fix: `@Inject(method = "finalizeSpawn", at = @At("HEAD"))` on `Cow`, `Pig` and `Chicken` (1.21.1 overrides; inject into `Animal.finalizeSpawn`/`AgeableMob` with an `instanceof` guard where the class has no override): run VB's variant selection, then `family.pickRandom`. Then cancel VB's RETURN-time `vb$finalizeSpawn` for these three (priority-1500 `@Inject` HEAD cancellable on `vb$finalizeSpawn`, `remap = false`) and drop the add-on's RETURN-time pick for them. Mooshrooms extend Cow in 1.21.1, so keep `SoundVariants.familyOf` as the guard.
- config_gate: `farm_animal_variants`

### SPAWN-12  Camel spawn-surface tag is `minecraft:camel_spawnable_on` (native `camels_spawnable_on`)
- severity: data
- vb: `core/data/tags/ModBlockTags.java:21`: `CAMELS_SPAWNABLE_ON = TAGS.register("camel_spawnable_on")`, with data `data/minecraft/tags/block/camel_spawnable_on.json` = `["#minecraft:sand"]`. It is used by `MobSpawns.java:15-17`.
- native: `Camel.java:145-149`: `level.getBlockState(pos.below()).is(BlockTags.CAMELS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos)`. `data/minecraft/tags/block/camels_spawnable_on.json` = `["#minecraft:sand"]`.
- difference: the contents are identical (sand, red sand, suspicious sand), so vanilla spawning matches. A datapack written for 26.2 that edits `minecraft:camels_spawnable_on` (for example adding sandstone for a camel farm platform) has no effect in VB, and VB's id is not a native tag.
- fix_kind: data-override + mixin-vb
- fix: ship `data/minecraft/tags/block/camels_spawnable_on.json` = `{"values":["#minecraft:sand"]}`. Then make `camel_spawnable_on.json` = `{"values":["#minecraft:camels_spawnable_on"]}` so either id works, or `@WrapOperation` the `BlockState.is(TagKey)` in `MobSpawns.checkCamelSpawnRules` to use `TagKey.create(Registries.BLOCK, withDefaultNamespace("camels_spawnable_on"))`.
- config_gate: `camel_spawns`

---

## Engine drift

### SPAWN-13  Villager → zombie villager conversion rolls equipment, including VB's iron spear, and loot pickup
- severity: gameplay (engine)
- vb/1.21.1: `src-121 .../monster/Zombie.java:428` `villager.convertTo(EntityType.ZOMBIE_VILLAGER, false)`, then `finalizeSpawn(..., MobSpawnType.CONVERSION, new ZombieGroupData(false, true))`. In `Zombie.finalizeSpawn`, `:473` `setCanPickUpLoot(random.nextFloat() < 0.55F * f)` and `:505-506` `populateDefaultEquipmentSlots(...); populateDefaultEquipmentEnchantments(...)` run for every reason. VB's `ZombieMixin` plus the add-on's `ZombieSpearMixin` turn roll 1 into an iron spear.
- native: `monster/zombie/Zombie.java:461-463` `if (spawnReason != EntitySpawnReason.CONVERSION) { setCanPickUpLoot(...); }` and `:496-499` `if (spawnReason != CONVERSION) { populateDefaultEquipmentSlots; populateDefaultEquipmentEnchantments }`. `ConversionParams.single(villager, true, true)` (`:258-260`) copies `canPickUpLoot` (always true for villagers).
- difference: in VB a converted zombie villager:
  - gets armour with chance 0.15 × the special multiplier;
  - gets a weapon with chance 1% (5% on Hard), split sword 1/6, **iron spear 1/6**, shovel 4/6, possibly enchanted;
  - can pick up loot only with chance 0.55 × special.
  Native conversions never get gear and can always pick up loot. In a curing or trading-hall setup on Hard, about 0.8% of conversions arrive with a spear and up to 15% wear armour (drop chance 0.085).
- fix_kind: engine
- fix: a full fix changes vanilla behaviour, so put it behind an add-on option that is off by default: `@WrapWithCondition` on the two populate calls and on `setCanPickUpLoot` in `Zombie.finalizeSpawn` → `reason != CONVERSION`, plus `zombieVillager.setCanPickUpLoot(villager.canPickUpLoot())` at the conversion site. A VB-only alternative: skip the spear substitution in `ZombieSpearMixin` while a CONVERSION `finalizeSpawn` is running (ThreadLocal), which leaves the vanilla shovel.
- config_gate: none (vanilla part); `has_spears` (spear)

### SPAWN-14  1.21.1 skips the door-breaking draw for drowned, so the RNG stream is offset
- severity: cosmetic (RNG order only)
- vb/1.21.1: `src-121 .../monster/Zombie.java:504` `setCanBreakDoors(this.supportsBreakDoorGoal() && randomsource.nextFloat() < f * 0.1F)`. Drowned's `supportsBreakDoorGoal()` is false, so the `nextFloat` is skipped.
- native: `monster/zombie/Zombie.java:495` `setCanBreakDoors(random.nextFloat() < difficultyModifier * 0.1F)`. The float is always drawn.
- difference: each 1.21.1 drowned `finalizeSpawn` makes one fewer level-RNG draw before the equipment pass. As a result, the trident (`>0.9`, `nextInt(16)`), shell (0.03) and zombie-nautilus (0.5) rolls read the stream one position early. Rates are unchanged (11.25% of natural drowned carry a trident; half of those outside `#more_frequent_drowned_spawns` become jockeys).
- fix_kind: engine
- fix: optional. `@WrapOperation` the `supportsBreakDoorGoal()` call at that site so `random.nextFloat()` is drawn first and the result is then ANDed. It affects only the RNG stream, so it is safe to apply globally.
- config_gate: none

### SPAWN-15  1.21.1 re-checks the global mob cap at each chunk; native checks it once per tick
- severity: farm (engine; multiplayer)
- vb/1.21.1:
  - `src-121/net/minecraft/server/level/ServerChunkCache.java:359-371`: for each shuffled ticking chunk, `NaturalSpawner.spawnForChunk(level, chunk, spawnState, spawnFriendlies, spawnEnemies, flag)`.
  - `NaturalSpawner.java:101-116`: per chunk, `spawnState.canSpawnForCategory(mobcategory, chunk.getPos())`.
  - `:524-527`: `int i = category.getMaxInstancesPerChunk() * spawnableChunkCount / MAGIC_NUMBER; return mobCategoryCounts.getInt(category) >= i ? false : localMobCapCalculator.canSpawn(category, pos);`.
  - `afterSpawn` (`:493-513`) increments `mobCategoryCounts`, so the global cap is enforced as the tick goes.
- native:
  - `ServerChunkCache.java:381-401`: `spawningCategories = NaturalSpawner.getFilteredSpawningCategories(spawnCookie, spawnEnemies, spawnPersistent)` once, then `tickSpawningChunk` for each shuffled chunk.
  - `NaturalSpawner.java:104-112`: `... && state.canSpawnForCategoryGlobal(mobCategory)`, evaluated once per tick.
  - `:120-128`: per chunk only `state.canSpawnForCategoryLocal(mobCategory, chunk.getPos())`.
- difference: with one player, the per-player local cap (70 monsters) is updated per spawn in both versions, so results match. With several players in different areas, native keeps spawning in every chunk whose local cap is free for the rest of the tick once the global count was below `70 × chunks / 289` at tick start. 1.21.1 stops all spawning the moment the running global count reaches it. Multiplayer servers running monster farms see native overshoot the global monster cap within single ticks. That applies to VB-content farms too (sulfur cubes in sulfur caves, parched and husks in deserts, drowned and zombie-nautilus jockeys), so their output near the cap is somewhat lower in VB.
- fix_kind: engine
- fix: none that leaves vanilla unchanged. A faithful port would mean `@Redirect`ing `spawnForChunk`'s `canSpawnForCategory` to a local-only check, after a once-per-tick global filter computed in `tickChunks`. That changes every vanilla mob farm, so offer it only as an opt-in "26.x spawn cap" add-on option.
- config_gate: none

### SPAWN-16  1.21.1 chunk-generation creature spawns ignore `doMobSpawning`
- severity: gameplay (engine)
- vb/1.21.1: `src-121/net/minecraft/world/level/NaturalSpawner.java:347-350`: `spawnMobsForChunkGeneration(...) { ... if (!weightedrandomlist.isEmpty()) { ... while (random.nextFloat() < getCreatureProbability()) ...`. There is no gamerule test.
- native: `NaturalSpawner.java:364-369`: `if (!mobs.isEmpty() && level.getLevel().getGameRules().get(GameRules.SPAWN_MOBS)) { ...`.
- difference: with `doMobSpawning false`, new 1.21.1 chunks still generate creature packs: camels in deserts (SPAWN-04 rates), cold and warm cow/pig/chicken variants, sheep colours, and badlands animals once SPAWN-03 is fixed. Native generates none. Pre-generated controlled worlds or test worlds (for example a world built with spawning off, then filled with hand-placed camels or variants) gain extra animals in VB. The rest of the routine is identical (draw order, `getTopNonCollidingPos`, 4 attempts, ±4 walk, CHUNK_GENERATION `finalizeSpawn`).
- fix_kind: engine
- fix: none without changing vanilla 1.21.1 behaviour. An opt-in add-on option could `@Inject(method = "spawnMobsForChunkGeneration", at = @At("HEAD"), cancellable = true)` and return when `!levelAccessor.getLevel().getGameRules().getBoolean(RULE_DOMOBSPAWNING)`.
- config_gate: none

### Noted without a finding
- **N1 (leader health):** native `Zombie.handleAttributes` heals to the new max after the leader bonus (`Zombie.java:554-556`). 1.21.1 does not, so 1.21.1 leaders (5% × special) start at 20 of up to 80 HP. This is vanilla; VB jockey riders only inherit it.
- **N2 (reinforcement type):** native reinforcements are `this.getType()` (husks call husks, drowned call drowned). 1.21.1 always spawns `new Zombie(level)` (`src-121 Zombie.java:311`). REINFORCEMENT never rolls VB jockeys, so VB content is unaffected.
- **N3 (conversion copies):** native `ConversionType.SINGLE` also copies effects, absorption, leash, passengers, handedness, silence, no-gravity, tags and custom data. 1.21.1 `Mob.convertTo` copies baby, noAi, name, persistence, invulnerability, equipment and vehicle. For VB content (spear, baby, persistence, camel-husk vehicle) the result matches.
- **N4 (trial spawners):** native `Mob.checkMobSpawnRules` skips `isValidSpawn` for SPAWNER and TRIAL_SPAWNER (`Mob.java:819-824`); 1.21.1 skips it only for SPAWNER (`src-121 Mob.java:878-883`). No trial-spawner config holds a VB mob, and trial zombies and husks use vanilla tables, so this is vanilla-only.
- **N5 (spawn point):** native blocks spawns within 24 blocks of the respawn point only in the respawn dimension (`NaturalSpawner.isRightDistanceToPlayerAndSpawnPoint`). 1.21.1 blocks the same coordinates in every dimension (`level.getSharedSpawnPos()`). No VB mob spawns naturally outside the overworld, so this is vanilla-only.
