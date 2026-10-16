package com.balugaq.netex.integrations.logitech;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Target for a quantum link. AUTO checks quantum storage → cells → drawers in that order;
 * other options only check the selected storage, and linking fails if the item is not found there.
 */
public enum LinkerTarget {
    AUTO,
    QUANTUM_STORAGE,
    CELL,
    DRAWER;

    @NotNull
    public static LinkerTarget of(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return AUTO;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }

    @NotNull
    public LinkerTarget next() {
        LinkerTarget[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
