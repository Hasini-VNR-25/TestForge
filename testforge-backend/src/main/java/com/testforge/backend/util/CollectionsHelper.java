package com.testforge.backend.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility showcasing bounded generics and collection operations.
 */
public class CollectionsHelper {

    /**
     * Bounded generic method that sorts items using their natural ordering
     * and returns the top K elements.
     *
     * @param items Collection of comparable items
     * @param k     Maximum number of elements to return
     * @param <T>   Any type that implements Comparable
     * @return Sorted list of top K items
     */
    public static <T extends Comparable<T>> List<T> sortTop(Collection<T> items, int k) {
        if (items == null || items.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }

        return items.stream()
                .filter(Objects::nonNull)
                .sorted()
                .limit(k)
                .collect(Collectors.toList());
    }

    /**
     * Bounded generic method to find the maximum element in any Comparable collection.
     */
    public static <T extends Comparable<T>> Optional<T> findMax(Collection<T> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        return items.stream().filter(Objects::nonNull).max(Comparable::compareTo);
    }
}
