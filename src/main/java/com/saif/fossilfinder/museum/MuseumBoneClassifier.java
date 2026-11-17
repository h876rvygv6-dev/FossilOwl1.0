package com.saif.fossilfinder.museum;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class MuseumBoneClassifier {
    public static final List<String> BONE_PARTS = List.of(
        "unique", "skull", "ribcage", "vertebrae", "tail", "arm_bone", "leg_bone", "foot");

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
        String normalized = path.toLowerCase(Locale.ROOT);
        return SPECIES.stream()
            .filter(species -> containsToken(normalized, species) && bonePartFor(normalized).isPresent())
            .max(Comparator.comparingInt(String::length));
    }

    public static Optional<String> speciesFromLabel(String label) {
        if (label == null || label.isBlank()) return Optional.empty();
        String normalized = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return SPECIES.stream()
            .filter(species -> normalized.contains(species.replace("_", "")))
            .max(Comparator.comparingInt(String::length));
    }

    public static Optional<String> bonePartFor(String path) {
        if (path == null) return Optional.empty();
        String normalized = path.toLowerCase(Locale.ROOT);
        for (String part : BONE_PARTS) {
            if (containsToken(normalized, part)) return Optional.of(part);
        }
        if (containsToken(normalized, "vertebra")) return Optional.of("vertebrae");
        if (containsToken(normalized, "rib") || containsToken(normalized, "ribs")
            || containsToken(normalized, "rib_cage")) return Optional.of("ribcage");
        if (containsToken(normalized, "feet")) return Optional.of("foot");
        return Optional.empty();
    }

    public static List<String> expectedBoneParts(String species) {
        return "megalodon".equals(species) ? List.of("unique") : BONE_PARTS;
    }

    public static String displayName(String species) {
        if (species == null || species.isBlank()) return "Unknown specimen";
        String[] parts = species.split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (name.length() > 0) name.append(' ');
            name.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return name.toString();
    }

    private static boolean containsToken(String path, String token) {
        return ("_" + path + "_").contains("_" + token + "_");
    }
}
