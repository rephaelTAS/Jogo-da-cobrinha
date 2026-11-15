package com.cobrinha.ui;

import com.cobrinha.core.GameMode;
import java.util.List;

/**
 * Textos de apresentação dos modos. O motor conhece apenas o comportamento;
 * nomes e descrições ficam na camada de interface.
 */
public record ModeInfo(GameMode mode, String name, String description) {

    public static ModeInfo of(GameMode mode) {
        return switch (mode) {
            case WRAP -> new ModeInfo(mode, "Atravessa",
                    "Saia por um lado e volte pelo outro. Sem paredes.");
            case CLASSIC -> new ModeInfo(mode, "Clássico",
                    "As paredes são o limite. Cada curva conta.");
            case MAZE -> new ModeInfo(mode, "Labirinto",
                    "Desvie dos blocos. A cada volta, o labirinto se move.");
        };
    }

    public static List<ModeInfo> all() {
        return List.of(of(GameMode.WRAP), of(GameMode.CLASSIC), of(GameMode.MAZE));
    }
}
