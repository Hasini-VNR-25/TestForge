package com.testforge.backend.service.generator.offline;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * B4: Registry of small Java reference solvers for supported operations.
 * Computes exact outputs for recognized operations or flags cases as 'Needs manual review'.
 */
@Component
public class ReferenceSolverRegistry {

    private final List<ReferenceSolver> solvers = new ArrayList<>();

    public ReferenceSolverRegistry() {
        registerDefaultSolvers();
    }

    private void registerDefaultSolvers() {
        // 1. Reverse Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "reverse".equalsIgnoreCase(operation) &&
                        ("string".equalsIgnoreCase(inputType) || "array".equalsIgnoreCase(inputType));
            }

            @Override
            public String solve(String input) {
                if (input == null) return "null";
                String trimmed = input.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String inside = trimmed.substring(1, trimmed.length() - 1).trim();
                    if (inside.isEmpty()) return "[]";
                    String[] parts = inside.split("\\s*,\\s*");
                    List<String> list = Arrays.asList(parts);
                    Collections.reverse(list);
                    return "[" + String.join(", ", list) + "]";
                }

                boolean quoted = (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2);
                String raw = quoted ? trimmed.substring(1, trimmed.length() - 1) : trimmed;
                String reversed = new StringBuilder(raw).reverse().toString();
                return quoted ? "\"" + reversed + "\"" : reversed;
            }
        });

        // 2. Sum Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "sum".equalsIgnoreCase(operation) &&
                        ("array".equalsIgnoreCase(inputType) || "integer".equalsIgnoreCase(inputType));
            }

            @Override
            public String solve(String input) {
                if (input == null) return "0";
                String trimmed = input.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String inside = trimmed.substring(1, trimmed.length() - 1).trim();
                    if (inside.isEmpty()) return "0";
                    String[] parts = inside.split("\\s*,\\s*");
                    long sum = 0;
                    for (String p : parts) {
                        try {
                            sum += Long.parseLong(p.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    return String.valueOf(sum);
                }

                try {
                    long n = Long.parseLong(trimmed);
                    if (n < 0) return "0";
                    long sum = n * (n + 1) / 2;
                    return String.valueOf(sum);
                } catch (Exception e) {
                    return "0";
                }
            }
        });

        // 3. Palindrome Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "palindrome".equalsIgnoreCase(operation);
            }

            @Override
            public String solve(String input) {
                if (input == null) return "false";
                String trimmed = input.trim();
                boolean quoted = (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2);
                String raw = quoted ? trimmed.substring(1, trimmed.length() - 1) : trimmed;
                String rev = new StringBuilder(raw).reverse().toString();
                return String.valueOf(raw.equalsIgnoreCase(rev));
            }
        });

        // 4. Sort Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "sort".equalsIgnoreCase(operation);
            }

            @Override
            public String solve(String input) {
                if (input == null) return "[]";
                String trimmed = input.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String inside = trimmed.substring(1, trimmed.length() - 1).trim();
                    if (inside.isEmpty()) return "[]";
                    String[] parts = inside.split("\\s*,\\s*");
                    List<Long> nums = new ArrayList<>();
                    boolean allNums = true;
                    for (String p : parts) {
                        try {
                            nums.add(Long.parseLong(p.trim()));
                        } catch (NumberFormatException e) {
                            allNums = false;
                            break;
                        }
                    }
                    if (allNums) {
                        Collections.sort(nums);
                        return nums.toString();
                    } else {
                        List<String> strs = new ArrayList<>(Arrays.asList(parts));
                        Collections.sort(strs);
                        return "[" + String.join(", ", strs) + "]";
                    }
                }

                char[] chars = trimmed.toCharArray();
                Arrays.sort(chars);
                return new String(chars);
            }
        });

        // 5. Factorial Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "factorial".equalsIgnoreCase(operation) && "integer".equalsIgnoreCase(inputType);
            }

            @Override
            public String solve(String input) {
                try {
                    long n = Long.parseLong(input.trim());
                    if (n < 0) return "Error: Negative input";
                    if (n == 0 || n == 1) return "1";
                    long res = 1;
                    for (int i = 2; i <= Math.min(n, 20); i++) {
                        res *= i;
                    }
                    return String.valueOf(res);
                } catch (Exception e) {
                    return "Error: Invalid number";
                }
            }
        });

        // 6. Prime Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "prime".equalsIgnoreCase(operation) && "integer".equalsIgnoreCase(inputType);
            }

            @Override
            public String solve(String input) {
                try {
                    long n = Long.parseLong(input.trim());
                    if (n <= 1) return "false";
                    if (n <= 3) return "true";
                    if (n % 2 == 0 || n % 3 == 0) return "false";
                    for (long i = 5; i * i <= n; i += 6) {
                        if (n % i == 0 || n % (i + 2) == 0) return "false";
                    }
                    return "true";
                } catch (Exception e) {
                    return "false";
                }
            }
        });

        // 7. Fibonacci Solver
        solvers.add(new ReferenceSolver() {
            @Override
            public boolean supports(String inputType, String operation) {
                return "fibonacci".equalsIgnoreCase(operation) && "integer".equalsIgnoreCase(inputType);
            }

            @Override
            public String solve(String input) {
                try {
                    int n = Integer.parseInt(input.trim());
                    if (n < 0) return "0";
                    if (n == 0) return "0";
                    if (n == 1) return "1";
                    long a = 0, b = 1;
                    for (int i = 2; i <= Math.min(n, 50); i++) {
                        long c = a + b;
                        a = b;
                        b = c;
                    }
                    return String.valueOf(b);
                } catch (Exception e) {
                    return "0";
                }
            }
        });
    }

    public void registerSolver(ReferenceSolver solver) {
        if (solver != null) {
            solvers.add(0, solver);
        }
    }

    public boolean canSolve(String inputType, String operation) {
        if (operation == null || "unknown".equalsIgnoreCase(operation)) return false;
        return solvers.stream().anyMatch(s -> s.supports(inputType, operation));
    }

    public String solve(String inputType, String operation, String input) {
        if (!canSolve(inputType, operation)) {
            return "Needs manual review";
        }
        for (ReferenceSolver solver : solvers) {
            if (solver.supports(inputType, operation)) {
                try {
                    return solver.solve(input);
                } catch (Exception e) {
                    return "Needs manual review";
                }
            }
        }
        return "Needs manual review";
    }
}
