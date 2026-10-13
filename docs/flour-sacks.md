# Flour Sacks

Three separate ordinary blocks in the Bakery collection and CraftDistrict Creative tab:

| Registry ID | Arrangement | Cuboids | Cached shape bands before optimization |
| --- | --- | --- | --- |
| `craftdistrict:flour_sacks_small` | Two broad upright cloth sacks with unequal heights and folded/gathered openings | 34 | 6 |
| `craftdistrict:flour_sacks_stacked` | Two flattened floor sacks with one horizontal sack turned diagonally across them | 40 | 9 |
| `craftdistrict:flour_sacks_pile` | Three horizontal lower sacks supporting two diagonal horizontal upper sacks | 63 | 15 |

Each arrangement fits within one 16x16x16 model volume and has four horizontal
orientations. There are no inventories, GUIs, interactions, Block Entities,
animations, ticking, custom renderers or multiblock partners. Each block/item has
its own stable registry ID and loot table, so breaking drops the whole arrangement.

## Geometry and revision

The editable source is `tools/generate_flour_sacks.py`. `LAYOUTS` lists each sack's
position, actual dimensions, upright/horizontal profile, cloth shade, wheat mark,
and optional horizontal turn. The profiles were substantially replaced:

- Upright sacks use a broad filled volume with sloping heel and shoulder corners,
  rather than a rectangular container with a neck and cap. Two short, off-center
  cloth folds form the opening. The pair is 6.75x10x6.5 and 6.25x8.5x6 model units
  before the cloth corner bounds; both remain upright, offset in depth.
- Floor sacks have an explicitly horizontal pillow profile, not a rotated upright
  sack. Broad bevels soften the compressed cross-section and sloping ends. A short
  sewn/folded end lies against the body. The stack's floor sacks are 11.5x3.75x5.5
  and 11x3.6x5.5 units, with an 11.4x3.5x5 upper sack turned 22.5 degrees.
- The pile's three lower sacks are approximately 12x3.2x4.3 units, arranged front,
  middle and rear. Two 10.4x3.55x3.8 and 10.2x3.4x3.8 upper sacks turn in opposite
  directions by 22.5 degrees. The pile is broad and low, without upright towers.

Upper contact surfaces overlap their supports by about 0.15-0.35 model units to
suggest compression and avoid a floating gap. Nominal heights are about 10 units
for the small pair, 6.9 for the stack and 6.5 for the pile, all within one block.

Five 16x16 opaque textures are shared across the family: two cloth shades,
gathers, twine and a subtle wheat stencil. All five textures were redrawn with
cream/muted burlap tones, broken cloth creases, faint stitching, gathered folds
and restrained markings. Cloth UVs span each whole sack rather than repeating a
complete stitched border on every cuboid. There are no letters/numbers to mirror.

Upright and lower horizontal sacks use three broad core cuboids and twelve edge
bevels; small recessed corners suggest pinched fabric without tiny corner cubes.
These bevels soften the body in front, side and rear views. Diagonal sacks use six
broad cuboids to form a bevelled
footprint and a flatter top: standard JSON supports only one rotation axis per
element, so a diagonal Y turn cannot also tilt a face about X/Z. Compound Y turns
are normalized to legal element angles by baking quarter-turns into coordinates,
remapping faces and preserving their UV direction. No illegal 67.5/90-degree
element rotations or custom mesh renderer are used.

Fully covered faces with the same transform are omitted. Any element whose faces
are all removed is omitted too: Minecraft rejects `"faces": {}` with
`Expected between 1 and 6 unique faces, got 0`. The generator validates this rule
before exporting both block and inventory models. Collision envelopes are computed
before render-only culling, so removing invisible elements does not change shapes.
Regression checks are available with `python tools/test_generate_flour_sacks.py`.

Partially overlapping
end faces of a bevelled volume have a 0.0002-0.0004-unit depth separation to avoid
coplanar flicker. Exposed bottoms remain available for item rendering. All textures
are opaque and use ordinary solid block rendering.

```powershell
python tools/generate_flour_sacks.py
python tools/preview_bakery_model.py flour_sacks_small --views --rear
python tools/preview_bakery_model.py flour_sacks_stacked --views --rear
python tools/preview_bakery_model.py flour_sacks_pile --views --rear
```

Generation rewrites only these three blocks' resources, the five shared sack
textures, and `src/main/java/com/craftdistrict/block/FlourSackShapes.java`.
Edit layouts/profile geometry here so both models and collision data stay in sync.
The exported JSON models also open in Blockbench; regeneration replaces those edits.
The previews are saved under ignored `build/asset-previews/` and are asset renders,
not Minecraft screenshots or proof of resource loading in a development client.

## Block behavior and shapes

`FlourSacksBlock` extends the existing `RotatableDecorationBlock`. Three registered
instances select a fixed `Arrangement`; there is no arrangement blockstate or
random visual change. The Minecraft 1.21.1 codec preserves the arrangement field
when reconstructing a block. Stonecutter isolates that codec from Minecraft 1.20.1.
The hand-written block behavior, registry IDs and all bakery furniture resources
are unchanged; only flour sack assets and generated shape data were revised.

All three collision/outline shapes were regenerated. Upright and lower horizontal
sacks have narrower heel and shoulder bands around the wider belly, including the
folded opening. Diagonal sacks use three coarse envelopes for the pillow,
compressed underside and folded closure. Diagonal bounds are computed after
rotation. Union and the four facing rotations happen once at
startup; shape queries return cached shapes. These conservative axis-aligned
envelopes approximate bevels and diagonals rather than tracing every visible face.

Registry IDs, facing blockstates, placement/breaking code, item transforms and
loot rules are unchanged. Existing placed sacks show the new model and collision
on reload; their identities and orientations remain compatible. No bakery furniture
resource changed. This is a visual/shape revision, not a behavior migration.

Placement uses vanilla BlockItem replacement/entity obstruction behavior, with
facing selected by the shared rotation block. As with existing static furniture,
no ground-survival requirement is imposed; support removal does not automatically
remove a sack decoration. These are ordinary grid-aligned blocks, not objects that
attach to arbitrary surface heights. Survival breaking uses a single-item loot
table, while direct Creative breaking uses vanilla no-drop behavior.

## Builds and manual checks

```powershell
.\gradlew.bat :forge:1.20.1:build :fabric:1.20.1:build :neoforge:1.21.1:build :fabric:1.21.1:build --no-parallel --console=plain
```

All four target builds passed for this redesign. The compiler reported a Minecraft
1.20.1 API deprecation note for block shape overrides; existing Gradle deprecation
warnings also remain. Build and preview checks alone do not prove that Minecraft
accepts the models; the empty-face loading problem was subsequently found in the
Forge client log and corrected in both models and their generator.

After that fix, a Forge 1.20.1 development client successfully loaded the resources.
A temporary read-only check of the live ModelManager verified all twelve block
facing models and all three inventory models: each had textured quads, none used
the missing-model fallback, and no quad used a missing-texture sprite. The report
is in ignored `build/model-loading-check/forge-1.20.1.txt`. Verification helpers
are confined to the build directory and are not packaged in distributable JARs.
An initial helper attempt ran too early and disturbed Forge startup; the successful
retry deferred inspection until normal startup finished.

Fabric/NeoForge clients were not launched for this resource fix. No automated
placement, breaking, drop or persistence test was performed in a running world.

Launch one target at a time, for example `.\gradlew.bat :fabric:1.21.1:runClient`.
The other targets use `:forge:1.20.1:runClient`, `:fabric:1.20.1:runClient` and
`:neoforge:1.21.1:runClient`. Check every variant on each target:

1. All three entries in the Creative tab; correct inventory/hand models, textures,
   front/rear/side appearance, cloth UVs, stencil orientation, and sack-to-sack contact.
2. Placement in all four orientations, selection outlines, collision around the
   compact pair, stacked top and leaning pile. Conservative envelopes need review.
3. Placement failure against a nonreplaceable block or an entity intersecting the
   shape. No unrelated block should be replaced or Survival item consumed on failure.
4. Survival breaking: exactly one item matching the selected variant. Creative
   breaking: no item. Adjacent arrangements and other decorations should remain.
5. Save/reload: registry variant and facing should persist. Right click should
   open nothing and change nothing. Check lighting and contact at normal viewing distance.

Build and static asset inspection do not establish these in-world results.
