package com.yzh.yingshi.common.auth;

import java.util.Set;

public final class UserRole {

    public static final String ADMIN = "ADMIN";
    public static final String OPERATOR = "OPERATOR";
    public static final String VIEWER = "VIEWER";

    private static final Set<String> WRITE_ROLES = Set.of(ADMIN, OPERATOR);
    private static final Set<String> KNOWN_ROLES = Set.of(ADMIN, OPERATOR, VIEWER);

    private UserRole() {
    }

    public static boolean canWrite(String role) {
        return WRITE_ROLES.contains(role);
    }

    public static boolean isKnown(String role) {
        return KNOWN_ROLES.contains(role);
    }
}
