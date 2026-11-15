package com.cobrinha.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;

/**
 * Contrato mínimo das telas do aplicativo. A cena delega teclas para a tela
 * atual; cada tela decide o que consome.
 */
public interface AppScreen {

    Parent view();

    String title();

    /**
     * @return {@code true} quando a tecla foi tratada e deve ser consumida
     */
    boolean handleKey(KeyEvent event);

    default void onShown() {
    }

    default void onHidden() {
    }

    default void dispose() {
    }

    default Node initialFocus() {
        return view();
    }
}
