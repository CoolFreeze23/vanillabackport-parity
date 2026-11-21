# Technical deep audit — axis `ai` (mob behaviour)

## Summary
- 28 findings against the effective code (VB 1.2.2 + add-on): farm 2, gameplay 16, visual 2, cosmetic 8 (6 of the cosmetic ones are spawn/breed RNG-order differences in variant selection). Plus 27 engine-drift items (1 farm, 21 gameplay, 1 audio, 4 cosmetic), listed per group under `### Engine drift`.
- Biggest player-visible gaps: nautilus and zombie nautilus never follow a tempting player (AI-MNT-01, the IS_TEMPTED memory is never registered in 1.21.1, so the MAYHEM-15 fix has no effect); camel husks are tempted by cactus, not rabbit feet (AI-MNT-02); a golem opening a single chest also opens its neighbour, and with the add-on's recheck it corrupts that neighbour's opener count (AI-CPG-01, farm); golems close double chests one tick early (AI-CPG-02, farm); the add-on's SULFUR-04 knockback fix counts calls and scales the wrong hits (AI-SLF-01).
- Covered: creaking (+ heart spawn/teardown), happy ghast + ghastling + dried ghast lifecycle, copper golem brain + item transport + iron/copper golem flower goals, nautilus, zombie nautilus, ChargeAttack, camel husk, zombie horse, parched, jockeys, sulfur cube, every spear-wielding mob (goal + piglin brain, equipment rolls, pickup), cow/pig/chicken/sheep/frog/cat/wolf variant and sound-variant logic, AdultSensorAnyType, and villager/wandering-trader trades touching VB items. Every random call on these paths is quoted with its order in the group tables.
- First-round fixes re-verified and marked "match (add-on)" in the tables: GARDEN-02/03/04, GHAST-10..17, COPPER-01/02/03/22, MAYHEM-07/08/12..15/21/24/25, SULFUR-01..04 (SULFUR-04 is incomplete: AI-SLF-01), MAYHEM-09 (incomplete: AI-SPR-D2), VARIANTS-20 (partly: AI-VAR-01..06).
- Not checked: runtime behaviour (this is source review only). The `p2-*` rework that has not been merged was ignored as instructed. Sounds, visuals and loot were checked only where they belong to a behaviour or shift RNG order.

## Coverage (by group)
The detailed per-mechanic tables (one row per goal/behaviour/sensor/attribute group/random path) are at the top of each group section below.

| group | mechanics checked | result |
|---|---|---|
| Creaking | attributes, brain activities/sensors/memories, freeze (isLookingAtMe, checkCanMove order), attack cooldown/damage/reach, heart spawn search + random calls, heart distance/teardown timers, navigation/malus, damage redirect, despawn | AI-CRK-01; drift D1–D4; rest match |
| Happy ghast / ghastling / dried ghast | attributes, brain, follow-adult, tempting, panic, air stroll, move/look control, ridden travel, still-when-player-on-top, ageing/feeding, healing, leash, hydration timers | AI-GHS-01; drift D1, D1b, D2–D5; rest match |
| Copper golem + flower goals | brain, transport (all timings, radius, chest priority, item matching, stack size, interaction counts, visited/unreachable), weathering/wax/lightning, statue conversion, chest open/close, iron golem OfferFlowerGoal | AI-CPG-01..03; drift D1–D2; rest match |
| Nautilus / zombie nautilus / ChargeAttack | brains, sensors, memories, tempt, panic, charge timings/distances/knockback, taming roll, breeding, riding/dash, air, restriction | AI-MNT-01, 03, 04, 07, 08, 10, 11; drift D1–D5 |
| Camel husk | tempt sensor, food, riding/dash/sit, jockey | AI-MNT-02, 09 |
| Zombie horse | goals, taming, ageing, sun burn, natural jockey | AI-MNT-05, 06; drift D6 |
| Parched | goals vs Skeleton, bow interval, weakness immunity | match |
| Sulfur cube | attributes/archetypes, goals, jump/move control, targeting, attack, swallow/absorb, fuse, split, bucket, persistence, dispenser interactions | AI-SLF-01..06; drift D1–D3 |
| Spear mobs | SpearUseGoal priority/which mobs, approach/retreat, jab/charge timings, reach, damage/knockback, piglin brain, spawn equipment rolls + order, pickup rules, durability | match; drift AI-SPR-D1, D2 |
| Variant farm animals + sound variants | spawn variant pick (order vs super.finalizeSpawn), breeding inheritance + RNG, frog/tadpole, cat witch hut, wolf sound pick, AdultSensorAnyType, egg-lay | AI-VAR-01..06; drift D2–D4 |
| Trades | villager/wandering-trader trades with VB items, pool sampling | drift AI-VAR-D1; values match |

## Finding index
| id | severity | title |
|---|---|---|
| AI-CRK-01 | gameplay | Illagers flee creakings with the wrong walk/sprint speed modifiers |
| AI-CRK-D1 | gameplay (drift) | Explosions and wind charges knock a frozen creaking around (native: frozen creakings ignore explosion knockback) |
| AI-CRK-D2 | gameplay (drift) | Hearts still spawn creakings when native's `spawn_monsters` rule would block them |
| AI-CRK-D3 | gameplay (drift) | Pathfinder node budget ignores the creaking's random follow-range bonus |
| AI-CRK-D4 | cosmetic (drift) | Sensor start delay draws from a different random source |
| AI-GHS-01 | gameplay | Wander-target home check uses the floored block corner instead of the exact position against the home block centre |
| AI-GHS-D1 | gameplay (drift) | Ridden happy ghast strafes about 39% faster on diagonals (1.21.1 player input is not square-normalized) |
| AI-GHS-D1b | gameplay (drift) | Ghastling horizontal flight is 2% slower (input decay runs after the move control in 1.21.1) |
| AI-GHS-D2 | gameplay (drift) | Goals, move control and look control still run while a player steers the ghast |
| AI-GHS-D3 | gameplay (drift) | Home-bias offset in random positions is integer `nextInt(r/2)` in 1.21.1 vs continuous `nextDouble()*r/2` (ghastling stroll and panic) |
| AI-GHS-D4 | gameplay (drift) | Panic start does not stop navigation in 1.21.1 |
| AI-GHS-D5 | cosmetic (drift) | Sensor start delays drawn from the entity RNG in native, from a static RNG in 1.21.1 |
| AI-CPG-01 | farm | A golem opening a single chest also opens the chest beside it (and corrupts its opener count with the add-on recheck) |
| AI-CPG-02 | farm | Double chests are closed by the golem one tick early (native leaves the close to the 5-tick recheck) |
| AI-CPG-03 | cosmetic | The iron-golem flower roll runs last in the goal list instead of at its native slot |
| AI-CPG-D1 | cosmetic (drift) | Brain sensors draw their start delay from the golem's own random (2 extra draws before the spawn cooldown) |
| AI-CPG-D2 | cosmetic (drift) | Restricted (leashed) golem strolls use int offsets of range/2 in 1.21.1 (always 0 for the golem's range 2) vs native double offsets |
| AI-MNT-01 | gameplay | Nautilus and zombie nautilus never follow a tempting player (IS_TEMPTED memory not registered) |
| AI-MNT-02 | gameplay | Camel husks are tempted by cactus instead of rabbit feet |
| AI-MNT-03 | gameplay | Interacting (lead, name tag, spawn egg, VB lead-chain) doesn't make these mobs persistent |
| AI-MNT-04 | gameplay | Spear hits (add-on's `minecraft:spear` damage) no longer cause panic |
| AI-MNT-05 | gameplay | Natural zombie-horse jockey: the zombie is rolled after the horse instead of before |
| AI-MNT-06 | gameplay | Zombie horse sun-burn roll runs after the horse's own aiStep rolls |
| AI-MNT-07 | gameplay | Dispensers can't put nautilus armour on a tamed nautilus |
| AI-MNT-08 | visual | Nautilus water travel ignores local control and never updates the walk animation |
| AI-MNT-09 | gameplay | Camel husks and zombie nautiluses can still be babies (Age NBT) |
| AI-MNT-10 | visual | Drowned riding a zombie nautilus don't align body rotation |
| AI-MNT-11 | cosmetic | Nautilus knockback resistance is the double 0.3, not the float 0.3F |
| AI-MNT-D1 | audio (drift) | Feeding to breed or grow up plays no eating sound |
| AI-MNT-D2 | gameplay (drift) | AnimalPanic doesn't stop the current path when it starts |
| AI-MNT-D3 | gameplay (drift) | Brain sensor ranges fixed at 16 instead of the follow range |
| AI-MNT-D4 | gameplay (drift) | Sensor start delay doesn't draw from the entity random |
| AI-MNT-D5 | gameplay (drift) | Water navigation waypoint vertical tolerance 1.0 instead of 0.5 |
| AI-MNT-D6 | gameplay (drift) | Leashed mobs aren't exempt from despawning |
| AI-SLF-01 | gameplay | SULFUR-04 fix counts knockback calls, so spear jabs, sweeps, i-frame hits and knockback-only hits get the wrong scale |
| AI-SLF-02 | gameplay | Primed (invulnerable) cubes can't be hit by spears |
| AI-SLF-03 | gameplay | Bucketing a cube always makes it (and its future split babies) persistent |
| AI-SLF-04 | gameplay | Dispensers put armor, elytra and carved pumpkins on sulfur cubes; a chestplate becomes the "swallowed block" |
| AI-SLF-05 | gameplay | Dispenser shears drop the block through NeoForge's sheep rule (y+1, random push, 5 extra entity RNG draws) |
| AI-SLF-06 | gameplay | Igniting with a fire charge in creative consumes it |
| AI-SLF-D1 | gameplay (drift) | Dispenser shears cut a leashed cube's leash before its block |
| AI-SLF-D2 | cosmetic (drift) | Trig lookups take float input in 1.21.1, double in 26.2 |
| AI-SLF-D3 | gameplay (drift) | No age lock (golden dandelion): feeding and growth are never gated by `canAgeUp()` |
| AI-SPR-D1 | gameplay (drift) | Spear pickups are decided by 1.21.1 item-class rules, not by attack damage |
| AI-SPR-D2 | gameplay (drift) | Adult piglin: golden spear vs crossbow uses 1.21.1's crossbow rule (MAYHEM-09 fix incomplete) |
| AI-VAR-01 | cosmetic | Cow/pig/chicken/wolf pick variant and sound variant after `super.finalizeSpawn` instead of before |
| AI-VAR-02 | cosmetic | Mooshrooms run the cow-variant pick (one extra level-RNG draw and a stray `variant` tag) |
| AI-VAR-03 | cosmetic | Wolf spawn: no variant draw, sound pick after `super`, sound list in a different order |
| AI-VAR-04 | cosmetic | Frog spawn and tadpole conversion: native makes one `nextInt(1)` variant draw before `initMemories` |
| AI-VAR-05 | cosmetic | Cat spawned inside a witch hut (spawn egg, `/summon`, hut piece): bound `nextInt(10|11)` instead of `nextInt(1)`, and the breed list is in a different order |
| AI-VAR-06 | cosmetic | Breeding: offspring variant drawn from the child's RNG, plus extra 1.21.1 draws (cow, pig, chicken, cat, wolf) |
| AI-VAR-D1 | farm (drift) | Wandering trader: 1.21.1 picks 5 generic + 1 rare from the trader's RNG; native picks 2 buying + 2 uncommon + 5 common from per-world random sequences |
| AI-VAR-D2 | gameplay (drift) | Golden dandelion age lock (baby growth pause) is absent |
| AI-VAR-D3 | gameplay (drift) | Wolves cannot be fed or bred with fish or rabbit stew |
| AI-VAR-D4 | cosmetic (drift) | Brain sensor start delay drawn from a global RNG instead of the mob's (frog, tadpole, happy ghast's AdultSensorAnyType) |


## Creaking

Effective behaviour = VB 1.2.2 + add-on garden mixins (CreakingAiMixin, CreakingMixin, PlayerSensorMixin, CreakingHeartBlockEntityMixin, CreakingHeartBlockMixin, BoatMixin, GardenCreaking, GardenResin, CreakingSensorData). First-round GARDEN-01..10 were re-checked against native; all fixes that touch creaking AI are correct and are not re-reported.

Paths: VB = `src-vb/com/blackgear/vanillabackport/...`, native = `src-mc/net/minecraft/...`, add-on = `src/main/java/com/coolfreeze23/vbparity/garden/...`.

| mechanic | VB/add-on location | native location | result |
|---|---|---|---|
| Attributes: max health 1, attack damage 3, follow range 32, step height 1.0625 | Creaking.java:120-127 | creaking/Creaking.java:128-135 | match |
| Attribute: movement speed `0.4` (double) vs `0.4F` | Creaking.java:123 | Creaking.java:131 | match in effect: MoveControl casts `(float)(speedMod * attr)`, and 0.3/1.0 × either value rounds to the same float |
| Follow-range random spawn bonus (`random.triangle(0, 0.11485)` then `nextFloat() < 0.05F` left-handed) | 1.21.1 Mob.java:1192-1204, via EntityType.create | Mob.java:1098-1111 | match (same level-random order: yaw `nextFloat()*360`, triangle, nextFloat) |
| Entity dimensions 0.9×2.7, eye 2.3, tracking 8 | ModEntityTypes.java:23-25 | EntityTypes.java:310-313 | match (`notInPeaceful` = 1.21.1 Monster peaceful despawn) |
| Brain sensors: NEAREST_LIVING_ENTITIES + NEAREST_PLAYERS, scan 20 | CreakingAi.java:28-30 | Creaking.java:61-63, SensorType.java:16-19 | match |
| Sensor start delay random | 1.21.1 Sensor.java:32 static `RANDOM.nextInt(scanRate)` | Brain.java:86 `randomlyDelayStart(body.getRandom())` | AI-CRK-D4 |
| Player sensor range / filters (32 follow range, NO_SPECTATORS, sorted, visible = non-combat, attackable = combat, ATTACK_TARGET ignores invisibility) | addon PlayerSensorMixin + GardenCreaking.tickPlayerSensor:30-61 | PlayerSensor.java:28-46, Sensor.java:52-76 | match (GARDEN-02 fix verified) |
| NEAREST_VISIBLE_ATTACKABLE_PLAYERS plural memory | addon CreakingSensorData field | PlayerSensor.java:40 | match in effect (field set every sensor tick, absent = false as native `orElse(false)`) |
| Nearest living entity sensor 16 vs 32 | 1.21.1 NearestLivingEntitySensor.java:17 (16) | NearestLivingEntitySensor.java:18 (follow range) | match in effect: only read by 8-block look target and melee-range `contains` checks |
| Memory modules list | CreakingAi.java:31-42 | derived from behaviours | match |
| CORE: Swim(0.8F) gated by canMove, LookAtTargetSink(45,90), MoveToTargetSink | CreakingAi.java:44-50 | CreakingAi.java:30-36 | match (Swim: `nextFloat() < 0.8F` per tick, both) |
| IDLE: StartAttacking(isActive, NEAREST_VISIBLE_ATTACKABLE_PLAYER) | CreakingAi.java:57 | CreakingAi.java:43 | match |
| IDLE: SetEntityLookTargetSometimes(8, UniformInt 30..60) | CreakingAi.java:58 | CreakingAi.java:44 | match (`interval.sample(level.random)` on each reset, both) |
| IDLE: RunOne{RandomStroll 0.3 w2, SetWalkTargetFromLookTarget 0.3/3 w2, DoNothing 30..60 w1} | CreakingAi.java:59-63 | CreakingAi.java:45-49 | match (ShufflingList own `RandomSource.create()` nextFloat per entry; DoNothing `level.random.nextInt(31)`; LandRandomPos 10/7 nextInt×3) |
| FIGHT: priorities 10/11/12, condition ATTACK_TARGET present, no memory erase on stop | addon CreakingAiMixin:27-39 | CreakingAi.java:54-65 | match (GARDEN-03 fix verified) |
| FIGHT: SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0) | CreakingAiMixin:32 | CreakingAi.java:59; behavior/SetWalkTargetFromAttackTargetIfTargetOutOfReach.java:27-38 | match |
| FIGHT: melee attack (canMove, cooldown 40, reach `sqrt(2.04F)-0.6F` box, visible-entity check, projectile/non-melee weapon skip) | CreakingMeleeAttack.java:117-148 | MeleeAttack.java:16-47, Mob.java:1363-1398 | match |
| FIGHT: StopAttackingIfTargetInvalid(not in visible+attackable list, tired 200 ticks) | addon GardenCreaking:73-93 | CreakingAi.java:61-70, StopAttackingIfTargetInvalid.java:25-54 | match (GARDEN-04 fix verified, same condition order) |
| updateActivity (frozen → IDLE, else FIGHT/IDLE first valid) | CreakingAi.java:95-101 | CreakingAi.java:76-82 | match |
| checkCanMove order (empty → deactivate; canAttack && !allied; pumpkin only when active; isLookingAtMe cone 0.5, VISUAL clip, 3 gaze heights; active → freeze; < 144 sq → activate) | Creaking.java:464-499 | Creaking.java:457-494 | match (carved pumpkin vs `#gaze_disguise_equipment` = `[carved_pumpkin]`) |
| isLookingAtMe / hasLineOfSight math (128 cap, MISS) | WorldUtilities.java:129-166 | LivingEntity.java:1768-1786, 3343-3355 | match |
| Freeze transition (stopInPlace, game event, freeze/unfreeze sound via makeSound) | Creaking.java:224-236 + addon CreakingMixin:48-54 | Creaking.java:224-238, Mob.java:453-460 | match |
| Move/Look/Jump/BodyRotation/PathNavigation tick gated by canMove | Creaking.java:527-593 | Creaking.java:523-599 | match |
| push / isPushable / knockback gated by canMove | Creaking.java:192-200, 458-462 | Creaking.java:188-198, 450-455 | match for entity push/attack knockback; explosion knockback: AI-CRK-D1 |
| Navigation: GroundPathNavigation, canFloat, HomeNodeEvaluator (BLOCKED beyond 1024 sq unless closer than mob), canPassDoors | Creaking.java:82-83, 577-605 | Creaking.java:91-92, 580-619 | match |
| Max visited nodes | 1.21.1 PathNavigation.java:65 | PathNavigation.java:63-83, Mob.java:1468-1472 | AI-CRK-D3 |
| Heart-bound path malus: DAMAGE_OTHER/POWDER_SNOW/LAVA 8, DAMAGE_FIRE/DANGER_FIRE 0 | Creaking.java:87-94 | Creaking.java:96-103 (DAMAGING/FIRE/FIRE_IN_NEIGHBOR) | match (1.21.1 names map 1:1 to 26.2 PathType; default malus values equal; BIG_MOBS_CLOSE_TO_DANGER needs width > 1, creaking entityWidth = 1) |
| getWalkTargetValue 0 (spawn rule + stroll weight) | Creaking.java:523-525 | Creaking.java:518-521 | match |
| Damage: heart-bound redirect (bypass tag, invulnerable, 8-tick window, direct Living/Projectile/player, event 66, ENTITY_ACTION, creakingHurt only with player, sway sound) | Creaking.java:143-169 | Creaking.java:152-181 | match |
| blameSourceForDamage | Creaking.java:171-190 | Creaking.java:183-186, LivingEntity.java:1362-1386 | match in effect (native also blames a player for `#no_anger` damage, which only holds `mob_attack_no_aggro`; creaking is not in `#no_anger_from_wind_charge`) |
| isInvulnerableTo | 1.21.1 LivingEntity.java:3759-3769 | LivingEntity.java:4014-4016 | match |
| Death: heart-bound tear-down after 45 ticks, removeProtector(source) path, tearDown particles/sound | Creaking.java:264-273, 286-329 + addon | Creaking.java:263-328 | match (GARDEN-08/10 fixes verified) |
| doHurtTarget (anim 15, event 4, attack sound on hit only) | Creaking.java:133-141 + addon CreakingMixin:60-66 | Creaking.java:141-150, 419-422 | match (GARDEN-09 verified) |
| playerIsStuckInYou (counter > 4) | Creaking.java:369-384 | Creaking.java:358-376 | match |
| Passengers / portals / fire immunity | Creaking.java:343-363 + addon CreakingMixin:69-75 | Creaking.java:343-351 | match (GARDEN-07 verified) |
| Boats | addon BoatMixin | AbstractBoat.java:283-288 | match (GARDEN-06) |
| Persistence / despawn | Monster defaults | Monster defaults | match (heart day teardown skips `isPersistenceRequired`) |
| NBT home_pos → setTransient | Creaking.java:386-398 | Creaking.java:378-388 | match |
| Heart BE: tick order (output signal → emitter → ticker `nextInt(5)+20`) | CreakingHeartBlockEntity.java:66-133 | CreakingHeartBlockEntity.java:73-138 | match |
| Heart BE: emitter 100/50/10, sound progress/volume, particle randoms (nextDouble×6 + nextInt(40)+10 per particle) | CreakingHeartBlockEntity.java:75-92, 280-303 | :82-99, 289-310 | match |
| Heart BE: spawn conditions (AWAKE, mob-spawn rule, not peaceful, nearest non-spectator player 32) | CreakingHeartBlockEntity.java:104-117 | :111-124 | AI-CRK-D2 (spawn_monsters rule) |
| Heart spawn search: 5 attempts, xz ±16 (`randomBetweenInclusive` x then z), y +8 down to −8, ON_TOP_OF_COLLIDER_NO_LEAVES, world border, collision AABB, checkSpawnRules + checkSpawnObstruction, playAmbientSound | WorldUtilities.java:322-381 | util/SpawnUtil.java:19-101 | match |
| Spawn side effects (ENTITY_PLACE, event 60, setTransient, spawn sounds) | CreakingHeartBlockEntity.java:197-215, 112-114 + addon CreakingHeartBlockEntityMixin:27-33 | :203-217, 118-120 | match |
| Heart distance / teardown: day & !persistent, > 34 blocks (sqrt to bottom centre), player stuck | CreakingHeartBlockEntity.java:118-129 | :125-135 | match |
| Heart: protector lookup, UUID grace 30 ticks | CreakingHeartBlockEntity.java:165-194 | :171-200 | match |
| Heart: creakingHurt (20 particles, `nextIntBetweenInclusive(2,3)` clumps, emitter 100) | CreakingHeartBlockEntity.java:225-243 | :228-250 | match |
| Heart: resin spread traversal (depth 2, 64, shuffles node-then-neighbour, STOP after one face) | addon GardenResin.java:32-79 | :252-287, BlockPos.java:481-520 | match (GARDEN-01 verified) |
| Heart removal → tearDown | addon CreakingHeartBlockMixin:20-25 | :312-315 | match (GARDEN-05) |
| Heart block: ticker only when not UPROOTED, updateShape → 1-tick scheduled tick, hasRequiredLogs (axis), idle sound `nextInt(16)` | CreakingHeartBlock.java:67-136 | CreakingHeartBlock.java:58-137 | match (`#creaking_heart_holders` = `#pale_oak_logs`) |
| Heart block: explosion / player break remove protector, XP `nextIntBetweenInclusive(20,24)` | CreakingHeartBlock.java:159-185 | :160-190 | match |
| Heart comparator 15 − floor(clamp(d,0,32)/32·15) | CreakingHeartBlockEntity.java:329-337 | :341-349 | match |
| Illagers avoiding creakings | MobIntegrations.java:71-74 | Vindicator.java:64, Pillager.java:72, Evoker.java:61, Illusioner.java:68 | AI-CRK-01 |
| AvoidEntityGoal body (canUse / getPosAway 16,7 / sprint < 49 sq) | 1.21.1 AvoidEntityGoal.java:64-118 | AvoidEntityGoal.java:71-118 | match |

### AI-CRK-01  Illagers flee creakings with the wrong walk/sprint speed modifiers
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/integrations/MobIntegrations.java:71-74 (registerGoals)
  ```
  event.registerGoal(EntityType.VINDICATOR, 1, mob -> new AvoidEntityGoal((PathfinderMob)mob, Creaking.class, 8.0F, 0.6, 1.2));
  event.registerGoal(EntityType.PILLAGER, 1, mob -> new AvoidEntityGoal((PathfinderMob)mob, Creaking.class, 8.0F, 0.6, 1.2));
  event.registerGoal(EntityType.ILLUSIONER, 3, mob -> new AvoidEntityGoal((PathfinderMob)mob, Creaking.class, 8.0F, 0.6, 1.2));
  event.registerGoal(EntityType.EVOKER, 3, mob -> new AvoidEntityGoal((PathfinderMob)mob, Creaking.class, 8.0F, 0.6, 1.2));
  ```
- native: src-mc/net/minecraft/world/entity/monster/illager/Vindicator.java:64 `new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 1.0, 1.2)`; Pillager.java:72 `new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 1.0, 1.2)`; Illusioner.java:68 `new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 1.0, 1.2)`; Evoker.java:61 `new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 0.6, 1.0)`
- difference: AvoidEntityGoal uses walkSpeedModifier while the creaking is 7 or more blocks away and sprintSpeedModifier when it is closer (AvoidEntityGoal.java:113-117). Vindicator, pillager and illusioner walk away at 0.6 instead of 1.0 (40% slower in the 7–8 block band and on the start of every flee path, since `start()` uses the walk modifier). The evoker sprints at 1.2 instead of 1.0 (20% faster inside 7 blocks). Priorities and goal order relative to the other goals match native (VB's priorities 1/1/3/3 sit where native's renumbered 1/1/3/3 sit; the Vindicator break-door goal it ties with has no flags).
- fix_kind: mixin-mc (net.minecraft.world.entity.ai.goal.AvoidEntityGoal.<init>)
- fix: `@Mixin(AvoidEntityGoal.class)`, `@ModifyVariable(method="<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;FDD)V", at=@At("HEAD"), argsOnly=true, ordinal=0)` on the walk `double` and `ordinal=1` on the sprint `double`, each also capturing `PathfinderMob mob` and `Class<?> avoidClass`. When `avoidClass == com.blackgear...creaking.Creaking.class`: for `mob.getType()` VINDICATOR, PILLAGER or ILLUSIONER return walk `1.0`; for EVOKER return sprint `1.0`; otherwise return the argument unchanged. Only VB constructs this goal with `Creaking.class`, so no vanilla goal changes.
- config_gate: none (VB registers these goals unconditionally)

### Engine drift (Creaking)

### AI-CRK-D1  Explosions and wind charges knock a frozen creaking around (native: frozen creakings ignore explosion knockback)
- severity: gameplay
- vb: src-121/net/minecraft/world/level/Explosion.java:304 (explode) `entity.setDeltaMovement(entity.getDeltaMovement().add(vec31));`. VB's guard is only on `push`: src-vb/.../creaking/Creaking.java:196-200 `public void push(double x, double y, double z) { if (this.canMove()) { super.push(x, y, z); } }`
- native: src-mc/net/minecraft/world/level/ServerExplosion.java:197-199 `Vec3 knockback = direction.scale(knockbackPower); entity.push(knockback);`, which reaches Entity.java:1918-1920 `this.push(impulse.x, impulse.y, impulse.z)` and so Creaking.java:193-198 `if (this.canMove()) { super.push(xa, ya, za); }`
- difference: natively a frozen (watched) creaking gets 0 knockback from TNT, creepers, beds/anchors and wind charges; a moving one gets the normal impulse. In 1.21.1 the explosion bypasses `push`, so a frozen creaking is flung by the full `(1 - dist) * seen * multiplier * (1 - knockbackResistance)` impulse. Wind-charge "launch the frozen creaking" tricks and TNT-based creaking traps behave differently.
- fix_kind: mixin-mc (net.minecraft.world.level.Explosion.explode), scoped to the VB creaking
- fix: `@WrapOperation(method="explode", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))`: `if (entity instanceof com.blackgear...creaking.Creaking c) { c.push(vec.x, vec.y, vec.z); } else { original.call(entity, vec); }`. Calling the creaking's own `push` reproduces native: no change while `canMove()` is false, and `getDeltaMovement().add(vec)` plus `hasImpulse` otherwise.
- config_gate: `The Garden Awakens.creaking`

### AI-CRK-D2  Hearts still spawn creakings when native's `spawn_monsters` rule would block them
- severity: gameplay
- vb: src-vb/.../block_entities/CreakingHeartBlockEntity.java:105-107 (serverTick) `updatedState.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && level.getDifficulty() != Difficulty.PEACEFUL`
- native: src-mc/net/minecraft/world/level/block/entity/CreakingHeartBlockEntity.java:113 `if (serverLevel.isSpawningMonsters() && serverLevel.getLevelData().getDifficulty() != Difficulty.PEACEFUL)`; server/level/ServerLevel.java:1791-1793 `return this.getGameRules().get(GameRules.SPAWN_MOBS) && this.getGameRules().get(GameRules.SPAWN_MONSTERS);`
- difference: VB's `doMobSpawning` is native's `spawn_mobs`, so that half matches. Native also needs the 26.x `spawn_monsters` rule, which 1.21.1 does not have. With default rules the result is identical. A world that turns monsters off with `spawn_monsters=false` but keeps `spawn_mobs` (for example a peaceful-style resin or animal setup) would get no creakings natively, but still gets them in 1.21.1.
- fix_kind: engine
- fix: none that is faithful, because the rule does not exist in 1.21.1. The closest option is a mod gamerule `vbparity:spawn_monsters` (default true) ANDed into this condition with `@ModifyExpressionValue` on the `GameRules.getBoolean(RULE_DOMOBSPAWNING)` call in `CreakingHeartBlockEntity.serverTick`. That only helps if NaturalSpawner honours the same rule.
- config_gate: `The Garden Awakens.creaking`

### AI-CRK-D3  Pathfinder node budget ignores the creaking's random follow-range bonus
- severity: gameplay
- vb: src-121/net/minecraft/world/entity/ai/navigation/PathNavigation.java:65 (constructor) `int i = Mth.floor(mob.getAttributeValue(Attributes.FOLLOW_RANGE) * 16.0); this.pathFinder = this.createPathFinder(i);`. It runs in the Mob constructor, before finalizeSpawn, and is never updated (1.21.1 PathFinder.java:23 `private final int maxVisitedNodes`).
- native: src-mc/net/minecraft/world/entity/Mob.java:1468-1472 (onAttributeUpdated) `if (attribute.is(Attributes.FOLLOW_RANGE) || attribute.is(Attributes.TEMPT_RANGE)) { this.getNavigation().updatePathfinderMaxVisitedNodes(); }`; PathNavigation.java:71-83 `int maxVisitedNodes = Mth.floor(this.getMaxPathLength() * 16.0F);` with `getMaxPathLength() = Math.max((float)FOLLOW_RANGE value, this.requiredPathLength /*16*/)`
- difference: the heart-spawned creaking gets `triangle(0, 0.11485)` × base as a follow-range bonus in finalizeSpawn (Mob.java:1101-1107, same in 1.21.1). Native then re-budgets the pathfinder to floor(FR·16), anywhere from 453 to 570 nodes. 1.21.1 keeps 512 for every creaking. Path search length (`createPath(..., followRange)`) does use the bonus in both. Long or complex chases through pale garden trunks can fail or succeed differently per creaking.
- fix_kind: mixin-mc (net.minecraft.world.entity.LivingEntity.onAttributeUpdated), scoped to the VB creaking
- fix: `@Inject(method="onAttributeUpdated", at=@At("TAIL"))` on LivingEntity: `if ((Object)this instanceof com.blackgear...creaking.Creaking c && attribute.is(Attributes.FOLLOW_RANGE)) { PathNavigation nav = c.getNavigation(); ((PathNavigationAccessor)nav).vbparity$setPathFinder(((PathNavigationInvoker)nav).vbparity$createPathFinder(Mth.floor(Math.max((float)c.getAttributeValue(Attributes.FOLLOW_RANGE), 16.0F) * 16.0F))); }`. This needs an `@Accessor @Mutable` for `PathNavigation.pathFinder` and an `@Invoker` for `createPathFinder(int)`. `CreakingPathNavigation.createPathFinder` rebuilds the HomeNodeEvaluator (with doors on) each time, so the call is safe. It also has to run once after NBT load, because 1.21.1 applies saved attribute modifiers without `onAttributeUpdated`. Inject at the TAIL of `Creaking.readAdditionalSaveData` with the same body.
- config_gate: `The Garden Awakens.creaking`

### AI-CRK-D4  Sensor start delay draws from a different random source
- severity: cosmetic
- vb: src-121/net/minecraft/world/entity/ai/sensing/Sensor.java:11,32 `private static final RandomSource RANDOM = RandomSource.createThreadSafe();` … `this.timeToTick = (long)RANDOM.nextInt(scanRate);`
- native: src-mc/net/minecraft/world/entity/ai/Brain.java:84-86 `Sensor<? super E> newSensor = sensorType.create(); newSensor.randomlyDelayStart(randomSource);` with `randomSource = body.getRandom()` (Brain.java:496-498); Sensor.java:40-42 `this.timeToTick = randomSource.nextInt(this.scanRate);`
- difference: native draws two `nextInt(20)` from the creaking's own entity random each time a brain is built (construction and NBT load), so later entity-random draws (voice pitch, Swim chance) shift by two. 1.21.1 uses a global static random. Both are unseeded, and the delay distribution (uniform 0–19) is the same, so nothing observable or farm-relevant changes. This is recorded only for RNG-order completeness.
- fix_kind: engine
- fix: none needed.
- config_gate: none

counts: farm=0 gameplay=1 visual=0 audio=0 data=0 cosmetic=0 drift=4


## Happy ghast + ghastling (+ dried ghast lifecycle)  — axis `ai`, prefix GHS

Effective code = VB 1.2.2 + add-on (`vbparity/ghast/**`). First-round items GHAST-10/11/12/13/14/15/16/17 were re-verified against native and are fixed correctly. They are marked "match (add-on)" below and not re-reported.

Paths used below:
- VB: `src-vb/com/blackgear/vanillabackport/...`, shortened to `vb:…/happy_ghast/X.java`.
- Native: `src-mc/net/minecraft/...`.
- 1.21.1: `src-121/net/minecraft/...`.
- Add-on: `src/main/java/com/coolfreeze23/vbparity/ghast/...`.

### Coverage

| mechanic | VB / add-on location | native location | result |
|---|---|---|---|
| Attributes: MAX_HEALTH 20, FLYING_SPEED 0.05, MOVEMENT_SPEED 0.05, FOLLOW_RANGE 16 | vb HappyGhast.java:145-151 | animal/happyghast/HappyGhast.java:152-160 | match |
| TEMPT_RANGE 16 (adult TemptGoal) | vb HappyGhast.java:110 `.setTemptRange(16)`; vb TemptGoal.java:98 | HappyGhast.java:155; goal/TemptGoal.java:58 | match |
| TEMPT_RANGE 16 (ghastling sensor) | add-on TemptingSensorMixin (16, no extra closerThan 10) | sensing/TemptingSensor.java:39-47 | match (add-on, GHAST-12) |
| CAMERA_DISTANCE 8 | client (first-round GHAST-18) | HappyGhast.java:159 | out of axis |
| sanitizeScale min(scale,1) | vb :153-155 | :162-165 | match |
| Goals: 3 FloatGoal (gated on still timeout), 4 TemptGoal.ForNonPathfinders(1.0, pred, false, 7.0), 5 RandomFloatAroundGoal(16) | vb :102-113, :611-622 | :102-119, :632-642 | match |
| Tempt predicate: unharnessed adult uses tempt items, otherwise food | vb :108 `!isHarnessed() && !isBaby()` (CHEST slot + harness tag) | :111-113 `!isWearingBodyArmor()` (BODY slot) | match (only a harness can be in that slot) |
| TemptGoal canUse/canContinueToUse/stop (calmDown reducedTickDelay(100)=50), tick stop distance 7² | vb TemptGoal.java:93-163 | goal/TemptGoal.java:52-127 | match |
| ForNonPathfinders.navigateTowards random `nextDouble()` (1 draw per goal tick, scale toward player eyes) | vb TemptGoal.java:186 | goal/TemptGoal.java:145 | match |
| RandomFloatAroundGoal canUse (dd<1 or dd>3600), canContinueToUse false | vb :755-770 | monster/Ghast.java:403-420 | match |
| RandomFloatAroundGoal draws: per attempt `nextFloat()` ×3 (x,y,z) ×(2f-1)×16, up to 64 attempts, then 3 more if every attempt was null | vb :783-792, :823-828 | Ghast.java:434-443, :476-481 | match (order, count and bounds) |
| RandomFloatAroundGoal restriction test | vb :833 `isWithinRestriction(BlockPos.containing(target))` | Ghast.java:486 `isWithinHome(target)` (Vec3, distance to block center) | **AI-GHS-01** |
| isGoodTarget (air, any non-air within 1..15 on 6 axes) and heightmap clamp | vb :794-821 | Ghast.java:445-474 | match |
| GhastMoveControl: stop when still; floatDuration `nextInt(5)+2`; impulse normalize×FLYING_SPEED×5/3 | vb :637-655 | Ghast.java:240-260 | match |
| GhastMoveControl canReach / blockTraversalPossible / forEachBlockIntersectedBetween / collidedWithShapeMovingFrom / collidedWithFluid / happy_ghast_avoids | vb :657-713; vb core/util/Utilities.java:77-121, :383-590 | Ghast.java:262-320; level/BlockGetter.java:173-290; phys/AABB.java:403-419; Entity.java:1411-1420 | match (VB's ports are line-for-line the same) |
| stopInPlace (nav stop, xxa/yya/speed 0, delta 0, reset angular leash momentum) | vb WorldUtilities.java:120-127 | Mob.java:453-460 | match |
| Look control: still → snap yaw to 90°; lookAt cooldown → face target; otherwise face movement | vb :721-742; Utilities.MthUtils.wrapDegrees90 | HappyGhast.java:651-666; Mth.java:242-253; Ghast.java:187-202 | match for an unridden ghast; the ridden case is in **AI-GHS-D2** |
| Body rotation control while a vehicle | vb :602-608 | :622-629 | match |
| Travel (unridden): water ×0.8, lava ×0.5, air ×0.91, speed = flying×5/3 for all three | vb :170-193 | HappyGhast.java:177-180 → LivingEntity.java:2455-2470 | match. VB's extra still-timeout zeroing is equivalent, because the move control already zeroes delta and input before travel |
| Ridden input: strafe = xxa, forward/up from pitch, back ×-0.5, jump +0.5, ×3.9F×FLYING_SPEED (×config 1.0) | vb :356-378 | :353-374 | code matches. The value of `xxa` the engine feeds in differs: **AI-GHS-D1** |
| Ridden rotation: yaw += wrap(diff)×0.08, pitch = controller×0.5 | vb :380-392 | :376-390 | match |
| Controlling passenger: harnessed, not still, first passenger is a player | vb :350-354 | :346-350 | match |
| AI while a player controls the ghast | 1.21.1 runs serverAiStep | 26.2 does not (isEffectiveAi false) | **AI-GHS-D2** |
| Still timeout: grace 60 ticks, max 10, scan box (bb ±1 xz, maxY-1e-5 .. maxY+h/2), add/remove passenger | vb :88-96, :318-343, :420-440, :547-572 | :88-96, :310-337, :418-439, :546-571 | match |
| requiresPrecisePosition = still (aiStep) | vb :442-445 | :441-445 | match |
| canBeCollidedWith | add-on HappyGhastMixin + EntityCollisionMixin | :578-589 | match (add-on, GHAST-09) |
| Restriction: unharnessed adult 64, otherwise 32, re-centre beyond r+16; cleared on last dismount | vb :451-462, :340 | :451-462, :334 | match |
| Healing: 1 HP every 20 ticks in clouds or precipitation, otherwise every 600 | vb :464-475 + add-on EnvironmentUtilsMixin / HappyGhastMixin natural() redirect | :464-471; Entity.java:1635-1647; Level.java:960-971 | match (add-on, GHAST-17) |
| Brain sensors (order): NEAREST_LIVING_ENTITIES, HURT_BY, tempting(food tag), NEAREST_ADULT_ANY_TYPE (followable_friendly_mobs, not baby), NEAREST_PLAYERS | vb HappyGhastAi.java:31-37; AdultSensorAnyType.java:22-26; ModSensorTypes.java:14-16 | HappyGhast.java:68-73; sensing/AdultSensorAnyType.java; TemptingSensor.forAnimal | match |
| Sensor ranges: nearest-living box and player range 16 (native: FOLLOW_RANGE = 16) | 1.21.1 sensing/NearestLivingEntitySensor, PlayerSensor (hard 16) | sensing/NearestLivingEntitySensor, PlayerSensor, Sensor.updateTargetingConditionRanges | match for this mob (follow range 16) |
| Sensor initial random delay (RNG source) | 1.21.1 sensing/Sensor.java (static RANDOM) | ai/Brain.java:86, :498 (entity RNG) | **AI-GHS-D5** |
| Brain tick order (forget, sensors, start, tick), scan rate 20 | 1.21.1 Brain.java:402-407 | Brain.java:395-400 | match |
| Activities: CORE p0 [Swim 0.8, AnimalPanic 2.0, LookAtTargetSink 45/90, MoveToTargetSink, CountDownCooldownTicks(TEMPTATION_COOLDOWN)] | vb HappyGhastAi.java:68-80 | HappyGhastAi.java:34-46 | match |
| AnimalPanic position getter (fly height 0, AirAndWater 5/4, view ±π/2) | add-on AnimalPanicMixin | behavior/AnimalPanic.java:39-45 | match (add-on, GHAST-15) |
| AnimalPanic start/duration (100-120, `level.getRandom().nextInt(21)`) | 1.21.1 behavior/AnimalPanic.java, Behavior.java | AnimalPanic.java:54, :69-73; Behavior.java | duration matches; native `start` also stops navigation: **AI-GHS-D4** |
| IDLE p1 FollowTemptation(1.25, close 3.0, eyes) | vb HappyGhastAi.java:86 + add-on HappyGhastAiMixin (5.0→3.0) + FollowTemptationMixin (EyeEntityTracker) | HappyGhastAi.java:52; behavior/FollowTemptation.java:84-96 | match (add-on, GHAST-13/14) |
| FollowTemptation memories: IS_TEMPTED REGISTERED/false vs VALUE_ABSENT/erase | 1.21.1 FollowTemptation | FollowTemptation.java:42, :79 | engine change with no effect on this mob (nothing else writes IS_TEMPTED) |
| IDLE p2/p3 follow player / adult, range UniformInt(3,16), speed 1.1, close min-1 = 2, eye trackers | vb GhastlingFollowAdult.java:20-37 + add-on HappyGhastAiMixin + GhastlingFollowAdultMixin | behavior/BabyFollowAdult.java:21-40; HappyGhastAi.java:28, :53-54 | match (add-on) |
| IDLE p4 RunOne[RandomStroll.fly(1.0) w1, SetWalkTargetFromLookTarget(1.0,3) w1]; ShufflingList `nextFloat()` per entry | vb HappyGhastAi.java:89 | HappyGhastAi.java:55; ShufflingList | match |
| RandomStroll.fly → AirAndWaterRandomPos(10,7,-2, view, π/2): per attempt `nextFloat` (angle), `nextDouble` (dist), `nextInt(15)` (y), ×10 attempts | 1.21.1 util/RandomPos.java, AirAndWaterRandomPos.java | util/RandomPos.java:27-45, :97-113 | match for angle/distance/y. The home-bias draw differs: **AI-GHS-D3** |
| Home-bias offset in generateRandomPosTowardDirection (used by stroll, panic and the MoveToTargetSink fallback) | 1.21.1 util/RandomPos.java:139-158 | util/RandomPos.java:115-134 | **AI-GHS-D3** |
| PANIC activity gated on IS_PANICKING; updateActivity [PANIC, IDLE] | vb HappyGhastAi.java:94-100 | HappyGhastAi.java:60-66 | match |
| Ghastling move control FlyingMoveControl(180, hovers) | vb :128 | :134; control/FlyingMoveControl.java | match (class identical in both engines) |
| Ghastling horizontal thrust (input decay ×0.98 order) | 1.21.1 LivingEntity.java:2806-2807 | LivingEntity.java:3087, :3181-3184 | **AI-GHS-D1b** |
| Ghastling nav: Flying, no doors, floats, canMoveDirectly no-water flag, required path length 48 (768 nodes) | vb :582-592 + add-on BabyFlyingPathNavigationMixin | :601-613; navigation/PathNavigation.java:56-90, :146-172 | match (add-on, GHAST-11) |
| FlyNodeEvaluator start/neighbours; FlyingPathNavigation followThePath | 1.21.1 vs 26.2 | pathfinder/FlyNodeEvaluator.java:36-120 | match (renames only) |
| LookControl / BodyRotationControl / MoveControl base | 1.21.1 control/* | control/* | match (setLookAt(Entity) Y is eyeY for living targets in both) |
| GoalSelector tick (odd ticks tickRunningGoals) | 1.21.1 Mob.java:773-812 | Mob.java:725-763 | match |
| GoalSelector.removeAllGoals (native stops running goals first) | 1.21.1 GoalSelector | goal/GoalSelector.java | engine change with no effect here (the age-boundary goal swaps start with no running goals) |
| Ageing: baby start -24000; +1/tick; feed food tag → ageUp((int)((-age/20)×0.1F)); forced-age particles 40 | Animal.mobInteract / AgeableMob (1.21.1) + vb :255-265 | animal/Animal.java:141-165; AgeableMob.java:125-143, :196-214, :261-263 | match |
| Golden-dandelion age lock (26.x) | not in VB | AgeableMob.java:70-104 | not backported: out of this group's scope (a separate feature) |
| canFallInLove false, breed offspring | vb :250-257 | :252-260 | match |
| Age boundary → swap move/look/nav/goals, clear brain | vb :115-143 | :121-150 | match |
| Leash rules: elastic 10, snap 16, elastic pull → WAIT, quad holder (-0.03125, 0.4375, 0.46875, 0.03125), holder flag 5 ticks, no stay-close | vb :211-213, :499-535 | :196-199, :496-532 | match |
| Flying-vehicle kick exemption | vb core/mixin/common/happy_ghast/ServerGamePacketListenerImplMixin.java:23-27 | ServerGamePacketListenerImpl.java:520 | match in effect |
| Dried ghast: hydration 0..3, scheduled tick 5000 from randomTick (waterlogged or level>0), +1 per tick while wet / -1 while dry, spawn at 3 | vb blocks/DriedGhastBlock.java:86-106, :167-171 | level/block/DriedGhastBlock.java:93-113, :161-166 | match |
| Dried ghast randomTicks property | vb registries/blocks/ModBlocks.java:328 | level/block/Blocks.java:3862 | match |
| Ghastling spawn (setBaby → -24000, yaw from facing, sound) | vb DriedGhastBlock.java:108-134 | DriedGhastBlock.java:115-127; core/Direction.java:134-142 | match |
| Dried ghast animateTick random order: dry `nextInt(40)`, `nextInt(6)`; wet `nextInt(40)`, `nextInt(6)`, `nextFloat`×3 | vb :136-165 (+ add-on WHITE_SMOKE / BLOCKS) | :130-159 | match (client-side only) |
| Dried ghast waterlogging on placement / placeLiquid | vb :174-197 + add-on DriedGhastBlockMixin | :168-193 | match (add-on, GHAST-24) |

### AI-GHS-01  Wander-target home check uses the floored block corner instead of the exact position against the home block centre
- severity: gameplay
- vb: `src-vb/com/blackgear/vanillabackport/common/level/entities/mob/animal/happy_ghast/HappyGhast.java:833` (RandomFloatAroundGoal.chooseRandomPositionWithRestriction): `return mob.hasRestriction() && !mob.isWithinRestriction(BlockPos.containing(target)) ? null : target;`. 1.21.1 `Mob.java:1304-1305`: `return this.restrictRadius == -1.0F ? true : this.restrictCenter.distSqr(pos) < (double)(this.restrictRadius * this.restrictRadius);` (distSqr = low-corner to low-corner).
- native: `src-mc/net/minecraft/world/entity/monster/Ghast.java:486`: `return mob.hasHome() && !mob.isWithinHome(target) ? null : target;` → `Mob.java:1219-1220`: `return this.homeRadius == -1 ? true : this.homePosition.distToCenterSqr(pos) < this.homeRadius * this.homeRadius;` (exact Vec3 against home + 0.5).
- difference: along one axis, native accepts target t in (h-r+0.5, h+r+0.5) open around the block centre. VB accepts floor(t) in (h-r, h+r), which is t in [h-r+1, h+r). So VB's accepted ball is shifted -0.5 on each axis and about 1 block narrower. That is 127 vs 128 blocks across for r=64 (unharnessed adult) and 63 vs 64 for r=32 (harnessed). A ghast near its home edge therefore rejects a different set of draws. Each rejection costs 3 more `nextFloat()` calls, so both the wander-target distribution and the number of RNG draws differ.
- fix_kind: mixin-vb (`HappyGhast$RandomFloatAroundGoal.chooseRandomPositionWithRestriction`)
- fix: `@Redirect(method="chooseRandomPositionWithRestriction", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Mob;isWithinRestriction(Lnet/minecraft/core/BlockPos;)Z"))` and capture the target with `@Local(ordinal=0) Vec3 target` (or wrap the whole method with `@WrapMethod`). Return `mob.getRestrictRadius() == -1.0F || mob.getRestrictCenter().distToCenterSqr(target) < (double)((int)mob.getRestrictRadius() * (int)mob.getRestrictRadius())`.
- config_gate: none

### Engine drift (happy ghast)

### AI-GHS-D1  Ridden happy ghast strafes about 39% faster on diagonals (1.21.1 player input is not square-normalized)
- severity: gameplay
- vb: `vb:…/happy_ghast/HappyGhast.java:357` (getRiddenInput) `float forward = player.xxa;` (the strafe component, later `new Vec3(forward, upward, strafe)`). In 1.21.1 that value is set by `src-121/net/minecraft/client/player/LocalPlayer.java:690-691`: `this.xxa = this.input.leftImpulse; this.zza = this.input.forwardImpulse;` and then decayed by `src-121/net/minecraft/world/entity/LivingEntity.java:2806-2807`: `this.xxa *= 0.98F; this.zza *= 0.98F;`. Holding W+A gives xxa = 0.98.
- native: `src-mc/.../happyghast/HappyGhast.java:354` `float strafe = controller.xxa;`. The value comes from `src-mc/net/minecraft/client/player/KeyboardInput.java` `this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();` then `LocalPlayer.java:705-731` (`input.scale(0.98F)` → `modifyInputSpeedForSquareMovement`). Holding W+A gives xxa = 0.70710677.
- difference: the strafe term is 0.98 in VB vs 0.7071 in native whenever forward/back and left/right are both held (+38.6%). Pure strafe (0.98) and the forward/up terms match, because getRiddenInput uses only the sign of zza. Horses and other mounts drift the same way, but the happy ghast is the only backported mount.
- fix_kind: mixin-vb (`HappyGhast.getRiddenInput`)
- fix: `@ModifyVariable(method="getRiddenInput", at=@At(value="STORE", ordinal=0), ordinal=0)` on the first float local. Return `(player.zza != 0.0F && player.xxa != 0.0F) ? Math.signum(player.xxa) * 0.70710677F : player.xxa`. This reproduces native normalize → ×0.98 → square-movement for keyboard input. Sneaking dismounts and item-use slowdown is skipped for passengers in both engines, so no other factor applies.
- config_gate: none

### AI-GHS-D1b  Ghastling horizontal flight is 2% slower (input decay runs after the move control in 1.21.1)
- severity: gameplay
- vb: `src-121/net/minecraft/world/entity/LivingEntity.java:2767` `this.serverAiStep();` (FlyingMoveControl sets zza = speed via `setSpeed`), then `:2806-2807` `this.xxa *= 0.98F; this.zza *= 0.98F;`, then `:2821` `this.travel(vec31);`. VB travel `vb:…/HappyGhast.java:185` `this.moveRelative(speed, travelVector);` therefore receives 0.98·speed.
- native: `src-mc/net/minecraft/world/entity/LivingEntity.java:3087` `this.applyInput();` (`:3181-3184` `this.xxa *= 0.98F; this.zza *= 0.98F;`) runs before `:3094` `this.serverAiStep();`. The move control then overwrites zza with the full speed before `:3141` `this.travel(input);`.
- difference: when a ghastling is pathing, its horizontal impulse per tick is 0.98 × (speedModifier × 0.05) in VB vs 1.0× in native: stroll/follow 1.0/1.1, tempt 1.25, panic 2.0. The vertical impulse (yya, never decayed) is the same. Net horizontal cruise speed is 2% lower.
- fix_kind: mixin-mc (`LivingEntity.aiStep`, scoped to HappyGhast babies)
- fix: `@WrapOperation` on the two `PUTFIELD xxa/zza` after the `travel` profiler push. When `this instanceof HappyGhast g && g.isBaby() && !level().isClientSide`, skip the decay. Add `@Inject(method="aiStep", at=@At(value="INVOKE", target="LivingEntity;isImmobile()Z"))` doing `xxa *= 0.98F; zza *= 0.98F` for the same entities, which moves the decay to native's position.
- config_gate: none

### AI-GHS-D2  Goals, move control and look control still run while a player steers the ghast
- severity: gameplay
- vb: `src-121/net/minecraft/world/entity/Entity.java:3219-3220` `public boolean isEffectiveAi() { return !this.level().isClientSide; }` → `src-121/net/minecraft/world/entity/LivingEntity.java:2765-2767` `} else if (this.isEffectiveAi()) { ... this.serverAiStep();`. So the ridden ghast ticks TemptGoal / RandomFloatAroundGoal / GhastMoveControl / HappyGhastLookControl on the server. VB only covers the look-control idle branch: `vb:…/HappyGhast.java:734-736` `if (HappyGhast.this.isVehicle() && HappyGhast.this.getControllingPassenger() instanceof Player) { return; }`.
- native: `src-mc/net/minecraft/world/entity/Entity.java:3607-3618` `return this.level.isClientSide() ? this.isLocalClientAuthoritative() : !this.isClientAuthoritative();` with `isClientAuthoritative()` true for a player controlling passenger. Then `Entity.java:3625-3626` `isEffectiveAi() { return this.isLocalInstanceAuthoritative(); }` and `LivingEntity.java:3092` `} else if (this.isEffectiveAi() && !this.level().isClientSide()) {`. No AI runs at all while a player controls the ghast.
- difference: while ridden, VB still does the following:
  - draws RNG: `nextFloat`×3 per wander attempt (up to 192), `nextInt(5)` in GhastMoveControl, and `nextDouble` in the TemptGoal step;
  - adds GhastMoveControl impulses, which travelRidden then zeroes;
  - turns the server-side yaw toward a rider holding a snowball through TemptGoal → `lookAtCooldown` branch (vb :726-732). That yaw is what other players receive from the server.

  None of this happens natively.
- fix_kind: mixin-vb (HappyGhast)
- fix: add `@Override public boolean isEffectiveAi()` to `HappyGhastMixin`: `return super.isEffectiveAi() && !(!this.level().isClientSide && this.getControllingPassenger() instanceof Player);`. 1.21.1 `isControlledByLocalInstance` checks the player passenger before `isEffectiveAi`, so client simulation is unchanged. VB's look-control early return can then stay as is.
- config_gate: none

### AI-GHS-D3  Home-bias offset in random positions is integer `nextInt(r/2)` in 1.21.1 vs continuous `nextDouble()*r/2` (ghastling stroll and panic)
- severity: gameplay
- vb: `src-121/net/minecraft/world/entity/ai/util/RandomPos.java:142-154` (generateRandomPosTowardDirection): `if (mob.hasRestriction() && range > 1) { ... i -= random.nextInt(range / 2); ... i += random.nextInt(range / 2); ... j -= random.nextInt(range / 2); ... j += random.nextInt(range / 2);`. Ghastlings always have a 32-block restriction (`vb:…/HappyGhast.java:455-460`), so every stroll, panic and MoveToTargetSink fallback draw takes this path.
- native: `src-mc/net/minecraft/world/entity/ai/util/RandomPos.java:118-130`: `if (mob.hasHome() && xzDist > 1.0) { ... xt -= random.nextDouble() * xzDist / 2.0; ... xt += random.nextDouble() * xzDist / 2.0; ... zt -= random.nextDouble() * xzDist / 2.0; ... zt += random.nextDouble() * xzDist / 2.0;`
- difference:
  - RandomStroll.fly (dist 10): offset ∈ {0,1,2,3,4} with mean 2.0 vs uniform [0,5) with mean 2.5.
  - Panic (dist 5): {0,1} with mean 0.5 vs [0,2.5) with mean 1.25.
  - MoveToTargetSink fallback (10): as stroll.

  The pull toward home is weaker in VB and quantized. The draw kind differs too (one `nextInt` vs one `nextDouble` per axis: one vs two LCG steps), so later draws are shifted.
- fix_kind: mixin-mc (`RandomPos.generateRandomPosTowardDirection`, scoped)
- fix: `@Inject(method="generateRandomPosTowardDirection", at=@At("HEAD"), cancellable=true)`. When `mob instanceof HappyGhast`, compute native's body with `double xt = pos.getX(), zt = pos.getZ()`, `mob.hasRestriction() && range > 1.0` and `random.nextDouble() * range / 2.0` for x then z, then return `BlockPos.containing(xt + mob.getX(), pos.getY() + mob.getY(), zt + mob.getZ())`.
- config_gate: none

### AI-GHS-D4  Panic start does not stop navigation in 1.21.1
- severity: gameplay
- vb: `src-121/net/minecraft/world/entity/ai/behavior/AnimalPanic.java` start: `entity.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true); entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);` (no navigation stop). The tick then gates on `if (owner.getNavigation().isDone())`.
- native: `src-mc/net/minecraft/world/entity/ai/behavior/AnimalPanic.java:69-73`: `body.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true); body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); body.getNavigation().stop();`, then `:80-82` `if (body.getNavigation().isDone()) { Vec3 panicToPos = this.getPanicPos(body, level);`.
- difference: a ghastling hurt while following a path natively draws its first panic target (AirAndWaterRandomPos, ~30 RNG calls) in the same brain tick. In VB that happens only after MoveToTargetSink has stopped the path, at least one brain tick later. Until then it keeps flying along the old path.
- fix_kind: mixin-mc (`AnimalPanic.start`, scoped)
- fix: `@Inject(method="start", at=@At("TAIL"))`: `if (entity instanceof HappyGhast) entity.getNavigation().stop();`
- config_gate: none

### AI-GHS-D5  Sensor start delays drawn from the entity RNG in native, from a static RNG in 1.21.1
- severity: cosmetic
- vb: `src-121/net/minecraft/world/entity/ai/sensing/Sensor.java` `private static RandomSource RANDOM = RandomSource.createThreadSafe();` … `this.timeToTick = (long)RANDOM.nextInt(scanRate);` (in the constructor)
- native: `src-mc/net/minecraft/world/entity/ai/Brain.java:86` `newSensor.randomlyDelayStart(randomSource);` with `:498` `new Brain<>(..., body.getRandom())`; `Sensor.java:40` `this.timeToTick = randomSource.nextInt(this.scanRate);`
- difference: native consumes 5 × `nextInt(20)` from the ghast's own RNG each time a brain is built (constructor and NBT load). VB consumes none. The distribution of the first sensor tick (uniform 0..19) is the same. Only the entity RNG sequence is shifted by 5 draws, so no averaged outcome changes.
- fix_kind: mixin-vb (`HappyGhast.makeBrain`)
- fix: optional. At `makeBrain` RETURN, call `this.getRandom().nextInt(20)` five times and set each sensor's `timeToTick` from those draws (accessor on `Sensor.timeToTick`).
- config_gate: none

counts: farm=0 gameplay=1 visual=0 audio=0 data=0 cosmetic=0 drift=6


## Copper golem (brain + item transport) and iron golem flower goals

Effective behaviour = VB 1.2.2 + add-on `copper` mixins (CopperGolemMixin, CopperGolemPathNavigationMixin, TransportItemsBetweenContainersMixin, OfferCopperGolemFlowerGoalMixin, OfferFlowerGoalMixin, ContainerOpenersCounterAdapterMixin, ContainerOpenersCounterMixin, MobEquipmentMixin) + `tags/block/transport_item_destination_blocks.json` (replace: chest, trapped_chest).
First-round findings COPPER-01/02/03/04/07/22/32 are fixed in the add-on. I checked each fix against native and found them correct, except where AI-CPG-01..03 below build on them.
Paths: VB = `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/`, native = `<mc-26.2>/net/minecraft/`, 1.21.1 = `<mc-1.21.1>/net/minecraft/`, add-on = `src/main/java/com/coolfreeze23/vbparity/copper/mixin/`.

### Coverage

| mechanic | VB/add-on location | native location | result |
|---|---|---|---|
| Attributes: MOVEMENT_SPEED 0.2, STEP_HEIGHT 1.0, MAX_HEALTH 12, FOLLOW_RANGE default 16 | CopperGolem.java:100-102 | golem/CopperGolem.java:103-105 | match |
| Entity type: MISC, 0.49x0.98, eye 0.8125, tracking 10 | registries/entities/ModEntityTypes.java:41-43 | entity/EntityTypes.java:299-301 | match |
| AbstractGolem base (no ambient/hurt/death sound, interval 120, never despawn) | 1.21.1 animal/AbstractGolem | animal/golem/AbstractGolem | match |
| Persistence, door opening (canOpenDoors true; canPassDoors true = native NodeEvaluator default) | CopperGolem.java:85,93-97 | CopperGolem.java:94-95, NodeEvaluator.java:21 | match |
| Path malus FIRE_IN_NEIGHBOR 16 / DAMAGING_IN_NEIGHBOR 16 / FIRE -1 (1.21.1 names DANGER_FIRE / DANGER_OTHER / DAMAGE_FIRE) | CopperGolem.java:87-89 | CopperGolem.java:97-99 | match (renamed enum) |
| Required path length 48 → max path length 48, 768 visited nodes | addon CopperGolemPathNavigationMixin:39-49 | CopperGolem.java:93, PathNavigation.java:72-82 | match (COPPER-01 fix correct) |
| createPath surface snap only when !canPathToTargetsBelowSurface; findSurfacePosition loops (minY / maxY = maxBuildHeight-1) | addon CopperGolemPathNavigationMixin:65-112 | GroundPathNavigation.java:44-89 | match (COPPER-02 fix correct; native's unused reachRange arg has no effect) |
| Spawn transport cooldown `nextInt(60, 100)` (entity random) | CopperGolem.java:90 | CopperGolem.java:100 | match; RNG position drift AI-CPG-D1 |
| Sensors NEAREST_LIVING_ENTITIES (16 cube = FOLLOW_RANGE 16), HURT_BY, scan 20 | CopperGolemAi.java:40-42 | CopperGolem.java:69-71, NearestLivingEntitySensor | match at default FOLLOW_RANGE; initial-delay RNG drift AI-CPG-D1 |
| Memories (VISITED/UNREACHABLE with GlobalPos list codec, TRANSPORT_ITEMS_COOLDOWN_TICKS without codec) | ModMemoryModuleTypes | MemoryModuleType.java:86-92 | match (same save/no-save) |
| Memory expiry semantics (ttl 6000 → erased on the 6001st brain tick) | 1.21.1 Brain.forgetOutdatedMemories:415-426 | MemorySlot.tick:17-25 | match |
| Brain tick order (forget → sensors → start → tick), TreeMap priority, HashMap activity, LinkedHashSet behaviours | 1.21.1 Brain:402-407,373-375 | Brain.java:395-400,379-381 | match |
| CORE: AnimalPanic(1.5) / LookAtTargetSink(45,90) / MoveToTargetSink / InteractWithDoor / CountDown GAZE / CountDown TRANSPORT | CopperGolemAi.java:76-89 | CopperGolemAi.java:54-67 | match (order, args) |
| AnimalPanic duration 100-120 (`level.getRandom().nextInt(21)`), LandRandomPos(5,4), water search when on fire | 1.21.1 AnimalPanic | ai/behavior/AnimalPanic | match. Native also calls `getNavigation().stop()` in start; VB stops in the same tick through MoveToTargetSink.stop, so no observable difference |
| MoveToTargetSink 150-250, cooldown `level.getRandom().nextInt(40)` when stuck, DefaultRandomPos.getPosTowards(10,7) | 1.21.1 MoveToTargetSink | ai/behavior/MoveToTargetSink | match |
| CountDownCooldownTicks (decrement, erase at <=0) | 1.21.1 | ai/behavior/CountDownCooldownTicks | match |
| IDLE p0 TransportItemsBetweenContainers(1.0, src, dst, 32, 8) | CopperGolemAi.java:95-107 | CopperGolemAi.java:73-85 | match (dst = `DEST && !SOURCE` with the add-on tag = chest/trapped_chest) |
| IDLE p1 SetEntityLookTargetSometimes(PLAYER, 6, UniformInt 40-80), `interval.sample(level.getRandom())` | CopperGolemAi.java:108 | CopperGolemAi.java:86, SetEntityLookTargetSometimes:26,51 | match |
| IDLE p2 RunOne{WALK_TARGET absent, COOLDOWN present}: RandomStroll(1.0,2,2) w1 / DoNothing(30,60) w1 | CopperGolemAi.java:109-120 | CopperGolemAi.java:87-95 | match; RandomPos drift for restricted golems AI-CPG-D2 |
| Transport memory requirements (VISITED/UNREACHABLE registered, COOLDOWN absent, IS_PANICKING absent); start/stop toggles below-surface flag | TransportItemsBetweenContainers.java:76-102,590-595 | ai/behavior/TransportItemsBetweenContainers.java:78-105,597-602 | match |
| Behavior duration 60/60 (`level.getRandom().nextInt(1)` per start), timedOut false, extra start `!isLeashed`, canStillUse cooldown/panic/leash | TIBC:104-114 | TIBC:107-118 | match |
| Tick order: updateInvalidTarget → QUEUING → TRAVELLING → INTERACTING | TIBC:116-133 | TIBC:120-137 | match |
| Target search: chunks `rangeClosed(chunk, floorDiv(32,16)+1)`, `getChunkNow`, only ChestBlockEntity, strict `distToCenterSqr <` (first in Object2ObjectOpenHashMap order wins ties), AABB `inflate(32,8,32)` around blockPosition, passenger range 1 | TIBC:270-302,393-404 + addon TransportItemsBetweenContainersMixin:55-58 | TIBC:274-304,394-405 | match (COPPER-04 fix correct) |
| Validity: wanted block, not visited/unreachable (both halves), not locked (`!lockKey.equals(NO_LOCK)`) | TIBC:305-335 | TIBC:307-336 | match |
| hasValidTarget / hasValidTravellingPath / canSeeAnyTargetSide (6 clips, COLLIDER, NONE) | TIBC:337-360,471-477 | TIBC:338-361,479-487 | match |
| Blocked chest (solid conductor above, sitting cat) | TIBC:372-374 (only ChestBlockEntity, now always) | TIBC:373-375 | match |
| Distances: queue 3.0, interact 0.5 / 1.0 when the path is done, keep interacting 2.0, y inflate 0.5 | TIBC:160-176,453-463 | TIBC:164-179,457-467 | match |
| Interaction ticks: 1 = open + state, 9 = sound, 60 = close + action, 60-tick threshold | CopperGolemAi.java:141-157, TIBC:177-189 | CopperGolemAi.java:119-135, TIBC:181-194 | **AI-CPG-01, AI-CPG-02** (what gets opened and closed) |
| Visited memory: cap 10 → cooldown, ttl 6000; unreachable: cap 50 → cooldown, ttl 6000 | TIBC:426-447 | TIBC:430-451 | match |
| Cooldown after no target: 140 ticks, clears both memories | TIBC:583-588 | TIBC:590-595 | match |
| Pickup: first non-empty slot, `min(count, golemItemCarryAmount)` (default 16) | TIBC:526-540 | TIBC:535-548 (`min(count, 16)`) | match at the default config |
| Deliver: target empty OR has `isSameItem` stack; insert into first empty slot or `isSameItemSameComponents` stacks; leftover → stopTargeting (no memory clear) | TIBC:487-568 | TIBC:497-575 | match (including the native `setCount(count - countThatCanBeAdded)` quirk) |
| Queue predicate (any ContainerUser incl. players with the chest open, both halves) | CopperGolemAi.java:171-173 | CopperGolemAi.java:149-153 | match |
| Opener recheck every 5 ticks counting golems + players, spectators excluded | addon ContainerOpenersCounterAdapterMixin, ContainerOpenersCounterMixin | ContainerOpenersCounter.java:28-102 | match (COPPER-03 fix correct) |
| hasContainerOpen (own pos or connected double half), interaction range 3.0 | CopperGolem.java:404-420 | CopperGolem.java:411-427 | match |
| Weathering: `nextIntBetweenInclusive(504000, 552000)` (level random) on unset and after each step; 0 when oxidized; -2 waxed | CopperGolem.java:252-280 | CopperGolem.java:259-278 | match at default config (`golem_weathering_tick_from/to`) |
| Statue conversion: oxidized and air at blockPosition, `level.getRandom().nextFloat() <= 0.0058F` per tick; pose `this.random.nextInt(0, 4)`; FACING from yRot; flags 3; drop preserved equipment; leash drop by ENTITY_DROPS | CopperGolem.java:275-305 | CopperGolem.java:273-309 | match (RNG order: weathering roll only when due, then statue nextFloat, then pose nextInt) |
| Lightning: per-bolt UUID, one step back, nextWeatheringTick = -1 | CopperGolem.java:443-454 | CopperGolem.java:455-466 | match |
| Lightning entity box (±3, +9 up), every tick of bolt life | 1.21.1 LightningBolt:155-165 | LightningBolt.java:144-154 | match |
| Wax / axe scrape / deoxidize interactions, empty-hand item throw | CopperGolem.java:200-250 + addon CopperGolemMixin:19-32 | CopperGolem.java:209-257 | match (COPPER-07 fix correct) |
| Shearing (poppy, 1.5 offset, PLAYERS source) | CopperGolem.java:422-431 + addon CopperGolemMixin:35-41 | CopperGolem.java:429-440 | match (COPPER-32 fix correct) |
| hurt → state IDLE; drop preserved equipment on death | CopperGolem.java:433-441 | CopperGolem.java:442-452 | match |
| Client idle head spin `random.nextInt(200, 240)`, sound at +10 | CopperGolem.java:307-357 | CopperGolem.java:311-361 | match (client only) |
| Golem build (carved pumpkin on #copper; spawn at pumpkin +0.05; chest replace flags 2; weather from block) | core/mixin/common/copper_golem/CarvedPumpkinBlockMixin.java:66-128 | block/CarvedPumpkinBlock.java:64-99,202-227 | match (VB checks copper before snow/iron, but the patterns are mutually exclusive on the block under the pumpkin) |
| Statue → golem: axe on unaffected statue, golem at block centre/bottom with FACING yaw, custom name, spawn sound, block removed | blocks/WeatheringCopperGolemStatueBlock.java:39-65, block_entities/CopperGolemStatueBlockEntity.java:31-51 | WeatheringCopperGolemStatueBlock.java:50-82, CopperGolemStatueBlockEntity.java:90-110 | match |
| Statue oxidation random tick (0.05688889, manhattan 4 scan, `(older+1)/(older+same+1)`², ×0.75 unaffected) | WeatheringCopperGolemStatueBlock.java:27-33 + 1.21.1 ChangeOverTimeBlock | WeatheringCopperGolemStatueBlock.java:36-44 + ChangeOverTimeBlock | match |
| Iron golem flower: isDay/isBrightOutside → `nextInt(8000)` → nearest villager or #accepts_iron_golem_gift within inflate(6,2,6), TargetingConditions nonCombat range 6 | OfferCopperGolemFlowerGoal.java:32-50 + addon OfferCopperGolemFlowerGoalMixin:24-50, OfferFlowerGoalMixin:67-70 | goal/OfferFlowerGoal.java:29-48, ServerEntityGetter.java:47-69 | target choice and rate match (COPPER-22 fix correct); goal position in the selector differs: **AI-CPG-03** |
| Flower goal flags MOVE+LOOK, 400 ticks (`adjustedTickDelay` → 200 goal ticks), look 30/30, poppy into the empty antenna slot when within the box, guaranteed drop | OfferCopperGolemFlowerGoal.java:27-86 | OfferFlowerGoal.java:24-87 | match (antenna slot HEAD vs SADDLE is first-round COPPER-25, engine) |
| Goal-selector cadence (full tick on even id-based ticks, running goals otherwise) | 1.21.1 Mob.java:780-792 | Mob.java:732-744 | match |
| Mob armor roll (copper armor area, MobEquipmentMixin) | addon MobEquipmentMixin:31-62 | Mob.java:975-1003 | match (COPPER-19 fix correct; RNG order nextFloat, nextInt(3), 3×nextFloat, then per slot nextFloat) |
| Path following tolerance (golem width 0.49 → 0.505; Forge offset `(int)(1.49)/2D` = 0.5) | 1.21.1 PathNavigation.java:264-276 | PathNavigation.java:251-266 | match (`<=` vs `<` only at the exact boundary) |

#### Random calls, in order (all identical in VB+add-on and native unless a finding says otherwise)
- Golem construction: native draws `sensor.randomlyDelayStart(body.getRandom())` = `nextInt(20)` ×2 (Brain.java:84-86) and then `getRandom().nextInt(60, 100)` (CopperGolem.java:100). VB draws only `getRandom().nextInt(60, 100)` (CopperGolem.java:90); sensor delays use the static `Sensor.RANDOM` (AI-CPG-D1).
- Per brain start: `Behavior.tryStart` → `level.getRandom().nextInt(max+1-min)` (TransportItems nextInt(1), AnimalPanic nextInt(21), MoveToTargetSink nextInt(101), DoNothing nextInt(31)). RunOne → ShufflingList shuffle (level random). SetEntityLookTargetSometimes → `UniformInt(40,80).sample(level.getRandom())`. RandomStroll → LandRandomPos 10 attempts × `generateRandomDirection` (`nextInt(5)`, `nextInt(5)`, `nextInt(5)` on the entity random).
- Per open: `ChestBlockEntity.playSound` → `level.random.nextFloat()*0.1F+0.9F` once (RIGHT or SINGLE half only). AI-CPG-01 adds one extra draw per neighbour chest.
- Per server tick: `updateWeathering` → `level.getRandom().nextIntBetweenInclusive(504000,552000)` only when unset or a step is due. When fully oxidized and standing in air: `level.getRandom().nextFloat()` (≤0.0058), then on success `this.random.nextInt(0,4)`.
- Iron golem goal tick: native order in `availableGoals` is MeleeAttack, MoveTowardsTarget, MoveBackToVillage, GolemRandomStrollInVillage, **OfferFlower `nextInt(8000)`**, LookAtPlayer `nextFloat()`, RandomLookAround `nextFloat()`. VB+add-on is the same list with the vanilla OfferFlower drawing nothing, then **VB OfferCopperGolemFlower `nextInt(8000)` last** (AI-CPG-03).

---

### AI-CPG-01  A golem opening a single chest also opens the chest beside it (and corrupts its opener count with the add-on recheck)
- severity: farm
- vb: `common/level/entities/mob/animal/golem/copper_golem/container/ContainerOpenersCounterAdapter.java:146-153` (handleDoubleChest), called from startOpen `:68-74` and stopOpen `:88-94`:
  ```java
  if (blockEntity instanceof ChestBlockEntity) {
     BlockPos connectedPos = ContainerManager.getConnectedBlockPos(blockEntity.getBlockPos(), blockEntity.getBlockState());
     BlockEntity connectedContainer = level.getBlockEntity(connectedPos);
     if (connectedContainer instanceof ChestBlockEntity) {
        getCounter(connectedContainer).ifPresent(counter -> consumer.accept(counter, connectedPos, connectedContainer.getBlockState()));
  ```
  It never checks `TYPE`. `ContainerManager.getConnectedBlockPos` → 1.21.1 `ChestBlock.getConnectedDirection` (`ChestBlock.java:194-196`): `return state.getValue(TYPE) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();`, which for a SINGLE chest is the counter-clockwise neighbour. Called from `CopperGolemAi.java:142` `ContainerManager.startOpen(target.blockEntity(), golem);` and `:152-153` `ContainerManager.stopOpen(target.blockEntity(), golem);`. The add-on (`ContainerOpenersCounterAdapterMixin:76-84`) also schedules a 5-tick recheck on that neighbour.
- native: `world/entity/animal/golem/CopperGolemAi.java:118-120`: `Container container = target.container(); if (ticksSinceReachingTarget == 1) { container.startOpen(copperGolem);`. `target.container()` comes from `ChestBlock.getContainer(chestBlock, blockState, level, blockPos, false)` (`TransportItemsBetweenContainers.java:646-647`). That is a `CompoundContainer` only when `DoubleBlockCombiner.combineWithNeigbour` pairs the halves (`DoubleBlockCombiner.java:38-42`: `neighbourState.is(state.getBlock())`, neighbour type not SINGLE and different, same FACING). Otherwise it is the single `ChestBlockEntity`, whose `startOpen` (`ChestBlockEntity.java:136-142`) increments only its own `openersCounter`.
- difference: Native opens exactly the target chest (both halves if it is a real double chest). In VB, when the target is a SINGLE chest (or copper chest) and any `ChestBlockEntity` sits at `FACING.getCounterClockWise()`, that neighbour is opened too. Typical neighbours are a trapped chest beside a chest, a vanilla chest beside a copper chest, chests with different facings, and shift-placed chests. The neighbour gets the open sound (one extra `level.random.nextFloat()`), the `CONTAINER_OPEN` game event (sculk sensors) and the lid block event. A trapped-chest neighbour outputs `clamp(openCount,0,15)` = 1 (`TrappedChestBlock.java:58-59`).
  - VB alone: the neighbour stays open for the whole 59 ticks.
  - VB + add-on: the neighbour recheck at +5 ticks finds no user (`CopperGolem.hasContainerOpen` is false for a SINGLE chest's neighbour) and closes it. At tick 60 `decrementOpeners` then takes it to `openCount = -1`, with no close event (`ContainerOpenersCounterAdapter.java:114-115`: `int previous = counter.openCount--; if (counter.openCount == 0)`). After that, a player opening the chest goes from -1 to 0. 1.21.1 `incrementOpeners` (`ContainerOpenersCounter.java:26-30`) then plays no sound, opens no lid, emits no game event and schedules no recheck, and a trapped chest outputs 0. This repeats on every golem visit next to that chest and lasts until two openers overlap or the chunk reloads.
  Every golem trip to a copper chest or chest with a chest at its counter-clockwise side triggers this, so sorters that read trapped chests or sculk get wrong pulses.
- fix_kind: mixin-vb (`ContainerOpenersCounterAdapter.handleDoubleChest`)
- fix: `@ModifyExpressionValue(method = "handleDoubleChest", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/level/block/entity/ChestBlockEntity", ordinal = 0))` (the first `instanceof`), returning `original && vbparity$isCombinedDouble(blockEntity, level)`:
  ```java
  private static boolean vbparity$isCombinedDouble(BlockEntity be, Level level) {
     BlockState s = be.getBlockState();
     if (!(s.getBlock() instanceof ChestBlock) || s.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return false;
     BlockPos np = be.getBlockPos().relative(ChestBlock.getConnectedDirection(s));
     BlockState n = level.getBlockState(np);
     return n.is(s.getBlock()) && n.getValue(ChestBlock.TYPE) != ChestType.SINGLE
         && n.getValue(ChestBlock.TYPE) != s.getValue(ChestBlock.TYPE)
         && n.getValue(ChestBlock.FACING) == s.getValue(ChestBlock.FACING);
  }
  ```
  These are the same conditions as `DoubleBlockCombiner.combineWithNeigbour`. Use a `@WrapOperation` on the constant instead if the injector prefers `(Object, Operation<Boolean>)`, as `TransportItemsBetweenContainersMixin` already does. The `ConnectedChestConsumer` parameter is a private interface, so avoid a HEAD inject that would have to name it.
- config_gate: has_copper_golems

### AI-CPG-02  Double chests are closed by the golem one tick early (native leaves the close to the 5-tick recheck)
- severity: farm
- vb: `common/level/entities/mob/animal/golem/copper_golem/CopperGolemAi.java:151-156` (lambda in onTargetReachedInteraction):
  ```java
  if (tickSinceReachingTarget == 60) {
     if (ContainerManager.getEntitiesWithContainerOpen(target.blockEntity()).contains(golem)) {
        ContainerManager.stopOpen(target.blockEntity(), golem);
     }
     golem.clearOpenedChestPos();
  ```
  `stopOpen` decrements both halves (`ContainerOpenersCounterAdapter.java:81-97`) and fires `CONTAINER_CLOSE` with the golem as source.
- native: `CopperGolemAi.java:129-135`: `if (ticksSinceReachingTarget == 60) { if (container.getEntitiesWithContainerOpen().contains(body)) { container.stopOpen(copperGolem); } copperGolem.clearOpenedChestPos(); }`. For a double chest `container` is a `CompoundContainer`, which does not override `getEntitiesWithContainerOpen`, so it gets the `Container` default (`Container.java:53-55`: `return List.of();`). `stopOpen` is never called. The halves close in `ContainerOpenersCounter.recheckOpeners` (`ContainerOpenersCounter.java:66-93`, `level.gameEvent(null, GameEvent.CONTAINER_CLOSE, pos)`), which runs every 5 ticks from the open (`scheduleRecheck`, `:33,100-101`).
- difference: Let the golem open at game tick G (interaction tick 1). Then tick 60 is at G+59, entity phase, and native rechecks run at G+5k in the block-tick phase. For a single chest both versions close at G+59. For a double chest native closes at G+60 (the first recheck after `clearOpenedChestPos`), so it is open for 60 ticks; VB closes at G+59 (59 ticks). A double trapped chest's redstone pulse is 1 tick shorter in VB. The close sound and the close game event come 1 tick earlier, and the game event has the golem as source where native has null.
- fix_kind: mixin-vb (`CopperGolemAi`, the synthetic `lambda$onTargetReachedInteraction$*` that holds the tick-60 block)
- fix: `@WrapOperation(method = "lambda$onTargetReachedInteraction$0" /* confirm the index with javap */, at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/golem/copper_golem/container/ContainerManager;stopOpen(Lnet/minecraft/world/level/block/entity/BlockEntity;Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/golem/copper_golem/container/ContainerUser;)V"))`. When `vbparity$isCombinedDouble(blockEntity, blockEntity.getLevel())` (helper from AI-CPG-01) is true, return without calling the original; otherwise call it. This depends on the add-on's COPPER-03 recheck, which is already merged and gated the same way.
- config_gate: has_copper_golems

### AI-CPG-03  The iron-golem flower roll runs last in the goal list instead of at its native slot
- severity: cosmetic
- vb: `common/integrations/MobIntegrations.java:75`: `event.registerGoal(EntityType.IRON_GOLEM, 5, mob -> new OfferCopperGolemFlowerGoal((IronGolem)mob));` → Platform `common/integration/MobIntegration.java:45-52`: `EntityEvents.ON_SPAWN.register((entity, level) -> { if (entity instanceof Mob mob && predicate.test(mob)) { mob.goalSelector.addGoal(priority, factory.apply(mob)); }`. It runs on `EntityJoinLevelEvent` (`neoforge/CommonLoaderPipelines.java:122-125`), after `registerGoals`, so the goal is appended at the end of the `ObjectLinkedOpenHashSet`. addon `OfferFlowerGoalMixin.java:67-70`: `@Inject(method = "canUse", at = @At("HEAD"), cancellable = true) ... cir.setReturnValue(false);` blanks the vanilla goal at its slot. The roll is `OfferCopperGolemFlowerGoal.java:35`: `this.golem.getRandom().nextInt(8000) != 0`.
- native: `world/entity/animal/golem/IronGolem.java:69-75`: `addGoal(1, MeleeAttack) … addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6)); addGoal(5, new OfferFlowerGoal(this)); addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F)); addGoal(8, new RandomLookAroundGoal(this));` with the roll at `OfferFlowerGoal.java:33`: `this.golem.getRandom().nextInt(8000) != 0`. `GoalSelector.tick` evaluates `canUse` in insertion order (`GoalSelector.java:25`, `ObjectLinkedOpenHashSet`).
- difference: Rate and target choice match (one 1/8000 roll per goal evaluation, nearest of villagers and copper golems). Two things differ:
  - RNG order: native draws `nextInt(8000)` before LookAtPlayer's and RandomLookAround's `nextFloat()`; VB draws it after both.
  - Start/stop calls: in a tick where LookAtPlayer (p7) and the offer both pass `canUse`, native starts only the offer (7 cannot replace 5). VB first starts LookAtPlayer, which draws `nextInt(40)` in `start`, then the offer replaces and stops it. RandomLookAround does the same and draws `nextDouble()` and `nextInt(20)`.
  No farm output changes, but the iron golem's RNG stream and its look-goal start/stop sequence differ.
- fix_kind: mixin-mc (`OfferFlowerGoal.canUse`/`stop`) + mixin-vb (`OfferCopperGolemFlowerGoal.canUse`)
- fix: invert the COPPER-22 fix so the native-slot goal does the work:
  1. In `OfferFlowerGoal.canUse`, `@WrapOperation` the 1.21.1 `Level.getNearestEntity(Class, TargetingConditions, LivingEntity, double, double, double, AABB)` call. Run the add-on's existing nearest search over `LivingEntity` with `type == VILLAGER || type.is(ModEntityTypeTags.ACCEPTS_IRON_GOLEM_GIFT)` (the body of `OfferCopperGolemFlowerGoalMixin.vbparity$nearestCandidate`). Widen the field: `@Shadow private Villager villager` becomes a stored `LivingEntity` through `@Unique`, plus a `tick()`/`stop()` redirect.
  2. `@Inject(method = "stop", at = @At("HEAD"))` on `OfferFlowerGoal`: port VB's antenna block (`OfferCopperGolemFlowerGoal.java:63-70`).
  3. `@Inject(method = "canUse", at = @At("HEAD"), cancellable = true)` on `OfferCopperGolemFlowerGoal` → `false`, before its `isDay`/`nextInt(8000)`, so it draws nothing.
  4. Drop `OfferFlowerGoalMixin`'s HEAD cancel.
  Gate steps 1-3 on `has_copper_golems`; with it off, the vanilla goal runs untouched.
- config_gate: has_copper_golems

### Engine drift (copper golem)

### AI-CPG-D1  Brain sensors draw their start delay from the golem's own random (2 extra draws before the spawn cooldown)
- severity: cosmetic
- vb: 1.21.1 `world/entity/ai/sensing/Sensor.java:11,32`: `private static final RandomSource RANDOM = RandomSource.createThreadSafe();` … `this.timeToTick = (long)RANDOM.nextInt(scanRate);`. VB `CopperGolem.java:90` then draws `this.getRandom().nextInt(60, 100)` as the first entity-random call.
- native: `world/entity/ai/Brain.java:84-86`: `Sensor<? super E> newSensor = sensorType.create(); newSensor.randomlyDelayStart(randomSource);` with `randomSource = body.getRandom()` (`Brain.java:498`). `Sensor.java:40-42`: `this.timeToTick = randomSource.nextInt(this.scanRate);`. That is two `nextInt(20)` draws (NEAREST_LIVING_ENTITIES, HURT_BY) before `CopperGolem.java:100` `nextInt(60, 100)`.
- difference: Same distributions (sensor phase 0-19, cooldown 60-99). Only the position of the cooldown draw in the golem's random stream differs, and the static sensor RNG is shared by every mob in 1.21.1. A farm sees nothing.
- fix_kind: engine
- fix: none needed. Native order could be reproduced with an `@Inject` at `CopperGolem.<init>` HEAD-after-super that calls `getRandom().nextInt(20)` twice and rewrites each sensor's `timeToTick` through an accessor, but it has no observable effect.
- config_gate: has_copper_golems

### AI-CPG-D2  Restricted (leashed) golem strolls use int offsets of range/2 in 1.21.1 (always 0 for the golem's range 2) vs native double offsets
- severity: cosmetic
- vb: 1.21.1 `world/entity/ai/util/RandomPos.java:142-153` (generateRandomPosTowardDirection): `if (mob.hasRestriction() && range > 1) { ... i -= random.nextInt(range / 2); ... j += random.nextInt(range / 2); }`. The restriction comes from `PathfinderMob.java:71` `this.restrictTo(leashHolder.blockPosition(), 5);`. Reached via VB `CopperGolemAi.java:118` `RandomStroll.stroll(1.0F, 2, 2)` → LandRandomPos.
- native: `world/entity/ai/util/RandomPos.java:116-130`: `if (mob.hasHome() && xzDist > 1.0) { ... xt -= random.nextDouble() * xzDist / 2.0; ... zt += random.nextDouble() * xzDist / 2.0; }`, with home from `PathfinderMob.java:71` `this.setHomeTo(leashHolder.blockPosition(), (int)this.leashElasticDistance() - 1);`.
- difference: Only for a leashed golem during its transport cooldown (RunOne needs the cooldown memory; transport itself never starts while leashed). In 1.21.1 the stroll candidates get `nextInt(1)` = 0 offset toward the leash holder. Native shifts them by `U[0,1)` blocks toward home, and the draw is `nextDouble` instead of `nextInt` (same count, different method), across 10 attempts. The stroll target distribution is slightly different, with no effect on transport.
- fix_kind: engine
- fix: none (shared RandomPos utility; scoping it to the golem would mean a golem-only LandRandomPos copy in the RunOne).
- config_gate: has_copper_golems

counts: farm=2 gameplay=0 visual=0 audio=0 data=0 cosmetic=1 drift=2


## Mounts of Mayhem mobs (nautilus, zombie nautilus, camel husk, zombie horse, parched) — axis `ai`

Effective behaviour = VB 1.2.2 + add-on (mayhem + ghast + variants areas). Paths below: `vb:` = `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/`, `mc:` = `<mc-26.2>/net/minecraft/world/entity/`, `121:` = `<mc-1.21.1>/net/minecraft/world/entity/`, `addon:` = `src/main/java/com/coolfreeze23/vbparity/`.
Spear goal/behaviour logic (SpearUseGoal, SpearApproach/Attack/Retreat, chargeSpeedModifier consumers) is left to the spear audit.

| mechanic | VB/add-on location | native location | result |
|---|---|---|---|
| Nautilus attributes (HP 15, speed 1.0, dmg 3, KB res) | vb: nautilus/AbstractNautilus.java:155-161 | mc: animal/nautilus/AbstractNautilus.java:111-117 | AI-MNT-11 (0.3 vs 0.3F); TEMPT_RANGE absent in 1.21.1 but fixed 10 = native default 10: match |
| Zombie nautilus attributes (speed 1.1F) | vb: ZombieNautilus.java:41-43 | mc: ZombieNautilus.java:51-53 | match |
| Camel husk attributes (= Camel) | vb: integrations/MobIntegrations.java:65 | mc: ai/attributes/DefaultAttributes.java:107 | match |
| Parched attributes (HP 16, skeleton base) | vb: skeleton/Parched.java:36-38 | mc: monster/skeleton/Parched.java:100-102 | match |
| Zombie horse attributes (base horse + HP 25), speed/jump randomisation and draw order (3 jump doubles, then 3 speed doubles) | vb: zombie_horses/ZombieHorseMixin.java:49-80 + addon: mayhem/mixin/ZombieHorseMixin.java:29-32 | mc: equine/ZombieHorse.java:53-88 | match (MAYHEM-27 fix verified bit-exact divisor) |
| Nautilus move/look control (SmoothSwimming 85,10,0.011,0,true; look 10), water malus 0, WaterBoundPathNavigation, walk target value 0, not pushed by fluid | vb: AbstractNautilus.java:105-111,163-173 | mc: AbstractNautilus.java:89-95,119-132 | match (navigation waypoint tolerance: AI-MNT-D5) |
| Nautilus in-water travel (moveRelative(speed), move, scale 0.9) | vb: AbstractNautilus.java:240-249 | mc: AbstractNautilus.java:218-224 | AI-MNT-08 |
| Nautilus brain memories | vb: NautilusAi.java:56-72 | mc: Nautilus.java:30-34 + ai/Brain.java:358-378 (behaviour memories auto-registered) | AI-MNT-01 |
| Nautilus sensors (nearest living, nearest adult, nearest players, hurt by, nautilus temptations) | vb: NautilusAi.java:53-55 | mc: Nautilus.java:32 | match (ranges: AI-MNT-D3, start delay draw: AI-MNT-D4) |
| Nautilus CORE: AnimalPanic 1.6, LookAtTargetSink(45,90), MoveToTargetSink, 3 cooldown countdowns | vb: NautilusAi.java:92-104 | mc: NautilusAi.java:222-235 | match (AnimalPanic start: AI-MNT-D2) |
| Nautilus IDLE: MakeLove(0.4,2) p1, FollowTemptation(1.3, baby 2.5/3.5) p2, StartAttacking p3, Gate{swim stroll 1.0 w2, look-target walk 1.0/3 w3} p4 | vb: NautilusAi.java:107-126 | mc: NautilusAi.java:237-256 | order/values match; FollowTemptation can never start: AI-MNT-01 |
| Nautilus FIGHT: ChargeAttack(80, cond, 0.6, 2.0, 12, 11, dash) + 4 activity conditions | vb: NautilusAi.java:128-139 + addon ChargeAttackMixin | mc: NautilusAi.java:258-269 | match after MAYHEM-12 (verified: fields swapped at <init> TAIL) |
| Zombie nautilus CORE (no panic) / IDLE (tempt p1, attack p2, gate p3) / FIGHT | vb: ZombieNautilusAi.java:42-89 + addon ZombieNautilusAiMixin.java:13-29 | mc: ZombieNautilusAi.java:224-269 | match after MAYHEM-14/15 (speeds 0.5/0.9 verified); but tempt never starts: AI-MNT-01 |
| Attack-target finder: !breeding, in water, !baby, !tame; ANGRY_AT filter in water + attackable ignoring LOS; cooldown memory; then `UniformInt(2400,3600).sample` then `nextFloat() < 0.5` → closest `#nautilus_hostiles` in water | vb: NautilusAi.java:141-166 | mc: NautilusAi.java:271-292 | match (same 2 level-random draws, same order) |
| Anger on hurt (400 ticks, erase CANT_REACH) | vb: AbstractNautilus.java:478-485, NautilusAi.java:168-173 | mc: AbstractNautilus.java:452-460, NautilusAi.java:294-299 | match |
| Targeting conditions (mob griefing / armor stand, world border) | vb: NautilusAi.java:48-52 | mc: NautilusAi.java:208-212 | match |
| ChargeAttack: start conditions, canStillUse order (tame, charge dist, detect dist, LOS, cooldown), start (velocity = dir*speed, sound if still usable), tick (lookAt 360, set velocity, first colliding target, skip passenger), damage (mob_attack, ATTACK_DAMAGE, post-attack effects), knockback (speed/slowness ±0.25 per level, clamp(speed*attr,0.2,2)+boost, *force), charger velocity (0.6,1.0,0.6), stop (cooldown 80, erase target), default duration 60 (`nextInt(1)` draw) | vb: ai/behavior/ChargeAttack.java:37-140 + addon: mayhem/mixin/ChargeAttackMixin.java:24-38 | mc: ai/behavior/ChargeAttack.java:35-132, LivingEntity.java:2746-2760 | match after MAYHEM-12/13 |
| Nautilus taming: taming items (pufferfish, bucket) when wild adult, `random.nextInt(3)==0`, events 7/6, eating sound | vb: AbstractNautilus.java:143-145,418-422,462-472 | mc: AbstractNautilus.java:98-100,409-413,435-445 | match (sound via makeSound after MAYHEM-16) |
| Nautilus heal feeding (2*nutrition else 1), bucket food returns water bucket | vb: AbstractNautilus.java:147-153,424-430 | mc: AbstractNautilus.java:103-109,415-418, TamableAnimal.java:136-141 | match |
| Nautilus breeding / baby feeding (Animal.mobInteract: love 600, ageUp speed-up) | 121: animal/Animal.java:136-158 | mc: animal/Animal.java:139-163 | values match; eating sound: AI-MNT-D1 |
| Nautilus offspring (tame copies owner) | vb: Nautilus.java:37-45 | mc: Nautilus.java:51-59 | match |
| Nautilus food tags (nautilus_food, taming_items, bucket_food, fishes) | vb data tags | mc data tags | match |
| Nautilus riding: controlling passenger (saddled player), ridden input, rotation, ridden speed 0.0325/0.02 * speed, dash (jump scale `>=90 ? 1 : 0.4+0.4*j/90`, impulse 1.2 water / 0.5 land * amount * speed * block factor, cooldown 40, dash flag < 35), breath effect 60 ticks refresh every 40 | vb: AbstractNautilus.java:194-264,284-292,318-390 | mc: AbstractNautilus.java:169-240,261-373 | match (MAYHEM-16/17 verified) |
| Restriction (home) radius 32 / 16 (+8 buffer) when tame, unleashed, unridden | vb: AbstractNautilus.java:266-282 | mc: AbstractNautilus.java:242-259 | match |
| Air supply (300, -1 per tick out of water, 2 dry-out dmg at -20, server+AI only) | vb: Nautilus.java:111-127 + addon NautilusMixin.java:32-38 | mc: Nautilus.java:126-150 | match after MAYHEM-21 |
| Poison immunity, removeWhenFarAway true, tame → persistent | vb: AbstractNautilus.java:474-489,576-578 | mc: AbstractNautilus.java:447-465,534-537 | match |
| Interaction sets persistence (native `interact` override) | vb: AbstractNautilus.java:409, CamelHusk.java:41, ZombieHorseMixin.java:87 (all in mobInteract) | mc: AbstractNautilus.java:394-397, CamelHusk.java:40-43, ZombieHorse.java:58-61 | AI-MNT-03 |
| Leashed mob persistence | 121: Mob.java:736-738 | mc: Mob.java:693-695 | AI-MNT-D6 |
| Body armour slot (tame adult), shearing armour then saddle | addon: mayhem/mixin/AbstractNautilusMixin.java:104-145 | mc: AbstractNautilus.java:155-157, Mob shear code | match after MAYHEM-19/20 |
| Dispenser equips nautilus armour | 121: item/ArmorItem.java:41-63, Mob.java:1233-1236 | mc: AbstractNautilus.java:160-162, LivingEntity.java:3875-3889 | AI-MNT-07 |
| Leash rules (nautilus !aggravated; zombie nautilus also !mob-controlled) | vb: Nautilus.java:129-131, ZombieNautilus.java:141-143 | mc: Nautilus.java:152-155, ZombieNautilus.java:179-182 | match |
| Zombie nautilus sun burn (BODY slot protection, `nextInt(2)` durability, 8 s fire) | vb: ZombieNautilus.java:148-166 | mc: Mob.java:464-467,489-523 | logic match; roll position in tick: AI-MNT-06 (server-side no extra draws between, so effective match for zombie nautilus) |
| Zombie nautilus cannot breed / be a baby | vb: ZombieNautilus.java:45-48,145-146 | mc: ZombieNautilus.java:55-58,184-187 | AI-MNT-09 |
| Zombie nautilus variant pick (no extra spawn-chance draw) | vb: ZombieNautilus.java:131-139 + addon: variants/mixin/VariantUtilsMixin.java:18-24 | mc: ZombieNautilus.java:171-177 | match (VARIANTS-20 fix applies) |
| Zombie nautilus jockey (drowned): reason NATURAL/STRUCTURE → trident → `nextFloat() < 0.5` → !baby → biome; structure → persistent; finalizeSpawn(reason, null); ride | addon: ghast/mixin/DrownedJockeyMixin.java:36-63 | mc: monster/zombie/Drowned.java:107-135 | match after GHAST-42 (order verified, after the 0.03 shell roll) |
| Drowned rider body rotation | none | mc: monster/zombie/Drowned.java:311-316 | AI-MNT-10 |
| Camel husk brain (= Camel: Swim 0.8, CamelPanic 4.0 + !mob-controlled, idle tree) | 121: animal/camel/CamelAi.java + vb: controllable_mounts/CamelPanicMixin.java:16-18 | mc: animal/camel/CamelAi.java:48-110 | match |
| Camel husk temptation sensor | 121: animal/camel/CamelAi.java:45-47,132-134 | mc: animal/camel/Camel.java:76-78, ai/sensing/SensorType.java:37 | AI-MNT-02 |
| Camel husk food / no breeding / no love / removeWhenFarAway / leash (!mob-controlled) / mob-controlled flag | vb: CamelHusk.java:31-71 | mc: CamelHusk.java:29-79 | match (baby via NBT: AI-MNT-09) |
| Camel husk riding/dash/sitting (Camel: dash cooldown 55, 22.2222*amount impulse, sit/stand pose times 40/52, RandomSitting 20 s, refuseToMove) | 121: animal/camel/Camel.java | mc: animal/camel/Camel.java | match (only sound getters and leash pull hook differ; leash pull is GHAST-37, sounds first-round) |
| Camel husk sun / targeting | none | none (not in `#burn_in_daylight`, no target AI) | match |
| Camel husk jockey (husk): pick-up-loot roll, HuskGroupData per husk, no-collision AABB, `nextFloat() < 0.1`, spear, camel + parched finalizeSpawn order, riding | addon: ghast/mixin/HuskJockeyMixin.java:28-32, ZombieJockeyMixin.java:41-86 | mc: monster/zombie/Husk.java:91-131 | match after GHAST-43 |
| Zombie horse goals: RunAroundLikeCrazy 1.2 (mob-controlled gate), Breed, FollowParent, Stroll 0.7, LookAtPlayer 6, RandomLook, RandomStand, Float 0, Tempt 1.25 red mushroom; no panic | 121: horse/AbstractHorse.java:162-180 + addon: mayhem/mixin/AbstractHorseMixin.java:29-35 + vb: MobIntegrations.java:78-81 (EntityJoinLevelEvent, appended after registerGoals) + RunAroundLikeCrazyGoalMixin | mc: equine/AbstractHorse.java:136-154, ZombieHorse.java:126-130 | match (same insertion order; MAYHEM-24 verified) |
| Zombie horse taming (riding temper `nextInt(maxTemper) < temper`), makeMad on non-food item, red mushroom heal 3 / temper 3 / no age-up, no love | vb: ZombieHorseMixin.java:82-112, zombie_horses/AbstractHorseMixin.java:208-238 | mc: ZombieHorse.java:121-185, equine/AbstractHorse.java:428-496 | match |
| Zombie horse never ages up | addon: mayhem/mixin/AgeableMobMixin.java:12-18 | mc: ZombieHorse.java:207-210, AgeableMob.java:207-213 | match after MAYHEM-25 |
| Zombie horse leash / slots / armour body type | vb: ZombieHorseMixin.java:106-139 | mc: ZombieHorse.java:172-195 | match |
| Zombie horse sun burn | vb: ZombieHorseMixin.java:166-183 | mc: Mob.java:464-467,493-510 | AI-MNT-06 |
| Zombie horse natural jockey (zombie, NATURAL only, iron spear) | vb: ZombieHorseMixin.java:151-164 (from Mob.finalizeSpawn RETURN) | mc: ZombieHorse.java:132-148 | AI-MNT-05 |
| Panic damage types (nautilus AnimalPanic, camel CamelPanic, every PanicGoal) | 121 data `#panic_causes` + addon spear damage type | mc data `#panic_causes` (`#is_player_attack`) | AI-MNT-04 |
| Parched goals (AbstractSkeleton list), bow interval 50 hard / 70 otherwise, bow goal (1.0, 20, 15), melee 1.2 | vb: Parched.java:57-63; 121: monster/AbstractSkeleton.java:49-81,156-181 | mc: skeleton/Parched.java:124-132, skeleton/AbstractSkeleton.java:56-92,138-163 | match |
| Parched arrow (Weakness 600 on plain arrows), Weakness immunity | vb: Parched.java:27-34 + addon ParchedMixin.java:14-19 | mc: Parched.java:90-98,134-137 | match after MAYHEM-30 |
| Parched sun (never burns; no `isSunBurnTick` draw) | vb: Parched.java:23-25 | mc: Mob.java:465 (not in `#burn_in_daylight`) | match (no RNG draw either side) |
| Parched finalizeSpawn (bow, enchant, `0.55*special` pickup roll, Halloween) | 121: AbstractSkeleton.java:134-154 | mc: AbstractSkeleton.java:119-136 | match |
| Breeding/age (no panic on spawn-egg etc.), CamelHusk HuskGroupData | vb: CamelHusk.java:123-129 | mc: Husk.java:138-144 | match |

### AI-MNT-01  Nautilus and zombie nautilus never follow a tempting player (IS_TEMPTED memory not registered)
- severity: gameplay
- vb: common/level/entities/mob/animal/nautilus/NautilusAi.java:56-72 (MEMORY_TYPES) — `ImmutableList.of(IS_PANICKING, HURT_BY, HURT_BY_ENTITY, NEAREST_LIVING_ENTITIES, NEAREST_VISIBLE_LIVING_ENTITIES, WALK_TARGET, LOOK_TARGET, CANT_REACH_WALK_TARGET_SINCE, PATH, ANGRY_AT, BREED_TARGET, ATTACK_TARGET, new MemoryModuleType[]{TEMPTATION_COOLDOWN_TICKS, CHARGE_COOLDOWN_TICKS, ATTACK_TARGET_COOLDOWN})`; :75 `return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);`; ZombieNautilusAi.java:29 reuses it. The 1.21.1 behaviour needs it registered: src-121 .../ai/behavior/FollowTemptation.java `builder.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED);` and 121: ai/Brain.java:225-231 `checkMemory`: `return optional == null ? false : ...` (only listed memories and sensor `requires()` are registered, Brain.java:134-146).
- native: animal/nautilus/Nautilus.java:30-34 `Brain.provider(List.of(ANGRY_AT, ATTACK_TARGET_COOLDOWN), List.of(...sensors...), var0 -> NautilusAi.getActivities())`; ai/Brain.java:375-377 `for (MemoryModuleType<?> requiredMemory : behavior.getRequiredMemories()) { this.registerMemory(requiredMemory); }` — every behaviour's memories, IS_TEMPTED included, are registered.
- difference: in VB `FollowTemptation.hasRequiredMemories` is always false, so neither mob ever swims after a player holding fish / fish buckets (native: speed 1.3 nautilus, 0.9 zombie nautilus, stop at 2.5/3.5). The TEMPTING_PLAYER sensor still works, so the FIGHT activity is still suppressed while tempted. Native also spends one `level.getRandom().nextInt(1)` (Behavior.tryStart duration) each time the tempt starts; VB never does. The MAYHEM-15 fix (0.9 speed) currently has no effect.
- fix_kind: mixin-vb (NautilusAi.brainProvider, ZombieNautilusAi.brainProvider)
- fix: `@ModifyArg(method="brainProvider", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ai/Brain;provider(Ljava/util/Collection;Ljava/util/Collection;)Lnet/minecraft/world/entity/ai/Brain$Provider;"), index=0)` in both classes (`remap=false` on the class): `return ImmutableList.builder().addAll(list).add(MemoryModuleType.IS_TEMPTED).build();`.
- config_gate: has_nautilus

### AI-MNT-02  Camel husks are tempted by cactus instead of rabbit feet
- severity: gameplay
- vb: CamelHusk does not override `brainProvider`, so it uses 1.21.1 Camel's: src-121 .../animal/camel/CamelAi.java:45-47 `SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.CAMEL_TEMPTATIONS, SensorType.NEAREST_ADULT` with :132-134 `return p_335266_ -> p_335266_.is(ItemTags.CAMEL_FOOD);` (1.21.1 `camel_food` = `["minecraft:cactus"]`). VB CamelHusk.java:49-51 `isFood → ModItemTags.CAMEL_HUSK_FOOD` is only used for feeding.
- native: animal/camel/Camel.java:76-78 `Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT), ...)`; ai/sensing/SensorType.java:37 `FOOD_TEMPTATIONS = register("food_temptations", TemptingSensor::forAnimal)`; TemptingSensor.java:29-31 `m instanceof Animal animal ? animal.isFood(i)`; CamelHusk.java:51-53 `itemStack.is(ItemTags.CAMEL_HUSK_FOOD)` = `["minecraft:rabbit_foot"]`.
- difference: native camel husks follow (FollowTemptation 2.5) a player holding a rabbit foot and ignore cactus; VB camel husks follow cactus and ignore rabbit feet.
- fix_kind: mixin-mc (TemptingSensor)
- fix: `@Mixin(TemptingSensor.class)`: `@Unique static final ThreadLocal<PathfinderMob> vbp$body`; `@Inject(method="doTick", at=@At("HEAD"))` set it, `@Inject(method="doTick", at=@At("RETURN"))` clear it; `@WrapOperation(method="isTemptation", at=@At(value="INVOKE", target="Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))` → `vbp$body.get() instanceof CamelHusk husk ? husk.isFood((ItemStack)o) : original.call(p, o)`. (Coexists with the ghast TemptingSensorMixin, which cancels only for HappyGhast.)
- config_gate: none (entity exists with has_camel_husks)

### AI-MNT-03  Interacting (lead, name tag, spawn egg, VB lead-chain) doesn't make these mobs persistent
- severity: gameplay
- vb: AbstractNautilus.java:409, CamelHusk.java:41, zombie_horses/ZombieHorseMixin.java:87 — `this.setPersistenceRequired();` as the first line of `mobInteract`. 1.21.1 Mob.interact (121: Mob.java:1243-1266) handles name tags / spawn eggs (`checkAndHandleImportantInteractions`) and Entity.interact handles leads (121: Entity.java:2014-2037) before `mobInteract` runs; VB's `LeashInteraction.onInteract` (common/integrations/interactions/LeashInteraction.java) also returns before `mobInteract`. The add-on's nautilus shears hook (mayhem/mixin/AbstractNautilusMixin.java:111-145) is a HEAD inject on `mobInteract`, also before VB's line 409.
- native: AbstractNautilus.java:394-397, CamelHusk.java:40-43, ZombieHorse.java:58-61 — `public InteractionResult interact(...) { this.setPersistenceRequired(); return super.interact(player, hand, location); }`
- difference: all four mobs have `removeWhenFarAway → true`. Natively any right-click makes them permanent; in VB putting a lead on a wild camel husk, zombie horse or nautilus, tying it to a fence, using a spawn egg on it, or shearing it leaves it despawnable (instantly beyond 128 blocks, 1/800 per tick beyond 32 for MONSTER; WATER_CREATURE 64/32). With AI-MNT-D6 a fence-tied camel husk or zombie horse despawns as soon as the player walks away.
- fix_kind: mixin-mc (Mob.interact)
- fix: `@Inject(method="interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at=@At("HEAD"))` on `Mob`: `if ((Object)this instanceof AbstractNautilus || (Object)this instanceof CamelHusk || (Object)this instanceof ZombieHorse) this.setPersistenceRequired();`. VB's own `mobInteract` call becomes redundant but harmless. Also call it at the head of `LeashInteraction.onInteract` for those types (it runs from the Platform interaction hook, which may fire before `Mob.interact`).
- config_gate: none / has_zombie_horses for the zombie horse branch

### AI-MNT-04  Spear hits (add-on's `minecraft:spear` damage) no longer cause panic
- severity: gameplay
- vb: addon resources `data/minecraft/tags/damage_type/is_player_attack.json` `{"replace":false,"values":["minecraft:spear"]}` (MAYHEM-02), but the effective 1.21.1 `#panic_causes` is `[#panic_environmental_causes, arrow, ..., mob_attack, mob_projectile, player_attack, ...]` with no `#is_player_attack` and no `spear`. 121: ai/behavior/AnimalPanic.java (default `DamageTypeTags.PANIC_CAUSES`) and ai/goal/PanicGoal.java:61-62 `getLastDamageSource().is(this.panicCausingDamageTypes...)`.
- native: data/minecraft/tags/damage_type/panic_causes.json ends with `"#minecraft:is_player_attack"`; is_player_attack.json = `["minecraft:player_attack","minecraft:spear","minecraft:mace_smash"]`.
- difference: a nautilus (AnimalPanic 1.6) or camel husk (CamelPanic 4.0) stabbed with a spear, by a player or by a mob, panics natively and does not in VB+add-on. The same applies to every vanilla PanicGoal/AnimalPanic animal. Before MAYHEM-02 these hits were `player_attack`/`mob_attack` and did panic, so this is a regression from the fix.
- fix_kind: data-override (data/minecraft/tags/damage_type/panic_causes.json)
- fix: `{"replace":false,"values":["#minecraft:is_player_attack"]}` (effective members in 1.21.1: player_attack, spear).
- config_gate: has_spears

### AI-MNT-05  Natural zombie-horse jockey: the zombie is rolled after the horse instead of before
- severity: gameplay (RNG order)
- vb: core/mixin/common/zombie_horses/ZombieHorseMixin.java:151-163 `vb$finalizeSpawn` (called from extension/entity/MobMixin.java:42-55 at `Mob.finalizeSpawn` RETURN): `Zombie zombie = EntityType.ZOMBIE.create(this.level()); ... zombie.finalizeSpawn(level, difficulty, reason, null); ... zombie.setItemSlot(MAINHAND, IRON_SPEAR); zombie.startRiding(this, false);`. By then 1.21.1 AbstractHorse.finalizeSpawn (121: horse/AbstractHorse.java:1144-1151) has run `randomizeAttributes(level.getRandom())` (6× `nextDouble`) and Mob.finalizeSpawn (121: Mob.java:1192-1201) `randomsource.triangle(0.0, 0.11485)` + `randomsource.nextFloat() < 0.05F`.
- native: animal/equine/ZombieHorse.java:137-147 — `if (spawnReason == NATURAL) { Zombie zombie = EntityTypes.ZOMBIE.create(...); zombie.snapTo(...); zombie.finalizeSpawn(level, difficulty, spawnReason, null); zombie.setItemSlot(MAINHAND, IRON_SPEAR); zombie.startRiding(this, false, false); } return super.finalizeSpawn(...)` — every zombie draw (pickup, baby, chicken, doors, equipment, enchant, Halloween, attributes) comes first, then the horse's 6 attribute draws, baby-group draw and follow-range/left-hand draws.
- difference: the same set of level-random draws in a different order (zombie block ↔ horse block), so a seeded spawn gives different zombie gear/baby state and horse speed/jump than native. Distributions are unchanged.
- fix_kind: mixin-vb (ZombieHorseMixin.vb$finalizeSpawn) + mixin-mc (AbstractHorse.finalizeSpawn)
- fix: `@Mixin(value=ZombieHorse.class, priority=1500)` `@Inject(method="vb$finalizeSpawn", at=@At("HEAD"), cancellable=true, remap=false)` → `cir.setReturnValue(data)`. Then `@Mixin(AbstractHorse.class) @Inject(method="finalizeSpawn", at=@At("HEAD"))`: `if ((Object)this instanceof ZombieHorse && reason == MobSpawnType.NATURAL)` run VB's body (create, moveTo, finalizeSpawn(reason), spear or sword per has_spears, `startRiding(this, false)`).
- config_gate: has_zombie_horses (has_spears for the held item)

### AI-MNT-06  Zombie horse sun-burn roll runs after the horse's own aiStep rolls
- severity: gameplay (RNG order)
- vb: zombie_horses/ZombieHorseMixin.java:166-183 — `super.aiStep(); if (this.isAlive() && this.isSunBurnTick()) { ... armor.setDamageValue(... + this.random.nextInt(2)) ... }`. `super.aiStep()` is 1.21.1 AbstractHorse.aiStep (121: horse/AbstractHorse.java:597-624): `random.nextInt(200)` (tail), Mob/LivingEntity aiStep, then `random.nextInt(900)` (heal) and `random.nextInt(300)` (eat grass). The burn check (121: Mob.java:1524-1535 `this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F`) therefore comes last. Same structure in ZombieNautilus.java:148-166.
- native: Mob.java:462-467 `super.aiStep(); if (this.is(EntityTypeTags.BURN_IN_DAYLIGHT)) { this.burnUndead(); }` — burn (Mob.java:493-523) runs inside Mob.aiStep, i.e. before AbstractHorse's `nextInt(900)`/`nextInt(300)` (equine/AbstractHorse.java:538-565).
- difference: per daylight tick the zombie horse's entity-random order is [200, ..., burn float, (nextInt 2), 900, 300] natively and [200, ..., 900, 300, burn float, (nextInt 2)] in VB. For the zombie nautilus nothing draws server-side between the two points (AgeableMob/Animal aiStep), so it is effectively equal.
- fix_kind: mixin-vb (ZombieHorseMixin.aiStep) + mixin-mc (Mob.aiStep)
- fix: `@WrapWithCondition` on VB's `isSunBurnTick()`-guarded block (or `@Inject(method="aiStep", at=@At("HEAD"), cancellable=true)` re-implementing `super.aiStep()` only), and `@Inject(method="aiStep", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;aiStep()V", shift=At.Shift.AFTER))` on `Mob`: if `this instanceof ZombieHorse` run the BODY-slot burn block there.
- config_gate: has_zombie_horses

### AI-MNT-07  Dispensers can't put nautilus armour on a tamed nautilus
- severity: gameplay
- vb: common/level/items/NautilusArmorItem.java `extends ArmorItem` (dispense behaviour = 1.21.1 `ArmorItem.dispenseArmor`, 121: item/ArmorItem.java:41-63, selector `MobCanWearArmorEntitySelector` → 121: Mob.java:1233-1235 `return this.getItemBySlot(equipmentslot).isEmpty() && this.canPickUpLoot();`). Nautiluses never `canPickUpLoot`.
- native: AbstractNautilus.java:160-162 `canDispenserEquipIntoSlot(slot) { return slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE || super... }`; LivingEntity.java:3875-3882 `canEquipWithDispenser: this.canUseSlot(slot) && equippable.canBeEquippedBy(...) ? this.getItemBySlot(slot).isEmpty() && this.canDispenserEquipIntoSlot(slot)` (canUseSlot = alive, adult, tame).
- difference: a dispenser facing a tamed adult nautilus or zombie nautilus equips nautilus armour natively; in VB it just drops the item. (Saddle dispensing matches: 1.21.1 saddle behaviour uses `isSaddleable`.)
- fix_kind: mixin-mc (ArmorItem.dispenseArmor)
- fix: `@Inject(method="dispenseArmor", at=@At("HEAD"), cancellable=true)`: if `armorItem.getItem() instanceof NautilusArmorItem`, find `AbstractNautilus` in `new AABB(pos.relative(facing))` with `isAlive() && !isSpectator() && canUseSlot(BODY) && getBodyArmorItem().isEmpty()`; `setBodyArmorItem(armorItem.split(1))`, `setDropChance(BODY, 2.0F)`, `setPersistenceRequired()`, return true.
- config_gate: has_nautilus

### AI-MNT-08  Nautilus water travel ignores local control and never updates the walk animation
- severity: visual
- vb: AbstractNautilus.java:240-249 — `public void travel(Vec3 input) { if (this.isAlive() && this.isInWater()) { this.moveRelative(speed, input); this.move(MoverType.SELF, this.getDeltaMovement()); this.setDeltaMovement(this.getDeltaMovement().scale(0.9)); } else { super.travel(input); } }`. 1.21.1 LivingEntity.travel (121: LivingEntity.java:2219-2346) gates all physics with `if (this.isControlledByLocalInstance())` and ends with `this.calculateEntityAnimation(this instanceof FlyingAnimal);`; aiStep calls `travel` on both sides (121: LivingEntity.java:2805-2821).
- native: AbstractNautilus.java:218-224 overrides only `travelInWater`; LivingEntity.java:3137-3149 calls `travel` only when `canSimulateMovement() && isEffectiveAi()` and `calculateEntityAnimation(...)` every client tick.
- difference: in VB the client also runs move physics for nautiluses it doesn't control (extra motion between position updates → jitter), and no side updates `walkAnimation` while the nautilus is in water, so the swim animation speed stays at its last land value.
- fix_kind: mixin-vb (AbstractNautilus.travel)
- fix: `@ModifyExpressionValue(method="travel", at=@At(value="INVOKE", target="...AbstractNautilus;isInWater()Z"))` → `original && this.isControlledByLocalInstance()` (non-local instances fall through to `super.travel`, which only animates), and `@Inject(method="travel", at=@At(value="INVOKE", target="...AbstractNautilus;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", shift=At.Shift.AFTER))` → `this.calculateEntityAnimation(false);`.
- config_gate: has_nautilus

### AI-MNT-09  Camel husks and zombie nautiluses can still be babies (Age NBT)
- severity: gameplay
- vb: CamelHusk.java:70-71 and ZombieNautilus.java:145-146 — `public void setBaby(boolean baby) { }` only; 1.21.1 AgeableMob still reads/writes `Age` and `isBaby()` = `getAge() < 0`, and AgeableMob.finalizeSpawn (121: AgeableMob.java:34-38) can `setAge(-24000)`.
- native: CamelHusk.java:76-79 / ZombieNautilus.java:184-187 `canBeABaby() { return false; }`; AgeableMob.java:166-185 skips `Age` load/save, :250-252 `isBaby() { return this.canBeABaby() && this.getAge() < 0; }`.
- difference: `/summon minecraft:camel_husk ~ ~ ~ {Age:-24000}` (or any structure/NBT with negative Age) gives a baby camel husk / zombie nautilus in VB that grows up after 20 min; natively it is an adult. Natural and jockey spawns pass null group data, so no baby roll happens there.
- fix_kind: mixin-mc (AgeableMob.isBaby / readAdditionalSaveData)
- fix: `@Inject(method="isBaby", at=@At("HEAD"), cancellable=true)` on `AgeableMob`: `if (this instanceof CamelHusk || this instanceof ZombieNautilus) cir.setReturnValue(false);` and `@WrapWithCondition` the `setAge(I)V` call in `readAdditionalSaveData` with the same check.
- config_gate: none

### AI-MNT-10  Drowned riding a zombie nautilus don't align body rotation
- severity: visual
- vb: no `rideTick` in 1.21.1 Drowned or Zombie (only 121: monster/AbstractSkeleton.java:121-126 has one); VB controllable_mounts/DrownedMixin.java adds none.
- native: monster/zombie/Drowned.java:311-316 — `public void rideTick() { super.rideTick(); if (this.getControlledVehicle() instanceof PathfinderMob entity) { this.yBodyRot = entity.yBodyRot; } }`
- difference: native drowned jockeys face where the nautilus faces; VB drowned keep their own body yaw while riding.
- fix_kind: mixin-mc (Drowned)
- fix: `@Mixin(Drowned.class)` add `@Override public void rideTick() { super.rideTick(); if (this.getControlledVehicle() instanceof PathfinderMob p) this.yBodyRot = p.yBodyRot; }` (mixin class extends Zombie).
- config_gate: has_nautilus

### AI-MNT-11  Nautilus knockback resistance is the double 0.3, not the float 0.3F
- severity: cosmetic
- vb: AbstractNautilus.java:160 — `.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);`
- native: AbstractNautilus.java:116 — `.add(Attributes.KNOCKBACK_RESISTANCE, 0.3F);` (widened to 0.30000001192092896)
- difference: knockback multiplier 0.7 vs 0.69999998807907; ~1.7e-8 relative, not observable, but not bit-exact (same class of issue as MAYHEM-27).
- fix_kind: mixin-vb (AbstractNautilus.createAttributes)
- fix: `@ModifyConstant(method="createAttributes", constant=@Constant(doubleValue=0.3), remap=false)` → `(double)0.3F`.
- config_gate: has_nautilus

### Engine drift (Mounts of Mayhem mobs)

### AI-MNT-D1  Feeding to breed or grow up plays no eating sound
- severity: audio
- vb: 121: animal/Animal.java:136-158 — `this.usePlayerItem(...); this.setInLove(player); return InteractionResult.SUCCESS;` / `this.ageUp(...); return ...` (no sound). Nautilus reaches it via AbstractNautilus.java:411/454 `super.mobInteract`.
- native: animal/Animal.java:146-156 — `this.setInLove(serverPlayer); this.playEatingSound();` and `this.ageUp(...); this.playEatingSound();`
- difference: breeding/age-up feeding of nautiluses is silent in VB; native plays `entity.(baby_)nautilus.eat` (with makeSound voice-pitch draws from the entity random).
- fix_kind: mixin-mc (Animal.mobInteract), scoped
- fix: `@Inject(method="mobInteract", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/animal/Animal;setInLove(Lnet/minecraft/world/entity/player/Player;)V", shift=AFTER))` and after the `ageUp(IZ)V` call: `if (this instanceof AbstractNautilus) ((AnimalInvoker)this).callPlayEatingSound();`.
- config_gate: has_nautilus

### AI-MNT-D2  AnimalPanic doesn't stop the current path when it starts
- severity: gameplay
- vb: 121: ai/behavior/AnimalPanic.java:56-59 — `setMemory(IS_PANICKING, true); eraseMemory(WALK_TARGET);`
- native: ai/behavior/AnimalPanic.java:69-73 — `... eraseMemory(WALK_TARGET); body.getNavigation().stop();`
- difference: a hurt nautilus or camel husk keeps following its old path for the tick(s) until MoveToTargetSink notices the erased walk target; natively it stops at once and the next panic position is picked (`navigation.isDone()`) on the next tick.
- fix_kind: mixin-mc (AnimalPanic.start), scoped
- fix: `@Inject(method="start", at=@At("TAIL"))`: `if (entity instanceof AbstractNautilus || entity instanceof CamelHusk) entity.getNavigation().stop();` (or unscoped, which is native for all animals).
- config_gate: none

### AI-MNT-D3  Brain sensor ranges fixed at 16 instead of the follow range
- severity: gameplay
- vb: 121: ai/sensing/NearestLivingEntitySensor.java:16-17 `inflate(this.radiusXZ() /*16*/, this.radiusY() /*16*/, ...)`; 121: ai/sensing/Sensor.java:14-26 all `TargetingConditions ... .range(16.0)`; PlayerSensor `closerThan(p, 16.0)`.
- native: NearestLivingEntitySensor.java:17-19 `double followRange = body.getAttributeValue(Attributes.FOLLOW_RANGE); ... inflate(followRange, ...)`; Sensor.java:47,52-60 `updateTargetingConditionRanges(body)` sets every condition's range to FOLLOW_RANGE; PlayerSensor `getFollowDistance(body)`.
- difference: FOLLOW_RANGE is 16 × (1 + triangle(0, 0.11485)) from Mob.finalizeSpawn, i.e. 16–17.84. Native nautiluses see pufferfish / become angry (`isEntityAttackableIgnoringLineOfSight`) up to that range; VB stops at 16. Same for camel husk sensors.
- fix_kind: mixin-mc (NearestLivingEntitySensor.doTick / Sensor), scoped
- fix: `@ModifyArg` the three `inflate` args in `NearestLivingEntitySensor.doTick` → `body.getAttributeValue(FOLLOW_RANGE)` when body is AbstractNautilus/CamelHusk; for `Sensor.isEntityAttackableIgnoringLineOfSight`/`isEntityTargetable`, wrap the `TargetingConditions.test` with a copy `.range(followRange)` for those bodies.
- config_gate: none

### AI-MNT-D4  Sensor start delay doesn't draw from the entity random
- severity: gameplay (RNG order)
- vb: 121: ai/sensing/Sensor.java:30-33 — `this.timeToTick = (long)RANDOM.nextInt(scanRate);` (global thread-safe random)
- native: ai/Brain.java:84-86 — `newSensor.randomlyDelayStart(randomSource);` with `body.getRandom()` (Brain.java:496-498); Sensor.java:40-42 `this.timeToTick = randomSource.nextInt(this.scanRate);`
- difference: native consumes 5 `nextInt(20)` (nautilus) / 4 (camel husk) from the entity's own random at construction, shifting every later entity-random roll (bubbles, voice pitch, taming `nextInt(3)`, burn rolls); VB does not.
- fix_kind: engine (approximable with mixin-mc on Brain.<init>, scoped to AbstractNautilus/CamelHusk by drawing `entity.getRandom().nextInt(20)` per sensor in the 1.21.1 `makeBrain` call)
- fix: `@Inject(method="makeBrain", at=@At("RETURN"))` in AbstractNautilus subclasses / CamelHusk: for each sensor, `((SensorAccessor)sensor).setTimeToTick(this.random.nextInt(20))`.
- config_gate: none

### AI-MNT-D5  Water navigation waypoint vertical tolerance 1.0 instead of 0.5
- severity: gameplay
- vb: 121: ai/navigation/PathNavigation.java:271 — `boolean flag = d0 <= maxDistanceToWaypoint && d2 <= maxDistanceToWaypoint && d1 < 1.0D;`
- native: ai/navigation/PathNavigation.java:258-260 `yDistance < this.getMaxVerticalDistanceToWaypoint()`; WaterBoundPathNavigation.java:61-63 `return 0.5F;`
- difference: nautiluses advance to the next path node when within 1.0 vertically (native 0.5), so they cut vertical corners and reach targets at a different height.
- fix_kind: mixin-mc (PathNavigation.followThePath), scoped
- fix: `@ModifyConstant(method="followThePath", constant=@Constant(doubleValue=1.0D))` → `(Object)this instanceof WaterBoundPathNavigation && this.mob instanceof AbstractNautilus ? 0.5 : 1.0`.
- config_gate: has_nautilus

### AI-MNT-D6  Leashed mobs aren't exempt from despawning
- severity: gameplay
- vb: 121: Mob.java:736-738 — `public boolean requiresCustomPersistence() { return this.isPassenger(); }`
- native: Mob.java:693-695 — `return this.isPassenger() || this.isLeashed();`
- difference: a leashed wild camel husk, zombie horse or (untamed) nautilus/zombie nautilus despawns in VB like an unleashed one (all have `removeWhenFarAway → true`); natively it never despawns while leashed. Together with AI-MNT-03 this loses fence-tied mounts.
- fix_kind: mixin-vb/mixin-mc (requiresCustomPersistence), scoped
- fix: `@Inject(method="requiresCustomPersistence", at=@At("RETURN"), cancellable=true)` on `Mob`: `if (!cir.getReturnValue() && this.isLeashed() && (this instanceof AbstractNautilus || this instanceof CamelHusk || this instanceof ZombieHorse)) cir.setReturnValue(true);`
- config_gate: none

counts: farm=0 gameplay=8 visual=2 audio=0 data=0 cosmetic=1 drift=6 (drift: gameplay 5, audio 1)


## Sulfur cube

Trees: VB `src-vb/com/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/{AbstractCubeMob,SulfurCube}.java` (below `VB-ACM`, `VB-SC`), VB `common/level/entities/mob/animal/happy_ghast/TemptGoal.java` (`VB-TG`), add-on `src/main/java/com/coolfreeze23/vbparity/sulfur/mixin/*` (`addon:`), native `src-mc/net/minecraft/world/entity/monster/cubemob/{AbstractCubeMob,SulfurCube}.java` (`MC-ACM`, `MC-SC`), native `world/entity/ai/goal/TemptGoal.java` (`MC-TG`). First-round SULFUR-01..08 and 09/16/17 were re-verified against native; only 04 is incomplete (AI-SLF-01). The sulfur cube has no brain, no sensors, no target goals, no panic/avoid goals and no breeding (`canBreed()` false, `getBreedOffspring` only used by spawn eggs) in either tree.

| mechanic | VB/add-on location | native location | result |
|---|---|---|---|
| Goal list: Float(1) / Tempt(2) / SearchForItems(3) / RandomDirection(4) / KeepOnJumping(5), no target goals | VB-ACM:55-61, VB-SC:131-142, 230-232 | MC-ACM:60-66, MC-SC:121-131, 213-215 | match |
| FloatGoal: flags JUMP+MOVE, `setCanFloat(true)`, canUse water/lava + CubeMobMoveControl, every tick, `nextFloat() < 0.8F` → jump, wanted 1.2 | VB-ACM:310-336 | MC-ACM:383-412 | match (1 nextFloat per tick while in fluid) |
| RandomDirectionGoal: LOOK, canUse target==null && (onGround\|water\|lava\|levitation); `adjustedTickDelay(40 + nextInt(60))` then `nextInt(360)` (order: nextInt(60), nextInt(360)) | VB-ACM:411-437 | MC-ACM:488-516 | match (not every-tick → delay = ceil(x/2), 20..50 goal ticks) |
| KeepOnJumpingGoal: JUMP+MOVE, `!isPassenger()`, wanted 1.0 | VB-ACM:338-355 | MC-ACM:414-433 | match |
| TemptGoal.ForNonPathfinders: flags overridden to LOOK, speed 1.0, canScare false, stop distance 1.0; items = baby ? `#sulfur_cube_food` : `#sulfur_cube_swallowable`; calmDown `reducedTickDelay(100)`; range 8 | VB-SC:133-140 (`setTemptRange(8)`), 957-977; VB-TG:18,54-62,102-107,135-150 | MC-SC:123-129, 601-603 (`TEMPT_RANGE 8.0`), 1002-1022; MC-TG:16,52-61,103-109,133-148 | match (VB 10-range constant is overwritten by `.range(8)` each canUse; Sulfur override of navigateTowards uses no random) |
| SearchForItemsGoal: LOOK, `!isBaby && pickupTimer <= 0`, items in bbox.inflate(8) with `!hasPickUpDelay && isAlive && swallowable`, nearest by `distanceToSqr` (first strict `<` wins); tick lookAt(10,10) + setDirection(yRot, true) | VB-SC:925-955, WorldUtilities.java:85-98 | MC-SC:963-1000, ServerEntityGetter.java:73-86 | match |
| Goal selector cadence (`tickCount + id` odd → only every-tick goals), GoalSelector.tick start/stop order | src-121 Mob.java:773-812, GoalSelector.java:83-127 | src-mc Mob.java:725-763, GoalSelector.java:66-107 | match |
| Goals removed while a block is held, re-registered when emptied; running goals stopped first | VB-SC:404-411 (`forEach(WrappedGoal::stop)` + removeAllGoals) | MC-SC:372-378, GoalSelector.java:32-40 (removeAllGoals stops running goals) | match |
| CubeMobMoveControl: initial yRot `180*yRot/PI`, `rotlerp(.., 90)`, MOVE_TO → WAIT, onGround speed = mod × MOVEMENT_SPEED, `jumpDelay-- <= 0` → `getJumpDelay()` (`nextInt(20) + 10`), `/= 3` if aggressive, jump + jump sound (volume 0.4·size, pitch `((r-r)*0.2+1)*(tiny?1.4:0.8)`), else xxa=zza=speed=0; airborne keeps speed | VB-ACM:357-409, 144-146, 260-275 | MC-ACM:435-486, 154-156, 276-292 | match (random order per jump: nextInt(20), nextFloat, nextFloat) |
| SulfurCubeMoveControl: no tick at all while holding a block | VB-SC:912-923 | MC-SC:950-961 | match |
| SulfurCubeLookControl: holding → snap yRot to nearest 90° (`wrapDegrees90`) | VB-SC:895-910, Utilities.java:352-362 | MC-SC:932-948, Mth.java:242-253 | match |
| JumpControl / rotlerp / jumpFromGround (`setDeltaMovement(x, getJumpPower(), z)`), LivingEntity jump branch, noJumpDelay 10 | VB-ACM:250-254; src-121 LivingEntity.java:2773-2801, 2168-2178 | MC-ACM:263-268; src-mc LivingEntity.java:3100-3124, 2379-2389 | match (`!(d3 > d4)` ≡ native `isInShallowFluid`) |
| Attributes per size: MAX_HEALTH 4·size (4 / 8), MOVEMENT_SPEED `0.2F + 0.1F*size` (0.3 / 0.4), FOLLOW_RANGE 16, TEMPT_RANGE 8 | VB-ACM:72-82, VB-SC:636-638, 890-893 | MC-ACM:79-89, MC-SC:601-603, 917-920 | match |
| Archetype attribute modifiers, buoyant, explosion, contact damage, knockback modifiers, sound settings (applied on equipment change) | VB-SC:400-454, SulfurCubeArchetypes | MC-SC:367-421, data/minecraft/sulfur_cube_archetype/*.json | match (values per first-round; ids fixed by SULFUR-16); contact damage gated by `do_sulfur_cubes_damage` |
| Targeting: no target selector goals, not an `Enemy`, `isDealsDamage()` false → no player/iron golem attack | VB-SC:238-241, VB-ACM:211-232 | MC-SC:222-225, MC-ACM:220-243 | match (VB `dealDamage` pitch `/0.2F` is unreachable) |
| Contact damage (doPush + playerPush): one `amount().sample(random)` per contact entry per contact | VB-SC:545-554, 743-746 | MC-SC:522-530, 730-734 | match |
| Player push of a block-holding cube: 1.3 radius, `max(0,1-KR)`, scale 0.3 / 0.16 (riding), clamp 0.5, vertical 0.3 on ground, push sound threshold/cooldown·20 | VB-SC:754-780 (VB `getKnownSpeed` mixins) | MC-SC:742-768, Entity.java:577-584, ServerPlayer.java:2230-2233 | match |
| Knockback with a block: angle scale 1.6, vertical transfer 0.5, position rotation 0.8, `sqrt(damage)`, KR, ×0.4 horizontal, ×1.2 vertical, hit sound | VB-SC:782-884 | MC-SC:770-877 | AI-SLF-01 (extra-knockback scaling) |
| Default knockback direction (projectile delta / firework & potion position / source position) | VB-SC:280-302, src-121 LivingEntity.java:1241-1253 | src-mc LivingEntity.java:1298-1314, Projectile.java:383-387 | match |
| Immunity while holding a block (`#sulfur_cube_with_block_immune_to`, blocked knockback, returns true) | VB-SC:255-278 | MC-SC:244-265 | match (tag contents: first-round SULFUR-10) |
| Spear (piercing weapon) hit test on a primed cube | VB PiercingWeapon.java:62-73 | MC-SC:922-925, PiercingWeapon.java:61-72 | AI-SLF-02 |
| Fuse: priming by fire/burning arrow (`primeTime(false)`), explosion (`primeTime(true)`: `nextInt(fuse/4) + fuse/8`), redstone (`getBestOwnOrNeighbourSignal != 0` at `BlockPos.containing(position())`), flint/fire charge; setInvulnerable, MAX_FUSE sync, TNT_PRIMED sound, PRIME_FUSE event | VB-SC:255-266, 364-387, 487-503; WorldUtilities.java:52-72 | MC-SC:244-253, 327-353, 451-488; PrimedTnt.java:182-184; SignalGetter.java:71-74 | match (fuse 120 → 15..44; VB's max-over-directions own signal equals `ownSignal` for every vanilla source; gamerule vs config = SULFUR-15; sound fixed by SULFUR-05) |
| Fuse tick order (tickFuse, primeWhenPowered, super.tick), explode at fuse 0: dropLeash, dead=true, explosion at Y(0.0625), mobGriefing → TNT/NONE, portal calculator, death effects, discard | VB-SC:324-362 | MC-SC:285-325 | match |
| Ignite with fire charge in creative | VB-SC:495-499 | MC-SC:479-484 | AI-SLF-06 |
| Block swallowing by interaction: adult, not the same item, old block dropped, set BODY(CHEST) with drop-when-killed, absorb sound, ENTITY_INTERACT, held item consumed | VB-SC:513-520, 526-543 | MC-SC:464-471, 491-520 | match (native also re-runs collectEquipmentChanges immediately on a swap; VB does it at the next detectEquipmentUpdates in the same server tick: same end state) |
| Item pickup from the ground: Mob.aiStep looting (canPickUpLoot = no body item, mobGriefing, reach 1/0/1, no pickup delay), `canHoldItem` (empty, swallowable, not baby), `pickupTimer <= 0`, split(1), absorb sound, guaranteed drop, take | VB-SC:652-674; src-121 Mob.java:571-593 | MC-SC:620-655; src-mc Mob.java:469-487 | match |
| pickupTimer 100 after shearing, pickupTimer/pushSoundCooldown decrement in customServerAiStep | VB-SC:389-398, 640-646 | MC-SC:355-365, 605-613 | match |
| Shearing by player (shears, readyForShearing = has block) | VB-SC:504-512 | MC-SC:454-463 | match (trigger = SULFUR-14) |
| Shearing by dispenser | src-121 ShearsDispenseItemBehavior.java:54-65, IShearable.java:66-115 | src-mc ShearsDispenseItemBehavior.java:50-64 | AI-SLF-05, AI-SLF-D1 |
| Dispenser feeding (swallowable with no registered behaviour, TNT) | addon: DispenserBlockMixin.java, SulfurCubeDispensing.java | DispenserBlock.java:118-126, SulfurCubeBlockDispenseItemBehavior.java:13-29 | match (SULFUR-09 fix verified) |
| Dispenser equipment (armor, elytra, carved pumpkin) onto a cube | src-121 ArmorItem.java:41-63, EntitySelector.java:98-103, Mob.java:1233-1236 | MC-SC:541-549, 629-637; EquipmentDispenseItemBehavior.java:20-37; LivingEntity.java:3875-3885 | AI-SLF-04 |
| Baby feeding: slime ball, `ageUp(getSpeedUpSecondsWhenFeeding(-age), true)` (10% of remaining), eat sound, SUCCESS | VB-SC:475-486 | MC-SC:441-450, AgeableMob.java:125-143 | match (VB has no `canAgeUp()` age-lock gate: AI-SLF-D3; sound fixed by SULFUR-05) |
| Growth: baby start age -24000, +1/tick in AgeableMob.aiStep, ageBoundaryReached → adult `setSize(2, true)`; `setSize(1, true)` on an adult makes a baby | VB-SC:690-724 | MC-SC:676-711, AgeableMob.java:31, 245-247 | match |
| Spawn size: baby → 1, else 2, chosen after AgeableMob baby roll (`nextFloat() <= 0.05` for group members after the first), then Mob.finalizeSpawn (`triangle(0, 0.11485)`, `nextFloat() < 0.05` left-handed) | addon: AbstractCubeMobMixin.java:88-104; VB-SC:685-688 | MC-ACM:311-317, MC-SC:667-674; src-mc AgeableMob.java:48-64, Mob.java:1098-1110 | match (SULFUR-03 fix verified, RNG order equal) |
| Splitting on death: size > 1, dead, not client; count 2 (0 if primed); offsets `(i%2-0.5)·w/2`, `(i/2-0.5)·w/2` (int); per child `nextFloat()*360` yaw; children size 1, baby; inherited state | VB-ACM:173-209, VB-SC:680-702; addon: AbstractCubeMobMixin.java:39-85 | MC-ACM:186-218, MC-SC:662-688; ConversionType.java SPLIT_ON_DEATH + convertCommon | match (SULFUR-01/02 fixes verified; copied age/forcedAge are overwritten by setBaby, forcedAge only affects adult age countdown, which a sulfur cube never uses) |
| Landing squish: `width*2*16` iterations × (nextFloat, nextFloat), then squish sound (nextFloat, nextFloat); `nextInt(20)` splash on size change in water | VB-ACM:111-138, 156-167 | MC-ACM:120-148, 168-179 | match (draws happen on the server too; same count) |
| Experience: baby 0, adult `1 + nextInt(2)` | VB-SC:676-678 | MC-SC:657-660 | match |
| Bucket pickup: empty bucket, alive, adult (babies fall through to Mob.mobInteract), sound 1/1, save NoAI/Silent/NoGravity/Glowing/Invulnerable/Health + age + block | VB-SC:177-190, 521-522, 556-582 | MC-SC:169-177, 473, 536-539; Bucketable.java:32-102 | AI-SLF-03 |
| Bucket release: BUCKET spawn reason → finalizeSpawn, then load tag, then `setFromBucket(true)` | SulfurCubeBucketItem.java:92-98, addon: SulfurCubeMixin.java (silent block) | MobBucketItem.java:46-52 | match |
| Despawn / persistence: `requiresCustomPersistence = super \|\| hasBodyItem \|\| fromBucket`; named cubes not persistent | VB-SC:247-249; addon: SulfurCubeMixin.java (SULFUR-08) | MC-SC:233-236; src-mc Mob.java:693-720 | match (native `super` adds `isLeashed()`, but a cube can only be leashed while holding a block, so it is already persistent) |
| Leash rule: only while holding a block | VB-SC:251-253 | MC-SC:238-241 | match |
| Panic / avoid / breeding food / love mode | none | none | match (not present) |
| Fluid buoyancy for buoyant archetypes (`0.2*sin(tick*0.4)`, threshold `bbHeight*0.2`, `min(1, immersion)*0.04`) | VB-SC:196-209, core/mixin/common/attributes/LivingEntityMixin.java:40-55 | MC-SC:184-199 | match (same condition and point in travel) |
| Bounce restitution / friction / air drag (physics) | addon: EntityBounceMixin.java; VB attributes mixins | Entity.restituteMovementAfterCollisions, LivingEntity.getAirDrag | not re-audited here (physics axis; first-round SULFUR-13 fixed). Every archetype has an air_drag modifier ≠ 0, so VB's omnidirectional vertical drag branch is always taken while holding a block |
| Trig table precision (`Mth.sin/cos`) used in knockback rotation, squish particles, buoyancy | Utilities.java:706-710 | Mth.java:50-56, Vec2.java:82-86 | AI-SLF-D2 |

### AI-SLF-01  SULFUR-04 fix counts knockback calls, so spear jabs, sweeps, i-frame hits and knockback-only hits get the wrong scale
- severity: gameplay
- vb: addon: src/main/java/com/coolfreeze23/vbparity/sulfur/mixin/SulfurCubeMixin.java:40-53 (hurt HEAD / knockback / tick HEAD)
  ```java
  this.vbp$knockbackCalls = 0;                                         // hurt HEAD
  return this.vbp$knockbackCalls++ == 0 ? powerMultiplier : powerMultiplier * (float) power * 0.25F;   // knockback
  this.pendingKnockbackSource = null;                                  // tick HEAD
  ```
  VB callers that reach `SulfurCube.knockback(double,double,double)` (VB-SC:841-884, which reads `pendingKnockbackSource`/`pendingKnockbackDamage` set in `hurt`, VB-SC:256-257):
  - src-121 LivingEntity.java:1188-1199: an attack inside the invulnerability window with more damage (`invulnerableTime > 10 && amount > lastHurt`) sets `flag1 = false`, so `hurt` makes no default knockback call.
  - src-121 Player.java:1324-1329 (sweep): `livingentity2.knockback(0.4F, …); livingentity2.hurt(damagesource, f5);`, knockback **before** hurt.
  - src-vb core/mixin/common/spear_behavior/PlayerMixin.java:162-165 and LivingEntityMixin.java:161-165: `boolean wasHurt = dealsDamage && target.hurt(…); if (dealsKnockback) { vb$causeExtraKnockback(target, 0.4F, …); vb$causeExtraKnockback(target, this.getKnockback(target, damageSource), …); }`
- native: src-mc/net/minecraft/world/entity/monster/cubemob/SulfurCube.java:860 `float powerMultiplier = Mth.sqrt(damage) * (comesFromEffect ? (float)power * 0.25F : 1.0F);`. The caller says whether a call is an extra one:
  - LivingEntity.java:2918-2920 (stab): `causeExtraKnockback(target, 0.4F, …, false); causeExtraKnockback(target, this.getKnockback(target, damageSource), …, true);`
  - Player.java:1170-1176 (sweep): `if (nearby.hurtServer(…)) { nearby.knockback(0.4F, …, damageSource, enchantedDamage); }`, i.e. comesFromEffect=false, after hurt.
  - Player.java:981-983: `causeExtraKnockback(entity, getKnockback + (knockbackAttack ? 0.5F : 0.0F), …, true)`, scaled even when `hurtServer` gave no default knockback (LivingEntity.java:1223-1256, `tookFullDamage = false`).
- difference: native scales a call only when the caller marks it `comesFromEffect`. The add-on scales every call after the first since the last `hurt`:
  - Spear jab on a block-holding cube: native = default (×1) + 0.4 jab (×1) + enchant/attribute jab (×power·0.25). VB+add-on = default (×1) + 0.4 jab (×0.4·0.25 = **×0.1**) + second jab (×power·0.25). The 0.4 jab impulse is 10× too weak.
  - Hit inside the i-frame window with higher damage, sprinting: native ×0.125 (0.5·0.25). Add-on: the sprint call is call #0 → **×1** (8× too strong).
  - Sweep target: the 1.21.1 knockback runs before `hurt`. With the cube not hurt yet this tick, `pendingKnockbackSource` is null, so it takes the vanilla `super.knockback` path, not the archetype path. If it was hurt earlier in the tick, the stale counter scales it ×0.1. Native: one archetype impulse ×1 after the default one.
  - Knockback-only hits (spear with `dealsDamage` false, or a `#no_knockback` damage source followed by an extra knockback): `hurt` is not called or makes no call. The source is null after the tick-HEAD reset, so VB uses vanilla knockback where native uses the archetype path with the caller's source.
  - Aside (spear area, not this group): VB's spear passes 1.21.1 `getKnockback` (not halved), native halves it (LivingEntity.java:1550-1554).
- fix_kind: mixin-vb (SulfurCube#knockback) + mixin-mc (net.minecraft.world.entity.player.Player#attack, net.minecraft.world.entity.Mob#doHurtTarget) + mixin-vb (spear_behavior PlayerMixin/LivingEntityMixin#vb$stabAttack)
- fix: replace the counter with an explicit "comes from effect" flag and source carried by the caller.
  1. In `SulfurCubeMixin` add `@Unique public static final ThreadLocal<Boolean> VBP_FROM_EFFECT = ThreadLocal.withInitial(() -> false)` and `@Unique static final ThreadLocal<DamageSource> VBP_SOURCE`, `VBP_DAMAGE`. In the `@ModifyExpressionValue` on `Mth.sqrt`, return `VBP_FROM_EFFECT.get() ? v * (float)power * 0.25F : v`. Add a `@ModifyVariable` at knockback HEAD for `attacker`/`damage`: if `VBP_SOURCE.get() != null`, use it instead of `pendingKnockbackSource`.
  2. `@WrapOperation` on src-121 Player.attack's `LivingEntity.knockback(DDD)` call inside `if (f4 > 0)` (ordinal 0): set FROM_EFFECT=true, SOURCE=damagesource, DAMAGE=f (the final attack damage) around the call. Do the same on Mob.doHurtTarget's `knockback(DDD)` call.
  3. In Player.attack, `@WrapOperation` the sweep `knockback(0.4F, …)` (ordinal 1) to skip the call, and `@WrapOperation` the following `hurt(damagesource, f5)` so it calls hurt first, then, only if hurt returned true, the knockback with FROM_EFFECT=false and SOURCE/DAMAGE = damagesource/f5. This reproduces native order. Scope it to `target instanceof SulfurCube` so other mobs keep 1.21.1 order, or apply it to all if the drift should also be fixed.
  4. In VB's `vb$stabAttack` (both mixins), wrap the first `vb$causeExtraKnockback` with FROM_EFFECT=false and the second with FROM_EFFECT=true, both with SOURCE=damageSource and DAMAGE=postEnchantmentDamage/totalDamage.
  5. Drop `vbp$knockbackCalls` and the hurt-HEAD reset. Keep the tick-HEAD clear of `pendingKnockbackSource`.
- config_gate: has_sulfur_cubes (spear parts: has_spears)

### AI-SLF-02  Primed (invulnerable) cubes can't be hit by spears
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/level/components/PiercingWeapon.java:62-64 (canHitEntity) `if (target.isInvulnerable() || !target.isAlive()) { return false; }`. VB-SC:381 `this.setInvulnerable(true);` in primeTime. VB SulfurCube has no override.
- native: src-mc/net/minecraft/world/entity/monster/cubemob/SulfurCube.java:923-925 `public boolean isInvulnerableToPiercingWeapon() { return this.isInvulnerable() && !this.isPrimed(); }`. Used by PiercingWeapon.java:61-62 `if (target.isInvulnerableToPiercingWeapon() || !target.isAlive()) return false;`. Then LivingEntity.java:2916-2920 applies both `causeExtraKnockback` calls even when `hurtServer` fails.
- difference: Natively a lit sulfur cube can be jabbed with a spear. The hit does no damage, but both knockback impulses (0.4 and the enchant/attribute one) throw the bomb, using the archetype knockback path (the source has an entity). In VB the spear passes through a primed cube (0 impulses), so lit cubes can't be batted away.
- fix_kind: mixin-vb (com.blackgear.vanillabackport.common.level.components.PiercingWeapon#canHitEntity)
- fix: `@WrapOperation(method="canHitEntity", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;isInvulnerable()Z"))` return `target instanceof SulfurCube cube ? original.call(target) && !cube.isPrimed() : original.call(target)`. The knockback scale then needs AI-SLF-01 step 4.
- config_gate: has_spears + has_sulfur_cubes

### AI-SLF-03  Bucketing a cube always makes it (and its future split babies) persistent
- severity: gameplay
- vb: src-vb/.../sulfur_cube/SulfurCube.java:559-561 (bucketMobPickup) `if (pickupEntity instanceof SulfurCube cube) { cube.setPersistenceRequired(); }`. Then saveToBucketTag :186-188 `if (this.isPersistenceRequired()) { tag.putBoolean("PersistenceRequired", …); }`, and loadFromBucketTag :221-223 `if (tag.contains("PersistenceRequired")) { this.setPersistenceRequired(); }`.
- native: src-mc/net/minecraft/world/entity/Bucketable.java:55-57 `if (entity.isPersistenceRequired()) { tag.putBoolean("PersistenceRequired", …); }` and :78-98 (bucketMobPickup), which never calls setPersistenceRequired. A released cube is kept by `requiresCustomPersistence()` through `fromBucket()` only (SulfurCube.java:233-236). `from_bucket` is not inherited by split children (ConversionType convertCommon copies only `persistenceRequired`).
- difference: In VB every released cube has `PersistenceRequired=true`. When it later dies at size 2, both size-1 babies inherit `setPersistenceRequired()` (VB-ACM:186-188), so they never despawn and are skipped by the MONSTER mob-cap count. Natively those babies have neither flag: they despawn (>128 blocks instantly, or 1/800 per tick past 32 blocks after 600 idle ticks) and count toward the cap. The released adult itself behaves the same in both (fromBucket keeps it).
- fix_kind: mixin-vb (SulfurCube#bucketMobPickup)
- fix: `@Redirect(method="bucketMobPickup", at=@At(value="INVOKE", target="Lcom/blackgear/vanillabackport/common/level/entities/mob/monster/sulfur_cube/SulfurCube;setPersistenceRequired()V"))` → no-op. Saving and loading then follow native (the key is written only when the Mob field is already true).
- config_gate: has_sulfur_cubes

### AI-SLF-04  Dispensers put armor, elytra and carved pumpkins on sulfur cubes; a chestplate becomes the "swallowed block"
- severity: gameplay
- vb: src-121/net/minecraft/world/item/ArmorItem.java:41-58 (dispenseArmor) `getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), NO_SPECTATORS.and(new MobCanWearArmorEntitySelector(armorItem)))` … `livingentity.setItemSlot(equipmentslot, itemstack); setDropChance(slot, 2.0F); setPersistenceRequired();`. The test is EntitySelector.java:98-102 → `Mob.canTakeItem` (Mob.java:1233-1236) `getItemBySlot(getEquipmentSlotForItem(stack)).isEmpty() && this.canPickUpLoot()`. VB SulfurCube overrides neither `canTakeItem` nor `getEquipmentSlotForItem`, and `canPickUpLoot()` is true for an empty cube (VB-SC:652-654). VB stores the swallowed block in CHEST (VB-SC:304-306 `hasBodyItem() { return !this.getItemBySlot(EquipmentSlot.CHEST).isEmpty(); }`).
- native: EquipmentDispenseItemBehavior.java:20-22 filters with `entity.canEquipWithDispenser(dispensed)`, which is LivingEntity.java:3875-3882 `this.canUseSlot(slot) && equippable.canBeEquippedBy(type) ? getItemBySlot(slot).isEmpty() && this.canDispenserEquipIntoSlot(slot)`. SulfurCube.java:635-637 `canDispenserEquipIntoSlot(slot) { return slot == EquipmentSlot.BODY; }`, and swallowable items have no BODY `equippable` component. The carved-pumpkin behaviour (DispenseItemBehavior.java:248-266) also goes through `dispenseEquipment`.
- difference:
  - Natively no dispensed armor piece, elytra, head or carved pumpkin can be put on a sulfur cube. The dispense fails, and a pumpkin is not swallowed either.
  - In VB, an empty adult or baby cube in front of a dispenser takes helmets/pumpkins (HEAD), leggings and boots, with armor points and guaranteed drops, and becomes persistent.
  - A chestplate or elytra goes into CHEST, so VB treats it as the swallowed block. All goals are removed, the cube stops moving, it gains no archetype, it renders the item in the inner layer and it can't swallow real blocks until sheared.
- fix_kind: mixin-vb (SulfurCube: add `canTakeItem`)
- fix: in `SulfurCubeMixin` add `@Override public boolean canTakeItem(ItemStack stack) { return false; }`. Native allows no dispenser equipment for sulfur cubes (BODY items come only from swallowing, which the add-on's FEED behaviour handles). `Mob.canTakeItem` has no other caller that matters for cubes.
- config_gate: has_sulfur_cubes

### AI-SLF-05  Dispenser shears drop the block through NeoForge's sheep rule (y+1, random push, 5 extra entity RNG draws)
- severity: gameplay
- vb: src-121/net/minecraft/core/dispenser/ShearsDispenseItemBehavior.java:56-58 `shearable.onSheared(null, stack, level, pos).forEach(drop -> shearable.spawnShearedDrop(level, pos, drop));`
  - IShearable.java:66-73 captures the drops of `shearable.shear(SoundSource.BLOCKS)`, so VB-SC:640-646 `spawnAtLocation(itemStackToShear, bbHeight)` is captured, not spawned.
  - IShearable.java:86-108 (default branch) `ItemEntity itemEntity = entity.spawnAtLocation(drop, 1); … add((rand.nextFloat() - rand.nextFloat()) * 0.1F, rand.nextFloat() * 0.05F, (rand.nextFloat() - rand.nextFloat()) * 0.1F)` with `rand = entity.getRandom()`.
- native: src-mc/net/minecraft/core/dispenser/ShearsDispenseItemBehavior.java:56-58 `if (entity.isAlive() && entity instanceof Shearable shearable && shearable.readyForShearing()) { shearable.shear(level, SoundSource.BLOCKS, tool); … }`. SulfurCube.java:606-613 spawns the block at the passenger attachment (y + bbHeight) with ItemEntity's own random velocity only.
- difference:
  - Drop height: VB y+1.0, native y+0.98 for a size-2 cube.
  - Motion: VB adds up to ±0.1 x/z and +0..0.05 y.
  - RNG: VB draws 5 extra `nextFloat()` from the cube's RNG per dispenser shear (native draws none from the cube), which shifts the cube's later jump delays and pitches.
  - Player shearing is unaffected: mobInteract calls `shear` directly.
- fix_kind: mixin-vb (SulfurCube: add `spawnShearedDrop`)
- fix: in `SulfurCubeMixin` add `@Override public void spawnShearedDrop(Level level, BlockPos pos, ItemStack drop) { this.spawnAtLocation(drop, this.getBbHeight()); }`. This is the native position (same as the SULFUR-07 offset), with no extra random.
- config_gate: has_sulfur_cubes

### AI-SLF-06  Igniting with a fire charge in creative consumes it
- severity: gameplay
- vb: src-vb/.../sulfur_cube/SulfurCube.java:495-499 (mobInteract) `if (!heldItem.isDamageableItem()) { heldItem.shrink(1); } else { heldItem.hurtAndBreak(1, player, getSlotForHand(hand)); }`
- native: src-mc/.../cubemob/SulfurCube.java:480-484 `if (heldItem.is(Items.FLINT_AND_STEEL)) { heldItem.hurtAndBreak(1, player, …); } else { heldItem.consume(1, player); }` (`consume` skips players with infinite materials)
- difference: A creative player loses one fire charge per ignited cube in VB, natively none. Survival is the same.
- fix_kind: mixin-vb (SulfurCube#mobInteract)
- fix: `@Redirect(method="mobInteract", at=@At(value="INVOKE", target="Lnet/minecraft/world/item/ItemStack;shrink(I)V", ordinal=1))` (the shrink in the ignite branch; ordinal 0 is the baby-food shrink, which already checks `instabuild`) → `stack.consume(amount, player)` (1.21.1 `ItemStack#consume(int, LivingEntity)` exists). Capture `player` with `@Local(argsOnly=true)`.
- config_gate: do_sulfur_cubes_explode

### Engine drift (Sulfur cube)

### AI-SLF-D1  Dispenser shears cut a leashed cube's leash before its block
- severity: gameplay
- vb: src-121/net/minecraft/core/dispenser/ShearsDispenseItemBehavior.java:54-61 (tryShearLivingEntity), which has no leash step: `if (livingentity instanceof IShearable shearable && shearable.isShearable(null, stack, level, pos)) { … onSheared … }`
- native: src-mc/net/minecraft/core/dispenser/ShearsDispenseItemBehavior.java:51-54 `for (Entity entity : …) { if (entity.shearOffAllLeashConnections(null)) { return true; } …`, then Entity.java:2356-2363 drops all leash connections with SHEARS_SNIP.
- difference: Only block-holding cubes can be leashed (`canBeLeashed() = hasBodyItem()`). Natively the first dispenser pulse on a leashed cube only cuts the leash, and the second ejects the block. In 1.21.1 the first pulse ejects the block and the leash stays. A farm that leashes cubes in front of shear dispensers sees one extra pulse per cube natively.
- fix_kind: engine
- fix: none scoped. Leash shearing is a general 1.21.6+ mechanic; adding it only for sulfur cubes would make them differ from every other leashed mob in 1.21.1.
- config_gate: none

### AI-SLF-D2  Trig lookups take float input in 1.21.1, double in 26.2
- severity: cosmetic
- vb: src-121/net/minecraft/util/Mth.java:54-63 `return SIN[(int)(value * 10430.378F) & 65535];` / `SIN[(int)(value * 10430.378F + 16384.0F) & 65535]`. Used by VB Utilities.java:706-710 (knockback `rotate`), VB-ACM:120-123 (squish particles), VB-SC:199 (buoyancy).
- native: src-mc/net/minecraft/util/Mth.java:50-56 `return SIN[(int)((long)(i * 10430.378350470453) & 65535L)];` / `… + 16384.0)`, used through Vec2.java:82-86 `rotate(double)`.
- difference: For some angles the table index differs by 1 (one 65536th of a turn), from float vs double rounding. This gives sub-0.01% differences in archetype knockback direction and particle offsets. No farm effect.
- fix_kind: engine
- fix: none (changing Mth would affect every entity)
- config_gate: none

### AI-SLF-D3  No age lock (golden dandelion): feeding and growth are never gated by `canAgeUp()`
- severity: gameplay
- vb: src-vb/.../sulfur_cube/SulfurCube.java:476 `if (this.isFood(heldItem)) {`. src-121 AgeableMob ages every living baby (no lock).
- native: src-mc/.../cubemob/SulfurCube.java:442 `if (this.isFood(heldItem) && this.canAgeUp()) {`. AgeableMob.java:209-213 `if (this.canAgeUp()) this.setAge(++age)` with `canAgeUp() = isBaby() && !isAgeLocked()` (:218-220). Bucket saves `age_locked` (SulfurCube.java:173-176).
- difference: Natively an age-locked baby cube neither eats slime balls nor grows. 1.21.1 + VB has no golden dandelion and no `AgeLocked` data, so a locked cube can't exist and the result is the same in practice. Recorded so the gate is added if the age-lock feature is ever backported.
- fix_kind: engine
- fix: none until the age-lock feature exists in the pack
- config_gate: none

counts: farm=0 gameplay=6 visual=0 audio=0 data=0 cosmetic=0 drift=3


## Mobs using spears (SPR)

Scope: zombie, husk, zombie villager, drowned, zombified piglin (goal AI); piglin (brain); skeleton family (pickup and melee only, natively they have no spear AI); every mob that spawns with or picks up a spear. The effective code is VB 1.2.2 plus the add-on as it is in `src/main` now: mayhem `ZombieSpearMixin`, `MobIntegrationsMixin`, `MobSpearDamageMixin`, `LivingEntitySpearMixin` (damage type, attack sound, TPHYS-C02 knockback halving), `LivingEntityUseVibrationMixin` (TPHYS-C04), `MeleeAttackMixin` (TPHYS-CE01), `PiercingWeaponMixin` (MAYHEM-05, TPHYS-C07), `AttackRangeMixin`, `ProjectileUtilsMixin`, `EntityComputeSpeedMixin`, the `MayhemParity` component event (MAYHEM-01/02), copper `MobEquipmentMixin` (COPPER-19), and `data/minecraft/tags/item/piglin_loved.json` (MAYHEM-09).
Spawn-time rolls are also covered in `findings/tech-spawning.md` (SPAWN-10, SPAWN-13 and rows 86-96), and the knockback/vibration/piercing paths in `findings/tech-physics.md` (TPHYS-C02/C04/C07/CE01). Those are cited here, not repeated.
Not covered: `core/mixin/common/controllable_mounts/**` (how a mob drives its mount). It is not in this group's file list, and first-round left it out too.

| mechanic | VB/add-on location | native location | result |
|---|---|---|---|
| SpearUseGoal flags MOVE+LOOK; canUse `target != null && mainhand has KINETIC_WEAPON && !isUsingItem`; canContinueToUse `state != null && !done && ableToAttack` | VB ai/goal/SpearUseGoal.java:34-54 | ai/goal/SpearUseGoal.java:42-64 | match |
| SpearUseGoal start/stop (setAggressive, new state; stop: navigation.stop, setAggressive(false), stopUsingItem) | SpearUseGoal.java:56-68 | SpearUseGoal.java:66-80 | match |
| SpearUseGoal tick: approach while distSqr > 10², engage = `reducedTickDelay(delay + damage.maxDurationTicks)` (iron spear 12+225 → 119 goal ticks), then flee to `getPosAway(max(0,9+m-d), max(1,11+m-d), 7)`, reposition `getPosAway(6+m-d, 7+m-d, 7)` when distSqr < 2² or navigation done, `MAX_FLEEING_TIME = reducedTickDelay(100)` = 50, mountDistance m = 2 when passenger, lookAt 30/30 | SpearUseGoal.java:70-163 | SpearUseGoal.java:83-176 | match |
| Goal tick rate: no `requiresUpdateEveryTick` override, so it runs on the 1-in-2 full goal ticks | inherited Goal | inherited Goal | match |
| Charge speed modifier: root vehicle `ControllableMob.chargeSpeedModifier()` (camel husk 4.0, zombie horse 1.4, else 1.0) vs `vehicleMob.chargeSpeedModifier()` (Mob 1.0, CamelHusk 4.0, ZombieHorse 1.4) | SpearUseGoal.java:74-78, Spear{Approach,Attack,Retreat}; CamelHusk.java:119, ZombieHorseMixin.java:186 | SpearUseGoal.java:87-91, Mob.java:1497, CamelHusk.java:131, ZombieHorse.java:203 | match |
| getPosAway RNG (in order): if `dirAway.length()==0` two `nextDouble()`; then up to 10 tries of `nextFloat()` (angle ±π/2), `nextDouble()` (sqrt lerp), `nextInt(2*7+1)` only if inside the box, then 2×`nextDouble()` only when the mob has a home/restriction; weight = `getWalkTargetValue` | core/util/WorldUtilities.java:240-318 (+ 1.21.1 LandRandomPos.movePosUpOutOfSolid) | ai/util/LandRandomPos.java:38-84, RandomPos.java:26-134, GoalUtils.java:16-41 | match (1.21.1 `< getMaxBuildHeight` == native `<= getMaxY`) |
| Registration: Zombie (and Husk/ZombieVillager by inheritance) SpearUseGoal(1.0, 1.0, 10, 2) at priority 2, ZombieAttackGoal moved 2→3 | MobIntegrations.java:76 + add-on MobIntegrationsMixin.java:19-26 (excludes Drowned, ZombifiedPiglin); ZombieMixin.java:27-37 | zombie/Zombie.java:120-122 | match (MAYHEM-08 fix verified) |
| Registration: ZombifiedPiglin SpearUseGoal at 1, ZombieAttackGoal stays 2 | MobIntegrations.java:77; src-121 ZombifiedPiglin.java:71-72 | zombie/ZombifiedPiglin.java:73-74 | match |
| Drowned: no SpearUseGoal; DrownedAttackGoal(2) melees with a held spear | add-on MobIntegrationsMixin; src-121 Drowned.addBehaviourGoals | zombie/Drowned.java:92-97 | match |
| Goal insertion order: VB adds the goal on `EntityJoinLevelEvent` (Platform MobIntegration.java:45-52, CommonLoaderPipelines.java:119-126), so it sits after ZombieAttackGoal in the LinkedHashSet; native adds it before | src-121 GoalSelector.java:97-111 | Zombie.java:121-122 | match in result: on the tick a target appears VB starts ZombieAttackGoal (its `canUse` builds a path, no RNG) and SpearUseGoal pre-empts it in the same `goalUpdate` loop (2 < 3), so the end state is identical. The only cost is one extra `createPath` per engagement |
| Skeleton family: no spear goal; `reassessWeaponGoal` gives melee for any non-bow item | src-121 AbstractSkeleton.reassessWeaponGoal | skeleton/AbstractSkeleton.java:150-155 | match |
| Piglin FIGHT activity: SpearApproach(1.0,10), SpearAttack(1.0,1.0,2), SpearRetreat(1.0) at list index 3-5 (priority 13-15), between SetWalkTargetFromAttackTargetIfTargetOutOfReach and MeleeAttack(20) | PiglinAiMixin.java:18-34 | piglin/PiglinAi.java:170-187 | match |
| Piglin spear memories: 5 registered (SPEAR_STATUS explicit in VB, auto-registered from entry conditions natively by Brain.java:375-377); none persisted (no codec) | PiglinMixin.java:24-41, Platform EntityRegistry.java:51-53 | piglin/Piglin.java:88-96, MemoryModuleType.java:95-99 | match |
| SpearApproach: entry SPEAR_STATUS absent; start sets APPROACH; canStillUse `ableToAttack && distSqr > 10²`; tick sets LOOK_TARGET and moves; stop sets CHARGING; `timedOut` false; Behavior(60,60) so `tryStart` draws `level.getRandom().nextInt(1)` | ai/behavior/SpearApproach.java | ai/behavior/SpearApproach.java; Behavior.java:44-54 | match |
| SpearAttack: start only when status CHARGING, ENGAGE_TIME = delay+damage window (not reduced, brain ticks every tick), counts down 1/tick, reposition 6/7 at distSqr < 2² or navigation done; stop → RETREAT | ai/behavior/SpearAttack.java | ai/behavior/SpearAttack.java:93-177 | match |
| SpearRetreat: Behavior(100,100) (times out at 100 ticks, `nextInt(1)` draw); start condition picks the flee pos `getPosAway(max(0,9+m-d), max(1,11+m-d), 7)` (RNG on every check while status is RETREAT); canStillUse FLEEING_TIME < 100, pos present, navigation not done; stop erases all three memories | ai/behavior/SpearRetreat.java | ai/behavior/SpearRetreat.java:199-273 | match |
| Piglin plain melee while holding a spear (native `Piglin.canUseNonMeleeWeapon` counts KINETIC_WEAPON, in either hand) | add-on MeleeAttackMixin.java:17-20 | MeleeAttack.java:28-29,45-46; Piglin.java:357-358 | match (TPHYS-CE01 fix verified: `isHolding` checks both hands, as natively) |
| Mob melee range with a spear: active item (use item, else main hand), `inflate(maxReach*mobFactor)` intersects and not `inflate(minReach*mobFactor)` → 1.0..2.25 for every spear; no attack_range → `sqrt(2.04)-0.6` | spear_behavior/MobMixin.java:33-71 | Mob.java:1363-1399, :116 | match |
| BehaviorUtils.isWithinAttackRange / SetWalkTargetFromAttackTargetIfTargetOutOfReach with a spear (not a ProjectileWeaponItem → melee range) | 1.21.1 BehaviorUtils | BehaviorUtils.java:115-122 | match |
| MeleeAttackGoal (skeleton/drowned spear melee): 20-tick canUse throttle, path recalculation `4 + nextInt(7)`, `canPerformAttack` | src-121 MeleeAttackGoal | ai/goal/MeleeAttackGoal.java | match (NeoForge `canPenalize` is false) |
| Kinetic hit: `ticksUsed >= delay`, actionFactor 0.2 for mobs, conditions `ticksUsed <= max && speed >= min*0.2 && rel >= minRel*0.2`, damage `baseAttack + floor(rel*damageMultiplier)`, contact cooldown 10, dragon part → parent, entity event 2 | components/KineticWeapon.java:106-152 | item/component/KineticWeapon.java:99-140 | match |
| Kinetic speed source for mobs: `getKnownSpeed()*20` of the root vehicle; computeSpeed at the end of Entity.baseTick (VB) vs its start (native) gives the same position delta for mobs, since nothing moves them between those points | entity_movement/EntityMixin.java:45-66 | Entity.java:524, 577-584, 4044-4046 | match |
| Hit search: `getHeadLookAngle`, from = eye + look*min, to = eye + look*(max + max(0, knownMovement·look)), hitbox margin 0.125, `canHitEntity` (invulnerable-to-piercing, alive, Interaction, canBeHitByProjectile, PvP, same vehicle) | util/ProjectileUtils.java:28-95; components/PiercingWeapon.java:62-73 + add-on PiercingWeaponMixin.java:27-31 | ProjectileUtil.java:38-107; PiercingWeapon.java:61-73 | match (TPHYS-C07 fixed) |
| Stab damage source = weapon `damage_type` (spear, #no_knockback) | add-on LivingEntitySpearMixin.java:29-37 + MayhemParity.weaponDamageSource | LivingEntity.java:2913 | match (MAYHEM-02 fixed) |
| Stab knockback: `0.4F` then `getKnockback(target, src)`; native getKnockback is `/2.0F` | add-on LivingEntitySpearMixin.java:65-69 halves the 1.21.1 value | LivingEntity.java:1550-1554, 2918-2921 | match (TPHYS-C02 fixed) |
| causeExtraKnockback: `knockback(kb, sin(yRot), -cos(yRot))`, attacker `delta*(0.6,1,0.6)`; zero-direction fallback draws `random.nextDouble()` natively and `Math.random()` in 1.21.1, but sin/cos of a yaw are never both ~0, so it never runs | spear_behavior/LivingEntityMixin.java:193-199; src-121 LivingEntity.java:1540-1543 | LivingEntity.java:2746-2760, 1657-1660 | match |
| Stab: dismount `target.stopRiding()`, `hurtEnemy`, post-attack enchant effects on damage only, setLastHurtMob + playAttackSound | spear_behavior/LivingEntityMixin.java:151-191 + add-on :39-49 | LivingEntity.java:2901-2943 | match (MAYHEM-04 fixed) |
| Dismount thresholds per tier (wood 14, stone 13, copper 12, iron 11, gold 13, diamond 10, netherite 9) | add-on MayhemParity.java:58-67,152-166; copper CopperParityEvents.java:50-58 | Items.java:1659-1679 | match (MAYHEM-01/COPPER-21 fixed) |
| Spear tier parameters (attack duration, damage multiplier, delay, knockback/damage time and threshold), attack damage = tier bonus | ModItems.java:185-204, SpearItem.java:48-92 | Items.java:1659-1679, Item.java:495-545 | match |
| Use start/finish vibrations (spears: `interact_vibrations=false`) | add-on LivingEntityUseVibrationMixin.java:22-27 | ItemStack.java:742-747, LivingEntity.java:3516-3521, 3630-3634 | match (TPHYS-C04 fixed) |
| Use ticking: `onUseTick` → damageEntities; NeoForge `canContinueUsing` = `isSameItem` (native `ItemStack.isSameItem` keeps using); 72000 duration; no use-effect RNG for spears (1.21.1 `triggerItemUseEffects` only for DRINK/EAT) | src-121 LivingEntity.java:3137-3170, 3240-3255; SpearItem.java:95-105 | LivingEntity.java:3437-3488; ItemStack.java:1093-1103 | match |
| Mob spears never wear (native mob paths call `hurtEnemy`, not `postHurtEnemy`; 1.21.1 Mob.doHurtTarget calls neither) | SpearItem.java:135-143; src-121 Mob.java:1491-1519 | ItemStack.java:535-555; Mob.java:1401-1421; LivingEntity.java:2928-2930 | match (MAYHEM-06 verdict "wrong" agreed) |
| Mob melee with a spear (Mob.doHurtTarget): damage type spear, knockback `getKnockback*0.5` (1.21.1) = native `getKnockback()/2` via causeExtraKnockback, then `postPiercingAttack` (lunge) | add-on MobSpearDamageMixin.java:13-20; spear_behavior/MobMixin.java:25-31 | Mob.java:1401-1421 | match |
| Zombie weapon roll: `nextFloat() < (HARD ? 0.05 : 0.01)`, then `nextInt(6)`: 0 sword, 1 iron spear, 2-5 shovel | ZombieMixin.java:39-62 + add-on ZombieSpearMixin.java:21-31 | Zombie.java:385-397 | match (MAYHEM-07 fixed; SPAWN row 86) |
| Zombified piglin: one `nextInt(20)==0` → golden spear else golden sword | ZombifiedPiglinMixin.java:24-32 (after the RNG-free 1.21.1 sword set) | ZombifiedPiglin.java:225-227 | match |
| Piglin spawn weapon: `nextFloat() < 0.5` crossbow, else `nextInt(10)==0` golden spear / golden sword (entity random) | PiglinMixin.java:43-53 | Piglin.java:292-296 | match |
| Husk camel-husk jockey iron spear (0.1), zombie-horse rider iron spear | add-on jockey mixins (GHAST-43); ZombieHorseMixin.java:151-162 | Husk.java:107-111; ZombieHorse.java:137-143 | GHAST-43 fixed; draw order: SPAWN-10 (open, tech-spawning) |
| Converted zombie villagers can roll the iron spear | ZombieSpearMixin | Zombie.java:497 (CONVERSION skips equipment) | SPAWN-13 (tech-spawning) |
| Armour roll before the weapon roll (Mob.populateDefaultEquipmentSlots) | copper MobEquipmentMixin.java:31-62 | Mob.java:975-1003 | match with `has_copper_toolset` on (COPPER-19); off = 1.21.1 roll (copper area) |
| Spawn enchantment of the spear (`#on_mob_spawn_equipment` ⊇ `#non_treasure` ⊇ lunge) | VB tags/enchantment/non_treasure.json | tags/enchantment/on_mob_spawn_equipment.json | match |
| Pickup refusal: skeleton family and drowned never pick up `#spears` | AbstractSkeletonMixin.java:17-19, DrownedMixin.java:17-19 | AbstractSkeleton.java:216-218, Drowned.java:319-321 | match |
| Zombie / husk / zombie villager / zombified piglin spear pickup: whether a spear replaces or is replaced by the held weapon | src-121 Mob.java:654-703 | Mob.java:613-654 | **AI-SPR-D1** |
| Piglin pickup/equip with golden spear vs crossbow (both "wanted") | src-121 Piglin.java:425-439 + add-on piglin_loved.json | Piglin.java:300-302, 385-397 | **AI-SPR-D2** (MAYHEM-09 fix incomplete) |
| Piglin admires golden spear, offhand → equip/inventory flow | 1.21.1 PiglinAi pickUpItem/stopHoldingOffHandItem + tag | PiglinAi.java:327-400 | match apart from the equip decision (D2) |
| Item drop on replacement: `Math.max(random.nextFloat() - 0.1F, 0) < dropChance` only when the slot was non-empty | src-121 Mob.java:623 | Mob.java:557 | match in form; count changes with D1/D2 |

### Engine drift (Mobs using spears)

### AI-SPR-D1  Spear pickups are decided by 1.21.1 item-class rules, not by attack damage
- severity: gameplay
- vb: src-121/net/minecraft/world/entity/Mob.java:654-703 (canReplaceCurrentItem), with no VB or add-on override for spears — `if (existing.isEmpty()) return true; else if (candidate.getItem() instanceof SwordItem) { if (!(existing.getItem() instanceof SwordItem)) return true; else { double d2 = this.getApproximateAttackDamageWithItem(candidate); ... } } ... else { if (candidate.getItem() instanceof DiggerItem) { if (existing.getItem() instanceof BlockItem) return true; if (existing.getItem() instanceof DiggerItem) {...compare damage...} } return false; }`. SpearItem extends TieredItem, so it is neither a SwordItem nor a DiggerItem.
- native: src-mc/net/minecraft/world/entity/Mob.java:613-654 — `return slot == EquipmentSlot.MAINHAND ? this.compareWeapons(newItemStack, currentItemStack, slot) : false;` and compareWeapons: `if (preferredWeaponType != null) { if (current.is(pref) && !new.is(pref)) return false; if (!current.is(pref) && new.is(pref)) return true; } double newAttackDamage = this.getApproximateAttributeWith(newItemStack, Attributes.ATTACK_DAMAGE, slot); double oldAttackDamage = ...; return newAttackDamage != oldAttackDamage ? newAttackDamage > oldAttackDamage : this.canReplaceEqualItem(newItemStack, currentItemStack);`
- difference: ATTACK_DAMAGE modifiers: spears wood 0, stone 1, copper 1, iron 2, gold 0, diamond 3, netherite 4; swords 3/4/(copper 4)/5/3/6/7; iron shovel 3.5. Zombie base 3 (zombified piglin base 5). (a) A spear never replaces a non-empty main hand in VB. Natively it does when its damage is higher, or when equal and `canReplaceEqualItem` holds. Examples: a zombie with an iron spear (5) takes a diamond spear (6) natively; a zombie with an iron shovel (6.5) takes a netherite spear (7); a zombie holding a block item (3) takes any spear except wood. VB refuses all of these. (b) Any sword replaces a held spear in VB. Natively it has to deal more damage: a zombie with a netherite spear (7) keeps it against a wooden or golden sword (6) natively, and VB swaps. (c) A digger tool never replaces a held spear in VB (`existing` is not a DiggerItem), natively it does when stronger (iron spear 5 < iron shovel 6.5). (d) Each extra or missing swap adds or removes one `random.nextFloat()` drop roll (Mob.java:623 / native :557), and it changes what is left on the ground for a player to collect. This affects zombies, husks, zombie villagers and zombified piglins that can pick up loot (`setCanPickUpLoot(nextFloat() < 0.55*mult)`), and a drowned holding a converted spear against a sword. Skeletons and drowned don't pick up spears at all.
- fix_kind: mixin-mc (Mob.canReplaceCurrentItem(ItemStack,ItemStack), 1.21.1), scoped to spears
- fix: `@Inject(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)` on `Mob`: `if (!existing.isEmpty() && (candidate.is(ModItemTags.SPEARS) || existing.is(ModItemTags.SPEARS)) && getEquipmentSlotForItem(candidate) == EquipmentSlot.MAINHAND) { TagKey<Item> pref = vbp$preferred(); /* AbstractSkeleton (not WitherSkeleton) → #skeleton_preferred_weapons {bow}; Pillager → {crossbow}; Drowned → {trident}; Piglin && !isBaby → {crossbow, golden_spear}; else null — build these as item sets, since 1.21.1 has no such tags */ if (pref != null) { if (existing in pref && !(candidate in pref)) { cir.setReturnValue(false); return; } if (!(existing in pref) && candidate in pref) { cir.setReturnValue(true); return; } } double n = approx(candidate), o = approx(existing); cir.setReturnValue(n != o ? n > o : this.canReplaceEqualItem(candidate, existing)); }` where `approx(s) = s.getAttributeModifiers().compute(getAttributes().hasAttribute(ATTACK_DAMAGE) ? getAttributeBaseValue(ATTACK_DAMAGE) : 0, EquipmentSlot.MAINHAND)` (native `getApproximateAttributeWith`). The Drowned trident/nautilus-shell checks and the Piglin override still run first, because they override this method and only reach it through `super`.
- config_gate: has_spears

### AI-SPR-D2  Adult piglin: golden spear vs crossbow uses 1.21.1's crossbow rule (MAYHEM-09 fix incomplete)
- severity: gameplay
- vb: src-121/net/minecraft/world/entity/monster/piglin/Piglin.java:425-439 (canReplaceCurrentItem) — `boolean flag = PiglinAi.isLovedItem(candidate) || candidate.is(Items.CROSSBOW); boolean flag1 = PiglinAi.isLovedItem(existing) || existing.is(Items.CROSSBOW); if (flag && !flag1) return true; else if (!flag && flag1) return false; else return this.isAdult() && !candidate.is(Items.CROSSBOW) && existing.is(Items.CROSSBOW) ? false : super.canReplaceCurrentItem(candidate, existing);`. The add-on's `piglin_loved.json` (`"minecraft:golden_spear"`) makes the golden spear "wanted", and the rest is 1.21.1.
- native: src-mc/net/minecraft/world/entity/monster/piglin/Piglin.java:385-397 — `boolean newItemWanted = PiglinAi.isLovedItem(newItemStack) || preferredWeaponType != null && newItemStack.is(preferredWeaponType); ... return !newItemWanted && currentItemWanted ? false : super.canReplaceCurrentItem(newItemStack, currentItemStack, slot);` with `getPreferredWeaponType()` = `isBaby() ? null : #piglin_preferred_weapons` {crossbow, golden_spear} (:300-302), then Mob.compareWeapons (Mob.java:639-653).
- difference: when both items are wanted: (a) An adult holding a crossbow is offered a golden spear: VB always returns false. Natively both are preferred, attack damage is equal (5 + 0 vs 5), so `canReplaceEqualItem` decides. The spear wins if it has more enchantments, or the same number and less damage than the crossbow (crossbows wear when piglins fire them), or the same damage and a custom name. (b) An adult holding a golden spear is offered a crossbow: VB goes to 1.21.1 `Mob.canReplaceCurrentItem` (crossbow vs non-crossbow), which returns false. Natively `canReplaceEqualItem(crossbow, spear)` decides, so a more-enchanted or named crossbow wins. In VB the losing item goes to the piglin's inventory (`stopHoldingOffHandItem` → `putInInventory`) instead of the main hand. That changes which weapon the piglin fights with (crossbow behaviours vs the spear charge) and what it drops.
- fix_kind: mixin-mc (Piglin.canReplaceCurrentItem(ItemStack,ItemStack), 1.21.1)
- fix: `@Inject(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)` on `Piglin`: `if (hasSpears && (candidate.is(GOLDEN_SPEAR) || existing.is(GOLDEN_SPEAR))) { if (EnchantmentHelper.has(existing, PREVENT_ARMOR_CHANGE)) {ret false} boolean pref = !isBaby(); boolean nw = isLovedItem(candidate) || pref && (candidate.is(CROSSBOW) || candidate.is(GOLDEN_SPEAR)); boolean cw = isLovedItem(existing) || pref && (existing.is(CROSSBOW) || existing.is(GOLDEN_SPEAR)); if (nw && !cw) ret true; if (!nw && cw) ret false; /* both or neither: native Mob.compareWeapons */ ret existing.isEmpty() || (pref && existingPreferred && !candidatePreferred ? false : pref && !existingPreferred && candidatePreferred ? true : approx(candidate) != approx(existing) ? approx(candidate) > approx(existing) : canReplaceEqualItem(candidate, existing)); }` (same `approx` as D1). Leave every other pair to the 1.21.1 code. If D1 is implemented with the piglin preferred set, this hook can just call it for the both-wanted case.
- config_gate: has_spears

counts: farm=0 gameplay=0 visual=0 audio=0 data=0 cosmetic=0 drift=2


## variant farm animals + trades (axis `ai`, prefix VAR)

Scope: (a) cow, pig, chicken, sheep, frog, cat and wolf as VB 1.2.2 + the add-on change them; (b) villager and wandering-trader trades that involve VB content.
Effective code = VB `core/mixin/common/mob_variants/*`, `common/api/modules/{mob_variant,sound_variant}/**`, `MobMixin` (`core/mixin/common/extension/entity/MobMixin.java`), `TradeIntegrations`, plus the add-on's `variants/mixin/*` and `wild/WildTrades.java`.
Paths: VB = `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/`, add-on = `src/main/java/com/coolfreeze23/vbparity/`, native = `<mc-26.2>/net/minecraft/`, 1.21.1 = `<mc-1.21.1>/net/minecraft/`.

Headline: nothing in this group changes a farm's rate or output any more. Egg laying, the farm-animal biome tags and the sheep colour roll are all correct after the first-round fixes (VARIANTS-07/18/20). Goals, tempt items, breed foods (except the wolf, see D3), love time, cooldowns and baby growth are untouched by VB and identical to native. What is left is the **order and count of level-RNG draws** in `finalizeSpawn` and breeding (findings 01–06, all cosmetic: same distributions, different stream). It also includes three engine-drift items: wandering-trader pool structure, golden-dandelion age lock, and wolf fish food.

## Variant farm animals (cow, pig, chicken, sheep, frog, cat, wolf)

| mechanic | VB / add-on location | native location | result |
|---|---|---|---|
| Variant pick hook point (cow/pig/chicken/cat/frog/wolf) | VB `extension/entity/MobMixin.java:43-55` (`Mob.finalizeSpawn` RETURN → `vb$finalizeSpawn`) | `Cow.java:83-89`, `Pig.java:316-322`, `Chicken.java:186-192`, `Wolf.java:263-280`, `Frog.java:264-270` (before `super`); `Cat.java:404-411` (after `super`) | AI-VAR-01 (order), cat order matches |
| Spawn-chance roll `mob_variants_spawn_chance` | VB `VariantUtils.java:102` + add-on `VariantUtilsMixin.java:18-24` (no draw at ≥1.0) | `variant/VariantUtils.java:34-40` (no roll) | match (VARIANTS-20 fix verified) |
| Priority selection algorithm | VB `PriorityProvider.java:20-58` | `variant/PriorityProvider.java` `select`/`pick` | match (identical sort, filter and `Util.getRandomSafe` → one `nextInt(n)`) |
| Cow/pig/chicken candidate sets (1 candidate per biome after tag fix) | VB `CowVariants/PigVariants/ChickenVariants` + add-on biome tags | `cow_variant/*.json` etc. | match (one `nextInt(1)` per spawn in both) |
| Sound-variant pick, cow/pig/chicken | add-on `AgeableMobMixin.java:29-38` (AgeableMob RETURN, `nextInt(size)`) | `Cow.java:87`, `Pig.java:320`, `Chicken.java:190` (before `super`) | values match; order AI-VAR-01 |
| Sound-variant registry order cow/pig/chicken/cat | add-on `SoundVariants.java:122-161` (classic,moody / big,classic,mini / classic,picky / classic,royal) | data-driven, sorted by id | match |
| Mooshroom variant pick | VB `CowMixin.vb$finalizeSpawn` inherited by 1.21.1 `MushroomCow extends Cow` | `MushroomCow extends AbstractCow` (no cow variant) | AI-VAR-02 |
| Wolf variant pick | 1.21.1 `Wolf.java:207-219` (`WolfVariants.getSpawnVariant`, no draw) + VB `WolfMixin.java:119-124` (empty registry) | `Wolf.java:266-276` (`selectVariantToSpawn`, `nextInt(1)`) | AI-VAR-03 |
| Wolf sound-variant pick (spawn) | VB `WolfMixin.java:120` `REGISTRIES.getRandomElement(level.getRandom())` → Platform `BuiltInCoreRegistry.java:120-143` `nextInt(7)` | `Wolf.java:278` → `WolfSoundVariants.java:41-43` `getRandom(random)` `nextInt(7)` | bound matches; order and list order AI-VAR-03 |
| Wolf ambient sound role (`nextInt(3)`) | VB `WolfSoundVariantsModule.java:8-20`; add-on `WolfSoundMixin.java:61-69` (baby remap, no extra draw) | `Wolf.java:283-291` | match (one `nextInt(3)`, the same role split) |
| Wolf hurt/death/step (armor, baby) | add-on `WolfSoundMixin.java:44-77` | `Wolf.java:294-301`, `:175-176`, `:235-236` | match (VARIANTS-01/02 fixes verified) |
| Frog variant pick (spawn and tadpole conversion) | 1.21.1 `Frog.java:273-285` (biome tags, no draw) + VB `FrogMixin.java:58-62` (empty registry) | `Frog.java:264-270`, `Tadpole.java:241-250` | result matches; draw count AI-VAR-04 |
| Frog `initMemories` order | 1.21.1 `Frog.java:283` | `Frog.java:268` | match apart from the missing `nextInt(1)` before it (AI-VAR-04) |
| Cat breed pick | 1.21.1 `Cat.java:344-356` (`getRandomElementOf(tag)`) + add-on `CatNativeSpawnMixin.java:20-26` (moon in fixed-time dimensions) | `Cat.java:404-411` + `cat_variant/*.json` | normal spawns: one `nextInt(10|11)` in both; in-hut spawns AI-VAR-05 |
| Cat sound-variant pick | add-on `CatSpawnMixin.java:24-28` (Cat RETURN) | `Cat.java:409` (after `super` and the breed) | match |
| Cat persistence / CatSpawner | add-on `CatNativeSpawnMixin.java:35-42`, `CatSpawnerMixin.java:14-17`, `CatHutSpawn` | `npc/CatSpawner.java:57-76` | persistence matches. Hut-cat breed (MC-147659) was left as NeoForge's fix on purpose (VARIANTS-21) and is not re-reported |
| CatSpawner tick (1200, offsets `8+nextInt(24)` × sign, village 48/≥5 homes/<5 cats, hut 16/0 cats) | 1.21.1 `CatSpawner.java:25-91` | `CatSpawner.java:23-76` | match (1.21.1 also requires the `spawn-animals` server property, which native removed; doMobSpawning is checked by both) |
| Sheep spawn colour | VB `SheepMixin.java:20-30` + add-on `SheepMixin.java:43-60` (legacy roll skipped) | `Sheep.java:271-274, 304-309`, `SheepColorSpawnRules.java:11-60` | match (`nextInt(100)` then `nextInt(500)` only for the 82 branch; before `super` in both) |
| Sheep offspring colour | 1.21.1 `Sheep.java:342-383` | `Sheep.java:282-291` → `DyeColor.getMixedColor` | match (recipe mix, fallback `level.random.nextBoolean()`) |
| Sheep eats grass → `ageUp(60)` | 1.21.1 `Sheep.java:352-358` (`isBaby()`) | `Sheep.java:294-300` (`canAgeUp()`) | match except age lock (D2) |
| Cow/pig/chicken offspring variant | VB `VariantDataHolder.java:18-43` on `child.getRandom()` | `Cow.java:73-80`, `Pig.java:246-253`, `Chicken.java:176-183` (`this.random`) | AI-VAR-06 |
| Cat/wolf offspring variant + collar | 1.21.1 `Cat.java:307-328`, `Wolf.java:598-619` + VB `CatMixin.java:78-93`, `WolfMixin.java:126-142` | `Cat.java:372-391`, `Wolf.java:597-618` | outcome matches (50/50 variant, mixed collar with level-random fallback); draws AI-VAR-06 |
| Wolf offspring sound variant | VB `SoundVariantHolder.java:15-17` (`father.getRandom()` = this.random, `nextInt(7)`) | `Wolf.java:614` (`this.random`) | bound matches; position AI-VAR-06 |
| Offspring sound variant cow/pig/chicken/cat | none (synced default classic) | none in `getBreedOffspring` (default classic) | match |
| Egg lay: conditions, `--eggTime`, loot, sound draws, `nextInt(6000)+6000` | add-on `ChickenLayMixin.java:37-55` + `data/minecraft/loot_table/gameplay/chicken_lay.json` (random_sequence) | `Chicken.java:135-142`, `loot_table/gameplay/chicken_lay.json` | match (draw order: loot sequence, `nextFloat`×2 only if dropped, then `nextInt(6000)`; the vanilla block is neutralised by the `eggTime++` give-back) |
| Initial egg timer | 1.21.1 `Chicken.java:46` (field init) | `Chicken.java:80` (constructor) | match (`nextInt(6000)+6000` on the entity random) |
| Thrown egg hatching | 1.21.1 `ThrownEgg.java:66-91` + VB `ThrownEggMixin.java:28-43` | `ThrownEgg.java:66-93` | match (`nextInt(8)`, `nextInt(32)`→4; variant from component after `moveTo`; native baby size from VARIANTS-13 feeds `fudgePositionAfterSizeChange`) |
| Goals cow/pig/chicken/sheep (priorities, speeds, tempt items) | 1.21.1 `Cow.java:42-50`, `Pig.java:59-68`, `Chicken.java:55-63`, `Sheep.java:115-125` | `AbstractCow.java:40-48`, `Pig.java:80-89`, `Chicken.java:85-93`, `Sheep.java:74-84` | match |
| Goals cat/wolf | 1.21.1 `Cat.java:108-122`, `Wolf.java:121-140` | `Cat.java:109-123`, `Wolf.java:131-150` | match |
| TemptGoal range/stop distance | 1.21.1 `TemptGoal` (`range(10.0)`, `6.25`) | `TemptGoal` (`Attributes.TEMPT_RANGE` = 10 from `createAnimalAttributes`, stop 2.5²) | match for these animals |
| Breed / tempt food tags | 1.21.1 `VanillaItemTagsProvider.java:430-456` | `tags/item/{cow,pig,chicken,sheep,cat,frog}_food.json` | match; wolf_food D3 |
| Love/breed cycle (inLove 600, parent age 6000, XP `nextInt(7)+1`) | 1.21.1 `Animal.java` | `Animal.java` | match (only code-style and NeoForge's BabyEntitySpawnEvent differ) |
| Baby growth (−24000, +1/tick, feed `(int)(t/20*0.1F)`) | 1.21.1 `AgeableMob.java:130-167` | `AgeableMob.java:205-262` | match except age lock (D2) |
| Group baby chance (`nextFloat() <= 0.05`, group>0) | 1.21.1 `AgeableMob.java:28-41` | `AgeableMob.java:48-63` | match (position in the stream AI-VAR-01) |
| Mob finalizeSpawn draws (`triangle(0,0.11485)`, `nextFloat()<0.05` left-handed) | 1.21.1 `Mob.java:1192-1204` | `Mob.java:1098-1110` | match (position AI-VAR-01) |
| Baby dimensions | add-on `BabyDimensions` + `{Cow,Chicken,Pig,Sheep,Wolf,Cat}Mixin` | `*.BABY_DIMENSIONS` | match (VARIANTS-13 fix) |
| Tame chance wolf (`nextInt(3)==0`) / cat | 1.21.1 `Wolf.tryToTame`, `Cat` | `Wolf.java:504-514`, `Cat` | match |
| AdultSensorAnyType (happy ghast) | VB `ai/sensor_types/AdultSensorAnyType.java:13-26` + add-on `data/minecraft/tags/entity_type/followable_friendly_mobs.json` (armadillo added) | `ai/sensing/AdultSensorAnyType.java:9-14`, `AdultSensor.java:26-42` | match: same requirements, predicate `is(FOLLOWABLE_FRIENDLY_MOBS) && !isBaby()`, `findClosest`, default scan rate 20 (GHAST-16 fix verified). Start-delay source D4 |
| Wolf/cat collar dye mixing | VB `Utilities.java:252-262` | `DyeColor.java:136-143` | match |
| Pig eat sound timing | add-on `AnimalMixin.java:45-56` | `Animal.mobInteract` → `Pig.playEatingSound` | match (2 voice-pitch draws on the mob random in both) |

### AI-VAR-01  Cow/pig/chicken/wolf pick variant and sound variant after `super.finalizeSpawn` instead of before
- severity: cosmetic
- vb: `core/mixin/common/extension/entity/MobMixin.java:43-55` (`vb$onFinalizeSpawn`, `@At("RETURN")` of `Mob.finalizeSpawn`): `SpawnGroupData handled = this.vb$finalizeSpawn(level, difficulty, reason, original);` → `CowMixin.java:56-59` `VariantUtils.<CowVariant>selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), CowVariants.REGISTRIES, VariantSpawner.FARM_ANIMALS).ifPresent(this::setVariantData);` (same in `PigMixin.java:56-59`, `ChickenMixin.java:61-66`). addon: `variants/mixin/AgeableMobMixin.java:29-37` `@Inject(method = "finalizeSpawn", at = @At("RETURN")) … family.pickRandom(this, level.getRandom());`. Wolf: `WolfMixin.java:119-121` `this.vb$setSoundVariant((WolfSoundVariant)WolfSoundVariants.REGISTRIES.getRandomElement(level.getRandom()));`, also at Mob RETURN. The 1.21.1 super chain they run after: `AgeableMob.java:33-37` `level.getRandom().nextFloat() <= …getBabySpawnChance()` and `Mob.java:1197` `randomsource.triangle(0.0, 0.11485000000000001)`, `:1201` `this.setLeftHanded(randomsource.nextFloat() < 0.05F);`.
- native: `world/entity/animal/cow/Cow.java:86-88`: `VariantUtils.selectVariantToSpawn(...).ifPresent(this::setVariant); this.setSoundVariant(CowSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom())); return super.finalizeSpawn(level, difficulty, spawnReason, groupData);`. The same order is in `Pig.java:319-321`, `Chicken.java:189-191` and `Wolf.java:266-279` (variant, then `setSoundVariant(...pickRandomSoundVariant(..., level.getRandom()))` at :278, then `super` at :279).
- difference: level-RNG order per spawn. Native: [variant `nextInt(1)`] → [sound `nextInt(2|3|2|7)`] → [baby `nextFloat` if group size > 0] → [`triangle`, 2 draws] → [left-handed `nextFloat`]. VB + add-on: [baby] → [triangle] → [left-handed] → [variant `nextInt(1)`] → [sound]. Each decision keeps the same distribution, but the draws are permuted, so a seeded or replayed spawn (test harness, `/summon` sequences) gives different sound variants, left-handedness, follow-range bonus and baby flags than native.
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.Cow`, `Pig`, `Chicken`, `Wolf`)
- fix: in add-on mixins for `Cow`, `Pig` and `Chicken` (none declares `finalizeSpawn` in 1.21.1), add `@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance d, MobSpawnType t, @Nullable SpawnGroupData g)`. Give it the body `if (getType()==EntityType.<X>) { VariantUtils.selectVariantToSpawn(SpawnContext.create(level, blockPosition()), <X>Variants.REGISTRIES, VariantSpawner.FARM_ANIMALS).ifPresent(((VariantDataHolder)this)::setVariantData); SoundVariants.<X>.pickRandom(this, level.getRandom()); } return super.finalizeSpawn(level, d, t, g);`. The type check skips mooshrooms (AI-VAR-02). Then neutralise the late picks. Use `@WrapMethod(method="vb$finalizeSpawn")` at priority 1100 on Cow/Pig/Chicken, returning `spawnData` unchanged. In `AgeableMobMixin`, exclude Cow/Pig/Chicken (keep the method for nothing else, or delete it). For `Wolf`, add `@Inject(method="finalizeSpawn", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/TamableAnimal;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"))`. It performs the VB sound pick there (after the 1.21.1 variant set, before super), and a `@WrapMethod` on `WolfMixin.vb$finalizeSpawn` that skips VB's own sound pick. Also add the native `nextInt(1)` variant draw for the non-pack branch (AI-VAR-03).
- config_gate: `farm_animal_variants` (variant part), `wolf_sound_variants` (wolf sound part)

### AI-VAR-02  Mooshrooms run the cow-variant pick (one extra level-RNG draw and a stray `variant` tag)
- severity: cosmetic
- vb: `CowMixin.java:30-31, 56-59`: `@Mixin({Cow.class}) … public SpawnGroupData vb$finalizeSpawn(...) { VariantUtils.<CowVariant>selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), CowVariants.REGISTRIES, VariantSpawner.FARM_ANIMALS).ifPresent(this::setVariantData);`. In 1.21.1, `src-121/.../animal/MushroomCow.java:45` `public class MushroomCow extends Cow`, so every mooshroom inherits it (and `CowMixin.vb$addAdditionalSaveData` writes `variant:"minecraft:temperate|warm|cold"`).
- native: `world/entity/animal/cow/MushroomCow.java:58` `public class MushroomCow extends AbstractCow implements Shearable` has no cow variant and no `finalizeSpawn` override, so the only draws are AgeableMob's and Mob's.
- difference: each mooshroom spawn makes one extra `level.getRandom().nextInt(1)` (`Util.getRandomSafe` on a one-element candidate list) between the left-handed roll and the end of `finalizeSpawn`. A `variant` key that native doesn't have is saved on mooshrooms. Rendering and behaviour are unaffected.
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.MushroomCow`)
- fix: add-on mixin on `MushroomCow` implementing VB's `MobBehaviorAccess` method: `@Override public SpawnGroupData vb$finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType r, @Nullable SpawnGroupData g) { return g; }`. Virtual dispatch from `MobMixin.vb$onFinalizeSpawn` then skips the pick. Optionally `@Inject(method="addAdditionalSaveData", at=@At("TAIL"))` → `tag.remove("variant")`. With AI-VAR-01's type check this collapses into the same patch.
- config_gate: `farm_animal_variants`

### AI-VAR-03  Wolf spawn: no variant draw, sound pick after `super`, sound list in a different order
- severity: cosmetic
- vb: 1.21.1 `src-121/.../animal/Wolf.java:207-218`: `holder1 = WolfVariants.getSpawnVariant(this.registryAccess(), holder); spawnGroupData = new Wolf.WolfPackData(holder1); … this.setVariant(holder1); return super.finalizeSpawn(...)`. `WolfVariants.java:47-54`: `registry.holders().filter(p -> p.value().biomes().contains(biome)).findFirst().or(() -> registry.getHolder(DEFAULT))`, which is deterministic with no draw. Then VB `WolfMixin.java:120`: `WolfSoundVariants.REGISTRIES.getRandomElement(level.getRandom())` → Platform `BuiltInCoreRegistry.java:124` `list.get(random.nextInt(list.size()))` over code-registration order `WolfSoundVariants.java:14-76` (classic, puglin, sad, angry, grumpy, big, cute). `VariantUtils.selectVariantToSpawn` on the empty `WolfDataVariants` registry makes no draw (add-on `VariantUtilsMixin` removes the chance roll and `getRandomSafe` on an empty list does not draw).
- native: `world/entity/animal/wolf/Wolf.java:266-279`: `if (groupData instanceof Wolf.WolfPackData wolfGroupData) { this.setVariant(wolfGroupData.type); } else { Optional<…> selectedVariant = VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.WOLF_VARIANT); … } this.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom())); return super.finalizeSpawn(...)`. `wolf_variant/*.json` gives one biome-matched candidate at priority 1 or `pale` at priority 0, so `nextInt(1)`. `WolfSoundVariants.java:41-43` `registryAccess.lookupOrThrow(Registries.WOLF_SOUND_VARIANT).getRandom(random)` uses the data-driven registry order (sorted by id: angry, big, classic, cute, grumpy, puglin, sad).
- difference: the first wolf of each pack makes 1 fewer draw in VB (native `nextInt(1)`). The sound pick (`nextInt(7)`) happens after `super`'s 3–4 draws instead of before them, and the same draw value maps to a different set (for example value 0 gives classic in VB and angry in native). Each wolf still has a uniform 1/7 chance per set, and the variant result is identical.
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.Wolf.finalizeSpawn`) + mixin-vb (`WolfMixin.vb$finalizeSpawn`)
- fix: `@Inject(method="finalizeSpawn", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/animal/WolfVariants;getSpawnVariant(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/core/Holder;)Lnet/minecraft/core/Holder;", shift=At.Shift.AFTER))` → `level.getRandom().nextInt(1);` (the native candidate draw, only reached on the non-pack branch). Add the INVOKE-`super` injection from AI-VAR-01 that sets `WolfSoundVariants.REGISTRIES` entry `order.get(level.getRandom().nextInt(7))`, where `order = [angry, big, classic, cute, grumpy, puglin, sad]`. Add `@WrapMethod` on `WolfMixin.vb$finalizeSpawn` that skips VB's own sound pick. Breeding uses the same sorted list (AI-VAR-06).
- config_gate: `wolf_sound_variants`

### AI-VAR-04  Frog spawn and tadpole conversion: native makes one `nextInt(1)` variant draw before `initMemories`
- severity: cosmetic
- vb: 1.21.1 `src-121/.../animal/frog/Frog.java:274-284`: `if (holder.is(BiomeTags.SPAWNS_COLD_VARIANT_FROGS)) { this.setVariant(...COLD) } else if (… WARM) {…} else {… DEFAULT_VARIANT} FrogAi.initMemories(this, level.getRandom()); return super.finalizeSpawn(...)`. VB `FrogMixin.java:58-61` selects from the empty `FrogDataVariants.REGISTRIES` and makes no draw. Tadpole conversion `Tadpole.java:236` calls the same `finalizeSpawn(…, MobSpawnType.CONVERSION, null)`.
- native: `world/entity/animal/frog/Frog.java:267-268`: `VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.FROG_VARIANT).ifPresent(this::setVariant); FrogAi.initMemories(this, level.getRandom());`. `frog_variant/{cold,warm}.json` are priority 1 with a biome tag and `temperate.json` is priority 0, so there is always one candidate and one `nextInt(1)`. `Tadpole.java:243-244` has the same call through `convertTo`.
- difference: native consumes one extra level-RNG value before `FrogAi.initMemories`. That shifts the long-jump cooldown draw (`initMemories` → `LONG_JUMP_COOLDOWN_TICKS` from `TIME_BETWEEN_LONG_JUMPS.sample(random)`) to the next value in the stream. Distributions and variant results are identical.
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.frog.Frog.finalizeSpawn`)
- fix: `@Inject(method="finalizeSpawn", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/animal/frog/FrogAi;initMemories(Lnet/minecraft/world/entity/animal/frog/Frog;Lnet/minecraft/util/RandomSource;)V"))` → `level.getRandom().nextInt(1);`.
- config_gate: none

### AI-VAR-05  Cat spawned inside a witch hut (spawn egg, `/summon`, hut piece): bound `nextInt(10|11)` instead of `nextInt(1)`, and the breed list is in a different order
- severity: cosmetic
- vb: 1.21.1 `src-121/.../animal/Cat.java:346-353`: `boolean flag = level.getMoonBrightness() > 0.9F; TagKey<CatVariant> tagkey = flag ? CatVariantTags.FULL_MOON_SPAWNS : CatVariantTags.DEFAULT_SPAWNS; BuiltInRegistries.CAT_VARIANT.getRandomElementOf(tagkey, level.getRandom()).ifPresent(this::setVariant); … if (…getStructureWithPieceAt(this.blockPosition(), StructureTags.CATS_SPAWN_AS_BLACK).isValid()) { this.setVariant(…ALL_BLACK); this.setPersistenceRequired(); }` (persistence is gated by add-on `CatNativeSpawnMixin.java:35-42`).
- native: `world/entity/animal/feline/Cat.java:408`: `VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.CAT_VARIANT).ifPresent(this::setVariant);`. `cat_variant/all_black.json` has `{"condition": structure #cats_spawn_as_black, "priority": 1}`, so inside a hut the list is `[all_black]` and the draw is `nextInt(1)`. Outside a hut, the 10 (or 11 at brightness ≥0.9) priority-0 entries are in registry order (all_black, black, british_shorthair, calico, jellie, persian, ragdoll, red, siamese, tabby, white).
- difference: same results (all-black in huts; uniform 1/10 or 1/11 elsewhere). In huts, 1.21.1 draws `nextInt(10|11)` and overwrites the result, where native draws `nextInt(1)`. Outside huts the same draw value maps to a different breed, because the 1.21.1 tag order (`cat_variant` tags) is not the native id order. CatSpawner hut spawns are not affected, because native runs `finalizeSpawn` at the origin (VARIANTS-21, kept by decision).
- fix_kind: mixin-mc (`net.minecraft.world.entity.animal.Cat.finalizeSpawn`)
- fix: `@WrapOperation` the `Registry.getRandomElementOf(TagKey, RandomSource)` call. If `level.getLevel().structureManager().getStructureWithPieceAt(blockPosition(), StructureTags.CATS_SPAWN_AS_BLACK).isValid()`, return `Optional.of(ALL_BLACK holder)` after `random.nextInt(1)`. Otherwise build the tag's holders sorted by key id and pick `list.get(random.nextInt(list.size()))`. Only worth it for seeded-replay parity.
- config_gate: none

### AI-VAR-06  Breeding: offspring variant drawn from the child's RNG, plus extra 1.21.1 draws (cow, pig, chicken, cat, wolf)
- severity: cosmetic
- vb: `common/api/modules/mob_variant/VariantDataHolder.java:21-24`: `RandomSource random = child.getRandom(); … heritableData = random.nextBoolean() ? fatherData : motherData;`. For cat and wolf (empty data registries) `:36-37`: `V variant = (V)(random.nextBoolean() ? ((VariantHolder)father).getVariant() : ((VariantHolder)mother).getVariant());` runs after 1.21.1 already did `if (this.random.nextBoolean()) wolf.setVariant(this.getVariant()) …` and, when tame, `if (this.random.nextBoolean()) wolf.setCollarColor(this.getCollarColor()) …` (`src-121/.../animal/Wolf.java:601-614`, `Cat.java:310-323`). VB then mixes the collar (`WolfMixin.java:133-137`, `CatMixin.java:85-89`, `Utilities.ColorUtils.getMixedColor`). For the wolf, `SoundVariantHolder.java:16` `of(child).vb$setSoundVariant(registry.getRandomElement(father.getRandom()))` (`WolfMixin.java:139`) comes before `VariantDataHolder.trySetOffspringVariant` (`:140`).
- native: `world/entity/animal/cow/Cow.java:76` `baby.setVariant(this.random.nextBoolean() ? this.getVariant() : partnerCow.getVariant());` (same in `Pig.java:249` and `Chicken.java:179`). `wolf/Wolf.java:600-614`: `if (this.random.nextBoolean()) baby.setVariant(this.getVariant()); else …; if (this.isTame()) { … baby.setCollarColor(DyeColor.getMixedColor(level, parent1CollarColor, parent2CollarColor)); } baby.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), this.random));`. `feline/Cat.java:375-387` is the same without the sound.
- difference: the outcome is the same: a 50/50 parent variant and the mixed collar with the `level.random` fallback. Draw sources differ. Cow/pig/chicken: native draws 1 `nextBoolean` from parent `this.random`; VB draws it from the newborn's random. Cat/wolf: native draws `this.random` = [variant] (+ wolf [sound `nextInt(7)`]). VB draws `this.random` = [variant] + [collar `nextBoolean`, tame only] (+ wolf [sound]) plus `child.random` = [re-pick `nextBoolean`], and the re-pick decides the final variant. The wolf sound list order also differs (AI-VAR-03).
- fix_kind: mixin-vb (`VariantDataHolder.trySetOffspringVariant`) + mixin-mc (`Cat/Wolf.getBreedOffspring`)
- fix: `@Redirect` the `LivingEntity.getRandom()` call in `trySetOffspringVariant` to `father.getRandom()`. For cat/wolf, `@WrapOperation` the `VariantHolder` re-pick branch (`:36-41`) into a no-op (1.21.1 already picked from `this.random`). Use `@Redirect` on both `this.random.nextBoolean()` calls inside the `isTame()` collar block of `Cat/Wolf.getBreedOffspring` (ordinal 1) to return `true` without drawing (VB overwrites the collar anyway). Map the wolf sound draw to the sorted list from AI-VAR-03.
- config_gate: `farm_animal_variants` (cow/pig/chicken path only when the variant registries are in use), `wolf_sound_variants`

## Villager and wandering-trader trades touching VB content

| mechanic | VB / add-on location | native location | result |
|---|---|---|---|
| Pale oak log 1 emerald → 8, 4 uses, 1 xp, 0.05 | VB `TradeIntegrations.java:12` (rare list) | `villager_trade/wandering_trader/emerald_pale_oak_log.json` (`max_uses` default 4, `xp` default 1: `item/trading/VillagerTrade.java:33-37`) in `#wandering_trader/uncommon` | values match; pool D1 |
| Open eyeblossom 1→1 ×7; pale oak sapling 5→1 ×8; pale hanging moss 1→3 ×4; pale moss block 1→2 ×5 | VB `TradeIntegrations.java:13-21` (generic) | `emerald_open_eyeblossom/pale_oak_sapling/pale_hanging_moss/pale_moss_block.json` in `#wandering_trader/common` | values match; pool D1 |
| Wildflowers 1→1 ×12; tall dry grass 1→1 ×12; firefly bush 3→1 ×12 | VB `TradeIntegrations.java:24-33` | `emerald_wildflowers/dry_tall_grass/firefly_bush.json` (common) | values match; pool D1 |
| Sulfur spike 1→2 ×5 | VB `TradeIntegrations.java:35-37` | `emerald_sulfur_spike.json` (common) | values match; pool D1 |
| Poplar log 1→8 ×4 (uncommon); poplar sapling 5→1 ×8, shelf mushroom 1→3 ×12 (common) | add-on `wild/WildTrades.java:20-24` | 26.3 `emerald_poplar_log/poplar_sapling/shelf_mushroom.json` | values match (WILD-26 fix verified); pool D1 |
| Price multiplier and xp | 1.21.1 `VillagerTrades.ItemsForEmeralds(…, int maxUses, int villagerXp)` → `this(..., 0.05F)` (`VillagerTrades.java:1690-1692`) | `reputation_discount: 0.05`, `xp` default 1 | match |
| Random calls inside each trade | 1.21.1 `ItemsForEmeralds.getOffer` (no draw without an enchantment provider) | `VillagerTrade.getOffer` with constant providers (no draw) | match |
| Professions (armorer, toolsmith, weaponsmith, cartographer, …) selling VB items: copper gear, spears, nautilus armour, harnesses, cushions, VB blocks, abandoned-camp maps | none | all 26.2 `villager_trade/<profession>/**` and 26.3 (scanned against the 310 VB item/block ids from VB lang files) | match: no native profession trade references a VB item; 26.3 has no abandoned-camp map trade |
| Librarian enchanted books: lunge in the pool | VB `data/minecraft/tags/enchantment/{non_treasure,tradeable}.json` (`lunge` appended; 1.21.1 `TagLoader` dedupes in insertion order) | `tags/enchantment/{non_treasure,tradeable}.json` (lunge last of 36 non-treasure; tradeable 40) | match: `#tradeable` has 40 entries with lunge at index 35 in both, and the draws are the same (`nextInt(40)`, `Mth.nextInt(level)`, cost `2+nextInt(5+10l)+3l`: 1.21.1 `EnchantBookForEmeralds.java:1522-1543` vs `EnchantRandomlyFunction.java:71-99`). The book-trade framework itself is engine |
| Traded equipment enchantments (`#on_traded_equipment` = `#non_treasure`) | 1.21.1 enchant with `ON_TRADED_EQUIPMENT` | same tag | match (lunge only applies to spears, which no villager sells) |
| Offer structure and sampling | 1.21.1 `WanderingTrader.java:134-151`, `AbstractVillager.java:245-256` + NeoForge `WandererTradesEvent` | `WanderingTrader.java:133-138`, `AbstractVillager.java:238-295`, `trade_set/wandering_trader/*.json` | D1 |

## Findings (trades)
None at VB or add-on level: every VB-item trade has native count, price, max uses, xp and multiplier. The remaining difference is the engine's pool structure (D1).

### Engine drift (variant farm animals + trades)

### AI-VAR-D1  Wandering trader: 1.21.1 picks 5 generic + 1 rare from the trader's RNG; native picks 2 buying + 2 uncommon + 5 common from per-world random sequences
- severity: farm (trader offer rate for VB items; emerald-sink and sapling farms)
- vb: 1.21.1 `src-121/.../npc/WanderingTrader.java:138-147`: `this.addOffersFromItemListings(merchantoffers, avillagertrades$itemlisting, 5); int i = this.random.nextInt(avillagertrades$itemlisting1.length); … getOffer(this, this.random)`. `AbstractVillager.java:249-250`: `arraylist.remove(this.random.nextInt(arraylist.size())).getOffer(this, this.random)`. Lists from `WandererTradesEvent`: generic = 64 vanilla (`VillagerTrades.java:666-729`) + 8 VB (`TradeIntegrations.java:16-36`) + 2 add-on (`WildTrades.java:22-23`) = 74; rare = 6 vanilla (`:733-738`) + VB pale oak log + add-on poplar log = 8.
- native: `world/entity/npc/wanderingtrader/WanderingTrader.java:135-137`: `addOffersFromTradeSet(level, offers, TradeSets.WANDERING_TRADER_BUYING); …_UNCOMMON); …_COMMON);`. `trade_set/wandering_trader/buying.json` `{"amount":2,"random_sequence":"minecraft:trade_set/wandering_trader/buying","trades":"#minecraft:wandering_trader/buying"}`, `uncommon.json` amount 2 and `common.json` amount 5, with tag sizes 6/15/76 (26.2) and 6/16/78 (26.3). `AbstractVillager.java:288`: `leftoverOffers.remove(lootContext.getRandom().nextInt(leftoverOffers.size()))`, where the random is the level's named random sequence (`LootContext.Builder.create(tradeSet.randomSequence())`, `:251`).
- difference: chance that a given trader offers each VB item. Pale oak log and poplar log: VB 1/8 = 12.5% each; native 2/15 = 13.3% (26.2 pale oak) and 2/16 = 12.5% (26.3, both). Each common item: VB 5/74 = 6.76%; native 5/76 = 6.58% (26.2) and 5/78 = 6.41% (26.3). Native also has 2 buying offers (water bottle, water bucket, milk, fermented spider eye, baked potato, hay block → emerald) that 1.21.1 lacks, and shows 9 offers in the order buying, uncommon, common, against 6 in the order generic, rare. Per-trader sequences differ: the trader's own RNG in 1.21.1 against per-world named sequences natively. The prices and uses of each offer match. Same as tech-outputs ED-1, given here with the numbers for this group.
- fix_kind: engine
- fix: not scopable to VB items. Faithful parity needs a full port of the 26.2 trader. That would be a mixin-mc `@Overwrite` of `WanderingTrader.updateTrades` that builds three lists in the native tag orders (buying 6, uncommon 15/16, common 76/78, VB ids at native positions, for example `emerald_pale_oak_log` index 12 of uncommon), and draws `amount` offers without duplicates from `level.getRandomSequence(ResourceLocation.withDefaultNamespace("trade_set/wandering_trader/<set>"))`. It would also add the 6 buying trades (`EmeraldForItems`). This changes vanilla trades too, so ship it only behind an add-on toggle.
- config_gate: `pale_trades` / `spring_to_life_trades` / `chaos_cubed_trades` (VB `doMerchantTrade*` keys) for the VB entries; none for the structure

### AI-VAR-D2  Golden dandelion age lock (baby growth pause) is absent
- severity: gameplay
- vb: 1.21.1 `src-121/.../AgeableMob.java:138-139` `if (i < 0) { this.setAge(++i); }` (always grows), `Animal.java:146` `if (this.isBaby()) { … this.ageUp(getSpeedUpSecondsWhenFeeding(-i), true);`, `Sheep.java:355` `if (this.isBaby()) { this.ageUp(60); }`. VB and the add-on have no `golden_dandelion` item or `AgeLocked` data (grep over VB and the add-on finds none).
- native: `world/entity/AgeableMob.java:72` `if (canUseGoldenDandelion(itemInHand, this.isBaby(), this.ageLockParticleTimer, this)) { setAgeLocked(...); return InteractionResult.SUCCESS; }`, `:209-210` `if (this.canAgeUp()) { this.setAge(++age); }`, `:219-220` `public boolean canAgeUp() { return this.isBaby() && !this.isAgeLocked(); }`. `Animal.java:152` `if (this.canAgeUp())` (feeding), `Sheep.java:297` `if (this.canAgeUp()) this.ageUp(60);`, and `Tadpole.java:104` `if (!this.level().isClientSide() && !this.isAgeLocked())`. Lock toggle cooldown 40 ticks (`AGE_LOCK_COOLDOWN_TICKS`); locking makes the mob persistent; `#cannot_be_age_locked` = zombie_horse, skeleton_horse, villager.
- difference: natively a golden dandelion freezes any baby or tadpole indefinitely (and resets it to −24000 on toggle). In VB babies always reach adulthood after 24000 ticks, so baby-mob farms and permanent-baby displays built natively cannot be reproduced.
- fix_kind: engine (a native item and system VB does not backport)
- fix: none scoped. It would need a new `minecraft:golden_dandelion` item plus mixin-mc on `AgeableMob` (synced `AGE_LOCKED`, NBT `AgeLocked`, `mobInteract` HEAD, `aiStep` age tick, `canAgeUp`) and redirects of `isBaby()` → `canAgeUp()` in `Animal.mobInteract`, `Sheep.ate` and `Tadpole.aiStep`/feeding. It belongs to whichever area owns 26.1 baby-mob content.
- config_gate: none

### AI-VAR-D3  Wolves cannot be fed or bred with fish or rabbit stew
- severity: gameplay
- vb: 1.21.1 `src-121/.../animal/Wolf.java:550-551` `public boolean isFood(ItemStack stack) { return stack.is(ItemTags.WOLF_FOOD); }` with `VanillaItemTagsProvider.java:430` `this.tag(ItemTags.WOLF_FOOD).addTag(ItemTags.MEAT);` (11 meats). VB ships no `wolf_food` tag (`src-vb/data/minecraft/tags/item/` has none), and neither does the add-on.
- native: `world/entity/animal/wolf/Wolf.java:548-549` (same `isFood`) with `data/minecraft/tags/item/wolf_food.json` `["#minecraft:meat","minecraft:cod","minecraft:cooked_cod","minecraft:salmon","minecraft:cooked_salmon","minecraft:tropical_fish","minecraft:pufferfish","minecraft:rabbit_stew"]`. Feeding heals `2.0F * nutrition` (`TamableAnimal.java:136-140`) and consumes via `Mob.usePlayerItem` (`Mob.java:1199-1208`), which returns the stew's bowl (`USE_REMAINDER`).
- difference: native wolves accept 7 more foods for breeding (BreedGoal/love), healing, puppy growth and tempting via begging. VB wolves accept only meat. A wolf breeder fed with fish works natively and does nothing in VB.
- fix_kind: data-override (`data/minecraft/tags/item/wolf_food.json`) + mixin-mc (`net.minecraft.world.entity.animal.Wolf.mobInteract`) for the stew bowl
- fix: ship `{"replace": false, "values": ["minecraft:cod","minecraft:cooked_cod","minecraft:salmon","minecraft:cooked_salmon","minecraft:tropical_fish","minecraft:pufferfish","minecraft:rabbit_stew"]}`. In 1.21.1 the heal path does `itemstack.consume(1, player)` (`Wolf.java:440`) and `Animal.usePlayerItem` just consumes, so the bowl would be lost. Add `@Inject(method="mobInteract", at=@At(value="INVOKE", target="Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))` on `Wolf` plus `@Inject` on `Animal.usePlayerItem` TAIL (Wolf only). When the stack was `rabbit_stew`, give `new ItemStack(Items.BOWL)` (`player.setItemInHand` if the stack is empty, else `getInventory().add`/drop) unless `player.hasInfiniteMaterials()`. Scoped to wolves.
- config_gate: none

### AI-VAR-D4  Brain sensor start delay drawn from a global RNG instead of the mob's (frog, tadpole, happy ghast's AdultSensorAnyType)
- severity: cosmetic
- vb: 1.21.1 `src-121/.../ai/sensing/Sensor.java:11` `private static final RandomSource RANDOM = RandomSource.createThreadSafe();`, `:30-32` `this.timeToTick = (long)RANDOM.nextInt(scanRate);` in the constructor.
- native: `world/entity/ai/sensing/Sensor.java:40-42` `public void randomlyDelayStart(final RandomSource randomSource) { this.timeToTick = randomSource.nextInt(this.scanRate); }`, called from `ai/Brain.java:86` with `body.getRandom()` (`:498`).
- difference: native consumes one `nextInt(20)` per sensor from each brain mob's own random when the brain is built (5 for a frog, 5 for a happy ghast); 1.21.1 does not. Phase distribution (uniform 0–19) is the same; every later draw on the frog's or ghast's own random is offset.
- fix_kind: engine
- fix: none worth scoping. It could be done with a `@Redirect` of `Sensor.<init>`'s `RANDOM.nextInt` to return 0 plus a `Brain` construction hook calling `nextInt(scanRate)` on the body random for the listed mobs only.
- config_gate: none

counts: farm=0 gameplay=0 visual=0 audio=0 data=0 cosmetic=6 drift=4 (D1 farm, D2 gameplay, D3 gameplay, D4 cosmetic)

