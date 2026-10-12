# Bakery Cash Register

Registry ID: `craftdistrict:bakery_cash_register`. One ordinary block with four
horizontal facing states, added to the existing CraftDistrict creative tab.
The price is a static `12.50` texture, not a calculated value. No inventory, GUI,
interaction, ticking, Block Entity, animation, custom renderer or light emission.

## Model and editing

The model fits within x=[2,14], y=[0,10], z=[2,14]: 12 x 10 x 12 model units
(each unit is 1/16 block). A cash drawer forms the base; the keypad deck and all
14 raised keys share a vanilla -22.5-degree X rotation. A short stem carries the
main price display, with a smaller customer-facing price display on the back.
The printer has a dark receipt slot and one short static paper stub. Drawer seam,
metal handle, rear service panel and cable socket complete the details.

Colors are charcoal/black, gray metal, subdued red/green action keys, a pale green
price readout and warm dark wood inlays. The wood tile is referenced from the
Display Counter without rewriting it. Seven new opaque pixel-art textures serve
33 cuboids. Numeric keycaps use cells from one atlas; small labels do not each
require their own texture. The display texture is 32x16, key atlas 32x32, and
other tiles 16x16. This uses standard solid block rendering.

Editable source and previews:

```powershell
python tools/generate_bakery_cash_register.py
python tools/preview_bakery_model.py bakery_cash_register --views --rear
```

Edit `register_model()` and texture samplers, then regenerate. Exported JSON also
opens in Blockbench. Regeneration rewrites only register resources. Front/side,
front three-quarter and rear three-quarter previews are saved under ignored
`build/asset-previews/`. These are asset previews, not Minecraft screenshots.

### Text orientation

Only the main screen's NORTH face and the customer screen's SOUTH face use the
`12.50` readout; their other faces are dark edging. The original full-face mapping
repeated the digits on every thin screen edge, including inverted top-face text.
The outward vertical screen UVs themselves already followed Minecraft's correct
face orientation and retain `[0,0,16,16]` without a horizontal flip.

All 14 key tops rotate their atlas cells by 180 degrees, so the labels face the
operator at the model's NORTH side. Numeric cell selection is also reversed in
both grid axes: the operator sees `1 2 3`, `4 5 6`, `7 8 9`, then `. 0 -` from
far to near and left to right. Key geometry, deck slope and action-key locations
are unchanged. The glyph textures are unchanged, and normal facing rotations
carry these local UVs with the register; blockstates do not use `uvlock`.

The preview renderer now uses Minecraft's face UV directions and camera
handedness. Previously its mirrored camera and mirrored vertical-face UVs
cancelled each other for screens, while masking the keypad orientation issue.
Inspecting these corrected asset previews is not an in-world rendering test.

## Placement, support and collision

`CashRegisterBlock` extends `RotatableDecorationBlock`. It selects a cached shape
for each facing: a union of eight bounding boxes for the drawer, stepped keypad
housing, printer, stem and display. The shape is substantially smaller than a
full block and leaves open space around the body and underneath the display.
Collision and outline share the same shapes. Two steps conservatively approximate
the angled keypad; they are not a triangle-perfect mesh. Rendered details such
as the small cable socket do not have individual collision boxes.

Placement requires the block below to expose a FULL sturdy UP face, using vanilla
`isFaceSturdy`, and uses normal BlockItem replacement/entity collision checks.
Full blocks, full top slabs and suitable upside-down stairs can qualify. A lower
slab, narrow fence, carpet, open frame or floating position does not qualify.
Placement is aligned to the block grid: there is no offset for lower slab tops,
no surface height search and no free positioning within the supporting block.
One register occupies the block above one supporting position.

The existing Bakery Display Counter uses the inherited full-cube collision and
support shape, despite its `noOcclusion` visual setting. Its modeled countertop
ends at y=16. Both saved parts therefore have a stable full top surface suitable
for the register, without changes to the counter model or collision. Other
blocks exposing the same support face also qualify; no registry-ID special case
or loader-specific placement path is needed.

When a normal neighbor update removes support, the register's `updateShape`
returns air and vanilla removes/drops it. To support this on either counter part,
linked-part cleanup/rollback in `MultiBlockDecorationBlock` now uses normal
`UPDATE_ALL` removal notifications. Associated-part placement retains its existing
client/known-shape flags. Recursion protection and exact state matching remain,
and associated furniture parts still do not invoke their own loot. The shared
change is required so registers above an indirectly removed counter part do not
remain floating. All three existing visual designs and saved registry/state
identities are unchanged.

Breaking the register directly uses its ordinary loot table: one item in Survival,
none in Creative. Losing support uses vanilla indirect-removal loot, which can
drop the register even if the player broke its support in Creative. Commands or
other mods using flags that suppress neighbor updates may leave unsupported
registers until a subsequent update. There is no periodic repair.

## Builds and manual in-world checks

```powershell
.\gradlew.bat :forge:1.20.1:build :fabric:1.20.1:build :neoforge:1.21.1:build :fabric:1.21.1:build --no-parallel --console=plain
```

Launch one target at a time, for example `.\gradlew.bat :fabric:1.21.1:runClient`.
Equivalent tasks are `:forge:1.20.1:runClient`, `:fabric:1.20.1:runClient` and
`:neoforge:1.21.1:runClient`.

No client or running-world test was performed for this addition. On each target:

1. Find the register in the Creative tab. Place it on stone and each counter half
   in all four orientations. Check model, front/rear digits, keys and receipt.
2. Check the visible support contact, selection outline and movement around the register.
   The block outline should follow the cached stepped shape rather than a cube.
   The keypad approximation and tiny detail selection need visual review.
3. Verify successful placement on a full top slab, failed placement on a lower
   slab/fence/air, and failure when an entity occupies the actual collision shape.
   Check that failed Survival placement does not consume an item.
4. Place one register above each counter part. In fresh trials, break either
   counter half: both supported registers must be removed, with one register
   item each, and one counter item in Survival. In Creative, direct counter
   breaking drops no counter item; indirect support loss may drop the registers.
5. Break the register itself in Survival/Creative: one/no item, with the supporting
   counter and neighboring decorations preserved. Verify right click has no GUI.
6. Save, quit and reload; check facing, contact, collision and support loss again.
7. Recheck paired removal and item counts for the counter, shelf and oven after
   the shared removal-notification change, including adjacent decorations.

Placement cancellation, suppressed updates, moving supports and other mods need
compatibility testing. Successful builds and model inspection do not verify any
of these in-world behaviors.
