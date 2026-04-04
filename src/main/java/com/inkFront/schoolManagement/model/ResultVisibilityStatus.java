package com.inkFront.schoolManagement.model;

public enum ResultVisibilityStatus {

    /**
     * Completely hidden from students and parents
     */
    HIDDEN,

    /**
     * Visible only to teachers/admin (internal review stage)
     */
    STAFF_ONLY,

    /**
     * Students/parents can VIEW but NOT print
     */
    PUBLISHED,

    /**
     * Students/parents can VIEW + PRINT
     */
    PRINTABLE
}