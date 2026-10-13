# Bakery expansion — Stage 3: Wall Tools

Exactly three new static, single-cell decorations:

| Registry ID (`craftdistrict:`) | Arrangement | Maximum depth from wall |
| --- | --- | --- |
| `bakery_bread_peel_rack` | Two broad wooden paddles, long handles, upper clamps and lower rests | 3.125/16 block |
| `bakery_utensil_wall_rack` | Bench scraper, pastry spatula, dough wheel cutter and small palette knife on four hooks | 3.75/16 block |
| `bakery_rolling_pin_rack` | Three differently sized horizontal rolling pins on wooden cradles | 4.625/16 block |

All fit within 1 × 1 × 1 block positions, have four horizontal facings and appear
in the CraftDistrict Creative tab. None has a GUI, inventory, Block Entity,
animation, entity renderer, ticking or tool interaction.

## Geometry and proportions

The peel rack uses compact short-handled traditional peels. Its two flat wooden
heads have broad octagonal outlines, different widths/heights and narrow necks.
The handles extend considerably farther than the paddles and vary in length.
The complete tool group is under one block tall: suitable for compact wall décor,
not a scale model of a several-metre oven peel. This avoids introducing a new
multi-block wall-mounting system; a full-length variant would need separate
design/placement approval later. Both mounting rails reach the wall, while
metal clamps/rest pads bridge to the handles/paddle backs.

The utensil rack has a steel bar, end brackets and four dark hooks. Its broad
bench scraper has a rounded wooden cross-grip and clipped blade corners; the
long spatula has a flared blade and narrower neck; the dough cutter has an
octagonal wheel, fork and central axle; the palette knife has a narrow tapered
blade and shorter grip. Tool tops meet their hooks. Warm handles and pale working
steel distinguish the tools without a dense cluster of tiny rods.

The rolling pins use six broad bevelled cuboids per barrel, producing octagonal
cross-sections with real angled faces. Each has two smaller end handles, one of
three wood tones, and a distinct barrel length/diameter. Paired projecting
cradles with small front lips support them from the wooden mounting frame.

Models have 22/37/40 visible cuboids and 126/213/224 faces respectively. The
generator omits hidden covered faces and rejects empty-face elements. Four new
opaque 16 × 16 textures supply three wood tones and working steel; existing
bakery trim and industrial metals are referenced without regenerating them.
Front/side/back UVs follow Minecraft's outward-face convention, with projected
UVs across curved sections. There are no letters or custom logos to mirror.

## Reused mounting architecture

`BakeryWallToolBlock` extends `BakerySignageBlock`. The only signage source change
removes its `final` modifier to permit reuse. Its methods, existing enum, codecs,
registrations, shapes, models and textures are unchanged. The subclass selects
the established wall-only `WALL_SHOP` mounting branch through its constructor,
then supplies its own fixed Tool enum, codec and shapes. This avoids duplicating
placement, survival or neighbor-removal methods. The inherited collision method
dispatches to the subclass's shape as well.

Wall attachment uses the same method signatures in Minecraft 1.20.1 and 1.21.1.
No version-specific attachment implementation was needed. The existing Stonecutter
conditional adds the block codec for 1.21; older versions use the same wall rules.

Click a **sturdy vertical block face** to place a rack into the adjacent cell.
The clicked face determines its outward direction. For a north-facing model,
the supporting block is south of it and the mount reaches model z=16. Top and
bottom clicks are rejected. Normal BlockItem placement checks replaceability
and entity obstruction. The relevant support neighbor is checked when updated;
if its face stops being sturdy, ordinary block propagation removes the rack.

Direct Survival breaking uses the ordinary one-item loot table. Direct Creative
breaking drops nothing. Removing the support in Creative may drop the rack,
following the signage pack's existing vanilla support-loss behavior. No special
drop handler, recursion or multi-block removal is involved for these single-cell
blocks. These interactions still need manual world verification.

Shapes are cached for all 12 tool/facing combinations. Curved paddles, wheels
and barrels use conservative envelopes, with separate narrow handles and supports.
Collision and outline are identical, remain thin and rotate with the mounting
face. Selection therefore approximates chamfered corners instead of tracing
every rendered bevel. Each decoration occupies one whole Minecraft cell; another
independent block cannot share that cell even where the rack looks open.

## Editable sources and resources

Run `python tools/generate_bakery_wall_tools.py` to regenerate only this family.
Its named `bread_peels`, `utensils` and `rolling_pins` layouts define both model
parts and collision groups. Generated outputs comprise three blockstates,
three block models, three centered inventory models, four textures, three loot
tables and `BakeryWallToolShapes.java`. The existing build adapts loot-table
directories for 1.21.1. Model generation uses Python's standard library; previews
reuse the established Pillow/NumPy renderer.

## Verification

All four builds passed: Forge/Fabric 1.20.1 and NeoForge/Fabric 1.21.1. Packaged
JAR checks cover loader metadata, Java versions, new classes, models, blockstates,
referenced textures, translations and version-appropriate loot tables. Six asset
regressions verify the exact three IDs, inherited mounting rules, model contracts,
thin profiles, shape rotation, tool/support connections, barrel silhouettes,
outward UVs, texture paths and opaque texture pixels.

Previous decoration files, including the approved refrigerated displays and tray
racks, match the pre-edit snapshot. The shared signage source differs only in
its extensibility, verified against that snapshot. No previous design changes.

Development-client checks inspect the 12 facing models and three inventory models
per target for baked geometry and missing sprites. They do not enter a world;
reports/logs are in ignored `build/wall-tool-verification`. Actual placement,
support removal, collision, drops and save/reload behavior have not been tested
inside a running world.

All four development clients launched, passed all 15 model checks and closed
successfully. Fresh logs contain no missing-model, missing-texture or model-loading
errors. Existing loader resource-URL and vanilla shader `Sampler2` warnings remain;
no custom tool renderer or transparency layer is used by this family.

## Previews and manual checks

Run `python tools/preview_bakery_model.py <registry_id_without_namespace> --views --rear`
for individual isometric/front/side/rear assets. The combined front/side/isometric
sheet is `build/asset-previews/bakery_wall_tools_family.png`. These previews are
renders of exported JSON geometry, not Minecraft screenshots.

In a disposable world on each target:

1. Find all three racks in CraftDistrict. Place each against all four sides of
   a solid wall. Check outward facing, mounting contact, normal-distance tool
   readability and the thin profile from oblique angles.
2. Confirm top/bottom clicks and incomplete supports are rejected. Check occupied
   destination cells and entity obstruction; unrelated blocks must remain intact.
3. Break each rack in Survival and Creative and check expected drops. Remove its
   supporting wall or change the support's shape; check removal and one support-loss
   drop. Confirm neighboring decorations do not disappear unexpectedly.
4. Inspect the curved-tool selection/collision envelopes, texture filtering and
   shading. Reload resources, inspect client logs and save/reload every facing.
   Repeat attachment/removal after reload. These world checks remain outstanding.

Stage 3 stops here for review; Stage 4 requires explicit approval.
