package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Rule 3: Detects keyboard-mash, repeated-character runs, extreme vowel ratios,
 * and low-entropy repeated-word patterns.
 */
public class GibberishMashRule extends ProblemValidationRule {

    private static final Pattern REPEATED_CHAR_RUN = Pattern.compile("(?i)(.)\\1{3,}");
    private static final Pattern CONSONANT_RUN = Pattern.compile("(?i)[bcdfghjklmnpqrstvwxz]{6,}");
    private static final Pattern VOWELS = Pattern.compile("(?i)[aeiouy]");

    public GibberishMashRule() {
        super("GibberishMashRule");
    }

    @Override
    public void validate(String rawDescription) throws InvalidProblemStatementException {
        if (rawDescription == null || rawDescription.isBlank()) return;

        // 1. Repeated-character runs (e.g., 'aaaa', 'xxxxx', '11111')
        if (REPEATED_CHAR_RUN.matcher(rawDescription).find()) {
            throw new InvalidProblemStatementException(
                "Problem description contains repeated character sequences or keyboard-mash patterns.",
                List.of(
                    "Avoid repeated character runs.",
                    "Provide a meaningful, natural language problem description."
                )
            );
        }

        // 2. Long consecutive consonant runs
        if (CONSONANT_RUN.matcher(rawDescription).find()) {
            throw new InvalidProblemStatementException(
                "Problem description contains unnatural consonant sequences indicative of keyboard mash.",
                List.of(
                    "Check for typos or random keyboard mash.",
                    "Ensure words are spelled correctly in standard English."
                )
            );
        }

        // 3. Vowel ratio among alphabetic characters
        int totalLetters = 0;
        int vowelCount = 0;
        for (int i = 0; i < rawDescription.length(); i++) {
            char c = Character.toLowerCase(rawDescription.charAt(i));
            if (Character.isLetter(c)) {
                totalLetters++;
                if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y') {
                    vowelCount++;
                }
            }
        }

        if (totalLetters >= 10) {
            double vowelRatio = (double) vowelCount / totalLetters;
            if (vowelRatio < 0.15 || vowelRatio > 0.80) {
                throw new InvalidProblemStatementException(
                    "Problem description exhibits an unnatural letter distribution (" + Math.round(vowelRatio * 100) + "% vowels).",
                    List.of(
                        "Ensure the statement consists of readable English words.",
                        "Avoid gibberish, random text, or vowel-less acronym strings."
                    )
                );
            }
        }

        // 4. Repeated-word entropy
        String[] words = rawDescription.trim().toLowerCase().split("[^a-zA-Z0-9]+");
        List<String> validWords = new ArrayList<>();
        for (String w : words) {
            if (!w.isBlank()) validWords.add(w);
        }

        if (validWords.size() >= 4) {
            Set<String> uniqueWords = new HashSet<>(validWords);
            double ratio = (double) uniqueWords.size() / validWords.size();
            if (ratio < 0.40) {
                throw new InvalidProblemStatementException(
                    "Problem description contains repetitive words with insufficient information entropy.",
                    List.of(
                        "Avoid repeating the same words repeatedly.",
                        "Provide diverse, descriptive requirements describing the algorithmic task."
                    )
                );
            }
        }
    }
}
