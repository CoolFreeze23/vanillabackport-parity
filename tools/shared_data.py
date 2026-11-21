"""Generates the data files several areas share (advancements, a few vanilla tags) into src/main/resources.

Run from the project root:  python -I tools/shared_data.py
Inputs (read-only, under $VBP_REF): src-mc (26.2), src-mc263 (26.3-pre-1, only for content 26.2 lacks),
src-vb (VanillaBackport 1.2.2), src-121data (vanilla 1.21.1 data).
"""
import json
import os
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

VB = REF + '/src-vb'
V121 = REF + '/src-121data'
N262 = REF + '/src-mc'
N263 = REF + '/src-mc263'
OUT = 'src/main/resources'


def load(*roots_rel):
    *roots, rel = roots_rel
    for r in roots:
        p = os.path.join(r, rel)
        if os.path.exists(p):
            with open(p, encoding='utf-8') as f:
                return json.load(f)
    return None


def write(rel, data):
    p = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


ADV = 'data/minecraft/advancement/'

# Gameplay advancements: start from what is loaded today (VB's copy, else vanilla 1.21.1) and add the native
# criteria it lacks. Requirement shape follows the native file: one OR group (kill_a_mob) or one group per criterion.
GAMEPLAY = {
    'adventure/adventuring_time': (N262, ['minecraft:pale_garden', 'minecraft:sulfur_caves']),
    'adventure/kill_a_mob': (N262, ['minecraft:camel_husk', 'minecraft:creaking', 'minecraft:parched',
                                    'minecraft:zombie_horse', 'minecraft:zombie_nautilus']),
    'adventure/kill_all_mobs': (N262, ['minecraft:camel_husk', 'minecraft:creaking', 'minecraft:parched',
                                       'minecraft:zombie_horse', 'minecraft:zombie_nautilus']),
    'husbandry/bred_all_animals': (N262, ['minecraft:nautilus']),
}
EXTRA_263 = {'adventure/adventuring_time': ['minecraft:dappled_forest']}

for name, (native_root, keys) in GAMEPLAY.items():
    rel = ADV + name + '.json'
    cur = load(VB, V121, rel)
    nat = load(native_root, rel)
    adds = [(nat, k) for k in keys]
    if name in EXTRA_263:
        nat3 = load(N263, rel)
        adds += [(nat3, k) for k in EXTRA_263[name]]
    single_or_group = len(cur['requirements']) == 1 and len(cur['requirements'][0]) > 1
    for src, key in adds:
        if key in cur['criteria']:
            continue
        cur['criteria'][key] = src['criteria'][key]
        if single_or_group:
            cur['requirements'][0].append(key)
        else:
            cur['requirements'].append([key])
    write(rel, cur)

# Wax On / Wax Off: the native block lists (COPPER-17).
for name in ('husbandry/wax_on', 'husbandry/wax_off'):
    rel = ADV + name + '.json'
    cur = load(VB, V121, rel)
    nat = load(N262, rel)

    def blocks(adv):
        crit = next(iter(adv['criteria'].values()))
        loc = crit['conditions']['location']
        for cond in loc:
            if 'predicate' in cond and 'block' in cond['predicate']:
                return cond['predicate']['block']
        raise SystemExit('no block predicate in ' + name)

    nb = blocks(nat)['blocks']
    cb = blocks(cur)
    have = cb['blocks'] if isinstance(cb['blocks'], list) else [cb['blocks']]
    cb['blocks'] = have + [b for b in (nb if isinstance(nb, list) else [nb]) if b not in have]
    write(rel, cur)

# Dried ghast: native criterion name, keeping VB's old one so existing progress still counts (GHAST-45).
rel = ADV + 'husbandry/place_dried_ghast_in_water.json'
cur = load(VB, rel)
old = next(k for k in cur['criteria'])
if old != 'place_dried_ghast_in_water':
    cur['criteria']['place_dried_ghast_in_water'] = cur['criteria'][old]
    cur['requirements'] = [['place_dried_ghast_in_water', old]]
write(rel, cur)

# Recipe-unlock advancements whose recipe VanillaBackport (or this mod) ships: use the native file. VB's own unlock
# advancements for the same recipes under other paths are disabled, so each recipe has exactly the native unlock.
take = json.load(open('tools/data/recipe_adv_take.json'))
native_rewards = {}
for rel, root in take.items():
    data = load(REF + '/' + root, rel)
    for crit in data.get('criteria', {}).values():
        cond = crit.get('conditions', {})
        # 26.3 recipe_unlocked takes "recipes"; 1.21.1 takes a single "recipe".
        if crit.get('trigger') == 'minecraft:recipe_unlocked' and 'recipes' in cond:
            r = cond.pop('recipes')
            cond['recipe'] = r if isinstance(r, str) else r[0]
    write(rel, data)
    for r in data.get('rewards', {}).get('recipes', []):
        native_rewards[r] = rel
disabled = []
for dp, _, fs in os.walk(os.path.join(VB, ADV, 'recipes')):
    for f in fs:
        rel = os.path.relpath(os.path.join(dp, f), VB).replace(os.sep, '/')
        if rel in take:
            continue
        data = load(VB, rel)
        recs = data.get('rewards', {}).get('recipes', [])
        if recs and all(native_rewards.get(r) not in (None, rel) for r in recs):
            write(rel, {'neoforge:conditions': [{'type': 'neoforge:false'}]})
            disabled.append(rel)

# How Did We Get Here: native adds Breath of the Nautilus (MAYHEM-31). The effect is dropped again at load when
# VB's nautilus is off (AdvancementGates).
rel = ADV + 'nether/all_effects.json'
cur = load(VB, V121, rel)
cur['criteria']['all_effects']['conditions']['effects']['minecraft:breath_of_the_nautilus'] = {}
write(rel, cur)

# Tags (NeoForge "remove" keeps other mods' additions intact; "replace": true would drop them).
write('data/minecraft/tags/block/replaceable.json', {'replace': False, 'values': [
    'minecraft:bush', 'minecraft:leaf_litter', 'minecraft:short_dry_grass', 'minecraft:tall_dry_grass',
    'minecraft:red_shrub', 'minecraft:resin_clump']})
write('data/minecraft/tags/block/replaceable_by_trees.json', {'replace': False, 'values': [
    'minecraft:open_eyeblossom', 'minecraft:closed_eyeblossom'],
                                                              'remove': ['minecraft:red_shrub']})
write('data/minecraft/tags/item/piglin_loved.json', {'replace': False, 'values': [
    'minecraft:golden_spear', 'minecraft:golden_nautilus_armor']})
write('data/minecraft/tags/item/non_flammable_wood.json', {'replace': False, 'values': [
    'minecraft:crimson_shelf', 'minecraft:warped_shelf']})

# Tag sweep (tools/datasweep/tags.py): native members VB lacks, and VB members native dropped.
# Native 26.2 lists supports_big_dripleaf explicitly without pale_moss_block; 1.21.1 pulls it in through #dirt.
write('data/minecraft/tags/block/big_dripleaf_placeable.json', {'replace': False, 'values': [],
                                                                 'remove': ['minecraft:pale_moss_block']})
# Native reaches the spear damage type through #is_player_attack, which 1.21.1's copies of these tags don't reference.
for t in ('can_break_armor_stand', 'panic_causes'):
    write('data/minecraft/tags/damage_type/' + t + '.json', {'replace': False, 'values': ['minecraft:spear']})
write('data/minecraft/tags/entity_type/can_breathe_under_water.json', {'replace': False, 'values': ['minecraft:nautilus']})
write('data/minecraft/tags/entity_type/fall_damage_immune.json', {'replace': False, 'values': ['minecraft:copper_golem']})
# Native bee_food lists open_eyeblossom only; 1.21.1's bee_food is #flowers, which VB's small_flowers fills with both.
write('data/minecraft/tags/item/bee_food.json', {'replace': False, 'values': [], 'remove': ['minecraft:closed_eyeblossom']})

# Native camel spawn-surface tag (SPAWN-12); VB reads camel_spawnable_on, which now includes the native one.
write('data/minecraft/tags/block/camels_spawnable_on.json', {'replace': False, 'values': ['#minecraft:sand']})
write('data/minecraft/tags/block/camel_spawnable_on.json', {'replace': False, 'values': ['#minecraft:camels_spawnable_on']})

print('gameplay advancements:', len(GAMEPLAY) + 3)
print('recipe advancements:', len(take), 'disabled VB duplicates:', len(disabled))
