"""Recipe sweep: every recipe (effective or native) that produces or consumes a VB item, by recipe id.
python recipes.py"""
import json
import sys
from common import *
from norm import diff
import os
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

SUB = 'data/minecraft/recipe'


def ing(x):
    if isinstance(x, list):
        v = sorted(ing(i) for i in x)
        return v[0] if len(v) == 1 else v
    if isinstance(x, dict):
        if 'item' in x:
            return x['item']
        if 'tag' in x:
            return '#' + x['tag']
        return x
    return x


def rnorm(r):
    r = json.loads(json.dumps(r))
    t = r.get('type')
    out = {'type': t}
    for k, v in r.items():
        if k in ('type', 'neoforge:conditions'):
            continue
        if k == 'key':
            out[k] = {kk: ing(vv) for kk, vv in v.items()}
        elif k in ('ingredients',):
            out[k] = sorted(json.dumps(ing(i)) for i in v)
        elif k in ('ingredient', 'base', 'addition', 'template', 'input', 'material'):
            out[k] = ing(v)
        elif k == 'result':
            if isinstance(v, str):
                v = {'id': v}
            v = dict(v)
            if 'item' in v:
                v['id'] = v.pop('item')
            v.setdefault('count', 1)
            out[k] = v
        elif k == 'show_notification' and v is True:
            continue
        elif k in ('experience', 'cookingtime') and isinstance(v, float) and v.is_integer():
            out[k] = int(v)
        else:
            out[k] = v
    if t not in ('minecraft:smithing_transform', 'minecraft:smithing_trim', 'minecraft:stonecutting'):
        out.setdefault('category', 'misc')
    if t in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking'):
        out.setdefault('experience', 0)
        out.setdefault('cookingtime', {'minecraft:smelting': 200, 'minecraft:blasting': 100,
                                       'minecraft:smoking': 100, 'minecraft:campfire_cooking': 100}[t])
    return out


def touches_vb(r):
    t = json.dumps(r)
    return any(('"' + i + '"') in t or ('"#' + i) in t for i in IDS_VB)


def main():
    rels = set()
    for root in (V121, VB, ADDON, N262, N263):
        for rel in files_under(root, SUB):
            j = jload(os.path.join(root, rel))
            if root in (VB, ADDON) or touches_vb(j):
                rels.add(rel)
    rows = []
    for rel in sorted(rels):
        layer, e = effective(rel)
        if e is not None and e.get('neoforge:conditions') == [{'type': 'neoforge:false'}]:
            e = None
        nroot, n = native(rel)
        rid = rel[len(SUB) + 1:-5]
        if n is None and e is None:
            continue
        if n is None:
            rows.append((rid, 'NOT NATIVE', layer))
            continue
        if e is None:
            rows.append((rid, 'MISSING', nroot))
            continue
        d = list(diff(rnorm(e), rnorm(n)))
        if d:
            rows.append((rid, d, layer))
    for rid, d, layer in rows:
        src = str(layer).replace(REF + '/', '')
        if isinstance(d, str):
            print(f'{rid}: {d} [{src}]')
        else:
            print(f'{rid}: [{src}]')
            for p, a, b in d:
                print(f'    {p}: {json.dumps(a)[:160]} -> {json.dumps(b)[:160]}')


if __name__ == '__main__':
    main()
