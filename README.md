# Fossil Finder Owl – Forge 1.20.1 source project

This is a reconstructed Forge port based on the supplied Fabric v2.0.0 JAR and Fossils and Archaeology: Revival 9.3.4.0 Forge JAR.

## Target
- Minecraft 1.20.1 / Forge 47.x / Java 17
- Fossils and Archaeology: Revival `fossil` 9.3.4.0 (required at runtime)
- GeckoLib Forge 4.8.4 (required at runtime)

## Build
1. Install a Java 17 JDK and Gradle 8.x.
2. Keep `geckolib-forge-1.20.1-4.8.4.jar` in `libs/` (provided).
3. Run `gradle build` from this directory. ForgeGradle will resolve the Forge toolchain and mappings from its configured repositories.
4. The output, if the build succeeds, is `build/libs/FossilFinderOwl-Forge-2.1.0.jar`.
5. Put that JAR in the instance `mods` folder alongside Forge, Fossils and Archaeology, and GeckoLib.

## In-game access
- Creative inventory: spawn egg named **Fossil Finder Owl Spawn Egg**.
- Command: `/summon fossil_finder_owl:fossil_finder_owl ~ ~ ~`
- Tame: right-click with one of `fossil:fossil_bio`, `fossil:fossil_plant`, `fossil:fossil_shale`, or `fossil:fossil_tar`.
- Assign tool: right-click the tamed owl with an iron pickaxe.
- Stay/follow: right-click with a vanilla bone.
- Collect its cargo: empty-hand right-click while close.

## Important reconstruction notes
- The original JAR contains intermediary-mapped Fabric bytecode, not the original Java source. This project is a clean Forge-side reconstruction, not a byte-for-byte port.
- The original 256x256 texture and Blockbench cube geometry have been carried over. The Blockbench model had 114 ungrouped cubes; its face-specific UV layout is approximated in the generated GeckoLib geo file, so visual inspection in-game is still required.
- Fossils and Archaeology block and item IDs were checked against its supplied 9.3.4.0 JAR. The verified fossil blocks are `fossil:fossil_stone`, `fossil:fossil_deepslate`, `fossil:fossil_calcite`, `fossil:fossil_dripstone`, `fossil:fossil_sandstone`, `fossil:fossil_red_sandstone`, `fossil:fossil_tuff`, and `fossil:amber_ore`. Verified fossil gifts are `fossil:fossil_bio`, `fossil:fossil_plant`, `fossil:fossil_shale`, and `fossil:fossil_tar`. There is no registered `fossil:amber` item in the supplied JAR, so it is deliberately not used as a gift.
- This first reconstruction includes biome spawning in forest/plains, a spawn egg, summonable entity, taming, stay/follow, 12-slot cargo, and basic fossil-block excavation. Structure-proximity spawning, robust pathfinding excavation, full configuration, dialogue scheduling, and a complete test pass remain follow-up work.
- This environment had no Gradle installation and no network access to resolve ForgeGradle/Minecraft dependencies. Consequently, the source project could not be compiled or launched here. Do not treat this archive as a verified ready-to-install binary JAR until it has been built and tested in a Forge 1.20.1 development environment.
