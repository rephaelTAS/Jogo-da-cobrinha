package com.cobrinha.ui;

import com.cobrinha.audio.Sound;
import com.cobrinha.core.SnakeGame;
import java.util.function.IntConsumer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Tela de jogo: apenas o placar e o tabuleiro, como pede o modo focado.
 * Pausa e fim de jogo aparecem em sobreposições dentro do tabuleiro.
 */
public final class GameScreen extends StackPane implements AppScreen {

    private final BoardView board;
    private final String title;
    private Timeline scorePulse;

    public GameScreen(SnakeGame game, int initialBest, Sound sound,
                      Runnable onHome, IntConsumer onRecord, Runnable onSoundChanged) {
        getStyleClass().add("game-screen");
        setPadding(new Insets(22, 40, 26, 40));
        board = new BoardView(game, initialBest, sound, onHome, onRecord, onSoundChanged);
        title = "Cobrinha — " + ModeInfo.of(game.mode()).name();

        Label caption = new Label(Fonts.tracked("PONTOS"));
        caption.getStyleClass().add("game-score-label");
        Label scoreLabel = new Label(Integer.toString(board.getScore()));
        scoreLabel.getStyleClass().add("game-score");
        scoreLabel.textProperty().bind(Bindings.createStringBinding(
                () -> Integer.toString(board.getScore()), board.scoreProperty()));

        VBox scoreBlock = new VBox(1, caption, scoreLabel);
        scoreBlock.setAlignment(Pos.CENTER);
        scoreBlock.setPadding(new Insets(2, 0, 10, 0));

        BoardArea boardArea = new BoardArea(board);
        VBox.setVgrow(boardArea, Priority.ALWAYS);

        VBox content = new VBox(scoreBlock, boardArea);
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(1060);
        content.setMaxHeight(Double.MAX_VALUE);
        getChildren().add(content);

        board.scoreProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() > oldValue.intValue()) {
                scorePulse = pulse(scoreLabel, scorePulse);
            }
        });
    }

    private static Timeline pulse(Label label, Timeline previous) {
        if (previous != null) {
            previous.stop();
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(label.scaleXProperty(), 1),
                        new KeyValue(label.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(80),
                        new KeyValue(label.scaleXProperty(), 1.06, Interpolator.EASE_OUT),
                        new KeyValue(label.scaleYProperty(), 1.06, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(210),
                        new KeyValue(label.scaleXProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(label.scaleYProperty(), 1, Interpolator.EASE_BOTH)));
        timeline.play();
        return timeline;
    }

    @Override
    public Parent view() {
        return this;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public void onShown() {
        board.startNewGame();
        board.requestFocus();
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        return board.handleKey(event.getCode());
    }

    @Override
    public Node initialFocus() {
        return board;
    }

    @Override
    public void dispose() {
        board.dispose();
    }

    public void pauseIfRunning() {
        board.pauseIfRunning();
    }

    /**
     * Área que mantém o tabuleiro quadrado e centralizado no espaço restante.
     */
    private static final class BoardArea extends Region {

        private final BoardView board;

        BoardArea(BoardView board) {
            this.board = board;
            getChildren().add(board);
        }

        @Override
        protected void layoutChildren() {
            double side = Math.floor(Math.min(getWidth(), getHeight()));
            side = Math.max(220, side);
            double x = (getWidth() - side) / 2;
            double y = (getHeight() - side) / 2;
            board.resizeRelocate(x, y, side, side);
        }

        @Override
        protected double computeMinWidth(double height) {
            return 220;
        }

        @Override
        protected double computeMinHeight(double width) {
            return 220;
        }

        @Override
        protected double computePrefWidth(double height) {
            return 700;
        }

        @Override
        protected double computePrefHeight(double width) {
            return 700;
        }
    }
}
