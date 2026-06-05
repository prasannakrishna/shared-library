package com.bhagwat.scm.common;
public enum SubscriptionType {
    TRIAL(1),
    GOLD(2),
    DIAMOND(3);

    private final int level;

    SubscriptionType(int level) {
        this.level = level;
    }

    public boolean isAtLeast(SubscriptionType required) {
        return this.level >= required.level;
    }
}
