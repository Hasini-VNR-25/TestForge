package com.testforge.backend;

import com.testforge.backend.exception.InvalidProblemStatementException;
import com.testforge.backend.service.validator.ProblemStatementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProblemStatementValidatorTest {

    private ProblemStatementValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProblemStatementValidator(null);
    }

    @Test
    @DisplayName("AC 1 & AC 2: Reject keyboard mash 'asdkjhasd qwe zxcv'")
    void testKeyboardMashRejected() {
        InvalidProblemStatementException ex = assertThrows(
                InvalidProblemStatementException.class,
                () -> validator.validate("asdkjhasd qwe zxcv")
        );
        assertNotNull(ex.getReason());
        assertFalse(ex.getReason().isBlank());
        assertNotNull(ex.getSuggestions());
        assertFalse(ex.getSuggestions().isEmpty());
    }

    @Test
    @DisplayName("AC 3: Reject empty and very short inputs")
    void testEmptyAndShortInputRejected() {
        assertThrows(InvalidProblemStatementException.class, () -> validator.validate(""));
        assertThrows(InvalidProblemStatementException.class, () -> validator.validate("   "));
        assertThrows(InvalidProblemStatementException.class, () -> validator.validate("short"));
        assertThrows(InvalidProblemStatementException.class, () -> validator.validate("a b c"));
    }

    @Test
    @DisplayName("AC 4: Valid problem 'Given a string, return it reversed' passes validation")
    void testValidProblemPasses() {
        assertDoesNotThrow(() -> validator.validate("Given a string, return it reversed"));
        assertDoesNotThrow(() -> validator.validate("Given an array of integers, compute the sum of all elements"));
        assertDoesNotThrow(() -> validator.validate("Given an integer n, calculate its factorial"));
    }

    @Test
    @DisplayName("Reject symbol-only text")
    void testSymbolsRejected() {
        assertThrows(
                InvalidProblemStatementException.class,
                () -> validator.validate("1234567890 !@#$%^&*()_+ ==================")
        );
    }

    @Test
    @DisplayName("Reject low-entropy repeated words")
    void testRepeatedWordsRejected() {
        assertThrows(
                InvalidProblemStatementException.class,
                () -> validator.validate("return return return return return return")
        );
    }

    @Test
    @DisplayName("Reject statements with no task signal words")
    void testNoTaskSignalWordsRejected() {
        assertThrows(
                InvalidProblemStatementException.class,
                () -> validator.validate("The computer system has some numbers in memory today")
        );
    }
}
