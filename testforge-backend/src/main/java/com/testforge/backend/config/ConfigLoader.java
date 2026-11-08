package com.testforge.backend.config;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Thread-safe configuration manager loading testforge.properties via java.util.Properties.
 * Demonstrates configuration management and property encapsulation.
 */
@Component
public class ConfigLoader {

    private static final String CONFIG_FILE = "testforge.properties";
    private static ConfigLoader instance;

    private final Properties properties = new Properties();

    public ConfigLoader() {
        loadProperties();
        synchronized (ConfigLoader.class) {
            instance = this;
        }
    }

    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
            } else {
                System.err.println("Warning: " + CONFIG_FILE + " not found on classpath, using hardcoded defaults.");
            }
        } catch (Exception e) {
            System.err.println("Error loading " + CONFIG_FILE + ": " + e.getMessage());
        }
    }

    public String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    public List<String> getListProperty(String key) {
        String val = properties.getProperty(key);
        if (val == null || val.isBlank()) return Collections.emptyList();
        return Arrays.stream(val.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Map<String, String> getAllProperties() {
        Map<String, String> map = new LinkedHashMap<>();
        properties.forEach((k, v) -> map.put(String.valueOf(k), String.valueOf(v)));
        return Collections.unmodifiableMap(map);
    }
}
