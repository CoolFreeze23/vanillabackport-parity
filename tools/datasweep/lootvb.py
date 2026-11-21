import json, sys
from common import *
from norm import loot_norm
SUB='data/minecraft/loot_table'
d=json.load(open(sys.argv[1],encoding='utf-8'))['loot']
def strip(x):
    if isinstance(x, dict): return {k: strip(v) for k, v in x.items() if not (k == 'name' and 'rolls' in x)}
    if isinstance(x, list): return [strip(v) for v in x]
    return x
def items_of(pool): return [e.get('name') for e in pool.get('entries',[])]
for rel in sorted(files_under(V121,SUB)):
    name=rel[len(SUB)+1:-5]
    v=loot_norm(jload(os.path.join(V121,rel)))
    e=loot_norm(strip(d.get('minecraft:'+name,{})))
    _,n=native(rel)
    if n is None: continue
    n=loot_norm(n)
    vbin=lambda t: any(('"'+i[10:]+'"') in json.dumps(t) for i in IDS_VB)
    if e==v and not vbin(n): continue
    if json.dumps(e)==json.dumps(n): continue
    print('==',name, 'pools eff',len(e.get('pools',[])),'nat',len(n.get('pools',[])),'v121',len(v.get('pools',[])))
    for pi,p in enumerate(n.get('pools',[])):
        print('  N',pi,items_of(p))
    for pi,p in enumerate(e.get('pools',[])):
        print('  E',pi,items_of(p))
