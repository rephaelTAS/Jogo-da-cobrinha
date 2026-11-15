package com.cobrinha.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Sobreposições de pausa, fim de jogo e vitória. O cartão é reconstruído a
 * cada estado; os botões delegam para as ações definidas pelo tabuleiro.
 */
public final class OverlayLayer extends StackPane {

    private static final Duration FADE_IN = Duration.millis(190);
    private static final Duration FADE_OUT = Duration.millis(150);
    private static final int CARD_WIDTH = 400;

    private final Region backdrop = new Region();
    private final StackPane cardHost = new StackPane();

    private Runnable primaryAction = () -> { };
    private Runnable secondaryAction = () -> { };
    private FadeTransition fade;
    private TranslateTransition slide;

    public OverlayLayer() {
        getStyleClass().add("overlay");
        backdrop.getStyleClass().add("overlay-backdrop");
        cardHost.setPadding(new Insets(22));
        cardHost.setAlignment(Pos.CENTER);
        getChildren().addAll(backdrop, cardHost);
        setVisible(false);
        setOpacity(0);
    }

    public void setActions(Runnable primaryAction, Runnable secondaryAction) {
        this.primaryAction = primaryAction == null ? () -> { } : primaryAction;
        this.secondaryAction = secondaryAction == null ? () -> { } : secondaryAction;
    }

    public void showPaused() {
        VBox content = card();
        content.getChildren().addAll(
                eyebrow("JOGO PAUSADO", false),
                spacer(10),
                title("Um instante."),
                spacer(8),
                subtitle("A cobrinha espera."),
                spacer(22),
                primaryButton("Continuar"),
                spacer(10),
                secondaryButton("Menu inicial"),
                spacer(13),
                hint("Esc também retoma"));
        show(content);
    }

    public void showGameOver(int score, int best, boolean newRecord, String reason) {
        VBox content = card();
        content.getChildren().addAll(
                eyebrow("FIM DE JOGO", true),
                spacer(10),
                bigScore(score),
                trackedLabel("PONTOS", "overlay-score-label"),
                spacer(8),
                subtitle(reason),
                spacer(16),
                recordRow(best, newRecord),
                spacer(20),
                primaryButton("Jogar novamente"),
                spacer(10),
                secondaryButton("Menu inicial"),
                spacer(13),
                hint("Enter para recomeçar"));
        show(content);
    }

    public void showWon(int score, int best, boolean newRecord) {
        VBox content = card();
        content.getChildren().addAll(
                eyebrow("TABULEIRO COMPLETO", false),
                spacer(8),
                title("Você venceu!"),
                spacer(5),
                subtitle("Não sobrou uma única célula livre."),
                spacer(16),
                bigScore(score),
                trackedLabel("PONTOS", "overlay-score-label"),
                spacer(16),
                recordRow(best, newRecord),
                spacer(20),
                primaryButton("Jogar novamente"),
                spacer(10),
                secondaryButton("Menu inicial"),
                spacer(13),
                hint("Enter para recomeçar"));
        show(content);
    }

    public void dispose() {
        stopAnimations();
        setVisible(false);
    }

    public void hide() {
        if (!isVisible()) {
            return;
        }
        stopAnimations();
        fade = new FadeTransition(FADE_OUT, this);
        fade.setFromValue(getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(event -> setVisible(false));
        fade.play();
    }

    private void show(VBox content) {
        stopAnimations();
        cardHost.getChildren().setAll(content);
        setVisible(true);
        setOpacity(0);
        content.setTranslateY(10);

        fade = new FadeTransition(FADE_IN, this);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        slide = new TranslateTransition(Duration.millis(250), content);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.play();
    }

    private void stopAnimations() {
        if (fade != null) {
            fade.stop();
            fade = null;
        }
        if (slide != null) {
            slide.stop();
            slide = null;
        }
    }

    private VBox card() {
        VBox box = new VBox();
        box.getStyleClass().add("overlay-card");
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(CARD_WIDTH);
        box.setMinWidth(CARD_WIDTH);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setMinHeight(Region.USE_PREF_SIZE);
        return box;
    }

    private Label eyebrow(String text, boolean danger) {
        Label label = new Label(Fonts.tracked(text));
        label.getStyleClass().add("overlay-eyebrow");
        if (danger) {
            label.getStyleClass().add("danger");
        }
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("overlay-title");
        label.setWrapText(true);
        label.setMaxWidth(CARD_WIDTH - 72);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private Label subtitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("overlay-subtitle");
        label.setWrapText(true);
        label.setMaxWidth(CARD_WIDTH - 72);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private Label trackedLabel(String text, String styleClass) {
        Label label = new Label(Fonts.tracked(text));
        label.getStyleClass().add(styleClass);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private Label bigScore(int score) {
        Label label = new Label(Integer.toString(score));
        label.getStyleClass().add("overlay-score");
        return label;
    }

    private HBox recordRow(int best, boolean newRecord) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        Label record = new Label(Fonts.tracked("RECORDE") + "  " + best);
        record.getStyleClass().add("overlay-record");
        row.getChildren().add(record);
        if (newRecord) {
            Label chip = new Label(Fonts.tracked("NOVO RECORDE"));
            chip.getStyleClass().add("record-chip");
            row.getChildren().add(chip);
        }
        return row;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);
        button.setOnAction(event -> primaryAction.run());
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);
        button.setOnAction(event -> secondaryAction.run());
        return button;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("overlay-hint");
        return label;
    }

    private Region spacer(double height) {
        Region region = new Region();
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
        VBox.setVgrow(region, Priority.NEVER);
        return region;
    }
}
