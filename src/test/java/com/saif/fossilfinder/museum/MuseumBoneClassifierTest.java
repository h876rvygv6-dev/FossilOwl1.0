package com.saif.fossilfinder.museum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuseumBoneClassifierTest {
    @Test
    void identifiesBoneSpeciesAndPartFromFossilItemIds() {
        assertEquals("tyrannosaurus",
            MuseumBoneClassifier.speciesFor("fossil", "bone_unique_tyrannosaurus").orElseThrow());
        assertEquals("skull", MuseumBoneClassifier.bonePartFor("alligator_gar_skull").orElseThrow());
        assertEquals("pachycephalosaurus",
            MuseumBoneClassifier.speciesFromLabel("Pachycephalosaurus").orElseThrow());
    }

    @Test
    void rejectsNonFossilsAndNonBoneItems() {
        assertTrue(MuseumBoneClassifier.speciesFor("minecraft", "bone_unique_tyrannosaurus").isEmpty());
        assertTrue(MuseumBoneClassifier.speciesFor("fossil", "fossil_bio").isEmpty());
    }

    @Test
    void usesEightFramesForSpeciesAndTheAvailableMegalodonBone() {
        assertEquals(8, MuseumBoneClassifier.expectedBoneParts("allosaurus").size());
        assertEquals(1, MuseumBoneClassifier.expectedBoneParts("megalodon").size());
    }
}
