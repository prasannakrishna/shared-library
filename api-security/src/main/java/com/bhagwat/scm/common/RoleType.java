package com.bhagwat.scm.common;

public enum RoleType {
    ADMIN,
    MANAGER,
    OPERATOR,
    FINANCE,
    VIEWER,
    ASSOCIATE;

    public boolean isAtLeast(RoleType required) {
        return this.ordinal() <= required.ordinal();
    }
}
