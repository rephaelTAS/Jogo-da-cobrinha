package com.cobrinha.ui;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Interruptor no estilo iOS usado na tela de configurações, construído com
 * nós simples para manter o visual consistente e o controle pelo teclado.
 */
public final class ToggleSwitch extends StackPane {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final double TRACK_WIDTH = 48;
    private static final double TRACK_HEIGHT = 28;
    private static final double KNOB_SIZE = 20;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Region knob = new Region();
    private final TranslateTransition knobAnimation = new TranslateTransition(Duration.millis(130), knob);

    public ToggleSwitch(boolean initial) {
        getStyleClass().add("toggle-switch");
        setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
        setPadding(new Insets(4));
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);
        setCursor(Cursor.HAND);
        setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);

        knob.getStyleClass().add("toggle-knob");
        knob.setMinSize(KNOB_SIZE, KNOB_SIZE);
        knob.setPrefSize(KNOB_SIZE, KNOB_SIZE);
        knob.setMaxSize(KNOB_SIZE, KNOB_SIZE);
        getChildren().add(knob);

        selected.addListener((observable, oldValue, newValue) -> applyState(newValue, true));
        setOnMouseClicked(event -> toggle());
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                toggle();
                event.consume();
            }
        });

        selected.set(initial);
        applyState(initial, false);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public void toggle() {
        selected.set(!selected.get());
    }

    private void applyState(boolean value, boolean animate) {
        pseudoClassStateChanged(SELECTED, value);
        setAccessibleText(value ? "Ativado" : "Desativado");
        if (knobAnimation != null) {
            knobAnimation.stop();
        }
        double target = value ? TRACK_WIDTH - KNOB_SIZE - 8 : 0;
        if (animate) {
            knobAnimation.setToX(target);
            knobAnimation.playFromStart();
        } else {
            knob.setTranslateX(target);
        }
    }
}
