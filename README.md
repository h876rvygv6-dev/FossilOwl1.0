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
4. The output, if the build succeeds, is `build/libs/FossilFinderOwl-Forge-3.0.0-dev1.jar`.
5. Put that JAR in the instance `mods` folder alongside Forge, Fossils and Archaeology, and GeckoLib.

## In-game access
- Creative inventory: spawn egg named **Blathers Spawn Egg**.
- Creative inventory: all six 2D hat sprites are in the Combat tab. Equip one in the player's helmet slot or use it on an owned Blathers. The owl's cargo screen has a hat slot for equipping, swapping, or retrieving hats; crouch and interact empty-handed still retrieves the hat directly.
- Hat recipes use leather, string, wool, and Fossils and Archaeology items. Hats are durable, grant +6 maximum health (+10 for the dinosaur plushie hats), and do not provide armor protection.
- Command: `/summon fossil_finder_owl:fossil_finder_owl ~ ~ ~`
- Tame: right-click with one of `fossil:fossil_bio`, `fossil:fossil_plant`, `fossil:fossil_shale`, or `fossil:fossil_tar`.
- Assign tool: right-click the tamed owl with an iron pickaxe.
- Stay/follow: right-click with a vanilla bone.
- Collect its cargo: empty-hand right-click while close to open the fossil-themed screen with 15 cargo slots, hat and pickaxe slots, and a live owl preview. Cargo from older 27-slot saves is preserved and moved into the visible slots as space becomes available.
- Museum Mode: craft a Dinopedia and right-click your tamed owl with it. Add F&A bone items to the owl's cargo and place empty item frames within eight blocks; the owl will fill them and group recognized species together. Blathers reacts when a species is first added and shares a Dinopedia fact from `libs/Dinopedia` when its first bone is displayed; he also asks for bones, frames, or a clear route when he needs them. Crouch and interact empty-handed to retrieve the Dinopedia and resume fieldwork, or give the owl a pickaxe to return to fieldwork with that tool equipped. A custom Museum Workbench is not required for this frame-based gameplay.
- Named owl easter eggs: use a name tag to name an owl **Blathers** or **Celeste** (case-insensitive) to apply that character's skin. Unnamed owls, including naturally spawning owls and spawn-egg owls, keep the default skin. Feeding a hurt owl or having it excavate ore triggers its dance.

## Important reconstruction notes
- The original JAR contains intermediary-mapped Fabric bytecode, not the original Java source. This project is a clean Forge-side reconstruction, not a byte-for-byte port.
- The active owl renderer uses the supplied Blockbench geometry and textures, with GeckoLib animations and added head/hand attachment points. The editable Blockbench project is kept at `tools/blockbench/fossil_finder_owl.bbmodel`.
- Fossils and Archaeology block and item IDs were checked against its supplied 9.3.4.0 JAR. The verified fossil blocks are `fossil:fossil_stone`, `fossil:fossil_deepslate`, `fossil:fossil_calcite`, `fossil:fossil_dripstone`, `fossil:fossil_sandstone`, `fossil:fossil_red_sandstone`, `fossil:fossil_tuff`, and `fossil:amber_ore`. Verified fossil gifts are `fossil:fossil_bio`, `fossil:fossil_plant`, `fossil:fossil_shale`, and `fossil:fossil_tar`. There is no registered `fossil:amber` item in the supplied JAR, so it is deliberately not used as a gift.
- This reconstruction includes biome spawning in forest/plains, a spawn egg, summonable entity, taming, stay/follow, a 15-slot cargo inventory, and basic fossil-block excavation. An owl now abandons its fossil target to follow its owner when they move more than eight blocks away; mining, loot collection, and movement still need in-game validation.
- GitHub Actions successfully compiles the source with Gradle, but that build does not verify the in-game model appearance, mining, pickup, or pathfinding. Test the generated JAR in a Forge 1.20.1 instance before treating those behaviors as game-verified.
