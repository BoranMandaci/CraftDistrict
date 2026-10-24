**English** | [Türkçe](README.tr.md)

# CraftDistrict

**Detailed decorations for the places that make a Minecraft city.**

**[Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/craftdistrict)**

![Minecraft 1.20.1 and 1.21.1](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1-4d7b31?style=flat-square)
![Fabric](https://img.shields.io/badge/Loader-Fabric-dbd0b4?style=flat-square)
![Forge](https://img.shields.io/badge/Loader-Forge-34495e?style=flat-square)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-d3712c?style=flat-square)

[Collections](#decoration-collections) · [Installation](#installation) · [Compatibility](#supported-versions-and-dependencies) · [Languages](#supported-languages)

**CraftDistrict 1.1 — Restaurant Update is now available on CurseForge.**

## About CraftDistrict

CraftDistrict is a decoration-focused Minecraft mod for everyday spaces, shops, workplaces, public buildings, and restaurants. Build a welcoming bakery, a busy supermarket, a fully furnished police or fire station, or a complete restaurant with detailed furniture, equipment, appliances, signs, and architectural props.

The collections share a Minecraft-friendly voxel style, while a reusable lighting collection helps tie different buildings together.

More decorations and new themed collections are planned for future updates.

## Features

- **Six themed collections:** Bakery, Police Station, Fire Station, Supermarket, Lighting, and Restaurant.
- **Details beyond furniture:** industrial equipment, storage, display racks, tools, signage, and small countertop props.
- **Flexible placement:** horizontal rotation, multi-block structures, and wall or ceiling mounts on supported decorations.
- **Connected layouts:** modular dining tables, booths, benches, prep tables, and connectable kitchen ventilation ducts.
- **Furniture interactions:** sit on supported seating and open selected doors, cabinets, and lids.
- **Real block lighting:** lighting fixtures emit Minecraft light, including wall lights, ceiling fixtures, and illuminated signs.
- **Performance-conscious design:** static decorative models, simplified collision shapes, and lightweight interactions.
- **Survival support:** material-appropriate mining tags let suitable axes and pickaxes speed up mining on tagged blocks.
- **Organized browsing:** one custom Creative tab with six collection filters and shared items where they make sense.
- **Nine languages:** localized names and interface text, with the full language list below.

**Decoration comes first.** Most machinery, appliances, and display props do not provide inventories, storage, cooking, or production systems. Players can sit on supported furniture and sleep in the functional Holding Cell Bed. Selected doors and cabinets open and close, as does the kitchen trash bin lid. Lighting fixtures emit block light.

Version 1.1 also improves decorative geometry, block shapes, and packaged resources. Release JARs are approximately **13–14% smaller** than the pre-optimization v1.1 builds.

## Decoration collections

### Bakery

Warm wooden displays meet commercial bakery equipment. Furnish the customer area with bread shelves, pastry trays, cash registers, and shop signs, then complete the preparation room with dough tables, flour sacks, tray racks, industrial ovens, mixers, and bread slicers. Refrigerated cake and pastry displays add a different showcase style.

### Police Station

Create everything from a public reception area to investigation, booking, and detention rooms. The collection includes reception desks, dispatch consoles, waiting benches, office chairs, evidence storage, investigation boards, interrogation furniture, fingerprint equipment, security cameras, holding-cell fittings, and decorative pistol and rifle racks.

### Fire Station

Equip the station with firefighter gear lockers, breathing apparatus racks, extinguishers, hose reels and storage, rescue tools, stretchers, and a station pole. Dispatch desks, benches, incident boards, clocks, and first-aid cabinets complete the working interior.

### Supermarket

Build varied grocery aisles with pantry, canned, snack, and candy shelves, plus specialized cleaning, personal care, pet supplies, paper products, household, and stationery shelving. Add double-sided gondolas, hanging merchandise displays, fruit and vegetable stands, refrigerated cabinets, beverage coolers, freezers, deli counters, checkouts, self-checkout kiosks, carts, and basket stacks.

### Universal Lighting

A general-purpose lighting family for shops, workplaces, hallways, and outdoor spaces:

- LED ceiling panels, fluorescent tubes, industrial pendants, recessed spotlights, and track lights.
- Modern wall lights, industrial floodlights, security lights, desk lamps, outdoor bollards, and under-cabinet strips.
- Illuminated EXIT signs with left/right arrows and neon OPEN signs, including supported hanging mounts.

### Restaurant

Furnish dining rooms with regular and modular connecting tables, sittable chairs, bar stools, booths, and benches. Service counters, POS terminals, and serving equipment complete the customer area. Build a professional kitchen with decorative commercial appliances, food decorations, prep stations, storage furniture, cookware, and accessories. Modular ventilation ducts complete the kitchen line; refrigerator and pantry cabinet doors open to reveal stocked interiors.

## Creative inventory

Open the **CraftDistrict** Creative tab and use the collection buttons around the inventory to choose **Bakery**, **Police Station**, **Fire Station**, **Supermarket**, **Lighting**, or **Restaurant**.

All six collections live inside one Minecraft Creative tab. Switching collections filters the displayed decorations; general-purpose items such as office chairs, clocks, and fire extinguishers appear in multiple relevant collections. Restaurant also shares the existing Cash Register and Wall Clock.

## Supported versions and dependencies

Choose the exact Minecraft version **and** loader combination below. The versions listed reflect the current project configuration and tested setup.

| Minecraft | Mod loader | Java | Required mod dependencies |
|---|---|---|---|
| 1.20.1 | Forge 47.4.26 | 17 | Architectury API 9.2.14 |
| 1.20.1 | Fabric Loader 0.19.3 | 17 | Architectury API 9.2.14 + Fabric API 0.92.11+1.20.1 |
| 1.21.1 | NeoForge 21.1.219 | 21 | Architectury API 13.0.8 |
| 1.21.1 | Fabric Loader 0.19.3 | 21 | Architectury API 13.0.8 + Fabric API 0.116.6+1.21.1 |

**Architectury API is required on every target, on both clients and dedicated servers. Fabric API is additionally required on Fabric.** Install the Architectury API build and other dependencies matching your Minecraft version and loader.

Forge 1.21.1 and NeoForge 1.20.1 are not supported targets. Architectury's build tooling, Stonecutter, and Gradle are development tools; players do not install them as mods.

## Installation

1. Choose a supported Minecraft version and install its matching loader from the table above.
2. Make sure the profile uses Java 17 for Minecraft 1.20.1 or Java 21 for Minecraft 1.21.1.
3. Download the matching Architectury API file, plus Fabric API if using Fabric.
4. Download the CraftDistrict release JAR for your exact Minecraft version and loader from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/craftdistrict).
5. Put CraftDistrict and its required dependency JARs into that Minecraft instance's `mods` folder. Install only one CraftDistrict target JAR per instance.
6. Launch Minecraft with the matching loader profile. In Creative mode, look for the **CraftDistrict** tab.

## Multiplayer and dedicated servers

CraftDistrict supports dedicated servers on all four listed targets. Production server startup and representative world save/restart checks have been verified for each configuration.

Install the matching CraftDistrict JAR and required dependencies on **both the server and joining clients**. Keep their Minecraft versions, loaders, and mod versions compatible.

Creative collection navigation is a client-side interface; the registered decorations remain server-compatible.

## Supported languages

| Language | Locale |
|---|---|
| English | `en_us` |
| Turkish | `tr_tr` |
| Spanish (Spain) | `es_es` |
| Portuguese (Brazil) | `pt_br` |
| German | `de_de` |
| French | `fr_fr` |
| Russian | `ru_ru` |
| Simplified Chinese | `zh_cn` |
| Japanese | `ja_jp` |

Turkish translations have received manual review and corrections. Minecraft's selected language determines the in-game localization.

## Downloads

Download **CraftDistrict 1.1 — Restaurant Update** from the official [CurseForge project page](https://www.curseforge.com/minecraft/mc-mods/craftdistrict).

Choose files using the compatibility table above. A Fabric, Forge, or NeoForge JAR is specific to its target and is not interchangeable with the others.

## Bug reports

Use this repository's Issues section when it is available. Include:

- Minecraft, loader, CraftDistrict, and dependency versions.
- Whether the problem occurs in singleplayer, on a dedicated server, or both.
- Clear reproduction steps, affected blocks, and expected versus actual behavior.
- Relevant logs or a crash report, with private information removed.

## License

Copyright (c) 2026 Boran Mandacı

CraftDistrict's original source code, 3D models, textures, and other original project assets are distributed under the **MIT License (`MIT`)**, except where otherwise noted. See [LICENSE](LICENSE) for the complete license text.

Third-party components retain their own licenses and notices.
