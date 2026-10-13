# Bakery Signage Pack

Five independent decorative blocks in the Bakery registry collection and the
CraftDistrict Creative tab. All are static, single-cell blocks with four horizontal
orientations. They have no editable sign text, inventories, GUIs, Block Entities,
animation or ticking.

| Registry ID (`craftdistrict:`) | Design | Mount |
| --- | --- | --- |
| `bakery_hanging_shop_sign` | Double-sided bread plaque, two suspension links, dark projecting arm and triangular brace | Wall |
| `bakery_wall_shop_sign` | Wide warm-wood frame, bread/wheat artwork and raised header | Wall |
| `bakery_menu_board_wall` | Large framed chalkboard, three product/menu rows with price marks | Wall |
| `bakery_daily_special_board` | Smaller chalkboard with a prominent crescent pastry and shallow chalk ledge | Wall |
| `bakery_sidewalk_menu_board` | Two tilted chalk faces, wooden legs, spreaders, small feet and metal hinge | Floor |

## Placement

`BakerySignageBlock` extends the existing horizontal decoration base. Its fixed
variant controls the model/shape and support direction; only `facing` is saved in
the blockstate. The 1.21 codec also serializes the constructor variant, following
the existing pastry-tray family pattern. No old block or shared behavior changes.

Wall variants are placed by clicking a **vertical, sturdy supporting face**.
Their facing equals the clicked face, pointing outward. They occupy the ordinary
neighboring cell, with the support behind their facing. For north-facing models,
the wall is in the south neighboring cell and the back reaches model z=16.
Boards are approximately 1.25–2.375 model units thick, including their details.
The hanging sign's arm projects almost a full cell outward; its plaque lies
perpendicular to the wall. Its west/east broad sides carry separately oriented
artwork, so it is naturally edge-on when viewed straight toward the wall.
There is no ceiling-hanging mode or automatic search for another nearby wall.

The sidewalk sign uses the existing player-relative horizontal orientation and
requires a sturdy top face below it, like the cash register and trays. Solid
blocks and suitable top slabs can support it. Its feet and splayed boards stay
within one cell and do not require a second block above.

Support checks and normal neighbor propagation remove signage when the relevant
support stops being sturdy. Ordinary BlockItem placement handles replacement
and entity obstruction. Ordinary block loot handles direct Survival breaking
and support-loss drops. Direct Creative breaking does not drop an item; removing
the support in Creative may still drop the sign, following vanilla support-loss
behavior. All world changes use the normal server-authoritative block lifecycle.

Each decoration occupies an entire Minecraft cell even though its visual and
collision geometry are small. Independent signs/props cannot share that cell.
The conservative sturdy-face rule intentionally excludes fences, glass panes,
open trapdoors and other incomplete supports; confirm actual support behavior
in-game. Existing counter/workbench geometry is unchanged.

## Editable assets and shapes

Run `python tools/generate_bakery_signage.py` to generate only this family:
five blockstates, five block models, five inventory models, six opaque 32 × 32
pixel-art textures, five loot tables and `BakerySignageShapes.java`. Existing warm
wood/trim textures are referenced read-only. Geometry uses named standard JSON
cuboids and legal single-axis rotations; no special rendering layer is needed.
Artwork uses explicit outward-facing UVs. Both hanging sides and both A-frame
faces use unreversed UVs with separate outward normals.

The models have 11/8/6/7/23 visible cuboids respectively. Shapes are cached for
all 20 variant/facing combinations and used for both collision and outline.
Wall shapes follow thin frames; the hanging shape includes the arm and plaque.
The A-frame uses three collision bands per tilted board rather than a full-cell
box. Rotated bars and legs use conservative bounding envelopes, so the outline
does not exactly follow every angled face or tiny artwork surface.

Variants have independent registrations, assets, loot and translations for easy
review. If pruning later, remove the corresponding entry consistently from
registration, the Creative tab, generator, enum and generated shape rows; keep
saved-world compatibility in mind before removing a released registry ID.

## Verification and previews

All four builds passed: Forge/Fabric 1.20.1 and NeoForge/Fabric 1.21.1. Packaged
JAR checks verify metadata, Java versions, signage classes, all blockstates and
models, referenced textures, translations and five version-appropriate loot tables.
Six asset regression tests cover IDs/order, legal model constraints, shape bounds,
wall contact, bracket contact, double-sided UVs, non-overlapping A-frame faces,
resource paths and facing variants. Existing decoration files match the pre-edit
snapshot. Existing 1.20.1 API and Gradle deprecation warnings remain.

`python tools/preview_bakery_model.py <registry_id_without_namespace> --views --rear`
generates isometric/front/side/rear previews in ignored `build/asset-previews`.
Family sheets are `bakery_signage_isometric.png`, `bakery_signage_front.png` and
`bakery_signage_side_views.png`. These are asset renders, not game screenshots.

No development client or running-world tests were performed for this pack.

### Manual checks

1. Launch a development client with the corresponding Gradle task:
   `:forge:1.20.1:runClient`, `:fabric:1.20.1:runClient`,
   `:neoforge:1.21.1:runClient` or `:fabric:1.21.1:runClient`.
2. In a disposable world, find all five in CraftDistrict. Inspect icons, front and
   rear artwork, and normal-distance readability. Reload resources and inspect
   logs for missing-model/texture errors.
3. Click each of the four sides of a solid wall. Check outward facing, flush
   board attachment, the hanging mount/brace and both broad sides of its plaque.
   Confirm wall variants reject top/bottom faces and unsupported placement.
4. Place the A-frame in four orientations on a full floor block and top slab.
   Check feet, entity obstruction, collision around the stand and its open sides.
   Confirm unsuitable supports and occupied destination cells reject placement.
5. Break each sign in Survival and Creative; check expected drops. Remove its
   wall/floor support and check removal, one support-loss drop and preserved
   unrelated neighbors. Repeat with a support changing shape rather than removed.
6. Save/reload with all orientations present, then repeat support/breaking checks.
   Test on all four targets; these behaviors have not been exercised in a world.
