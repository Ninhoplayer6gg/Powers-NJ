package com.powersnj.core.config;

import java.util.Objects;

/**
 * Holder of the currently active {@link PowersSettings}. Starts with {@link PowersSettings#DEFAULTS}
 * so nothing ever reads an unloaded Forge config value.
 */
public final class Settings {

    private static volatile PowersSettings current = PowersSettings.DEFAULTS;

    private Settings() {
    }

    public static PowersSettings get() {
        return current;
    }

    public static void update(PowersSettings settings) {
        current = Objects.requireNonNull(settings);
    }

    public static void reset() {
        current = PowersSettings.DEFAULTS;
    }
}
