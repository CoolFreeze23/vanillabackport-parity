"""Writes src/main/resources/vbparity/loot_parity.json, read by data/LootParity at loot-table load (after VB's
own modifiers): native random_sequence ids, native entry positions for the chest items VB appends, and the native
pools VB lacks.  python gen_loot.py <vbparity-dump.json>"""
import json
import sys
from common import *

SUB = 'data/minecraft/loot_table'
dump = json.load(open(sys.argv[1], encoding='utf-8'))['loot']


def nat(table):
    rel = f'{SUB}/{table}.json'
    _, n = native(rel, prefer263=('minecraft:' + table.split('/')[-1]) in WB_ONLY)
    return n


out = {'random_sequence': {}, 'placements': [], 'insert_pools': [], 'replace_pools': []}

# 1. random_sequence: every table the dump shows without one whose native table has one (OUT-07)
for key, t in sorted(dump.items()):
    if not key.startswith('minecraft:') or 'random_sequence' in t:
        continue
    n = nat(key[10:])
    if n and 'random_sequence' in n:
        out['random_sequence'][key] = n['random_sequence']

# 2. chest entries VB appends at the end of pool 0 (OUT-04, OUT-06)
P = {
    'chests/bastion_treasure': {'minecraft:diamond_spear': None},
    'chests/desert_pyramid': {'minecraft:copper_horse_armor': None},
    'chests/jungle_temple': {'minecraft:copper_horse_armor': None},
    'chests/simple_dungeon': {'minecraft:copper_horse_armor': None},
    'chests/stronghold_corridor': {'minecraft:copper_horse_armor': None},
    'chests/nether_bridge': {'minecraft:copper_horse_armor': None},
    'chests/end_city_treasure': {'minecraft:copper_horse_armor': None, 'minecraft:diamond_spear': None},
    'chests/village/village_weaponsmith': {'minecraft:iron_spear': None, 'minecraft:copper_spear': None,
                                           'minecraft:copper_horse_armor': None},
    # VB gives ruins an iron spear; native has a stone spear (both only with VB's spears on)
    'chests/underwater_ruin_big': {'minecraft:iron_spear': None, 'minecraft:stone_spear': 'minecraft:iron_spear'},
    'chests/underwater_ruin_small': {'minecraft:iron_spear': None, 'minecraft:stone_spear': 'minecraft:iron_spear'},
}
for table, items in P.items():
    pool = nat(table)['pools'][0]
    out['placements'].append({
        'table': 'minecraft:' + table, 'pool': 0,
        'items': sorted(items),
        'present_if': {k: (v or k) for k, v in items.items()},
        'native_order': [e.get('name', '<' + e['type'] + '>') for e in pool['entries']],
        'native_entries': [e for e in pool['entries'] if e.get('name') in items],
    })

# 3. native pools VB lacks (OUT-01, OUT-02) and the native zombie-horse pool (OUT-03), in 1.21.1 syntax
LOOTING = {'count': {'type': 'minecraft:uniform', 'min': 0.0, 'max': 1.0}, 'enchantment': 'minecraft:looting',
           'function': 'minecraft:enchanted_count_increase'}


def rider_pool(item, vehicle):
    return {'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{
        'type': 'minecraft:item', 'name': item,
        'conditions': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                        'predicate': {'vehicle': {'type': vehicle}}}],
        'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0.0, 'max': 1.0},
                       'add': False}, LOOTING]}]}


out['insert_pools'].append({'table': 'minecraft:entities/zombie', 'index': 1,
                            'pool': rider_pool('minecraft:red_mushroom', 'minecraft:zombie_horse')})
out['insert_pools'].append({'table': 'minecraft:entities/husk', 'index': 1,
                            'pool': rider_pool('minecraft:rabbit_foot', 'minecraft:camel_husk')})
out['replace_pools'].append({'table': 'minecraft:entities/zombie_horse', 'index': 0, 'pool': {
    'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{
        'type': 'minecraft:item', 'name': 'minecraft:rotten_flesh',
        'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2.0, 'max': 3.0},
                       'add': False}, LOOTING]}]}})

p = os.path.join(ADDON, 'vbparity', 'loot_parity.json')
os.makedirs(os.path.dirname(p), exist_ok=True)
with open(p, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(out, f, indent=1)
    f.write('\n')
print('random_sequence', len(out['random_sequence']), 'placements', len(out['placements']))
