# Fossil Finder Owl 3.0 development snapshot

This is an in-progress source update based on the user's uploaded Forge 2.1.0 project and Fossils and Archaeology Revival 1.20.1-9.3.4.0 dependency.

## Changes in this snapshot
- Added the exact Fossils and Archaeology Revival 9.3.4.0 and GeckoLib 4.8.4 compile-time JARs under `libs/`.
- Restored `pack.mcmeta`, the biome modifier, and the owl biome tag from the last known working 2.1.0 JAR.
- Reworked server-side movement arbitration so danger avoidance, fossil investigation, and owner following do not each issue competing navigation commands.
- Switched the owl to flying navigation and a flying move controller; tamed, active owls use no-gravity movement and flee upward/away from nearby hostile mobs.
- Added hostile detection, frantic alarm hoots, and parrot-style imitation for zombie, skeleton, creeper, and spider sounds.
- Added basic torch/lantern item acceptance and persistence, plus owner interaction catchphrases.
- Corrected the fossil block registry IDs to the names used by the supplied Fossils and Archaeology Revival 9.3.4.0 `ModBlocks` registry fields (`stone_fossil`, `deepslate_fossil`, etc.).
- Registered the existing owl hoot audio through `sounds.json` and moved the OGG into the expected `sounds/` folder.

## Not complete / not verified
- This is **not a compiled or game-tested JAR**. The execution environment did not have Gradle installed and could not reach the Gradle distribution host, so a ForgeGradle build could not be run here.
- Torch/lantern attachment rendering and dynamic world lighting are not implemented. The entity currently accepts and saves the item, and explicitly tells the player this is visual/light functionality pending.
- Independent wing/leg/head animation is not yet implemented. The supplied geo model has a single `root` bone containing all 114 cubes, so the current walk/dig animations cannot articulate separate body parts.
- Texture UV/eye alignment has not been changed; it requires editing the geo model/texture together and checking it in-game.
- Fossil mining still needs in-game validation for exact block drops, tool suitability, mining timing, path access, and cargo collection. The source uses a hard-coded fossil block allowlist and must be tested against the installed mod.
- Existing saves should be backed up before testing any development build.

## Build
Use Java 17 and run `gradle build` from this project directory in an environment with access to ForgeGradle/Maven repositories. The target Forge dependency is currently `1.20.1-47.2.0`; adjust only if the user's Modrinth instance uses a different Forge 47.x build.
