package com.cobrinha.ui;

import com.cobrinha.audio.Sound;
import com.cobrinha.core.PlayerProfile;
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
import javafx.scene.layout.VBox;

/**
 * Histórico de recordes por modo.
 */
public final class RecordsScreen extends BorderPane implements AppScreen {

    private final Sound sound;
    private final Runnable onBack;
    private final Button backButton;

    public RecordsScreen(PlayerProfile profile, Sound sound, Runnable onBack) {
        this.sound = sound;
        this.onBack = onBack;
        getStyleClass().add("records-root");
        setPadding(new Insets(28, 44, 24, 44));

        backButton = ghostButton("Voltar");
        backButton.setOnAction(event -> goBack());
        HBox top = new HBox(backButton);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(top);

        boolean empty = profile.overallBest() == 0;

        Label eyebrow = new Label(Fonts.tracked("SEU HISTÓRICO"));
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Recordes");
        title.getStyleClass().add("screen-title");
        Label subtitle = new Label(empty
                ? "Jogue uma partida para começar seu histórico."
                : "Seu melhor placar em cada modo.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox rows = new VBox(10);
        rows.setFillWidth(true);
        for (ModeInfo info : ModeInfo.all()) {
            rows.getChildren().add(buildRecordRow(info, profile.best(info.mode())));
        }

        Label note = new Label("Os recordes são salvos automaticamente neste computador.");
        note.getStyleClass().add("screen-note");
        note.setPadding(new Insets(4, 0, 0, 0));

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(760);
        center.setMaxHeight(Region.USE_PREF_SIZE);
        center.getChildren().addAll(eyebrow, spacer(8), title, spacer(6), subtitle,
                spacer(26), rows, spacer(16), note);

        setCenter(center);
        BorderPane.setAlignment(center, Pos.CENTER);
    }

    private HBox buildRecordRow(ModeInfo info, int best) {
        var icon = ModeIcon.create(info.mode(), 42);

        Label name = new Label(info.name());
        name.getStyleClass().add("record-mode-name");
        Label description = new Label(info.description());
        description.getStyleClass().add("record-mode-desc");
        VBox mode = new VBox(2, name, description);
        mode.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mode, Priority.ALWAYS);

        Label caption = new Label(Fonts.tracked("MELHOR"));
        caption.getStyleClass().add("record-caption");
        Label value = new Label(best == 0 ? "—" : Integer.toString(best));
        value.getStyleClass().add("record-value");
        if (best == 0) {
            value.setOpacity(0.4);
        }
        VBox record = new VBox(0, caption, value);
        record.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(14, icon, mode, record);
        row.getStyleClass().add("record-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAccessibleText(info.name() + ": recorde "
                + (best == 0 ? "nenhum" : Integer.toString(best)));
        return row;
    }

    private void goBack() {
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
        return "Cobrinha — Recordes";
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
            goBack();
            return true;
        }
        return false;
    }

    @Override
    public Node initialFocus() {
        return backButton;
    }
}
