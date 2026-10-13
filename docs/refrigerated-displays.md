# Bakery expansion — Stage 2: Refrigerated Displays

- `craftdistrict:refrigerated_cake_display`
- `craftdistrict:refrigerated_pastry_display`

Both are static 2 × 2 × 1 decorations in the CraftDistrict Creative tab. Their
charcoal/steel case is shared; only the dessert contents differ. There are no
inventories, GUIs, Block Entities, refrigeration, animation or emitted light.

## Case and compositions

The case has a low machinery housing, recessed front/side ventilation, a steel
toe strip, thin corner posts, a charcoal roof and two rear service doors with
handles. Three pale enamel display shelves sit at model heights 8, 15.5 and 23.5.
Small warm-colored strips under the roof suggest interior lights through texture
detail only. There are no actual light sources, emissive shaders or dynamic lights.

Both variants contain nine desserts arranged four on the bottom, three in the
middle and two on top. The cake display uses stepped wedge slices, octagonal
layer cakes, thin frosting caps and tapered strawberry toppings: chocolate,
cheesecake, vanilla/cream and muted pink assortments. Its larger cream cake
anchors the middle level. The pastry display has rounded chocolate éclairs,
custard tarts with red/purple fruit, a cream profiterole, folded diamond pastries
and a longer cream-filled pastry on top. Products rest directly on their shelves
with separated footprints and clear space above them.

The source has 36 shared case cuboids/planes and 108/119 dessert cuboids for
cake/pastry respectively. Broad rounded bevel pieces replace dense stair-step
geometry. Hidden fully covered faces and cut-plane faces are removed; elements
with no faces are never exported.

## Reliable static glass

Glass uses **binary-alpha cutout**, following the existing counter/oven approach.
More than 99% of the glass texture is clear; seven opaque pixels provide sparse
highlights. Each physical panel is a single zero-thickness double-sided plane,
so there are no front/back transparent surfaces at slightly different depths.
Glass has no `cullface` entries. The three panels meet opaque framing rather than
overlapping each other. The front pane is split exactly across the four sections,
with continuous outward-facing UVs and no duplicated area at the boundaries.

All exported block/item models declare `minecraft:cutout` for Forge/NeoForge.
The existing Architectury client registration adds both new blocks to the cutout
layer, including Fabric. No old block's render layer changes.

This avoids translucent depth-sorting problems across blocks/items, at the cost
of physically tinted glass, partial opacity and reflections. Clear pixels reveal
the real static geometry inside. Lighting, ambient occlusion, mipmapping and
seams under world lighting still need human review inside Minecraft. Previews
show asset geometry, not a simulation of the game's complete rendering pipeline.

## Four-part behavior and shapes

`RefrigeratedDisplayBlock` subclasses the existing `WideTallDecorationBlock`.
The only shared-class modification removes its `final` modifier; all placement,
offset, mirror, linked removal and codec behavior remains unchanged for the oven.
The new subclass uses a fixed CAKE/PASTRY constructor variant and fitted shapes.

The four saved `part=0..3` values follow the existing layout: origin is lower
part 0, part 1 is clockwise from facing, and parts 2/3 are above them. Four
horizontal orientations are supported. The shared system checks every occupied
cell for replaceability, bounds, loaded chunks and entity obstruction. It
rechecks server placement, rolls back only matching writes and removes linked
matching states with a recursion guard and no second loot invocation.
Normal Survival breaking of any section should drop one matching display;
direct Creative breaking should drop none. These world interactions need manual
verification. No existing registry ID or saved blockstate schema changes.

Both sealed cases share 16 cached part/facing shapes. The housing and glass
enclosure have filled collision envelopes, with separate protruding rear handles.
Players cannot enter the case interior. The outline approximates the physical
enclosure rather than individual dessert surfaces or foot gaps. Four Minecraft
cells are exclusive even where the model is visually empty. The inherited system
does not introduce gravity or require a separate sturdy-floor survival rule.

## Editable source and generated files

Run `python tools/generate_refrigerated_displays.py` to regenerate only this family.
Named case elements, shelf heights, product constructors and compositions are
separate functions. The exporter crops geometry and UVs using Minecraft's
outward face conventions. Y-rotated products may cross the horizontal boundary;
X/Z-rotated pieces must stay inside one section, checked during export.

Generated assets comprise two multipart blockstates, four shared case models,
eight dessert component models, two combined inventory models, fourteen 32 × 32
textures, two loot tables and `RefrigeratedDisplayShapes.java`. Existing oven
materials are referenced read-only. Both states use the exact same case models
and both inventories include identical case elements. Existing build rules adapt
the source loot-table directory for Minecraft 1.21.1.

Stage 1 tray racks and all previous decoration assets/behavior remain unchanged.
Only registry/Creative/language entries, client cutout registration and the
shared class's extensibility change in existing source files.

## Verification

All four builds passed: Forge/Fabric 1.20.1 and NeoForge/Fabric 1.21.1. Packaged
JAR verification checks new classes and Java versions, loader metadata, every
model, blockstate, referenced texture, translation and version-appropriate loot.
Six asset regressions cover exact variants, shared case/multipart coverage,
binary-alpha single-plane glass, legal models, local bounds, UV clipping,
dessert contact/spacing, item models and ordinary loot. Existing decoration
files are compared with the pre-edit snapshot, including the approved tray racks.

Development-client resource checks inspect all 32 block states and both inventory
models per target. They check baked geometry for missing models/sprites and
inspect fresh client logs; results are saved in ignored
`build/refrigerated-display-verification`. These checks do not enter a world.
All four clients launched, passed their 34-model checks and closed successfully.
Fresh logs contain no missing-model, missing-texture or model-loading errors.
Existing development-loader resource-URL warnings and the vanilla shader's
`Sampler2` warning are unrelated to these static cutout models.
No in-world placement, breaking, collision, drops or persistence have been tested.

## Previews and manual review

Use `python tools/preview_bakery_model.py refrigerated_cake_display --views --rear`
and the same command for `refrigerated_pastry_display`. Individual front, side,
rear/isometric previews and `refrigerated_displays_family.png` are in ignored
`build/asset-previews`.

Manual checks on all four targets:

1. Inspect both Creative icons and place all four orientations on a clear 2 × 2
   footprint. Compare the shared case, dessert readability, frosting/toppings,
   shelf contact and rear handles from front, side and rear viewpoints.
2. Check transparency under bright/dim lighting and at several viewing distances;
   inspect all four section boundaries for seams or flickering. Reload resources
   and review the client log for resource/rendering errors.
3. Obstruct each of the four cells in turn and check that placement fails without
   consuming an item or replacing the obstruction. Check entity obstruction too.
4. Break each of the four sections in Survival and Creative. Expect full removal,
   exactly one matching display item in Survival and no direct Creative drop.
   Ensure unrelated neighbors remain and supported blocks above removed parts
   receive normal updates.
5. Check the closed enclosure's collision and outline, save/reload every rotation,
   and repeat the section-breaking checks. These behaviors remain unverified.

Stage 2 stops here for review. Wall tools and the bread slicer require later approval.
