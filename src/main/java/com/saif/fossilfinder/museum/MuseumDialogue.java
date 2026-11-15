package com.saif.fossilfinder.museum;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MuseumDialogue {
    public enum CueType {
        SPECIES_RECEIVED,
        SPECIES_DISPLAYED,
        NEEDS_SPECIMENS,
        NEEDS_FRAMES,
        FRAME_UNREACHABLE
    }

    public record Cue(CueType type, String species) {}

    private static final List<String> RECEIVED_LINES = List.of(
        "A new %s specimen! I shall try not to let my scholarly excitement ruffle my feathers.",
        "Well, well! %s has joined the collection. This is a rather good day for paleontology.",
        "A fresh %s fossil! I have just the display spot in mind... assuming the frames cooperate.",
        "The collection grows: %s! I can already hear the future visitors saying, \"Owl, tell us more!\"",
        "Oh, marvelous! A piece of %s. Let us give this ancient celebrity the proper introduction.",
        "One more mystery from %s, entrusted to my careful beak. I shall be most responsible.",
        "A %s specimen! History has arrived, and I am wearing my very serious curator face.",
        "Splendid find! %s is now part of our little museum. Little in size, enormous in importance.",
        "A new %s bone? I would applaud, but my wings are full of catalogues.",
        "The %s collection has its first guest! Do be welcome; the frames are just over there.",
        "How exciting, a %s specimen! I promise to place it somewhere dignified. Mostly dignified.",
        "A %s fossil! The past has sent us a rather compelling calling card.",
        "I see %s has arrived. Excellent! My museum instincts are tingling... politely, of course.",
        "A first-class %s discovery! If anyone asks, I was calm and professional the whole time.",
        "Another chapter of natural history begins with %s. Let us find it a frame worthy of the tale.",
        "A %s bone! This is the sort of delivery that makes a curator's whole week.",
        "Oh, look! %s. A remarkable addition, and not a speck of dust on my spectacles.",
        "Our records now include %s. I shall try to keep my notes neater than my nest.",
        "A fossil from %s! I knew this expedition would be educational. I was only slightly worried.",
        "What a treat: %s has joined us. I shall introduce it to the other exhibits.",
        "The %s collection begins! A small bone for the frame, a great leap for the museum.",
        "A %s specimen, delivered safely. My beak is trembling with academic anticipation.",
        "Ah, %s! A name worth remembering, and I have a rather good memory for dinner and dinosaurs.",
        "A new %s fossil! Let us preserve the wonder and perhaps avoid standing directly beneath it."
    );
    private static final List<String> TYRANNOSAURUS_LINES = List.of(
        "EEEEEEEK! A terrible lizard! A magnificent specimen, though I shall admire it from this side of the frame.",
        "A tyrant lizard fossil! I am perfectly composed. That was a scholarly squeak, not a frightened one.",
        "Tyrannosaurus has joined the collection! I shall label it carefully, from a sensible distance.",
        "A mighty tyrant lizard! I do hope the frame is sturdy and that the fossil has eaten recently. Oh, it is extinct."
    );
    private static final List<String> DISPLAYED_LINES = List.of(
        "Into the frame it goes. Did you know? %s",
        "A fine display! Here is a fact for your next expedition: %s",
        "There, history, handsomely hung. One fascinating detail: %s",
        "The exhibit is taking shape! My notes say: %s",
        "A proper place for a remarkable past. Did you know that %s",
        "Carefully placed, proudly displayed. I have been reading, and %s",
        "Another little window into prehistory: %s",
        "The museum gains a new chapter. Let me tell you: %s",
        "Excellent framing. I shall add this to the tour: %s",
        "There! The specimen is safe, and the story is even better: %s",
        "A curator's reward is a well-placed bone and a good fact: %s",
        "Displayed with care! My Dinopedia tells us that %s"
    );
    private static final List<String> NEEDS_SPECIMENS_LINES = List.of(
        "I am ready to curate! Could you add some fossil bones to my cargo?",
        "The frames are waiting, and so am I. A few dinosaur bones would start the show nicely.",
        "My beak is polished, my notes are open, but I do need specimens to catalogue.",
        "A museum cannot run on enthusiasm alone. Please bring me some identified fossil bones.",
        "No specimens just yet. Have you any fossil bones for our collection?",
        "I have room in my cargo for bones. The past is calling, and it sounds rather dusty.",
        "We need a first exhibit! Please place some F&A bones in my cargo.",
        "I am all set to curate. The bones, however, appear to be somewhere else.",
        "A little specimen would turn this quiet museum into a very exciting one.",
        "Whenever you are ready, add fossil bones to my cargo. I shall keep the catalogue open."
    );
    private static final List<String> NEEDS_FRAMES_LINES = List.of(
        "There are no empty item frames nearby. Could you hang a few for our exhibits?",
        "I have a bone and nowhere to hang it! Where is a good place for this fossil?",
        "This specimen deserves a frame. I cannot very well balance it on my head.",
        "No empty frames in sight. A small gallery wall would make a splendid start.",
        "I am carrying history, but the walls are full! Could you make room with an empty frame?",
        "Where shall I display this fossil? I see no empty item frames nearby.",
        "A frame, please! My wings are not suitable shelving, however elegant they look.",
        "There is a specimen ready for display, but not a single empty frame nearby.",
        "The collection is eager to grow. Could you place an empty frame within eight blocks?",
        "I would hang this fossil myself, but I need an empty frame to get started."
    );
    private static final List<String> FRAME_UNREACHABLE_LINES = List.of(
        "I can see the frame, but I cannot reach it. Could you leave a little room to fly?",
        "That is a lovely display spot, but the route is blocked. Where is a good place to hang this fossil?",
        "My beak says yes; my path says no. Could you clear a way to the frame?",
        "I cannot quite get to that frame. A more open spot would make this curator very happy.",
        "The exhibit is within sight, not within reach. Could you move the obstruction?",
        "I would love to place this specimen, but the path is rather uncooperative.",
        "A fine frame, inconveniently fenced off. Could you give me a clear approach?",
        "The fossil is ready; the route is not. Let us try a reachable frame."
    );
    private static final String[] SPECIES_PARTS = {
        "arm_bone", "bone", "bones", "feet", "foot", "leg_bone", "rib", "ribs",
        "ribcage", "skull", "tail", "vertebra", "vertebrae"
    };

    private MuseumDialogue() {}

    public static String line(Cue cue, ResourceManager resources, int variant, int factIndex) {
        return switch (cue.type()) {
            case SPECIES_RECEIVED -> choose(cue.species().equals("tyrannosaurus")
                ? TYRANNOSAURUS_LINES : RECEIVED_LINES, variant).formatted(displayName(cue.species()));
            case SPECIES_DISPLAYED -> {
                String fact = fact(resources, cue.species(), factIndex);
                yield choose(DISPLAYED_LINES, variant).formatted(fact);
            }
            case NEEDS_SPECIMENS -> choose(NEEDS_SPECIMENS_LINES, variant);
            case NEEDS_FRAMES -> choose(NEEDS_FRAMES_LINES, variant);
            case FRAME_UNREACHABLE -> choose(FRAME_UNREACHABLE_LINES, variant);
        };
    }

    public static String fact(ResourceManager resources, String species, int factIndex) {
        List<String> facts = loadFacts(resources, species);
        if (facts.isEmpty()) {
            return "the bones of " + displayName(species) + " help us piece together a remarkable past.";
        }
        String selected = facts.get(Math.floorMod(factIndex, facts.size()));
        return selected.endsWith(".") || selected.endsWith("!") || selected.endsWith("?")
            ? selected : selected + ".";
    }

    public static List<String> sentences(String text) {
        return Arrays.stream(text.trim().split("(?<=[.!?])\\s+"))
            .map(String::trim)
            .filter(sentence -> !sentence.isEmpty())
            .toList();
    }

    public static String displayName(String species) {
        if (species == null || species.isBlank()) return "this specimen";
        String[] parts = species.split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (name.length() > 0) name.append(' ');
            name.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return name.toString();
    }

    public static boolean isSpeciesPart(String path, String species) {
        if (path == null || species == null) return false;
        String normalized = path.toLowerCase(Locale.ROOT);
        if (normalized.contains("leggings") || normalized.contains("chestplate")
            || normalized.contains("helmet") || normalized.contains("boots")) return false;
        String boundedPath = "_" + normalized + "_";
        if (!boundedPath.contains("_" + species + "_")) return false;
        for (String part : SPECIES_PARTS) {
            if (boundedPath.contains("_" + part + "_")) return true;
        }
        return false;
    }

    private static List<String> loadFacts(ResourceManager resources, String species) {
        ResourceLocation location = new ResourceLocation(
            "fossil_finder_owl", "dinopedia/" + species + ".txt");
        var resource = resources.getResource(location);
        if (resource.isEmpty()) {
            System.getLogger(MuseumDialogue.class.getName()).log(
                System.Logger.Level.WARNING, "No Dinopedia entry found for {0}", species);
            return List.of();
        }
        Resource entry = resource.get();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(entry.open(), StandardCharsets.UTF_8))) {
            return sentences(reader.lines().reduce("", (first, next) -> first.isEmpty() ? next : first + " " + next));
        } catch (IOException exception) {
            System.getLogger(MuseumDialogue.class.getName()).log(
                System.Logger.Level.WARNING, "Could not read Dinopedia entry for " + species, exception);
            return List.of();
        }
    }

    private static String choose(List<String> lines, int variant) {
        return lines.get(Math.floorMod(variant, lines.size()));
    }
}
