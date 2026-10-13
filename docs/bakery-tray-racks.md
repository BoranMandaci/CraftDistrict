# Bakery expansion — Stage 1: Tray Racks

Only two new registrations:

- `craftdistrict:bakery_tray_rack_empty` — open industrial sheet-pan trolley.
- `craftdistrict:bakery_tray_rack_baked` — the same trolley with four loaded trays.

Both are static, non-interactive, 1 × 2 × 1 decorations in the Bakery collection
and CraftDistrict Creative tab. No other expansion family is implemented here.

## Geometry and shared assets

`tools/generate_bakery_tray_racks.py` owns the named geometry, six rail heights,
four occupied levels, product layouts and collision envelopes. It generates only
this family. Imported helpers reuse the existing vertical splitter, hidden-face
removal, UV projection and rounded/crescent bread geometry without regenerating
any existing decoration. Python 3 is sufficient for generation; previews use
the existing Pillow/NumPy renderer.

The single shared frame has four silver uprights, lower and upper perimeter
frames, six pairs of recessed tray runners, an upper rear diagonal brace and
a rear push handle. Four static caster assemblies use stepped tire profiles,
metal forks and textured axle hubs. There is no wheel rotation or trolley movement.

The baked rack loads levels 1, 2, 4 and 6 (bottom to top). Levels 3 and 5 remain
empty. Its nine products comprise three scored rolls, two elongated baguettes,
two round loaves, one crescent pastry and one berry tart. Products sit directly
on the sheet pans. No additional trays are placed in the empty variant.

Vanilla multipart blockstates reference the exact same lower/upper frame models
for both registrations. The baked variant adds the separate lower/upper contents
models; it does not replace the frame. Complete item models combine the same
frame with optional contents, centered vertically for inventory display.

The frame has 52 source cuboids; baked contents add 83, including four shallow
five-piece pans. Hidden fully covered faces are omitted. Frame clipping produces
continuous UVs at y=16 and omits internal cut-plane faces. All crossing rotations
are around Y; the rear diagonal brace stays entirely in the upper cell.
The source material palette reuses existing oven metal/steel, bakery bread and
pastry textures. One new opaque 32 × 32 caster texture is generated. No existing
model or texture is changed, and there is no custom rendering layer.

Generated resources:

- Two blockstate JSONs, with eight frame cases per variant; the baked state also
  has eight content cases.
- Four block models: shared lower/upper frame and lower/upper baked contents.
- Two complete inventory models, two ordinary single-item loot tables and one
  new caster texture. Source loot paths are adapted by the existing build for 1.21.1.
- `BakeryTrayRackShapes.java`, generated from conservative structural envelopes.

## Placement, removal and collision

`BakeryTrayRackBlock` extends the existing `MultiBlockDecorationBlock`, with UP
as the extension direction. Its fixed EMPTY/BAKED constructor variant selects
cached shapes. Only the existing `facing` and `primary` properties are saved;
the lower cell is primary. The 1.21 codec includes the fixed constructor variant.

The shared system validates both cells for replaceability, height/world bounds,
loaded chunks and entity obstruction, then rechecks the partner before writing
it on the server. Linked removal matches the exact block/state, guards recursion
and clears the partner without invoking loot a second time. The single-item loot
table applies to either part. Direct Creative breaking follows vanilla no-drop
behavior. Existing saved block IDs and behavior remain unchanged.

Outline/collision shapes are built once for each variant, part and horizontal
orientation: 16 cached shapes. Four caster envelopes replace their tiny individual
render pieces, while the open frame and runner gaps remain open. The baked
variant adds tray and product envelopes. Small curved/diagonal details therefore
do not exactly match selection outlines. Each occupied cell is exclusive: another
independent decoration cannot be placed inside the empty rack's visual gaps.

As with the current vertical decoration system, floor-standing describes the
model. It has no gravity or separate floor-support survival rule; removing the
floor does not automatically remove the rack. This addition does not alter the
shared placement architecture.

## Verification completed

- Forge 1.20.1, Fabric 1.20.1, NeoForge 1.21.1 and Fabric 1.21.1 builds passed.
- All four distributable JARs contain expected classes, loader metadata, models,
  referenced textures, translations and version-appropriate loot-table paths.
- Seven asset regressions passed: exact IDs/levels, shared frame, multipart
  cases, model/resource constraints, caster/open-frame shapes, product contact,
  seam UVs and ordinary loot. Existing decoration files match the pre-edit snapshot.
- Forge 1.20.1 development client launched. A temporary read-only client probe
  verified all 16 multipart block states and both inventory models have textured
  quads and no missing-model/missing-sprite fallback. The probe remains in ignored
  build output and is not part of a mod JAR.
- The fresh Forge client log has no missing-model, missing-texture or model-baking
  errors. Existing loader/development warnings are unrelated to rack resources.
- No running-world placement, collision, rotation, breaking, drop or persistence
  behavior has been tested. Other targets were built and package-checked, but
  their development clients were not launched for this stage.

## Previews and manual review

Run `python tools/preview_bakery_model.py bakery_tray_rack_empty --views --rear`
and the same command for `bakery_tray_rack_baked`. Previews are exported JSON
asset renders, not Minecraft screenshots. The combined sheet is
`build/asset-previews/bakery_tray_racks_family.png`; individual front/side/rear
views are in the same ignored directory.

Manual checks on all four targets:

1. Use the CraftDistrict tab or `/give @s craftdistrict:bakery_tray_rack_empty`
   and `/give @s craftdistrict:bakery_tray_rack_baked`. Inspect inventory icons,
   normal-distance bread silhouettes, rear handle, casters and the open levels.
2. Place both variants in all four orientations; check tray/rail contact and the
   y=16 seam under game lighting. Compare the frames for identical geometry.
3. Obstruct the lower or upper destination cell separately: placement should fail
   without consuming an item or replacing the obstruction. Test player/entity
   collision with the actual structural shape; open visual gaps remain open.
4. Break either part in Survival: both should disappear with exactly one matching
   rack item. Repeat in Creative: no rack item should drop. Preserve adjacent
   unrelated blocks and check that decorations above removed parts update.
5. Walk against the casters, runners and loaded trays. Save/reload with each
   orientation present, then repeat the upper/lower breaking checks.
6. Reload resources and inspect each target's client log. The other three clients
   still need runtime confirmation, including lighting/mipmapping and rendering.

Stage 1 is complete and awaits review. Later expansion stages require explicit approval.
