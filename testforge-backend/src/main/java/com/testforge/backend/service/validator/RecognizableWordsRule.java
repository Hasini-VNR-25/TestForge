package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;
import java.util.*;

/**
 * Rule 4: Requires a minimum share of recognizable English and programming words.
 */
public class RecognizableWordsRule extends ProblemValidationRule {

    private static final double MIN_RECOGNIZABLE_SHARE = 0.45;

    private static final Set<String> DICTIONARY = new HashSet<>(Arrays.asList(
            // Common English words
            "a", "about", "above", "after", "again", "against", "all", "also", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can",
            "cannot", "could", "did", "do", "does", "doing", "don't", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his",
            "how", "i", "if", "in", "into", "is", "isn't", "it", "its", "itself", "just", "me", "more", "most", "must",
            "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once", "only", "or", "other", "our", "ours",
            "ourselves", "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the",
            "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to",
            "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while",
            "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves",
            "given", "find", "return", "compute", "calculate", "determine", "check", "write", "solve", "implement",
            "print", "read", "evaluate", "contain", "contains", "consist", "consists", "represent", "represents",
            "equal", "equals", "greater", "less", "maximum", "minimum", "max", "min", "limit", "length", "size",
            "assume", "condition", "following", "between", "order", "value", "values", "positive", "negative", "zero",
            "even", "odd", "true", "false", "valid", "invalid", "empty", "null", "unique", "distinct", "duplicate",
            "pair", "pairs", "total", "index", "indices", "target", "difference", "product", "quotient", "remainder",
            "count", "case", "cases", "test", "example", "examples", "constraint", "constraints", "note", "notes",
            "description", "title", "problem", "solution", "algorithm", "function", "method", "class", "program",
            "code", "input", "output", "format", "single", "multiple", "two", "three", "first", "second", "last",
            "next", "previous", "start", "end", "begin", "finish", "middle", "left", "right", "top", "bottom",
            "array", "arrays", "string", "strings", "char", "character", "characters", "integer", "integers", "int",
            "number", "numbers", "digit", "digits", "num", "nums", "list", "lists", "element", "elements", "item",
            "items", "sequence", "sequences", "subsequence", "substring", "substrings", "matrix", "matrices", "grid",
            "row", "rows", "column", "columns", "vector", "vectors", "set", "sets", "map", "maps", "table", "tables",
            "stack", "stacks", "queue", "queues", "tree", "trees", "node", "nodes", "graph", "graphs", "edge", "edges",
            "vertex", "vertices", "heap", "trie", "hash", "key", "password", "sum", "sums", "add", "subtract",
            "multiply", "divide", "modulo", "power", "sqrt", "factorial", "factorials", "prime", "primes", "composite",
            "fibonacci", "palindrome", "palindromic", "anagram", "reverse", "reversed", "reversing", "sort", "sorted",
            "sorting", "ascending", "descending", "search", "binary", "linear", "traverse", "traversal", "depth",
            "breadth", "path", "cycle", "loop", "range", "interval", "intervals", "merge", "split", "replace",
            "remove", "insert", "delete", "append", "prepend", "filter", "transform", "convert", "parse", "process",
            "match", "matches", "prefix", "suffix", "rotate", "rotation", "shift", "swap", "frequency", "occur",
            "occurs", "occurrence", "occurrences", "consecutive", "adjacent", "contiguous", "longest", "shortest",
            "smallest", "largest", "highest", "lowest", "best", "worst", "optimal", "possible", "impossible",
            "either", "neither", "both", "one", "zero", "none", "every", "all", "any", "some", "exists", "exist"
    ));

    public RecognizableWordsRule() {
        super("RecognizableWordsRule");
    }

    @Override
    public void validate(String rawDescription) throws InvalidProblemStatementException {
        if (rawDescription == null || rawDescription.isBlank()) return;

        String[] tokens = rawDescription.toLowerCase().split("[^a-zA-Z]+");
        List<String> words = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() >= 2) {
                words.add(t);
            }
        }

        if (words.size() >= 3) {
            int recognizedCount = 0;
            for (String w : words) {
                if (isRecognized(w)) {
                    recognizedCount++;
                }
            }

            double share = (double) recognizedCount / words.size();
            if (share < MIN_RECOGNIZABLE_SHARE) {
                throw new InvalidProblemStatementException(
                    "Problem description contains too few recognizable English or programming words (" + Math.round(share * 100) + "% recognized).",
                    List.of(
                        "State the problem using standard English words and software development terminology.",
                        "Check for typing errors, missing spaces, or non-English input text."
                    )
                );
            }
        }
    }

    private boolean isRecognized(String word) {
        if (DICTIONARY.contains(word)) return true;
        // Simple English morphological stemming
        if (word.endsWith("s") && DICTIONARY.contains(word.substring(0, word.length() - 1))) return true;
        if (word.endsWith("es") && DICTIONARY.contains(word.substring(0, word.length() - 2))) return true;
        if (word.endsWith("ed") && DICTIONARY.contains(word.substring(0, word.length() - 2))) return true;
        if (word.endsWith("ing") && DICTIONARY.contains(word.substring(0, word.length() - 3))) return true;
        if (word.endsWith("ly") && DICTIONARY.contains(word.substring(0, word.length() - 2))) return true;
        if (word.endsWith("d") && DICTIONARY.contains(word.substring(0, word.length() - 1))) return true;
        return false;
    }
}
