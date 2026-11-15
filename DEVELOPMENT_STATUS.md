# Fossil Finder Owl 3.0 development snapshot

This is an in-progress source update based on the user's uploaded Forge 2.1.0 project and Fossils and Archaeology Revival 1.20.1-9.3.4.0 dependency.

## Changes in this snapshot
- Added the exact Fossils and Archaeology Revival 9.3.4.0 and GeckoLib 4.8.4 compile-time JARs under `libs/`.
- Restored `pack.mcmeta`, the biome modifier, and the owl biome tag from the last known working 2.1.0 JAR.
- Reworked server-side movement arbitration so danger avoidance, fossil investigation, and owner following do not each issue competing navigation commands.
- Switched the owl to flying navigation and a flying move controller; tamed, active owls use no-gravity movement and flee upward/away from nearby hostile mobs.
- Added hostile detection, frantic alarm hoots, and parrot-style imitation for zombie, skeleton, creeper, and spider sounds.
- Added basic torch/lantern item acceptance and persistence, plus owner interaction catchphrases.
- Corrected the fossil block registry IDs to the names used by the supplied Fossils and Archaeology Revival 9.3.4.0 JAR (`fossil_stone`, `fossil_deepslate`, etc.).
- Registered the existing owl hoot audio through `sounds.json` and moved the OGG into the expected `sounds/` folder.
- Added the supplied low-poly Blathers Collada assets, but switched the active renderer back to the stable block geometry in `geo/owl.geo.json` after the skinned mesh rendered badly in-game. The existing head and hand attachments remain available on the active model.
- Replaced the placeholder cargo panel with a fossil-textured GUI, 15 visible cargo slots, a functional pickaxe slot, player inventory/hotbar, and an owl preview. Cargo saved in the previous 27-slot layout is retained and queued into the new slots as capacity opens.

## Not complete / not verified
- This is **not a compiled or game-tested JAR**. The execution environment did not have Gradle installed and could not reach the Gradle distribution host, so a ForgeGradle build could not be run here.
- Torch/lantern attachment rendering and dynamic world lighting are not implemented. The entity currently accepts and saves the item, and explicitly tells the player this is visual/light functionality pending.
- The active owl model's animation and attachment positions still need in-game testing.
- Fossil mining, pickup, and the new cargo GUI still need in-game validation. Ore drops use the owner as the loot-context entity and enter a shared, persisted 15-slot cargo container; overflow from previous 27-slot saves is preserved.
- Existing saves should be backed up before testing any development build.

## Build
Use Java 17 and run `gradle build` from this project directory in an environment with access to ForgeGradle/Maven repositories. The target Forge dependency is currently `1.20.1-47.2.0`; adjust only if the user's Modrinth instance uses a different Forge 47.x build.
