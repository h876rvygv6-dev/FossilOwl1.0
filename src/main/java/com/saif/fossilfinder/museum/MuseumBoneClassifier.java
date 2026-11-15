package com.saif.fossilfinder.museum;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class MuseumBoneClassifier {
    private static final Set<String> SPECIES = Set.of(
        "alligator_gar", "allosaurus", "ankylosaurus", "aquilolamna", "arthropleura",
        "brachiosaurus", "ceratosaurus", "citipati", "coelacanth", "compsognathus",
        "confuciusornis", "crassigyrinus", "deinonychus", "dicranurus", "dilophosaurus",
        "dimetrodon", "dimorphodon", "diplocaulus", "diplodocus", "dodo", "dryosaurus",
        "edaphosaurus", "elasmotherium", "gallimimus", "gastornis", "henodus",
        "ichthyosaurus", "kelenken", "liopleurodon", "lonchodomas", "mammoth",
        "megalania", "megaloceros", "megalodon", "megalograptus", "meganeura",
        "mosasaurus", "nautilus", "ornitholestes", "pachycephalosaurus",
        "pachyrhinosaurus", "parasaurolophus", "phorusrhacos", "platybelodon",
        "plesiosaurus", "postosuchus", "protoceratops", "psittacosaurus", "pteranodon",
        "quagga", "quetzalcoatlus", "sarcosuchus", "scotoharpes", "smilodon",
        "spinosaurus", "stegosaurus", "sturgeon", "therizinosaurus", "tiktaalik",
        "titanis", "triceratops", "tyrannosaurus", "velociraptor", "walliserops"
    );

    private MuseumBoneClassifier() {}

    public static Optional<String> speciesFor(String namespace, String path) {
        if (!"fossil".equals(namespace) || path == null) return Optional.empty();

        String normalizedPath = path.toLowerCase(Locale.ROOT);
        return SPECIES.stream()
            .filter(species -> MuseumDialogue.isSpeciesPart(normalizedPath, species))
            .max(Comparator.comparingInt(String::length));
    }
}
