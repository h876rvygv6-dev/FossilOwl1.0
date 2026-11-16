package com.saif.fossilfinder.fieldwork;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldworkDialogueTest {
    @Test
    void splitsSentencesIntoReadableChatLines() {
        List<String> sentences = FieldworkDialogue.sentences(
            "A discovery! The fossil is safe with me.");

        assertEquals(2, sentences.size());
        assertEquals("A discovery!", sentences.get(0));
        assertEquals("The fossil is safe with me.", sentences.get(1));
    }

    @Test
    void keepsEveryDialogueVariantShortEnoughForChat() {
        for (FieldworkDialogue.Cue cue : FieldworkDialogue.Cue.values()) {
            for (int variant = 0; variant < 24; variant++) {
                List<String> lines = FieldworkDialogue.sentences(FieldworkDialogue.line(cue, variant));
                assertFalse(lines.isEmpty(), cue.name());
                for (String line : lines) {
                    assertTrue(line.length() <= 52, cue + " has an overlong line: " + line);
                }
            }
        }
    }

    @Test
    void everyShovelRequestVariantNamesTheNeededTool() {
        for (int variant = 0; variant < 24; variant++) {
            assertTrue(FieldworkDialogue.line(FieldworkDialogue.Cue.SHOVEL_REQUEST, variant)
                .toLowerCase().contains("shovel"));
        }
    }
}
