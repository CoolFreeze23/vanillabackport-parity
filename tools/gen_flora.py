"""Generates the flora worldgen overrides (1.21.1 format) from the 1.21.1 vanilla data and the 26.2 native data."""
import json, os, copy, sys
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

NAT = REF + '/src-mc/data/minecraft/worldgen'
OLD = REF + '/mc-1.21.1-data/data/minecraft/worldgen'
OUT = 'src/main/resources/data'
JAVA = 'src/main/java/com/coolfreeze23/vbparity/flora/worldgen/NativeVegetalOrder.java'

def load(base, kind, name):
    with open(os.path.join(base, kind, name + '.json'), encoding='utf-8') as f:
        return json.load(f)

def write(rel, obj):
    p = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')

def placed_inline(configured):
    return {"feature": configured, "placement": []}

def switch(cases, default):
    return {"type": "vbparity:config_switch", "config": {"cases": [{"when": w, "feature": f} for w, f in cases], "default": default}}

LITTER_TO_OLD = {
    'minecraft:oak_leaf_litter': 'minecraft:oak_checked',
    'minecraft:birch_leaf_litter': 'minecraft:birch_checked',
    'minecraft:dark_oak_leaf_litter': 'minecraft:dark_oak_checked',
    'minecraft:fancy_oak_leaf_litter': 'minecraft:fancy_oak_checked',
    'minecraft:oak_bees_0002_leaf_litter': 'minecraft:oak_bees_0002',
    'minecraft:birch_bees_0002_leaf_litter': 'minecraft:birch_bees_0002',
    'minecraft:fancy_oak_bees_0002_leaf_litter': 'minecraft:fancy_oak_bees_0002',
}

def is_fallen(f):
    return isinstance(f, str) and f.startswith('minecraft:fallen_')

def variant(native_cfg, old_cfg, fallen, litter):
    """Native random_selector translated to 1.21.1 names; drop fallen entries unless `fallen`,
    map *_leaf_litter trees back to their 1.21.1 trees unless `litter`. Inline entries come from 1.21.1."""
    old_inline = [e['feature'] for e in old_cfg['config']['features'] if not isinstance(e['feature'], str)]
    out = []
    for e in native_cfg['config']['features']:
        f = e['feature']
        if not isinstance(f, str):
            f = old_inline.pop(0)
        elif is_fallen(f):
            if not fallen:
                continue
        elif f in LITTER_TO_OLD and not litter:
            f = LITTER_TO_OLD[f]
        out.append({"chance": e['chance'], "feature": f})
    d = native_cfg['config']['default']
    if not isinstance(d, str):
        d = old_cfg['config']['default']
    elif d in LITTER_TO_OLD and not litter:
        d = LITTER_TO_OLD[d]
    return {"type": "minecraft:random_selector", "config": {"features": out, "default": d}}

def check_equal_to_old(v, old_cfg, name):
    a = json.dumps(v['config'], sort_keys=True)
    b = json.dumps({"features": old_cfg['config']['features'], "default": old_cfg['config']['default']}, sort_keys=True)
    if a != b:
        sys.exit(f'{name}: native-without-flora does not reduce to 1.21.1\n{a}\n{b}')

# ---- FLORA-07/08: tree selectors that are configured random_selectors in 1.21.1
CONF = {  # 1.21.1 name -> 26.2 name
    'trees_birch_and_oak': 'trees_birch_and_oak_leaf_litter',
    'dark_forest_vegetation': 'dark_forest_vegetation',
    'trees_flower_forest': 'trees_flower_forest',
    'birch_tall': 'birch_tall',
    'trees_jungle': 'trees_jungle',
    'trees_sparse_jungle': 'trees_sparse_jungle',
    'trees_taiga': 'trees_taiga',
    'trees_old_growth_pine_taiga': 'trees_old_growth_pine_taiga',
    'trees_old_growth_spruce_taiga': 'trees_old_growth_spruce_taiga',
    'trees_plains': 'trees_plains',
    'trees_savanna': 'trees_savanna',
    'trees_windswept_hills': 'trees_windswept_hills',
}
for old_name, nat_name in CONF.items():
    old = load(OLD, 'configured_feature', old_name)
    nat = load(NAT, 'configured_feature', nat_name)
    check_equal_to_old(variant(nat, old, False, False), old, old_name)
    has_litter = any(isinstance(e['feature'], str) and e['feature'] in LITTER_TO_OLD for e in nat['config']['features']) \
        or (isinstance(nat['config']['default'], str) and nat['config']['default'] in LITTER_TO_OLD)
    cases = []
    if has_litter:
        cases.append((["fallen_trees", "leaf_litter"], placed_inline(variant(nat, old, True, True))))
    cases.append((["fallen_trees"], placed_inline(variant(nat, old, True, False))))
    if has_litter:
        cases.append((["leaf_litter"], placed_inline(variant(nat, old, False, True))))
    write(f'minecraft/worldgen/configured_feature/{old_name}.json', switch(cases, placed_inline(old)))

# ---- placed features that point straight at one tree in 1.21.1
PLACED = {  # name -> (fallen entry, chance, default when fallen on, litter default or None)
    'trees_birch': ('minecraft:fallen_birch_tree', 0.0125, 'minecraft:birch_bees_0002', None),
    'trees_snowy': ('minecraft:fallen_spruce_tree', 0.0125, 'minecraft:spruce_checked', None),
    'trees_badlands': ('minecraft:fallen_oak_tree', 0.0125, 'minecraft:oak_checked', 'minecraft:oak_leaf_litter'),
}
for name, (fallen, chance, dflt, litter_dflt) in PLACED.items():
    old = load(OLD, 'placed_feature', name)
    nat = load(NAT, 'configured_feature', name)
    assert nat['config']['features'] == [{"chance": chance, "feature": fallen}], name
    assert nat['config']['default'] == (litter_dflt or dflt), name
    def rs(d):
        return placed_inline({"type": "minecraft:random_selector", "config": {"features": [{"chance": chance, "feature": fallen}], "default": d}})
    cases = []
    if litter_dflt:
        cases.append((["fallen_trees", "leaf_litter"], rs(litter_dflt)))
    cases.append((["fallen_trees"], rs(dflt)))
    if litter_dflt:
        cases.append((["leaf_litter"], litter_dflt))
    write(f'minecraft/worldgen/placed_feature/{name}.json',
          {"feature": switch(cases, placed_inline(old['feature'])), "placement": old['placement']})

# ---- FLORA-08: leaf-litter tree variants (1.21.1 tree + native place_on_ground decorators)
LITTER = {'oak_leaf_litter': ('oak', 'oak_sapling'), 'birch_leaf_litter': ('birch', 'birch_sapling'),
          'dark_oak_leaf_litter': ('dark_oak', 'dark_oak_sapling'), 'fancy_oak_leaf_litter': ('fancy_oak', 'oak_sapling'),
          'oak_bees_0002_leaf_litter': ('oak_bees_0002', 'oak_sapling'), 'birch_bees_0002_leaf_litter': ('birch_bees_0002', 'birch_sapling'),
          'fancy_oak_bees_0002_leaf_litter': ('fancy_oak_bees_0002', 'oak_sapling')}
for name, (base, sapling) in LITTER.items():
    tree = load(OLD, 'configured_feature', base)
    nat = load(NAT, 'configured_feature', name)
    pog = [copy.deepcopy(d) for d in nat['config']['decorators'] if d['type'] == 'minecraft:place_on_ground']
    other = [d for d in nat['config']['decorators'] if d['type'] != 'minecraft:place_on_ground']
    assert json.dumps(other, sort_keys=True) == json.dumps(tree['config']['decorators'], sort_keys=True), name
    assert len(pog) == 2
    for d in pog:
        d.setdefault('radius', 2)  # codec default, written out
    tree['config']['decorators'] = tree['config']['decorators'] + pog
    write(f'minecraft/worldgen/configured_feature/{name}.json', tree)
    natp = load(NAT, 'placed_feature', name)
    assert natp['placement'][0]['predicate']['state']['Name'] == 'minecraft:' + sapling, name
    write(f'minecraft/worldgen/placed_feature/{name}.json', natp)

# ---- FLORA-05: cactus column with the native flower layer
old = load(OLD, 'configured_feature', 'patch_cactus')
nat = load(NAT, 'configured_feature', 'cactus')
with_flower = copy.deepcopy(old)
col = with_flower['config']['feature']['feature']['config']
assert len(col['layers']) == 1 and col['layers'][0] == nat['config']['layers'][0]
col['layers'].append(nat['config']['layers'][1])
write('minecraft/worldgen/configured_feature/patch_cactus.json', switch([(["cactus_flowers"], placed_inline(with_flower))], placed_inline(old)))
write('minecraft/worldgen/placed_feature/cactus_flower.json', {"feature": {"type": "minecraft:no_op", "config": {}}, "placement": []})

# ---- FLORA-09: patch_bush 24 tries
pb = json.load(open(REF + '/src-vb/data/minecraft/worldgen/configured_feature/patch_bush.json'))
assert pb['config']['tries'] == 25
pb['config']['tries'] = 24
write('minecraft/worldgen/configured_feature/patch_bush.json', pb)

# ---- FLORA-12: native vegetal (step 9) order per biome, in 1.21.1 names
RENAME = {'minecraft:trees_birch_and_oak_leaf_litter': 'minecraft:trees_birch_and_oak',
          'minecraft:patch_grass_meadow': 'minecraft:patch_grass_plain'}
rows = []
for fn in sorted(os.listdir(os.path.join(NAT, 'biome'))):
    j = json.load(open(os.path.join(NAT, 'biome', fn)))
    if len(j['features']) < 10:
        continue
    v = j['features'][9]
    v = [v] if isinstance(v, str) else v
    v = [RENAME.get(x, x) for x in v]
    rows.append((fn[:-5], v))
lines = ['package com.coolfreeze23.vbparity.flora.worldgen;', '', 'import java.util.List;', 'import java.util.Map;', '',
         '/** Vegetal-decoration (step 9) feature order of every 26.2 biome, with 26.2-only names mapped to their 1.21.1 names. Generated. */',
         'final class NativeVegetalOrder {', '    static final Map<String, List<String>> ORDER = Map.ofEntries(']
for i, (b, v) in enumerate(rows):
    lines.append(f'        Map.entry("minecraft:{b}", List.of(' + ', '.join(f'"{x}"' for x in v) + '))' + (',' if i < len(rows) - 1 else ''))
lines += ['    );', '', '    private NativeVegetalOrder() {}', '}', '']
os.makedirs(os.path.dirname(JAVA), exist_ok=True)
open(JAVA, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))
print('ok', len(rows), 'biomes')
