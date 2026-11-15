package com.cobrinha.core;

/**
 * Eventos produzidos por um passo de simulação. A interface usa esses sinais
 * para som, partículas e transições de estado sem precisar adivinhar o que
 * aconteceu comparando o tabuleiro.
 */
public record TickResult(
        boolean moved,
        boolean ateSmall,
        boolean ateBig,
        boolean bigSpawned,
        boolean bigExpired,
        boolean wrapped,
        boolean mazeShifted,
        Collision collision,
        Cell head,
        boolean won) {

    public enum Collision {
        NONE,
        WALL,
        SELF,
        OBSTACLE
    }

    public static TickResult idle() {
        return new TickResult(false, false, false, false, false, false, false,
                Collision.NONE, null, false);
    }

    public boolean died() {
        return collision != Collision.NONE;
    }
}
