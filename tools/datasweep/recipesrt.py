"""Recipe sweep on the runtime dump (VB's code-generated recipes included)."""
import json, sys
from common import *
from norm import diff
from recipes import rnorm, touches_vb, SUB
d = json.load(open(sys.argv[1], encoding='utf-8'))['recipes']
eff = {}
for k, v in d.items():
    ns, path = k.split(':', 1)
    eff.setdefault(path, []).append((k, v))
nat = {}
for root in (N262, N263):
    for rel in files_under(root, SUB):
        p = rel[len(SUB) + 1:-5]
        if p not in nat:
            nat[p] = jload(os.path.join(root, rel))
keys = {p for p, lst in eff.items() if any(touches_vb(v) for _, v in lst)} | {p for p, v in nat.items() if touches_vb(v)}
for p in sorted(keys):
    n = nat.get(p)
    lst = eff.get(p, [])
    if n is None:
        print(f'{p}: NOT NATIVE ({", ".join(k for k, _ in lst)})'); continue
    if not lst:
        print(f'{p}: MISSING'); continue
    for k, v in lst:
        dd = list(diff(rnorm(v), rnorm(n)))
        if dd:
            print(f'{p} [{k}]:')
            for a in dd: print(f'    {a[0]}: {json.dumps(a[1])[:140]} -> {json.dumps(a[2])[:140]}')
    if len(lst) > 1: print(f'{p}: DUPLICATE ids {[k for k, _ in lst]}')
