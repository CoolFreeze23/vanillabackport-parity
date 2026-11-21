"""Enchantment applicability for VB items: resolve supported_items / primary_items of every enchantment."""
import os
from common import *
from tags import Resolver, eff_exists
eff = Resolver(EFF_LAYERS, None); n2 = Resolver([N262], None); n3 = Resolver([N263], None)

def items(res, v):
    if v is None: return set()
    if isinstance(v, str): v = [v]
    out = set()
    for x in v:
        out |= res.resolve('item', x[1:]) if x.startswith('#') else {x}
    return out

names = set(os.path.basename(p)[:-5] for r in EFF_LAYERS for p in files_under(r, 'data/minecraft/enchantment'))
for n in sorted(names):
    rel = f'data/minecraft/enchantment/{n}.json'
    _, e = effective(rel); _, nat = native(rel)
    if nat is None: print(n, 'NO NATIVE'); continue
    for k in ('supported_items', 'primary_items'):
        E = {i for i in items(eff, e.get(k, e.get('supported_items') if k == 'primary_items' else None)) if i in IDS_VB}
        N = {i for i in items(n2, nat.get(k, nat.get('supported_items') if k == 'primary_items' else None)) if i in IDS_VB}
        if E != N:
            print(n, k, 'extra:', sorted(short(i) for i in E - N), 'missing:', sorted(short(i) for i in N - E))
    for k in ('weight', 'max_level', 'anvil_cost', 'min_cost', 'max_cost', 'slots', 'exclusive_set', 'effects'):
        if e.get(k) != nat.get(k) and k != 'effects':
            print(n, k, e.get(k), '->', nat.get(k))
    if e.get('effects') != nat.get('effects'):
        print(n, 'effects differ (keys eff', sorted(e.get('effects', {})), 'nat', sorted(nat.get('effects', {})), ')')
