# Industrial Bakery Oven

Registry ID: `craftdistrict:industrial_bakery_oven`. A static 2-wide, 2-tall,
1-deep decoration in the CraftDistrict creative tab. No inventory, interaction,
cooking, light emission, Block Entity, ticking or custom renderer.

## Placement and saved states

`WideTallDecorationBlock` defines `part=0..3` and four horizontal facings:

| Part | Column | Row |
| --- | --- | --- |
| 0 | Origin | Lower |
| 1 | Clockwise from facing | Lower |
| 2 | Origin | Upper |
| 3 | Clockwise from facing | Upper |

The shared `MultiBlockDecorationBlock` now supplies placement/removal loops with
small hooks for part count, state, index and offset. The counter and bread shelf
keep their existing registry IDs and `facing,primary=true/false` saved states;
their two-part offset implementations and all their visual assets are unchanged.
The oven uses 16 states, without a redundant primary property.

Placement checks build height, world border, loaded chunks, replaceability and
entity collision for all four positions. Server placement rechecks the entire
extension footprint before writing, checks each position again at its write,
and verifies previously written states after callbacks. On failure it removes
only the matching states written by this attempt, without loot. A late callback
failure can still consume the item under vanilla BlockItem flow; this needs
testing with mods that cancel or change placement.

Breaking any part derives the origin from that part's index and facing. Removal
compares exact expected states at the other three positions. Unrelated blocks
and differently indexed/facing neighboring ovens are preserved. A recursion guard
prevents repeated cleanup. Placement uses client/known-shape flags; removal now
uses normal neighbor notifications so supported decorations above removed parts
lose their support. Associated parts are removed with `setBlock`, which does
not invoke loot. Vanilla breaking invokes the broken part's ordinary loot table:
one oven item in Survival, none in Creative. These are implementation intentions;
in-world behavior is not yet verified.

All four collision shapes are full cubes. Placement needs no floor support.
Rotation transforms facing; structure mirrors also swap the two column indices.
Commands that spawn individual parts do not assemble the oven, and there is no
ticking orphan repair. Cleanup reads all member positions, loading a neighboring
chunk if necessary to avoid leaving an unloaded member behind. Explosion, piston,
structure-tool and other-mod interactions need separate testing.

## Editable model and materials

Run from the project root:

```powershell
python tools/generate_industrial_bakery_oven.py
python tools/preview_bakery_model.py industrial_bakery_oven --views --rear
```

Edit `whole_oven()` for the full 32 x 32 x 16 model, with north (-Z) at the front.
The generator clips cuboids at x=16 and y=16, translates each quadrant into
local block coordinates, crops UVs continuously and omits newly created interior
seam faces. Item rendering uses the unsplit model centered at (8,8,8).
Regeneration replaces the oven exports; JSON also opens in Blockbench.

- Dark metal body with brushed steel trim and a stone-colored base.
- Two stacked chambers, framed dark windows, raised handles and side hinges.
- Upper chamber has one tray with a long bread, narrow tin loaf and round loaf.
  Lower chamber has two trays with a round loaf, tin loaf, slimmer long bread and
  flatter loaf, making its door presentation distinct. Seven products total.
- Right-side temperature panels show static 220/190 indicators and button pixels,
  with raised control knobs. None of the indicators or controls function.
- Upper front vent, side vents and framed service panels, plus rear service
  panel and two ventilation strips.

There are 68 source cuboids and 112 exported cuboids across four block parts
(32/28/25/27). Nine new 32x32 tiles retain roughly one pixel per model unit on
the body. Existing bread textures are referenced without modification.
Window textures use binary alpha and opaque dark glass marks, registered on the
cutout layer across loaders. They approximate dark glass rather than true tinted
translucency, avoiding transparency sorting and a separate translucent pass.
Models use no animated textures or emissive rendering.

Previews are written to ignored `build/asset-previews/`. They show exported asset
geometry and textures, not actual Minecraft ambient occlusion or chunk lighting.
Use the front/side comparison to evaluate proportions before editing the source.

### Focused visual polish

- Handles retain their 15-unit width. Grip height increases from 0.5 to 0.75 and
  depth from 0.5 to 0.625 model units. Slightly wider dark mounts and a 1-unit
  downward shift place part of each grip against the glass instead of the matching
  steel frame. The front edge stays inside the footprint at z=0.125.
- Four shallow rear frame rails surround the maintenance panel. Its existing tile
  gains a gasket line, seam highlight, stamped ribs and small slotted fasteners;
  the same subtle texture detail appears on the sides. Each side gets one thin
  reinforcing strap. Vent geometry and textures are unchanged.
- Two round loaves use clipped-corner shoulders and a stepped crown; other
  loaves vary in width, length and height. The flatter loaf and slender breads
  contrast with the round forms. Product count, tray locations and bread textures
  are unchanged. Only ten source cuboids were added: six structural pieces and
  four loaf shoulders.
- Export keeps the existing continuous geometry/UV clipping at x=16 and y=16.
  Cropped UVs are normalized to six decimal places so shared edges serialize
  consistently. No artificial seam caps or model offsets were introduced. Actual
  Minecraft lighting at block/chunk-section boundaries still needs in-world review.

The double-deck shape, metallic palette, temperature displays, glass and vents,
registry ID, states, collision and Java behavior are unchanged by this pass.
The `--rear` preview option writes an additional rear three-quarter image.

## Build and manual validation

Build all four targets:

```powershell
.\gradlew.bat :forge:1.20.1:build :fabric:1.20.1:build :neoforge:1.21.1:build :fabric:1.21.1:build --no-parallel --console=plain
```

Launch one target at a time with the corresponding task, for example
`.\gradlew.bat :fabric:1.21.1:runClient`; the other tasks are
`:forge:1.20.1:runClient`, `:fabric:1.20.1:runClient` and
`:neoforge:1.21.1:runClient`.

Perform these checks in a separate test world on each target:

1. Find the oven in the CraftDistrict tab. Place facing all four directions;
   check all four parts, windows, handles, panels, side/rear details and collision.
2. Obstruct each of the four occupied positions separately with stone. Placement
   must fail without replacing stone, leaving partial ovens or consuming a
   Survival item. Repeat with an entity obstructing an upper/side position.
3. Check build-height and world-border limits. In the Overworld, lower y=318
   should fit and lower y=319 should fail. Check placement across chunk borders.
4. In Survival, break each of the four parts on separate fresh ovens. All four
   parts must disappear, dropping exactly one item each time. Clear prior drops.
5. Repeat all four break choices in Creative: all parts disappear with zero drops.
6. Place adjacent ovens with equal and different facings. Break one; its neighbor
   and nearby unrelated blocks must remain intact. Replace a part using a command
   and check preservation of the replacement and cleanup of the matching parts.
7. Save, quit and reload; revisit after chunks unload. Check state persistence,
   collision, rotation and paired removal. Inspect lighting at y=16 and chunk seams.
8. Recheck placement, obstruction, Survival/Creative breaking and reload of the
   counter and bread shelf because their shared placement/removal code changed.

No development client or running-world test was performed for this addition.
Successful builds and asset inspection do not verify these gameplay behaviors.
