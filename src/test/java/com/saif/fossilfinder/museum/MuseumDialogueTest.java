package com.saif.fossilfinder.museum;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuseumDialogueTest {
    @Test
    void givesSpeciesSpecificReactionsAndPlacementFacts() {
        var tyrannosaurus = new MuseumDialogue.Cue(
            MuseumDialogue.CueType.SPECIES_RECEIVED, "tyrannosaurus");
        String reaction = MuseumDialogue.line(tyrannosaurus, null, 0, 0);
        assertTrue(reaction.contains("EEEEEEEK!"));

        assertEquals("Alligator Gar",
            MuseumDialogue.displayName("alligator_gar"));
        assertTrue(MuseumDialogue.isSpeciesPart("bone_unique_tyrannosaurus", "tyrannosaurus"));
        assertFalse(MuseumDialogue.isSpeciesPart("bone_leggings", "tyrannosaurus"));
    }

    @Test
    void splitsDinopediaEntriesIntoRotatableFactSentences() {
        List<String> facts = MuseumDialogue.sentences(
            "First fact about fossils. Second fact about habitats! Final fact?");

        assertEquals(3, facts.size());
        assertEquals("Second fact about habitats!", facts.get(1));
    }

    @Test
    void packagesEveryImportedSpeciesEntryAsAModAsset() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (Stream<Path> entries = Files.list(Path.of("libs/Dinopedia"))) {
            List<Path> facts = entries.filter(path -> path.getFileName().toString().endsWith(".txt")).toList();
            assertFalse(facts.isEmpty());
            for (Path fact : facts) {
                String species = fact.getFileName().toString().replaceFirst("\\.txt$", "");
                String asset = "assets/fossil_finder_owl/dinopedia/" + fact.getFileName();
                assertNotNull(classLoader.getResource(asset), () -> asset + " was not copied into mod resources");
                assertEquals(species,
                    MuseumBoneClassifier.speciesFor("fossil", species + "_skull").orElse(null));
            }
        }
    }
}
