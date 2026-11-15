package com.cobrinha.core;

/**
 * Direções possíveis de movimento. A ordem dos fatores é irrelevante para o jogo,
 * mas os deltas são o único lugar onde o sinal de cada eixo é definido.
 */
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    /**
     * Direção contrária, que nunca pode ser aplicada de uma vez.
     */
    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }
}
