# Dough Preparation Table

Registry ID: `craftdistrict:dough_preparation_table`. A static 2 x 1 x 1 bakery
workbench, registered in the Bakery collection and CraftDistrict Creative tab.
No interaction, inventory, GUI, Block Entity, ticking, animation or custom renderer.
All materials are opaque and use the normal solid block render layer.

## Editable geometry and assets

Source: `tools/generate_dough_preparation_table.py`, Python standard library only.
The whole model occupies x=[0,32], y=[0,16], z=[0.5,15.5]. NORTH (-Z) is the front.
The primary block occupies x=[0,16]; the associated block extends toward EAST.
The whole footprint rotates together using the four facing states.

The preparation surface is at y=12.5, leaving room for the fixed props within
the one-block height. Its 32x16 pixel-art texture includes restrained flour
patches. Warm light wood differentiates it from the darker display furniture.

Named source groups:

- `furniture()`: four legs, front/rear aprons, end cross supports, lower shelf
  and shelf edging. The rear remains open and finished like the front.
- `dough_and_pin()`: a thin irregular dough sheet, a broad stepped dough ball,
  and a seven-piece diagonal rolling pin. Its barrel has a stepped round cross
  section, short necks and narrower handles. Every pin element shares a vanilla
  22.5-degree Y rotation and pivot; the pin stays entirely within the primary.
- `bowl_and_flour_bag()`: a recessed mixing bowl with a smaller foot and raised
  rim, visible flour, and a cloth flour bag with an open folded mouth. An upright
  wheat emblem appears only on the bag's NORTH face; no mirrored text is used.
- `storage()`: an open wooden crate with a board inside and two tied cloth sacks.

The exported block models are clipped at x=16. Spanning face UVs are cropped
using Minecraft's actual face directions, and artificial faces at the joint
are omitted. Contact undersides and selected internal faces are also omitted.
The item model contains the whole unsplit geometry, centered at x=8; its rolling
pin pivot receives the same translation as the cuboids. JSON can be edited in
Blockbench, but regenerating replaces those exported edits.

```powershell
python tools/generate_dough_preparation_table.py
python tools/preview_bakery_model.py dough_preparation_table --views --top --rear
```

The generator rewrites only this decoration's resources. Previews are saved in
ignored `build/asset-previews/`: front, side, top, front/side sheet, and front/rear
three-quarter views. The top view places the NORTH front at the bottom. These
are renders of exported assets, not Minecraft screenshots; lighting, ambient
occlusion, filtering and resource loading still require an in-world review.

## Placement, breaking and collision

`DoughPreparationTableBlock` inherits `MultiBlockDecorationBlock` and uses the
same clockwise width direction as `WideDecorationBlock`. It retains the shared
`facing` and `primary` states. No shared placement or removal code was changed.

The inherited placement path checks both positions for replacement eligibility,
build height, world border, loaded chunk and entity obstruction, then rechecks
associated placement on the server. Failed associated writes are rolled back
only where the exact state still belongs to this placement. Breaking either part
removes the matching other part without invoking its loot. One ordinary loot
table serves both parts: the part actually broken drops one table in Survival;
direct Creative breaking drops none. Recursion protection and exact partner-state
matching are inherited and preserve unrelated blocks and adjacent tables.

Eight cached outline/collision shapes cover two parts and four orientations.
They follow the legs, supports, tabletop and lower shelf, with conservative
envelopes for the fixed props and storage. The diagonal pin and rounded props
use simple bounding envelopes; tiny rims, handles and flour specks do not each
have individual collision boxes. Shape queries allocate no new geometry.

The table uses ordinary block-grid placement and the existing furniture support
policy, which does not add a floor-survival check. It does not provide a full
sturdy UP face at y=16: the worktop is at y=12.5 and the props are fixed. The cash
register cannot be placed onto this lower preparation surface via its current
full-top support rule. No surface-height attachment system has been added.

## Build and manual verification

```powershell
.\gradlew.bat :forge:1.20.1:build :fabric:1.20.1:build :neoforge:1.21.1:build :fabric:1.21.1:build --no-parallel --console=plain
```

For a development client, use one target at a time, for example
`.\gradlew.bat :fabric:1.21.1:runClient`. The other tasks are
`:forge:1.20.1:runClient`, `:fabric:1.20.1:runClient`, and
`:neoforge:1.21.1:runClient`.

No client or running-world test was performed for this addition. Check on each target:

1. Creative tab entry, inventory/hand model, all textures, object contact and
   front/side/rear/top appearance. Inspect the tabletop joint under game lighting.
2. Place in NORTH/EAST/SOUTH/WEST orientations. Both halves should face together;
   check selection outlines and collision around legs, storage and surface props.
3. Obstruct the primary or associated position with a solid block, then with an
   entity intersecting the occupied shape. Placement must fail without replacing
   unrelated blocks or consuming a Survival item. Test near border/height limits.
4. Break each half in separate Survival trials: both disappear with exactly one
   table item. Repeat in Creative: both disappear without a table item. Verify
   adjacent tables, unrelated neighboring blocks and other bakery blocks remain.
5. Save/reload each orientation and repeat breaking from both halves. Check that
   right-clicking opens no GUI and changes nothing.

Build success and static asset inspection do not establish those in-world results.
Commands, external editing, moving structures and other mods that cancel placement
or suppress neighbor updates still require separate compatibility testing.
