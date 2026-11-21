"""Block property / item component sweep: effective dump (DataDump GameTest) vs native dumps (tools/nativedump on 26.2
and 26.3-pre-1). python props.py <vbparity-dump.json> <native-262.json> <native-263.json> [--json out]"""
import json
import sys
from common import *

eff = json.load(open(sys.argv[1], encoding='utf-8'))
n2 = json.load(open(sys.argv[2], encoding='utf-8'))
n3 = json.load(open(sys.argv[3], encoding='utf-8'))

PUSH263 = {'PUSH_PULL': 'NORMAL', 'POPPED': 'DESTROY'}
BLOCK_KEYS = ['destroy_time', 'explosion_resistance', 'friction', 'speed_factor', 'jump_factor', 'has_collision',
              'ignite_odds', 'burn_odds', 'map_color', 'light', 'destroy_speed', 'push_reaction', 'requires_correct_tool',
              'sound', 'ignited_by_lava', 'replaceable', 'random_ticks', 'instrument', 'solid', 'liquid',
              'blocks_motion', 'can_occlude', 'offset', 'large_collision']
ITEM_KEYS = ['max_stack_size', 'max_damage', 'rarity', 'food', 'damage_resistant', 'attribute_modifiers', 'jukebox_playable',
             'consumable', 'use_remainder', 'container', 'bundle_contents', 'tool', 'kinetic_weapon', 'piercing_weapon',
             'attack_range', 'minimum_attack_range', 'damage_type', 'provides_trim_material', 'provides_banner_patterns']
_ALL_ITEM_KEYS = ['max_stack_size', 'max_damage', 'rarity', 'food', 'fire_resistant', 'damage_resistant', 'jukebox_playable',
             'enchantable', 'repairable', 'tool', 'attribute_modifiers', 'equippable', 'consumable', 'use_remainder',
             'use_cooldown', 'weapon', 'item_name', 'lore', 'glider', 'blocks_attacks', 'tooltip_display', 'death_protection',
             'enchantment_glint_override', 'bundle_contents', 'container', 'kinetic_weapon', 'piercing_weapon',
             'attack_range', 'swing_animation', 'minimum_attack_range', 'damage_type', 'use_effects', 'provides_trim_material',
             'provides_banner_patterns', 'break_sound', 'potion_duration_scale']


def nat_for(i):
    return n3 if i in WB_ONLY else n2


def norm_state_map(v):
    """{state: value} keys differ in property order/naming across versions; compare as a sorted value list when keys mismatch."""
    return v


def comp(d):
    c = d.get('components', {})
    return {k.replace('minecraft:', ''): v for k, v in c.items()}


def cmpv(a, b):
    if isinstance(a, float) or isinstance(b, float):
        try:
            return abs(float(a) - float(b)) < 1e-6
        except (TypeError, ValueError):
            return False
    return a == b


def state_cmp(a, b):
    if isinstance(a, dict) != isinstance(b, dict):
        return False
    if isinstance(a, dict):
        if set(a) == set(b):
            return all(state_cmp(a[k], b[k]) for k in a)
        return sorted(json.dumps(x, sort_keys=True) for x in a.values()) == sorted(json.dumps(x, sort_keys=True) for x in b.values())
    if isinstance(a, (int, float)) and isinstance(b, (int, float)):
        return cmpv(a, b)
    return a == b


out = {'blocks': {}, 'items': {}}
for i in sorted(IDS_VB):
    N = nat_for(i)
    if i in eff['blocks'] and i in N['blocks']:
        e, n = eff['blocks'][i], dict(N['blocks'][i])
        if isinstance(n.get('push_reaction'), str):
            n['push_reaction'] = PUSH263.get(n['push_reaction'], n['push_reaction'])
        d = {k: (e.get(k), n.get(k)) for k in BLOCK_KEYS if n.get(k) != 'n/a' and not state_cmp(e.get(k), n.get(k))}
        if d:
            out['blocks'][i] = d
    if i in eff['items'] and i in N['items']:
        e, n = comp(eff['items'][i]), comp(N['items'][i])
        if 'fire_resistant' in e:
            e['damage_resistant'] = {'types': '#minecraft:is_fire'}
            del e['fire_resistant']
        am = e.get('attribute_modifiers')
        if isinstance(am, dict):
            am = am.get('modifiers', [])
            for m in am:
                m['type'] = m['type'].replace('generic.', '').replace('player.', '')
            e['attribute_modifiers'] = am
        for x in (e, n):
            if x.get('attribute_modifiers') == []:
                del x['attribute_modifiers']
        if i not in WB_ONLY or True:
            pass
        e.setdefault('max_stack_size', 64)
        n.setdefault('max_stack_size', 64)
        d = {}
        for k in ITEM_KEYS:
            if k in ('item_name', 'lore', 'tooltip_display', 'break_sound'):
                continue
            if json.dumps(e.get(k), sort_keys=True) != json.dumps(n.get(k), sort_keys=True):
                d[k] = (e.get(k), n.get(k))
        if d:
            out['items'][i] = d

if '--json' in sys.argv:
    json.dump(out, open(sys.argv[sys.argv.index('--json') + 1], 'w'), indent=1)
from collections import Counter
print('blocks with diffs', len(out['blocks']), Counter(k for v in out['blocks'].values() for k in v))
print('items with diffs', len(out['items']), Counter(k for v in out['items'].values() for k in v))
