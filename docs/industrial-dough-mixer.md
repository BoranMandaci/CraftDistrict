# Industrial Dough Mixer

Registry ID: `craftdistrict:industrial_dough_mixer`. A static 1 × 2 × 1 decoration,
registered in the Bakery collection and CraftDistrict Creative tab.

## Editable geometry

`tools/generate_industrial_dough_mixer.py` owns the complete model, new textures,
two-part blockstates, inventory model, loot table and generated collision shapes.
Run it with Python 3, then use `tools/preview_bakery_model.py
industrial_dough_mixer --views --rear --top` with Pillow and NumPy for previews.
Only mixer resources and `IndustrialDoughMixerShapes.java` are generated.
The existing oven's steel, charcoal, vents and maintenance textures are referenced
without rewriting them. Six new 32 × 32 opaque pixel-art textures provide the bowl,
controls, dough and buttons.

The machine faces north in model space. It has four rubber feet, a heavy base,
a rear motor column, a lift pedestal and cradle, an overhanging motor housing,
side/rear vents and a framed rear service panel. Three expanding octagonal bowl
wall bands, two interior ledges and a narrow rolled rim enclose a genuine hollow
interior above an octagonal bottom. A small static dough mound sits at its bottom.
A seven-piece hook includes a diagonal upper bend and an open descending curl.
The sparse guard uses an octagonal rail, two posts and two crossbars.
The front controls have start/stop symbols, an upright `02` indicator, a speed dial
and arrow, and a protruding red emergency stop. All controls are visual only.

The source contains 105 cuboids and 542 textured faces after hidden-face removal.
Clipping at y=16 gives 56 lower and 62 upper cuboids. Crossing geometry uses
cropped UVs and no duplicated faces on the cut plane. The complete inventory
model is centered vertically and uses reduced display scales.

## Placement and collision

`IndustrialDoughMixerBlock` reuses `MultiBlockDecorationBlock`, with its extension
direction set to UP. The lower cell is `primary=true`; the upper is
`primary=false`. Both have the existing horizontal-facing property. No existing
block, saved state schema or shared placement/removal code changes.

The shared system checks both cells for replacement, world/build bounds, loaded
chunks and entity obstruction before placement, and rechecks partner placement
on the server. Linked removal matches the exact block and facing, guards against
recursive cleanup, and removes the partner without another loot invocation.
The same single-item loot table applies when either part is broken normally;
Creative breaking follows vanilla no-drop behavior.

Outline and collision shapes are clipped per cell and cached for all eight states.
They approximate the base, column, pedestal, bowl, head, controls and lift handle.
The bowl envelope is deliberately filled for collision, and the sparse guard and
small hook are not represented by dozens of tiny boxes. Thin render details may
therefore differ slightly from the selection outline. Floor-standing describes
the model: the inherited system does not add gravity or require a full sturdy
floor, and removing the floor does not automatically remove the mixer.

## Verification

All four Gradle builds passed: Forge/Fabric 1.20.1 and NeoForge/Fabric 1.21.1.
Five asset regression tests cover Minecraft model constraints, resource paths,
eight states, loot, a hollow bowl, closed ledges, readable control UVs, and
geometry/UV continuity at the block seam. Packaged JAR checks include loader
metadata, mixer classes, models, blockstates, all referenced textures, translation
and the version-appropriate loot-table directory.

Previews are deterministic renders of exported assets, not Minecraft screenshots.
No client or running-world interaction was tested for this addition.

### Manual world checks

1. Launch the desired target with `./gradlew.bat :forge:1.20.1:runClient`,
   `:fabric:1.20.1:runClient`, `:neoforge:1.21.1:runClient`, or
   `:fabric:1.21.1:runClient`, then enter a disposable Creative world.
2. Find Industrial Dough Mixer in CraftDistrict, or use
   `/give @s craftdistrict:industrial_dough_mixer`. Inspect the inventory icon,
   front controls, bowl interior, hook, guard, sides and rear at normal distance.
3. Place all four orientations and inspect the y=16 seam under game lighting.
   Reload resources (F3+T) and check the client log for model/texture errors.
4. Occupy either cell with a solid block and confirm placement fails without
   consuming an item or replacing the obstruction. Check entity obstruction too.
5. In Survival, break the lower part and then another mixer's upper part: expect
   both cells cleared and exactly one mixer item each time. Repeat in Creative:
   expect both cells cleared and no mixer drop. Check neighboring blocks remain.
6. Walk against the base, column and bowl from all directions; check outline and
   collision acceptability. Save/reload with all four orientations present and
   repeat breaking. These checks remain unperformed inside a running world.
