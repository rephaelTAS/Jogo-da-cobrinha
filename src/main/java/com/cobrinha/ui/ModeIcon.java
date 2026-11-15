package com.cobrinha.ui;

import com.cobrinha.core.GameMode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Ícones vetoriais dos modos, desenhados em canvas para ficarem nítidos em
 * qualquer escala.
 */
public final class ModeIcon {

    private ModeIcon() {
    }

    public static Canvas create(GameMode mode, double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.scale(size / 44.0, size / 44.0);

        gc.setFill(Color.web("#172027"));
        gc.fillRoundRect(0.5, 0.5, 43, 43, 11, 11);
        gc.setStroke(Color.color(1, 1, 1, 0.065));
        gc.setLineWidth(1);
        gc.strokeRoundRect(0.5, 0.5, 43, 43, 11, 11);

        switch (mode) {
            case WRAP -> drawWrap(gc);
            case CLASSIC -> drawClassic(gc);
            case MAZE -> drawMaze(gc);
        }
        gc.restore();
        return canvas;
    }

    private static void drawWrap(GraphicsContext gc) {
        gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.5));
        gc.setLineWidth(1.4);
        gc.setLineDashes(3, 3);
        gc.strokeRoundRect(6.5, 6.5, 31, 31, 8, 8);
        gc.setLineDashes();

        gc.setStroke(Palette.ACCENT);
        gc.setLineWidth(1.7);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(9, 22, 34, 22);
        gc.strokeLine(30, 18, 34.5, 22);
        gc.strokeLine(30, 26, 34.5, 22);
        gc.setFill(Palette.ACCENT);
        gc.fillOval(8, 19.5, 5, 5);
    }

    private static void drawClassic(GraphicsContext gc) {
        gc.setFill(Color.web("#222C33"));
        gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.45));
        gc.setLineWidth(1.4);
        gc.fillRoundRect(8, 12, 28, 20, 5, 5);
        gc.strokeRoundRect(8, 12, 28, 20, 5, 5);

        gc.setStroke(Color.color(1, 1, 1, 0.16));
        gc.setLineWidth(1);
        gc.strokeLine(8, 18, 36, 18);
        gc.strokeLine(8, 26, 36, 26);
        gc.strokeLine(18, 12, 18, 18);
        gc.strokeLine(27, 18, 27, 26);
        gc.strokeLine(16, 26, 16, 32);

        gc.setFill(Palette.FOOD);
        gc.fillOval(30, 21, 4, 4);
    }

    private static void drawMaze(GraphicsContext gc) {
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(5);
        gc.setStroke(Color.web("#2B363E"));
        gc.strokeLine(13, 10, 13, 26);
        gc.strokeLine(13, 26, 30, 26);
        gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.55));
        gc.setLineWidth(1.3);
        gc.strokeLine(13, 11, 13, 25);
        gc.strokeLine(14, 26, 30, 26);

        gc.setStroke(Color.web("#2B363E"));
        gc.setLineWidth(5);
        gc.strokeLine(30, 34, 30, 20);
        gc.strokeLine(30, 20, 36, 20);
        gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.55));
        gc.setLineWidth(1.3);
        gc.strokeLine(30, 33, 30, 20);
        gc.strokeLine(30, 19, 35, 19);

        gc.setFill(Palette.FOOD);
        gc.fillOval(9.5, 31, 4.5, 4.5);
    }
}
