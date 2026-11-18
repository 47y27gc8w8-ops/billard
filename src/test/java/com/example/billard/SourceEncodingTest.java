package com.example.billard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps every source file plain ASCII, so the game's text comes out right whichever encoding a compiler or IDE
 * reads the files with. Symbols such as arrows belong in strings as unicode escapes (backslash, u, four hex digits).
 */
class SourceEncodingTest {

    @Test
    void sourceFilesArePlainAscii() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/test/java"), Path.of("src/main/resources"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(SourceEncodingTest::isText).toList()) {
                    byte[] bytes = Files.readAllBytes(file);
                    for (int i = 0; i < bytes.length; i++) {
                        if (bytes[i] < 0) {
                            offenders.add(file + " at byte " + i);
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Non-ASCII characters in: " + offenders);
    }

    private static boolean isText(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".css") || name.endsWith(".fxml")
                || name.endsWith(".properties");
    }
}
