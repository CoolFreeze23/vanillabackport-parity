"""Builds the README charts (light + dark SVG) from docs/data/parity-values.csv and parity-summary.csv.

Run from the project root:  python -I tools/charts/make_charts.py
Output: docs/charts/*.svg
"""
import csv
import math
import os
from html import escape

DATA = 'docs/data'
OUT = 'docs/charts'
FONT = "-apple-system, 'Segoe UI', Helvetica, Arial, sans-serif"

THEMES = {
    'light': dict(surface='#ffffff', text='#0b0b0b', text2='#52514e', muted='#8a8984', grid='#e6e5e1', axis='#c9c8c3',
                  vb='#eb6834', addon='#2a78d6', native='#0b0b0b',
                  good='#0ca30c', warning='#fab219', serious='#ec835a', critical='#d03b3b', wrong='#b9b8b2'),
    'dark': dict(surface='#0d1117', text='#f0efec', text2='#c3c2b7', muted='#8f8e86', grid='#2a2a27', axis='#44443f',
                 vb='#d95926', addon='#3987e5', native='#f0efec',
                 good='#0ca30c', warning='#fab219', serious='#ec835a', critical='#d03b3b', wrong='#5d5c57'),
}


def rows(name):
    with open(os.path.join(DATA, name), encoding='utf-8') as f:
        return list(csv.DictReader(f))


def svg(w, h, body, t):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" '
            f'font-family="{FONT}" role="img">\n<rect width="{w}" height="{h}" rx="8" fill="{t["surface"]}"/>\n'
            + body + '</svg>\n')


def text(x, y, s, t, size=13, color=None, anchor='start', weight='normal'):
    return (f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{color or t["text"]}" text-anchor="{anchor}" '
            f'font-weight="{weight}">{escape(s)}</text>\n')


def legend(items, x, y, t):
    out = ''
    for label, kind, color in items:
        if kind == 'square':
            out += f'<rect x="{x}" y="{y - 9}" width="11" height="11" rx="2" fill="{color}"/>\n'
        elif kind == 'dot':
            out += f'<circle cx="{x + 5.5}" cy="{y - 3.5}" r="5" fill="{color}"/>\n'
        elif kind == 'ring':
            out += f'<line x1="{x + 5.5}" y1="{y - 11}" x2="{x + 5.5}" y2="{y + 4}" stroke="{color}" stroke-width="2"/>\n'
        out += text(x + 17, y, label, t, 12, t['text2'])
        x += 17 + 7.2 * len(label) + 22
    return out


# 1. Status per area -----------------------------------------------------------------------------------------------
AREA_NAMES = {
    'garden': 'The Garden Awakens', 'ghast': 'Chase the Skies', 'copper': 'The Copper Age',
    'mayhem': 'Mounts of Mayhem', 'variants': 'Mob & sound variants', 'sulfur': 'Chaos Cubed',
    'flora': 'Spring to Life flora', 'wild': 'Wilderness Bound', 'misc': 'Bundles, music, boats, misc',
    'tech-spawning': 'Technical: spawning', 'tech-ai': 'Technical: mob AI', 'tech-physics': 'Technical: physics & combat',
    'tech-outputs': 'Technical: farm outputs', 'tech-blocks': 'Technical: blocks & redstone',
}
STATUS = [('fixed', 'Fixed', 'good'), ('partial', 'Partly fixed', 'warning'), ('engine_limit', '1.21.1 engine limit', 'serious'),
          ('not_fixed', 'Not fixed', 'critical'), ('wrong', 'Audit was wrong', 'wrong')]


def chart_status(t):
    data = rows('parity-summary.csv')
    left, top, barw, rowh = 230, 74, 520, 30
    maxn = max(int(r['findings']) for r in data)
    h = top + rowh * len(data) + 30
    w = left + barw + 70
    body = text(24, 32, 'What happened to every finding', t, 17, weight='600')
    body += legend([(lab, 'square', t[c]) for _, lab, c in STATUS], 24, 56, t)
    for i, r in enumerate(data):
        y = top + i * rowh
        body += text(left - 12, y + 15, AREA_NAMES.get(r['area'], r['area']), t, 13, t['text2'], 'end')
        x = left
        for key, _, c in STATUS:
            n = int(r[key])
            if not n:
                continue
            seg = barw * n / maxn
            body += (f'<rect x="{x:.1f}" y="{y + 3}" width="{max(seg - 2, 1):.1f}" height="18" rx="3" fill="{t[c]}">'
                     f'<title>{escape(AREA_NAMES.get(r["area"], r["area"]))}: {n} {key.replace("_", " ")}</title></rect>\n')
            x += seg
        body += text(x + 6, y + 16, f'{r["fixed"]}/{r["findings"]}', t, 12, t['text2'])
    total = sum(int(r['findings']) for r in data)
    fixed = sum(int(r['fixed']) for r in data)
    body += text(24, h - 10, f'{fixed} of {total} findings fixed. Numbers are fixed / total findings per area.', t, 11, t['muted'])
    return svg(w, h, body, t)


# 2. How far off VB was: dumbbells on a log ratio axis, native = 1x --------------------------------------------------
def fmt(v, unit):
    v = float(v)
    s = f'{v:.0f}' if abs(v - round(v)) < 1e-9 and abs(v) >= 1 else f'{v:.3g}'
    return s + ('%' if unit == '%' else ('' if unit in ('count', 'x', 'weight', 'signal') else ' ' + unit))


def chart_ratios(t, per_group=5):
    data = [r for r in rows('parity-values.csv') if float(r['vb']) > 0 and float(r['native']) > 0 and float(r['addon']) > 0]
    groups = {}
    for r in data:
        r['_vb'] = float(r['vb']) / float(r['native'])
        r['_ad'] = float(r['addon']) / float(r['native'])
        groups.setdefault(r['group'], []).append(r)
    order = ['Spawning', 'Mob AI', 'Combat', 'Drops & outputs', 'Blocks & redstone', 'Worldgen', 'Entity size', 'Sounds', 'Misc']
    picked = []
    for g in order:
        rs = sorted(groups.get(g, []), key=lambda r: -abs(math.log(r['_vb'])))
        seen = set()
        out = []
        for r in rs:
            key = r['source']
            if key in seen or abs(math.log(r['_vb'])) < 1e-9:
                continue
            seen.add(key)
            out.append(r)
            if len(out) == per_group:
                break
        if out:
            picked.append((g, out))
    lo, hi = 1 / 64, 64
    left, plotw, right = 430, 360, 150
    rowh, top = 24, 92
    nrows = sum(len(rs) for _, rs in picked) + len(picked)
    h = top + nrows * rowh + 40
    w = left + plotw + right

    def X(ratio):
        ratio = min(max(ratio, lo), hi)
        return left + plotw * (math.log(ratio) - math.log(lo)) / (math.log(hi) - math.log(lo))

    body = text(24, 32, 'How far VanillaBackport was from native, and where the add-on lands', t, 17, weight='600')
    body += legend([('VanillaBackport 1.2.2', 'dot', t['vb']), ('Native 26.2 / 26.3', 'ring', t['native']),
                    ('With the add-on', 'dot', t['addon'])], 24, 56, t)
    ticks = [(1 / 64, '1/64×'), (1 / 8, '1/8×'), (1, 'native'), (8, '8×'), (64, '64×')]
    for v, lab in ticks:
        x = X(v)
        body += f'<line x1="{x:.1f}" y1="{top - 10}" x2="{x:.1f}" y2="{h - 34}" stroke="{t["grid"]}" stroke-width="1"/>\n'
        body += text(x, top - 16, lab, t, 11, t['muted'], 'middle')
    x1 = X(1)
    body += f'<line x1="{x1:.1f}" y1="{top - 10}" x2="{x1:.1f}" y2="{h - 34}" stroke="{t["native"]}" stroke-width="2"/>\n'
    y = top
    for g, rs in picked:
        body += text(24, y + 15, g, t, 13, t['text'], weight='600')
        y += rowh
        for r in rs:
            label = r['metric'] if len(r['metric']) <= 58 else r['metric'][:56] + '…'
            body += text(left - 12, y + 14, label, t, 12, t['text2'], 'end')
            xv, xa = X(r['_vb']), X(r['_ad'])
            body += f'<line x1="{xv:.1f}" y1="{y + 10}" x2="{xa:.1f}" y2="{y + 10}" stroke="{t["axis"]}" stroke-width="2"/>\n'
            tip = (f'{r["metric"]}: VanillaBackport {fmt(r["vb"], r["unit"])}, native {fmt(r["native"], r["unit"])}, '
                   f'add-on {fmt(r["addon"], r["unit"])} ({r["source"]})')
            body += (f'<circle cx="{xv:.1f}" cy="{y + 10}" r="5.5" fill="{t["vb"]}" stroke="{t["surface"]}" stroke-width="2">'
                     f'<title>{escape(tip)}</title></circle>\n')
            body += (f'<circle cx="{xa:.1f}" cy="{y + 10}" r="5.5" fill="{t["addon"]}" stroke="{t["surface"]}" stroke-width="2">'
                     f'<title>{escape(tip)}</title></circle>\n')
            body += text(left + plotw + 12, y + 14, f'{fmt(r["vb"], r["unit"])} → {fmt(r["native"], r["unit"])}', t, 11, t['text2'])
            y += rowh
    body += text(24, h - 12, 'Log scale: each dot is the value divided by native. Right column: VanillaBackport value → native value. '
                 'Top differences per group; full table in docs/data/parity-values.csv.', t, 11, t['muted'])
    return svg(w, h, body, t)


# 3. Concrete examples in their own units: small multiples of grouped bars -------------------------------------------
def panel(title, unit, items, t, x0, y0, pw, ph):
    """items: list of (label, vb, native, addon)."""
    body = text(x0, y0 + 14, title, t, 13, t['text'], weight='600')
    body += text(x0, y0 + 30, unit, t, 11, t['muted'])
    top, bottom = y0 + 46, y0 + ph - 28
    raw = max(max(a, b, c) for _, a, b, c in items) or 1
    step = 10 ** math.floor(math.log10(raw / 2))
    vmax = next(m * step * 2 for m in (1, 1.25, 2.5, 5, 10, 20) if m * step * 2 >= raw)   # nice top: half lands on a round tick
    n = len(items)
    gw = pw / n
    bw = min(14, (gw - 14) / 3)
    for frac in (0, 0.5, 1):
        yy = bottom - (bottom - top) * frac
        body += f'<line x1="{x0}" y1="{yy:.1f}" x2="{x0 + pw}" y2="{yy:.1f}" stroke="{t["grid"]}" stroke-width="1"/>\n'
        body += text(x0 - 6, yy + 4, f'{vmax * frac:g}', t, 10, t['muted'], 'end')
    for i, (lab, a, b, c) in enumerate(items):
        gx = x0 + i * gw + (gw - 3 * bw - 4) / 2
        for j, (v, key, who) in enumerate(((a, 'vb', 'VanillaBackport'), (b, 'native', 'Native'), (c, 'addon', 'Add-on'))):
            hh = (bottom - top) * v / vmax
            col = t['native'] if key == 'native' else t[key]
            fill = col if key != 'native' else 'none'
            stroke = f' stroke="{col}" stroke-width="1.5"' if key == 'native' else ''
            bx = gx + j * (bw + 2)
            body += (f'<path d="M{bx:.1f},{bottom} v{-max(hh - 3, 0):.1f} q0,-3 3,-3 h{bw - 6:.1f} q3,0 3,3 v{max(hh - 3, 0):.1f} z" '
                     f'fill="{fill}"{stroke}><title>{escape(lab)}: {who} {v:g}</title></path>\n')
        body += text(x0 + i * gw + gw / 2, bottom + 15, lab, t, 11, t['text2'], 'middle')
    return body


EXAMPLES = [
    ('Spear dismount threshold', 'minimum speed', [
        ('stone', 'Spear dismount min speed, stone'), ('copper', 'Spear dismount min speed, copper'),
        ('iron', 'Spear dismount min speed, iron'), ('gold', 'Spear dismount min speed, golden'),
        ('diamond', 'Spear dismount min speed, diamond'), ('neth.', 'Spear dismount min speed, netherite')]),
    ('Ghastling (baby happy ghast) AI', 'blocks', [
        ('path length', 'Ghastling max path length'), ('tempt range', 'Ghastling tempt range'),
        ('stop dist.', 'Ghastling stop distance from a tempting player'),
        ('follow dist.', 'Ghastling adult-follow minimum distance')]),
    ('Desert spawn shares', '% of spawn picks', [
        ('skeleton', 'Desert monster packs: skeleton share'), ('parched', 'Desert monster packs: parched share'),
        ('husk', 'Desert monster packs: husk share'), ('camel', 'Desert camel share of creature packs')]),
    ('Furnace fuel', 'ticks', [
        ('cushion', 'Cushion fuel time'), ('wool slab', 'Wool slab fuel time'),
        ('wooden spear', 'Wooden spear furnace burn time'), ('poplar shelf', 'Poplar shelf fuel time')]),
]


def chart_examples(t):
    by_metric = {}
    for r in rows('parity-values.csv'):
        by_metric.setdefault(r['metric'], r)
    panels = []
    for title, unit, spec in EXAMPLES:
        items = [(lab, float(by_metric[m]['vb']), float(by_metric[m]['native']), float(by_metric[m]['addon']))
                 for lab, m in spec if m in by_metric]
        if items:
            panels.append((title, unit, items))
    pw, ph, gap = 420, 250, 70
    cols = 2
    w = 40 + cols * pw + (cols - 1) * gap + 30
    h = 80 + ((len(panels) + 1) // 2) * (ph + 30) + 10
    body = text(24, 32, 'Examples in their own units', t, 17, weight='600')
    body += legend([('VanillaBackport 1.2.2', 'square', t['vb']), ('Native', 'ring', t['native']),
                    ('With the add-on', 'square', t['addon'])], 24, 56, t)
    for i, (title, unit, items) in enumerate(panels):
        x0 = 60 + (i % cols) * (pw + gap)
        y0 = 74 + (i // cols) * (ph + 30)
        body += panel(title, unit, items, t, x0, y0, pw, ph)
    return svg(w, h, body, t)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (('status', chart_status), ('ratios', chart_ratios), ('examples', chart_examples)):
        for mode, t in THEMES.items():
            with open(os.path.join(OUT, f'{name}-{mode}.svg'), 'w', encoding='utf-8', newline='\n') as f:
                f.write(fn(t))
    print('charts written to', OUT)


if __name__ == '__main__':
    main()
