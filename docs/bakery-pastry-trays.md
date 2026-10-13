# Bakery Pastry Trays

Four ordinary static decorations in the existing Bakery collection and CraftDistrict
Creative tab. Each occupies one grid position, with four saved `facing` states.
No GUI, inventory, interaction, Block Entity, ticking, animation or custom renderer.

| Registry ID | Contents | Visible cuboids | Cached shape envelopes before optimization |
| --- | --- | --- | --- |
| `craftdistrict:croissant_tray` | Three golden crescents with open centers, curved arms and smaller tips | 65 | 14 |
| `craftdistrict:cookie_tray` | Six varied cookies; two chip patterns and a plain/oat-like top | 41 | 11 |
| `craftdistrict:bread_rolls_tray` | Five round voxel rolls of different sizes/heights, with scored crust tops | 35 | 10 |
| `craftdistrict:empty_baking_tray` | An empty pressed-metal baking tray | 5 | 5 |

## Geometry and assets

The shared tray is 12x10 model units (x=2..14, z=3..13), 0.35 units thick with
0.85-unit-high rims. Products touch the floor at y=0.35. Filled arrangements are
under three units high, substantially smaller than a full block. Bread rolls use
continuous octagonal bodies with shaded/scored tops; cookies are thinner octagonal
discs. Croissant centers use a bevelled cross-section with two rounded arms and
tapered tips, leaving real negative space inside each crescent. Cloth-bag assets
are not used; only established geometry/UV/validation helper functions are reused.

Ten new opaque 16x16 textures, prefixed `bakery_trays_`, supply pressed metal,
rim highlights, golden laminated dough, two chip patterns, plain cookies and scored
roll crust. No letters, numbers, mirrored UV ranges or transparent pixels are used.
Texture scoring quarter-turns rotate both the atlas crop and face vertices and
compose with model UV rotation, preserving continuous shading between pieces.
Item geometry is generated from the same model as its block, with separate item
display transforms. The common loot-table path is renamed by the existing build
for Minecraft 1.21.1's singular `loot_table` directory.

The editable source is `tools/generate_bakery_pastry_trays.py`. Named pieces,
`CROISSANTS`, `COOKIES`, `ROLLS`, palettes and tray dimensions can be revised there.
The source reuses functions from the existing counter/flour asset tools without
calling their entry points or writing their assets. Fully hidden elements are
removed; every exported element must retain 1-6 faces to satisfy Minecraft's model
loader. Models stay within one block and use only legal vanilla element rotations.
Generated block/item JSON files also open in Blockbench; regeneration overwrites
direct edits to them. Selection and collision use the generated cached envelopes
in `PastryTrayShapes.java`, with no shape construction during queries.

```powershell
python tools/generate_bakery_pastry_trays.py
python tools/test_generate_bakery_pastry_trays.py
python tools/preview_bakery_model.py croissant_tray --views --top
python tools/preview_bakery_model.py cookie_tray --views --top
python tools/preview_bakery_model.py bread_rolls_tray --views --top
python tools/preview_bakery_model.py empty_baking_tray --views --top
```

Previews go to ignored `build/asset-previews/`. They render exported assets and do
not simulate Minecraft ambient occlusion, world lighting, resource loading or UI.

## Placement and tabletop limitations

`PastryTrayBlock` extends the existing `RotatableDecorationBlock`. Its enum selects
one of four fixed model/shape families; it is not an extra blockstate property.
Stonecutter gates the Minecraft 1.21.1 codec in the same way as the flour sacks.
Support checks follow the existing cash register: the block below must expose a
sturdy full upward face. Vanilla BlockItem handles replacement checks, build bounds,
permissions and entity obstruction. Nothing places two blocks in one position.

| Support | Placement plan |
| --- | --- |
| Bakery Display Counter, either part | Supported: inherited full-cube support/collision and model top at y=16 give flush contact |
| Solid blocks and full-height top/double slabs | Supported when their upward face is sturdy; tray uses the cell above |
| Lower slabs, stairs with incomplete tops, fences, other trays | Rejected when their top is not sturdy at the full block boundary |
| Existing Dough Preparation Table | Not supported: its worktop is y=12.5, not y=16, and fixed props occupy that surface |

One supporting cell allows one tray, even when the model leaves spare visual space.
A two-block counter can hold a tray over each half if both upper cells are empty.
A cash register or another tray already occupying a cell prevents placing a second
independent decoration there. Placement is grid-centered, not free positioning.

The preparation table cannot accept the same y=0 tray without a 3.5-unit floating
gap, and lowering a tray would overlap its fixed dough/tools/bowl/sack. No existing
furniture model, collision, saved state or placement rule was changed to accommodate
this. A maintainable next option is a separate clear preparation-table variant with
a full-height sturdy top. Supporting arbitrary lower surfaces would instead need an
explicit mounting-height state, corresponding model/collision offsets and checks
against the support's fixed props; that is a separate architectural task.

The declared support shape, rather than visible JSON geometry, determines eligibility
for other mods' blocks or existing open-frame furniture with full-cube support.
Commands using flags that suppress neighbor updates may leave an unsupported tray
until another update. There is no tick-based repair or surface-height search.

## Removal and manual checks

On support loss, the tray's normal `updateShape` returns air and vanilla neighbor
propagation removes it with ordinary loot. Existing linked counter-part removal
already notifies supported decorations above both halves; no shared-system changes
were required. Direct Survival breaking drops one tray item; direct Creative
breaking drops none. Indirect support loss can drop a tray even when its supporting
counter is broken in Creative, matching the existing cash register policy.
Pastries are permanent model contents and never separate food/item drops.

```powershell
.\gradlew.bat :forge:1.20.1:build :fabric:1.20.1:build :neoforge:1.21.1:build :fabric:1.21.1:build --no-parallel --console=plain
```

All four target builds passed for this addition. Six new tray asset tests and
four existing Flour Sacks regression tests passed. The Minecraft 1.20.1 compiler
reported the same deprecated block-API override note as existing decorations;
Gradle deprecation warnings remain. No development client or running-world test
was performed for these trays.

On all four targets, manual in-world verification is still required:

1. Find all four entries and inspect their inventory/hand models, textures, loading,
   croissant openings, cookie chips and roll scoring at ordinary viewing distances.
2. Place every tray on both counter parts and sturdy vanilla surfaces in all four
   orientations. Check flush contact, custom outlines, food heights and collision.
3. Confirm rejection on air/lower slabs/the existing preparation table, occupied
   target cells and obstructing entities. Failed Survival placement should consume
   no item or replace an unrelated block. One cell must not accept two decorations.
4. Break a tray directly in Survival and Creative; check one/no matching tray item.
   Break either half of a counter supporting trays above both parts, and check
   removal/loot without duplicated trays, stale supports or unrelated block removal.
5. Save/reload facing and contents; verify no UI or item-transfer interaction.
   Review the conservative food-envelope collision against the visible curves.

Static asset tests, preview renders and successful builds do not prove these
placement, breaking, persistence or rendering results inside a running world.
