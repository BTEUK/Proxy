package org.btuk.proxy.core.chat.automod;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoModWordRuleTest {

    @Test
    void testTooBadMatch() {
        AutoModWordRule rule = new AutoModFlagRule("test", List.of("too bad"), 1, Duration.ZERO, true);
        List<CandidateWord> candidates = AutoModWordRule.getCandidateWords("this is too bad");
        List<AutoModMatch> matches = rule.getMatches(candidates);
        assertFalse(matches.isEmpty(), "Should match 'too bad'");
        assertEquals("too bad", matches.getFirst().flaggedWord());
        assertEquals("too bad", matches.getFirst().messageWord());
    }

    @Test
    void testTooBadLeetspeakMatch() {
        AutoModWordRule rule = new AutoModFlagRule("test", List.of("too bad"), 1, Duration.ZERO, true);
        List<CandidateWord> candidates = AutoModWordRule.getCandidateWords("this is t00 b.a.d");
        List<AutoModMatch> matches = rule.getMatches(candidates);
        assertFalse(matches.isEmpty(), "Should match 't00 b.a.d' against 'too bad'");
        assertEquals("too bad", matches.getFirst().flaggedWord());
    }

    @Test
    void testPunctuationInFlaggedWord() {
        // Test case where flagged word has punctuation
        AutoModWordRule rule = new AutoModFlagRule("test", List.of("bad-word"), 1, Duration.ZERO, true);
        List<CandidateWord> candidates = AutoModWordRule.getCandidateWords("you are a bad-word");
        List<AutoModMatch> matches = rule.getMatches(candidates);
        
        assertFalse(matches.isEmpty(), "Should match 'bad-word' even if flagged as 'bad-word'");
        assertTrue(matches.stream().anyMatch(m -> m.flaggedWord().equals("badword") || m.flaggedWord().equals("bad word")));
    }

    @Test
    void testDoubleSpaceInMessage() {
        AutoModWordRule rule = new AutoModFlagRule("test", List.of("too bad"), 1, Duration.ZERO, true);
        List<CandidateWord> candidates = AutoModWordRule.getCandidateWords("this is too  bad");
        List<AutoModMatch> matches = rule.getMatches(candidates);
        assertFalse(matches.isEmpty(), "Should match 'too  bad' (double space) against 'too bad'");
    }

    @Test
    void testMixedPunctuationAndLeetspeak() {
        AutoModWordRule rule = new AutoModFlagRule("test", List.of("bad word"), 1, Duration.ZERO, true);
        List<CandidateWord> candidates = AutoModWordRule.getCandidateWords("you are b.a.d-w.0.r.d");
        List<AutoModMatch> matches = rule.getMatches(candidates);
        assertFalse(matches.isEmpty(), "Should match 'b.a.d-w.0.r.d' against 'bad word'");
    }
}
