package com.example.billard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps every Java source line within the agreed 120 characters, so code reads without sideways scrolling. */
class SourceLineLengthTest {

    private static final int MAX_LINE_LENGTH = 120;

    @Test
    void sourceLinesStayWithinTheLimit() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/test/java"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).length() > MAX_LINE_LENGTH) {
                            offenders.add(file + ":" + (i + 1));
                        }
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Lines longer than " + MAX_LINE_LENGTH + " characters: " + offenders);
    }
}
