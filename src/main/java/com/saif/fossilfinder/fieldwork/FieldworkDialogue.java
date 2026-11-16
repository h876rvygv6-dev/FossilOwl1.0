package com.saif.fossilfinder.fieldwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FieldworkDialogue {
    public enum Cue {
        TAMED,
        FOLLOWING,
        MINING_COMMENT,
        OWNER_FOUND_FOSSIL,
        OWL_FOUND_FOSSIL,
        QUIET_EXPEDITION,
        HAT_GIFT,
        HEALING,
        PICKAXE,
        SHOVEL,
        SHOVEL_REQUEST,
        REJECTED_PICKAXE,
        REJECTED_HAT,
        LIGHT,
        STAY,
        FOLLOW,
        CARGO,
        FIELDWORK_RETURNED
    }

    private static final int MAX_LINE_LENGTH = 52;

    private static final List<String> TAMED = List.of(
        "A fossil for me? Hoo! I accept your offer and your company.",
        "Well, this is unexpected. I shall be your companion and field scholar.",
        "A new partner in paleontology! I promise to keep my notes tidy.",
        "Thank you for the fossil. Let us see what the ancient ground remembers.",
        "I am honored. Do keep up; my curiosity has quite a wingspan.",
        "Hoo, a companion! I was beginning to think the rocks were my only friends."
    );
    private static final List<String> FOLLOWING = List.of(
        "I am right behind you. This expedition has no shortage of turns.",
        "Do you have a destination, or are we studying wandering?",
        "I follow faithfully. My questions about this route are less faithful.",
        "Another long walk. At least the scenery is geologically dependable.",
        "I do hope there is a plan. The map appears to disagree.",
        "Hoo. Your pace is impressive; your sense of direction remains a mystery.",
        "I shall keep close. Someone must remember where we started.",
        "More travelling, then. My feathers are filing a formal complaint."
    );
    private static final List<String> MINING_COMMENT = List.of(
        "A great deal of stone has moved. The fossil remains elusive.",
        "An energetic excavation. Perhaps let us preserve a little hillside.",
        "That was a bold approach to the surrounding rock.",
        "The pickaxe is making excellent introductions to ordinary stone.",
        "A careful eye may save us from excavating the entire mountain.",
        "I admire your enthusiasm. The geology would prefer more precision.",
        "The rocks are certainly noticing us. The fossils may need more time.",
        "A little less rubble might leave more clues for the catalogue."
    );
    private static final List<String> OWNER_FOUND_FOSSIL = List.of(
        "A fossil deposit! Excellent. I knew your pickaxe had a purpose.",
        "There it is! A genuine trace of ancient life, not another pebble.",
        "A fine discovery. I shall revise my notes about this expedition.",
        "Hoo! That layer holds a story. Do take a closer look.",
        "At last, something the geology department will want to hear about.",
        "A proper fossil! This is why patience belongs in every field kit.",
        "A promising find. The old world has not finished surprising us.",
        "Remarkable! I shall try to look less pleased than I feel."
    );
    private static final List<String> OWL_FOUND_FOSSIL = List.of(
        "A fossil deposit! I found one. You may applaud quietly.",
        "Discovery! The earth has yielded a rather interesting clue.",
        "This deposit looks promising. My beak and I approve.",
        "A real fossil layer! Our expedition has earned a bright spot.",
        "Hoo! Ancient history ahead. I shall begin the careful excavation.",
        "The ground has answered. I will collect the evidence safely.",
        "A splendid deposit. There is hope for our field notes yet.",
        "I found a fossil! My professional excitement is entirely controlled."
    );
    private static final List<String> QUIET_EXPEDITION = List.of(
        "A quiet stretch. Even the fossils seem to be taking a nap.",
        "The ground is keeping its secrets today. We shall keep looking.",
        "No discoveries yet. Persistence is still a respectable result.",
        "Another patch of ordinary stone. How very ordinary of it.",
        "The silence is informative. It tells us to keep searching.",
        "I have catalogued several rocks and one determined companion.",
        "Nothing new in this layer. The next one may have better manners.",
        "Hoo. The fossils are making us earn this chapter."
    );
    private static final List<String> HAT_GIFT = List.of(
        "A hat! My scholarly appearance has reached a new peak.",
        "How distinguished I look. The field notes will be impressed.",
        "A fine hat for fieldwork. Dust may now approach respectfully.",
        "This suits my plumage rather well. Thank you!",
        "A new hat! I shall wear it during all important excavations.",
        "Excellent taste. I feel ready to lecture the nearest boulder."
    );
    private static final List<String> HEALING = List.of(
        "Hoo, thank you. That has restored more than my feathers.",
        "Much better. I can resume my scholarly duties now.",
        "A snack and a kind thought. You are a good expedition leader.",
        "Thank you. My energy has returned, along with my appetite.",
        "I feel better already. Let us continue carefully.",
        "You noticed I was hurt. That means a great deal."
    );
    private static final List<String> PICKAXE = List.of(
        "A sturdy pickaxe! I am ready to examine the fossil layers.",
        "Excellent tool. I shall put it to careful scientific use.",
        "A proper pickaxe. The deposits will not know what hit them.",
        "Thank you. I shall keep the edge sharp and the notes sharper.",
        "Now we can excavate. I promise to return with evidence.",
        "A fine instrument for our fieldwork. Shall we begin?"
    );
    private static final List<String> SHOVEL = List.of(
        "Thank you for the shovel. I can clear the softer layers now.",
        "A shovel! This will help me reach the fossil through the soil.",
        "Excellent. Sand and gravel will no longer stop our expedition."
    );
    private static final List<String> SHOVEL_REQUEST = List.of(
        "The fossil is behind soil. Could you bring me a shovel?",
        "I found a fossil, but dirt blocks the way. I need a shovel.",
        "Sand and gravel are in the way. Please bring me a shovel."
    );
    private static final List<String> REJECTED_PICKAXE = List.of(
        "I need a sturdier pickaxe for fossil-bearing stone.",
        "That tool is a little too delicate for proper excavation.",
        "A stronger pick, please. Ancient stone is rather stubborn."
    );
    private static final List<String> REJECTED_HAT = List.of(
        "I can only wear one hat at a time, though I admire your enthusiasm.",
        "Another hat? My head is scholarly, not especially spacious.",
        "I appreciate the offer, but this hat already suits me rather well."
    );
    private static final List<String> LIGHT = List.of(
        "A light for the expedition! You do think of everything.",
        "Thank you. The shadows may keep their secrets elsewhere.",
        "A lantern! At last, my notes and I can see one another.",
        "That will brighten our search. How thoughtful!",
        "A useful gift. I shall try not to mistake it for a star."
    );
    private static final List<String> STAY = List.of(
        "I shall perch here. Do call if the rocks become interesting.",
        "Very well, I will wait. Someone must supervise this patch.",
        "A quiet post. I shall keep watch and guard the field notes.",
        "I will stay nearby. Try not to miss me too much.",
        "Perching it is. I shall practice my patient expression."
    );
    private static final List<String> FOLLOW = List.of(
        "Back on the trail! I was beginning to study that patch of dirt.",
        "Gladly. There is always more history just around the corner.",
        "Let us go. My curiosity has already packed its bags.",
        "Following you now. The field notes are ready for a new page.",
        "Onward, then! I shall keep a careful eye on the ground."
    );
    private static final List<String> CARGO = List.of(
        "Let us take stock. My cargo is a curious little archive.",
        "A moment to sort the finds would be most welcome.",
        "Please inspect my cargo; I have tried to keep the fossils tidy.",
        "A field scholar must catalogue. I have brought the evidence.",
        "Here are our finds. The dust is complimentary."
    );
    private static final List<String> FIELDWORK_RETURNED = List.of(
        "Back to fieldwork! I have missed the honest smell of stone.",
        "The museum can wait. There are layers to investigate.",
        "My pickaxe and I are ready for the next expedition.",
        "Field notes open, wings ready. Let us find something remarkable.",
        "Back to the trail. I promise to complain only when warranted."
    );

    private FieldworkDialogue() {}

    public static String line(Cue cue, int variant) {
        return choose(lines(cue), variant);
    }

    public static List<String> sentences(String dialogue) {
        List<String> result = new ArrayList<>();
        for (String sentence : dialogue.trim().split("(?<=[.!?])\\s+")) {
            wrapSentence(sentence, result);
        }
        return List.copyOf(result);
    }

    private static List<String> lines(Cue cue) {
        return switch (cue) {
            case TAMED -> TAMED;
            case FOLLOWING -> FOLLOWING;
            case MINING_COMMENT -> MINING_COMMENT;
            case OWNER_FOUND_FOSSIL -> OWNER_FOUND_FOSSIL;
            case OWL_FOUND_FOSSIL -> OWL_FOUND_FOSSIL;
            case QUIET_EXPEDITION -> QUIET_EXPEDITION;
            case HAT_GIFT -> HAT_GIFT;
            case HEALING -> HEALING;
            case PICKAXE -> PICKAXE;
            case SHOVEL -> SHOVEL;
            case SHOVEL_REQUEST -> SHOVEL_REQUEST;
            case REJECTED_PICKAXE -> REJECTED_PICKAXE;
            case REJECTED_HAT -> REJECTED_HAT;
            case LIGHT -> LIGHT;
            case STAY -> STAY;
            case FOLLOW -> FOLLOW;
            case CARGO -> CARGO;
            case FIELDWORK_RETURNED -> FIELDWORK_RETURNED;
        };
    }

    private static String choose(List<String> lines, int variant) {
        return lines.get(Math.floorMod(variant, lines.size()));
    }

    private static void wrapSentence(String sentence, List<String> result) {
        StringBuilder line = new StringBuilder();
        for (String word : Arrays.asList(sentence.split("\\s+"))) {
            if (line.length() > 0 && line.length() + 1 + word.length() > MAX_LINE_LENGTH) {
                result.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) result.add(line.toString());
    }
}
