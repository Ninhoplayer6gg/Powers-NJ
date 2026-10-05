package com.powersnj.core.suit;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates {@code src/main/resources} for tests running from the root build or the core
 * verification build.
 */
public final class ResourceRoot {

    private ResourceRoot() {
    }

    public static Path get() {
        String property = System.getProperty("powersnj.resourceRoot");
        if (property != null) {
            return Path.of(property);
        }
        for (Path candidate : new Path[]{Path.of("src/main/resources"), Path.of("../../src/main/resources")}) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return Path.of("src/main/resources");
    }

    public static Path sources() {
        String property = System.getProperty("powersnj.sourceRoot");
        if (property != null) {
            return Path.of(property);
        }
        for (Path candidate : new Path[]{Path.of("src/main/java"), Path.of("../../src/main/java")}) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return Path.of("src/main/java");
    }
}
