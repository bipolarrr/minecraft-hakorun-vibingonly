package com.example.hakorun.model;

public enum LifePolicy {
    SHARED_POOL("공유목숨"),
    PER_PLAYER("인당 데스");

    private final String displayName;

    LifePolicy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Accepts both technical names (SHARED_POOL, PER_PLAYER) and friendly aliases. */
    public static LifePolicy fromInput(String input) {
        for (LifePolicy p : values()) {
            if (p.name().equalsIgnoreCase(input) || p.displayName.equalsIgnoreCase(input)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown policy: " + input);
    }
}
