# Police Station Collection — Stage 1: Public Entrance

Only these three IDs are implemented:

- `craftdistrict:police_reception_desk`
- `craftdistrict:police_department_sign`
- `craftdistrict:police_waiting_area_bench`

All are static two-cell decorations, with four horizontal facings and saved
`primary=true/false` states. Each cell has its own block model and fitted shape.
All existing IDs are retained. No seating mechanics, opening drawers, functional
computer, GUI, inventory, Block Entity, ticking, animation or entity renderer.
Later Police stages are not implemented and require explicit approval.

## Visual design

North (-Z) is the public/front face. Geometry spans X=0..32, Y=0..16, Z=0..16.
The family uses navy, muted blue, gray, white and brushed metal. The insignia is
an original generic shield; no real department branding is used. Monitor colors
are static pixels with no emitted/dynamic light. Textures are opaque, requiring
no new render-layer configuration.

| Decoration | Geometry | Placement footprint |
| --- | --- | --- |
| Reception desk | 44 visible cuboids; 25/29 per half | 2 wide × 1 tall × 1 deep |
| Department sign | 7 visible cuboids; 6/6 per half | 2 wide × 1 tall, wall-mounted |
| Waiting bench | 29 visible cuboids; 17/17 per half | 2 wide × 1 tall × 1 deep |

The reception desk has a navy front with a shield, light surface and metal edge,
recessed plinth, two rear drawer pedestals with handles and open staff knee space.
The public counter ledge is at Y=16, raised from the original 11.5 units (about
39% higher), with navy frontage extended to Y=15 and short tall side returns.
A lower staff work surface remains at Y=11.5 behind it, keeping all props inside
the existing one-block height and two-cell footprint. This stepped arrangement
reads as a reception counter while avoiding props projecting into unreserved
block cells. The monitor screen faces staff behind the counter. Its wider
8.5×3.5-unit housing has a metal lower bezel, broader layered foot, a thicker
stand and a generic shield/status-and-case-list UI. The keyboard is wider and
slightly thicker. The compact phone has a handset down the left side with two
receiver ends and a narrower raised grip; the right-hand keypad/display is
unobstructed, with a raised rear section and subtle front trim. Paperwork and
pen cup retain their positions; the front shield is recentered on the taller
frontage. This is not an empty mounting surface for separate blocks.

The department sign has metal backing flush with its supporting wall at Z=16,
light framing, navy inset, generic shield and readable pixel-font `POLICE`.
Its depth is only 1.375 model units. The asymmetric lettering is mapped solely
to the outward north face; clipping preserves continuous U across the center
seam. It reserves two ordinary block cells even though the mesh is thin.

The bench has three separated muted-blue seat cushions and tilted backrests,
a connected metal beam, two pedestal legs and floor feet, and four padded
armrests. Both rear supports now end flush with the back crossbar at Y=8.125,
instead of projecting up to Y=10; the two awkward upward stubs are removed.
Under-seat space stays open. Backrests use legal 22.5-degree X rotations;
X clipping does not change those rotations. Collision conservatively encloses
the tilted backrests, rather than exactly following their sloped mesh. The bench
is decorative and cannot be used to sit down.

## Shared behavior

`PoliceEntranceBlock` extends the existing `MultiBlockDecorationBlock` and
uses its placement validation, server-side placement rechecks, rollback and
matching-part removal. Width extends clockwise from the facing, exactly like
the existing wide Bakery decorations. A new protected `getPlacementFacing`
hook keeps the default furniture orientation and lets the wall sign use the
horizontal supporting face that was clicked. Floor/ceiling clicks are rejected
for the sign.

The shared placement checks now call each part's `canSurvive`, including a
server-side primary recheck before writing the partner. Existing Bakery
multi-block types retain their default survival behavior and facing; no Bakery
model or decoration-specific implementation is changed.

New furniture needs an upward sturdy surface below **both** occupied cells.
The wall sign needs sturdy vertical faces behind **both** cells. Each part
responds to losing its support by returning air through vanilla neighbor-shape
propagation; the existing linked-removal path clears its matching partner.

The old global boolean removal guard could suppress cleanup of a *different*
decoration losing support during a linked removal. It is now scoped to the
level, block and structure origin. Same-structure callbacks remain guarded;
dependent structures can clean up independently. The transient thread-local set
is released after the final callback. Matching-state checks, update flags and
no-second-loot partner removal are retained. This support cascade still needs
manual world verification.

Both destination cells must be replaceable, inside placement bounds and free
of entity obstruction. The server rechecks before writes. Breaking either half
uses the same one-item loot definition; partner removal never invokes a second
loot roll. No unrelated replacement or adjacent block is cleared. Support-loss
removal and Survival/Creative drop behavior require manual testing.

`PoliceEntranceShapes.java` is generated from the named geometry and caches 24
part/facing shapes. Queries do not rebuild shapes. The sign's outline/collision
stays thin; desk and bench shapes retain openings. Rounded or tilted surfaces
use conservative cuboid envelopes.

## Editing and previews

`tools/generate_police_entrance.py` contains the named model components, original
pixel-art textures, clipping and generated shape data. Shared validation helpers
and the existing JSON preview renderer are imported without running Bakery
generators. Only this family's resources and its shape class are written.

```powershell
python tools/generate_police_entrance.py --previews
python tools/test_generate_police_entrance.py
```

Eleven textures are 16×16; the sign's single lettering strip is 64×16. UI,
paperwork, keys and lettering are restricted to their intended outward faces.
Fully hidden elements and cut faces at the seam are removed. Six block models,
three complete item models, three blockstates and three loot tables are exported.

Front, side, rear and isometric previews for each decoration, individual four-view
sheets and `police_entrance_family.png` are under ignored `build/asset-previews/`.
These render exported model geometry; they are not Minecraft screenshots and do
not reproduce the full game lighting/ambient occlusion pipeline.

## Manual review

Launch the desired target from the repository root:

```powershell
.\gradlew.bat :forge:1.20.1:runClient
.\gradlew.bat :fabric:1.20.1:runClient
.\gradlew.bat :neoforge:1.21.1:runClient
.\gradlew.bat :fabric:1.21.1:runClient
```

In a disposable test world:

1. Find all three blocks at the end of the CraftDistrict Creative tab.
2. Place desk and bench on two solid supporting blocks, in all four orientations.
   The partner extends clockwise from the facing; check both destination cells.
3. Mount the sign by clicking a vertical wall face with two solid wall blocks
   behind the intended footprint. Check all four walls, lettering direction and
   center seam. It should reject floor/ceiling clicks or either missing support.
4. Obstruct either cell and attempt placement; also test entity obstruction and
   the world/build limits where practical. No unrelated block should be replaced.
5. Inspect outlines, collision, tilted backrests, phone and staff-facing screen.
6. Break either half in Survival (one item) and Creative (no duplicate drop).
7. Remove either floor/wall support and confirm both parts disappear with one
   normal support-loss item drop. Also mount a sign on a sturdy two-part Bakery
   counter face and break the counter's other half: the complete sign should
   disappear without an orphan or duplicate drop. Recheck unaffected Bakery
   linked placement/removal and existing decorations supported above counters.
8. Save/reload; check states, rotations, appearance and linked removal afterward.

Static checks and client model baking do not verify these world interactions.

## Files added or modified

New files:

- `src/main/java/com/craftdistrict/block/PoliceEntranceBlock.java`
- `src/main/java/com/craftdistrict/block/PoliceEntranceShapes.java`
- Three `police_*` blockstates, six block models in `models/block/police/`,
  three item models and three one-item loot tables under the shared resources.
- Twelve `entrance_*` PNGs in `textures/block/police/`.
- `tools/generate_police_entrance.py`
- `tools/test_generate_police_entrance.py`
- `docs/police-entrance-set.md`

Existing files changed:

- `MultiBlockDecorationBlock.java` — facing hook, part support checks and scoped
  removal guard necessary for supported two-cell furniture/signs.
- `CraftDistrictBlocks.java` — new nested Police collection and three block/items.
- `CraftDistrictCreativeTabs.java` — three entries after the Bakery collection.
- `assets/craftdistrict/lang/en_us.json` — three translations.

No build plugin, platform matrix, previous ID or Bakery visual asset is changed.
Build logs, temporary read-only model probe and verification reports are isolated
in ignored `build/police-entrance-verification/`, never packaged into mod JARs.
All changes remain uncommitted on the existing branch. Stop after Stage 1.

## Initial Stage 1 verification

- All six static asset/geometry checks passed, including text direction,
  seam clipping, both halves, collision rotation and exported loot/models.
- All four final builds passed: Forge/Fabric 1.20.1 and NeoForge/Fabric 1.21.1.
  An initial collision-array generator brace error was corrected in the source
  generator before these final builds.
- All four distributable JARs contain the expected mod metadata, byte-identical
  Police assets/textures, three translations, the correct plural/singular loot
  directory and Java 17/21 bytecode. The scoped removal-key class is present;
  temporary probe classes are absent.
- All 109 project blockstate/block/item JSON files have resolving local model
  and texture references and no empty element face maps.
- All four final development clients launched, baked all 24 part/facing models
  and three inventory models without missing model/sprite errors, and stopped
  normally. Client logs were checked for model-loading and missing-texture errors.
- Existing Java API/Gradle deprecation and vanilla/development warnings remain;
  they did not block builds or resource loads.
- All pre-existing Bakery assets, generators and decoration-specific classes
  match their pre-Stage-1 hashes. Only the four documented existing files changed
  during this stage. There are 32 new project files, plus ignored previews/reports.
- No world was opened. Placement, support cascades, Survival/Creative drops,
  entity obstruction, collision and saved-world behavior remain manual tests.

## Visual-review revision

- Bench: the two rear support columns now stop at Y=8.125 rather than Y=10.
  No other bench geometry changed. Three seats, four armrests, metal frame,
  colors, symmetry and footprint are retained; collision loses the same stubs.
- Desk: public ledge raised from Y=11.5 to Y=16 (+4.5 units, approximately 39%).
  Navy front extends to Y=15 with tall side returns and a recentered shield.
  The rear staff surface stays at Y=11.5; cabinet drawers remain integrated.
- Computer: housing enlarged from 7×3 to 8.5×3.5 units, with a metal lower bezel,
  broader layered foot, thicker stand, wider keyboard and updated symbolic UI.
  The UI is mapped only to the outward staff-facing south face with increasing U;
  all props remain within the existing one-block height.
- Phone: compact stepped body, handset down one side with distinct receivers
  and a raised narrower grip, exposed keypad/display on the other side, and
  subtle metal front trim. Its texture is mapped only to the upward control face.
- Desk and bench block/item models and cached shapes are regenerated. Only
  `entrance_monitor.png` and `entrance_phone.png` textures changed. Department
  sign geometry, shape data and all resources remain identical.
- Exactly 12 existing files changed: six desk/bench block and item models,
  two textures, `PoliceEntranceShapes.java`, generator, tests and these notes.
  No registration, blockstate, loot, translation, block behavior, Bakery asset,
  registry ID or platform configuration changed.
- All seven static checks and all four builds passed. All four distributable
  JARs contain the revised assets, expected metadata and Java 17/21 bytecode.
- Front, side, staff/rear and isometric previews were regenerated. These are
  deterministic asset renders, not in-game screenshots. Client resource loading
  and in-world tests were not repeated for this visual revision. Review the
  taller counter and new prop readability under actual Minecraft lighting,
  and confirm selection/collision against the revised geometry.
- Everything remains uncommitted. Stop for model review; no next stage begins.
