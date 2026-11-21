"""Loot sweep on the runtime dump (VB's code modifiers included)."""
import json, sys
from common import *
from norm import diff, loot_norm
from loot import SUB, mentions_vb, N262_FILES
DUMP = sys.argv[1]
d = json.load(open(DUMP, encoding='utf-8'))['loot']

def strip(x):
    if isinstance(x, dict):
        return {k: strip(v) for k, v in x.items() if not (k == 'name' and 'rolls' in x)}
    if isinstance(x, list):
        return [strip(v) for v in x]
    return x

rels = set()
for r in (VB, ADDON):
    rels |= {x for x in files_under(r, SUB)}
for r in (N262, N263):
    for rel in files_under(r, SUB):
        if mentions_vb(jload(os.path.join(r, rel))): rels.add(rel)
for k, v in d.items():
    if mentions_vb(v): rels.add(f'{SUB}/{k.split(":")[1]}.json')
for i in IDS_VB:
    for kind in ('blocks', 'entities'):
        rel = f'{SUB}/{kind}/{short(i)}.json'
        if native(rel)[1] is not None: rels.add(rel)
for rel in sorted(rels):
    name = rel[len(SUB) + 1:-5]
    e = d.get('minecraft:' + name)
    id_ = 'minecraft:' + name.split('/')[-1]
    nroot, n = native(rel, prefer263=id_ in WB_ONLY)
    if n is None: print(f'{name}: NO NATIVE'); continue
    if e is None: print(f'{name}: MISSING'); continue
    dd = list(diff(loot_norm(strip(e)), loot_norm(n, nroot == N263 and rel not in N262_FILES)))
    if dd:
        print(f'{name}: {len(dd)} diffs')
        for p, a, b in dd[:None if '-v' in sys.argv else 8]:
            print(f'    {p}: {json.dumps(a)[:170]}  ->  {json.dumps(b)[:170]}')
