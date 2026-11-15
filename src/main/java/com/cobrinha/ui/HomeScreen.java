package com.cobrinha.ui;

import com.cobrinha.audio.Sound;
import com.cobrinha.core.GameMode;
import com.cobrinha.core.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Menu inicial: marca, escolha de modo, recordes de cada modo e acesso às
 * configurações.
 */
public final class HomeScreen extends BorderPane implements AppScreen {

    private final PlayerProfile profile;
    private final Sound sound;
    private final Consumer<GameMode> onPlay;
    private final Runnable onRecords;
    private final Runnable onSettings;
    private final List<HBox> modeRows = new ArrayList<>();

    public HomeScreen(PlayerProfile profile, Sound sound, Consumer<GameMode> onPlay,
                      Runnable onRecords, Runnable onSettings) {
        this.profile = profile;
        this.sound = sound;
        this.onPlay = onPlay;
        this.onRecords = onRecords;
        this.onSettings = onSettings;

        getStyleClass().add("home-root");
        setPadding(new Insets(30, 44, 22, 44));

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(820);
        center.setMaxHeight(Region.USE_PREF_SIZE);
        center.getChildren().add(buildHero());
        center.getChildren().add(spacer(28));

        VBox modes = new VBox(10);
        modes.setFillWidth(true);
        for (ModeInfo info : ModeInfo.all()) {
            HBox row = buildModeRow(info);
            modeRows.add(row);
            modes.getChildren().add(row);
        }
        center.getChildren().add(modes);
        center.getChildren().add(spacer(16));
        center.getChildren().add(buildBonusHint());

        setCenter(center);
        BorderPane.setAlignment(center, Pos.CENTER);
        setBottom(buildFooter());
    }

    private VBox buildHero() {
        VBox hero = new VBox();
        hero.setAlignment(Pos.CENTER);

        var url = HomeScreen.class.getResource("/icons/marca.png");
        if (url != null) {
            ImageView mark = new ImageView(new Image(url.toExternalForm(), 64, 64, true, true));
            mark.setFitWidth(60);
            mark.setFitHeight(60);
            mark.setPreserveRatio(true);
            mark.setSmooth(true);
            hero.getChildren().add(mark);
            hero.getChildren().add(spacer(14));
        }

        Label title = new Label("COBRINHA");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Escolha um modo e coma o máximo que puder.");
        subtitle.getStyleClass().add("hero-subtitle");
        hero.getChildren().addAll(title, spacer(7), subtitle);
        return hero;
    }

    private HBox buildModeRow(ModeInfo info) {
        Canvas icon = ModeIcon.create(info.mode(), 46);

        Label name = new Label(info.name());
        name.getStyleClass().add("mode-name");
        Label description = new Label(info.description());
        description.getStyleClass().add("mode-desc");
        description.setMaxWidth(380);
        VBox text = new VBox(2, name, description);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        int best = profile.best(info.mode());
        Label recordCaption = new Label(Fonts.tracked("RECORDE"));
        recordCaption.getStyleClass().add("mode-record-label");
        Label recordValue = new Label(best == 0 ? "—" : Integer.toString(best));
        recordValue.getStyleClass().add("mode-record-value");
        if (best == 0) {
            recordValue.setOpacity(0.45);
        }
        VBox record = new VBox(2, recordCaption, recordValue);
        record.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(14, icon, text, record, chevron());
        row.getStyleClass().add("mode-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setFocusTraversable(true);
        row.setCursor(Cursor.HAND);
        row.setAccessibleRole(AccessibleRole.BUTTON);
        row.setAccessibleText(info.name() + ". " + info.description() + ". Recorde: "
                + (best == 0 ? "nenhum" : Integer.toString(best)));
        row.setOnMouseClicked(event -> activate(info));
        row.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                activate(info);
                event.consume();
            }
        });
        return row;
    }

    private HBox buildBonusHint() {
        Region dot = new Region();
        dot.getStyleClass().add("bonus-dot");
        Label text = new Label(
                "A cada 5 bolinhas, uma dourada vale 5 pontos — e some rápido.");
        text.getStyleClass().add("bonus-hint");
        HBox hint = new HBox(8, dot, text);
        hint.setAlignment(Pos.CENTER);
        return hint;
    }

    private VBox buildFooter() {
        Region divider = new Region();
        divider.getStyleClass().add("footer-divider");
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);

        Button records = ghostButton("Recordes");
        records.setOnAction(event -> {
            sound.playUiClick();
            onRecords.run();
        });
        Button settings = ghostButton("Configurações");
        settings.setOnAction(event -> {
            sound.playUiClick();
            onSettings.run();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, records, spacer, settings);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox footer = new VBox(14, divider, row);
        footer.setMaxWidth(820);
        footer.setPadding(new Insets(18, 0, 0, 0));

        StackPane wrapper = new StackPane(footer);
        wrapper.setAlignment(Pos.CENTER);
        VBox container = new VBox(wrapper);
        return container;
    }

    private static Button ghostButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("ghost-button");
        button.setCursor(Cursor.HAND);
        return Buttons.enterActivates(button);
    }

    private static Canvas chevron() {
        Canvas canvas = new Canvas(14, 14);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.color(1, 1, 1, 0.32));
        gc.setLineWidth(1.5);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(4, 3, 9, 7);
        gc.strokeLine(9, 7, 4, 11);
        return canvas;
    }

    private static Region spacer(double height) {
        Region region = new Region();
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
        return region;
    }

    private void activate(ModeInfo info) {
        sound.playUiClick();
        onPlay.accept(info.mode());
    }

    @Override
    public Parent view() {
        return this;
    }

    @Override
    public String title() {
        return "Cobrinha";
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        GameMode mode = switch (event.getCode()) {
            case DIGIT1, NUMPAD1 -> GameMode.WRAP;
            case DIGIT2, NUMPAD2 -> GameMode.CLASSIC;
            case DIGIT3, NUMPAD3 -> GameMode.MAZE;
            default -> null;
        };
        if (mode == null) {
            return false;
        }
        activate(ModeInfo.of(mode));
        return true;
    }

    @Override
    public Node initialFocus() {
        return modeRows.isEmpty() ? this : modeRows.get(0);
    }
}
