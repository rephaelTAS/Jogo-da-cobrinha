package com.cobrinha.ui;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;

/**
 * Pequenos ajustes de teclado para os botões. O JavaFX ativa botões com Espaço;
 * aqui acrescentamos Enter, que é o que o jogador espera em um jogo.
 */
public final class Buttons {

    private Buttons() {
    }

    public static Button enterActivates(Button button) {
        button.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                button.fire();
                event.consume();
            }
        });
        return button;
    }
}
