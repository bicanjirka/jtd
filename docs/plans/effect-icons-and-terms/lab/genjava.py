"""Turns the icon lab files into the Java entry lists the plan hands to the implementer."""
import sys

FAMILIES = [
    ('EffectPictograms', 'effects.txt', 'The effect kinds, keyed by {@code EffectKind.key()}.'),
    ('TowerTermPictograms', 'towers.txt', 'Tower buffs, debuffs and named mechanics, keyed by {@code TowerTerm.key()}.'),
    ('EnemyTermPictograms', 'enemy.txt', 'Enemy traits and abilities, keyed by {@code TraitTerm.key()} and {@code AbilityTerm.key()}.'),
    ('PerkPictograms', 'perks.txt', 'Perk lines, keyed by the id of the node whose art the line shows ({@code UpgradeLine.artNode()}).'),
]


def entries(path):
    out = []
    for raw in open(path, encoding='utf-8'):
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        parts = [p.strip() for p in line.split('|')]
        key, tone, frame, title, spec = parts[0], parts[1], parts[2], parts[3], parts[4]
        spec = ' '.join(spec.split())
        if spec.startswith('='):
            spec = '=' + spec[1:].strip()
        out.append((key, title, spec))
    return out


def java(cls, path, doc):
    lines = []
    lines.append('package td.ui;')
    lines.append('')
    lines.append('import java.util.List;')
    lines.append('import java.util.Map;')
    lines.append('')
    lines.append('/** ' + doc + ' Each spec is {@link Pictogram#parse} input; "=key" reuses another pictogram. */')
    lines.append('final class ' + cls + ' {')
    lines.append('')
    lines.append('    static final List<Map.Entry<String, String>> SPECS = List.of(')
    es = entries(path)
    for i, (key, title, spec) in enumerate(es):
        comma = ',' if i < len(es) - 1 else ');'
        lines.append('            // ' + title)
        lines.append('            Map.entry("' + key + '", "' + spec + '")' + comma)
    lines.append('')
    lines.append('    private ' + cls + '() {')
    lines.append('    }')
    lines.append('}')
    return '\n'.join(lines)


if __name__ == '__main__':
    target = sys.argv[1]
    with open(target, 'w', encoding='utf-8') as f:
        for cls, path, doc in FAMILIES:
            f.write('### `src/main/java/td/ui/' + cls + '.java`\n\n```java\n')
            f.write(java(cls, path, doc))
            f.write('\n```\n\n')
