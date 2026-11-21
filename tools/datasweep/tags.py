"""Tag sweep: resolve every tag in effective (1.21.1 + VB + add-on) and native (26.2; 26.3-pre-1 for Wilderness Bound
ids) and print membership differences that involve a VB-added id.  python tags.py [--json out.json]"""
import json
import os
import sys
from common import *

ROOTS_EFF = EFF_LAYERS


def tag_types(root):
    out = set()
    d = os.path.join(root, 'data')
    if not os.path.isdir(d):
        return out
    for ns in os.listdir(d):
        t = os.path.join(d, ns, 'tags')
        if not os.path.isdir(t):
            continue
        for dp, _, fs in os.walk(t):
            for f in fs:
                if f.endswith('.json'):
                    rel = os.path.relpath(os.path.join(dp, f), t).replace(os.sep, '/')[:-5]
                    # type = dir part; registry types may be nested (worldgen/biome)
                    parts = rel.split('/')
                    if parts[0] == 'worldgen':
                        typ, name = '/'.join(parts[:2]), '/'.join(parts[2:])
                    else:
                        typ, name = parts[0], '/'.join(parts[1:])
                    out.add((typ, ns + ':' + name))
    return out


def tag_path(root, typ, tag):
    ns, name = tag.split(':', 1)
    return os.path.join(root, 'data', ns, 'tags', typ, name + '.json')


def entry_id(e):
    if isinstance(e, dict):
        return e['id'], e.get('required', True)
    return e, True


def build_raw(roots, typ, tag):
    """Merged entry list following TagLoader: replace clears, remove appended after entries."""
    entries = []
    for r in roots:
        p = tag_path(r, typ, tag)
        if not os.path.exists(p):
            continue
        j = jload(p)
        if j.get('replace'):
            entries = []
        for e in j.get('values', []):
            entries.append(('add',) + entry_id(e))
        for e in j.get('remove', []):
            entries.append(('remove', entry_id(e)[0], False))
    return entries


class Resolver:
    def __init__(self, roots, exists):
        self.roots = roots
        self.exists = exists
        self.cache = {}

    def resolve(self, typ, tag, stack=()):
        key = (typ, tag)
        if key in self.cache:
            return self.cache[key]
        if tag in stack:
            return set()
        out = []
        for op, i, req in build_raw(self.roots, typ, tag):
            if i.startswith('#'):
                vals = self.resolve(typ, i[1:], stack + (tag,))
            else:
                vals = [i] if (self.exists is None or self.exists(typ, i)) else []
            for v in vals:
                if op == 'add':
                    if v not in out:
                        out.append(v)
                elif v in out:
                    out.remove(v)
        res = set(out)
        self.cache[key] = res
        return res


DATA_REG_DIRS = {
    'worldgen/biome': 'worldgen/biome', 'damage_type': 'damage_type', 'enchantment': 'enchantment',
    'worldgen/structure': 'worldgen/structure', 'painting_variant': 'painting_variant',
    'banner_pattern': 'banner_pattern', 'instrument': 'instrument', 'jukebox_song': 'jukebox_song',
}


def data_ids(roots, d):
    s = set()
    for r in roots:
        for rel in files_under(r, 'data'):
            parts = rel.split('/')
            # data/<ns>/<d...>/<name>.json
            sub = '/'.join(parts[2:])
            if sub.startswith(d + '/'):
                s.add(parts[1] + ':' + sub[len(d) + 1:-5])
    return s


EFF_DATA = {t: data_ids(EFF_LAYERS, d) for t, d in DATA_REG_DIRS.items()}
V121_DATA = {t: data_ids([V121], d) for t, d in DATA_REG_DIRS.items()}
N262_DATA = {t: data_ids([N262], d) for t, d in DATA_REG_DIRS.items()}
N263_DATA = {t: data_ids([N263], d) for t, d in DATA_REG_DIRS.items()}

OBJ = ('block', 'item', 'entity_type')


def eff_exists(typ, i):
    if typ in OBJ:
        return i in EXISTS
    if typ in EFF_DATA:
        return i in EFF_DATA[typ]
    return True


def vb_added(typ, i):
    if typ in OBJ:
        return i in IDS_VB
    if typ in EFF_DATA:
        return i in EFF_DATA[typ] and i not in V121_DATA[typ]
    return False


def wb_only(typ, i):
    if typ in OBJ:
        return i in WB_ONLY
    if typ in EFF_DATA:
        return i in N263_DATA[typ] and i not in N262_DATA[typ]
    return False



def vb_code_tags():
    """(type, tag) for every TagKey VB registers in core/data/tags/Mod*Tags.java."""
    import re
    out = set()
    d = os.path.join(VB, 'com/blackgear/vanillabackport/core/data/tags')
    typ = {'ModBiomeTags': 'worldgen/biome', 'ModBlockTags': 'block', 'ModDamageTypeTags': 'damage_type',
           'ModEntityTypeTags': 'entity_type', 'ModItemTags': 'item', 'ModStructureTags': 'worldgen/structure'}
    for f, t in typ.items():
        txt = open(os.path.join(d, f + '.java'), encoding='utf-8').read()
        for m in re.findall(r'register\("([a-z0-9_/]+)"\)', txt):
            out.add((t, 'minecraft:' + m))
    return out


RENAMES = {('block', 'minecraft:' + a): 'minecraft:' + b for a, b in [
    ('bamboo_plantable_on', 'supports_bamboo'), ('big_dripleaf_placeable', 'supports_big_dripleaf'),
    ('camel_spawnable_on', 'camels_spawnable_on'), ('concrete_powder', 'concrete_powders'),
    ('dead_bush_may_place_on', 'supports_dry_vegetation'), ('mushroom_grow_block', 'overrides_mushroom_light_requirement'),
    ('small_dripleaf_placeable', 'supports_small_dripleaf'), ('snow_layer_can_survive_on', 'support_override_snow_layer'),
    ('snow_layer_cannot_survive_on', 'cannot_support_snow_layer'), ('creaking_heart_holders', 'pale_oak_logs')]}
RENAMES[('item', 'minecraft:lunge_enchantable')] = 'minecraft:enchantable/lunge'
RENAMES[('item', 'minecraft:enchantable/sword')] = 'minecraft:enchantable/sweeping'
SKIP_NS = ('c:', 'create:', 'sable:', 'neoforge:')


def main():
    eff = Resolver(EFF_LAYERS, None)
    n262 = Resolver([N262], None)
    n263 = Resolver([N263], None)
    consumed = tag_types(V121) | tag_types(VB) | tag_types(ADDON) | vb_code_tags()
    native_tags = tag_types(N262) | tag_types(N263)
    result = []

    def native_set(typ, tag):
        N2 = n262.resolve(typ, tag)
        N3 = n263.resolve(typ, tag)
        if not os.path.exists(tag_path(N262, typ, tag)):
            return N3
        return {i for i in N2 if not wb_only(typ, i)} | {i for i in N3 if wb_only(typ, i)}

    for typ, tag in sorted(consumed):
        if tag.startswith(SKIP_NS):
            continue
        E = {i for i in eff.resolve(typ, tag) if eff_exists(typ, i)}
        cp = RENAMES.get((typ, tag), tag)
        if (typ, cp) not in native_tags:
            # renamed upstream? best match on the 1.21.1 members
            base = {i for i in E if not vb_added(typ, i)}
            best, score = None, 0
            for t2, n2 in native_tags:
                if t2 != typ:
                    continue
                N = {i for i in native_set(typ, n2) if eff_exists(typ, i) and not vb_added(typ, i)}
                if not N and not base:
                    continue
                sc = len(base & N) / max(1, len(base | N))
                if sc > score:
                    best, score = n2, sc
            cp = best if score >= 0.8 else None
        if cp is None:
            if any(vb_added(typ, i) for i in E):
                result.append({'type': typ, 'tag': tag, 'native': None, 'members_vb': sorted(i for i in E if vb_added(typ, i))})
            continue
        N = {i for i in native_set(typ, cp) if eff_exists(typ, i)}
        if not any(vb_added(typ, i) for i in N | E):
            continue
        missing = sorted(N - E)
        extra = sorted(E - N)
        if not missing and not extra:
            continue
        result.append({'type': typ, 'tag': tag, 'native': cp,
                       'missing_vb': [i for i in missing if vb_added(typ, i)],
                       'missing_other': [i for i in missing if not vb_added(typ, i)],
                       'extra_vb': [i for i in extra if vb_added(typ, i)],
                       'extra_other': [i for i in extra if not vb_added(typ, i)]})
    # native-only tags with VB ids that nothing in 1.21.1 + VB reads
    unread = []
    for typ, tag in sorted(native_tags - consumed):
        N = {i for i in native_set(typ, tag) if eff_exists(typ, i) and vb_added(typ, i)}
        if N:
            unread.append((typ, tag, len(N)))
    if '--json' in sys.argv:
        json.dump({'diffs': result, 'unread_native': unread}, open(sys.argv[sys.argv.index('--json') + 1], 'w'), indent=1)
    for r in result:
        if r.get('native') is None:
            print(f"{r['type']} #{r['tag']}  NO NATIVE COUNTERPART  vb members: {' '.join(short(i) for i in r['members_vb'])}")
            continue
        if r['missing_vb'] or r['extra_vb'] or '-a' in sys.argv:
            ren = '' if r['native'] == r['tag'] else f"  (native {r['native']})"
            print(f"{r['type']} #{r['tag']}{ren}")
            for k in ('missing_vb', 'extra_vb', 'missing_other', 'extra_other'):
                if r[k]:
                    print('   ', k, ' '.join(short(i) for i in r[k]))
    print('native-only tags with VB ids (unread here):', len(unread))


if __name__ == '__main__':
    main()
