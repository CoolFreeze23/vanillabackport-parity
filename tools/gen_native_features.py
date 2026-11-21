"""Writes src/main/resources/vbparity/native_biome_features.json: every biome's per-step placed-feature list from the
native data (26.2, and 26.3-pre-1 for the Wilderness Bound drop). Used at runtime to rebuild the native global feature
order (FeatureSorter) and so the native per-feature decoration seeds."""
import json, os, sys
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

ROOT = sys.argv[1] if len(sys.argv) > 1 else REF
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'vbparity', 'native_biome_features.json')

def lists(base):
    d = os.path.join(ROOT, base, 'data', 'minecraft', 'worldgen', 'biome')
    out = {}
    for fn in sorted(os.listdir(d)):
        j = json.load(open(os.path.join(d, fn), encoding='utf-8'))
        steps = [[s] if isinstance(s, str) else list(s) for s in j['features']]
        for s in steps:
            for x in s:
                assert not x.startswith('#'), (fn, x)
        out['minecraft:' + fn[:-5]] = steps
    return out

data = {'26.2': lists('src-mc'), '26.3': lists('src-mc263')}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(data, f, separators=(',', ':'), sort_keys=True)
    f.write('\n')
print('wrote', OUT, len(data['26.2']), len(data['26.3']))
