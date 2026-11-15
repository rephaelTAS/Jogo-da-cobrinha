package com.cobrinha.ui;

import com.cobrinha.audio.Sound;
import java.util.function.Consumer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Configurações: efeitos sonoros e limpeza dos recordes, com confirmação
 * própria para não depender de diálogos nativos.
 */
public final class SettingsScreen extends StackPane implements AppScreen {

    private final Sound sound;
    private final Runnable onBack;
    private final Consumer<Boolean> onSoundChanged;
    private final Runnable onResetRecords;

    private final Button backButton;
    private final Label feedback = new Label();
    private final StackPane modalLayer = new StackPane();
    private final Button cancelButton;
    private PauseTransition feedbackTimer;
    private FadeTransition feedbackFade;
    private FadeTransition modalFade;

    public SettingsScreen(Sound sound, Consumer<Boolean> onSoundChanged,
                          Runnable onResetRecords, Runnable onBack) {
        this.sound = sound;
        this.onSoundChanged = onSoundChanged;
        this.onResetRecords = onResetRecords;
        this.onBack = onBack;
        getStyleClass().add("settings-root");

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(28, 44, 24, 44));

        backButton = ghostButton("Voltar");
        backButton.setOnAction(event -> goBack());
        HBox top = new HBox(backButton);
        top.setAlignment(Pos.CENTER_LEFT);
        content.setTop(top);

        Label eyebrow = new Label(Fonts.tracked("PREFERÊNCIAS"));
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Configurações");
        title.getStyleClass().add("screen-title");
        Label subtitle = new Label("Ajuste o jogo do seu jeito.");
        subtitle.getStyleClass().add("screen-subtitle");

        ToggleSwitch soundSwitch = new ToggleSwitch(sound.isEnabled());
        soundSwitch.selectedProperty().addListener((observable, oldValue, enabled) -> {
            sound.setEnabled(enabled);
            onSoundChanged.accept(enabled);
            if (enabled) {
                sound.playUiClick();
            }
        });

        Button resetButton = new Button("Apagar recordes");
        resetButton.getStyleClass().add("danger-button");
        resetButton.setOnAction(event -> showResetConfirmation());
        Buttons.enterActivates(resetButton);

        VBox rows = new VBox(10);
        rows.setFillWidth(true);
        rows.getChildren().addAll(
                settingsRow("Som", "Efeitos sonoros do jogo.", soundSwitch),
                settingsRow("Recordes", "Zerar os melhores placares dos três modos.", resetButton),
                settingsRow("Sobre", "Cobrinha 1.0.0 · JavaFX 21 · Fira Sans (SIL OFL 1.1).", null));

        feedback.getStyleClass().add("settings-feedback");
        feedback.setMinHeight(18);
        feedback.setOpacity(0);

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(760);
        center.setMaxHeight(Region.USE_PREF_SIZE);
        center.getChildren().addAll(eyebrow, spacer(8), title, spacer(6), subtitle,
                spacer(26), rows, spacer(6), feedback);
        content.setCenter(center);
        BorderPane.setAlignment(center, Pos.CENTER);

        getChildren().add(content);

        VBox modalCard = new VBox();
        modalCard.getStyleClass().addAll("overlay-card", "modal-card");
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setMaxWidth(400);
        modalCard.setMinWidth(400);
        modalCard.setMaxHeight(Region.USE_PREF_SIZE);

        Label warning = new Label(Fonts.tracked("ATENÇÃO"));
        warning.getStyleClass().addAll("overlay-eyebrow", "danger");
        Label modalTitle = new Label("Apagar recordes?");
        modalTitle.getStyleClass().add("modal-title");
        Label modalText = new Label(
                "Os melhores placares dos três modos serão zerados. Não dá para desfazer.");
        modalText.getStyleClass().add("overlay-subtitle");
        modalText.setWrapText(true);
        modalText.setMaxWidth(300);
        modalText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button confirmButton = new Button("Apagar");
        confirmButton.getStyleClass().add("danger-button");
        confirmButton.setOnAction(event -> confirmReset());
        Buttons.enterActivates(confirmButton);
        cancelButton = ghostButton("Cancelar");
        cancelButton.setOnAction(event -> hideModal());
        HBox actions = new HBox(10, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER);

        modalCard.getChildren().addAll(warning, spacer(10), modalTitle, spacer(8),
                modalText, spacer(22), actions);

        Region dim = new Region();
        dim.getStyleClass().add("modal-backdrop");
        dim.setOnMouseClicked(event -> hideModal());
        modalLayer.getChildren().addAll(dim, modalCard);
        modalLayer.setVisible(false);
        modalLayer.setOpacity(0);
        getChildren().add(modalLayer);
    }

    private HBox settingsRow(String name, String description, Node trailing) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("settings-name");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("settings-desc");
        VBox text = new VBox(2, nameLabel, descLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(16, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-row");
        row.setMaxWidth(Double.MAX_VALUE);
        if (trailing != null) {
            row.getChildren().add(trailing);
        }
        return row;
    }

    private void showResetConfirmation() {
        sound.playUiClick();
        modalLayer.setVisible(true);
        modalFade = new FadeTransition(Duration.millis(150), modalLayer);
        modalFade.setFromValue(0);
        modalFade.setToValue(1);
        modalFade.play();
        cancelButton.requestFocus();
    }

    private void hideModal() {
        if (!modalLayer.isVisible()) {
            return;
        }
        if (modalFade != null) {
            modalFade.stop();
        }
        modalFade = new FadeTransition(Duration.millis(130), modalLayer);
        modalFade.setFromValue(modalLayer.getOpacity());
        modalFade.setToValue(0);
        modalFade.setOnFinished(event -> modalLayer.setVisible(false));
        modalFade.play();
        backButton.requestFocus();
    }

    private void confirmReset() {
        sound.playUiClick();
        onResetRecords.run();
        hideModal();
        showFeedback("Recordes apagados.");
    }

    private void showFeedback(String text) {
        feedback.setText(text);
        feedback.setOpacity(1);
        if (feedbackTimer != null) {
            feedbackTimer.stop();
        }
        if (feedbackFade != null) {
            feedbackFade.stop();
        }
        feedbackTimer = new PauseTransition(Duration.seconds(2.4));
        feedbackTimer.setOnFinished(event -> {
            feedbackFade = new FadeTransition(Duration.millis(300), feedback);
            feedbackFade.setToValue(0);
            feedbackFade.play();
        });
        feedbackTimer.playFromStart();
    }

    private void goBack() {
        if (modalLayer.isVisible()) {
            hideModal();
            return;
        }
        sound.playUiClick();
        onBack.run();
    }

    private static Button ghostButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("ghost-button");
        return Buttons.enterActivates(button);
    }

    private static Region spacer(double height) {
        Region region = new Region();
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
        return region;
    }

    @Override
    public Parent view() {
        return this;
    }

    @Override
    public String title() {
        return "Cobrinha — Configurações";
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        boolean escape = event.getCode() == KeyCode.ESCAPE
                || event.getCode() == KeyCode.BACK_SPACE;
        if (!escape) {
            return false;
        }
        if (modalLayer.isVisible()) {
            hideModal();
        } else {
            goBack();
        }
        return true;
    }

    @Override
    public Node initialFocus() {
        return backButton;
    }
}
