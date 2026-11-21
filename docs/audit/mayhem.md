# Area: mayhem (Mounts of Mayhem)

**Summary**
- 43 findings: gameplay 21, audio 8, visual 6, data 6, cosmetic 2. The biggest gameplay ones: dismount thresholds are wrong for 6 of 7 spear tiers (MAYHEM-01); spears never use the `minecraft:spear` damage type, so a damaging stab also applies the normal hurt knockback on top of the spear knockback (MAYHEM-02); the ChargeAttack distance arguments are swapped and its vertical recoil is 4x (MAYHEM-12/13); the iron spear that zombies should spawn with gets overwritten by a shovel (MAYHEM-07); parched are not immune to Weakness (MAYHEM-30); server-side player speed never decays in 1.21.1, which feeds the kinetic charge (MAYHEM-32, engine).
- Covered: spear item/components (KineticWeapon, PiercingWeapon, AttackRange, SwingAnimation(Type), UseEffects), spear player/mob mixins, client spear rendering and picking, the AI behaviours (ChargeAttack, Spear*, SpearUseGoal), nautilus and zombie nautilus (entity, AI, variants, model, animation, renderer, armour item, inventory menu), camel husk with its camel sound mixin, parched, zombie-horse mixins, attribute and entity-movement mixins, the breath-of-the-nautilus effect, the lunge enchantment with its effect types, ModAttributes/ModMobEffects/ModDamageTypes, data (loot, recipes, tags, advancements, variants, biome spawns) and sounds.json against `assets-26.2`.
- The 1.21.1 vanilla data used as the base for VB tag appends was extracted (read-only) from the 1.21.1 client jar into a working folder, so tag results are effective (base + VB) against native.
- Not covered: `core/mixin/common/controllable_mounts/**` (mob-controlled mount panic, mount AI), which is outside the listed scope. Husk/camel-husk and drowned/zombie-nautilus natural jockey spawning is left out because GHAST-42/43 already cover it. First-person `ItemInHandRendererMixin` was checked for its math but not for every injection-point ordinal.
- Could not check: the .ogg audio content, and the sulfur-cube consumers of the bounciness, friction and air-drag attributes (another area). The mixins that define those attributes were checked and match.

## Coverage

| VB class / data | Native counterpart | Result |
|---|---|---|
| components/KineticWeapon | world/item/component/KineticWeapon | match (formula, conditions, `HIT_FEEDBACK` 10) |
| components/PiercingWeapon | world/item/component/PiercingWeapon | findings (05) |
| components/AttackRange | world/item/component/AttackRange | findings (39) |
| components/SwingAnimation, SwingAnimationType | component/SwingAnimation, item/SwingAnimationType | match |
| components/UseEffects | component/UseEffects + LocalPlayer.itemUseSpeedMultiplier | findings (40) |
| items/spear/SpearItem + ModItems spear entries | Item.Properties.spear + Items.*_SPEAR | findings (01, 02, 06) |
| items/spear/SpearAnimations | client/model/effects/SpearAnimations | findings (34) |
| api/extensions/entity/spear/** | LivingEntity / Player stab API | match (interfaces) |
| arms/ArmPoses, ItemUseAnimations | HumanoidModel.ArmPose.SPEAR, ItemUseAnimation.SPEAR | match (twoHanded=false, affectsOffhand=true) |
| modifiers/CamelSoundModifier + mixin extension/sound/camel/CamelMixin | Camel getters + CamelHusk overrides | match |
| movement/MotionAwareEntity, TravelAwareEntity, PositionAwareEntity | Entity.getKnownSpeed/getHeadLookAngle | findings (32, 33) |
| ai/behavior/ChargeAttack | ai/behavior/ChargeAttack | findings (12, 13) |
| ai/behavior/SpearApproach, SpearAttack, SpearRetreat, SpearStatus | same names | match |
| ai/goal/SpearUseGoal | ai/goal/SpearUseGoal | match (registration: 08) |
| util/ProjectileUtils.getHitEntitiesAlong / WorldUtilities.getPosAway | ProjectileUtil / LandRandomPos | match |
| mixin spear_behavior/LivingEntityMixin | LivingEntity.stabAttack etc. | findings (02, 04) |
| mixin spear_behavior/PlayerMixin | Player.stabAttack / cannotAttackWithItem | findings (02, 03) |
| mixin spear_behavior/MobMixin | Mob.doHurtTarget / isWithinMeleeAttackRange | findings (02) |
| mixin spear_behavior/EnchantmentHelperMixin | sweeping_edge supported items | findings (10) |
| mixin spear_behavior/ServerPlayerMixin, ServerGamePacketListenerImplMixin | ServerGamePacketListenerImpl STAB / handleAttack | match |
| mixin spear_handler_mob/ZombieMixin | Zombie.addBehaviourGoals/populateDefaultEquipmentSlots | findings (07) |
| mixin spear_handler_mob/ZombifiedPiglinMixin | ZombifiedPiglin.populateDefaultEquipmentSlots | match (1/20 golden spear) |
| mixin spear_handler_mob/PiglinMixin, PiglinAiMixin | Piglin.createSpawnWeapon, PiglinAi.initFightActivity | match (order idx 3-5, RNG order) |
| mixin spear_handler_mob/AbstractSkeletonMixin, DrownedMixin | wantsToPickUp spears=false | match |
| MobIntegrations SpearUseGoal registration | Zombie/ZombifiedPiglin.addBehaviourGoals | findings (08) |
| mixin client spear_behavior/GameRendererMixin | LocalPlayer.raycastHitResult | findings (38) |
| mixin client spear_behavior/LocalPlayerMixin | LocalPlayer.isSlowDueToUsingItem | match for spears (see 40) |
| mixin client spear_behavior/MinecraftMixin, MultiPlayerGameModeMixin | Minecraft.startAttack/continueAttack, MultiPlayerGameMode.piercingAttack | findings (05) |
| mixin client spear_rendering/* | HumanoidModel, HumanoidMobRenderer, AbstractZombieRenderer, AvatarRenderer, ItemInHandLayer, ItemInHandRenderer, AnimationUtils | findings (34, 35, 36) |
| client SpearRenderer + spear models/textures | assets/minecraft/items/*_spear.json | findings (37); textures identical |
| nautilus/AbstractNautilus | animal/nautilus/AbstractNautilus | findings (16-21) |
| nautilus/Nautilus | animal/nautilus/Nautilus | findings (16, 21) |
| nautilus/NautilusAi | animal/nautilus/NautilusAi | match |
| nautilus/ZombieNautilus | animal/nautilus/ZombieNautilus | findings (16) |
| nautilus/ZombieNautilusAi | animal/nautilus/ZombieNautilusAi | findings (14, 15) |
| nautilus/ZombieNautilusVariant(s) | ZombieNautilusVariants + data/minecraft/zombie_nautilus_variant | match |
| items/NautilusArmorItem + ModItems nautilus armour | Item.Properties.nautilusArmor | match (defence 4/5/7/11/19, toughness, KB res) |
| client NautilusModel, ZombieNautilusCoralModel, NautilusAnimation, renderers | client/model/animal/nautilus/*, NautilusAnimation | match (geometry/keyframes identical); textures: 43 |
| inventory/NautilusInventoryMenu | world/inventory/NautilusInventoryMenu | match (slot positions) |
| camel/CamelHusk | animal/camel/CamelHusk | findings (22, 23) |
| skeleton/Parched | monster/skeleton/Parched | findings (30) |
| mixin zombie_horses/ZombieHorseMixin | animal/equine/ZombieHorse | findings (24-27) |
| mixin zombie_horses/AbstractHorseMixin | handleEating red mushroom (zombie_horse_food) | match |
| mixin zombie_horses/EntityTypeMixin | EntityTypes.ZOMBIE_HORSE MobCategory.MONSTER | match |
| mixin attributes/* (bounciness, friction, air drag, KB-resistance min -2) | Attributes + LivingEntity friction code | match for definitions (consumer behaviour in sulfur-cube area) |
| mixin entity_movement/* | Entity.computeSpeed, ServerPlayer.getKnownSpeed | findings (32, 33) |
| mixin mob_effects/MobEffectUtilMixin, ModMobEffects | MobEffects.BREATH_OF_THE_NAUTILUS (BENEFICIAL, 65518), MobEffectUtil | match (NeoForge breathe hook also freezes air) |
| enchantment effects ApplyEntityImpulse / ApplyExhaustion / PlaySoundEffect | item/enchantment/effects/* | match (impulse grace only on players: engine, mobs have no impulse context in 1.21.1) |
| registries/enchantment/* + data enchantment/lunge.json | data/minecraft/enchantment/lunge.json | match (is_in_water done in code); tags: 10, 11 |
| ModDamageTypes | data/minecraft/damage_type/spear.json | findings (02): `spear` missing |
| ModAttributes | Attributes | match |
| triggers/SpearMobsTrigger | advancements/criterion/SpearMobsTrigger | findings (41) |
| loot nautilus/zombie_nautilus/camel_husk/parched | same | match (pool vs entry condition only) |
| recipes spears / netherite smithing / nuggets | data/minecraft/recipe/* | findings (42) |
| tags item/entity_type/enchantment | same | findings (09, 10, 11) |
| advancements | adventure/kill_a_mob, kill_all_mobs, husbandry/bred_all_animals, nether/all_effects | findings (31) |
| biome spawns (MountsOfMayhemFeatureManager + biome tags) | worldgen/biome/*.json spawners | findings (28) |
| sounds.json entries (70 keys) | assets-26.2 sounds.json | match except 29 |

---

### MAYHEM-01  Spear dismount thresholds wrong for stone..netherite
- severity: gameplay
- vb: src-vb/com/blackgear/vanillabackport/common/registries/items/ModItems.java:186-204 (6th ctor arg = dismountThreshold) — `wooden 14.0F, stone 10.0F, copper 9.0F, iron 8.0F, golden 10.0F, diamond 7.5F, netherite 7.0F`
- native: src-mc/net/minecraft/world/item/Items.java:1659-1678 — `WOOD 14.0F, STONE 13.0F, COPPER 12.0F, IRON 11.0F, GOLD 13.0F, DIAMOND 10.0F, NETHERITE 9.0F` (Item.Properties.spear dismountThreshold)
- difference: `KineticWeapon.dismount_conditions.min_speed` is 3 lower for stone/copper/iron/golden/diamond (2.5 lower for diamond, 2 for netherite). VB spears dismount riders at much lower charge speed. Every other per-tier argument matches.
- fix_kind: mixin-vb (SpearItem.<init>)
- fix: `@ModifyArg(method="<init>", at=@At(value="INVOKE", target="...KineticWeapon$Condition;ofAttackerSpeed(IF)Ljava/util/Optional;", ordinal=0), index=1)` on `SpearItem`, mapping by the `Tier` ctor arg (capture with `@Local(argsOnly=true) Tier tier`): WOOD→14, STONE→13, COPPER→12, IRON→11, GOLD→13, DIAMOND→10, NETHERITE→9. Or rewrite the `KINETIC_WEAPON` component in a `ModifyDefaultComponentsEvent` for each spear with `ofAttackerSpeed((int)(dismountTime*20), nativeValue)`.
- config_gate: has_spears

### MAYHEM-02  Spear hits never use the `minecraft:spear` damage type (double knockback, wrong death message/tags)
- severity: gameplay
- vb: core/mixin/common/spear_behavior/LivingEntityMixin.java:158 — `DamageSource damageSource = this.damageSources().mobAttack(self);`; PlayerMixin.java:149 — `this.damageSources().playerAttack(self)`; ModDamageTypes registers only `sulfur_cube_hot`. Mobs' plain melee with a spear (Mob.doHurtTarget, 1.21.1) also uses `mobAttack`.
- native: Item.java:506 `.delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR)`; LivingEntity.stabAttack `DamageSource damageSource = weaponItem.getDamageSource(this);`, Player.stabAttack `this.createAttackSource(weaponItem)` → `getDamageSource`, Mob.doHurtTarget `weaponItem.getDamageSource(this)`. data/minecraft/damage_type/spear.json `{"exhaustion":0.1,"message_id":"spear","scaling":"when_caused_by_living_non_player"}`, in tags `damage_type/is_player_attack` and `damage_type/no_knockback`. Lang `death.attack.spear` "%1$s was speared by %2$s", `death.attack.spear.item` "%1$s was speared by %2$s using %3$s".
- difference: native spear damage is in `#no_knockback`, so `hurt` applies no default 0.4 knockback and only the two `causeExtraKnockback` calls (0.4 + enchant) push the target. VB's `player_attack`/`mob_attack` also apply the hurt knockback (0.4 from the attacker's position), so every damaging stab or charge knocks back roughly twice. Death messages read "slain by" instead of "speared by", and `#is_player_attack` holds for players only by accident.
- fix_kind: data-override + mixin-vb
- fix: add `data/minecraft/damage_type/spear.json` (above); append `"minecraft:spear"` to `data/minecraft/tags/damage_type/no_knockback.json` and `is_player_attack.json` (`replace:false`); add the two lang keys. Then `@ModifyVariable(method="vb$stabAttack", at=@At("STORE"), ordinal=0)` in both VB `LivingEntityMixin` and `PlayerMixin`: if `weaponItem.is(ModItemTags.SPEARS)` return `new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SPEAR_KEY), self)`. Add a mixin-mc on `Mob.doHurtTarget` (`@ModifyVariable` on the DamageSource local) with the same substitution when `getMainHandItem().is(#spears)`.
- config_gate: has_spears

### MAYHEM-03  Player stab plays PLAYER_ATTACK_STRONG/WEAK, native plays none
- severity: audio
- vb: PlayerMixin.java:191-205 (vb$attackVisualEffects) — `playSound(null, ..., dealsDamage ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK, ...)`
- native: Player.java:1062 attackVisualEffects — `if (!criticalAttack && !sweepAttack && !stabAttack) { playServerSideSound(...STRONG/WEAK) }`; stabAttack passes `stabAttack=true`
- difference: every successful jab or charge hit in VB adds a vanilla strong/weak attack sound on top of `item.spear.hit`. Native plays no player-attack sound for stabs.
- fix_kind: mixin-vb (PlayerMixin.vb$attackVisualEffects)
- fix: `@Redirect`/`@WrapWithCondition` the `Level.playSound(...)` call inside `vb$attackVisualEffects` to do nothing (keep the `magicCrit` branch).
- config_gate: has_spears

### MAYHEM-04  Mob stab hits never call playAttackSound
- severity: audio
- vb: LivingEntityMixin.java:184-189 — `this.setLastHurtMob(target); return true;`
- native: LivingEntity.java:2938-2941 — `this.setLastHurtMob(target); this.playAttackSound(); return true;`
- difference: mobs with an attack sound (e.g. modded ones, Creaking-style overrides) skip it on spear stabs. No vanilla spear-wielder overrides `playAttackSound`, so the effect is limited to mobs given spears by commands or mods.
- fix_kind: mixin-vb (LivingEntityMixin.vb$stabAttack)
- fix: `@Inject(method="vb$stabAttack", at=@At(value="INVOKE", target="...LivingEntity;setLastHurtMob(...)V", shift=AFTER))`, then call `((LivingEntityAccessor)this).callPlayAttackSound()` (1.21.1 has `protected void playAttackSound()` on Mob; invoke it when `this instanceof Mob`).
- config_gate: has_spears

### MAYHEM-05  Jab sound is not client-predicted (attacker hears the server copy late)
- severity: audio
- vb: components/PiercingWeapon.java:54 — `.playSound(null, causer.getX(), ... sound, causer.getSoundSource(), 1.0F, 1.0F)` (also called client-side from MultiPlayerGameModeMixin.piercingAttack)
- native: PiercingWeapon.java:50 — `causer.level().playSound(causer, ...)`; MultiPlayerGameMode.java:520 `weapon.makeSound(this.minecraft.player)`
- difference: native plays the `item.spear.attack` sound locally at once and the server excludes the attacker. In VB the client call with `except=null` plays nothing (the ClientLevelMixin plays only when except==player), and the server sends it to everyone including the attacker, so the attacker hears it one round-trip late.
- fix_kind: mixin-vb (PiercingWeapon.makeSound)
- fix: `@ModifyArg(method="lambda in makeSound", ... index=0)` or `@Overwrite makeSound`: pass `causer` instead of `null` to `SoundExtensions.playSound`. Leave `makeHitSound` at `null`, which matches native.
- config_gate: has_spears

### MAYHEM-06  Spears held by mobs never lose durability
- severity: gameplay
- vb: items/spear/SpearItem.java:139-143 — `if (attacker instanceof Player player && !player.isCreative()) stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);`
- native: Item.java:548 `.component(DataComponents.WEAPON, new Weapon(1))`; ItemStack.postHurtEnemy:549-555 — `if (weapon != null) this.hurtAndBreak(weapon.itemDamagePerAttack(), attacker, EquipmentSlot.MAINHAND);` (any attacker; creative skipped via hasInfiniteMaterials)
- difference: native spears used by zombies, piglins and other mobs wear down 1 per hit (melee via Mob.doHurtTarget → postHurtEnemy). VB spears on mobs never wear.
- fix_kind: mixin-vb (SpearItem.postHurtEnemy)
- fix: replace the body with `stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);`. 1.21.1 `hurtAndBreak` already skips players with infinite materials.
- config_gate: has_spears

### MAYHEM-07  Zombies never actually spawn with an iron spear (overwritten by shovel)
- severity: gameplay
- vb: core/mixin/common/spear_handler_mob/ZombieMixin.java:48 `return value == 3 ? 6 : value;` and :59 `if (hasSpears && chance == 1) this.setItemSlot(MAINHAND, new ItemStack(IRON_SPEAR));` injected at `INVOKE_ASSIGN` right after `nextInt`. The 1.21.1 code then runs `if (i == 0) sword else shovel` (src-121 Zombie.java:388-393).
- native: Zombie.java:388-395 — `rand==0 → IRON_SWORD, rand==1 → IRON_SPEAR, else IRON_SHOVEL`
- difference: VB sets the spear, then the vanilla `else` branch replaces it with an iron shovel. Result: sword 1/6, spear 0, shovel 5/6, against native 1/6, 1/6, 4/6. (Zombie-horse jockeys and camel-husk jockeys set their spear separately and are not affected.)
- fix_kind: mixin-vb (ZombieMixin)
- fix: drop `vb$setSpearEquipment`. Add `@WrapOperation(method="populateDefaultEquipmentSlots", at=@At(value="INVOKE", target="...Zombie;setItemSlot(...)V", ordinal=1))` (the shovel call) that substitutes `new ItemStack(IRON_SPEAR)` when the captured `rand == 1 && hasSpears`.
- config_gate: has_spears

### MAYHEM-08  SpearUseGoal added to Drowned and duplicated on Zombified Piglin
- severity: gameplay
- vb: common/integrations/MobIntegrations.java:76-77 — `registerGoal(mob -> mob instanceof Zombie, 2, SpearUseGoal(...))` and `registerGoal(mob -> mob instanceof ZombifiedPiglin, 1, SpearUseGoal(...))`
- native: Zombie.addBehaviourGoals:121 adds `SpearUseGoal` at 2; Drowned and ZombifiedPiglin override `addBehaviourGoals` (Drowned has none, ZombifiedPiglin has only priority 1 at ZombifiedPiglin.java:73)
- difference: VB drowned (for example a converted zombie keeping its spear) charge with SpearUseGoal, which native drowned never do. Zombified piglins get two SpearUseGoals (priority 1 and 2).
- fix_kind: mixin-vb (MobIntegrations.registerGoals)
- fix: change the first predicate to `mob -> mob instanceof Zombie && !(mob instanceof Drowned) && !(mob instanceof ZombifiedPiglin)` (via `@ModifyArg` on the first `registerGoal` call, index 0).
- config_gate: has_spears

### MAYHEM-09  golden_spear missing from #piglin_loved (and piglin preferred weapons)
- severity: gameplay
- vb: no `data/minecraft/tags/item/piglin_loved.json` in VB; effective 1.21.1 tag lacks `golden_spear`
- native: data/minecraft/tags/item/piglin_loved.json contains `"minecraft:golden_spear"`; tags/item/piglin_preferred_weapons.json `["minecraft:crossbow","minecraft:golden_spear"]`; Piglin.canReplaceCurrentItem:384-396 treats both as wanted
- difference: native piglins admire and pick up golden spears and won't swap a held golden spear for a sword. VB piglins ignore dropped golden spears and may swap a spawned golden spear for a better-damage item.
- fix_kind: data-override (data/minecraft/tags/item/piglin_loved.json)
- fix: `{"replace":false,"values":["minecraft:golden_spear"]}`. The 1.21.1 `Piglin.canReplaceCurrentItem` then treats it as loved, which covers the preferred-weapon behaviour (there is no `piglin_preferred_weapons` tag in 1.21.1).
- config_gate: has_spears

### MAYHEM-10  Sweeping Edge applicable to spears via anvil/books/commands
- severity: data
- vb: data/minecraft/tags/item/enchantable/sword.json `{"values":["#minecraft:spears"]}`; spear_behavior/EnchantmentHelperMixin.java only strips it from `getAvailableEnchantmentResults` (enchanting table)
- native: enchantment/sweeping_edge.json `supported_items: #minecraft:enchantable/sweeping` = `["#minecraft:swords"]`
- difference: in VB a Sweeping Edge book can be put on a spear in an anvil (1.21.1 sweeping_edge supports `#enchantable/sword`, which now includes spears). Native forbids it.
- fix_kind: data-override (data/minecraft/enchantment/sweeping_edge.json)
- fix: copy the 1.21.1 sweeping_edge.json and change `"supported_items"` to `"#minecraft:swords"`. The VB table mixin becomes redundant but is harmless.
- config_gate: has_spears

### MAYHEM-11  Lunge missing from #tooltip_order
- severity: cosmetic
- vb: no `tags/enchantment/tooltip_order.json` in VB, so lunge sorts last in tooltips
- native: tags/enchantment/tooltip_order.json `..., "minecraft:frost_walker", "minecraft:lunge", "minecraft:sharpness", ...`
- difference: tooltip line order on spears.
- fix_kind: data-override (data/minecraft/tags/enchantment/tooltip_order.json)
- fix: `replace:true` with the 1.21.1 list plus `"minecraft:lunge"` inserted after `"minecraft:frost_walker"` (an append cannot place it mid-list).
- config_gate: has_spears

### MAYHEM-12  ChargeAttack charge/detection distances swapped
- severity: gameplay
- vb: ai/behavior/ChargeAttack.java:42-43 ctor `(..., float knockbackForce, double maxTargetDetectionDistance, double maxChargeDistance, SoundEvent)`. The call sites (NautilusAi.java:131, ZombieNautilusAi.java:80) pass `12.0, 11.0`, so detection is 12 and charge is 11.
- native: ChargeAttack ctor `(..., knockbackForce, final double maxChargeDistance, final double maxTargetDetectionDistance, ...)` with `12.0, 11.0` and constants `MAX_CHARGE_DISTANCE = 12.0`, `MAX_TARGET_DETECTION_DISTANCE = 11.0`
- difference: VB nautiluses stop charging after 11 blocks of travel (native 12) and keep a target up to 12 blocks away (native 11).
- fix_kind: mixin-vb (ChargeAttack.<init>)
- fix: `@Inject(method="<init>", at=@At("TAIL"))` swap the two final fields via an accessor (`@Mutable @Final @Shadow`), or `@ModifyArgs` at both call sites to pass `11.0, 12.0`.
- config_gate: has_nautilus

### MAYHEM-13  ChargeAttack knockback multiplies the charger's Y velocity by 4
- severity: gameplay
- vb: ChargeAttack.java:130-134 — `target.knockback(knockback, sin, -cos); entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.6, 4.0, 0.6));`
- native: ChargeAttack.dealKnockBack → `body.causeExtraKnockback(target, speedFactor * knockbackForce, ...)` → LivingEntity.java:2756 `this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));`
- difference: after each hit, a VB nautilus's vertical velocity is quadrupled (native unchanged), so it shoots up or down after charging.
- fix_kind: mixin-vb (ChargeAttack.dealKnockback)
- fix: `@ModifyArg(method="dealKnockback", at=@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"), index=1)` return `1.0`.
- config_gate: has_nautilus

### MAYHEM-14  Zombie nautilus charge speed 0.6 instead of 0.5
- severity: gameplay
- vb: ZombieNautilusAi.java:80 — `new ChargeAttack(80, ..., 0.6F, 2.0F, 12.0, 11.0, ZOMBIE_NAUTILUS_DASH)`
- native: ZombieNautilusAi.java:73 — `new ChargeAttack(80, ..., 0.5F, 2.0F, 12.0, 11.0, ...)` (`SPEED_WHEN_ATTACKING = 0.5F`)
- difference: the zombie nautilus charges 20% faster and hits harder (knockback scales with speed).
- fix_kind: mixin-vb (ZombieNautilusAi.initFightActivity)
- fix: `@ModifyArg(method="initFightActivity", at=@At(value="INVOKE", target="...ChargeAttack;<init>(...)V"), index=2)` → `0.5F`.
- config_gate: has_nautilus

### MAYHEM-15  Zombie nautilus tempt speed 1.3 instead of 0.9
- severity: gameplay
- vb: ZombieNautilusAi.java:60 — `new FollowTemptation(mob -> 1.3F, ...)`
- native: ZombieNautilusAi.java — `new FollowTemptation(mob -> 0.9F, mob -> mob.isBaby() ? 2.5 : 3.5)` (`SPEED_MULTIPLIER_WHEN_TEMPTED = 0.9F`)
- difference: tempted zombie nautiluses move 44% faster.
- fix_kind: mixin-vb (ZombieNautilusAi.initIdleActivity)
- fix: `@ModifyArg` on the `FollowTemptation.<init>(Function,Function)` call, index 0 → `mob -> 0.9F`.
- config_gate: has_nautilus

### MAYHEM-16  Nautilus dash / dash-ready / eat sounds lack volume and pitch variation
- severity: audio
- vb: AbstractNautilus.java:331 `this.playSound(this.getDashReadySound());`, :375 `this.playSound(this.getDashSound());`; Nautilus.java:104 `this.playSound(nautilusEatSound);`; ZombieNautilus.java:97 `this.playSound(ZOMBIE_NAUTILUS_EAT)`
- native: AbstractNautilus.java:309/356 `this.makeSound(...)`; Nautilus.java:118 / ZombieNautilus.java:114 `this.makeSound(...)` → LivingEntity.makeSound `playSound(sound, getSoundVolume(), getVoicePitch())`
- difference: VB uses volume 1, pitch 1. Native uses `getVoicePitch()` = `(r1-r2)*0.2+1` (baby `+1.5` base), giving random pitch variation per play.
- fix_kind: mixin-vb (AbstractNautilus.tick / handleStartJump, Nautilus.playEatingSound, ZombieNautilus.playEatingSound)
- fix: `@Redirect` each `Entity.playSound(SoundEvent)` call in those methods to `if (s != null) self.playSound(s, ((LivingEntityAccessor)self).callGetSoundVolume(), self.getVoicePitch());`.
- config_gate: has_nautilus

### MAYHEM-17  Nautilus dash emits ENTITY_INTERACT instead of ENTITY_ACTION
- severity: gameplay
- vb: AbstractNautilus.java:376 — `this.gameEvent(GameEvent.ENTITY_INTERACT);`
- native: AbstractNautilus.handleStartJump — `this.gameEvent(GameEvent.ENTITY_ACTION);`
- difference: sculk sensors read a frequency of 6 instead of 4, and the event is in different vibration tags.
- fix_kind: mixin-vb (AbstractNautilus.handleStartJump)
- fix: `@ModifyArg(... target="...gameEvent(Lnet/minecraft/core/Holder;)V")` → `GameEvent.ENTITY_ACTION` (exists in 1.21.1).
- config_gate: has_nautilus

### MAYHEM-18  Nautilus saddle/armour equip sounds at half volume and replayed on every inventory change
- severity: audio
- vb: AbstractNautilus.java:604-617 containerChanged — `playSound(getSaddleSoundEvent(), 0.5F, 1.0F)` and `if (isBodyArmorItem(armor) && !armor.isEmpty()) playSound(ARMOR_EQUIP_NAUTILUS, 0.5F, 1.0F)`
- native: LivingEntity.onEquipItem:706-719 `playSeededSound(null, ..., getEquipSound(...), source, 1.0F, 1.0F, seed)`. AbstractNautilus.getEquipSound picks SADDLE_UNDERWATER_EQUIP / SADDLE_EQUIP / equippable `ARMOR_EQUIP_NAUTILUS`. assets-26.2 sounds.json `item.nautilus_saddle_equip`, `item.armor.equip_nautilus` have volume 1.0. Played only when that slot's item changes.
- difference: VB plays both at 0.5 (native 1.0) and replays the armour sound whenever any slot changes while armour is worn (adding or removing the saddle, shift-click).
- fix_kind: mixin-vb (AbstractNautilus.containerChanged)
- fix: `@Overwrite containerChanged`: remember the previous armour stack in a field and play `ARMOR_EQUIP_NAUTILUS` at (1.0, 1.0) only when the new armour differs from the old one and is non-empty. Play the saddle sound at volume 1.0.
- config_gate: has_nautilus

### MAYHEM-19  Body armour can be put on an untamed adult nautilus
- severity: gameplay
- vb: AbstractNautilus.java:632 `public boolean canUseSlot(EquipmentSlot slot) { return true; }`; mobInteract `if (this.canUseSlot(BODY) && isBodyArmorItem(stack) && !isWearingBodyArmor()) equipBodyArmor(...)`
- native: AbstractNautilus.canUseSlot — `slot != SADDLE && slot != BODY ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby() && this.isTame();`
- difference: in VB, right-clicking a wild adult nautilus with nautilus armour equips it. Natively the armour can only go on tamed adults.
- fix_kind: mixin-vb (AbstractNautilus.canUseSlot)
- fix: `@Overwrite canUseSlot`: `return slot == EquipmentSlot.BODY ? this.isAlive() && !this.isBaby() && this.isTame() : true;`
- config_gate: has_nautilus

### MAYHEM-20  Nautilus shearing: owner-only, armour-only, wrong drop point, no trigger
- severity: gameplay
- vb: AbstractNautilus.java:443-449 — `if (stack.is(SHEARS) && !player.isSecondaryUseActive() && this.isOwnedBy(player) && !this.isVehicle() && this.isWearingBodyArmor()) { ...; this.spawnAtLocation(armor, this.getBbHeight() + 0.5F); }`
- native: Entity.java:2310-2315 + Mob.attemptToShearEquipment/shearItem:580-607 — any player, `!isVehicle()`; iterates `EquipmentSlot.VALUES` (BODY before SADDLE, so armour first, then saddle on the next use); spawns at `getAttachments().getAverage(PASSENGER)`; `gameEvent(SHEAR, player)`; `CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT`
- difference: native lets any player shear the armour and then the saddle off; VB allows only the owner, never shears the saddle, drops the item 0.5 above the bounding-box top instead of at the passenger point (1.1375), and emits no SHEAR game event.
- fix_kind: mixin-vb (AbstractNautilus.mobInteract)
- fix: replace the shears branch: if shears, not secondary-use and not vehicle: if armour is worn, remove it; else if saddled, remove `inventory.getItem(0)` (play the saddle-unequip sound if VB registers one, otherwise the armour-unequip sound). Then `stack.hurtAndBreak(1, player, slot)`, `spawnAtLocation(item, 1.1375F)`, `gameEvent(GameEvent.SHEAR, player)`. Drop the `isOwnedBy` check.
- config_gate: has_nautilus

### MAYHEM-21  Nautilus air supply also runs for NoAI and on the client
- severity: gameplay
- vb: Nautilus.java:123-127 — `super.baseTick(); this.handleAirSupply(airSupply);`
- native: Nautilus.baseTick — `if (!this.isNoAi() && this.level() instanceof ServerLevel serverLevel) this.handleAirSupply(serverLevel, airSupply);`
- difference: NoAI nautiluses on land dry out and die in VB (native: never). The client-side air value also desyncs.
- fix_kind: mixin-vb (Nautilus.baseTick)
- fix: `@WrapWithCondition` on the `handleAirSupply(I)V` call: `!this.isNoAi() && !this.level().isClientSide`.
- config_gate: has_nautilus

### MAYHEM-22  Camel husk footsteps 2.5x too loud
- severity: audio
- vb: camel/CamelHusk.java:83/85 — `this.playSound(CAMEL_HUSK_STEP_SAND / CAMEL_HUSK_STEP, 1.0F, 1.0F);`
- native: CamelHusk.playStepSound — `this.playSound(SoundEvents.CAMEL_HUSK_STEP_SAND, 0.4F, 1.0F)` / `CAMEL_HUSK_STEP, 0.4F, 1.0F`. sounds.json volume 0.3 in both trees.
- difference: effective volume 0.3 in VB vs 0.12 native.
- fix_kind: mixin-vb (CamelHusk.playStepSound)
- fix: `@ModifyArg(method="playStepSound", at=@At(value="INVOKE", target="...playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"), index=1)` → `0.4F` (both ordinals).
- config_gate: none (entity exists when has_camel_husks)

### MAYHEM-23  Camel husk saddle sound at half volume
- severity: audio
- vb: CamelHusk.getSaddleSoundEvent → played by 1.21.1 AbstractHorse.containerChanged `playSound(getSaddleSoundEvent(), 0.5F, 1.0F)`; VB sounds.json `entity.camel_husk.saddle` `["mob/horse/leather"]` (volume 1)
- native: Camel.getEquipSound(SADDLE) → getSaddleSound `CAMEL_HUSK_SADDLE` at volume 1.0 (LivingEntity.onEquipItem); assets-26.2 sounds.json `entity.camel_husk.saddle` `["mob/horse/leather"]` (volume 1). Unlike horse/camel saddles, the 0.5 was not moved into the JSON for camel husk.
- difference: 0.5 effective in VB vs 1.0 native.
- fix_kind: mixin-mc (AbstractHorse.containerChanged)
- fix: `@ModifyArg(method="containerChanged", at=@At(value="INVOKE", target="...AbstractHorse;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", ordinal=0), index=1)` → `this instanceof CamelHusk ? 1.0F : v`.
- config_gate: none

### MAYHEM-24  Zombie horses panic when hurt (native never)
- severity: gameplay
- vb: zombie horse keeps 1.21.1 `AbstractHorse.registerGoals` `this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));` (src-121 AbstractHorse.java:163); ZombieHorseMixin and controllable_mounts/PanicGoalMixin only suppress it when mob-controlled
- native: AbstractHorse.registerGoals:136-148 has no PanicGoal; it moved to addBehaviourGoals (`MountPanicGoal`), which ZombieHorse overrides with only FloatGoal + TemptGoal
- difference: hitting an unridden VB zombie horse makes it flee at 1.2x speed; native zombie horses never panic.
- fix_kind: mixin-mc (AbstractHorse.registerGoals)
- fix: `@WrapWithCondition(method="registerGoals", at=@At(value="INVOKE", target="...GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V", ordinal=0))` → `!((Object)this instanceof ZombieHorse)`.
- config_gate: has_zombie_horses

### MAYHEM-25  Baby zombie horses grow up; baby size 0.5 instead of 0.7
- severity: gameplay
- vb: no `canAgeUp`/`getDefaultDimensions` override in ZombieHorseMixin; 1.21.1 ZombieHorse BABY_DIMENSIONS `.withAttachments(PASSENGER, 0, height - 0.03125F, 0).scale(0.5F)`
- native: ZombieHorse.java:44-47 `BABY_DIMENSIONS ... attach(PASSENGER, 0.0F, getHeight() - 0.25F, 0.0F).scale(0.7F)`; `canAgeUp() { return false; }`
- difference: native baby zombie horses (spawn egg on an adult, `/summon` Age<0) stay babies forever and are 0.98x1.12 blocks; VB babies grow up after 20 minutes and are 0.7x0.8.
- fix_kind: mixin-vb (ZombieHorseMixin) + mixin-mc (AgeableMob.aiStep)
- fix: add `getDefaultDimensions(Pose)` to ZombieHorseMixin returning a static `EntityType.ZOMBIE_HORSE.getDimensions().withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER,0,1.6F-0.25F,0)).scale(0.7F)` when baby. In AgeableMob.aiStep, `@WrapOperation` the `setAge(++age)` path for ZombieHorse: when `age < 0`, do not increment. (Age-lock particles belong to the separate age-lock feature.)
- config_gate: has_zombie_horses

### MAYHEM-26  Zombie horse uses horse leash attachment points
- severity: visual
- vb: common/api/modules/leash_behavior/LeashPhysics.java:52 — `entity instanceof AbstractHorse → createQuadOffsets(entity, 0.04, 0.52, 0.23, 0.87)`
- native: ZombieHorse.getQuadLeashOffsets — `Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73)`
- difference: quad-leash knots on zombie horses sit higher and wider than native.
- fix_kind: mixin-vb (LeashPhysics QUAD_LEASH_OFFSETS)
- fix: `@Inject(<clinit> TAIL)` and rebuild the map with a first entry `entity instanceof ZombieHorse → createQuadOffsets(entity, 0.04, 0.41, 0.18, 0.73)` (an ImmutableMap keeps insertion order; the first matching predicate wins).
- config_gate: has_zombie_horses

### MAYHEM-27  Zombie horse speed divisor double vs float
- severity: cosmetic
- vb: ZombieHorseMixin.java:79 — `(9.0 + r + r + r) / 42.16`
- native: ZombieHorse.generateZombieHorseSpeed — `(9.0 + r*1.0 + r*1.0 + r*1.0) / 42.16F` (float literal widened to 42.15999984741211)
- difference: speed is about 3.6e-9 relative higher in native; not observable, but the value is not bit-exact.
- fix_kind: mixin-vb (ZombieHorseMixin.vb$generateZombieHorseSpeed)
- fix: `@ModifyConstant(constant=@Constant(doubleValue=42.16))` → `(double)42.16F`.
- config_gate: has_zombie_horses

### MAYHEM-28  Zombie horses don't spawn in sunflower plains
- severity: data
- vb: data/minecraft/tags/worldgen/biome/spawns_zombie_horses.json `["minecraft:plains","minecraft:snowy_plains","#minecraft:is_savanna"]` used by MountsOfMayhemFeatureManager (weight 5, 1-1, MONSTER)
- native: zombie_horse spawner (monster, weight 5, 1-1) in plains, savanna, savanna_plateau, snowy_plains, **sunflower_plains**, windswept_savanna
- difference: sunflower_plains is missing in VB.
- fix_kind: data-override (data/minecraft/tags/worldgen/biome/spawns_zombie_horses.json)
- fix: `{"replace":false,"values":["minecraft:sunflower_plains"]}`.
- config_gate: has_zombie_horses

### MAYHEM-29  Parrots don't imitate zombie horses
- severity: audio
- vb: CommonSetup.java:70-73 adds parched, camel_husk and zombie_nautilus to `Parrot.MOB_SOUND_MAP`, but not zombie_horse; VB sounds.json lacks `entity.parrot.imitate.zombie_horse`
- native: Parrot.java:123 `map.put(EntityTypes.ZOMBIE_HORSE, SoundEvents.PARROT_IMITATE_ZOMBIE_HORSE)`; sounds.json `entity.parrot.imitate.zombie_horse` `{"sounds":[{"name":"entity.zombie_horse.ambient","pitch":1.8,"type":"event","volume":0.7}],"subtitle":"subtitles.entity.parrot.imitate.zombie_horse"}`
- difference: no imitation sound, because zombie horses are now hostile.
- fix_kind: asset-override (assets/minecraft/sounds.json) + mixin-vb (CommonSetup) / registration
- fix: register a SoundEvent `entity.parrot.imitate.zombie_horse` and add the sounds.json entry above plus a lang subtitle (`"Parrot groans"`, per native en_us `subtitles.entity.parrot.imitate.zombie_horse`). In common setup, `Parrot.MOB_SOUND_MAP.put(EntityType.ZOMBIE_HORSE, that)`.
- config_gate: has_zombie_horses

### MAYHEM-30  Parched are not immune to Weakness (logic inverted)
- severity: gameplay
- vb: skeleton/Parched.java:66 — `return effect.getEffect() == MobEffects.WEAKNESS || super.canBeAffected(effect);`
- native: Parched.canBeAffected — `return newEffect.getEffect() == MobEffects.WEAKNESS ? false : super.canBeAffected(newEffect);`
- difference: VB parched accept Weakness (including from their own tipped arrows hitting each other, and from splash potions); native parched are immune.
- fix_kind: mixin-vb (Parched.canBeAffected)
- fix: `@Inject(method="canBeAffected", at=@At("HEAD"), cancellable=true)`: `if (effect.getEffect() == MobEffects.WEAKNESS) cir.setReturnValue(false);`
- config_gate: has_parched_skeletons

### MAYHEM-31  Advancements missing the new mobs/effect
- severity: data
- vb: VB ships no overrides of adventure/kill_a_mob, adventure/kill_all_mobs, husbandry/bred_all_animals, nether/all_effects (1.21.1 versions are used)
- native: kill_a_mob and kill_all_mobs add criteria `minecraft:camel_husk`, `minecraft:parched`, `minecraft:zombie_horse`, `minecraft:zombie_nautilus` (and `creaking`, which is another area); bred_all_animals adds `minecraft:nautilus`; all_effects adds `"minecraft:breath_of_the_nautilus": {}`
- difference: Monster Hunter / Monsters Hunted don't count these mobs, Two by Two doesn't require nautilus, and How Did We Get Here doesn't require Breath of the Nautilus.
- fix_kind: data-override (data/minecraft/advancement/adventure/kill_a_mob.json, kill_all_mobs.json, husbandry/bred_all_animals.json, nether/all_effects.json)
- fix: copy each 1.21.1 file and add the native criteria, mirroring an existing one (`{"conditions":{"entity":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"type":"minecraft:<id>"}}]},"trigger":"minecraft:player_killed_entity"}` for kills; `bred_animals` with `"child":[...type nautilus]` for breeding), and add each to `requirements` (kill_a_mob: into the single OR group; kill_all_mobs and bred_all_animals: new singleton groups). In all_effects add the effect key.
- config_gate: has_parched_skeletons / has_camel_husks / has_zombie_horses / has_nautilus

### MAYHEM-32  Server-side player known movement never resets in 1.21.1 (charges from a standstill)
- severity: gameplay
- vb: entity_movement/ServerPlayerMixin.getKnownSpeed returns the 1.21.1 field `lastKnownClientMovement`, which is set only in `handleMovePlayer`/`handleMoveVehicle` (src-121 ServerGamePacketListenerImpl.java:462/974) and `startRiding`
- native: ServerGamePacketListenerImpl.java:2194-2201 handleClientTickEnd — `if (!this.receivedMovementThisTick) this.player.setKnownMovement(Vec3.ZERO); this.receivedMovementThisTick = false;`
- difference: the 1.21.1 client stops sending move packets when still, so the last nonzero delta persists for up to 20 ticks until the position reminder. A VB player who sprints and stops still has their charge speed for KineticWeapon conditions and damage (`relativeSpeed * damageMultiplier`) and for the extra jab reach (`getKnownMovement().dot(look)`); native drops it to 0 on the next tick.
- fix_kind: engine (approximable with mixin-mc)
- fix: mixin `ServerGamePacketListenerImpl`: add a `@Unique boolean vb$moved`; set it true at the `setKnownMovement` calls in handleMovePlayer and handleMoveVehicle; `@Inject(method="tick", at=@At("HEAD"))`: `if (!vb$moved) player.setKnownMovement(Vec3.ZERO); vb$moved = false;`. This differs from native only by packet/tick alignment.
- config_gate: has_spears

### MAYHEM-33  Minecart known speed computed after movement (one tick later than native)
- severity: gameplay
- vb: entity_movement/AbstractMinecartMixin.java — `@Inject(method="tick", at=@At("TAIL")) computeSpeed()`
- native: AbstractMinecart.tick:289-292 — `this.checkBelowWorld(); this.computeSpeed(); this.handlePortal(); this.behavior.tick();` (before movement)
- difference: VB's minecart speed is the current tick's motion; native's lags one tick. This affects the kinetic relative-speed math for targets or attackers in minecarts (root vehicle).
- fix_kind: mixin-vb (AbstractMinecartMixin)
- fix: move the injection to `@At(value="INVOKE", target="...AbstractMinecart;handleNetherPortal()V")` (the 1.21.1 equivalent of handlePortal, before `moveAlongTrack`/`comeOffTrack`).
- config_gate: has_spears

### MAYHEM-34  Third-person spear charge animation not interpolated
- severity: visual
- vb: items/spear/SpearAnimations.java:37/41 `entity.getTicksUsingItem()` (int); ItemInHandLayerMixin.java:65 `? entity.getTicksUsingItem() : 0.0F`
- native: HumanoidMobRenderer.java:81 / AvatarRenderer `state.ticksUsingItem = entity.getTicksUsingItem(partialTicks)`; SpearAnimations uses `state.ticksUsingItem`; ItemInHandLayer uses `state.ticksUsingItem(arm)`
- difference: in VB the raise, sway and lower curves step at 20 Hz on other players and mobs (and the local player in F5); native is smooth.
- fix_kind: mixin-vb (SpearAnimations.thirdPersonHandUse, ItemInHandLayerMixin.vb$renderArm)
- fix: replace `entity.getTicksUsingItem()` with `entity.isUsingItem() ? entity.getTicksUsingItem() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true) : 0F` in both places.
- config_gate: has_spears

### MAYHEM-35  Mobs never take the SPEAR arm pose
- severity: visual
- vb: only PlayerRendererMixin returns `ArmPoses.SPEAR`; nothing sets `rightArmPose`/`leftArmPose` for mobs. Zombie, illager, piglin and zombie-villager model mixins only skip `animateZombieArms`.
- native: HumanoidMobRenderer.java:41-48 `if (STAB swing && mob.swinging) SPEAR else itemHeldByArm.is(#spears) ? SPEAR : EMPTY`; AbstractZombieRenderer.java:39-42 also sets SPEAR on the other arm when the opposite hand holds a STAB item
- difference: VB zombies, husks, piglins and zombified piglins holding spears show default dangling arms instead of the couched-spear pose (head-tracking arm with charge sway); the zombie off arm doesn't mirror it.
- fix_kind: mixin-mc (HumanoidModel.prepareMobModel)
- fix: `@Inject(method="prepareMobModel", at=@At("TAIL"))` for `entity instanceof Mob && !(entity instanceof Player)`: for each arm, `stack = entity.getItemHeldByArm(arm)`; pose = (`SwingAnimation.get(stack).type()==STAB && entity.swinging`) || `stack.is(SPEARS)` ? SPEAR : current. If `this instanceof AbstractZombieModel` and the opposite arm's item is STAB, set SPEAR too.
- config_gate: has_spears

### MAYHEM-36  Swim stroke still animates the spear arm
- severity: visual
- vb: no change to 1.21.1 HumanoidModel.setupAnim swim block (src-121 HumanoidModel.java:243-246) — `f1 = attackArm == RIGHT && attackTime > 0 ? 0 : swimAmount`
- native: HumanoidModel.java:297-302 — `rightArmSwimAmount = state.rightArmPose != SPEAR && (attackArm != RIGHT || !(attackTime > 0)) ? swimAmount : 0.0F` (same for the left)
- difference: swimming players (and mobs, once 35 is fixed) holding a spear windmill the spear arm in VB; native keeps it in the spear pose.
- fix_kind: mixin-mc (HumanoidModel.setupAnim)
- fix: `@ModifyVariable` on the two swim-amount locals (ordinal 1 and 2 floats after the `swimAmount > 0` check) → `0.0F` when `rightArmPose/leftArmPose == ArmPoses.SPEAR.get()`.
- config_gate: has_spears

### MAYHEM-37  Spear swap/cooldown dip uses scale 1.0 instead of 1.95
- severity: visual
- vb: SpearRenderer only swaps models; no `swap_animation_scale` (1.21.1 ItemInHandRenderer uses `1 - lerp(mainHandHeight)` directly)
- native: assets/minecraft/items/*_spear.json `"swap_animation_scale": 1.95`; ItemInHandRenderer.java:360 `itemModelResolver.swapAnimationScale(mainHandItem) * (1 - lerp(...))`
- difference: after a jab or item switch, native first-person spears drop 1.95x as far.
- fix_kind: mixin-mc (ItemInHandRenderer.renderHandsWithItems)
- fix: `@ModifyArg` on the main-hand and off-hand `renderArmWithItem(...)` calls, index of `equippedProgress` (arg 6): multiply by `1.95F` when the stack is in `#spears`.
- config_gate: has_spears

### MAYHEM-38  Spear crosshair raycast uses the main hand only and can target spectators
- severity: gameplay
- vb: client/spear_behavior/GameRendererMixin.java:45 `ItemStack activeStack = this.minecraft.player.getMainHandItem();`; :74/103 `Entity::isPickable`
- native: LocalPlayer.raycastHitResult:1239-1241 `ItemStack itemStack = this.getActiveItem();` (use item while using, else main hand; empty for spectators) with `EntitySelector.CAN_BE_PICKED`. 1.21.1 vanilla picking also excludes spectators (`!e.isSpectator() && e.isPickable()`).
- difference: VB keeps spear reach (min 2.0 / max 4.5) while the player is using an off-hand item, and the spear path can pick spectator players.
- fix_kind: mixin-vb (GameRendererMixin.vb$onPick / vb$raycastHitResult)
- fix: use `player.isUsingItem() ? player.getUseItem() : player.getMainHandItem()` (EMPTY for spectators), and the predicate `e -> !e.isSpectator() && e.isPickable()`.
- config_gate: has_spears

### MAYHEM-39  AttackRange codec defaults differ
- severity: data
- vb: components/AttackRange.java:35-38 — `max_reach` default 0.0F, `max_creative_reach` 0.0F, `hitbox_margin` 0.0F
- native: AttackRange.java:31-34 — `max_reach` 3.0F, `max_creative_reach` 5.0F, `hitbox_margin` 0.3F
- difference: an `attack_range` component given by command or datapack without these fields gets 0 reach in VB. Built-in spears set every field, so they are unaffected.
- fix_kind: mixin-vb (AttackRange CODEC)
- fix: `@ModifyArg` on the three `optionalFieldOf(String, Object)` calls in `<clinit>` (ordinals 1, 3, 4) → 3.0F, 5.0F, 0.3F.
- config_gate: has_spears

### MAYHEM-40  UseEffects.speedMultiplier ignored
- severity: data
- vb: client/spear_behavior/LocalPlayerMixin.java — use-slowdown is only toggled on or off by `canSprint`; the 1.21.1 factor stays `0.2F`
- native: LocalPlayer.modifyInput:710-711 `newInput = newInput.scale(this.itemUseSpeedMultiplier())`
- difference: an item with `use_effects{speed_multiplier:x}` other than 0.2 moves at 0.2 in VB. Spears (canSprint=true, 1.0) are unaffected.
- fix_kind: mixin-mc (LocalPlayer.aiStep)
- fix: `@ModifyConstant(method="aiStep", constant=@Constant(floatValue=0.2F, ordinal=0/1))` → `UseEffects.get(this.useItem).speedMultiplier()`.
- config_gate: has_spears

### MAYHEM-41  spear_mobs trigger `player` field uses the loot-condition codec
- severity: data
- vb: triggers/SpearMobsTrigger.java — `ContextAwarePredicate.CODEC.optionalFieldOf("player")`
- native: SpearMobsTrigger — `EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")`
- difference: native-format advancements with `"player": {<entity predicate>}` fail to parse in VB (only the list-of-conditions form works).
- fix_kind: mixin-vb (SpearMobsTrigger.TriggerInstance CODEC)
- fix: `@ModifyArg` the `optionalFieldOf` receiver → `EntityPredicate.ADVANCEMENT_CODEC` (exists in 1.21.1).
- config_gate: has_spears

### MAYHEM-42  Spear/nautilus-armour blasting takes 200 ticks instead of 100
- severity: gameplay
- vb: common/level/recipes/VanillaDynamicRecipes.java:343-362 `SimpleCookingRecipeBuilder.blasting(Ingredient.of(GOLDEN_SPEAR, GOLDEN_NAUTILUS_ARMOR) ... 0.1F, 200)` and the same for iron
- native: recipe/gold_nugget_from_blasting.json, iron_nugget_from_blasting.json — no `cookingtime` (blasting default 100), `experience 0.1`, ingredient list includes the spear and nautilus armour
- difference: blasting these items takes twice as long in VB. (The `data/vanillabackport/recipes/*` JSONs sit in the pre-1.21 plural folder and are never loaded.)
- fix_kind: mixin-vb (VanillaDynamicRecipes.buildMountsOfMayhemRecipes)
- fix: `@ModifyArg(method="buildMountsOfMayhemRecipes", at=@At(value="INVOKE", target="...SimpleCookingRecipeBuilder;blasting(...)...", ordinal=0/1), index=4)` → `100`.
- config_gate: has_spears / has_nautilus

### MAYHEM-43  Nautilus / zombie nautilus textures have a 16-pixel hole
- severity: visual
- vb: assets/minecraft/textures/entity/nautilus/nautilus.png, zombie_nautilus.png, zombie_nautilus_coral.png — pixels at u 12/25, v 68-75 are transparent (`(0,0,0,0)`)
- native: same files — opaque (e.g. nautilus `(142,38,35,255)`, zombie `(24,38,35,255)`); all other pixels identical
- difference: two 1-px vertical slits on the body box's side faces (`texOffs(0,54)`, 10x8x14) show through in VB.
- fix_kind: asset-override (assets/minecraft/textures/entity/nautilus/{nautilus,zombie_nautilus,zombie_nautilus_coral}.png)
- fix: ship the 26.2 PNGs (byte-identical copies of src-mc versions). camel_husk.png also differs (955 px), but that is the saddle UV the 1.21.1 CamelModel draws from the base texture (native moved it to a separate saddle layer), so it must not be replaced.
- config_gate: has_nautilus
