package com.saif.fossilfinder.museum;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuseumBoneClassifierTest {
    @Test
    void identifiesSpeciesFromBoneItemIds() {
        assertEquals(Optional.of("tyrannosaurus"),
            MuseumBoneClassifier.speciesFor("fossil", "bone_unique_tyrannosaurus"));
        assertEquals(Optional.of("alligator_gar"),
            MuseumBoneClassifier.speciesFor("fossil", "alligator_gar_skull"));
    }

    @Test
    void rejectsNonFossilItemsAndNonBoneItems() {
        assertTrue(MuseumBoneClassifier.speciesFor("minecraft", "bone").isEmpty());
        assertTrue(MuseumBoneClassifier.speciesFor("fossil", "fossil_bio").isEmpty());
        assertTrue(MuseumBoneClassifier.speciesFor("fossil", "bone_leggings").isEmpty());
    }
}
