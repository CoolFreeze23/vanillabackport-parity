# misc — VanillaBackport 1.2.2 vs native 26.2

**Summary**
- 37 findings: gameplay 13, visual 8, audio 7, data 6, cosmetic 3. Where 26.2 lacks a feature, the native reference is 26.3-pre-1 (`src-mc263`): cushion, map decorations, shelf-mushroom recipes.
- Covered: bundles (logic, tooltip, scroll, models, recipes, loot, creative order), locator bar / waypoints (server manager, transmitter, tracked waypoint math, renderer, XP/jump priority, commands, arguments, packet), ender pearl persistence (tickets, save/load, vanish rules, teleport), end flashes (state, sound, sky, lightmap, End skylight patch), music (fade, frequency, play while paused, toast, options), clouds, boats (custom boat entity/item/dispenser/blacklist), monsters_on_peaceful, recipes (VanillaDynamicRecipes, BundleColoring), triggers, value providers, game rules, map decorations, data components (non-spear), painting/jukebox data, lang, sounds.json, banner-pattern renderer, LeashInteraction, nautilus inventory, network packets, core/util colour/vector helpers.
- Sounds: checked against the 26.2 `sounds.json` (`<mc-26.2-assets>/minecraft/sounds.json`, SHA-1 `9ac006d5…`; volume, pitch, weight, stream, attenuation and subtitle compared per entry) and the 1.21.1 one (index 17). Every misc-scope event (`weather.end_flash`, `item.bundle.insert_fail`, `music_disc.tears/lava_chicken/bounce`, `music.overworld.sulfur_caves`) matches. Textures compared pixel by pixel; all misc-scope PNGs are identical.
- Not checked: in-game runtime (MISC-17 is derived from code, not seen in game); `.ogg` content; `NewMinecartBehavior` (VB never references it, so nothing runs); cushion/poplar/shelf-mushroom sound entries (26.3-only; no 26.3 sounds.json in the trees). The Platform lib (`src-platform`) shows `ItemRendererRegistry` swaps the model in every display context (`ItemRendererMixin.render` HEAD + `getModel` STORE), GUI included.
- Cross-area items noticed but not filed here: `subtitles.entity.zombie_nautilus.ambient`/`ambient_land` text differs ("clacks" vs native "burbles"); nine `block.*.fall` sounds.json entries lack native `subtitles.block.generic.fall`; `block.deadbush.idle` points to the old paths (native uses `sandblock_version/`); the `minimum_attack_charge` component id is wrong (MISC-33, spear area).

## Coverage

| VB class / data | Native counterpart | Result |
|---|---|---|
| common/api/bundle/BundleFeatures (deprecated shim) | — | no native counterpart |
| common/api/modules/bundle_ui/BundleFeatures, BundleItemHandler | world/item/BundleItem | findings (01) |
| common/api/modules/bundle_ui/BundleColoring + data/minecraft/recipe/bundle_coloring.json | recipe/*_bundle.json (crafting_transmute) | findings (02) |
| common/api/modules/bundle_ui/ModernBundle, core/mixin/common/bundle_ui/BundleContentsMixin, MutableBundleContentsMixin | world/item/component/BundleContents | match (getNumberOfItemsToShow, toggle, removeOne, weights) |
| core/mixin/common/bundle_ui/BundleItemMixin | BundleItem | findings (01); bar colour match |
| client/api/modules/bundle_ui/BundleTooltipHandler, ClientBundleTooltipMixin | client/gui/screens/inventory/tooltip/ClientBundleTooltip | findings (03, 04) |
| client/api/modules/bundle_ui/BundleMouseActions, ScrollWheelHandler, ItemSlotMouseAction | client/gui/BundleMouseActions, client/ScrollWheelHandler | findings (05); selection math match |
| client/api/modules/bundle_ui/BundleRenderHandler, core/mixin/client/bundle_ui/AbstractContainerScreenMixin | items/*bundle.json (`bundle/selected_item` special model) | match (layering, textures pixel-identical) |
| client/level/renderer/item/BundleRenderer, ItemPropertyIntegrations, assets models/item/*bundle*.json | assets items/*bundle.json, models/item/*bundle.json | match with `has_modern_bundle_models=true` (default): Platform swaps in `vanillabackport:item/<bundle>` (closed texture, pixel-identical to native) in all contexts. With the option off, empty bundles show the 1.21.1 open `bundle.png` / `<colour>_bundle_open.png` (native always closed) |
| core/network/ServerboundSelectBundleItemPacket | network/protocol/game/ServerboundSelectBundleItemPacket + AbstractContainerMenu.setSelectedBundleItemIndex | match |
| client/integrations/VanillaTabIntegrations | world/item/CreativeModeTabs | findings (07) |
| common/level/recipes/VanillaDynamicRecipes, RecipeStrategy | data/minecraft/recipe/{bundle,lead,saddle,lodestone,cake,pumpkin_pie,…}.json; 26.3 mushroom_stew, rabbit_stew_from_shelf_mushroom | findings (09); patterns/ingredients match. Mushroom stew: VB's extra `vanillabackport:mushroom_stew` (2x #mushrooms + bowl) plus vanilla brown+red equals native 26.3. VB `#minecraft:mushrooms` also has optional `#c:mushrooms` (26.3: brown, red, shelf only) |
| common/registries/ModRecipeSerializers | — (native uses crafting_transmute) | covered by 02 |
| — (missing) | Items.FIELD_MASONED/BORDURE_INDENTED_BANNER_PATTERN, tags/banner_pattern/no_item_required | findings (08) |
| client/level/renderer/item/BannerPatternRenderer + textures | items/*_banner_pattern.json | match (textures pixel-identical) |
| common/api/modules/waypoints/ServerWaypointManager | server/waypoints/ServerWaypointManager | match |
| common/api/modules/waypoints/WaypointTransmitter | world/waypoints/WaypointTransmitter | match (332 really-far, 0.008726646 rad, Manhattan/chessboard >1) |
| common/api/modules/waypoints/TrackedWaypoint | world/waypoints/TrackedWaypoint | findings (11); azimuth/chunk/pitch math match |
| common/api/modules/waypoints/Waypoint (Icon) | world/waypoints/Waypoint | match (team colour, black → -13619152) |
| common/api/modules/waypoints/WaypointStyleAsset(s), WaypointManager, TrackedWaypointManager | world/waypoints/* | match |
| common/api/modules/waypoints/WaypointUtils | ServerPlayer.updatePlayerAttributes, MobEffects.INVISIBILITY, Waypoint.addHideAttribute | findings (13) |
| core/mixin/common/waypoints/LivingEntityMixin, PlayerMixin, EntityMixin, EntityCallbacksMixin, ServerChunkCacheMixin, ServerScoreboardMixin | LivingEntity, Player, Entity, ServerLevel.EntityCallbacks, ServerChunkCache.move, ServerScoreboard | match except respawn copy, findings (10) |
| common/registries/ModAttributes (waypoint ranges) | Attributes.WAYPOINT_TRANSMIT/RECEIVE_RANGE (0, 0–6e7; player 6e7) | match |
| common/registries/ModGameRules (locatorBar) | GameRules.LOCATOR_BAR (`locator_bar`, true) + MinecraftServer onGameRuleChanged | match (name is engine) |
| client/api/modules/waypoints/LocatorBarRenderer, ExperienceDisplay, core/mixin/client/waypoints/* | client/gui/contextualbar/LocatorBar, ContextualBar, Hud.nextContextualInfoState, LocalPlayer | findings (12); dot math/sizes/colour match |
| client/api/modules/waypoints/WaypointStyle, WaypointStyleManager, assets/waypoint_style/{default,bowtie} | client/resources/WaypointStyle(+Manager) | match (128/332, lerpInt) |
| client/api/modules/waypoints/ClientWaypointManager | client/waypoints/ClientWaypointManager | match |
| client/api/modules/waypoints/provider/* (cardinal, compass, heads) + waypoint_style north/south/…/death/lodestone | — | no native counterpart (config, default off) |
| common/commands/WaypointCommand, arguments/HexColorArgument, WaypointArgument | server/commands/WaypointCommand, commands/arguments/* | match |
| common/commands/arguments/ColorArgument, ModArgumentTypes | TeamColorArgument, ArgumentTypeInfos (`team_color`, `hex_color`) | findings (14) |
| core/network/ClientboundTrackedWaypointPacket | network/protocol/game/ClientboundTrackedWaypointPacket | match |
| common/api/modules/ender_pearl_persistance/*, core/mixin/common/ender_pearl_persistance/* | ThrownEnderpearl, ServerPlayer (pearl save/load/ticket), PlayerList.remove, TicketType.ENDER_PEARL | findings (15, 16); ticket radius 2 / timeout 40 / save keys match |
| client/api/modules/end_flashes/EndFlashState | client/renderer/EndFlashState | match (600/200/100/380, random order) |
| client/api/modules/end_flashes/EndFlashRenderer (tick, sky) , core/mixin/client/end_flashes/ClientLevelMixin, LevelRendererMixin | ClientLevel.tick, SkyRenderer.renderEndFlash, LevelRenderer sky pass | findings (19, 20); quad math, blend, texture match |
| client/api/modules/end_flashes/DirectionalSoundInstance | client/resources/sounds/DirectionalSoundInstance | findings (18) |
| core/mixin/client/end_flashes/LightTextureMixin + EndFlashRenderer.applyLightTint, core/mixin/common/end_flashes/DimensionTypeMixin, ChunkSerializerMixin | the_end.json attributes + LightmapRenderStateExtractor + lightmap.fsh | findings (17); MonsterSettings match |
| core/mixin/common/end_flashes/EnderManMixin | EnderMan.getWalkTargetValue → 0 | match |
| client/api/modules/end_flashes/BadOptimizationsCommonHook | — | no native counterpart |
| client/api/modules/music_fade/MusicFadeManager, core/mixin/client/music_fade/MusicManagerMixin | MusicManager.fadePlaying + MUSIC_VOLUME attribute | findings (23) |
| client/api/modules/music_frequency/*, core/mixin/client/music_frequency/MusicManagerMixin | MusicManager.MusicFrequency | findings (22); enum values match |
| client/api/modules/music_on_pause/*, core/mixin/client/music_on_pause/* | Minecraft.runTick / tick, SoundEngine.pauseAllExcept | findings (21, 26) |
| client/api/modules/music_toast/*, core/mixin/client/music_toast/* | NowPlayingToast, ToastManager, PauseScreen, ColorLerper | findings (24, 25); ColorLerper match |
| client/api/modules/options/OptionsAccess, core/mixin/client/game_options/* | Options.musicFrequency/musicToast, SoundOptionsScreen | match (keys, defaults, file names) |
| core/mixin/client/clouds/LevelRendererMixin | Options.cloudRange + CloudRenderer | findings (27) |
| client/api/experiment/EnvironmentalFogRenderer | — | no native counterpart (unused helper) |
| client/api/bundled_tabs/*, client/registries/ModBundledTabs | — | no native counterpart (VB UI) |
| client/api/modules/block_rendering/DynamicBlockRenderingHandler | — | no native counterpart (config watcher) |
| common/level/entities/boat/*, CustomBoatRenderer, ModBoatTypes | AbstractBoat / per-wood boat EntityTypes | findings (28) |
| common/level/items/CustomBoatItem, common/integrations/dispenser/BoatDispenseBehavior | BoatItem, BoatDispenseItemBehavior | findings (29) |
| core/mixin/common/boat_blacklist/BoatMixin | AbstractBoat.tick + tag cannot_be_pushed_onto_boats | findings (30) |
| common/api/experiment/minecart_behavior/*, AbstractMinecartMixin | NewMinecartBehavior (experiment only) | no effect: dead code, never instantiated; default native = OldMinecartBehavior = 1.21.1 |
| core/mixin/common/monsters_on_peaceful/NaturalSpawnerMixin, tag monsters_that_spawn_on_peaceful | EntityType.allowedInPeaceful/canSpawn, MinecraftServer spawn settings | findings (31) |
| common/level/entities/decoration/Cushion, CushionItem, cushion recipes | 26.3 world/entity/decoration/Cushion, Items.CUSHION, recipe/*_cushion.json | findings (06, 37); size 1.0x0.25, tracking 10, anchor/buried/suffocation logic, sounds 1.0/1.0, recipe (3 wool slabs, group cushion), stack 16 match |
| common/triggers/SpearMobsTrigger, ModCriteriaTriggers | advancements/triggers/SpearMobsTrigger | match |
| common/value_providers/TrapezoidInt | util/valueproviders/TrapezoidInt | match |
| common/registries/ModMapDecorationTypes (abandoned_camp, ancient_city, desert_pyramid, mineshaft, ocean_ruin_warm) | 26.3 MapDecorationTypes.java:46-50 | match (asset ids, showOnItemFrame true, trackCount false) |
| common/registries/items/ModDataComponents (non-spear) | core/component/DataComponents | findings (33) |
| ModJukeboxSongs + data/jukebox_song/* | data/minecraft/jukebox_song/* | match (175/10, 134/9, 234/8) |
| ModPaintingVariants + data/painting_variant/dennis + tag placeable | data/minecraft/painting_variant/dennis.json | match (3×3; title/author through same lang keys) |
| common/integrations/interactions/LeashInteraction | Entity.interact (leash/shear branch) | findings (32) |
| common/integrations/LootIntegrations (bundle, lodestone pools) | loot_table/chests/village/*, ruined_portal | findings (35); weights match |
| common/integrations/BlockIntegrations (fuel) | 26.3 cookingFuel context providers | findings (36) for wool slabs and cushions; dry grass/leaf litter belong to flora |
| common/integrations/compat/everycompat/*, core/compat/**, client/integrations/rendering/* | — | no native counterpart (integration glue) |
| common/level/inventory/NautilusInventoryMenu, client NautilusInventoryScreen | NautilusInventoryMenu / AbstractMountInventoryMenu / Screen | match (slot layout, quick-move); empty-slot icons drawn from baked texture instead of sprites, cosmetic |
| core/util/Utilities (ColorUtils.setBrightness/srgbLerp, VectorUtils.rotateClockwise90, BufferUtils.writeEither) | ARGB, Vec3, FriendlyByteBuf | match |
| core/registries, core/mixin/common/registry/* | — | no native counterpart (bootstrap) |
| assets/minecraft/lang + assets/vanillabackport/lang (en_us) | assets/minecraft/lang/en_us.json | findings (34) |
| assets/minecraft/sounds.json (misc entries) | 26.2 sounds.json | match |
| data/c/tags/**, fieldguide | — | no native counterpart (convention tags / VB-only) |
| trim_material/resin, damage_type (non-sulfur), advancements (feature-bound) | — | other areas |

---

### MISC-01  Using a bundle drops one item per click instead of emptying it while you hold the button
- severity: gameplay
- vb: `core/mixin/common/bundle_ui/BundleItemMixin.java:57-66` (dropContents HEAD → `cir.setReturnValue(BundleItemHandler.dropContents(stack, player))`); `common/api/modules/bundle_ui/BundleItemHandler.java:117-128` drops only `removeOneItemFromBundle(...)` once. `use` is the 1.21.1 one (`src-121 BundleItem.java:106-115`: `if (dropContents(itemstack, player)) { this.playDropContentsSound(player); … return InteractionResultHolder.sidedSuccess(...)}` else `fail`). No `getUseDuration`/`onUseTick`/use animation is added.
- native: `net/minecraft/world/item/BundleItem.java:139-143` `player.startUsingItem(hand); return InteractionResult.SUCCESS;` · `:220-229` `onUseTick`: `boolean isFirstTick = ticksRemaining == useDuration; if (isFirstTick || ticksRemaining < useDuration - 10 && ticksRemaining % 2 == 0) this.dropContent(level, player, itemStack);` · `:231-234` `getUseDuration → 200` · `:236-239` `ItemUseAnimation.BUNDLE` (first-person `swingArm`, ItemInHandRenderer:570) · `:270-274` drop sound `level.playSound(null, entity.blockPosition(), BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.8F + r*0.4F)` · stat only when an item actually dropped (`:145-150`).
- difference: Native drops 1 item on the first use tick, then 1 every 2 ticks from tick 11 onward, for up to 200 ticks, with an arm-swing animation. VB drops exactly 1 item per right-click and the use fails when the bundle is empty (native always returns SUCCESS).
- fix_kind: mixin-mc (BundleItem)
- fix: `@Mixin(BundleItem.class)`, gated on `BundleFeatures.onBundleUpdate()`:
  - `@Inject(method="use", at=HEAD, cancellable=true)` → `player.startUsingItem(hand); cir.setReturnValue(InteractionResultHolder.consume(player.getItemInHand(hand)));`
  - Add `getUseDuration(ItemStack, LivingEntity)` returning 200.
  - Add `onUseTick(Level level, LivingEntity e, ItemStack s, int remaining)`: if `e instanceof Player p` and (`remaining == 200 || (remaining < 190 && remaining % 2 == 0)`), and `BundleItemHandler.dropContents(s, p)` (already plays REMOVE_ONE), then `level.playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.8F + level.getRandom().nextFloat()*0.4F); p.awardStat(Stats.ITEM_USED.get(this));`. Server side only, like the native ServerLevel drop.
  - `getUseAnimation`: 1.21.1 has no BUNDLE `UseAnim`. Add it as an extended enum value the same way VB adds `REAL_SPEAR`, and in `ItemInHandRenderer.renderArmWithItem` map it to `swingArm(attack, poseStack, invert, arm)`; otherwise use `UseAnim.NONE` (engine).
- config_gate: `has_updated_bundles` (CommonConfig.hasModernBundles)

### MISC-02  Bundle dyeing: same-colour dye is accepted, and the recipe is not in the recipe book
- severity: gameplay
- vb: `common/api/modules/bundle_ui/BundleColoring.java:47` `return bundles == 1 && dyes == 1;` and `:69` `bundle.transmuteCopy(result, 1)`. `data/minecraft/recipe/bundle_coloring.json` = `{"type":"minecraft:crafting_special_bundlecoloring","category":"misc"}` (special recipe, not shown in the recipe book).
- native: `data/minecraft/recipe/red_bundle.json` (×16) `{"type":"minecraft:crafting_transmute","category":"equipment","group":"bundle_dye","input":"#minecraft:bundles","material":"minecraft:red_dye","result":{"id":"minecraft:red_bundle"}}`; `TransmuteRecipe.java:131` `return result.isEmpty() ? false : !ItemStack.isSameItemSameComponents(foundInput, result);`
- difference: A red bundle plus red dye matches in VB (the dye is used up and nothing changes); native refuses it. Native lists 16 recipes in the recipe book (equipment tab, grouped as `bundle_dye`); VB lists none.
- fix_kind: mixin-vb (BundleColoring.matches)
- fix: `@Inject(method="matches", at=RETURN, cancellable=true)`: when the return is true, find the bundle and dye stacks again and `cir.setReturnValue(!bundle.is(BundleFeatures.getByColor(dye.getDyeColor())))`. A recipe-book fix needs 16 `crafting_shapeless` entries (bundle tag + dye), which 1.21.1 cannot express as a transmute (engine); keep the special recipe.
- config_gate: `has_dyeable_bundle_recipe`

### MISC-03  Empty-bundle tooltip height is fixed at 39 px
- severity: visual
- vb: `client/api/modules/bundle_ui/BundleTooltipHandler.java:54` `return this.contents.isEmpty() ? 39 : this.backgroundHeight();` (the body draws `font.split(BUNDLE_EMPTY_DESCRIPTION, width).size()*9 + 4 + 13`)
- native: `ClientBundleTooltip.java:44-46` `this.contents.isEmpty() ? getEmptyBundleBackgroundHeight(font) : …`; `:58-60` `getEmptyBundleDescriptionTextHeight(font) + 13 + 8` where `:204-206` `font.split(BUNDLE_EMPTY_DESCRIPTION, 96).size() * 9`
- difference: 39 is only correct when the description wraps to exactly 2 lines (18+21). If it wraps to 3 lines (common in other languages), native height is 48 and VB's tooltip box is 9 px too short, so the progress bar overflows the frame.
- fix_kind: mixin-vb (BundleTooltipHandler.getHeight)
- fix: change the constant: `return contents.isEmpty() ? Minecraft.getInstance().font.split(Component.translatable("item.minecraft.bundle.empty.description"), getWidth()).size() * 9 + 13 + 8 : backgroundHeight();` (`@Inject HEAD cancellable`).
- config_gate: `has_updated_bundles`

### MISC-04  Bundle tooltip is hidden while an item is on the cursor
- severity: visual
- vb: nothing; 1.21.1 `AbstractContainerScreen.renderTooltip` only runs when `this.menu.getCarried().isEmpty()`.
- native: `AbstractContainerScreen.java:171` `if (this.menu.getCarried().isEmpty() || this.showTooltipWithItemInHand(item))`; `ClientBundleTooltip.java:54-56` `showTooltipWithItemInHand() → true`
- difference: Native keeps showing the hovered bundle's contents/fullness tooltip while you hold a stack to insert; VB shows nothing.
- fix_kind: mixin-mc (AbstractContainerScreen.renderTooltip)
- fix: `@Inject(method="renderTooltip", at=HEAD, cancellable=true)`: if `hoveredSlot != null && hoveredSlot.hasItem() && !menu.getCarried().isEmpty() && hoveredSlot.getItem().is(ItemTags…BUNDLES)`, call `graphics.renderTooltip(font, getTooltipFromContainerItem(stack), stack.getTooltipImage(), x, y)` and cancel.
- config_gate: `has_updated_bundles`

### MISC-05  Horizontal scroll wheel does not move the bundle selection
- severity: cosmetic
- vb: `client/api/modules/bundle_ui/BundleMouseActions.java:26,32` passes only `deltaY`; `ScrollWheelHandler.java:11-24` keeps only a Y accumulator and returns `new Vector2i(0, scroll)`.
- native: `client/gui/BundleMouseActions.java:34-35` `onMouseScroll(scrollX, scrollY); int wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;`; `client/ScrollWheelHandler.java:9-29` accumulates X and Y separately.
- difference: Native tilt-wheel or trackpad horizontal scroll cycles the selection (sign inverted); VB ignores it.
- fix_kind: mixin-vb (BundleMouseActions.bootstrap lambda / ScrollWheelHandler)
- fix: add an X accumulator to `ScrollWheelHandler.onMouseScroll(double x, double y)` mirroring native lines 10-28, pass `deltaX` from the `SCROLLING_PRE` callback, and use `wheel = v.y == 0 ? -v.x : v.y`.
- config_gate: `has_updated_bundles`

### MISC-06  Cushion fire check runs every tick and ignores fluids (native: every 100 ticks, with fluid effects)
- severity: gameplay
- vb: `common/level/entities/decoration/Cushion.java:168-173` `public void tick() { super.tick(); if (this.level() instanceof ServerLevel serverLevel) this.destroyIfInFire(serverLevel); }`. The 1.21.1 `BlockAttachedEntity.tick` (`src-121 BlockAttachedEntity.java:35-46`) only does the 100-tick `survives()` check; there is no fluid handling.
- native (26.3-pre-1): `world/entity/decoration/BlockAttachedEntity.java:42-53` `if (this.ticksSinceLastCheck++ >= 100) { this.ticksSinceLastCheck = 0; this.tickAtCheckInterval(); if (!isRemoved() && !survives()) { discard(); dropItem(level, null); } }`; `Cushion.java:79-90` `tickAtCheckInterval()`: `if (this.collidedWithFluid(fluidState, ...)) { fluidState.entityInside(level, blockPos, this, insideEffectCollector); insideEffectCollector.applyAndClear(this); } this.destroyIfInFire(level);`
- difference: Native checks fire (and lava/fluid effects) once every 100 ticks; VB checks fire every tick and never applies fluid effects. Fire next to a cushion breaks it within 1 tick in VB, but after up to 5 s natively (any damage breaks it). A cushion in lava is never destroyed in VB.
- fix_kind: mixin-vb (Cushion.tick)
- fix: `@Redirect` the `destroyIfInFire` call in `tick` behind a 100-tick counter of your own (`if (++vb$check >= 100) { vb$check = 0; <lava check>; destroyIfInFire(level); }`). Lava check: `if (level.getFluidState(blockPosition()).is(FluidTags.LAVA)) hurt(damageSources().lava(), 4.0F)` (1.21.1 has no fluid `entityInside` for non-living entities). Leave the vanilla `survives()` check in `super.tick()`.
- config_gate: none

### MISC-07  Creative-tab order differs (dyed bundles, Tears disc, others)
- severity: cosmetic
- vb: `client/integrations/VanillaTabIntegrations.java:30-48` bundles after BUNDLE as `white, light_gray, gray, black, brown, red, orange, yellow, lime, green, cyan, light_blue, blue, purple, magenta, pink`; `:174` `after(MUSIC_DISC_RELIC).add(MUSIC_DISC_TEARS)`.
- native: `CreativeModeTabs.java:1083-1084` `BUNDLE; Items.DYED_BUNDLE.forEach(...)`, and `ColorCollection.forEach` order is `white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black`; `:1152-1157` `RELIC, 5, PIGSTEP, TEARS, LAVA_CHICKEN, BOUNCE`.
- difference: Bundles appear in rainbow order instead of dye-ordinal order, and Tears sits before disc 5/Pigstep instead of after Pigstep. An automated check also flagged these anchors, which belong to other areas: TORCHFLOWER (native puts CACTUS_FLOWER before eyeblossoms), COW_SPAWN_EGG/CREAKING_SPAWN_EGG, SADDLE→harness block (native harnesses aren't after SADDLE), CHEST→copper chests, ENDER_EYE→copper golem statues, WOLF_ARMOR (native starts with COPPER_NAUTILUS_ARMOR), SOUL_LANTERN→COPPER_LANTERN, spawn eggs for camel husk/nautilus/parched/happy ghast/copper golem, the CUT_COPPER_SLAB family.
- fix_kind: mixin-vb (VanillaTabIntegrations.registerBundlesOfBravery / registerChaseTheSkies)
- fix: reorder the bundle array to the ColorCollection order above; change line 174 to `output.after(Items.MUSIC_DISC_PIGSTEP).add(MUSIC_DISC_TEARS)`.
- config_gate: none

### MISC-08  Field Masoned and Bordure Indented banner pattern items are missing; the loom still makes both patterns for free
- severity: gameplay
- vb: no such items; VB does not override `tags/banner_pattern/no_item_required`. 1.21.1 tag contains `"minecraft:curly_border"` and `"minecraft:bricks"`.
- native: `Items.java:1854-1865` `FIELD_MASONED_BANNER_PATTERN` / `BORDURE_INDENTED_BANNER_PATTERN` (`stacksTo(1)`, `PROVIDES_BANNER_PATTERNS → #pattern_item/field_masoned|bordure_indented`). Recipes `field_masoned_banner_pattern.json` = shapeless `paper + bricks`, `bordure_indented_banner_pattern.json` = shapeless `paper + vine`. Tags `banner_pattern/pattern_item/field_masoned = [bricks]`, `…/bordure_indented = [curly_border]`. `no_item_required` lacks both.
- difference: Native needs the pattern item in the loom for Field Masoned and Bordure Indented; VB/1.21.1 offers them without any item, and the two items, recipes and lang keys (`item.minecraft.field_masoned_banner_pattern`, `…bordure_indented…`) don't exist.
- fix_kind: data-override + new item registration
- fix: register `minecraft:field_masoned_banner_pattern` and `minecraft:bordure_indented_banner_pattern` as `BannerPatternItem(TagKey(banner_pattern, minecraft:pattern_item/field_masoned|bordure_indented), new Properties().stacksTo(1))` (VB already registers into the minecraft namespace). Add the two tag files above, the two shapeless recipes, the item models (`layer0 minecraft:item/<id>`; the textures exist in 26.2), en_us names, creative placement after `GUSTER_BANNER_PATTERN`, and `data/minecraft/tags/banner_pattern/no_item_required.json` with `"replace": true` and the 32 native values (the 1.21.1 list minus `curly_border` and `bricks`).
- config_gate: none

### MISC-09  Default "APPEND" recipe strategy keeps the old lodestone and lead recipes and uses non-native ids
- severity: gameplay
- vb: `common/level/recipes/VanillaDynamicRecipes.java:390-393` `case APPEND: builder.save(output, path.apply(name))` (→ `vanillabackport:<name>`; vanilla recipe kept); `case REPLACE: builder.save(output, ResourceLocation.withDefaultNamespace(name))`. Config defaults: `bundle_recipe`, `lead_recipe`, `saddle_recipe`, `lodestone_recipe` = `APPEND`. 1.21.1 `lodestone.json` uses `minecraft:netherite_ingot`; 1.21.1 `lead.json` is `["~~","~O","~"]` with `O = slime_ball`.
- native: `data/minecraft/recipe/lodestone.json` `# = minecraft:iron_ingot` only; `lead.json` `["~~ ","~~ ","  ~"]` string only, count 2; `bundle.json` / `saddle.json` under `minecraft:`.
- difference: With default settings the netherite lodestone and slime-ball lead stay craftable (native removed them). The new recipes live under `vanillabackport:` ids, so `/recipe give @s minecraft:lead` and the recipe-book unlock ids differ.
- fix_kind: mixin-vb (VanillaDynamicRecipes.configurableRecipe) or data-override
- fix: either `@ModifyVariable(method="configurableRecipe", at=HEAD, argsOnly=true)` mapping `APPEND → REPLACE`, or ship `data/minecraft/recipe/lodestone.json` and `lead.json` with the native JSON above (REPLACE writes exactly these).
- config_gate: `bundle_recipe`, `lead_recipe`, `saddle_recipe`, `lodestone_recipe`

### MISC-10  Player waypoint icon (style/colour) is lost on respawn
- severity: data
- vb: `core/mixin/common/waypoints/LivingEntityMixin.java:33` `@Unique private Waypoint.Icon locatorBarIcon = new Waypoint.Icon();`, saved/loaded only through NBT (`:83-103`). No `restoreFrom` hook; `Waypoint.Icon` has no `copyFrom`.
- native: `ServerPlayer.java:1664` (restoreFrom) `this.waypointIcon().copyFrom(oldPlayer.waypointIcon());`; `Waypoint.java:77-80` `copyFrom`.
- difference: After death or returning from the End, a style/colour set with `/waypoint modify` resets to default in VB; native keeps it.
- fix_kind: mixin-mc (ServerPlayer.restoreFrom)
- fix: `@Inject(method="restoreFrom", at=TAIL)` → `Waypoint.Icon n = ((WaypointTransmitter)this).waypointIcon(), o = ((WaypointTransmitter)oldPlayer).waypointIcon(); n.style = o.style; n.color = o.color;`
- config_gate: none

### MISC-11  Locator-bar position of nearby entities is not interpolated
- severity: visual
- vb: `common/api/modules/waypoints/TrackedWaypoint.java:283-284` `… entity.blockPosition().distManhattan(this.vector) > 3 ? null : entity.getEyePosition()` (current tick, no partial tick). `LocatorBarRenderer.java:71` calls `yawAngleToCamera(level, camera)` with no partial-tick supplier.
- native: `TrackedWaypoint.java:288-294` `e.getEyePosition(partialTick.apply(e))`; `LocatorBar.java:47-48` `partialTickSupplier = entity -> deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity))`.
- difference: Close players' dots and up/down arrows step at 20 Hz instead of moving smoothly; the arrow can flicker at the ±1 screen-edge thresholds.
- fix_kind: mixin-vb (TrackedWaypoint.Vec3iWaypoint.position)
- fix: `@Redirect` the `Entity.getEyePosition()` call at line 284 to `entity.getEyePosition(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(!entity.level().tickRateManager().isEntityFrozen(entity)))`. This is client-only (the class is shared, so guard with `level.isClientSide()`).
- config_gate: `enable_locator_bar`

### MISC-12  XP bar takes over the locator bar at the wrong times
- severity: visual
- vb: `core/mixin/client/waypoints/LocalPlayerMixin.java:14` `private int experienceDisplayStartTick;` (default 0) and `:31` sets `tickCount` at TAIL of every `setExperienceValues` call. `client/api/modules/waypoints/ClientWaypointManager.java:36-37` also sets it every tick while `EnchantmentScreen`/`AnvilScreen` is open. `LocatorBarRenderer.java:148-150` `jumpableVehicle() != null ? willPrioritizeJumpInfo() : hasExperience() && willPrioritizeExperienceInfo()`.
- native: `LocalPlayer.java:140` `experienceDisplayStartTick = Integer.MIN_VALUE`; `:483-499` only `if (experienceProgress != this.experienceProgress)`, and the first change sets it to `-2147483647`. `Hud.java:1336-1350`: when there are waypoints, `canShowVehicleJumpInfo && willPrioritizeJumpInfo() → JUMPABLE_VEHICLE`, else `canShowExperienceInfo && willPrioritizeExperienceInfo() → EXPERIENCE`, else `LOCATOR`.
- difference: VB hides the locator for 100 ticks after joining, respawning, changing dimension (any XP packet), and after closing an enchanting table or anvil. Native does none of these. While riding a horse, native still shows the XP bar for 100 ticks after an XP change; VB never does.
- fix_kind: mixin-vb (LocalPlayerMixin, ClientWaypointManager.bootstrap, LocatorBarRenderer.shouldSkipRendering)
- fix:
  - LocalPlayerMixin: initialise the field to `Integer.MIN_VALUE`. Move the injection to HEAD and set the tick only when `currentXP != ((LocalPlayer)this).experienceProgress`, using `v == Integer.MIN_VALUE ? -2147483647 : tickCount`.
  - Remove the Enchantment/Anvil `CONTAINER_TICK` handler.
  - In `shouldSkipRendering` use `vehicle != null && willPrioritizeJumpInfo() || hasExperience() && willPrioritizeExperienceInfo()`, and draw the jump bar / XP bar accordingly.
- config_gate: `enable_locator_bar`, `locator_display_info_bar`

### MISC-13  Head-item locator hiding is a player-only tag check, not an item attribute modifier
- severity: data
- vb: `common/api/modules/waypoints/WaypointUtils.java:49-52` `if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItemTags.DISABLES_WAYPOINT_TRACKING)) attribute.addOrUpdateTransientModifier(WAYPOINT_TRANSMIT_RANGE_CROUCH_MODIFIER);` (tag `[minecraft:carved_pumpkin, #minecraft:skulls]`)
- native: `Waypoint.java:22-33` `addHideAttribute` puts `ATTRIBUTE_MODIFIERS {waypoint_transmit_range, id minecraft:waypoint_transmit_range_hide, -1, ADD_MULTIPLIED_TOTAL, slot HEAD, display hidden}` on CARVED_PUMPKIN (`Items.java:449-452`) and the 7 skull/head items (`:1514-1550`); `ServerPlayer.java:667-674` crouch modifier only for `isCrouching()`.
- difference: In-game result for players is the same. But native items carry an `attribute_modifiers` component (visible via `/data`, copied by `/item`, and it applies to any LivingEntity with a transmit range, e.g. mobs given one with `/attribute` and wearing a pumpkin); VB applies the crouch modifier id instead.
- fix_kind: mixin-mc (Items registration properties) or leave as is
- fix: add the default component `DataComponents.ATTRIBUTE_MODIFIERS = ItemAttributeModifiers.builder().add(ModAttributes.WAYPOINT_TRANSMIT_RANGE, new AttributeModifier(withDefaultNamespace("waypoint_transmit_range_hide"), -1, ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD).build().withTooltip(false)` to those 8 items (NeoForge `ModifyDefaultComponentsEvent`), and drop the tag branch in WaypointUtils.
- config_gate: none

### MISC-14  `/waypoint modify <e> color <name>` accepts formatting codes and "reset"
- severity: data
- vb: `common/commands/arguments/ColorArgument.java:32-40` `ChatFormatting result = ChatFormatting.getByName(id); if (result == null) throw …` (no `isColor()` check; suggestions come from `getNames(true,false)`). The id is registered as `minecraft:team_color` (`ModArgumentTypes.java`).
- native: `TeamColorArgument` parses `TeamColor` (16 colours only); `WaypointCommand.java:109-115` `icon.color = Optional.of(color.rgb())`.
- difference: `bold`, `italic`, `obfuscated`, `reset` etc. parse in VB, set `color = Optional.ofNullable(null)` (acts as reset) and print "Waypoint color is now bold". Native rejects them with `argument.color.invalid`.
- fix_kind: mixin-vb (ColorArgument.parse)
- fix: `@Inject(method="parse", at=RETURN, cancellable=true)`: `if (!cir.getReturnValue().isColor()) throw ERROR_INVALID_VALUE.createWithContext(reader, id)`. Rethrow via `@WrapMethod` because of the checked exception.
- config_gate: none

### MISC-15  Ender pearl teleport destination and endermite rule are the 1.21.1 ones
- severity: gameplay
- vb: no ThrownEnderpearl.onHit change. 1.21.1 `ThrownEnderpearl.onHit` teleports to `event.getTarget()` = `this.getX(),getY(),getZ()` (the hit point) via `entity.changeDimension(new DimensionTransition(serverlevel, …, entity.getDeltaMovement(), …))` after `entity.unRide()`. Endermite: `random.nextFloat() < 0.05F && getGameRules().getBoolean(RULE_DOMOBSPAWNING)`. Damage `damageSources().fall()`.
- native: `ThrownEnderpearl.java:106` `Vec3 teleportPos = this.oldPosition();`; `:109` `this.random.nextFloat() < 0.05F && level.isSpawningMonsters() && level.getLevelData().getDifficulty() != Difficulty.PEACEFUL`; `:117-118` `if (this.isOnPortalCooldown()) owner.setPortalCooldown();`; `:121-129` `player.teleport(new TeleportTransition(level, teleportPos, Vec3.ZERO, 0, 0, Relative.union(ROTATION, DELTA), DO_NOTHING))`, then `resetFallDistance`, `resetCurrentImpulseContext`, `hurtServer(…, damageSources().enderPearl(), 5.0F)`; `:132` sound at `teleportPos`.
- difference: Native lands you at the pearl's previous-tick position (one step back from the impact, so you don't clip into walls/ceilings); VB lands you at the impact point. On Peaceful VB still rolls the 5% endermite (it is then discarded). Native also passes the pearl's portal cooldown to the owner. The `ender_pearl` damage type is `fall`-equivalent (same message, scaling and tags), so damage needs no fix.
- fix_kind: mixin-mc (ThrownEnderpearl.onHit)
- fix:
  - `@ModifyArg` or `@Redirect` NeoForge `EventHooks.onEnderPearlLand(..., this.getX(), this.getY(), this.getZ(), ...)` to use `this.xOld, this.yOld, this.zOld`, and move the `playSound` position the same way.
  - `@WrapOperation` the `getGameRules().getBoolean(RULE_DOMOBSPAWNING)` call to `&& level.getDifficulty() != Difficulty.PEACEFUL`.
  - Before `changeDimension` add `if (this.isOnPortalCooldown()) entity.setPortalCooldown();`.
- config_gate: none

### MISC-16  Ender pearls vanish in two cases where native keeps them
- severity: gameplay
- vb: `core/mixin/common/ender_pearl_persistance/ThrownEnderpearlMixin.java:86-93` (tick HEAD): `if (ownerUUID != null) { ServerPlayer onlinePlayer = …getPlayerList().getPlayer(ownerUUID); if (onlinePlayer == null) { this.setRemoved(RemovalReason.UNLOADED_WITH_PLAYER); ci.cancel(); …` The dead-owner check is the 1.21.1 one: `entity instanceof ServerPlayer && !entity.isAlive() && RULE_ENDER_PEARLS_VANISH_ON_DEATH`.
- native: `ThrownEnderpearl.java:169-177` `Entity owner = this.owner != null ? findOwnerIncludingDeadPlayer(...) : null; if (owner instanceof ServerPlayer serverPlayer && !owner.isAlive() && !serverPlayer.wonGame && gamerule ENDER_PEARLS_VANISH_ON_DEATH) this.discard(); else super.tick();`. A pearl with an offline or non-player owner just keeps flying.
- difference: (a) During the End credits (`wonGame`, player removed so `!isAlive()`), VB/1.21.1 discards all of the player's pearls; native keeps them. (b) VB silently deletes, without saving, any pearl whose owner UUID is not an online player: pearls saved in a chunk after a crash, or pearls whose `Owner` was set to a mob by command. Native lets them fly and land.
- fix_kind: mixin-vb (ThrownEnderpearlMixin.vb$checkForMissingPlayerOnTick) + mixin-mc (ThrownEnderpearl.tick)
- fix: (a) `@WrapOperation` 1.21.1 `ThrownEnderpearl.tick`'s `GameRules.getBoolean(RULE_ENDER_PEARLS_VANISH_ON_DEATH)` → `original && !((ServerPlayer)getOwner()).wonGame`. (b) Remove the offline-owner discard in VB's HEAD inject: keep only the `vb$startChunkX/Z` capture, and do not cancel.
- config_gate: none

### MISC-17  End lighting: enabling End skylight makes outdoor End terrain fully day-lit, and the flash tint formula is not native
- severity: visual
- vb: `core/mixin/common/end_flashes/DimensionTypeMixin.java:53` `this.hasSkyLight = true;` for END effects. The 1.21.1 `LightTexture.updateLightTexture` then uses `float f = clientlevel.getSkyDarken(1.0F)` (End fixed time 6000 → `timeOfDay 0` → `getSkyDarken = 1.0`), `f1 = f*0.95F+0.05F = 1.0`, sky colour `new Vector3f(f,f,1).lerp(white,0.35) = (1,1,1)`, `f8 = getBrightness(dim, sky) * f1`. So sky 15 adds full white, the same as overworld noon. The flash is applied afterwards on the final pixels (`client/api/modules/end_flashes/EndFlashRenderer.java:124-149`): `baseFlash = intensity*(sky/15)`, `lerp toward (0.6745,0.3765,0.8039)` blended with additive `fr*baseFlash*invBlock`, `fg*…*0.4F`, `fb*…*1.2F` by blockFactor.
- native: `data/minecraft/dimension_type/the_end.json` `"minecraft:visual/sky_light_factor": 0.0`, `"sky_light_color": "#ac60cd"`, `"ambient_light_color": "#3f473f"`. `LightmapRenderStateExtractor.java:57-65` `skyFactor += intensity` (`/3` with boss fog). `lightmap.fsh`: `sky_brightness = get_brightness(sky_level) * SkyFactor; color = max(AmbientColor, NV); color += SkyLightColor * sky_brightness; …` then darkness, clamp and notGamma mix.
- difference: Native End sky light contributes nothing outside flashes, and during a flash adds `#ac60cd × brightness(sky) × intensity` before gamma. VB (1.21.1 lightmap plus VB's sky-light patch) lights every sky-exposed End block as at overworld noon, all the time, and applies a different post-gamma tint. Derived from code; check in game.
- fix_kind: mixin-mc (LightTexture.updateLightTexture) + drop VB's post-tint
- fix: in `updateLightTexture`, when `clientlevel.effects().skyType() == SkyType.END`:
  - `@ModifyVariable` on the sky factor local (the float stored from `f * 0.95F + 0.05F` / the `1.0F` lightning branch, LVT `f1`) → `EndFlashRenderer.calculateLightIntensity(minecraft, partialTicks)` (already 0 when no flash, `/3` with boss fog).
  - `@ModifyVariable` on the sky colour vector `vector3f` (constructed from `new Vector3f(f, f, 1.0F).lerp(…, 0.35F)`) → `new Vector3f(0.6745098F, 0.3764706F, 0.8039216F)`.
  - Cancel `LightTextureMixin.vb$applyEndFlashTint` (`EndFlashRenderer.applyLightTint`).
  - The ambient-colour part of the native lightmap redesign is engine-level and out of scope.
- config_gate: `end_flash_terrain_visuals`

### MISC-18  End flash rumble plays with linear distance falloff, starting from world origin
- severity: audio
- vb: `client/api/modules/end_flashes/DirectionalSoundInstance.java:20` `super(sound, source, random);` with no `setPosition()` in the constructor. `setPosition()` (`:26-32`, which also sets `this.attenuation = Attenuation.NONE`) runs only from `tick()` (`:35`).
- native: `client/resources/sounds/DirectionalSoundInstance.java:14-22` calls `this.setPosition();` in the constructor.
- difference: When the delayed (30-tick) sound starts, 1.21.1 `SoundEngine.play` reads position (0,0,0) and attenuation LINEAR (the AbstractSoundInstance default) and fixes the channel's attenuation for the whole sound. So in VB the rumble uses linear falloff (range 16×volume) and starts from world origin. After the first tick it sits 10 blocks away (≈37% volume at 10 of 16 blocks); on outer islands the first frame is inaudible. Native: no attenuation, always 10 blocks from the camera along the flash direction.
- fix_kind: mixin-vb (DirectionalSoundInstance.<init>)
- fix: `@Inject(method="<init>", at=@At("TAIL"))` → `this.setPosition();` (make it accessible with an `@Invoker` or a `@Shadow` private method call).
- config_gate: `end_flash_sky_visuals`

### MISC-19  "Hide Lightning Flashes" also hides the End flash in the sky
- severity: visual
- vb: `EndFlashRenderer.java:62` `if (level != null && !minecraft.options.hideLightningFlash().get() && !minecraft.gui.getBossOverlay().shouldCreateWorldFog())` before drawing the sky quad.
- native: `SkyRenderer.java:283-290` extracts `endFlashIntensity/XAngle/YAngle` with no option check; `LevelRenderer.java:342-347` draws it when `> 1.0E-5F`. Only `LightmapRenderStateExtractor.java:58` checks `hideLightningFlash()`. The 26.2 option text says "The sources of flashes themselves will still be visible."
- difference: With the accessibility option on, native still shows the purple flash in the End sky (only the terrain lighting flash is suppressed); VB removes both. Boss fog is equivalent, since native skips the whole sky pass under boss fog.
- fix_kind: mixin-vb (EndFlashRenderer.render)
- fix: `@Redirect` the `OptionInstance.get()` call for `hideLightningFlash` inside `render` to return `false` (keep the boss-fog check).
- config_gate: `end_flash_sky_visuals`

### MISC-20  End flash schedule runs on game time, not the End world clock
- severity: visual
- vb: `EndFlashRenderer.java:39` `state.tick(level.getGameTime());`
- native: `ClientLevel.java:310-311` `this.endFlashState.tick(this.getDefaultClockTime());` → `the_end.json "default_clock": "minecraft:the_end"`; `ServerClockManager.java:63-66` clocks advance only while `GameRules.ADVANCE_TIME` is true.
- difference: Native flashes freeze (and keep their schedule) when time advance is off, and their phase follows the End clock's total ticks. VB flashes keep running on world game time even with daylight cycle off, so timing and angles differ from a native world at the same tick.
- fix_kind: engine
- fix: 1.21.1 has no per-dimension clocks. The closest is to tick with `level.getDayTime()` (shared overworld day time, frozen by `doDaylightCycle=false`) via `@Redirect` of `getGameTime()` at line 39. The phase still won't match native (different clock origin).
- config_gate: `end_flash_sky_visuals`

### MISC-21  Music manager does not tick while the game is paused
- severity: audio
- vb: `core/mixin/client/music_on_pause/MinecraftMixin.java:26-35` `@WrapOperation(... MusicManager.tick ...) → instance.tick();`, but the wrapped call sits inside 1.21.1 `Minecraft.tick` `if (!this.pause) { this.musicManager.tick(); }` (`src-121 Minecraft.java:1874-1876`), so it still never runs while paused.
- native: `Minecraft.java:1813-1814` `this.musicManager.tick();` unconditional (outside `if (!this.pause)`).
- difference: With the pause menu open in single-player, the current song keeps playing (VB doesn't pause the MUSIC channel), but when it ends no new song is scheduled, the biome fade freezes, and the frequency option change doesn't take effect until you unpause. Native keeps ticking music during pause.
- fix_kind: mixin-mc (Minecraft.tick)
- fix: `@Inject(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/client/sounds/SoundManager;tick(Z)V"))` → `if (this.pause) this.musicManager.tick();` and drop VB's no-op WrapOperation.
- config_gate: none

### MISC-22  Time between songs is the minimum of two random draws
- severity: audio
- vb: `core/mixin/client/music_frequency/MusicManagerMixin.java:58-63` (after `isActive`): `int customDelay = gameMusicFrequency.getNextSongDelay(situationalMusic, random); this.nextSongDelay = Math.min(this.nextSongDelay, customDelay);`, then 1.21.1 `:53` `nextSongDelay = Math.min(nextSongDelay, Mth.nextInt(random, music.getMinDelay(), music.getMaxDelay()))`.
- native: `MusicManager.java:53-56` `this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));` (a single draw from `[min(minDelay,maxFreq), min(maxDelay,maxFreq)]`).
- difference: VB takes `min(U[min,max], U[min',max'])`. For DEFAULT with overworld game music (12000–24000) the expected gap drops from 18000 to 16000 ticks and the distribution is skewed short. It also uses 2 RNG calls instead of 1.
- fix_kind: mixin-mc (MusicManager.tick)
- fix: `@Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"))` → `return ((MusicFrequencyAccess)this).vb$frequency().getNextSongDelay(music, random);` and remove VB's `onSongFinishedDelay` inject.
- config_gate: none

### MISC-23  After a biome fade-out, the next song starts at full volume instead of fading in
- severity: audio
- vb: `core/mixin/client/music_fade/MusicManagerMixin.java:55-56` (startPlaying TAIL) → `MusicFadeManager.onStartPlaying()` (`:53-54`) `this.currentGain = this.getTargetVolume();` (1.0 outside the Pale Garden). VB's gain is per channel (`setVolume`).
- native: `MusicManager.java:25` `currentGain = 1.0F` persists across songs; `:108-135` fade in `gain += clamp(gain, 5e-4, 0.005)`, fade out `0.03*v + 0.97*gain`, stop at `<= 1e-4`; applied as `SoundEngine.updateCategoryVolume(MUSIC, gain)` (`SoundEngine.java:152-155`, multiplied in `calculateVolume` `:464`). `startPlaying` (`:70-83`) does not reset it.
- difference: In native, once music fades to silence in the Pale Garden (gain ≈1e-4), the next song after leaving starts near silence and swells up (≈200+ ticks to full). In VB it starts at full volume. Native's category-wide gain also survives slider changes; VB's per-channel volume is reset to full when the music slider is touched mid-fade.
- fix_kind: mixin-vb (MusicFadeManager.onStartPlaying / setVolume)
- fix: make `onStartPlaying` a no-op (keep `currentGain`), and apply the gain when the new channel starts (call `setVolume(engine, currentMusic, currentGain)` on the next `tickFade`; `tickFade` already runs while `currentGain != target`). Optionally store the gain in a `SoundEngine` mixin multiplier read by `calculateVolume` for MUSIC, to match native exactly.
- config_gate: none

### MISC-24  With music volume at 0, songs don't start silently, yet the "Now Playing" toast still shows
- severity: audio
- vb: `core/mixin/client/music_toast/MusicManagerMixin.java:28` shows the toast unconditionally at `SoundManager.play`. 1.21.1 `SoundEngine.java:454-456` `if (f2 == 0.0F && !p_sound.canStartSilent()) { … "volume was zero" … return; }`, so music isn't started and `isActive` is false next tick.
- native: `SoundEngine.java:385-392` `if (volume == 0.0F) { if (!instance.canStartSilent() && soundSource != SoundSource.MUSIC) return NOT_STARTED; startedSilently = true; }`; `MusicManager.java:73-80` toast only on `STARTED`, `toastShown=false` on `STARTED_SILENTLY`; `:85-90` + `Options.java:1320-1324` show the toast when the MUSIC/MASTER slider is raised above 0.
- difference: Native keeps a muted song running so raising the slider reveals it mid-track (with the toast then). VB/1.21.1 never starts it, cycles songs invisibly by the frequency timer, and flashes a toast naming a song that isn't playing.
- fix_kind: mixin-mc (SoundEngine.play, MusicManager.startPlaying, Options sound slider)
- fix:
  - `@WrapOperation` the `SoundInstance.canStartSilent()` call in `SoundEngine.play` → `original || instance.getSource() == SoundSource.MUSIC`.
  - In VB's toast inject, show the toast only if `ToastInstance.getFinalSoundSourceVolume(options, MUSIC) > 0`, otherwise set a `vb$toastPending` flag.
  - Hook the MUSIC/MASTER `OptionInstance` callbacks (`Options.createSoundSliderOptionInstance` lambda) to call `showNowPlayingToast()` when the flag is set and the final volume is > 0.
- config_gate: none

### MISC-25  Pause-menu music-note colour cycles twice as fast; toast key ignores the sound namespace
- severity: cosmetic
- vb: `core/mixin/client/music_toast/PauseScreenMixin.java:37` calls `NowPlayingToast.tickMusicNotes()` from `render` (every frame, gated by 25 ms). `client/api/modules/music_toast/NowPlayingToast.java:86-87` `translationKey = path.replace("/", ".")` + `translatableWithFallback`.
- native: `PauseScreen.java:215-220` ticks the notes in `tick()` (once per 50 ms game tick); `MusicManager.java:142-151` `sound.getLocation().toShortLanguageKey()` (`namespace.path` for non-minecraft sounds) and `NowPlayingToast.java:62-64` `Component.translatable(key.replace("/", "."))` with no fallback.
- difference: In the pause menu VB advances the colour tick up to 40×/s instead of 20×/s (0.75 s per colour vs native 1.5 s). Modded music shows "music.x" keys / "Modid - X" fallback instead of native's `modid.music.x` key.
- fix_kind: mixin-vb (PauseScreenMixin, NowPlayingToast.getNowPlayingString)
- fix: move the `tickMusicNotes()` call to an `@Inject(method="tick", at=TAIL)` on PauseScreen. Build the key as `location.toShortLanguageKey().replace("/", ".")` and drop the fallback.
- config_gate: options `musicToast`

### MISC-26  Pausing also pauses UI sounds
- severity: audio
- vb: `core/mixin/client/music_on_pause/MinecraftMixin.java:23` `pauseAllExcept(SoundSource.MUSIC)` (and it zeroes the volume of each paused channel).
- native: `Minecraft.java:1242` `this.soundManager.pauseAllExcept(SoundSource.MUSIC, SoundSource.UI);`
- difference: A UI sound playing at the moment of pausing (e.g. a button click or toast sound) is frozen until unpause in VB; native lets UI sounds finish.
- fix_kind: mixin-vb (MinecraftMixin.vb$dontPauseMusic)
- fix: change to `pauseAllExcept(SoundSource.MUSIC, SoundSource.UI)`, and in `SoundEngineMixin.vb$onResume` skip `SoundSource.UI` as well as MUSIC.
- config_gate: none

### MISC-27  Cloud distance is tied to render distance (×96 blocks, square) instead of the native Cloud Distance option
- severity: visual
- vb: `core/mixin/client/clouds/LevelRendererMixin.java:29` fancy loop bounds `1 - renderDistance .. renderDistance` sections of 8 cells × 12 blocks (= ±96·RD blocks, square); `:51,62` fast `±RD*4`.
- native: `Options.java:192-202` `cloudRange` "options.renderCloudsDistance", `IntRange(2, 128)`, default **128** chunks (graphics presets 32/64/128); `CloudRenderer.java:143-144,247-252` `radiusBlocks = range*16; radiusCells = ceil(radiusBlocks/12)` with a circular mask `x²+z² <= r²`; `Camera.java:95` far plane `max(renderDistance*4, cloudRange*16)`.
- difference: Native has a separate Cloud Distance slider (default 2048-block radius, circular, far plane extended). VB has no option: at RD 12 the radius is ±1152 blocks, square, and clouds beyond the 1.21.1 far plane (RD·64 blocks) are clipped. The lang key `options.renderCloudsDistance` is also missing.
- fix_kind: engine
- fix: add a client option `cloudRange` (2–128, default 128) next to VB's options mixin. In `vb$getCloudRenderDistance()` return `ceil(cloudRange*16/96)` sections and skip cells outside `x²+z² <= (cloudRange*16/12)²` in the 1.21.1 `buildClouds` cell loop (`@ModifyExpressionValue` on the inner add condition). Raise `GameRenderer.getDepthFar()` to `max(current, cloudRange*16)`. An exact match needs the 26.x CloudRenderer.
- config_gate: `extended_cloud_reach`

### MISC-28  Boats still break into planks and sticks after falling
- severity: gameplay
- vb: `common/level/entities/boat/CustomBoat.java:49-66` and `CustomChestBoat.java:50-67`: `if (this.fallDistance > 3.0F) { … this.kill(); … spawnAtLocation(getBoatType().getPlanks()) ×3, Items.STICK ×3 }`. Vanilla boats run the same 1.21.1 `Boat.checkFallDamage`.
- native: `AbstractBoat.java:723-732` `if (onGround) { this.resetFallDistance(); } else if (!water below && ya < 0) fallDistance -= ya;` (no breaking).
- difference: Native boats never break from falls; VB (all boats, including pale oak) breaks into 3 planks + 3 sticks after a >3-block drop onto land.
- fix_kind: mixin-mc (Boat.checkFallDamage) + mixin-vb (CustomBoat/CustomChestBoat.checkFallDamage)
- fix: `@Inject(method="checkFallDamage", at=HEAD, cancellable=true)` on `net.minecraft.world.entity.vehicle.Boat` and both VB classes: `this.lastYd = getDeltaMovement().y; if (!isPassenger()) { if (onGround) resetFallDistance(); else if (!level().getFluidState(blockPosition().below()).is(FluidTags.WATER) && y < 0) fallDistance -= (float)y; } ci.cancel();`
- config_gate: none

### MISC-29  Pale oak boats placed or dispensed ignore item components (custom name etc.)
- severity: gameplay
- vb: `common/level/items/CustomBoatItem.java:58,80-84` `new CustomBoat(level, x, y, z, type)` with no stack config; `common/integrations/dispenser/BoatDispenseBehavior.java:48` same.
- native: `BoatItem.java:78-90` `EntityType.createDefaultStackConfig(serverLevel, itemStack, player).apply(boat)`; `BoatDispenseItemBehavior.execute` `createDefaultStackConfig(level, dispensed, null).apply(boat)`. 1.21.1 `BoatItem.java:83` has it too.
- difference: A renamed (anvil) pale oak or poplar boat item places an unnamed boat in VB; native and vanilla-wood 1.21.1 boats keep the name and `entity_data`.
- fix_kind: mixin-vb (CustomBoatItem.getBoat, BoatDispenseBehavior.execute)
- fix: after constructing, `if (level instanceof ServerLevel sl) EntityType.<Boat>createDefaultStackConfig(sl, heldStack, player).accept(boat);` (dispenser: `player = null`, `stack`). Needs the stack passed into `getBoat` (`@ModifyReturnValue` at line 58 with the captured stack).
- config_gate: none

### MISC-30  Boats pick up elder guardians and creakings
- severity: gameplay
- vb: `core/mixin/common/boat_blacklist/BoatMixin.java:22` excludes only `SULFUR_CUBE` and `AbstractNautilus`, on top of 1.21.1 `!(entity instanceof WaterAnimal) && !(entity instanceof Player)`.
- native: `AbstractBoat.java:282-288` `… && entity instanceof LivingEntity && !entity.is(EntityTypeTags.CANNOT_BE_PUSHED_ONTO_BOATS)`; tag = `player, elder_guardian, cod, pufferfish, salmon, tropical_fish, dolphin, squid, glow_squid, tadpole, creaking, nautilus, zombie_nautilus, sulfur_cube`.
- difference: Elder guardians (a Monster, not a WaterAnimal) and creakings can be scooped into boats in VB; native blocks them. Regular guardians and axolotls are allowed in both.
- fix_kind: mixin-vb (BoatMixin.vb$skipMobsOnBoat) + data
- fix: add `data/minecraft/tags/entity_type/cannot_be_pushed_onto_boats.json` with the 14 native values, and in the wrap return `false` when `entity.getType().is(thatTag)`.
- config_gate: none

### MISC-31  "Monsters on Peaceful" is not implemented
- severity: gameplay
- vb: `core/mixin/common/monsters_on_peaceful/NaturalSpawnerMixin.java` is an empty class. Tag `data/minecraft/tags/entity_type/monsters_that_spawn_on_peaceful.json = [piglin, hoglin, sulfur_cube]` is registered (`ModEntityTypeTags.java:11`) but never read. 1.21.1 `MinecraftServer.java:1274-1276` `isSpawningMonsters() { return difficulty != PEACEFUL; }` so the MONSTER category never spawns on Peaceful.
- native: `MinecraftServer.java:1322` `level.setSpawnSettings(level.isSpawningMonsters())` with `ServerLevel.java:1791-1793` `SPAWN_MOBS && SPAWN_MONSTERS` (gamerules only); `EntityType.java:303-305` `canSpawn = isAllowedInPeaceful() || difficulty != PEACEFUL`, checked in `create` (`:314`); `Mob.java:698-700` despawns only `!isAllowedInPeaceful()`. MONSTER-category types allowed in Peaceful: `camel_husk, ender_dragon, piglin, shulker, sulfur_cube, zombie_horse, zombie_nautilus` (hoglin is `notInPeaceful`).
- difference: Native Peaceful naturally spawns piglins (nether wastes/crimson forest), zombie horses, camel husks, zombie nautilus and sulfur cubes; VB spawns none. VB's tag is also wrong (includes hoglin; lacks zombie_horse, camel_husk, zombie_nautilus, shulker, ender_dragon).
- fix_kind: mixin-mc (MinecraftServer.isSpawningMonsters, EntityType.create, Mob.checkDespawn)
- fix:
  - `@ModifyReturnValue MinecraftServer.isSpawningMonsters → true`.
  - `@Inject(method="create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;", at=HEAD, cancellable=true)` on EntityType: `if (level.getDifficulty()==PEACEFUL && this.getCategory()==MobCategory.MONSTER && !this.is(TAG)) cir.setReturnValue(null)`. Commands use `EntityType.loadEntityRecursive`, so native's summon error is a separate check.
  - Fix the tag to `[camel_husk, ender_dragon, piglin, shulker, sulfur_cube, zombie_horse, zombie_nautilus]` with `replace: true`.
  - `Mob.checkDespawn`: `@WrapOperation shouldDespawnInPeaceful()` → `original || (PEACEFUL && MONSTER && !tag)`.
- config_gate: none

### MISC-32  Lead interaction sounds and game events are the 1.21.1 ones; mob equipment shearing is missing
- severity: audio
- vb: `common/integrations/interactions/LeashInteraction.java:41-42` `GameEvent.ENTITY_INTERACT` + `SoundEvents.LEASH_KNOT_PLACE`; `:57-58` `ENTITY_INTERACT` + `LEASH_KNOT_BREAK`; `:75` `LEASH_KNOT_PLACE`; `:92` `SoundEvents.SHEEP_SHEAR`. 1.21.1 sounds: `entity.leash_knot.place = leashknot/place1-3`, `entity.leash_knot.break = leashknot/break1-3`.
- native: `Entity.java:2299-2300` `GameEvent.ENTITY_ACTION` + `SoundEvents.LEAD_TIED`; `:2327` `LEAD_UNTIED`; `:2345` `LEAD_TIED`; `:2359` `SoundEvents.SHEARS_SNIP`; `:2310-2315` `Mob.attemptToShearEquipment` (shear saddles/harnesses/carpets via `Equippable.canBeSheared`). 26.2 sounds.json: `item.lead.tied = entity/leashknot/leash1-3` (subtitle `subtitles.item.lead.tied`), `item.lead.untied = entity/leashknot/unleash1-3`, `item.shears.snip = mob/sheep/shear` (subtitle `subtitles.item.shears.snip`).
- difference: VB plays different .ogg files (place/break instead of leash/unleash) with old subtitles. Mass-leashing emits `entity_interact` (sculk frequency 6) instead of `entity_action` (frequency 4). Shearing leashes shows the "Shears click" sheep subtitle instead of "Shears snip". Shearing equipment off mobs is absent.
- fix_kind: asset-override + mixin-vb (LeashInteraction.onInteract); equipment shearing is engine (no `Equippable` in 1.21.1)
- fix: register `minecraft:item.lead.tied`, `item.lead.untied`, `item.shears.snip` with the 26.2 sounds.json entries above. The leash1-3/unleash1-3 .ogg files are 26.2 assets and must be shipped. `@Redirect` the four `playSound`s and the `SHEEP_SHEAR` field read to them, and the line-41 `GameEvent.ENTITY_INTERACT` to `GameEvent.ENTITY_ACTION`. Add lang `subtitles.item.lead.tied/untied`, `subtitles.item.shears.snip`.
- config_gate: `modern_leashes`

### MISC-33  Data component registered as `minecraft:minimum_attack_range` instead of `minimum_attack_charge`
- severity: data
- vb: `common/registries/items/ModDataComponents.java:48-49` `MINIMUM_ATTACK_CHARGE = register("minimum_attack_range", …floatRange(0,1)…)`
- native: `core/component/DataComponents.java:127` `"minimum_attack_charge"`
- difference: `/give @s stone_spear[minimum_attack_charge=0.5]` and datapacks using the native id fail in VB. The VB id collides in name with native `attack_range`, but they are separate. Probably also filed by the spear area.
- fix_kind: engine (registry id; VB class field)
- fix: register an alias. In a `MappedRegistry` mixin (or NeoForge `RegisterEvent` + `Registry.addAlias`) add an alias `minecraft:minimum_attack_charge → minecraft:minimum_attack_range` for `DATA_COMPONENT_TYPE`, so parsing accepts the native name.
- config_gate: `has_spears`

### MISC-34  Language: missing and outdated strings in misc features
- severity: data
- vb: effective en_us (1.21.1 + `assets/minecraft/lang/en_us.json` + `assets/vanillabackport/lang/en_us.json`) lacks `argument.hexcolor.invalid` (used by `HexColorArgument.ERROR_INVALID_HEX`) and `options.renderCloudsDistance`. It keeps 1.21.1 `options.hideLightningFlashes = "Hide Lightning Flashes"` / tooltip "Prevents Lightning Bolts from making the sky flash…". Every other misc-scope key (bundle, waypoint, music, music toast, jukebox, painting, end flash) matches native exactly.
- native: `"argument.hexcolor.invalid": "Invalid hex color code '%s'"`, `"options.hideLightningFlashes": "Hide Sky Flashes"`, `"options.hideLightningFlashes.tooltip": "Prevents Lightning Bolts or other environmental effects from making the sky flash. The sources of flashes themselves will still be visible."`, `"options.renderCloudsDistance": "Cloud Distance"`
- difference: An invalid hex in `/waypoint … color hex zz` prints the raw key; the accessibility option still talks only about lightning although it now also suppresses End flashes.
- fix_kind: asset-override (`assets/minecraft/lang/en_us.json`)
- fix: add the four native strings above (and the MISC-08 item names if you add those items).
- config_gate: none

### MISC-35  Ruined-portal lodestone pool lists its entries in reverse order
- severity: data
- vb: `common/integrations/LootIntegrations.java:127-128` `.add(LootItem.lootTableItem(Items.LODESTONE).setWeight(2)…UniformGenerator.between(1,2))` then `.add(EmptyLootItem.emptyItem().setWeight(1))`
- native: `loot_table/chests/ruined_portal.json` pool 1 entries `[ {"type":"minecraft:empty"}, {"name":"minecraft:lodestone","weight":2,…} ]`
- difference: Probabilities match (2/3 lodestone), but entry order decides which RNG values map to which outcome, so the same seed yields different ruined-portal loot than native. Village bundle pools match exactly (bundle w1 first, empty w2, appended as pool 1).
- fix_kind: mixin-vb (LootIntegrations modify)
- fix: swap the two `.add(...)` calls (empty weight 1 first, lodestone weight 2 second).
- config_gate: `lodestone_loot`

### MISC-36  Cushion and wool-slab furnace fuel times are wrong
- severity: gameplay
- vb: `common/integrations/BlockIntegrations.java:22-23` `event.registerFuelItem(ModItemTags.WOOL_SLABS, 100); event.registerFuelItem(ModItemTags.CUSHIONS, 200);` (wool stairs 100 at `:21`)
- native (26.3-pre-1): `Items.java:507` wool slabs `cookingFuel(ContextIntProviders.COOKING_TIME_WOOL_SLABS)`; `:2024` cushions `cookingFuel(COOKING_TIME_WOOL_SLABS)`; `data/minecraft/context_int_provider/cooking/time_wool_slabs.json` `{"type":"minecraft:div","left":50,"right":<normal_burn_time_reduction_factor = 1>}` = 50 ticks; `time_wool.json` left 100 (wool, wool stairs, `Items.java:501-504`).
- difference: A wool slab burns 100 ticks in VB (native 50); a cushion burns 200 ticks (native 50, i.e. 4x). Wool stairs (100) match.
- fix_kind: data-override (NeoForge data map) or mixin-vb (BlockIntegrations.registerFuel)
- fix: ship `data/neoforge/data_maps/item/furnace_fuels.json` = `{"values": {"#minecraft:wool_slabs": {"burn_time": 50}, "#minecraft:cushions": {"burn_time": 50}}}`, or `@ModifyArg(index=1)` on the two `registerFuelItem` calls → `50`.
- config_gate: none

### MISC-37  Cushion details: break particles, lightning drop, sit-click response
- severity: gameplay
- vb: `Cushion.java:100-114` `sendParticles(..., 10, getBbWidth()/4F, getBbWidth()/4F, getBbWidth()/4F, 0.05)` at `getY(0.67F)`; `:185-190` `thunderHit` → `kill(); dropItem(null);` (dropped item is exposed to the lightning fire); `:70-81` `interact`: client returns `SUCCESS`, server `CONSUME`, and `PASS` if riding fails.
- native (26.3-pre-1): `Cushion.java:115-129` Y spread `getBbHeight()/4F` (0.0625 vs VB 0.25), `getY(0.6666666666666666)`; `:21-33` `dropItem` calls `itemEntity.setInvulnerableTime(20)` when `causedBy instanceof LightningBolt`; `:53-63` `interact`: `if (secondary || isVehicle()) PASS; else if (!client && player.startRiding(this)) { CUSHION_SIT; SUCCESS_SERVER } else CONSUME` (client: CONSUME, no hand swing, never falls through to the held item).
- difference: VB break particles spread 4x taller. A lightning-broken cushion's item can burn up in VB, while native protects it for 20 ticks. Clicking a cushion swings your hand client-side in VB, and a failed mount passes the click on to the held item.
- fix_kind: mixin-vb (Cushion.showBreakingParticles / thunderHit / interact)
- fix:
  - `@ModifyArg` the yDist arg of `sendParticles` → `getBbHeight()/4F`.
  - In `thunderHit`, after `kill()`, play the break sound and particles, then `ItemEntity e = spawnAtLocation(<cushion stack with custom name>); if (e != null) e.setInvulnerableTime(20);` (respect `doEntityDrops`), instead of `dropItem(null)`.
  - `interact` HEAD cancellable: `PASS` if secondary use or a vehicle; on the server, `startRiding` → sit sound + `SUCCESS`; otherwise `CONSUME`.
- config_gate: none
