package com.cobrinha.ui;

import java.io.InputStream;
import javafx.scene.text.Font;

/**
 * Carrega a fonte empacotada com o jogo. Fira Sans garante a mesma tipografia
 * em qualquer sistema — sem depender das fontes instaladas na máquina.
 */
public final class Fonts {

    private static final String[] FILES = {
        "/fonts/fira-sans-regular.ttf",
        "/fonts/fira-sans-medium.ttf",
        "/fonts/fira-sans-semibold.ttf",
        "/fonts/fira-sans-bold.ttf"
    };

    public static final String FAMILY = "Fira Sans";

    private Fonts() {
    }

    public static void load() {
        for (String file : FILES) {
            try (InputStream in = Fonts.class.getResourceAsStream(file)) {
                if (in != null) {
                    Font.loadFont(in, 12);
                }
            } catch (Exception ignored) {
                // A interface usa a fonte padrão se o recurso não estiver disponível.
            }
        }
    }

    /**
     * Insere espaços finos entre as letras para simular o letter-spacing que o
     * CSS do JavaFX ainda não oferece. Usado apenas em rótulos curtos.
     */
    public static String tracked(String text) {
        StringBuilder builder = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                builder.append('\u2009');
            }
            builder.append(text.charAt(i));
        }
        return builder.toString();
    }
}
