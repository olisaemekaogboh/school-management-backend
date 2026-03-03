// src/main/java/com/inkFront/schoolManagement/utils/ClassProgression.java
package com.inkFront.schoolManagement.utils;

import java.util.HashMap;
import java.util.Map;

public class ClassProgression {

    private static final Map<String, String> NEXT_CLASS_MAP = new HashMap<>();

    static {
        // Nursery progression
        NEXT_CLASS_MAP.put("Nursery 1", "Nursery 2");
        NEXT_CLASS_MAP.put("Nursery 2", "Kindergarten 1");

        // Kindergarten progression
        NEXT_CLASS_MAP.put("Kindergarten 1", "Kindergarten 2");
        NEXT_CLASS_MAP.put("Kindergarten 2", "Primary 1");

        // Primary progression
        NEXT_CLASS_MAP.put("Primary 1", "Primary 2");
        NEXT_CLASS_MAP.put("Primary 2", "Primary 3");
        NEXT_CLASS_MAP.put("Primary 3", "Primary 4");
        NEXT_CLASS_MAP.put("Primary 4", "Primary 5");
        NEXT_CLASS_MAP.put("Primary 5", "Primary 6");
        NEXT_CLASS_MAP.put("Primary 6", "JSS 1");

        // Junior Secondary progression
        NEXT_CLASS_MAP.put("JSS 1", "JSS 2");
        NEXT_CLASS_MAP.put("JSS 2", "JSS 3");
        NEXT_CLASS_MAP.put("JSS 3", "SSS 1");

        // Senior Secondary progression
        NEXT_CLASS_MAP.put("SSS 1", "SSS 2");
        NEXT_CLASS_MAP.put("SSS 2", "SSS 3");
        NEXT_CLASS_MAP.put("SSS 3", "GRADUATED");
    }

    public static String getNextClass(String currentClass) {
        return NEXT_CLASS_MAP.getOrDefault(currentClass, currentClass);
    }

    public static boolean isGraduatingClass(String currentClass) {
        return "SSS 3".equals(currentClass);
    }
}