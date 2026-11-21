# variants — VB 1.2.2 vs native 26.2 parity audit

**Summary.** 22 findings: 3 gameplay, 6 audio, 6 visual, 5 cosmetic, 2 data. (Finding 21 is gameplay but is a NeoForge bug fix, so we advise leaving it alone.)
**Covered:** Spring to Life farm-animal variants (cow, pig, chicken), blue and brown eggs (items, thrown eggs, laying loot, dispensers, recipes), sheep colour spawn rules and the wool undercoat, wolf, cat and frog variant hooks, wolf sound variants, the spawn-condition and priority API, variant inheritance on breeding, variant models (cold and warm cow, cold pig, cold chicken) and textures, spawn-egg and dye-palette item rendering, player and food predicates, and baby models, textures, sounds and hitboxes for the mobs in this area.
**Important context:** VB ships **no** data for its `cat_variants`, `frog_variants` or `wolf_variants` registries. Platform's BuiltInRegistryReloadListener (in `Platform-neoforge-1.21.1-1.4.2.jar`) loads `data/*/cat_variants` and the like, and VB has none of those files. So cats, frogs and wolves fall through to vanilla 1.21.1 logic, and that logic was verified to give the same spawn results as native 26.2.
**Not checked:** `.ogg` content, because sounds are not in the jar. The native `sounds.json` used here comes from the local **26.1.2** asset index (index 30). The 26.2 index (32) is not on disk, so the volume and pitch figures assume the two match. Native NeoForge `c:` tag contents were read from `src-121` NeoForgeBiomeTagsProvider. Baby-model geometry is cited by file, not transcribed.
**Out of area:** the files in scope's `client/level/model/entity/*` and `renderer/entity/mob/*` scopes that belong to other features (CopperGolem, Creaking, HappyGhast, Nautilus, SulfurCube, Parched, CamelHusk, ZombieNautilus, ZombieHorse). Their models and renderers are left to those features' audits.

## Coverage

| VB class / data | Native counterpart | Result |
|---|---|---|
| `common/level/entities/mob/animal/cow/CowVariant(s)` | `world/entity/animal/cow/CowVariant(s)`, `data/minecraft/cow_variant/*` | findings 03, 08, 10 (spawn priorities, biome checks and model enum match) |
| `.../pig/PigVariant(s)` | `animal/pig/PigVariant(s)`, `pig_variant/*` | findings 04, 09, 10 (spawn data matches) |
| `.../chicken/ChickenVariant(s)` | `animal/chicken/ChickenVariant(s)`, `chicken_variant/*` | findings 05, 10 (spawn data matches) |
| `.../cat/CatDataVariant(s)` (empty registry) | `animal/feline/CatVariant(s)`, `cat_variant/*` | spawn results match through vanilla 1.21.1; findings 06, 11, 21 |
| `.../frog/FrogDataVariant(s)` (empty registry) | `animal/frog/FrogVariant(s)`, `frog_variant/*` | match (1.21.1 frog tags are identical to 26.2 and do not overlap; textures are pixel-identical) |
| `.../wolf/WolfDataVariant(s)` (empty registry) | `animal/wolf/WolfVariant(s)`, `wolf_variant/*` | spawn results match through 1.21.1 `WolfVariants.getSpawnVariant` (same biome per variant, pale as fallback); finding 11 |
| `.../sheep/SheepColorSpawnRules` + `SheepMixin` | `animal/sheep/SheepColorSpawnRules`, `Sheep.finalizeSpawn` | weights match exactly; findings 07 (tags) and 20 (RNG) |
| `common/api/modules/mob_variant/**` (PriorityProvider, SpawnPrioritySelectors, Biome/Structure/MoonBrightness checks, ModelAndTexture, ClientAsset) | `world/entity/variant/**` | match (selection algorithm is identical); findings 20, 22 |
| `VariantUtils.selectVariantToSpawn` | `variant/VariantUtils.selectVariantToSpawn` | finding 20 (extra roll for `mob_variants_spawn_chance`) |
| `VariantDataHolder.trySetOffspringVariant` | `Cow/Pig/Chicken/Cat/Wolf.getBreedOffspring` | match (50/50 parent pick; the default key always resolves for cow, pig and chicken) |
| `api/modules/sound_variant/**` (WolfSoundVariant(s), Module, Holder) | `animal/wolf/WolfSoundVariant(s)`, `wolf_sound_variant/*` | findings 01, 02; the 7 sets and their `sounds.json` entries match |
| (none) | `cow/pig/chicken/cat_sound_variant/*` and their `*SoundVariant(s)` classes | not implemented in VB: findings 03–06 |
| `api/variant/**` (deprecated shims) | n/a | no behaviour |
| `api/extensions/access/entity/**`, `core/mixin/common/extension/**` (MobMixin, Block*, UseAnim, ServerboundPlayerAction), `client/extension/**`, `sound_extension/ClientLevelMixin`, `common/extension/sound/{Level,ServerLevel}Mixin` | `ServerLevel/ClientLevel.playSeededSound(Entity, …)` | infrastructure only; the sound overloads match native |
| `core/mixin/common/mob_variants/{Cow,Pig,Chicken,Cat,Frog,Wolf,Sheep}Mixin` | the native entity classes | findings 01, 18, 20 (CowMixin also writes a harmless `variant` tag on mooshrooms, which extend Cow in 1.21.1) |
| `ThrownEggMixin` | `ThrownEgg.onHit` | match (1/8 chance, then 1/32 for four chicks; variant comes from the item component) |
| `ModItems` blue_egg / brown_egg | `Items.BLUE_EGG` / `BROWN_EGG` | match (stack size 16, variant component, dispenser projectile; textures are pixel-identical) |
| `data/minecraft/loot_table/gameplay/chicken_lay.json` | same path | finding 18 (the predicate means the same as native) |
| `data/minecraft/tags/item/eggs.json`, `vanillabackport` dynamic cake and pumpkin-pie recipes | `tags/item/eggs.json`, `recipe/cake.json`, `pumpkin_pie.json` | match (tag is identical; recipes accept `#eggs`) |
| `data/minecraft/tags/worldgen/biome/spawns_{cold,warm}_variant_farm_animals.json` | same path | finding 07 |
| `data/minecraft/fieldguide/variants/*` | n/a (compatibility data for another mod) | no native counterpart |
| `client/api/modules/mob_rendering/{Abstract,Cow,Pig,Chicken,Cat,Frog,Wolf}VariantRenderer`, `DynamicMobRendererRegistry`, `SpecialMobRenderer`, `RenderConditions` | `CowRenderer`, `PigRenderer`, `ChickenRenderer`, `CatRenderer`, `WolfRenderer`, `FrogRenderer` | findings 08, 10, 11 |
| `core/mixin/client/entity_rendering/LivingEntityRendererMixin` | `LivingEntityRenderer` | infrastructure; the texture and model swaps are correct for adults |
| `client/level/model/entity/cow/{CowVariant,Cold,Warm}CowModel` | `model/animal/cow/{CowModel,ColdCowModel,WarmCowModel}` | match (geometry, UVs and 64×64 size are identical) |
| `client/level/model/entity/pig/ColdPigModel` | `model/animal/pig/ColdPigModel` | finding 09 |
| `client/level/model/entity/chicken/ColdChickenModel` | `model/animal/chicken/ColdChickenModel` | match (the beak and comb are root parts with the head pivot, so they render the same as native head children) |
| `client/level/model/entity/AdultAndBabyModelPair`, `renderer/entity/mob/AgeableMobRenderer` | same-named native classes | match; VB does not use them for vanilla mobs (finding 10) |
| `client/level/layer/SheepWoolUndercoatLayer` | `layers/SheepWoolUndercoatLayer` | findings 14, 15, 16 (the model, `SHEEP` body layer and texture pixels match) |
| `client/level/renderer/item/SpawnEggRenderer` | `items/*_spawn_egg.json`, `models/item/*_spawn_egg.json` | match (88 untinted models; textures are binary-identical) |
| `client/level/renderer/item/DyePaletteRenderer` | `textures/item/*_dye.png` | finding 19 |
| `common/predicates/{Food,Player}Predicate`, `ModEntitySubPredicates` | `advancements/predicates/FoodPredicate`, `entity/PlayerPredicate` | match (native's extra `input` field needs the 1.21.2+ engine and lunge does not use it) |
| `ModEntityDataSerializers` (`dye_color`) | `EntityDataSerializers` | infrastructure |
| `ModSyncedEntityData` (variant keys and defaults) | `DATA_VARIANT_ID` / `DATA_SOUND_VARIANT_ID` defaults | match (defaults are temperate, classic, pale, tabby and temperate) |
| textures `entity/{cow,pig,chicken}/{cold,warm}_*.png`, `sheep_wool_undercoat.png` | `entity/*/<mob>_{cold,warm}.png`, `sheep_wool_undercoat.png` | pixel-identical; VB's `warm_pig` is 64×32 and native's is 64×64 with an empty bottom half, which is equivalent under the 64×32 PigModel |
| `sounds.json` (42 `entity.wolf_*` events) | 26.1.2 `sounds.json` | match |

---

### VARIANTS-01  Wolf variant hurt sound replaces the wolf-armor damage sound
- severity: audio
- vb: `core/mixin/common/mob_variants/WolfMixin.java:94-104` (`vb$getHurtSound`, `@At("HEAD")`, cancellable) → `WolfSoundVariantsModule.java:22-24`: `return !VanillaBackport.COMMON_CONFIG.hasWolfSoundVariants.get() ? null : (SoundEvent)((WolfSoundVariant)wolf.vb$getSoundVariant()).hurtSound().value();`. This returns the variant hurt sound for every damage source.
- native: `world/entity/animal/wolf/Wolf.java:294-296`: `return this.canArmorAbsorb(source) ? SoundEvents.WOLF_ARMOR_DAMAGE : this.getSoundSet().hurtSound().value();`. Vanilla 1.21.1 (`src-121 .../animal/Wolf.java:233-235`) also plays `WOLF_ARMOR_DAMAGE` in this case.
- difference: when a wolf wearing armor takes damage the armor absorbs, VB plays the wolf's hurt sound (`entity.wolf_<set>.hurt`). Native and 1.21.1 play `item.wolf_armor.damage`.
- fix_kind: mixin-vb (`WolfMixin.vb$getHurtSound`), or mixin-mc on `Wolf.getHurtSound` at a higher priority.
- fix: `@Inject(method="getHurtSound", at=@At("HEAD"), cancellable=true, priority=1100)` on `net.minecraft.world.entity.animal.Wolf`: `if (((Wolf)(Object)this).canArmorAbsorb(source)) cir.setReturnValue(SoundEvents.WOLF_ARMOR_DAMAGE);`. Priority 1100 makes it apply before VB's injection (default priority 1000). An alternative is to `@WrapMethod` VB's handler and skip it when `canArmorAbsorb(source)`. `canArmorAbsorb` is private in 1.21.1, so call it through an `@Invoker` accessor on Wolf.
- config_gate: `wolf_sound_variants` (the bug only appears when it is true)

### VARIANTS-02  Wolves have no baby sound set and footsteps are not variant-driven
- severity: audio
- vb: `WolfSoundVariant.java:9-16`: the record holds six adult sounds (`ambientSound, deathSound, growlSound, hurtSound, pantSound, whineSound`) with no baby set and no step sound. `WolfSoundVariantsModule.getAmbientSound/getHurtSound/getDeathSound` never check `isBaby()`, and `playStepSound` stays vanilla (`SoundEvents.WOLF_STEP, 0.15F, 1.0F`).
- native: `data/minecraft/wolf_sound_variant/*.json` hold `adult_sounds` and `baby_sounds`. Every variant's `baby_sounds` maps to `entity.baby_wolf.{ambient,death,growl,hurt,pant,step,whine}`, and `adult_sounds.step_sound` is `entity.wolf.step`. `Wolf.java:175-176`: `getSoundSet() { return this.isBaby() ? ...babySounds() : ...adultSounds(); }`. `Wolf.java:235-236`: `this.playSound(this.getSoundSet().stepSound().value(), 0.15F, 1.0F)`. In 26.1.2 `sounds.json`, all `entity.baby_wolf.*` events except step use `pitch 0.75` (files `mob/wolf/baby/*`), and step uses `mob/wolf/baby/step1-5`.
- difference: native baby wolves of every sound variant use one shared baby set (pitch 0.75 in `sounds.json`, then ×1.5 from `getVoicePitch`) and their own footstep sound. VB babies use their adult variant's sounds and the adult step sound.
- fix_kind: mixin-mc (`Wolf`) plus asset-override (`assets/minecraft/sounds.json` and `sounds/mob/wolf/baby/*.ogg`). Sound events must be registered for `entity.baby_wolf.*`.
- fix: register the seven `SoundEvent`s `minecraft:entity.baby_wolf.{ambient,death,growl,hurt,pant,step,whine}` and copy their `sounds.json` entries from 26.x. In `Wolf`, add `@Inject(method={"getAmbientSound","getDeathSound"}, at=@At("HEAD"), cancellable=true, priority=1100)` and the same for `getHurtSound(DamageSource)`, placed after the VARIANTS-01 armor check. When `isBaby()`, return the baby event and use the same `isAngry → growl`, `random.nextInt(3)==0 → (isTame && health<20 ? whine : pant)`, `else ambient` logic as `WolfSoundVariantsModule.getAmbientSound`. Add `@Inject(method="playStepSound", at=@At("HEAD"), cancellable=true)`: when `isBaby()`, call `playSound(BABY_WOLF_STEP, 0.15F, 1.0F)` and cancel.
- config_gate: `wolf_sound_variants`; baby sounds are not gated in native.

### VARIANTS-03  Cow sound variants (classic / moody) are missing
- severity: audio
- vb: none. `CowMixin` adds only `CowVariant`, and 1.21.1 `Cow.getAmbientSound/getHurtSound/getDeathSound/playStepSound` return the fixed `COW_AMBIENT/HURT/DEATH/STEP`.
- native: `animal/cow/Cow.java:86-87`: `finalizeSpawn` runs `this.setSoundVariant(CowSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()))`, a uniform pick over `classic` and `moody`. `Cow.java:57-60` saves and loads `"sound_variant"`. `AbstractCow.java:61-77` takes ambient, hurt, death and step (step at volume 0.15, pitch 1.0) from `getSoundSet()`. `cow_sound_variant/moody.json` uses `entity.cow_moody.{ambient,death,hurt,step}`, with step reusing `mob/cow/step1-4`. Offspring are not given a sound variant in `getBreedOffspring`, so they keep the synced default `classic`. Cow sound sets have no baby/adult split.
- difference: about half of naturally spawned native cows use the "moody" sound set. Every VB cow uses classic sounds.
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.Cow`) plus asset-override (sounds).
- fix: register `minecraft:entity.cow_moody.{ambient,death,hurt,step}` and copy their 26.x `sounds.json` entries and `.ogg` files (`mob/cow/moody/*`). In a Cow mixin, add a synced key (VB's `SyncedDataKey` pattern, default `classic`) named `sound_variant` with values `minecraft:classic` and `minecraft:moody`. Save it as the string `"sound_variant"` in `addAdditionalSaveData`/`readAdditionalSaveData`. At `finalizeSpawn` HEAD (native picks before `super`), set it with `level.getRandom().nextInt(2)`. Do not set it in `getBreedOffspring`. Redirect `getAmbientSound/getHurtSound/getDeathSound/playStepSound` to the chosen set. Apply this to `Cow` only, not `MushroomCow`, which is always classic natively (`AbstractCow.getSoundSet`).
- config_gate: none (gate under `farm_animal_variants` if a toggle is wanted)

### VARIANTS-04  Pig sound variants (classic / big / mini) and baby pig sounds are missing
- severity: audio
- vb: none. 1.21.1 `Pig` uses the fixed `PIG_AMBIENT/HURT/DEATH/STEP`, and its eat sound comes from `Animal`.
- native: `animal/pig/Pig.java:316-321`: `finalizeSpawn` → `PigSoundVariants.pickRandomSoundVariant(...)` (uniform over 3). `Pig.java:281-282`: `getSoundSet() = isBaby() ? babySounds() : adultSounds()`. `:141-162`: ambient, hurt, death, `playEatingSound → makeSound(eatSound)`, and step (0.15, 1.0). `pig_sound_variant/{big,mini}.json` use `entity.pig_big.*` and `entity.pig_mini.*` (step is `entity.pig.step`). Every `baby_sounds` maps to `entity.baby_pig.{ambient,death,eat,hurt,step}`; those events use pitch 0.75 except eat. The variant is saved as `"sound_variant"`, and offspring keep the default `classic`.
- difference: about two-thirds of native pigs sound "big" or "mini", and all native piglets use the baby pig set. VB pigs and piglets all use classic adult sounds.
- fix_kind: mixin-mc (`Pig`) plus asset-override (sounds).
- fix: follow the pattern in VARIANTS-03 with values `classic`, `big` and `mini`, picked with `level.getRandom().nextInt(3)`, plus a branch on `isBaby()` that returns the `entity.baby_pig.*` events (register 5 + 4 + 4 + 5 events). Also override `Animal.playEatingSound`/`makeSound` for pigs so the eat sound comes from the set.
- config_gate: none

### VARIANTS-05  Chicken sound variants (classic / picky) and baby chicken sounds are missing
- severity: audio
- vb: none. 1.21.1 `Chicken` uses the fixed `CHICKEN_AMBIENT/HURT/DEATH/STEP`.
- native: `animal/chicken/Chicken.java:104-105`: `getSoundSet()` branches on `isBaby()`. `:156-172` take ambient, hurt, death and step (0.15, 1.0) from the set. `:190` picks a random sound variant in `finalizeSpawn`. `chicken_sound_variant/picky.json` uses `entity.chicken_picky.{ambient,death,hurt}` with step `entity.chicken.step`. `baby_sounds` map to `entity.baby_chicken.{ambient,death,hurt,step}`; ambient is pitch 0.75 with weights 3 and 1. Chicks hatched from thrown eggs (`ThrownEgg.onHit`, which does not call `finalizeSpawn`) keep `classic`.
- difference: about half of native adult chickens are "picky", and all native chicks use baby sounds. VB has neither.
- fix_kind: mixin-mc (`Chicken`) plus asset-override (sounds).
- fix: same pattern as VARIANTS-03 with values `classic` and `picky`, plus a baby branch. Do not set the variant in `ThrownEggMixin` or `getBreedOffspring`.
- config_gate: none

### VARIANTS-06  Cat sound variants (classic / royal) and baby cat sounds are missing
- severity: audio
- vb: none. 1.21.1 `Cat` uses the fixed `CAT_PURR/PURREOW/AMBIENT/STRAY_AMBIENT/HISS/HURT/DEATH/EAT/BEG_FOR_FOOD`.
- native: `animal/feline/Cat.java:404-410`: `finalizeSpawn` runs `super` first, then the variant, then `this.setSoundVariant(CatSoundVariants.pickRandomSoundVariant(...))`. `:142` `getSoundSet()` branches on `isBaby()`. `:259-316` take every cat sound (ambient, purr, purreow, stray ambient, hiss, hurt, death, eat at 1.0/1.0, beg-for-food at 1.0/1.0, and purr at `0.6F + 0.4F*(rand-rand)`) from the set. `cat_sound_variant/royal.json` uses `entity.cat_royal.*`. `baby_sounds` use `entity.baby_cat.*`; ambient, death and hurt are pitch 0.75, and eat and hiss are pitch 2.0.
- difference: about half of native cats use "royal" sounds, and all native kittens use the baby set. VB has neither.
- fix_kind: mixin-mc (`Cat`) plus asset-override (sounds).
- fix: same pattern as VARIANTS-03 with values `classic` and `royal` and a baby branch. Pick after `super.finalizeSpawn`, which matches native's order for cats. Redirect each `SoundEvents.CAT_*` use in `Cat` (`getAmbientSound`, `getHurtSound`, `getDeathSound`, `hiss`, `playEatingSound`/eat, the `BEG_FOR_FOOD` playSound in `CatTemptGoal`/`tick`, and the purr in `CatRelaxOnOwnerGoal`) with `@ModifyArg` or `@Redirect` on `SoundEvents.CAT_*` field reads (`@At(value="FIELD", target=".../SoundEvents;CAT_AMBIENT:...")`).
- config_gate: none

### VARIANTS-07  Farm-animal biome tags pull in NeoForge `c:` tags, which changes variants in stony_shore, mushroom_fields and stony_peaks
- severity: gameplay
- vb: `data/minecraft/tags/worldgen/biome/spawns_cold_variant_farm_animals.json` lists 21 explicit entries (no `minecraft:windswept_hills`; it adds VB's `minecraft:dappled_forest`), plus optional `#c:is_cold`, `#c:is_snowy`, `#c:climate_cold`, `#c:snowy`, `#c:icy`. `spawns_warm_variant_farm_animals.json` lists the native entries plus optional `#c:is_hot` and `#c:climate_hot`. NeoForge 21.1 (`NeoForgeBiomeTagsProvider.java:37-88`) fills `c:is_hot_overworld` with `jungle, bamboo_jungle, sparse_jungle, desert, badlands, wooded_badlands, eroded_badlands, savanna, savanna_plateau, windswept_savanna, stony_peaks, mushroom_fields, warm_ocean`, and `c:is_cold_overworld` with `taiga, …, windswept_hills, …, stony_shore, …`. All of these are consumed by `ChickenVariants`, `CowVariants`, `PigVariants` (each `RawBiomeCheck`, priority 1) and `SheepColorSpawnRules.getSheepColorConfiguration`, which checks warm first.
- native: `data/minecraft/tags/worldgen/biome/spawns_cold_variant_farm_animals.json`: `snowy_plains, ice_spikes, frozen_peaks, jagged_peaks, snowy_slopes, frozen_ocean, deep_frozen_ocean, grove, deep_dark, frozen_river, snowy_taiga, snowy_beach, #is_end, cold_ocean, deep_cold_ocean, old_growth_pine_taiga, old_growth_spruce_taiga, taiga, windswept_forest, windswept_gravelly_hills, windswept_hills, stony_peaks`. `spawns_warm_variant_farm_animals.json`: `desert, warm_ocean, #is_jungle, #is_savanna, #is_nether, #is_badlands, mangrove_swamp, deep_lukewarm_ocean, lukewarm_ocean`.
- difference: in VB, **stony_shore** is cold (native: temperate). **mushroom_fields** is warm (native: temperate); this affects spawn-egg and command-spawned cows, pigs and chickens, and the sheep colour table. **stony_peaks** is in both tags, so a cow, pig or chicken there picks warm or cold 50/50 and sheep get the warm table (native: always cold). Every modded biome in the `c:` tags also gets variants that native would not give it. windswept_hills is cold in both only because VB includes `c:is_cold`.
- fix_kind: data-override (`data/minecraft/tags/worldgen/biome/spawns_cold_variant_farm_animals.json` and `spawns_warm_variant_farm_animals.json`)
- fix: ship both files with `"replace": true` and the native `values` lists quoted above, exactly in that order. Optionally keep `"minecraft:dappled_forest"` (a VB-only biome with no native counterpart) in the cold list if the dappled-forest feature should keep cold animals.
- config_gate: `farm_animal_variants` (the tags are only read when it is true)

### VARIANTS-08  Temperate cow keeps the 1.21.1 model and texture
- severity: visual
- vb: `client/api/modules/mob_rendering/CowVariantRenderer.java:29`: `map.put(CowVariant.ModelType.NORMAL, null);`. `AbstractVariantRenderer.java:45`: `return variant.filter(v -> !this.isDefaultVariant((V)v)).map(...)`, so temperate cows render with the vanilla 1.21.1 `CowModel` and `textures/entity/cow/cow.png` (64×32). The 1.21.1 `CowModel.createBodyLayer` (`src-121 .../client/model/CowModel.java`) has horns `addBox(-5,-5,-4,1,3,1)` and `(4,-5,-4,…)`, no snout cube, front legs at `PartPose.offset(±4, 12, -6)`, no mirrored legs, and `LayerDefinition.create(mesh, 64, 32)`.
- native: `client/model/animal/cow/CowModel.java` `createBaseCowModel()` (the same geometry as VB's own `CowVariantModel.createBaseCowModel()`) has a snout `texOffs(1,33).addBox(-3,1,-7,6,3,1)`, horns at z −5, front legs at z −5, left legs `mirror()`, and 64×64. `CowRenderer.java:38-41` maps temperate to `entity/cow/cow_temperate.png` (64×64; 264 changed pixels and 72 new pixels compared with 1.21.1 `cow.png`).
- difference: native temperate cows have the Spring to Life model (snout, moved horns and front legs, mirrored left legs) and the new texture. VB keeps the old cow, so temperate cows look inconsistent next to VB's warm and cold cows.
- fix_kind: mixin-vb (`CowVariantRenderer.bakeModels`, `AbstractVariantRenderer.getTexture`) plus asset-override (`assets/minecraft/textures/entity/cow/cow.png`).
- fix: (a) register a layer `vanillabackport_parity:temperate_cow` → `LayerDefinition.create(CowVariantModel.createBaseCowModel(), 64, 64)`, which is reachable as package-private static or through an accessor. Add `@Inject(method="bakeModels", at=@At("RETURN"))` on `CowVariantRenderer` that does `cir.getReturnValue().put(NORMAL, new CowVariantModel<>(ctx.bakeLayer(TEMPERATE_COW)))`. (b) Override `textures/entity/cow/cow.png` with the 26.2 `cow_temperate.png` (64×64). This also fixes the case where `farm_animal_variants=false`, because the vanilla model then still needs a 64×32-compatible texture, so in that case gate (b) together with a mixin-mc on `CowRenderer` that sets the model to the new layer. `MushroomCowRenderer` also uses the cow model natively (`ModelLayers.MOOSHROOM` = the same base); it belongs to the mooshroom audit.
- config_gate: `farm_animal_variants`

### VARIANTS-09  Pig models do not mirror the left-leg UVs
- severity: cosmetic
- vb: `client/level/model/entity/pig/ColdPigModel.java:23`: `MeshDefinition mesh = QuadrupedModel.createBodyMesh(6, CubeDeformation.NONE);`. Temperate and warm pigs use 1.21.1 `PigModel.java:21`, which makes the same call with no mirroring.
- native: `client/model/animal/pig/PigModel.java:28`: `QuadrupedModel.createBodyMesh(6, true, false, g)`. In `QuadrupedModel.java:56-61`, `leftLeg = CubeListBuilder.create().mirror(mirrorLeftLeg)…` is applied to `left_hind_leg` and `left_front_leg`. `ColdPigModel` builds on `createBasePigModel`, so it is mirrored too.
- difference: the UVs on both left legs of every native pig (temperate, warm, cold) are mirrored horizontally. VB's are not.
- fix_kind: mixin-mc (`net.minecraft.client.model.PigModel.createBodyLayer`) plus mixin-vb (`ColdPigModel.createBodyLayer`).
- fix: in both methods, add `@ModifyVariable(at=@At(value="STORE"), ordinal=0)` (or a `RETURN` rebuild) that replaces the two left legs: `root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().mirror().texOffs(0,16).addBox(-2,0,-2,4,6,4, g), PartPose.offset(3,18,7))` and the same for `"left_front_leg"` at `PartPose.offset(3,18,-5)`.
- config_gate: none

### VARIANTS-10  Calves, piglets and chicks use scaled adult models and adult textures
- severity: visual
- vb: `CowVariantRenderer.java:26-32`, `PigVariantRenderer.java:26-31` and `ChickenVariantRenderer.java:26-31` hold one model per variant. Babies get 1.21.1 `AgeableListModel` scaling of that adult model, and `getTexture` returns `variant.modelAndTexture().asset().path()` (the adult texture) for babies. VB's variant codecs (`CowVariant`, `PigVariant`, `ChickenVariant`) have no `baby_asset_id`.
- native: `CowRenderer.java:17-27`, `PigRenderer.java:28-36` and `ChickenRenderer.java:15-27` bake `AdultAndBabyModelPair`s. Every cow model type gets `BabyCowModel` (`ModelLayers.COW_BABY`, `WARM_COW_BABY` and `COLD_COW_BABY` all use `BabyCowModel.createBodyLayer()`, 64×64). Pigs use `BabyPigModel` (32×32) and chickens `BabyChickenModel` (16×16) for both model types. `getTextureLocation` returns `state.isBaby ? variant.babyTexture() : adult`, with `entity/cow/cow_{temperate,warm,cold}_baby`, `entity/pig/pig_{…}_baby` and `entity/chicken/chicken_{…}_baby` (from `baby_asset_id` in `cow_variant/*.json` etc.). Geometry is in `client/model/animal/cow/BabyCowModel.java`, `pig/BabyPigModel.java` and `chicken/BabyChickenModel.java`.
- difference: native babies have dedicated models (for example a calf head 6×6×5 with no udder, or a chick with a 4×4×4 body and 0-thick wings) and variant-specific baby textures. VB shows half-size adults with big heads and adult variant textures.
- fix_kind: mixin-vb (`AbstractVariantRenderer.getModel/getTexture`, `*VariantRenderer.bakeModels`) plus mixin-mc (`CowRenderer`, `PigRenderer`, `ChickenRenderer` for temperate) plus asset-override (copy the nine 26.2 `*_baby.png` textures).
- fix: port the three baby models as `CowModel<Cow>`, `PigModel<Pig>` and `ChickenModel<Chicken>` subclasses with native layer definitions (constructed so `AgeableListModel` scaling is off: pass `babyBodyScale=1`, `babyHeadScale=1` and zero offsets to `QuadrupedModel`, or force `young=false` before render). Register `*_baby` layers. Wrap `AbstractVariantRenderer.getModel`: if `entity.isBaby()`, return the baby model for the type, ignoring variant for pig and chicken, and use one `BabyCowModel` for every cow type. Wrap `getTexture`: if `entity.isBaby()`, return `textures/entity/<mob>/<mob>_<variant>_baby.png`, including for the temperate default (remove the default filter for babies). Add an optional `baby_asset_id` field to the three codecs so datapacks can set it.
- config_gate: `farm_animal_variants` (VB's render condition); the baby redesign is not gated natively.

### VARIANTS-11  Puppies and kittens use scaled adult models and adult textures
- severity: visual
- vb: `WolfVariantRenderer.java:15-21` and `CatVariantRenderer.java:13-15` only swap textures, and only for data-driven variants (none ship), so 1.21.1 `WolfRenderer`/`CatRenderer` render babies as scaled adults with adult textures (`entity/wolf/wolf_*.png`, `entity/cat/*.png`), the adult collar texture, and wolf armor on puppies.
- native: `WolfRenderer.java:5` builds `AgeableMobRenderer` with `new AdultWolfModel(ModelLayers.WOLF)` and `new BabyWolfModel(ModelLayers.WOLF_BABY)`, and `Wolf.getTexture()` (`Wolf.java:155-160`) uses `variant.babyInfo()`, which gives `entity/wolf/wolf_<v>[_tame|_angry]_baby`. `WolfCollarLayer.java:15,32` uses `wolf_collar_baby.png` for babies. `WolfArmorLayer.java:52` skips babies (`&& !state.isBaby`). `CatRenderer.java:5` builds `new BabyCatModel(ModelLayers.CAT_BABY)` (`BabyFelineModel.createBabyLayer()`), with texture `assetInfo(state.isBaby)`, which gives `entity/cat/cat_<v>_baby`. `CatCollarLayer.java:17,24,38-39` uses the baby collar model and `cat_collar_baby.png`. Geometry is in `client/model/animal/wolf/BabyWolfModel.java` and `feline/BabyFelineModel.java`/`BabyCatModel.java`.
- difference: native puppies and kittens have dedicated models and per-variant baby textures, a baby collar, and puppies never render armor. VB has none of these.
- fix_kind: mixin-mc (`WolfRenderer`, `CatRenderer`, `WolfCollarLayer`, `CatCollarLayer`, `WolfArmorLayer`) plus asset-override (27 wolf baby textures, 11 cat baby textures and 2 collar textures from 26.2).
- fix: port `BabyWolfModel` and `BabyCatModel` as 1.21.1 `WolfModel<Wolf>` and `CatModel<Cat>` subclasses with native layer definitions. In each renderer's `render`, choose the model from `entity.isBaby()` (VB's `AgeableMobRenderer` pattern, or the `entity_rendering` model-swap hook). In `getTextureLocation`, append `_baby` to the 1.21.1 variant texture path, mapping each 1.21.1 path to its 26.2 name (for example `entity/cat/tabby` → `entity/cat/cat_tabby_baby`). In the collar layers, use the baby model and texture when the entity is a baby. In `WolfArmorLayer.render`, return early at HEAD if the wolf is a baby.
- config_gate: none

### VARIANTS-12  Lambs use the scaled adult sheep model, texture and wool
- severity: visual
- vb: no VB code. 1.21.1 `SheepRenderer` and `SheepFurLayer` scale the adult model and use `sheep.png` and `sheep_fur.png`. VB's `SheepWoolUndercoatLayer` also draws on lambs (see VARIANTS-15).
- native: `SheepRenderer.java:6-11` builds `AgeableMobRenderer` with `new SheepModel(SHEEP)` and `new BabySheepModel(SHEEP_BABY)`, and the texture is `isBaby ? sheep_baby.png : sheep.png`. `SheepWoolLayer.java:15,25-26` uses the baby wool model (`SHEEP_BABY_WOOL`, which is `BabySheepModel.createBodyLayer()`) and `sheep_wool_baby.png` for lambs. Geometry is in `client/model/animal/sheep/BabySheepModel.java`.
- difference: native lambs have a dedicated model and texture, and their wool layer uses the lamb model and texture.
- fix_kind: mixin-mc (`SheepRenderer`, `SheepFurLayer`) plus asset-override (`entity/sheep/sheep_baby.png`, `sheep_wool_baby.png`).
- fix: port `BabySheepModel` (64×32). Swap the model on `isBaby()` in `SheepRenderer.render`, and return `sheep_baby.png` from `getTextureLocation` when the entity is a baby. In `SheepFurLayer.render`, use a `SheepFurModel` baked from the baby layer and `sheep_wool_baby.png` when the entity is a baby.
- config_gate: none

### VARIANTS-13  Baby hitboxes, eye heights and passenger attachments differ
- severity: gameplay
- vb: VB leaves the 1.21.1 values in place. Chicken: `src-121 .../animal/Chicken.java:39` `EntityType.CHICKEN.getDimensions().scale(0.5F).withEyeHeight(0.2975F)` gives 0.2×0.35, eye 0.2975, passenger (0, 0.35, −0.05). Cow: `Cow.java:35` `scale(0.5F).withEyeHeight(0.665F)` gives 0.45×0.7, eye 0.665, passenger y 0.684375. Pig, sheep, wolf and cat use `LivingEntity.getDefaultDimensions` → `getDimensions().scale(getAgeScale()=0.5)`. Pig: 0.45×0.45, eye 0.3825, passenger 0.434375. Sheep: 0.45×0.65, eye 0.6175, passenger 0.61875. Wolf: 0.3×0.425, eye 0.34, passenger (0, 0.409375, −0.03125). Cat: 0.3×0.35, eye 0.175, passenger 0.25625.
- native: `Chicken.java:59-61` `EntityDimensions.scalable(0.3F, 0.4F).withEyeHeight(0.28125F).withAttachments(...PASSENGER, 0, 0.375F, 0)`. `Cow.java:37-39` `scalable(0.45F, 0.7F).withEyeHeight(0.69F)`, passenger 0.75 (MushroomCow `:61-63` is identical). `Pig.java:70-72` `scalable(0.45F, 0.45F).withEyeHeight(0.40625F)`, passenger 0.5. `Sheep.java:60-62` `scalable(0.45F, 0.65F).withEyeHeight(0.65625F)`, passenger 0.5625. `Wolf.java:104-106` `scalable(0.3F, 0.425F).withEyeHeight(0.34375F)`, passenger 0.4375. `Cat.java:86-88` `scalable(0.3F, 0.35F).withEyeHeight(0.34375F)`, passenger 0.3125. Each class overrides `getDefaultDimensions` to return `isBaby() ? BABY_DIMENSIONS : super…`.
- difference: native chicks are 0.3 wide and 0.4 tall (VB 0.2×0.35). Eye heights differ for every baby (the cat's doubles, 0.175 → 0.34375), which changes line of sight, suffocation checks and ray targeting. Passenger offsets differ as well, which matters for chicken jockeys on chicks and baby zombies riding.
- fix_kind: mixin-mc (`Chicken`, `Cow`, `Pig`, `Sheep`, `Wolf`, `Cat` → `getDefaultDimensions(Pose)`)
- fix: for each class, add `@Inject(method="getDefaultDimensions", at=@At("HEAD"), cancellable=true)` with `if (this.isBaby()) cir.setReturnValue(PARITY_BABY);`, where `PARITY_BABY` is the exact native `EntityDimensions.scalable(w,h).withEyeHeight(e).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0F, y, 0F))` from the values above. Pig, Sheep, Wolf and Cat do not override the method in 1.21.1, so add the override through the mixin (an `@Override` method in a mixin extending the parent).
- config_gate: none

### VARIANTS-14  Sheep wool undercoat is off by default
- severity: visual
- vb: `client/ClientConfig.java:40`: `this.useSheepWoolUndercoat = builder.comment("use the modern colored wool undercoat for sheep").define("use_sheep_wool_undercoat", false);`. `EntityRendering.java:103-105` adds `SheepWoolUndercoatLayer` only when `RenderConditions.SHEEP_UNDERCOAT` is true.
- native: `SheepRenderer.java:10`: `this.addLayer(new SheepWoolUndercoatLayer(this, context.getModelSet()));` always.
- difference: out of the box, sheared coloured sheep in VB show a bare skin. Native always shows the coloured undercoat.
- fix_kind: mixin-vb (`com.blackgear.vanillabackport.client.ClientConfig.<init>`), or the add-on's own layer registration.
- fix: `@ModifyArg(method="<init>", at=@At(value="INVOKE", target="...ModConfigSpec$Builder;define(Ljava/lang/String;Ljava/lang/Object;)...", ordinal=<index of use_sheep_wool_undercoat>), index=1)` → `true`. A sturdier option is for the add-on to add a corrected layer itself (VARIANTS-15) when VB's is disabled.
- config_gate: `use_sheep_wool_undercoat`

### VARIANTS-15  Undercoat is drawn on white sheep and on lambs
- severity: visual
- vb: `client/level/layer/SheepWoolUndercoatLayer.java:41`: `if (sheep.getType() == EntityType.SHEEP && !sheep.isInvisible()) {` followed by an unconditional `coloredCutoutModelCopyLayerRender(...)`.
- native: `layers/SheepWoolUndercoatLayer.java:32`: `if (!state.isInvisible && (state.isJebSheep || state.woolColor != DyeColor.WHITE) && !state.isBaby) {`.
- difference: VB draws the undercoat texture tinted white (`-1644826`) on sheared white sheep and draws it on lambs. Native skips white sheep other than jeb_ and skips all babies.
- fix_kind: mixin-vb (`SheepWoolUndercoatLayer.render`)
- fix: `@Inject(method="render(...Lnet/minecraft/world/entity/animal/Sheep;FFFFFF)V", at=@At("HEAD"), cancellable=true)`: `boolean jeb = sheep.hasCustomName() && "jeb_".equals(sheep.getName().getString()); if (sheep.isBaby() || (!jeb && sheep.getColor() == DyeColor.WHITE)) ci.cancel();`.
- config_gate: `use_sheep_wool_undercoat`

### VARIANTS-16  jeb_ colour cycle uses the old formula (entity-id offset, linear lerp)
- severity: cosmetic
- vb: `SheepWoolUndercoatLayer.java:44-51`: `int offset = sheep.tickCount / 25 + sheep.getId(); … float delta = (sheep.tickCount % 25 + partialTick) / 25.0F; … color = ARGB32.lerp(delta, fromColor, toColor);`. This matches 1.21.1 `SheepFurLayer`.
- native: `SheepRenderState.getWoolColor()` → `ColorLerper.getLerpedColor(ColorLerper.Type.SHEEP, this.ageInTicks)`: `int tickCount = Mth.floor(tick); int value = tickCount / 25; … float subStep = (tickCount % 25 + Mth.frac(tick)) / 25; return ARGB.srgbLerp(subStep, color1, color2);`. There is no `getId()` offset. The wool and undercoat layers share it.
- difference: native jeb_ sheep all cycle in phase, starting from WHITE at age 0, and blend in sRGB space. VB offsets each sheep by its entity id and blends linearly. The palette (`getModifiedColor`, ×0.75, white = `-1644826`) is the same.
- fix_kind: mixin-vb (`SheepWoolUndercoatLayer.render`) plus mixin-mc (`net.minecraft.client.renderer.entity.layers.SheepFurLayer.render`). Patch both so the two layers stay in sync.
- fix: in both, `@Redirect` the `getId()` call to return 0, and `@Redirect` `FastColor$ARGB32.lerp(FII)I` to an sRGB lerp. Port `ARGB.srgbLerp`: convert each channel to linear with the sRGB transfer function, lerp, convert back, and lerp alpha linearly.
- config_gate: none (undercoat side: `use_sheep_wool_undercoat`)

### VARIANTS-17  Sheep wool texture lacks the 26.x touch-up
- severity: cosmetic
- vb: no override; 1.21.1 `assets/minecraft/textures/entity/sheep/sheep_fur.png` is used.
- native: `textures/entity/sheep/sheep_wool.png`, renamed, with 19 pixels brightened. For example (43,11) goes from 223 to 239 grey and (49,29) from 212 to 255, with the rest in rows 25–29 and columns 28 and 47–55.
- difference: VB's wool texture has 19 slightly darker pixels than native.
- fix_kind: asset-override (`assets/minecraft/textures/entity/sheep/sheep_fur.png` ← 26.2 `sheep_wool.png`)
- fix: copy the 26.2 `sheep_wool.png` file over the 1.21.1 path.
- config_gate: none

### VARIANTS-18  Temperate chickens skip the `chicken_lay` loot table, and the egg sound plays before the drop is known
- severity: data
- vb: `ChickenMixin.java:80-97`, in `@ModifyArg` on `spawnAtLocation(ItemLike)` inside `aiStep`: `return variant.isPresent() && !VariantUtils.matches(ChickenVariants.REGISTRIES, variant.get(), ChickenVariants.TEMPERATE) && WorldUtilities.LootUtils.dropFromGiftLootTable(this, …, ModBuiltInLootTables.CHICKEN_LAY, …) ? Items.AIR : originalItem;`. The vanilla 1.21.1 `aiStep` (`Chicken.java:93-97`) has already played `CHICKEN_EGG` and runs `gameEvent(ENTITY_PLACE)` afterwards regardless. VB's `chicken_lay.json` has no `random_sequence`.
- native: `Chicken.java:135-141`: `if (this.dropFromGiftLootTable(level, BuiltInLootTables.CHICKEN_LAY, this::spawnAtLocation)) { this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (rand-rand)*0.2F+1.0F); this.gameEvent(GameEvent.ENTITY_PLACE); } this.eggTime = this.random.nextInt(6000) + 6000;`. The loot table has `"random_sequence": "minecraft:gameplay/chicken_lay"`.
- difference: with vanilla data the results match: the same egg for each variant, the same 6000–11999 tick interval, and the same sound. With a datapack that overrides `chicken_lay`, VB ignores it for temperate chickens, drops a plain egg when the table returns nothing for warm or cold chickens, and always plays the sound and game event. Native drops only what the table yields, and stays silent with no game event when nothing drops. Loot RNG comes from the level random instead of a sequence.
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.Chicken.aiStep`) plus data-override (`data/minecraft/loot_table/gameplay/chicken_lay.json`)
- fix: add `@WrapOperation` on the `--eggTime <= 0` block, or `@Redirect` both `playSound(CHICKEN_EGG…)` and `spawnAtLocation(Items.EGG)`, plus `gameEvent`. Replace the block with the native version: call `dropFromGiftLootTable(CHICKEN_LAY)` for all variants, and play the sound and game event only when it returns true. Disable VB's `vb$modifyEggDrop` by `@WrapMethod` returning `originalItem` unchanged. Add `"random_sequence": "minecraft:gameplay/chicken_lay"` to the JSON and keep VB's NBT predicates.
- config_gate: none

### VARIANTS-19  Modern orange and brown dye textures are each one pixel off
- severity: cosmetic
- vb: `DyePaletteRenderer.renderFirstPerson` draws `vanillabackport:item/dye_palette` and then `vanillabackport:item/<color>_dye`. The composite matches native for 14 of 16 dyes. `textures/item/orange_dye.png` pixel (2,9) is `(123,55,27,255)`, the palette shade, and `brown_dye.png` pixel (11,8) is `(129,87,51,255)`.
- native: `textures/item/orange_dye.png` (2,9) is `(179,85,0,255)` and `brown_dye.png` (11,8) is `(185,117,78,255)`.
- difference: one wrong pixel each on the orange and brown dye icons.
- fix_kind: asset-override (`assets/vanillabackport/textures/item/orange_dye.png`, `brown_dye.png`)
- fix: set those two pixels to the native values, or replace the two VB layer PNGs with `native_<color>_dye.png` minus the palette pixels.
- config_gate: `enable_modern_dye_textures` / `enable_dye_palettes`

### VARIANTS-20  Extra or reordered random draws during spawn variant selection
- severity: cosmetic
- vb: `VariantUtils.java:102`: `return random.nextFloat() >= VanillaBackport.COMMON_CONFIG.mobVariantSpawnChance.get() ? Optional.empty() : PriorityProvider.pick(...)`. This is one extra `nextFloat()` on every cow, pig, chicken, cat, frog and wolf `finalizeSpawn`, including the empty cat, frog and wolf registries. `SheepMixin.java:27-30`: `DyeColor originalColor = Sheep.getRandomSheepColor(random); return SheepColorSpawnRules.getRandomSheepColor(originalColor, …, random);` rolls the legacy colour (1–2 draws) and then the new table. `MobMixin.java:42-55` runs the variant selection at `finalizeSpawn` `RETURN`, after `super`. `WolfMixin.java:120` rolls the sound variant before the variant pick.
- native: `VariantUtils.selectVariantToSpawn` has no chance roll. `Sheep.finalizeSpawn:307` makes one `SheepColorSpawnRules.getSheepColor` call. Cow, pig, chicken, frog and wolf pick the variant, then the sound variant, before `super.finalizeSpawn`. Cat picks after `super`.
- difference: the distributions are identical with the default `mob_variants_spawn_chance = 1.0`, but the level RNG stream is shifted, so seeded or replayed spawns (for example spawn-egg tests) do not reproduce native. With a chance below 1.0, VB spawns temperate animals that native would vary; this is a VB-only knob.
- fix_kind: mixin-vb (`VariantUtils.selectVariantToSpawn`, `SheepMixin.vb$updateColors`)
- fix: `@WrapOperation` the `RandomSource.nextFloat()` call in `selectVariantToSpawn` so it returns `0.0F` without consuming a draw when `mobVariantSpawnChance >= 1.0`. In `SheepMixin.vb$updateColors`, `@WrapMethod` to `return VanillaBackport.COMMON_CONFIG.hasFarmAnimalVariants.get() ? SheepColorSpawnRules.getRandomSheepColor(DyeColor.WHITE, this.level(), this.blockPosition(), random) : Sheep.getRandomSheepColor(random);`. Keeping the order before or after `super` is not worth fixing: AgeableMob's draws only affect group baby chance.
- config_gate: `mob_variants_spawn_chance`, `farm_animal_variants`

### VARIANTS-21  Swamp-hut cats from CatSpawner: native has bug MC-147659, NeoForge 1.21.1 fixes it
- severity: gameplay
- vb: VB adds nothing because the cat registry is empty. NeoForge 1.21.1 `CatSpawner.spawnCat` (`src-121 .../npc/CatSpawner.java`) runs `cat.moveTo(pos, 0.0F, 0.0F); // Fix MC-147659` before `cat.finalizeSpawn(...)`, and 1.21.1 `Cat.finalizeSpawn:350-353` sets ALL_BLACK and `setPersistenceRequired()` inside `cats_spawn_as_black`.
- native: `npc/CatSpawner.java:65-74`: `cat.finalizeSpawn(level, …, NATURAL, null); if (makePersistent) cat.setPersistenceRequired(); cat.snapTo(spawnPos, 0.0F, 0.0F);`. `finalizeSpawn` runs at (0,0,0), so `cat_variant/all_black.json`'s `structure` check (priority 1) fails and the cat gets a random moon-based variant. `Cat.finalizeSpawn:404-410` never sets persistence.
- difference: in native, cats that CatSpawner respawns at witch huts are random breeds (persistent through `makePersistent`). In VB on NeoForge they are always all-black. A cat spawned by egg inside a hut becomes persistent in VB and stays despawnable in native.
- fix_kind: engine (NeoForge patch)
- fix: **recommend not changing this**, because it reproduces a known vanilla bug. If strict parity is wanted, use a mixin-mc on `CatSpawner.spawnCat`: `@Redirect` `Cat.moveTo(BlockPos,FF)` to run after `finalizeSpawn` (move the call). In `Cat.finalizeSpawn`, `@Redirect` `setPersistenceRequired()` to a no-op, and have `CatSpawner.spawnInHut` set persistence.
- config_gate: none

### VARIANTS-22  Datapack variants for cow, pig, chicken and wolf-sound never reach clients on a dedicated server
- severity: data
- vb: Platform `BuiltInRegistryReloadListener.parse` → `this.registry.registerDataDriven(name, value)` runs only through `ResourceReloadManager.registerServer`, and no sync packet is sent. The entity sync sends only a `RegistryKey` (`ModSyncedEntityData.COW_VARIANTS` etc.). The client looks it up in its own `BuiltInCoreRegistry`, which holds code-driven entries only on a dedicated client, so `getVariantData()` is empty and the mob renders temperate.
- native: `cow_variant`, `pig_variant`, `chicken_variant`, `*_sound_variant` and the cat, frog and wolf variants are synchronized dynamic registries, so clients receive datapack entries.
- difference: any datapack that adds or changes these variants (including a parity data-override that uses this mechanism) works in singleplayer only. On dedicated servers, clients fall back to temperate.
- fix_kind: engine
- fix: when designing fixes, do not rely on data-driven VB variant entries for client-visible changes. Use code-driven `BuiltInCoreRegistry.register` from the add-on on both sides, or add a custom payload that sends `registry.entries()` on login and on `/reload` and calls `registerDataDriven` on the client.
- config_gate: none
