package com.fico.karate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class YamlSchemaUtils {
    
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    
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
    
    /**
     * Populates a JSON template with values from CSV data using dot-notation paths
     * @param templateJson The base JSON template as a Map
     * @param csvRow Map containing CSV data with dot-notation keys (e.g., "application.applicationId")
     * @return Populated JSON as Map
     */
    public static Map<String, Object> populateJsonFromCsv(Map<String, Object> templateJson, Map<String, String> csvRow) {
        try {
            String jsonString = jsonMapper.writeValueAsString(templateJson);
            Map<String, Object> populatedJson = jsonMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            
            for (Map.Entry<String, String> entry : csvRow.entrySet()) {
                String path = entry.getKey();
                String value = entry.getValue();
                
                if (path.startsWith("response.") || value == null || value.trim().isEmpty()) {
                    continue;
                }
                
                setValueAtPath(populatedJson, path, value);
            }
            
            return populatedJson;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to populate JSON from CSV data", e);
        }
    }
    
    /**
     * Validates response JSON against expected values from CSV using dot-notation paths
     * @param responseJson The response JSON as a Map
     * @param csvRow Map containing CSV data with "response." prefixed keys
     * @return Map containing validation results (path -> result)
     */
    public static Map<String, ValidationResult> validateResponseFromCsv(Map<String, Object> responseJson, Map<String, String> csvRow) {
        Map<String, ValidationResult> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : csvRow.entrySet()) {
            String path = entry.getKey();
            String expectedValue = entry.getValue();
            
            if (!path.startsWith("response.") || expectedValue == null || expectedValue.trim().isEmpty()) {
                continue;
            }
            
            String jsonPath = path.substring("response.".length());
            
            try {
                Object actualValue = getValueAtPath(responseJson, jsonPath);
                ValidationResult result = validateValue(actualValue, expectedValue, jsonPath);
                results.put(path, result);
                
            } catch (Exception e) {
                results.put(path, new ValidationResult(false, null, expectedValue, 
                    "Error accessing path: " + e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * Sets a value at the specified dot-notation path in a nested Map structure
     * Supports array notation like "applicants[0].firstName"
     */
    private static void setValueAtPath(Map<String, Object> json, String path, String value) {
        String[] parts = parsePath(path);
        Object current = json;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (isArrayAccess(part)) {
                ArrayAccess access = parseArrayAccess(part);
                current = navigateToArrayElement(current, access, true);
            } else {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) current;
                    
                    if (!currentMap.containsKey(part)) {
                        if (i + 1 < parts.length && isArrayAccess(parts[i + 1])) {
                            currentMap.put(part, new ArrayList<>());
                        } else {
                            currentMap.put(part, new HashMap<String, Object>());
                        }
                    }
                    current = currentMap.get(part);
                } else {
                    throw new RuntimeException("Cannot navigate to path: " + path + " at part: " + part);
                }
            }
        }
        
        String finalPart = parts[parts.length - 1];
        if (isArrayAccess(finalPart)) {
            ArrayAccess access = parseArrayAccess(finalPart);
            setArrayElementValue(current, access, convertValue(value));
        } else {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                currentMap.put(finalPart, convertValue(value));
            } else {
                throw new RuntimeException("Cannot set value at path: " + path);
            }
        }
    }
    
    /**
     * Gets a value at the specified dot-notation path from a nested Map structure
     */
    private static Object getValueAtPath(Map<String, Object> json, String path) {
        String[] parts = parsePath(path);
        Object current = json;
        
        for (String part : parts) {
            if (isArrayAccess(part)) {
                ArrayAccess access = parseArrayAccess(part);
                current = navigateToArrayElement(current, access, false);
            } else {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) current;
                    current = currentMap.get(part);
                } else {
                    return null;
                }
            }
            
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Parses a dot-notation path into individual parts
     */
    private static String[] parsePath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBrackets = false;
        
        for (char c : path.toCharArray()) {
            if (c == '[') {
                inBrackets = true;
                current.append(c);
            } else if (c == ']') {
                inBrackets = false;
                current.append(c);
            } else if (c == '.' && !inBrackets) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Checks if a path part contains array access notation
     */
    private static boolean isArrayAccess(String part) {
        return part.contains("[") && part.contains("]");
    }
    
    /**
     * Parses array access notation like "applicants[0]"
     */
    private static ArrayAccess parseArrayAccess(String part) {
        Pattern pattern = Pattern.compile("([^\\[]+)\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(part);
        
        if (matcher.matches()) {
            String fieldName = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            return new ArrayAccess(fieldName, index);
        } else {
            throw new RuntimeException("Invalid array access notation: " + part);
        }
    }
    
    /**
     * Navigates to an array element, creating the structure if needed
     */
    private static Object navigateToArrayElement(Object current, ArrayAccess access, boolean createIfMissing) {
        if (current instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> currentMap = (Map<String, Object>) current;
            
            Object arrayObj = currentMap.get(access.fieldName);
            if (arrayObj == null && createIfMissing) {
                arrayObj = new ArrayList<>();
                currentMap.put(access.fieldName, arrayObj);
            }
            
            if (arrayObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) arrayObj;
                
                while (list.size() <= access.index && createIfMissing) {
                    list.add(new HashMap<String, Object>());
                }
                
                if (access.index < list.size()) {
                    return list.get(access.index);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Sets a value in an array element
     */
    private static void setArrayElementValue(Object current, ArrayAccess access, Object value) {
        if (current instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> currentMap = (Map<String, Object>) current;
            
            Object arrayObj = currentMap.get(access.fieldName);
            if (arrayObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) arrayObj;
                
                while (list.size() <= access.index) {
                    list.add(new HashMap<String, Object>());
                }
                
                list.set(access.index, value);
            }
        }
    }
    
    /**
     * Converts string values to appropriate types
     */
    private static Object convertValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }
            
            return value;
        }
    }
    
    /**
     * Validates an actual value against an expected value
     */
    private static ValidationResult validateValue(Object actualValue, String expectedValue, String path) {
        try {
            Object expectedObj = convertValue(expectedValue);
            
            boolean isValid = Objects.equals(actualValue, expectedObj);
            
            return new ValidationResult(isValid, actualValue, expectedObj, 
                isValid ? "Match" : "Value mismatch");
                
        } catch (Exception e) {
            return new ValidationResult(false, actualValue, expectedValue, 
                "Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Helper class for array access parsing
     */
    private static class ArrayAccess {
        final String fieldName;
        final int index;
        
        ArrayAccess(String fieldName, int index) {
            this.fieldName = fieldName;
            this.index = index;
        }
    }
    
    /**
     * Result of a validation operation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Object actualValue;
        private final Object expectedValue;
        private final String message;
        
        public ValidationResult(boolean valid, Object actualValue, Object expectedValue, String message) {
            this.valid = valid;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public Object getActualValue() { return actualValue; }
        public Object getExpectedValue() { return expectedValue; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, actual=%s, expected=%s, message='%s'}", 
                valid, actualValue, expectedValue, message);
        }
    }
}
