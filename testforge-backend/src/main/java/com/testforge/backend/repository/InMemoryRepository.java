package com.testforge.backend.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Generic thread-safe in-memory repository implementation.
 *
 * @param <T>  The domain entity type
 * @param <ID> The entity identifier type
 */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {

    protected final Map<ID, T> storage = new ConcurrentHashMap<>();
    protected final Function<T, ID> idExtractor;

    public InMemoryRepository(Function<T, ID> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "ID extractor must not be null");
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot save null entity.");
        }
        ID id = idExtractor.apply(entity);
        if (id == null) {
            throw new IllegalArgumentException("Entity ID extracted was null.");
        }
        storage.put(id, entity);
        return entity;
    }

    @Override
    public boolean deleteById(ID id) {
        if (id == null) return false;
        return storage.remove(id) != null;
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
