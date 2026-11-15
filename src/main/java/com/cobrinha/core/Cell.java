package com.cobrinha.core;

/**
 * Uma célula do tabuleiro, identificada por coluna ({@code x}) e linha ({@code y}).
 * O canto superior esquerdo do tabuleiro é {@code (0, 0)}.
 */
public record Cell(int x, int y) {

    /**
     * Devolve a célula vizinha na direção informada.
     */
    public Cell translate(Direction direction) {
        return new Cell(x + direction.dx(), y + direction.dy());
    }

    /**
     * Indica se a célula está dentro de um tabuleiro com as dimensões dadas.
     */
    public boolean isInside(int columns, int rows) {
        return x >= 0 && y >= 0 && x < columns && y < rows;
    }
}
