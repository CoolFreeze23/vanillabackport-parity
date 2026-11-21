"""Turns add-on tag overrides that use "replace": true into "replace": false + NeoForge "remove", so other mods'
additions to the same tag survive. The resulting tag (vanilla 1.21.1 + VanillaBackport + this file) is unchanged.
Order-sensitive tags, and tags whose entries include tag references, are left alone. Run from the project root: python -I tools/tag_replace_to_remove.py"""
import json, os, zipfile
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md
RES = 'src/main/resources'
VB = REF + '/src-vb'
V121 = zipfile.ZipFile(os.environ.get('MC121_CLIENT_JAR', os.path.join(REF, 'minecraft-1.21.1-client.jar')))
KEEP = {'data/minecraft/tags/enchantment/tooltip_order.json'}   # ordered list: a full replace is the only way to get native order


def entries(data):
    out = []
    for v in data.get('values', []):
        out.append(v if isinstance(v, str) else v['id'])
    return out


for dp, _, fs in os.walk(RES + '/data'):
    for f in fs:
        p = os.path.join(dp, f).replace(os.sep, '/')
        rel = p[len(RES) + 1:]
        if '/tags/' not in rel or not f.endswith('.json') or rel in KEEP:
            continue
        data = json.load(open(p, encoding='utf-8'))
        if data.get('replace') is not True:
            continue
        base = []
        try:
            base += entries(json.loads(V121.read(rel)))
        except KeyError:
            pass
        vbp = os.path.join(VB, rel)
        if os.path.exists(vbp):
            base += entries(json.load(open(vbp, encoding='utf-8')))
        want = data.get('values', [])
        want_ids = set(entries(data))
        remove = sorted(set(b for b in base if b not in want_ids))
        # NeoForge applies "remove" after every addition, and a tag reference removes all of its members. If either
        # side holds a tag reference, a removal could drop an entry this file adds, so keep the full replace there.
        if any(e.startswith('#') for e in remove) or any(e.startswith('#') for e in want_ids):
            print(rel, 'kept as replace (tag references)')
            continue
        new = {'replace': False, 'values': want}
        if remove:
            new['remove'] = remove
        with open(p, 'w', encoding='utf-8', newline='\n') as out:
            json.dump(new, out, indent=2)
            out.write('\n')
        print(rel, 'removes', len(remove))
