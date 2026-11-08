package com.testforge.backend.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Generic file-based repository persisting entities to disk in JSON format.
 * Demonstrates generics, file I/O, and serialization.
 *
 * @param <T>  The domain entity type
 * @param <ID> The entity identifier type
 */
public class JsonFileRepository<T, ID> implements Repository<T, ID> {

    private final Path storagePath;
    private final Class<T> entityClass;
    private final Function<T, ID> idExtractor;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonFileRepository(Path storagePath, Class<T> entityClass, Function<T, ID> idExtractor) {
        this.storagePath = Objects.requireNonNull(storagePath, "Path cannot be null");
        this.entityClass = Objects.requireNonNull(entityClass, "Entity class cannot be null");
        this.idExtractor = Objects.requireNonNull(idExtractor, "ID extractor cannot be null");
        initStorage();
    }

    private synchronized void initStorage() {
        try {
            if (Files.notExists(storagePath)) {
                if (storagePath.getParent() != null) {
                    Files.createDirectories(storagePath.getParent());
                }
                Files.writeString(storagePath, "[]");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize JSON repository: " + storagePath, e);
        }
    }

    private synchronized List<T> readAllFromFile() {
        try {
            if (!Files.exists(storagePath) || Files.size(storagePath) == 0) {
                return new ArrayList<>();
            }
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, entityClass);
            return mapper.readValue(storagePath.toFile(), type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private synchronized void writeAllToFile(List<T> list) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), list);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist JSON repository: " + storagePath, e);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        return readAllFromFile().stream()
                .filter(item -> Objects.equals(idExtractor.apply(item), id))
                .findFirst();
    }

    @Override
    public List<T> findAll() {
        return readAllFromFile();
    }

    @Override
    public synchronized T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot save null entity.");
        }
        ID id = idExtractor.apply(entity);
        List<T> all = readAllFromFile();
        all.removeIf(item -> Objects.equals(idExtractor.apply(item), id));
        all.add(entity);
        writeAllToFile(all);
        return entity;
    }

    @Override
    public synchronized boolean deleteById(ID id) {
        if (id == null) return false;
        List<T> all = readAllFromFile();
        boolean removed = all.removeIf(item -> Objects.equals(idExtractor.apply(item), id));
        if (removed) {
            writeAllToFile(all);
        }
        return removed;
    }

    @Override
    public long count() {
        return readAllFromFile().size();
    }

    @Override
    public synchronized void clear() {
        writeAllToFile(new ArrayList<>());
    }
}
