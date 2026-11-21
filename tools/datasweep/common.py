"""Shared loaders for the data sweep: effective = vanilla 1.21.1 + VanillaBackport 1.2.2 + this add-on;
native = 26.2 (26.3-pre-1 for ids 26.2 lacks)."""
import json
import os
import re
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

SCR = os.environ.get('V121_DIR', os.path.join(os.environ.get('VBP_REF', 'ref'), 'mc-1.21.1-data'))
V121 = SCR
VB = REF + '/src-vb'
ADDON = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources')
N262 = REF + '/src-mc'
N263 = REF + '/src-mc263'
EFF_LAYERS = [V121, VB, ADDON]


def jload(p):
    with open(p, encoding='utf-8-sig') as f:
        return json.load(f)


def lang(root, ns='minecraft', code='en_us'):
    p = os.path.join(root, 'assets', ns, 'lang', code + '.json')
    return jload(p) if os.path.exists(p) else {}


def all_lang(root, code='en_us'):
    out = {}
    a = os.path.join(root, 'assets')
    if not os.path.isdir(a):
        return out
    for ns in sorted(os.listdir(a)):
        out.update(lang(root, ns, code))
    return out


L121 = lang(V121)
LVB = all_lang(VB)
L262 = lang(N262)
L263 = lang(N263)

KEYRE = re.compile(r'^(block|item|entity)\.minecraft\.([a-z0-9_]+)$')


def ids_from_lang(l):
    s = set()
    for k in l:
        m = KEYRE.match(k)
        if m:
            s.add('minecraft:' + m.group(2))
    return s


IDS_121 = ids_from_lang(L121)
IDS_VB_LANG = ids_from_lang(LVB) - IDS_121
IDS_262 = ids_from_lang(L262)
IDS_263 = ids_from_lang(L263)


def _registered_vb_ids():
    """Every id VB registers under minecraft: (blocks, items, entities), read from its registry classes."""
    ids = set()
    root = os.path.join(VB, 'com/blackgear/vanillabackport/common/registries')
    pat = re.compile(r'\(\s*"([a-z0-9_]+)"')
    for f in ('blocks/ModBlocks.java', 'items/ModItems.java', 'entities/ModEntityTypes.java'):
        txt = open(os.path.join(root, f), encoding='utf-8').read()
        ids.update('minecraft:' + m for m in pat.findall(txt) if m not in ('minecraft', 'copper', 'pale_oak', 'poplar'))
    return ids


IDS_VB = (IDS_VB_LANG | (_registered_vb_ids() - IDS_121))
EXISTS = IDS_121 | IDS_VB
WB_ONLY = (IDS_263 - IDS_262)  # Wilderness Bound: compare to 26.3-pre-1


def native_root_for(id_):
    return N263 if id_ in WB_ONLY else N262


def layered(rel):
    """[(layer, json)] for every effective layer that has rel."""
    out = []
    for r in EFF_LAYERS:
        p = os.path.join(r, rel)
        if os.path.exists(p):
            out.append((r, jload(p)))
    return out


def effective(rel):
    """Top-most effective file (data files other than tags are replaced whole by later packs)."""
    lay = layered(rel)
    return lay[-1] if lay else (None, None)


def native(rel, prefer263=False):
    for r in ((N263, N262) if prefer263 else (N262, N263)):
        p = os.path.join(r, rel)
        if os.path.exists(p):
            return r, jload(p)
    return None, None


def files_under(root, sub):
    base = os.path.join(root, sub)
    out = set()
    for dp, _, fs in os.walk(base):
        for f in fs:
            if f.endswith('.json'):
                out.add(os.path.relpath(os.path.join(dp, f), root).replace(os.sep, '/'))
    return out


def short(i):
    return i[10:] if i.startswith('minecraft:') else i
