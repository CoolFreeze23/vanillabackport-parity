"""Normalise 1.21.1 / 26.x data JSON so that format-only differences compare equal."""
import re
import os
REF = os.environ.get('VBP_REF', 'ref')  # root of the reference trees, see tools/README.md

IDRE = re.compile(r'^minecraft:')


def s(x):
    return IDRE.sub('', x) if isinstance(x, str) else x


ENTITY_PRED_KEYS = {'vehicle', 'flags', 'equipment', 'location', 'effects', 'nbt', 'type', 'distance', 'movement',
                    'stepping_on', 'movement_affected_by', 'targeted_entity', 'passenger', 'slots', 'team',
                    'periodic_tick', 'components', 'predicates'}


def norm(x, key=None):
    if isinstance(x, dict):
        out = {}
        for k, v in x.items():
            k2 = s(k)
            # 26.x entity predicate sub-predicates: "minecraft:type_specific/player": {...}
            if k2.startswith('type_specific/'):
                v = dict(v)
                v['type'] = k2.split('/', 1)[1]
                k2 = 'type_specific'
            out[k2] = norm(v, k2)
        # single-element item/entity lists == bare string
        for k in ('items', 'blocks', 'tag', 'fluids', 'entries_id'):
            if k in out and isinstance(out[k], list) and len(out[k]) == 1:
                out[k] = out[k][0]
        # implicit defaults
        if out.get('type') == 'constant' and set(out) == {'type', 'value'}:
            return out['value']
        return out
    if isinstance(x, list):
        return [norm(v, key) for v in x]
    if isinstance(x, float) and x.is_integer():
        return int(x)
    return s(x)


def diff(a, b, path=''):
    """Yield (path, a, b) for every leaf difference."""
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(set(a) | set(b)):
            if k not in a:
                yield (path + '/' + k, '<absent>', b[k])
            elif k not in b:
                yield (path + '/' + k, a[k], '<absent>')
            else:
                yield from diff(a[k], b[k], path + '/' + k)
    elif isinstance(a, list) and isinstance(b, list):
        if len(a) != len(b):
            yield (path, f'<{len(a)} items>', f'<{len(b)} items>')
            # still descend pairwise for context
        for i, (x, y) in enumerate(zip(a, b)):
            yield from diff(x, y, f'{path}[{i}]')
    elif a != b:
        yield (path, a, b)


# ---- 26.3-pre-1 loot format ("modifier"/"condition" objects keyed by "type") -> 1.21.1 ("functions"/"conditions")
def _cond263(c):
    if isinstance(c, list):
        return [_cond263(x) for x in c]
    if isinstance(c, str):  # named predicate reference
        import json
        ns, p = c.split(':', 1)
        with open(f'{REF}/src-mc263/data/{ns}/predicate/{p}.json', encoding='utf-8') as f:
            c = json.load(f)
    c = dict(c)
    t = c.pop('type')
    out = {'condition': t}
    if t in ('minecraft:match_block',):
        out['condition'] = 'minecraft:block_state_property'
        out['block'] = c.pop('blocks')
        if 'state' in c:
            out['properties'] = c.pop('state')
    for k, v in c.items():
        if k in ('term',):
            out[k] = _cond263(v)
        elif k == 'terms':
            out[k] = [_cond263(x) for x in (v if isinstance(v, list) else [v])]
        else:
            out[k] = v
    return out


def _fn263(f):
    f = dict(f)
    out = {'function': f.pop('type')}
    for k, v in f.items():
        if k == 'condition':
            out['conditions'] = _cond263(v) if isinstance(v, list) else [_cond263(v)]
        elif k == 'modifier':  # filtered
            out['modifier'] = _fn263(v) if isinstance(v, dict) else [_fn263(x) for x in v]
        else:
            out[k] = v
    return out


def from263(x):
    if isinstance(x, list):
        return [from263(v) for v in x]
    if not isinstance(x, dict):
        return x
    out = {}
    for k, v in x.items():
        if k == 'modifier':
            out['functions'] = [_fn263(f) for f in (v if isinstance(v, list) else [v])]
        elif k == 'condition':
            out['conditions'] = _cond263(v) if isinstance(v, list) else [_cond263(v)]
        else:
            out[k] = from263(v)
    return out


DEFAULTS = {'bonus_rolls': 0, 'add': False, 'weight': 1, 'quality': 0}


def strip_defaults(x):
    if isinstance(x, list):
        return [strip_defaults(v) for v in x]
    if isinstance(x, dict):
        return {k: strip_defaults(v) for k, v in x.items()
                if not (k in DEFAULTS and v == DEFAULTS[k]) and not (k in ('functions', 'conditions') and v == [])}
    return x


def loot_norm(x, is263=False):
    if is263:
        x = from263(x)
    return strip_defaults(norm(x))
