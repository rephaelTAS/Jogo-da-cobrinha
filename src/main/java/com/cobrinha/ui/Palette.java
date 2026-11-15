package com.cobrinha.ui;

import javafx.scene.paint.Color;

/**
 * Paleta central do jogo. Manter as cores em um só lugar evita que o CSS, o
 * canvas e a interface divirjam com o tempo.
 */
public final class Palette {

    public static final Color BACKGROUND = Color.web("#090B0E");
    public static final Color SURFACE = Color.web("#12171D");
    public static final Color SURFACE_RAISED = Color.web("#161D24");

    public static final Color TEXT = Color.web("#ECF2F7");
    public static final Color MUTED = Color.web("#8A95A1");
    public static final Color FAINT = Color.web("#5B6672");

    public static final Color ACCENT = Color.web("#63F2C0");
    public static final Color ACCENT_LIGHT = Color.web("#AAFFE3");
    public static final Color ACCENT_DARK = Color.web("#1E8A6B");

    public static final Color FOOD = Color.web("#FF7A6B");
    public static final Color FOOD_LIGHT = Color.web("#FFB2A6");
    public static final Color DANGER = Color.web("#FF6B62");

    public static final Color GOLD = Color.web("#FFC15E");
    public static final Color GOLD_LIGHT = Color.web("#FFE0A3");
    public static final Color GOLD_DARK = Color.web("#E09A32");

    public static final Color OBSTACLE = Color.web("#242F37");
    public static final Color OBSTACLE_LIGHT = Color.web("#3A4852");

    public static final Color GRID_DOT = Color.rgb(255, 255, 255, 0.07);
    public static final Color DEAD_BODY = Color.web("#4F5F5B");
    public static final Color DEAD_TAIL = Color.web("#2E3A37");

    private Palette() {
    }
}
