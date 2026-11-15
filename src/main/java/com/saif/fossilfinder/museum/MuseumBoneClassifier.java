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
    private static final Set<String> BONE_PARTS = Set.of(
        "bone", "bones", "skull", "rib", "ribs", "ribcage", "vertebra", "vertebrae",
        "tail", "foot", "feet", "arm", "leg"
    );
    private static final Set<String> ARMOR_PARTS = Set.of(
        "boots", "chestplate", "helmet", "leggings"
    );

    private MuseumBoneClassifier() {}

    public static Optional<String> speciesFor(String namespace, String path) {
        if (!"fossil".equals(namespace) || path == null) return Optional.empty();

        String normalizedPath = path.toLowerCase(Locale.ROOT);
        String[] parts = normalizedPath.split("_");
        boolean hasBonePart = false;
        for (String part : parts) {
            if (BONE_PARTS.contains(part)) hasBonePart = true;
            if (ARMOR_PARTS.contains(part)) return Optional.empty();
        }
        if (!hasBonePart) return Optional.empty();

        String boundedPath = "_" + normalizedPath + "_";
        return SPECIES.stream()
            .filter(species -> boundedPath.contains("_" + species + "_"))
            .max(Comparator.comparingInt(String::length));
    }
}
