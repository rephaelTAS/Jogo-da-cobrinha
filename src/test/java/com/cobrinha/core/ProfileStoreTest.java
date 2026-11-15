package com.cobrinha.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileGivesDefaultProfile() {
        PlayerProfile profile = new ProfileStore(tempDir.resolve("nao-existe")).load();

        for (GameMode mode : GameMode.values()) {
            assertEquals(0, profile.best(mode));
        }
        assertTrue(profile.isSoundEnabled());
    }

    @Test
    void savesRecordsPerModeAndSound() {
        ProfileStore store = new ProfileStore(tempDir.resolve("sub/config.properties"));
        PlayerProfile profile = new PlayerProfile();
        profile.recordBest(GameMode.WRAP, 24);
        profile.recordBest(GameMode.CLASSIC, 12);
        profile.recordBest(GameMode.MAZE, 30);
        profile.setSoundEnabled(false);

        store.save(profile);
        PlayerProfile loaded = new ProfileStore(store.file()).load();

        assertEquals(24, loaded.best(GameMode.WRAP));
        assertEquals(12, loaded.best(GameMode.CLASSIC));
        assertEquals(30, loaded.best(GameMode.MAZE));
        assertFalse(loaded.isSoundEnabled());
        assertEquals(30, loaded.overallBest());
    }

    @Test
    void invalidContentFallsBackGracefully() throws Exception {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "record.wrap=abc\nrecord.classic=-5\nsound.enabled=maybe\n");

        PlayerProfile profile = new ProfileStore(file).load();

        assertEquals(0, profile.best(GameMode.WRAP));
        assertEquals(0, profile.best(GameMode.CLASSIC));
        assertTrue(profile.isSoundEnabled());
    }

    @Test
    void overwritesAndResetsRecords() {
        ProfileStore store = new ProfileStore(tempDir.resolve("config.properties"));
        PlayerProfile profile = new PlayerProfile();
        profile.recordBest(GameMode.MAZE, 40);
        store.save(profile);

        profile.resetRecords();
        store.save(profile);

        PlayerProfile loaded = store.load();
        assertEquals(0, loaded.best(GameMode.MAZE));
        assertTrue(loaded.isSoundEnabled());
    }

    @Test
    void recordingOnlyImprovesTheBest() {
        PlayerProfile profile = new PlayerProfile();
        assertTrue(profile.recordBest(GameMode.WRAP, 5));
        assertFalse(profile.recordBest(GameMode.WRAP, 3));
        assertEquals(5, profile.best(GameMode.WRAP));
    }
}
