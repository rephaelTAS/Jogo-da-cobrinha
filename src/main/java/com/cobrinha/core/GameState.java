package com.cobrinha.core;

/**
 * Estados possíveis do jogo. A interface reage a cada um deles com uma sobreposição
 * diferente (tela inicial, pausa, fim de jogo ou vitória).
 */
public enum GameState {
    READY,
    RUNNING,
    PAUSED,
    GAME_OVER,
    WON
}
