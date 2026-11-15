# Fossil Finder Owl 3.0 development snapshot

This is an in-progress source update based on the user's uploaded Forge 2.1.0 project and Fossils and Archaeology Revival 1.20.1-9.3.4.0 dependency.

## Changes in this snapshot
- The project compiles against GeckoLib 4.8.4; Fossils and Archaeology Revival 9.3.4.0 remains a mandatory runtime dependency referenced through registry IDs.
- Restored `pack.mcmeta`, the biome modifier, and the owl biome tag from the last known working 2.1.0 JAR.
- Reworked server-side movement arbitration so danger avoidance, fossil investigation, and owner following do not each issue competing navigation commands.
- Switched the owl to flying navigation and a flying move controller; tamed, active owls use no-gravity movement and flee upward/away from nearby hostile mobs.
- Added hostile detection, frantic alarm hoots, and parrot-style imitation for zombie, skeleton, creeper, and spider sounds.
- Added basic torch/lantern item acceptance and persistence, plus owner interaction catchphrases.
- Corrected the fossil block registry IDs to the names used by the supplied Fossils and Archaeology Revival 9.3.4.0 JAR (`fossil_stone`, `fossil_deepslate`, etc.).
- Registered the existing owl hoot audio through `sounds.json` and moved the OGG into the expected `sounds/` folder.
- Replaced the prior owl geometry with the supplied Blockbench model, extracted its default/Blathers/Celeste textures, and preserved the head/hand attachment bones for hats and held items. Owls use the default skin unless their custom name is exactly Blathers or Celeste (case-insensitive).
- Replaced the prior animation set with the supplied standing, sitting, moving, flying, and dance motions. Adapted Blockbench variable expressions to GeckoLib Molang; idle uses the standing pose, and a short dance plays after feeding a hurt owl or excavating ore.
- Replaced the placeholder cargo panel with a fossil-textured GUI, 15 visible cargo slots, functional hat and pickaxe slots, player inventory/hotbar, and an owl preview. The hat slot reads and writes the owl's normal head equipment slot. Cargo saved in the previous 27-slot layout is retained and queued into the new slots as capacity opens.
- Added Museum Mode, activated with a crafted Dinopedia. The owl pauses excavation, reads recognized species-specific F&A bone items from its existing cargo, and places one item into each nearby empty item frame, prioritizing frames adjacent to the same species. Museum state and the stored Dinopedia persist; crouch-empty interaction or giving a pickaxe returns the book and resumes fieldwork.
- Added randomized, context-aware curator dialogue for new species, frame placement, missing specimens, missing frames, and inaccessible displays. The first displayed bone for each species is followed by a fact loaded from that species' `libs/Dinopedia/*.txt` file; Gradle packages all 64 source entries as mod assets. Pending dialogue and species discovery state persist across saves.
- Added a separate fieldwork dialogue queue for taming, gifts, healing, tool and light equipment, stay/follow commands, cargo checks, mining, discoveries, and quiet expeditions. Chat sends one sentence per entry, wraps long lines at 52 characters, and spaces entries by two seconds. Mining and follow chatter use cooldowns; queued field dialogue persists across saves.
- Added dance and heart-particle reactions for accepted gifts and fossil discoveries in fieldwork and Museum Mode. Rejected pickaxes and duplicate hats trigger a brief turn-away, smoke particles, and a context-appropriate response. In the owl inventory preview, a held torch or lantern uses its fixed 3D item model.

## Not complete / not verified
- GitHub Actions compiles the source and runs the JUnit tests; the resulting JAR has not yet been installed and tested in-game.
- Dynamic world lighting from carried torches or lanterns is not implemented. Their inventory-preview model now uses the fixed 3D item context; visual placement still needs in-game confirmation.
- The active owl model's animation and attachment positions still need in-game testing.
- Fossil mining, pickup, and the new cargo GUI still need in-game validation. Ore drops use the owner as the loot-context entity and enter a shared, persisted 15-slot cargo container; overflow from previous 27-slot saves is preserved.
- Museum Mode's item-ID classifier and dialogue formatting have focused tests, but item-frame navigation and the complete set of F&A bone registry IDs still need in-game validation against the exact runtime mod JAR.
- Existing saves should be backed up before testing any development build.

## Build
Use Java 17 and run `gradle build` from this project directory in an environment with access to ForgeGradle/Maven repositories. The target Forge dependency is currently `1.20.1-47.2.0`; adjust only if the user's Modrinth instance uses a different Forge 47.x build.
