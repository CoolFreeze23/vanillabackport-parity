"""Loot-table sweep: every loot table that VB or this add-on ships, or that mentions a VB id in effective or native data,
compared after format normalisation.  python loot.py [-v]"""
import json
import sys
from common import *
from norm import norm, diff, loot_norm
import os
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

SUB = 'data/minecraft/loot_table'


def mentions_vb(obj):
    t = json.dumps(obj)
    return any(('"' + i + '"') in t for i in IDS_VB)


N262_FILES = files_under(N262, SUB)


def main():
    rels = set()
    for r in (VB, ADDON):
        rels |= files_under(r, SUB)
    for r in (V121, N262, N263):
        for rel in files_under(r, SUB):
            if mentions_vb(jload(os.path.join(r, rel))):
                rels.add(rel)
    # VB block/entity tables that native has but nobody ships here
    for i in IDS_VB:
        for kind in ('blocks', 'entities'):
            rel = f'{SUB}/{kind}/{short(i)}.json'
            if native(rel)[1] is not None:
                rels.add(rel)
    out = []
    for rel in sorted(rels):
        name = rel[len(SUB) + 1:-5]
        layer, e = effective(rel)
        id_ = 'minecraft:' + name.split('/')[-1]
        nroot, n = native(rel, prefer263=id_ in WB_ONLY)
        if n is None:
            out.append((rel, 'NO NATIVE', layer))
            continue
        if e is None:
            out.append((rel, 'MISSING (native has it)', nroot))
            continue
        d = list(diff(loot_norm(e), loot_norm(n, nroot == N263 and rel not in N262_FILES)))
        if d:
            out.append((rel, d, layer))
    for rel, d, layer in out:
        src = layer.replace(REF + '/', '') if isinstance(layer, str) else layer
        if isinstance(d, str):
            print(f'{rel}: {d} [{src}]')
        else:
            print(f'{rel}: {len(d)} diffs [eff from {src}]')
            for p, a, b in d[:None if '-v' in sys.argv else 6]:
                print(f'    {p}: {json.dumps(a)[:150]}  ->  {json.dumps(b)[:150]}')


if __name__ == '__main__':
    main()
