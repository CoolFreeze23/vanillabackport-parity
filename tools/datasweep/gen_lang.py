"""Writes assets/vanillabackport/lang/<code>.json in the add-on: the native 26.2 translation (26.3-pre-1 for
Wilderness Bound keys, in every language) of every key VB defines or that names a VB id, wherever the effective value differs or is
missing. Same namespace as VB's own lang files, so the add-on (loaded after VB) wins regardless of namespace order.
Keys already in the add-on file and absent natively are kept.  python gen_lang.py"""
import json
import os
from common import *
from lang import eff_lang, nat_lang, NS_ORDER, VB_KEY_PREFIX, A262
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

OUTDIR = os.path.join(ADDON, 'assets', 'vanillabackport', 'lang')


def vb_keys(code, n2, n3):
    keys = set()
    for root in (VB, ADDON):
        for ns in NS_ORDER:
            p = os.path.join(root, 'assets', ns, 'lang', code + '.json')
            if os.path.exists(p):
                keys |= set(jload(p))
    # VB's en_us keys define what VB content exists; every language should cover them
    keys |= set(LVB)
    # keys the add-on itself adds in English (Wilderness Bound subtitles and names among them)
    keys |= set(all_lang(ADDON))
    keys |= {k for k in list(n2) + list(n3) if k.startswith(VB_KEY_PREFIX)}
    return keys


def main():
    codes = sorted({f[:-5] for f in os.listdir(A262) if f.endswith('.json') and f != 'deprecated.json'} | {'en_us'})
    total = 0
    for code in codes:
        eff, _ = eff_lang(code)
        n2, n3 = nat_lang(code)
        if n2 is None:
            continue
        out_p = os.path.join(OUTDIR, code + '.json')
        cur = jload(out_p) if os.path.exists(out_p) else {}
        add = {}
        for k in sorted(vb_keys(code, n2, n3)):
            nv = n2.get(k, n3.get(k))
            if nv is None:
                continue
            if code != 'en_us' and nv == L262.get(k, L263.get(k)) and k not in eff:
                # native file carries the English text itself: falling back to en_us gives the same string
                continue
            if eff.get(k, (None,))[0] != nv or k in cur:
                add[k] = nv
        merged = dict(cur)
        merged.update(add)
        if not merged:
            continue
        if merged != cur:
            os.makedirs(OUTDIR, exist_ok=True)
            with open(out_p, 'w', encoding='utf-8', newline='\n') as f:
                json.dump(dict(sorted(merged.items())), f, indent=2, ensure_ascii=False)
                f.write('\n')
            total += 1
            print(code, len(add))
    print('files written:', total)


if __name__ == '__main__':
    main()
