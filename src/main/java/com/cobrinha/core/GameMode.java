package com.cobrinha.core;

import java.util.Locale;

/**
 * Os três modos de jogo, inspirados no clássico da cobrinha.
 */
public enum GameMode {

    /** Atravessa as bordas e reaparece do outro lado. */
    WRAP,

    /** As paredes matam. O modo clássico. */
    CLASSIC,

    /** Atravessa as bordas, mas há obstáculos que mudam de lugar a cada volta. */
    MAZE;

    public boolean wraps() {
        return this != CLASSIC;
    }

    public boolean hasMaze() {
        return this == MAZE;
    }

    /** Chave estável usada na persistência. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
