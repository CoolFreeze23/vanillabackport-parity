"""Lang sweep: every key VB (or this add-on) defines, plus native keys for VB ids that nothing here defines,
compared with native 26.2 (26.3-pre-1 for keys 26.2 lacks, every language).  python lang.py [code ...]"""
import os
import sys
from common import *
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

A262 = REF + '/assets-26.2/files/minecraft/lang'
A263 = REF + '/assets-26.3/lang'  # 26.3-pre-1 language files (asset index, sha1-checked)
NS_ORDER = ['minecraft', 'vanillabackport', 'vbparity', 'vbparity_mayhem', 'vbparity_variants']


def eff_lang(code):
    """key -> (value, namespace, layer) ; later namespace/pack wins in our model, conflicts recorded."""
    out, conflicts = {}, []
    base = jload(os.path.join(V121, 'assets/minecraft/lang/en_us.json')) if code == 'en_us' else {}
    for k, v in base.items():
        out[k] = (v, 'minecraft', 'v121')
    for ns in NS_ORDER:
        for layer, root in (('vb', VB), ('addon', ADDON)):
            p = os.path.join(root, 'assets', ns, 'lang', code + '.json')
            if not os.path.exists(p):
                continue
            for k, v in jload(p).items():
                if k in out and out[k][1] != ns and out[k][0] != v:
                    conflicts.append((k, out[k], (v, ns, layer)))
                out[k] = (v, ns, layer)
    return out, conflicts


def nat_lang(code):
    p = os.path.join(A262, code + '.json') if code != 'en_us' else os.path.join(N262, 'assets/minecraft/lang/en_us.json')
    n2 = jload(p) if os.path.exists(p) else None
    if code == 'en_us':
        n3 = L263
    else:
        p3 = os.path.join(A263, code + '.json')
        n3 = jload(p3) if os.path.exists(p3) else {}
    return n2, n3


VB_KEY_PREFIX = tuple(f'{t}.minecraft.{short(i)}' for i in IDS_VB for t in ('block', 'item', 'entity'))


def run(code):
    eff, conflicts = eff_lang(code)
    n2, n3 = nat_lang(code)
    if n2 is None:
        print(f'[{code}] no native file')
        return
    vbkeys = set()
    for root in (VB, ADDON):
        for ns in NS_ORDER:
            p = os.path.join(root, 'assets', ns, 'lang', code + '.json')
            if os.path.exists(p):
                vbkeys |= set(jload(p))
    vbkeys |= {k for k in list(n2) + list(n3) if k.startswith(VB_KEY_PREFIX)}
    bad, missing, extra = [], [], []
    for k in sorted(vbkeys):
        nv = n2.get(k, n3.get(k))
        if nv is None:
            if k in eff and eff[k][2] != 'v121':
                extra.append(k)
            continue
        if k not in eff:
            if code == 'en_us' or nv != L262.get(k, L263.get(k)):
                missing.append((k, nv))
        elif eff[k][0] != nv:
            bad.append((k, eff[k], nv))
    print(f'[{code}] keys {len(vbkeys)}  differ {len(bad)}  missing {len(missing)}  not-native {len(extra)}  ns-conflicts {len(conflicts)}')
    if '-v' in sys.argv or code == 'en_us':
        for k, e, nv in bad:
            print(f'   DIFF {k}: {e[0]!r} [{e[1]}/{e[2]}] -> {nv!r}')
        for k, nv in missing:
            print(f'   MISSING {k}: {nv!r}')
        for k in extra:
            print(f'   NOT NATIVE {k}: {eff[k][0]!r}')
        for k, a, b in conflicts:
            print(f'   CONFLICT {k}: {a} vs {b}')
    return bad, missing


if __name__ == '__main__':
    codes = [c for c in sys.argv[1:] if not c.startswith('-')] or ['en_us']
    if codes == ['all']:
        codes = sorted({f[:-5] for root in (VB, ADDON) for ns in NS_ORDER
                        for f in (os.listdir(os.path.join(root, 'assets', ns, 'lang'))
                                  if os.path.isdir(os.path.join(root, 'assets', ns, 'lang')) else [])})
    for c in codes:
        run(c)
