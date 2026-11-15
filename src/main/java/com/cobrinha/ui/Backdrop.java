package com.cobrinha.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Fundo do aplicativo: brilho verde suave no topo, calor coral no canto
 * inferior e vinheta discreta. É desenhado uma vez por tamanho.
 */
public final class Backdrop extends Region {

    private final Canvas canvas = new Canvas();

    public Backdrop() {
        setMouseTransparent(true);
        getChildren().add(canvas);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        canvas.setWidth(width);
        canvas.setHeight(height);
        canvas.relocate(0, 0);
        draw(width, height);
    }

    private void draw(double width, double height) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.setFill(Palette.BACKGROUND);
        gc.fillRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, 0.5, 0.03, 0.85, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.39, 0.95, 0.75, 0.065)),
                new Stop(0.45, Color.color(0.39, 0.95, 0.75, 0.018)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, 0.88, 1.02, 0.62, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1.0, 0.48, 0.42, 0.028)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, 0.5, 0.46, 0.78, true,
                CycleMethod.NO_CYCLE,
                new Stop(0.5, Color.TRANSPARENT),
                new Stop(1, Color.color(0, 0, 0, 0.4))));
        gc.fillRect(0, 0, width, height);
    }
}
