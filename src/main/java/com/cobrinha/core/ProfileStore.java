package com.cobrinha.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Lê e grava o perfil do jogador em um arquivo de propriedades. Falhas de
 * leitura/escrita nunca impedem o jogo de rodar.
 */
public final class ProfileStore {

    private static final String SOUND_KEY = "sound.enabled";

    private final Path file;

    public ProfileStore(Path file) {
        this.file = file;
    }

    /**
     * Arquivo padrão do usuário, respeitando o XDG Base Directory no Linux.
     * A propriedade {@code cobrinha.config.dir} permite um diretório alternativo
     * (usada pelos testes e pelo modo de captura de telas).
     */
    public static ProfileStore createDefault() {
        String override = System.getProperty("cobrinha.config.dir");
        if (override != null && !override.isBlank()) {
            return new ProfileStore(Path.of(override).resolve("config.properties"));
        }
        String configHome = System.getenv("XDG_CONFIG_HOME");
        Path base;
        if (configHome != null && !configHome.isBlank()) {
            base = Path.of(configHome);
        } else {
            base = Path.of(System.getProperty("user.home"), ".config");
        }
        return new ProfileStore(base.resolve("cobrinha").resolve("config.properties"));
    }

    public PlayerProfile load() {
        PlayerProfile profile = new PlayerProfile();
        if (!Files.isRegularFile(file)) {
            return profile;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | RuntimeException e) {
            return profile;
        }

        for (GameMode mode : GameMode.values()) {
            profile.recordBest(mode, readScore(properties.getProperty("record." + mode.id())));
        }
        profile.setSoundEnabled(readBoolean(properties.getProperty(SOUND_KEY), true));
        return profile;
    }

    public void save(PlayerProfile profile) {
        if (profile == null) {
            return;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Properties properties = new Properties();
            for (GameMode mode : GameMode.values()) {
                properties.setProperty("record." + mode.id(), Integer.toString(profile.best(mode)));
            }
            properties.setProperty(SOUND_KEY, Boolean.toString(profile.isSoundEnabled()));

            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "Cobrinha - perfil do jogador");
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException ignored) {
            // Sem perfil salvo o jogo continua perfeitamente jogável.
        }
    }

    public Path file() {
        return file;
    }

    private static int readScore(String raw) {
        if (raw == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean readBoolean(String raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim().toLowerCase();
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        return fallback;
    }
}
