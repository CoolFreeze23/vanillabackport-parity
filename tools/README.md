# Tools

These scripts regenerate the data the add-on ships. They read decompiled reference trees from a folder named by the
`VBP_REF` environment variable (default `ref/`):

| Folder under `VBP_REF` | Contents |
|---|---|
| `src-vb/` | VanillaBackport 1.2.2, decompiled (classes and its `data/`, `assets/`) |
| `src-platform/` | Platform 1.4.2, decompiled |
| `src-mc/` | Minecraft 26.2 client, decompiled, with its `data/` and `assets/` |
| `src-mc263/` | Minecraft 26.3-pre-1 client, decompiled, with its `data/` and `assets/` |
| `src-121data/` | vanilla 1.21.1 `data/` extracted from the 1.21.1 client jar |
| `mc-1.21.1-data/` | the same 1.21.1 data, as read by `datasweep` and `gen_flora.py` |
| `assets-26.2/files/` | 26.2 asset-index files (sounds, `sounds.json`, lang), sha1-checked |
| `assets-26.3/` | 26.3-pre-1 `sounds.json` and `lang/` |

`MC121_CLIENT_JAR` points at the vanilla 1.21.1 client jar. It defaults to `$VBP_REF/minecraft-1.21.1-client.jar`.

Run every script from the project root:

| Script | Writes |
|---|---|
| `shared_data.py` | native advancement criteria, recipe unlocks, shared tag entries |
| `tag_replace_to_remove.py` | turns full tag replacements into single-entry `remove` lists where that is safe |
| `gen_flora.py` | the 1.21.1-format flora worldgen overrides |
| `gen_native_features.py` | `vbparity/native_biome_features.json` (native feature order and decoration seeds) |
| `datasweep/` | effective-vs-native comparisons and the translation generator |
| `charts/make_charts.py` | the README charts, from `docs/data/*.csv` |
