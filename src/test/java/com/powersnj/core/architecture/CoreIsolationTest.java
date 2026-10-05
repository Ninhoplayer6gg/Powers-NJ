package com.powersnj.core.architecture;

import com.powersnj.core.suit.ResourceRoot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The engine-agnostic core must never import Minecraft, Forge, Palladium or GeckoLib. This keeps
 * it unit-testable without a game and portable to future loaders/versions.
 */
class CoreIsolationTest {

    private static final List<String> FORBIDDEN = List.of("net.minecraft", "net.minecraftforge", "net.threetag", "software.bernie", "com.mojang");

    @Test
    void coreHasNoGameDependencies() throws IOException {
        Path core = ResourceRoot.sources().resolve("com/powersnj/core");
        assertTrue(Files.isDirectory(core), "missing " + core);
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(core)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("import ")) {
                        for (String forbidden : FORBIDDEN) {
                            if (trimmed.contains(forbidden)) {
                                violations.add(core.relativize(file) + ": " + trimmed);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "Core imports game classes:\n" + String.join("\n", violations));
    }
}
