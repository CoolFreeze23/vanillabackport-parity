"""Native recipes VB lacks (stonecutter paths from raw sulfur/cinnabar) and native recipe-book groups on VB's recipe
files, plus the native unlock advancements of the added recipes.  python gen_recipes.py <vbparity-dump.json>"""
import json, sys
from common import *
from recipes import rnorm
SUB = 'data/minecraft/recipe'
d = json.load(open(sys.argv[1], encoding='utf-8'))['recipes']


def to121(x):
    if isinstance(x, str):
        return {'tag': x[1:]} if x.startswith('#') else {'item': x}
    if isinstance(x, list):
        return [to121(i) for i in x]
    return x


def write(rel, j):
    p = os.path.join(ADDON, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(j, f, indent=2)
        f.write('\n')


added = []
eff_sigs = {json.dumps(rnorm(v), sort_keys=True) for v in d.values()}
for rel in sorted(files_under(N262, SUB)):
    rid = rel[len(SUB) + 1:-5]
    n = jload(os.path.join(N262, rel))
    if n.get('type') != 'minecraft:stonecutting' or ('minecraft:' + rid) in d:
        continue
    if not any(x in rid for x in ('cinnabar', 'sulfur')):
        continue
    sig = dict(rnorm(n)); 
    if json.dumps(rnorm(n), sort_keys=True) in eff_sigs:
        continue
    r = {'type': n['type'], 'ingredient': to121(n['ingredient']), 'result': dict(n['result'])}
    r['result'].setdefault('count', 1)
    write(f'{SUB}/{rid}.json', r)
    added.append(rid)
    for cat in ('building_blocks', 'decorations'):
        arel = f'data/minecraft/advancement/recipes/{cat}/{rid}.json'
        p = os.path.join(N262, arel)
        if os.path.exists(p):
            a = jload(p)
            for c in a['criteria'].values():
                cond = c.get('conditions', {})
                if c.get('trigger') == 'minecraft:recipe_unlocked' and 'recipes' in cond:
                    r_ = cond.pop('recipes'); cond['recipe'] = r_ if isinstance(r_, str) else r_[0]
                if c.get('trigger') == 'minecraft:inventory_changed':
                    for it in cond.get('items', []):
                        if isinstance(it.get('items'), str) and not it['items'].startswith('#'):
                            pass
            write(arel, a)

# native recipe-book groups on VB's file recipes
grouped = []
for key, v in d.items():
    if not key.startswith('minecraft:'):
        continue
    rid = key[10:]
    rel = f'{SUB}/{rid}.json'
    _, n = native(rel, prefer263=True)
    layer, e = effective(rel)
    if n is None or e is None or layer == V121:
        continue
    if n.get('group') and e.get('group') != n.get('group'):
        a, b = dict(rnorm(e)), dict(rnorm(n))
        a.pop('group', None); b.pop('group', None)
        if a == b:
            e = dict(e); e['group'] = n['group']
            write(rel, e)
            grouped.append(rid)
print('added', len(added), added)
print('grouped', len(grouped))
