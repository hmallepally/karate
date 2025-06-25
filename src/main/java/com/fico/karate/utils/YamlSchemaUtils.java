package com.fico.karate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.Map;

public class YamlSchemaUtils {
    
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    public static Map<String, Object> loadSchemaAsMap(String filePath) {
        try {
            InputStream inputStream;
            if (filePath.startsWith("classpath:")) {
                String resourcePath = filePath.substring("classpath:".length());
                inputStream = YamlSchemaUtils.class.getClassLoader().getResourceAsStream(resourcePath);
                if (inputStream == null) {
                    throw new RuntimeException("Resource not found: " + resourcePath);
                }
            } else {
                inputStream = YamlSchemaUtils.class.getClassLoader().getResourceAsStream(filePath);
                if (inputStream == null) {
                    throw new RuntimeException("Resource not found: " + filePath);
                }
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = yamlMapper.readValue(inputStream, Map.class);
            return schema;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML schema from: " + filePath, e);
        }
    }
}
