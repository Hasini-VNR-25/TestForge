package com.testforge.backend.service.generator.offline;

/**
 * Interface for reference algorithmic solvers.
 */
public interface ReferenceSolver {

    boolean supports(String inputType, String operation);

    String solve(String input);
}
