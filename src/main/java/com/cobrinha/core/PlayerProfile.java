package com.cobrinha.core;

import java.util.EnumMap;

/**
 * Preferências e recordes do jogador. Objeto pequeno, mantido na thread da
 * interface e persistido por {@link ProfileStore}.
 */
public final class PlayerProfile {

    private final EnumMap<GameMode, Integer> bestScores = new EnumMap<>(GameMode.class);
    private boolean soundEnabled = true;

    public PlayerProfile() {
        for (GameMode mode : GameMode.values()) {
            bestScores.put(mode, 0);
        }
    }

    public int best(GameMode mode) {
        return bestScores.getOrDefault(mode, 0);
    }

    /**
     * Registra um placar se ele for melhor que o atual.
     *
     * @return {@code true} quando o recorde mudou
     */
    public boolean recordBest(GameMode mode, int score) {
        if (score < 0) {
            throw new IllegalArgumentException("O placar não pode ser negativo");
        }
        if (score > best(mode)) {
            bestScores.put(mode, score);
            return true;
        }
        return false;
    }

    public int overallBest() {
        int best = 0;
        for (int value : bestScores.values()) {
            best = Math.max(best, value);
        }
        return best;
    }

    public void resetRecords() {
        for (GameMode mode : GameMode.values()) {
            bestScores.put(mode, 0);
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }
}
