# Technical deep audit: physics (entity physics, block physics, combat math)

## Summary
- 17 findings: **farm 5** (A01, A02, B02, C01, C04), **gameplay 11** (A03, A04, B01, B03, B04, C02, C03, C05, C06, C07, C08), **data 1** (C09); plus 5 engine-drift entries (AE01 farm with a scoped fix, BE01, BE02, CE01 with a scoped fix, CE02 not fixable).
- Incomplete add-on fixes found: SULFUR-13 bounce (A02 slime stepOn order lost, B04 shelf mushroom restitution missing), WILD-24 shelf-mushroom bounce (B01 rest cut-off + gravity/drag compensation are portable), SULFUR-25 geyser (B03 server still pushes players), MAYHEM-02 spear damage type (C01 not in `#panic_causes` / `#can_break_armor_stand` -> regression), SULFUR-04 (C05 knockback-only stab path). GHAST-08 / GHAST-44 "wrong" verdicts re-checked and agreed.
- Covered: dimensions/eye/attachments (adult, baby, scaled) for every VB entity, attributes, friction/air drag/bounce attributes, fluid movement and buoyancy, fall damage tags, collision/push rules, riding maths (happy ghast, nautilus dash, camel husk dash), leash physics; every VB block's properties, exact VoxelShapes, pathfinding, waterlogging, washing-away, pistons, potent sulfur state machine + geyser launch, noxious gas, spike falling; spear components and KineticWeapon formula, Lunge, materials, ChargeAttack, sulfur cube knockback and explosion, creaking combat, damage types and their tags, enchantment applicability, shields, piglin/zombie spear AI.
- Not checked: Better Combat integration paths, client-side stab prediction, anything requiring an in-game run (all results from reading code). Cushion fire/fluid tick is WILD-30 (not repeated).
- Paths: VB = `<vanillabackport-1.2.2>/...`, native = `<mc-26.2>/...` (26.2) or `src-mc263` (Wilderness Bound content), 1.21.1 = `src-121`, add-on = `src/main/...`.

## Coverage

| mechanic | result |
|---|---|
| Entity type dims/eye/attachments/ridingOffset/category: creaking, happy ghast, copper golem, parched, camel husk, nautilus, zombie nautilus, sulfur cube (incl. spawnDimensionsScale 2.0), cushion | match (VB ModEntityTypes vs native EntityTypes:257,299,310,483,677,735,943,1107; cushion vs 26.3 EntityTypes:318; happy-ghast seats 1.7 via add-on GHAST-07) |
| VB pale-oak / poplar boat + chest boat dims | TPHYS-A04 (eye height) |
| Zombie horse dims, passenger 1.31875, MONSTER category, baby 0.7 + no aging (add-on MAYHEM-25) | match |
| Baby dims cow/mooshroom/pig/chicken/sheep/wolf/cat (add-on BabyDimensions) | match (native Cow:37, MushroomCow:61, Pig:70, Chicken:59, Sheep:60, Wolf:104, Cat:86) |
| Baby ghastling dims ×0.2375 eye 0.46875 (add-on GHAST-10) | match |
| Baby nautilus dims | TPHYS-A03 (passenger attachment) |
| Sulfur cube size scaling, passenger point `height - 0.015625*size*scale`, max health 4·size, speed 0.2F+0.1F·size, maxUpStep 0 with block, babies size 1 | match |
| Attributes: copper golem, happy ghast (TEMPT_RANGE/CAMERA_DISTANCE handled by GHAST-12/18), parched 16, camel husk = Camel.createAttributes, zombie nautilus 1.1F, zombie horse 25 HP + jump/speed formulas | match |
| Attributes: creaking MOVEMENT_SPEED, nautilus KNOCKBACK_RESISTANCE | TPHYS-C09 (double vs float literal) |
| Sulfur cube archetype modifiers (KB res, explosion KB res, bounciness, friction ×, air drag ×) for all 12 archetypes | match (VB SulfurCubeArchetypes vs native SulfurCubeArchetypes:20-150) |
| Friction attribute: ground friction `clamp(1-(1-f)*mod)`, air drag, vertical drag (omni vs 0.98) | match (VB attributes/LivingEntityMixin vs native LivingEntity.travelInAir:2471-2498) |
| getFrictionInfluencedSpeed `f>0.6 ? speed*0.216/f³ : speed` (26.2) vs 1.21.1 uncapped | match in effect: only sticky cubes reach f<0.6 (0.6→0.2) and a cube holding a block has speed 0 (`setSpeed(0.0F)` on equip, move control idle) |
| Bounce restitution maths for bouncy cubes (add-on EntityBounceMixin vs native Entity.restituteMovementAfterCollisions:813-845) | match for the velocity formula; TPHYS-A02 (slime stepOn order lost) |
| Bounce for non-bouncy cubes (sticky, babies, empty adults) on slime/bed | TPHYS-AE01 |
| BOUNCE game event (26.2) vs HIT_GROUND (add-on, 1.21.1 has no BOUNCE) | match for vibration frequency (both 2, VibrationSystem:59,61); not reported |
| Sulfur cube buoyancy `min(1,immersion)*0.04F`, vibe `0.2F*sin(tick*0.4F)`, jump threshold `bbHeight*0.2` | match (VB postTravelInFluid vs native travelInFluid:108-117) |
| Sulfur cube player push (1.3F, 0.3F/0.16F, clamp 0.5, vertical `kb*0.3F`), hit-angle knockback (1.6F/0.5F/0.8F, `*0.4F`, `*1.2`), default knockback direction for projectiles/fireworks/potions | match (known-speed source is MAYHEM-32) |
| Sulfur cube canFreeze / omnidirectional / canBreatheUnderwater with block | match |
| Happy ghast travel (`FLYING_SPEED*5/3` water ×0.8F, lava ×0.5, air ×0.91F), still-timeout stop, move control float 2..6 ticks, canReach / blockTraversalPossible, happy_ghast_avoids tag | match (VB zeroes velocity in travel during still timeout; native zeroes it in move control before travel — same result) |
| Happy ghast ridden input `(xxa, -sin(xRot)·k, cos(xRot)·k) + 0.5 up on jump, ×3.9F·FLYING_SPEED`, back ×-0.5F, turn 0.08F, config `happy_ghast_speed_modifier`=1.0 | match |
| Happy ghast collision (add-on GHAST-09) vs native canBeCollidedWith(Entity) incl. boat canVehicleCollide | match |
| Happy ghast dismount at maxY, flight-kick exemption (native isFlyingVehicle), precise position sync, fall damage none, onClimbable false, dismounts_underwater, fall_damage_immune | match |
| Nautilus water travel `moveRelative(speed); ×0.9`, ridden speed 0.0325F/0.02F × MOVEMENT_SPEED, ridden input, turn 0.5F, isPushedByFluid false | match |
| Nautilus dash `look × (inWater?1.2F:0.5F) × amount × speed × blockSpeedFactor`, cooldown 40, dashing cleared <35, jump pending scale | match |
| Zombie nautilus sun burn (BODY slot, nextInt(2) wear, 8 s) | match |
| Camel husk dash `22.2222F·amount·speed·speedFactor`, `1.4285F·amount·jumpPower`, cooldown 55, ridden speed +0.1F sprint, can't be baby, undead via #zombies | match (1.21.1 Camel identical) |
| Zombie horse sun burn, jump `0.5 + 3×rand×0.0666…`, speed `(9+3 rand)/42.16F` (add-on MAYHEM-27), horse fall attrs (safe 6, ×0.5) | match |
| can_float_while_ridden `+0.04F` when fluid height > jump threshold (VB postTravelInFluid) | match (placement re-checked in GHAST-44) |
| Creaking pushable/push/knockback gated by canMove, fireImmune & canUsePortal when heart-bound, step 1.0625, stopInPlace | match |
| Copper golem step 1.0, speed 0.2F, leash offset `0.75F*eyeHeight`, can_breathe_under_water | match |
| Copper golem fall damage | TPHYS-A01 |
| Parched (no daylight burn, skeleton physics) | match |
| Entity-type tags with physics effect: fall_damage_immune, can_breathe_under_water, dismounts_underwater, can_float_while_ridden, cannot_be_pushed_onto_boats (add-on), non_controlling_rider (add-on), not_affected_by_geysers, zombies/undead, freeze_immune, powder_snow_walkable | A01 (fall_damage_immune); the rest match (nautilus breathing and zombie horse dismount are handled in code / by the 1.21.1 tag) |
| Cushion seat entity (dims 1.0×0.25, no passenger override, updateInterval MAX) | match (fire/fluid tick is WILD-30; `dontTrackDeltas` has no 1.21.1 builder equivalent, the cushion never moves) |
| Known speed tracking (computeSpeed at baseTick TAIL vs native HEAD) | match in effect (baseTick moves the entity only on portal teleport, which resets the tracker); player/minecart parts are MAYHEM-32/33 |
| Modern leash physics constants (elastic 6, snap 12, 0.8/0.2/0.8, torsion 10, stiffness 0.11, ×0.3F same-direction, quad ×0.25, angular friction) | match; VB skips elastic pull for player-controlled leashees (native does not), but the server impulse is discarded by client-authoritative movement, so not reported |
| VB experiment `NewMinecartBehavior` | no consumer (dead code in 1.2.2) |
| friction / speedFactor / jumpFactor (all VB blocks; native sets them only on ice, soul sand, slime, honey, beds, shelf mushroom) | match (none set either side) |
| bounceRestitution / fallDistanceReduction: shelf mushroom 0.75 / 0.5 | fall reduction match (`fallOn(fd*0.5)` = `fd*(1-0.5)`); bounce TPHYS-B01, TPHYS-B04 |
| straw bed bounce / fall reduction (26.3: no `bounceRestitution`, no `fallDistanceReduction`; VB: none) | match |
| shapes: leaf litter, wildflowers (1.21.1 PinkPetals quadrant table = native Segmentable CCW, h=3), resin clump (1px multiface), mossy carpet (shape + BASE-only collision), pale hanging moss (base 1..15 / tip y2..16), shelf (3-box), shelf mushroom age 0/1 (same column/move maths), straw bed head/foot, dried ghast (3..13, 0..10), copper golem statue (3..13, 0..14), copper chest (1.21.1 chest boxes = native `boxZ(14,0,14,0,15)`), copper bars (post 7..9 + arms; same union), copper chain (6.5..9.5), copper lantern standing/hanging, lightning rods (6..10), copper torch / wall torch, speleothem TIP_MERGE/TIP_UP/TIP_DOWN/FRUSTUM/MIDDLE/BASE, bush, cactus flower, short/tall dry grass, eyeblossom/firefly bush (flower/bush defaults) | match |
| sulfur spike XZ offset (max 0.125, y = 0) | match |
| isPathfindable overrides (statue/shelf WATER-only, dried ghast/speleothem false, straw bed/shelf mushroom false via WILD-18, lantern/chain/rod/chest/bars false) | match |
| path type of sulfur spike (WalkNodeEvaluator) | TPHYS-B02 |
| waterlogging: speleothem, shelf, golem statue, dried ghast (GHAST-24 fix: source-only, = native `is(Fluids.WATER)`), resin clump | match |
| washed away by fluids (26.2 content: `blocksMotion` rule identical 26.2/1.21.1; 26.3 content: straw bed fixed WILD-17, shelf mushroom both ages non-solid in 1.21.1 → washed, = 26.3 tag) | match |
| pushReaction (26.3 `POPPED` = 26.2/1.21.1 `DESTROY`, same piston code) | match |
| potent sulfur state machine (DRY/WET/DORMANT/ERUPTING/CONTINUOUS, `isSourceIfFluid`, countdown `10*(w-1)+[15,30]` / `w-1+[1,2]` with the extra `nextInt()`, %20 cadence, salt -904011478, block event / sounds) | match |
| geyser launch per tick (`+0.2F` while `dy < 0.3F + w*0.1`, force height `6*w` with collision-shape passability, AABB) | math match; server half TPHYS-B03 |
| noxious gas (every 10 ticks, AABB inflate 2.5/0/2.5, dist² ≤ 9, nausea 80 t amp 0 ambient+visible, LOS fixed to COLLIDER by SULFUR-26) | match |
| geyser / gas tags (`causes_*_geyser_eruptions`, `not_affected_by_geysers`) | match |
| sulfur spike falling: 2-tick/1-tick schedule, `Math.max(1+dy, 6)` damage per distance, cap 40, `falling_stalactite` source, FallingBlockEntity damage formula | match |
| sulfur spike growth / tip merge / trident break (SULFUR-21/22/23/24 fixes re-checked against native code) | match |
| dried ghast hydration (5000-tick schedule from randomTick, ±1 hydration, spawn at bottom centre with block yaw) | match |
| hanging moss / mossy carpet support (`MultifaceBlock.canAttachTo` identical) | match |
| leaf litter / cactus flower support rules | match |
| BlockSetType pale_oak / poplar (defaults), button 30 ticks, pressure plates forceSolid + noCollision | match |
| `#suppresses_bounce` (honey only, all trees) | match |
| Spear registration args (attackDuration, damageMultiplier, delay, knockback/damage time+threshold), 7 tiers | match (dismount fixed by MAYHEM-01 / COPPER-21) |
| Spear attributes: damage `0 + tier bonus`, speed `1/attackDuration - 4` | match |
| Spear durability / repair / enchantability (Tiers vs ToolMaterial, incl. copper 190/5.0/1.0/13) | match |
| Spear `Weapon(1)` durability on hit (`postHurtEnemy` damage 1, MAINHAND) | match (VB `SpearItem.postHurtEnemy`) |
| `piercing_weapon`, `attack_range` (2.0/4.5/2.0/6.5/0.125/0.5), `minimum_attack_charge` 1.0, `swing_animation` STAB | match (codec defaults fixed by MAYHEM-39) |
| `use_effects` canSprint / speedMultiplier | match (MAYHEM-40) |
| `use_effects.interact_vibrations = false` | TPHYS-C04 |
| `KineticWeapon.damageEntities` formula `(float)baseMobDamage + Mth.floor(relativeSpeed * damageMultiplier)`, `Condition.test`, contact cooldown 10 | match |
| `ProjectileUtil.getHitEntitiesAlong` / `getManyEntityHitResult`, `AttackRange.effectiveMin/MaxRange`, `isInRange` | match |
| `PiercingWeapon.canHitEntity` | TPHYS-C07 |
| Player stab: magic boost, `baseDamageScaleFactor` `0.2F + s*s*0.8F`, deflect, exhaustion 0.1, stats, hearts | match |
| Stab knockback, second `causeExtraKnockback` power | TPHYS-C02 |
| Player stab post-attack enchant target for dragon parts | TPHYS-C08 |
| Mob stab damage source / attack sound | match (MAYHEM-02 / MAYHEM-04 fixes) |
| Spear damage type `minecraft:spear` JSON (exhaustion 0.1, scaling, message id) + lang keys | match (MAYHEM-02) |
| Spear in `#no_knockback`, `#is_player_attack`, `#sulfur_cube_with_block_immune_to` | match (MAYHEM-02, SULFUR-10) |
| Spear via `#is_player_attack` in `#panic_causes` and `#can_break_armor_stand` | TPHYS-C01 (incomplete MAYHEM-02 fix) |
| `sulfur_cube_hot` JSON and tags (is_fire, bypasses_shield, no_knockback, burn_from_stepping, panic_environmental_causes, immune) | match (SULFUR-11 fixed) |
| `#sulfur_cube_with_block_immune_to` full list | match (SULFUR-10 fix; `mace_smash` is `player_attack` in 1.21.1) |
| Lunge enchantment: costs, weight, max level, slots, exhaustion `4 + 4/level`, impulse `0.458/level` `[0,0,1]` scale `[1,0,1]`, item damage 1, food >= 7 | match |
| Lunge: no lunge while in water | match (VB hard-codes `!user.isInWater()` in `EnchantmentUtils.doPostPiercingAttackEffects`) |
| Lunge: elytra-only check (`is_fall_flying`) | TPHYS-C03 |
| Lunge impulse grace time on non-players | TPHYS-CE02 |
| `ApplyEntityImpulse` local-coordinate math | match |
| Enchantment applicability (sharpness, smite, bane, knockback, fire aspect, looting, unbreaking, mending, vanishing, sweeping) | match (VB tags spears into the 1.21.1 `enchantable/sword`, `sharp_weapon`, `weapon`, `durability`; MAYHEM-10 for sweeping) |
| Enchantment tags: lunge in non_treasure / tradeable / tooltip_order | match (MAYHEM-11) |
| Exclusive sets (damage) | match |
| Copper armor (durability 11, defense 1/3/4/2, body 4, ench 8, toughness 0, KB 0) | match |
| Copper horse armor, copper/iron/gold/diamond nautilus armor (BODY values), netherite nautilus 19 | match |
| Nautilus armor not damaged on hurt | match (no max damage) |
| ChargeAttack (nautilus 0.6, zombie nautilus 0.5, force 2.0, 12/11, cooldown 80, damage, knockback via `causeExtraKnockback`) | match after MAYHEM-12/13/14 |
| Nautilus / zombie nautilus attributes | TPHYS-C09 (float literal) |
| Nautilus anger on hurt | match |
| Sulfur cube archetype knockback (angles 1.6/0.5/0.8, sqrt(damage), extra x `power*0.25`, x0.4, clamp 128, y x1.2) | match after SULFUR-04 |
| Sulfur cube knockback-only stab | TPHYS-C05 |
| Sulfur cube explosion (power 3, no fire, fuse 120, y+0.0625, TNT/NONE by mobGriefing, portal calculator, damage source) | match (gate SULFUR-15, engine) |
| Sulfur cube contact damage (`sulfur_cube_hot` 1.0, unattributed, on push and player push) | match |
| Creaking damage 3, cooldown 40, attack animation 15, invulnerability animation 8, heart-bound hurt rules | match |
| Creaking knockback while frozen | TPHYS-C06 |
| Creaking movement speed literal | TPHYS-C09 |
| Parched (16 HP, Weakness 600 arrows, intervals 50/70, weakness immunity) | match (MAYHEM-30) |
| Camel husk / zombie horse / copper golem / happy ghast attributes | match |
| Entity type tags for combat (undead via skeletons/zombies, aquatic for impaling) | match |
| Shield blocking of stabs, shield disable (spears never disable) | match (engine path equal) |
| Piglin golden spear spawn roll (`nextFloat() < 0.5`, `nextInt(10) == 0`) and fight-activity order | match |
| Piglin melee while holding a spear | TPHYS-CE01 |
| Mob melee range with spear (`isWithinMeleeAttackRange` via attack_range x mob_factor) | match |
| Zombie/drowned spear goals | match (MAYHEM-07/08) |

## Findings

### Entity physics
### TPHYS-A01  Copper golems take fall damage (native: immune)
- severity: farm
- vb: src-vb/data/minecraft/tags/entity_type/fall_damage_immune.json:2-4 `"values": [ "minecraft:happy_ghast" ]` (the only VB addition; the add-on adds nothing to this tag); 1.21.1 tag (src-121data) lists iron_golem, snow_golem, … breeze but not copper_golem. VB `CopperGolem` has no `causeFallDamage`/`calculateFallDamage` override.
- native: src-mc/data/minecraft/tags/entity_type/fall_damage_immune.json:3 `"minecraft:copper_golem",`; LivingEntity.java:1858-1860 `protected int calculateFallDamage(...) { if (this.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) { return 0; }`
- difference: In VB a copper golem (12 HP, safe fall 3) takes `ceil(fall - 3)` damage: a 10-block drop costs 7 HP and a 15-block drop kills it. Native golems never take fall damage. Sorting systems that drop golems between floors, or golems knocked off ledges, lose golems only in VB.
- fix_kind: data-override (data/minecraft/tags/entity_type/fall_damage_immune.json)
- fix: `{ "replace": false, "values": [ { "id": "minecraft:copper_golem", "required": false } ] }` (1.21.1's `calculateFallDamage` already reads this tag, LivingEntity.java:1665).
- config_gate: has_copper_golems

### TPHYS-A02  Bouncy sulfur cubes on slime blocks lose the slime step slow-down (SULFUR-13 fix incomplete)
- severity: farm
- vb/addon: src/main/java/com/coolfreeze23/vbparity/sulfur/mixin/EntityBounceMixin.java:80 `@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "...Entity;tryCheckInsideBlocks()V"))` → :95-100 `Vec3 current = this.vbp$velocityBeforeCollide; … double x = this.vbp$xCollision ? -current.x * restitution : current.x; double z = … : current.z;` → :125 `this.setDeltaMovement(x, y, z);`. In 1.21.1 `Entity.move` (src-121 Entity.java:682 `block.updateEntityAfterFallOn(...)`, :686 `block.stepOn(this.level(), blockpos, blockstate, this);`, :728 `this.tryCheckInsideBlocks();`) the slime `stepOn` (SlimeBlock.java:56-60 `if (d0 < 0.1 && !entity.isSteppingCarefully()) { double d1 = 0.4 + d0 * 0.2; entity.setDeltaMovement(entity.getDeltaMovement().multiply(d1, 1.0, d1)); }`) runs before the add-on's restitution, which then overwrites x/z with the pre-collision velocity.
- native: Entity.java:794-797 restitution runs inside `move`; `stepOn` is no longer in `move` but in `applyEffectsFromBlocks` (Entity.java:960-965 `if (this.onGround()) { … effectState.getBlock().stepOn(this.level(), effectPos, effectState, this); }`), called from LivingEntity.aiStep after travel (LivingEntity.java:3139-3145). Native therefore applies the slime factor `0.4 + |vy|*0.2` to the post-bounce, post-drag velocity.
- difference: For every cube with BOUNCINESS > 0 (all archetypes except sticky) that rests on or slides over a slime block (|vy| < 0.1 after the floor bounce is zeroed), native multiplies x/z by ≈0.416 per tick (`vy = -0.08*0.98`), VB+add-on by 1.0. A cube pushed onto slime keeps sliding at slime friction `0.8*0.91*airdrag` instead of stopping within 2-3 ticks. Cube sorters/transport lines built on slime behave differently.
- fix_kind: mixin-mc (net.minecraft.world.entity.Entity#move, net.minecraft.world.entity.LivingEntity#aiStep), scoped to the add-on's bouncy entities
- fix: (1) In EntityBounceMixin add `@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))` that skips the call when `vbp$isBouncy(self)`. (2) New `@Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))` plus the same after `travelRidden` (the `label104` block in 1.21.1 LivingEntity.aiStep), running only for `vbp$isBouncy(this) && (!level().isClientSide() || isControlledByLocalInstance()) && onGround()`: `BlockPos p = getOnPosLegacy(); BlockState s = level().getBlockState(p); s.getBlock().stepOn(level(), p, s, this);` (native applyEffectsFromBlocks order: stepOn before the inside-block checks). Magma/turtle-egg/redstone-ore stepOn effects move with it, as natively.
- config_gate: none (sulfur cubes: has_sulfur_cubes for spawning only)

### TPHYS-A03  Baby nautilus passenger attachment is scaled (0.56875) instead of native 0.5
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/level/entities/mob/animal/nautilus/Nautilus.java:47-48 `return this.isBaby() ? super.getDefaultDimensions(pose).scale(0.5F) : super.getDefaultDimensions(pose);` (type attachment `passengerAttachments(new float[]{1.1375F})`, ModEntityTypes.java NAUTILUS) → passenger point 1.1375 × 0.5 = 0.56875.
- native: src-mc/net/minecraft/world/entity/animal/nautilus/Nautilus.java:26-29 `BABY_DIMENSIONS = EntityTypes.NAUTILUS.getDimensions().scale(0.5F).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.5F, 0.0F));`
- difference: Width 0.4375, height 0.475 and eye height 0.13755 match; a passenger on a baby nautilus sits 0.06875 higher in VB (0.56875 vs 0.5). Only reachable by mobs/commands (babies can't be saddled), so low impact.
- fix_kind: mixin-vb (com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.Nautilus#getDefaultDimensions)
- fix: `@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)`: if `isBaby()` return a static `ModEntityTypes.NAUTILUS.get().getDimensions().scale(0.5F).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.5F, 0.0F))` (build lazily; the type supplier is registered later than the mixin class loads).
- config_gate: has_nautilus

### TPHYS-A04  VB pale-oak and poplar boats/chest boats have eye height 0.478125 (native 0.5625)
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/registries/entities/ModEntityTypes.java:27 `"custom_boat", Builder.of(CustomBoat::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10)` and :30 same for `custom_chest_boat` — no `.eyeHeight(...)`, so `EntityDimensions.scalable` default `0.85 × 0.5625 = 0.478125`.
- native: src-mc/net/minecraft/world/entity/EntityTypes.java:716-722 PALE_OAK_BOAT `.sized(1.375F, 0.5625F).eyeHeight(0.5625F)` (chest boat the same); src-mc263 EntityTypes.java:809-824 POPLAR_BOAT / POPLAR_CHEST_BOAT `.sized(1.375F, 0.5625F).eyeHeight(0.5625F)`; 1.21.1 vanilla boats also use `.eyeHeight(0.5625F)` (src-121 EntityType.java:220,246).
- difference: Eye height feeds 1.21.1 `Boat.getLeashOffset()` (src-121 Boat.java:457-458 `0.88F * this.getEyeHeight()` → 0.4208 vs 0.495), `getEyeY`/eye-in-fluid state and every eye-anchored check (e.g. `/tp ... facing`, eyes anchor). Only VB's two wood types differ from all other boats.
- fix_kind: mixin-vb (com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes `<clinit>`)
- fix: `@ModifyExpressionValue` (or `@Redirect`) on the two `EntityType$Builder.sized(FF)` results inside `<clinit>` for ordinals of custom_boat/custom_chest_boat, appending `.eyeHeight(0.5625F)`; the add-on's ghast `ModEntityTypesMixin` already patches this `<clinit>` (passenger seats), so add the two handlers there.
- config_gate: none (pale_garden / wilderness-bound content toggles only gate generation)

### Block physics
### TPHYS-B01  Shelf mushroom bounce: no rest cut-off and no gravity/drag compensation (WILD-24 fix incomplete)
- severity: gameplay
- vb/addon: `src/main/java/com/coolfreeze23/vbparity/wild/mixin/ShelfMushroomBlockMixin.java` (`vbparity$wild$nativeBounce`, replaces VB `ShelfMushroomBlock.bounceUp` at `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/common/level/blocks/ShelfMushroomBlock.java:101`)
  ```java
  if (movement.y < 0.0) {
      double bounciness = entity instanceof LivingEntity ? 1.0 : 0.8;
      entity.setDeltaMovement(movement.x, -movement.y * 0.75 * bounciness, movement.z);
  ```
  The javadoc says the cut-off and compensation are "26.x engine" and were not ported.
- native: `<mc-26.3-pre-1>/net/minecraft/world/entity/Entity.java:857-876` (`restituteMovementAfterCollisions`), block value `Blocks.SHELF_MUSHROOM ... .bounceRestitution(0.75F).fallDistanceReduction(0.5F)`
  ```java
  restitution = !(-currentMovement.y <= this.getEffectiveGravity()) && !this.isSuppressingBounce() && !effectState.is(BlockTags.SUPPRESSES_BOUNCE)
     ? Math.max(restitution, this.getBlockBounciness(effectState.getBlock())) : 0.0;
  ...
  double portionWithMovement = movement.y / currentMovement.y;
  gravityCompensation = portionWithMovement * this.getEffectiveGravity();
  effectiveDrag = Mth.lerp(portionWithMovement, 1.0, (double)this.getAirDrag());
  ...
  movementAfterBounce = movementAfterBounce.with(Direction.Axis.Y, (gravityCompensation - currentMovement.y) * effectiveDrag * restitution);
  ```
- difference: (1) Rest: native sets dy = 0 once `-dy <= gravity` (0.08), so an entity standing on a mushroom is still. VB bounces every tick: a standing mob gets dy −0.0784 → +0.0588 → … and oscillates with `onGround` flipping, which changes jumps, sprint-jumps and mob AI on mushrooms. (2) Real bounce: native y = `(p·g − dy)·lerp(p,1,0.98)·0.75`, VB y = `−dy·0.75`. Example: dy −0.5, p = 0.5 → native 0.40095, VB 0.375 (+6.9% in native). Both pieces work in 1.21.1: `Entity.getGravity()` exists, the air drag is the 0.98 that `LivingEntity.travel` uses (0.8 for non-living is already applied), and the collided movement is available in `Entity.move`.
- fix_kind: mixin-vb (`ShelfMushroomBlock.bounceUp`) + mixin-mc (`Entity.move`, capture of the collided movement)
- fix: reuse the capture in `sulfur/mixin/EntityBounceMixin` (`vbp$captureCollision`, which today runs only for `vbp$isBouncy`). Widen that capture to every entity whose `getOnPosLegacy()` state is a `ShelfMushroomBlock`, or keep `movement.y` in a ThreadLocal. In `vbparity$wild$nativeBounce` use: `Vec3 cur = entity.getDeltaMovement(); double g = entity.getGravity(); if (cur.y < 0 && -cur.y > g && !entity.isSuppressingBounce()) { double r = 0.75 * (entity instanceof LivingEntity ? 1.0 : 0.8); double p = collidedY / cur.y; double drag = Mth.lerp(p, 1.0, 0.98) /* native getAirDrag(): 0.98 for LivingEntity and ItemEntity/most entities */; entity.setDeltaMovement(cur.x, (p * g - cur.y) * drag * r, cur.z); playSound(...) } else if (cur.y < 0) entity.setDeltaMovement(cur.x, 0.0, cur.z);`. The `<=` cut-off is the 26.3 one (26.2 used `<`, see TPHYS-BE01). The sound gate stays as already fixed.
- config_gate: none

### TPHYS-B02  Sulfur spikes are BLOCKED path nodes instead of DAMAGE_CAUTIOUS
- severity: farm
- vb: `<mc-1.21.1>/net/minecraft/world/level/pathfinder/WalkNodeEvaluator.java:520` (engine code VB runs; no VB or add-on mixin touches it)
  ```java
  } else if (!blockstate.is(Blocks.WITHER_ROSE) && !blockstate.is(Blocks.POINTED_DRIPSTONE)) {
  ```
  so a sulfur spike falls through to `if (!blockState.isPathfindable(LAND)) return BLOCKED` (VB `SpeleothemBlock.isPathfindable` returns false).
- native: `<mc-26.2>/net/minecraft/world/level/pathfinder/WalkNodeEvaluator.java:523` and `:551`, plus `:470` / `:497`
  ```java
  } else if (!blockState.is(Blocks.WITHER_ROSE) && !blockState.is(BlockTags.SPELEOTHEMS)) { ... } else { return PathType.DAMAGE_CAUTIOUS; }
  ...
  case DAMAGE_CAUTIOUS -> PathType.DAMAGE_CAUTIOUS;          // node standing on a spike
  if (pathType == PathType.DAMAGE_CAUTIOUS) return PathType.DAMAGE_CAUTIOUS;   // any of the 26 neighbours
  ```
- difference: natively the spike block, any node on top of it, and every node with a spike among its 3×3×3 neighbours is DAMAGE_CAUTIOUS (malus 0; sniffers −1, so they avoid it). In VB the spike is BLOCKED and its neighbours stay WALKABLE. Mob paths through sulfur-spike caves and spike-lined funnels differ, and sniffers walk next to spikes they would avoid natively.
- fix_kind: mixin-mc (`net.minecraft.world.level.pathfinder.WalkNodeEvaluator#getPathTypeFromState`)
- fix: `@WrapOperation` on the `BlockState.is(Block)` call whose argument is `Blocks.POINTED_DRIPSTONE` (ordinal of the second `is(Block)` in that `else if`): `return original.call(state, block) || state.is(ModBlockTags.SPELEOTHEMS);` (VB tag `minecraft:speleothems` = pointed_dripstone + sulfur_spike, the same as native).
- config_gate: none

### TPHYS-B03  Geyser server half still pushes server players (native: client-only), and sets hasImpulse on skipped entities
- severity: gameplay
- addon: `src/main/java/com/coolfreeze23/vbparity/sulfur/mixin/PotentSulfurBlockEntityMixin.java` (`vbp$skipPlayerDrivenVehicles`, `vbp$normalImpulse` on VB `PotentSulfurBlockEntity` lambda `SERVER_LAUNCH_ENTITY_TICKER`, `<vanillabackport-1.2.2>/com/blackgear/vanillabackport/common/level/block_entities/PotentSulfurBlockEntity.java:118-137`)
  ```java
  if (!(entity.getControllingPassenger() instanceof Player)) { original.call(entity, impulse); }
  ...
  private static void vbp$normalImpulse(Entity entity, boolean value) { entity.hasImpulse = true; }
  ```
- native: `<mc-26.2>/net/minecraft/world/level/block/entity/PotentSulfurBlockEntity.java:124-131` and `<mc-26.2>/net/minecraft/world/entity/Entity.java:3607-3622`, `player/Player.java:1270`
  ```java
  entityToBeLaunched.checkFallDistanceAccumulation();
  if (entityToBeLaunched.canSimulateMovement() && ...) { entityToBeLaunched.addDeltaMovement(new Vec3(0.0, 0.2F, 0.0)); entityToBeLaunched.needsSync = true; }
  // canSimulateMovement() == isLocalInstanceAuthoritative() == (server) !isClientAuthoritative(); Player.isClientAuthoritative() returns true
  ```
- difference: on the server, native never adds the 0.2/tick impulse to a player (players are client-authoritative), only to non-player entities and to vehicles not driven by a player. The add-on still adds it to every `ServerPlayer` in the column, and its client ticker adds it again on the owning client. The server-side velocity is not harmless. 1.21.1 `LivingEntity.knockback` keeps the current server `dy` when the target is airborne (`this.onGround() ? Math.min(0.4, vec3.y / 2.0 + strength) : vec3.y`) and `hurtMarked` then sends that velocity to the client. A player hit while rising in a geyser gets up to `0.5 + 0.1·w` extra upward speed that native does not give. Also, `hasImpulse = true` is written even when the push was skipped (flying players, passengers, tagged mobs, fast entities), which causes extra motion broadcasts. Native sets `needsSync` only inside the `if`.
- fix_kind: mixin-vb (`PotentSulfurBlockEntity` lambda `lambda$static$5`)
- fix: in `vbp$skipPlayerDrivenVehicles` call `original` only when `!(entity instanceof Player) && !(entity.getControllingPassenger() instanceof Player)`. Drop the `PUTFIELD hurtMarked` redirect's unconditional write, and set `entity.hasImpulse = true` inside the wrapped call right after `original.call`, with the `hurtMarked` PUTFIELD redirected to a no-op. `checkSlowFallDistance` stays unconditional (it runs before the test, as in native).
- config_gate: `do_geysers_erupt`

### TPHYS-B04  Bouncy sulfur cubes ignore the shelf mushroom's 0.75 restitution
- severity: gameplay (minor)
- addon: `src/main/java/com/coolfreeze23/vbparity/sulfur/mixin/EntityBounceMixin.java` (`vbp$blockBounciness`)
  ```java
  if (block == Blocks.SLIME_BLOCK) { return 1.0F; }
  return block instanceof BedBlock ? 0.75F : 0.0F;
  ```
  This runs after VB's `ShelfMushroomBlock.updateEntityAfterFallOn` and overwrites its result (`setDeltaMovement(x, y, z)` from the pre-collision velocity).
- native: `<mc-26.3-pre-1>/net/minecraft/world/entity/Entity.java:860` `Math.max(restitution, this.getBlockBounciness(effectState.getBlock()))`; `Blocks.SHELF_MUSHROOM ... .bounceRestitution(0.75F)` (26.3 `Blocks.java`, the only block besides beds and slime that has one)
- difference: a sulfur cube whose BOUNCINESS attribute is below 0.75 landing on a shelf mushroom bounces at its own restitution in the add-on and at 0.75 natively (26.3, where both exist).
- fix_kind: mixin-mc (add-on's own `EntityBounceMixin`)
- fix: `if (block instanceof com.blackgear.vanillabackport.common.level.blocks.ShelfMushroomBlock) return 0.75F;` in `vbp$blockBounciness`. The cut-off for that block is 26.3's `-dy <= g`, not the 26.2 `< g` used for slime and beds.
- config_gate: none

### Combat math
### TPHYS-C01  Spear hits don't make mobs panic and can't break armor stands (MAYHEM-02 fix incomplete)
- severity: gameplay, farm
- vb: `src/main/resources/data/minecraft/tags/damage_type/is_player_attack.json` — `{"replace": false, "values": ["minecraft:spear"]}`. The add-on's `MayhemParity.weaponDamageSource` (`mayhem/MayhemParity.java:72`) now gives every spear hit (player stab, player left-click via `PlayerSpearMixin.vbp$attackDamageSource`, mob stab, mob melee via `MobSpearDamageMixin`) the `minecraft:spear` type. The 1.21.1 tags that should then include it don't: `src-121data/data/minecraft/tags/damage_type/panic_causes.json` lists `"minecraft:mob_attack"`, `"minecraft:player_attack"`, ... and no tag reference; `src-121data/data/minecraft/tags/damage_type/can_break_armor_stand.json` = `["minecraft:player_attack", "minecraft:player_explosion"]`. The add-on writes neither file.
- native: `src-mc/net/minecraft/data/tags/DamageTypeTagsProvider.java:159` `this.tag(DamageTypeTags.CAN_BREAK_ARMOR_STAND).add(DamageTypes.PLAYER_EXPLOSION).addTag(DamageTypeTags.IS_PLAYER_ATTACK);` and `:186-210` `this.tag(DamageTypeTags.PANIC_CAUSES).addTag(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES).add(... DamageTypes.MOB_ATTACK ...).addTag(DamageTypeTags.IS_PLAYER_ATTACK);` with `:175` `IS_PLAYER_ATTACK.add(DamageTypes.PLAYER_ATTACK, DamageTypes.SPEAR, DamageTypes.MACE_SMASH)`.
- difference: natively a spear hit (stab, charge or mob melee) is in `#panic_causes` (via `#is_player_attack`), so `PanicGoal`/`AnimalPanic` run. With the add-on, animals hit by a spear don't panic. Before MAYHEM-02 VB used `player_attack`/`mob_attack`, so this is a regression from the fix. Also `ArmorStand.hurt` (`src-121/.../ArmorStand.java:418`, `CAN_BREAK_ARMOR_STAND`) returns false for spear hits, so a spear never breaks an armor stand. Natively it takes 2 hits (`lastHit` 5-tick rule), or 1 in creative.
- fix_kind: data-override
- fix: add `data/minecraft/tags/damage_type/panic_causes.json` = `{"replace": false, "values": ["#minecraft:is_player_attack"]}` and `data/minecraft/tags/damage_type/can_break_armor_stand.json` = `{"replace": false, "values": ["#minecraft:is_player_attack"]}`. In 1.21.1 `#is_player_attack` is `player_attack` + the add-on's `spear`, which equals native minus `mace_smash` (not a type in 1.21.1).
- config_gate: VB spears feature (`hasSpears` / the spear experiment toggle); none for the tags themselves

### TPHYS-C02  Stab bonus knockback is twice native (1.21.1 `getKnockback` is not halved)
- severity: gameplay
- vb: `src-vb/com/blackgear/vanillabackport/core/mixin/common/spear_behavior/PlayerMixin.java:164-165` (`vb$stabAttack`) `this.vb$causeExtraKnockback(target, 0.4F, oldMovement); this.vb$causeExtraKnockback(target, this.getKnockback(target, damageSource), oldMovement);` and the same at `LivingEntityMixin.java:164-165`. 1.21.1 `getKnockback` (`src-121/net/minecraft/world/entity/LivingEntity.java:1504-1508`): `float f = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK); return ... EnchantmentHelper.modifyKnockback(serverlevel, this.getWeaponItem(), attacker, damageSource, f) : f;` (no /2. 1.21.1 callers multiply by 0.5 themselves: `Mob.java:1500-1503` `knockback((double)(f1 * 0.5F), ...)`).
- native: `src-mc/net/minecraft/world/entity/LivingEntity.java:1550-1554` `return this.level() instanceof ServerLevel level ? EnchantmentHelper.modifyKnockback(level, this.getWeaponItem(), target, damageSource, knockback) / 2.0F : knockback / 2.0F;`, used raw at `LivingEntity.java:2920` and `Player.java:1234` `this.causeExtraKnockback(target, this.getKnockback(target, damageSource), oldMovement, damageSource, totalDamage, true);`.
- difference: Knockback I/II on a spear gives a second stab knockback of power 1.0/2.0 in VB and 0.5/1.0 natively (`knockback.json`: add `linear 1.0 + 1.0/level`). Horizontal push `deltaVector = (x,z).normalize().scale(power)` is therefore twice native. On a block-holding sulfur cube, the second call's multiplier `sqrt(damage) * power * 0.25` is twice native too.
- fix_kind: mixin-vb (`LivingEntityMixin` / `PlayerMixin` merged into `LivingEntity` / `Player`, method `vb$stabAttack`)
- fix: `@ModifyExpressionValue(method = "vb$stabAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F"), remap = false) float half(float kb) { return kb / 2.0F; }` in a LivingEntity mixin and in a Player mixin (target owner `Player` for the Player copy). Leave vanilla `getKnockback` alone, because its 1.21.1 callers already halve.
- config_gate: spears feature

### TPHYS-C03  Lunge never triggers while creative-flying (VB uses `is_flying`, native `is_fall_flying`)
- severity: gameplay
- vb: `src-vb/data/minecraft/enchantment/lunge.json:71-72` `"flags": { "is_flying": false }`. 1.21.1 `EntityFlagsPredicate.matches` (`src-121/net/minecraft/advancements/critereon/EntityFlagsPredicate.java:45-55`): flying = `livingentity.isFallFlying() || livingentity instanceof Player player && player.getAbilities().flying`.
- native: `src-mc/data/minecraft/enchantment/lunge.json:72` `"minecraft:flags": { "is_fall_flying": false }` (and `:81` `"is_in_water": false`). `EntityFlagsPredicate.java:62` checks `livingx.isFallFlying()` only.
- difference: a creative player flying with `abilities.flying` gets the Lunge impulse natively and doesn't in VB. Elytra gliding is excluded in both. The water term is equal (VB hard-codes `!user.isInWater()` in `EnchantmentUtils.doPostPiercingAttackEffects`).
- fix_kind: data-override (`data/minecraft/enchantment/lunge.json`) + a small loot-condition registration
- fix: register a loot condition `vbparity:not_fall_flying` (`LootItemConditionType`, test: `!(ctx.getParam(THIS_ENTITY) instanceof LivingEntity l && l.isFallFlying())`). Override `lunge.json` as the VB file with the `is_flying` term replaced by `{"condition": "vbparity:not_fall_flying"}` (keep the VB effect type ids).
- config_gate: spears feature

### TPHYS-C04  Using a spear emits ITEM_INTERACT_START/FINISH vibrations (native: none)
- severity: farm
- vb: `src-vb/.../common/level/items/spear/SpearItem.java:71` `.component(ModDataComponents.USE_EFFECTS.get(), new UseEffects(true, false, 1.0F))`. VB reads only `canSprint` and `speedMultiplier`; `interactVibrations` is never read (no reference outside `UseEffects.java`). The 1.21.1 engine emits unconditionally: `src-121/net/minecraft/world/entity/LivingEntity.java:3202` `this.gameEvent(GameEvent.ITEM_INTERACT_START);` and `:3324` `this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);`.
- native: `src-mc/net/minecraft/world/item/ItemStack.java:742-746` `public void causeUseVibration(...) { UseEffects useEffects = this.get(DataComponents.USE_EFFECTS); if (useEffects != null && useEffects.interactVibrations()) { causer.gameEvent(event); } }`, called from `LivingEntity.startUsingItem` (`:3518` `this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_START)`) and `stopUsingItem` (`:3633`). The spear sets `new UseEffects(true, false, 1.0F)` (`Item.java:547`).
- difference: each spear charge (player or mob) makes a sculk sensor fire twice in VB, on ITEM_INTERACT_START (frequency 4) and ITEM_INTERACT_FINISH. Natively it makes none. A sculk-based detector or a warden near spear-wielding zombies or piglins sees extra vibrations.
- fix_kind: mixin-mc (`LivingEntity.startUsingItem`, `LivingEntity.stopUsingItem`)
- fix: `@WrapWithCondition` on `LivingEntity.gameEvent(Holder)` in `startUsingItem` (ordinal 0) and in `stopUsingItem`: return `false` when `UseEffects ue = UseEffects.get(useItem)` (use `itemstack` local in start, `this.useItem` in stop) is non-null and `!ue.interactVibrations()`. Items without VB's component keep vanilla behaviour. VB's own `@Inject(..., shift = AFTER)` on the same call still runs.
- config_gate: spears feature

### TPHYS-C05  Knockback-only stab on a block-holding sulfur cube uses plain knockback instead of the archetype path
- severity: gameplay
- vb: `src-vb/.../sulfur_cube/SulfurCube.java:256-257` (`hurt`) `this.pendingKnockbackSource = source; this.pendingKnockbackDamage = damage;` and `:841-843` (`knockback`) `Entity attacker = this.pendingKnockbackSource != null ? this.pendingKnockbackSource.getEntity() : null; float damage = this.pendingKnockbackDamage; if (attacker != null && this.hasBodyItem()) {...} else { super.knockback(power, xd, zd); }`. Add-on `sulfur/mixin/SulfurCubeMixin.java` clears `pendingKnockbackSource` at `tick` HEAD and resets `vbp$knockbackCalls` only at `hurt` HEAD. When a stab has `dealsKnockback && !dealsDamage`, `PlayerMixin.java:162` `boolean wasHurt = dealsDamage && target.hurt(damageSource, totalDamage);` never calls `hurt`.
- native: `src-mc/net/minecraft/world/entity/player/Player.java:1224-1233` `float totalDamage = dealsDamage ? baseDamage + magicBoost : 0.0F; ... this.causeExtraKnockback(target, 0.4F, oldMovement, damageSource, totalDamage, false);`. The mob path (`LivingEntity.java:2919`) passes `postEnchantmentDamage` whatever `dealsDamage` is. `SulfurCube.java:829-830` `knockback(power, xd, zd, source, damage, comesFromEffect) { if (source.getEntity() != null && this.hasBodyItem()) {... float powerMultiplier = Mth.sqrt(damage) * (comesFromEffect ? (float)power * 0.25F : 1.0F);`
- difference: knockback-only stab (attacker speed >= knockback threshold, e.g. 5.1 b/s, but relative speed below the damage threshold 4.6): with a player attacker, native `sqrt(0) = 0`, so the cube isn't moved (the hit sound plays). With a mob attacker, it gets the archetype push with `sqrt(postEnchantmentDamage)`. VB has no pending source, so it applies vanilla `knockback(0.4)` (0.4 horizontal, up to 0.4 vertical) and plays no archetype hit sound. A stale `vbp$knockbackCalls` from the last `hurt` can also scale the first call by `power*0.25`.
- fix_kind: mixin-vb (merged `Player.vb$stabAttack`, `LivingEntity.vb$stabAttack`) + `SulfurCubeMixin` accessor
- fix: `@Inject(method = "vb$stabAttack", at = @At(value = "INVOKE", target = "...vb$causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V", ordinal = 0), remap = false)`. If `target instanceof SulfurCube cube`, set `pendingKnockbackSource = damageSource`, `pendingKnockbackDamage =` (Player: `totalDamage` local; LivingEntity: `postEnchantmentDamage` local) and `vbp$knockbackCalls = 0`. This is a no-op when `hurt` already ran with the same values.
- config_gate: sulfur cube + spears features

### TPHYS-C06  A frozen creaking ignores enchantment, sprint and stab knockback (native: only default knockback is blocked)
- severity: gameplay
- vb: `src-vb/.../creaking/Creaking.java:458-461` `public void knockback(double strength, double x, double z) { if (this.canMove()) { super.knockback(strength, x, z); } }`. This is the only knockback entry point in 1.21.1, so it also catches `Player.attack`'s extra knockback (`src-121/.../Player.java:1295` `livingentity1.knockback(...)`), `Mob.doHurtTarget` (`Mob.java:1502`) and VB stabs (`PlayerMixin.vb$causeExtraKnockback` → `living.knockback(knockback, sin, -cos)`).
- native: `src-mc/net/minecraft/world/entity/monster/creaking/Creaking.java:451-454` overrides only the 5-argument `knockback(power, xd, zd, source, damage)`. `LivingEntity.causeExtraKnockback` (`LivingEntity.java:2750`) and `Player.causeExtraKnockback` (`Player.java:1126`) call the 6-argument `knockback(..., comesFromEffect)`, which the creaking doesn't override.
- difference: natively a creaking that can't move still gets pushed by a Knockback-enchanted hit (`getKnockback/2`), a sprint hit (+0.5), and stab knockback (0.4 plus enchant). Only `dealDefaultKnockback` (0.4) and shield knockback are blocked. In VB all of these are blocked, so a frozen creaking never moves when hit.
- fix_kind: mixin-vb (`Creaking.knockback(DDD)`) + mixin-mc call sites
- fix: add a `ThreadLocal<Boolean> EXTRA_KNOCKBACK`, set around the extra-knockback calls: `Player.attack`'s `LivingEntity.knockback(DDD)` invoke (ordinal 0, the `f4 * 0.5F` one), `Mob.doHurtTarget`'s `knockback(DDD)` invoke, and VB's `vb$causeExtraKnockback` (both Player and LivingEntity copies). Use `@WrapOperation` setting true/false around the call. In `Creaking.knockback(DDD)` add `@ModifyExpressionValue` on the `canMove()` call: `return original || EXTRA_KNOCKBACK.get();`.
- config_gate: pale garden / creaking feature

### TPHYS-C07  Spears can't hit an invulnerable primed sulfur cube
- severity: gameplay
- vb: `src-vb/.../components/PiercingWeapon.java:63` `if (target.isInvulnerable() || !target.isAlive()) { return false; }`
- native: `src-mc/.../component/PiercingWeapon.java:62` `if (target.isInvulnerableToPiercingWeapon() || !target.isAlive())` with `SulfurCube.java:923-925` `public boolean isInvulnerableToPiercingWeapon() { return this.isInvulnerable() && !this.isPrimed(); }`.
- difference: a sulfur cube with `Invulnerable:1` (e.g. a map or build exhibit) that is primed can be jabbed and stab-pushed natively. In VB it can't be targeted. Other entities are equal.
- fix_kind: mixin-vb (`PiercingWeapon.canHitEntity`)
- fix: `@WrapOperation(method = "canHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvulnerable()Z"), remap = false)` → `original.call(e) && !(e instanceof SulfurCube c && c.isPrimed())`.
- config_gate: sulfur cube feature

### TPHYS-C08  Player stab post-attack enchantment effects target the dragon body instead of the hit part
- severity: gameplay
- vb: `src-vb/.../spear_behavior/PlayerMixin.java:241-255` (`vb$itemAttackInteraction`) `Entity hurtTarget = target; if (target instanceof EnderDragonPart part) { hurtTarget = part.parentMob; } ... if (wasHurt) { EnchantmentHelper.doPostAttackEffectsWithItemSource(level, hurtTarget, damageSource, weapon); }`
- native: `src-mc/.../player/Player.java:1084-1097` `Entity hurtTarget = entity; if (entity instanceof EnderDragonPart enderDragonPart) { hurtTarget = enderDragonPart.parentMob; } ... if (applyToTarget) { EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, entity, damageSource, attackingItemStack); }`
- difference: native passes the part as the post-attack "victim". VB passes the parent dragon, so effects that test or act on the victim (Fire Aspect ignite, Lunge-like entity effects, location-based effects at the victim position) apply to a different entity and position.
- fix_kind: mixin-vb (`Player.vb$itemAttackInteraction`)
- fix: `@ModifyArg(method = "vb$itemAttackInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffectsWithItemSource(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;)V"), index = 1, remap = false)` → return the method's `target` argument (`@Local(argsOnly = true, ordinal = 0) Entity target`).
- config_gate: spears feature

### TPHYS-C09  Attribute literals widened from double instead of float
- severity: data
- vb: `src-vb/.../nautilus/AbstractNautilus.java:160` `.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);` and `src-vb/.../creaking/Creaking.java:123` `.add(Attributes.MOVEMENT_SPEED, 0.4)`
- native: `src-mc/.../nautilus/AbstractNautilus.java:116` `.add(Attributes.KNOCKBACK_RESISTANCE, 0.3F);` and `src-mc/.../creaking/Creaking.java:131` `.add(Attributes.MOVEMENT_SPEED, 0.4F)`
- difference: native base values are 0.30000001192092896 and 0.4000000059604645, VB's are 0.3 and 0.4. Knockback on nautili/zombie nautili is scaled by 0.6999999880790710 natively vs 0.7. This is about 1.7e-8 relative and only affects bit-exact comparisons.
- fix_kind: mixin-vb (`AbstractNautilus.createAttributes`, `Creaking.createAttributes`)
- fix: `@ModifyArg(method = "createAttributes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;add(Lnet/minecraft/core/Holder;D)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;", ordinal = 3 /* nautilus KB */ ), index = 1)` → `(double) 0.3F`; same for creaking ordinal 1 → `(double) 0.4F`.
- config_gate: none

## Engine drift

### TPHYS-AE01  Non-bouncy sulfur cubes (sticky archetype, babies, empty adults) bounce on slime blocks and beds with the 1.21.1 formulas
- severity: farm
- 1.21.1 code: src-121/net/minecraft/world/level/block/SlimeBlock.java:47-52 `if (vec3.y < 0.0) { double d0 = entity instanceof LivingEntity ? 1.0 : 0.8; entity.setDeltaMovement(vec3.x, -vec3.y * d0, vec3.z); }`; BedBlock.java:153-157 `entity.setDeltaMovement(vec3.x, -vec3.y * 0.66F * d0, vec3.z);`. The add-on's native restitution only runs for `BOUNCINESS > 0` (EntityBounceMixin.java:132-133 `return entity instanceof LivingEntity living && vbp$attribute(living, ModAttributes.BOUNCINESS, 0.0) > 0.0;`), and the sticky archetype sets bounciness `+0.0` (`archetype(2.0F, 0.0F, 2.0F, 0.01F)`), babies/empty cubes have none.
- native code: Entity.java:813-845 for every entity: on a floor hit `restitution = !(-currentMovement.y < this.getEffectiveGravity()) && !this.isSuppressingBounce() && !effectState.is(BlockTags.SUPPRESSES_BOUNCE) ? Math.max(restitution, this.getBlockBounciness(effectState.getBlock())) : 0.0;` then `y = (portionWithMovement * gravity - currentMovement.y) * Mth.lerp(portionWithMovement, 1.0, getAirDrag()) * restitution`; Blocks.java:684 beds `.bounceRestitution(0.75F)`, :2926 slime `.bounceRestitution(1.0F)`.
- what a farm sees: a sticky cube (or a baby/empty cube) landing on slime at vy = -0.5 with portion p rebounds to `0.5 + 0.08p` (×0.9991 drag for sticky-with-block, ×(1-(0.02·p)) for empty) natively, to exactly 0.5 in VB, so each bounce peaks lower in VB; on beds native uses 0.75 with the same compensation, VB 0.66 without it. Native also cuts the bounce to 0 when `-vy < gravity` (VB keeps micro-bounces). Slime/bed launchers and bounce-sorters for these cubes give different heights.
- fix: scoped to VB's sulfur cubes: widen `vbp$isBouncy` to `entity instanceof SulfurCube || BOUNCINESS > 0` (the restitution code already takes `max(restitution, block bounciness)` and handles restitution 0 as `y = 0`), and cancel 1.21.1 `SlimeBlock/BedBlock.updateEntityAfterFallOn` for those entities (wrap `Block.updateEntityAfterFallOn` in `Entity.move` the same way). Do not extend it to other entities: native's formula is an engine-wide change for every mob, player and item.
- config_gate: none

### TPHYS-BE01  Block bounce is one generic restitution formula in 26.x (slime, beds, shelf mushroom)
- native 26.2: `<mc-26.2>/net/minecraft/world/entity/Entity.java:828` `restitution = !(-currentMovement.y < this.getEffectiveGravity()) && ...` with `(gravityCompensation - currentMovement.y) * effectiveDrag * restitution`; beds `.bounceRestitution(0.75F)`, slime `1.0F`. 26.3-pre-1: the same at `src-mc263/.../Entity.java:859`, but the gate is `<=`.
- 1.21.1: `SlimeBlock.bounceUp` `-vec3.y * d` (d = 1.0 living / 0.8), `BedBlock.bounceUp` `-vec3.y * 0.66F * d`.
- farm impact: vanilla slime launchers and bed bounces differ in every 1.21.1 world. That is not VB content, so no fix here. The add-on already ports the native formula for VB's bouncy sulfur cubes (SULFUR-13). For VB's shelf mushroom see TPHYS-B01. A port for all entities would change vanilla, so it is not proposed.

### TPHYS-BE02  26.3 replaced the "washed away" rule with `#washed_away_by_fluids`
- native 26.3: `<mc-26.3-pre-1>/net/minecraft/world/level/material/FlowingFluid.java:406-408` `return block instanceof LiquidBlockContainer ? true : state.is(BlockTags.WASHED_AWAY_BY_FLUIDS);`
- 26.2 / 1.21.1: `!state.blocksMotion()` plus the door/sign/ladder/sugar-cane/portal exclusions (`src-mc/.../FlowingFluid.java:407-424`, `src-121/.../FlowingFluid.java:407-425`).
- farm impact on VB content: none left. 26.2 content follows the old rule on both sides. For 26.3 content, the straw bed (solid in 1.21.1) is fixed by WILD-17, and the shelf mushroom (bounds average 0.417 at age 0 and 0.604 at age 1, below 0.729) is already washed away in 1.21.1. No fix needed.

### TPHYS-CE01  Spear-holding piglins also do normal melee hits
- native: `src-mc/net/minecraft/world/entity/ai/behavior/MeleeAttack.java:28-29` `if (canAttackPredicate.test((T)body) && !isHoldingUsableNonMeleeWeapon(body) && ...` with `:45-46` `return body.isHolding(body::canUseNonMeleeWeapon);` and `Piglin.java:357-358` `return item.getItem() == Items.CROSSBOW || item.has(DataComponents.KINETIC_WEAPON);`
- 1.21.1: `src-121/net/minecraft/world/entity/ai/behavior/MeleeAttack.java:28` `if (!isHoldingUsableProjectileWeapon(p_258540_) && ...` with `:44-48` `item instanceof ProjectileWeaponItem && mob.canFireProjectileWeapon(...)`. VB adds the spear behaviours (`PiglinAiMixin`, index 3-5) and the 1-in-10 golden spear (`PiglinMixin.vb$spawnWithSpear`), but `MeleeAttack.create(20)` still fires.
- what a player/farm sees: a golden-spear piglin (5% of piglin spawns) does both stab charges and plain melee (piglin attack 5, every 20 ticks) whenever the target is inside `isWithinMeleeAttackRange`, i.e. between `inflate(1.0)` and `inflate(2.25)` (spear range x mob factor 0.5). Natively it only stabs. That's more damage to players and to anything in a piglin/gold farm kill area.
- fix_kind: mixin-mc (`MeleeAttack`), scoped
- fix: `@ModifyReturnValue(method = "isHoldingUsableProjectileWeapon", at = @At("RETURN"))` → `original || (mob instanceof Piglin && mob.isHolding(s -> KineticWeapon.get(s) != null))`. That's native's `Piglin.canUseNonMeleeWeapon` for kinetic weapons only, with no change for other mobs.
- config_gate: spears feature

### TPHYS-CE02  Lunge's post-impulse grace time only reaches players
- native: `src-mc/.../effects/ApplyEntityImpulse.java` `if (entity instanceof LivingEntity livingEntity) { livingEntity.applyPostImpulseGraceTime(10); }`. The impulse context (`currentImpulseImpactPos`, `LivingEntity.java:1802-1855`) lives on every LivingEntity.
- 1.21.1 / VB: `src-vb/.../effects/ApplyEntityImpulse.java` `if (entity instanceof Player player) { ... setCurrentImpulseContextResetGraceTime(Math.max(..., 10)); }`. 1.21.1 keeps the impulse context on `Player` only.
- what a player/farm sees: nothing for players. For a mob that lunges (an enchanted spear on a zombie) while a wind-charge impulse context is active, native extends the fall-damage exemption by 10 ticks; 1.21.1 has no mob impulse context at all. No fix: it can't be scoped without porting the mob impulse context.
- fix_kind: engine
- fix: none.
- config_gate: none
