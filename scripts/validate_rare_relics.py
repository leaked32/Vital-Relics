import json
import struct
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resources = root / 'common/src/main/resources/vitalrelics'
ids = ['frostbind_ring', 'timberheart_ring', 'quarry_ring', 'borebolt_bracelet', 'discord_bracelet']
skills = ['slowing_aura', 'tree_feller', 'area_mining', 'borebolt', 'compel_attack']
relics = json.loads((resources / 'relics.json').read_text())['relics']
assert len({r['id'] for r in relics}) == len(relics)
by_id = {r['id']: r for r in relics}
recipes = json.loads((resources / 'recipes.json').read_text())
for i, identifier in enumerate(ids):
    relic = by_id[identifier]
    assert relic['rarity'] == 'rare'
    assert relic['curio_slot'] == ('ring' if i < 3 else 'bracelet')
    assert skills[i] in relic['passive_skills' if i < 3 else 'available_spells']
    assert sum(r['texture'] == relic['texture'] for r in relics) == 1
    png = (resources / 'textures' / relic['texture']).read_bytes()
    assert png[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', png[16:24]) == (16,16)
    assert 'vitalrelics:rare_relic_core' in recipes['recipes'][identifier]['key'].values()
    assert all(0 < row['chance'] <= 1 for row in recipes['loot'][identifier])
    for locale in ['en_us','zh_cn','zh_tw','ja_jp']:
        translations = json.loads((resources/'lang'/f'{locale}.json').read_text())
        for prefix in ['item.vitalrelics.', 'tooltip.vitalrelics.']:
            assert translations[prefix+identifier]
        assert translations['relic.vitalrelics.'+('passive_skill.' if i<3 else 'spell.')+skills[i]]
        assert '`'+skills[i]+'`' in (resources/'guide'/locale/'configure-curios.md').read_text()
print('PASS: five rare relics, unique textures, recipes, loot and four-language documentation')
