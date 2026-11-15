package com.cobrinha;

import com.cobrinha.audio.Sound;
import com.cobrinha.core.GameMode;
import com.cobrinha.core.PlayerProfile;
import com.cobrinha.core.ProfileStore;
import com.cobrinha.core.SnakeGame;
import com.cobrinha.ui.AppScreen;
import com.cobrinha.ui.Backdrop;
import com.cobrinha.ui.Fonts;
import com.cobrinha.ui.GameScreen;
import com.cobrinha.ui.HomeScreen;
import com.cobrinha.ui.Palette;
import com.cobrinha.ui.RecordsScreen;
import com.cobrinha.ui.SettingsScreen;
import java.util.EnumSet;
import java.util.Set;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Aplicativo: navegação entre menu, jogo, recordes e configurações, além de
 * manter o perfil do jogador e o serviço de som.
 */
public final class CobrinhaApp extends Application {

    private static final double PREFERRED_WIDTH = 900;
    private static final double PREFERRED_HEIGHT = 940;

    private final Set<KeyCode> heldKeys = EnumSet.noneOf(KeyCode.class);

    private PlayerProfile profile;
    private ProfileStore store;
    private Sound sound;
    private Stage stage;
    private StackPane host;
    private AppScreen current;
    private GameScreen currentGame;

    static {
        System.setProperty("prism.lcdtext", "false");
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Fonts.load();

        store = ProfileStore.createDefault();
        profile = store.load();
        sound = new Sound();
        sound.setEnabled(profile.isSoundEnabled());

        host = new StackPane();
        host.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane root = new StackPane(new Backdrop(), host);
        root.getStyleClass().add("app-root");
        root.setFocusTraversable(true);

        Scene scene = new Scene(root, initialWidth(), initialHeight());
        scene.setFill(Palette.BACKGROUND);
        scene.getStylesheets().add(
                CobrinhaApp.class.getResource("/css/estilo.css").toExternalForm());
        installKeyboard(scene);

        stage.setTitle("Cobrinha");
        stage.setScene(scene);
        stage.setMinWidth(620);
        stage.setMinHeight(700);
        loadIcon(stage);
        stage.centerOnScreen();
        stage.show();
        showHome();
        root.requestFocus();
    }

    @Override
    public void stop() {
        if (current != null) {
            current.dispose();
        }
        if (profile != null && sound != null) {
            profile.setSoundEnabled(sound.isEnabled());
            store.save(profile);
        }
        if (sound != null) {
            sound.close();
        }
    }

    // ------------------------------------------------------------------
    // Navegação
    // ------------------------------------------------------------------

    private void showHome() {
        showScreen(new HomeScreen(profile, sound, this::startGame,
                this::showRecords, this::showSettings));
    }

    private void showRecords() {
        showScreen(new RecordsScreen(profile, sound, this::showHome));
    }

    private void showSettings() {
        showScreen(new SettingsScreen(sound,
                enabled -> {
                    profile.setSoundEnabled(enabled);
                    store.save(profile);
                },
                () -> {
                    profile.resetRecords();
                    store.save(profile);
                },
                this::showHome));
    }

    private void startGame(GameMode mode) {
        SnakeGame game = new SnakeGame(mode);
        GameScreen screen = new GameScreen(game, profile.best(mode), sound,
                this::showHome,
                score -> {
                    if (profile.recordBest(mode, score)) {
                        store.save(profile);
                    }
                },
                () -> {
                    profile.setSoundEnabled(sound.isEnabled());
                    store.save(profile);
                });
        showScreen(screen);
    }

    private void showScreen(AppScreen screen) {
        if (screen == current) {
            return;
        }
        if (current != null) {
            current.onHidden();
            current.dispose();
            host.getChildren().clear();
        }
        current = screen;
        currentGame = screen instanceof GameScreen gameScreen ? gameScreen : null;

        Parent view = screen.view();
        view.setOpacity(0);
        view.setTranslateY(8);
        host.getChildren().add(view);

        FadeTransition fade = new FadeTransition(Duration.millis(230), view);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), view);
        slide.setFromY(8);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.play();

        stage.setTitle(screen.title());
        heldKeys.clear();
        screen.onShown();

        Node focus = screen.initialFocus();
        if (focus != null) {
            Platform.runLater(focus::requestFocus);
        }
    }

    // ------------------------------------------------------------------
    // Entrada e ciclo de vida
    // ------------------------------------------------------------------

    private void installKeyboard(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!heldKeys.add(event.getCode())) {
                return;
            }
            if (current != null && current.handleKey(event)) {
                event.consume();
            }
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED,
                event -> heldKeys.remove(event.getCode()));

        stage.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (wasFocused && !focused) {
                heldKeys.clear();
                if (currentGame != null) {
                    currentGame.pauseIfRunning();
                }
            }
        });
    }

    private void loadIcon(Stage stage) {
        var url = CobrinhaApp.class.getResource("/icons/cobrinha.png");
        if (url != null) {
            stage.getIcons().add(new Image(url.toExternalForm()));
        }
    }

    private static double initialWidth() {
        double available = screenWidth() * 0.86;
        return Math.max(640, Math.min(PREFERRED_WIDTH, available));
    }

    private static double initialHeight() {
        double available = screenHeight() * 0.92;
        return Math.max(720, Math.min(PREFERRED_HEIGHT, available));
    }

    private static double screenWidth() {
        try {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            return bounds.getWidth() > 0 ? bounds.getWidth() : PREFERRED_WIDTH;
        } catch (RuntimeException e) {
            return PREFERRED_WIDTH;
        }
    }

    private static double screenHeight() {
        try {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            return bounds.getHeight() > 0 ? bounds.getHeight() : PREFERRED_HEIGHT;
        } catch (RuntimeException e) {
            return PREFERRED_HEIGHT;
        }
    }
}
