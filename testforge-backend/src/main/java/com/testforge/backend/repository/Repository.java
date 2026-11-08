package com.testforge.backend.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface defining data-access operations.
 *
 * @param <T>  The domain entity type
 * @param <ID> The entity identifier type
 */
public interface Repository<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll();

    T save(T entity);

    boolean deleteById(ID id);

    long count();

    void clear();
}
