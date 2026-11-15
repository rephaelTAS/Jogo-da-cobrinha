package com.cobrinha.ui;

import com.cobrinha.audio.Sound;
import com.cobrinha.core.Cell;
import com.cobrinha.core.Direction;
import com.cobrinha.core.GameMode;
import com.cobrinha.core.GameState;
import com.cobrinha.core.SnakeGame;
import com.cobrinha.core.TickResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntConsumer;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Tabuleiro jogável: controla o passo da simulação, a renderização no canvas e
 * as sobreposições de pausa/fim. O motor fica isolado em {@link SnakeGame}.
 */
public final class BoardView extends StackPane {

    private static final Color EYE_DARK = Color.web("#07110D");
    private static final Color DEAD_HEAD = Color.web("#465450");
    private static final double DEATH_DELAY_MS = 500;
    private static final double WIN_DELAY_MS = 620;
    private static final double WRAP_FLASH_SECONDS = 0.32;
    private static final double MAZE_FLASH_SECONDS = 0.42;

    private final SnakeGame game;
    private final Sound sound;
    private final Runnable onHome;
    private final IntConsumer onNewBest;
    private final Runnable onSoundChanged;
    private final Random random = new Random();

    private final Canvas backgroundLayer = new Canvas();
    private final Canvas surface = new Canvas();
    private final OverlayLayer overlay = new OverlayLayer();

    private final ReadOnlyIntegerWrapper score = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper best = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyObjectWrapper<GameState> state =
            new ReadOnlyObjectWrapper<>(GameState.READY);
    private final ReadOnlyBooleanWrapper muted = new ReadOnlyBooleanWrapper(false);

    private final AnimationTimer frameTimer;
    private final PauseTransition endOfRunDelay = new PauseTransition();
    private final List<Spark> sparks = new ArrayList<>();
    private final List<ScorePop> scorePops = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();

    private List<Cell> previousCells = List.of();
    private List<Cell> currentCells = List.of();
    private double alpha;
    private double accumulatorMs;
    private double currentTickMs = 150;
    private long lastFrameNanos;
    private double elapsedSeconds;
    private double shakeTime;
    private double deathFlash;
    private double wrapFlash;
    private double mazeFlash;
    private int bestAtRunStart;
    private double lastBackgroundWidth = -1;
    private double lastBackgroundHeight = -1;
    private boolean disposed;

    public BoardView(SnakeGame game, int initialBest, Sound sound, Runnable onHome,
                     IntConsumer onNewBest, Runnable onSoundChanged) {
        this.game = Objects.requireNonNull(game, "game");
        this.sound = Objects.requireNonNull(sound, "sound");
        this.onHome = onHome == null ? () -> { } : onHome;
        this.onNewBest = onNewBest == null ? value -> { } : onNewBest;
        this.onSoundChanged = onSoundChanged == null ? () -> { } : onSoundChanged;

        getStyleClass().add("board");
        setMinSize(280, 280);
        setPrefSize(620, 620);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setFocusTraversable(true);

        backgroundLayer.setMouseTransparent(true);
        surface.setMouseTransparent(true);
        clipToRoundedCard(backgroundLayer);
        clipToRoundedCard(surface);
        getChildren().addAll(backgroundLayer, surface, overlay);

        best.set(Math.max(0, initialBest));
        muted.set(!sound.isEnabled());
        syncBody();
        state.set(GameState.READY);

        widthProperty().addListener((observable, oldValue, newValue) -> updateCanvasSize());
        heightProperty().addListener((observable, oldValue, newValue) -> updateCanvasSize());

        frameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tickFrame(now);
            }
        };
        frameTimer.start();
    }

    // ------------------------------------------------------------------
    // Ciclo de partida
    // ------------------------------------------------------------------

    public void startNewGame() {
        if (disposed) {
            return;
        }
        endOfRunDelay.stop();
        game.restart();
        score.set(0);
        bestAtRunStart = best.get();
        accumulatorMs = 0;
        alpha = 0;
        shakeTime = 0;
        deathFlash = 0;
        wrapFlash = 0;
        mazeFlash = 0;
        sparks.clear();
        scorePops.clear();
        rings.clear();
        syncBody();
        lastFrameNanos = 0;
        state.set(GameState.RUNNING);
        overlay.hide();
        sound.playStart();
    }

    public void pauseIfRunning() {
        if (game.state() == GameState.RUNNING) {
            pauseGame();
        }
    }

    public boolean handleKey(KeyCode code) {
        Direction direction = directionFor(code);
        if (direction != null) {
            if (game.state() == GameState.READY) {
                startNewGame();
                game.requestDirection(direction);
            } else if (game.state() == GameState.RUNNING) {
                game.requestDirection(direction);
            }
            return true;
        }

        switch (code) {
            case M -> {
                sound.toggle();
                muted.set(!sound.isEnabled());
                onSoundChanged.run();
                return true;
            }
            case ESCAPE -> {
                if (game.state() == GameState.RUNNING) {
                    pauseGame();
                } else if (game.state() == GameState.PAUSED) {
                    resumeGame();
                } else if (game.state() == GameState.GAME_OVER
                        || game.state() == GameState.WON) {
                    leaveToHome();
                }
                return true;
            }
            case SPACE -> {
                if (game.state() == GameState.READY) {
                    startNewGame();
                } else if (game.state() == GameState.RUNNING
                        || game.state() == GameState.PAUSED) {
                    togglePause();
                }
                return true;
            }
            case ENTER -> {
                if (game.state() == GameState.PAUSED) {
                    resumeGame();
                } else if (game.state() == GameState.READY
                        || game.state() == GameState.GAME_OVER
                        || game.state() == GameState.WON) {
                    startNewGame();
                }
                return true;
            }
            case R -> {
                startNewGame();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        frameTimer.stop();
        endOfRunDelay.stop();
        overlay.dispose();
    }

    private static Direction directionFor(KeyCode code) {
        return switch (code) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }

    private void pauseGame() {
        game.pause();
        if (game.state() == GameState.PAUSED) {
            state.set(GameState.PAUSED);
            overlay.setActions(this::overlayPrimary, this::leaveToHome);
            overlay.showPaused();
        }
    }

    private void resumeGame() {
        game.resume();
        if (game.state() == GameState.RUNNING) {
            state.set(GameState.RUNNING);
            overlay.hide();
        }
    }

    private void togglePause() {
        if (game.state() == GameState.RUNNING) {
            pauseGame();
        } else if (game.state() == GameState.PAUSED) {
            resumeGame();
        }
    }

    private void overlayPrimary() {
        if (game.state() == GameState.PAUSED) {
            resumeGame();
        } else if (game.state() == GameState.GAME_OVER
                || game.state() == GameState.WON) {
            startNewGame();
        }
    }

    private void leaveToHome() {
        if (disposed) {
            return;
        }
        frameTimer.stop();
        endOfRunDelay.stop();
        onHome.run();
    }

    // ------------------------------------------------------------------
    // Loop de animação
    // ------------------------------------------------------------------

    private void tickFrame(long now) {
        if (disposed) {
            return;
        }
        if (lastFrameNanos == 0) {
            lastFrameNanos = now;
            render();
            return;
        }
        double deltaMs = Math.min((now - lastFrameNanos) / 1_000_000.0, 100);
        lastFrameNanos = now;
        double deltaSeconds = deltaMs / 1000.0;

        if (game.state() != GameState.PAUSED) {
            elapsedSeconds += deltaSeconds;
            updateEffects(deltaSeconds);
        }

        if (game.state() == GameState.RUNNING) {
            currentTickMs = Math.max(1, game.tickIntervalMs());
            accumulatorMs += deltaMs;
            int guard = 0;
            while (accumulatorMs >= currentTickMs && game.state() == GameState.RUNNING
                    && guard++ < 4) {
                accumulatorMs -= currentTickMs;
                advance();
                currentTickMs = Math.max(1, game.tickIntervalMs());
            }
            alpha = clamp(accumulatorMs / currentTickMs, 0, 1);
        } else if (game.state() == GameState.READY) {
            alpha = 0;
        }

        render();
    }

    private void advance() {
        previousCells = currentCells;
        Cell smallBefore = game.smallFood();
        Cell bigBefore = game.bigFood();
        TickResult result = game.tick();
        currentCells = game.body();

        int newScore = game.score();
        if (newScore != score.get()) {
            score.set(newScore);
            if (newScore > best.get()) {
                best.set(newScore);
                onNewBest.accept(newScore);
            }
        }

        if (result.died()) {
            previousCells = currentCells;
            accumulatorMs = 0;
            alpha = 0;
            handleDeath(result);
            return;
        }
        if (result.ateSmall()) {
            handleSmallEaten(smallBefore != null ? smallBefore : result.head());
        }
        if (result.ateBig()) {
            handleBigEaten(bigBefore);
        }
        if (result.bigSpawned()) {
            handleBigSpawned();
        }
        if (result.bigExpired()) {
            handleBigExpired(bigBefore);
        }
        if (result.wrapped()) {
            handleWrap(result);
        }
        if (result.mazeShifted()) {
            mazeFlash = MAZE_FLASH_SECONDS;
            sound.playMazeShift();
        }
        if (result.won()) {
            handleWin();
        }
    }

    private void handleSmallEaten(Cell cell) {
        if (cell == null) {
            return;
        }
        double x = canvasCenterX(cell);
        double y = canvasCenterY(cell);
        spawnSparks(x, y, 12, Palette.FOOD_LIGHT, Palette.ACCENT_LIGHT);
        scorePops.add(new ScorePop(x, y - cellSize() * 0.2, SnakeGame.POINTS_SMALL,
                Palette.ACCENT_LIGHT));
        sound.playEatSmall(game.smallsEaten());
    }

    private void handleBigEaten(Cell cell) {
        if (cell == null) {
            return;
        }
        double x = canvasCenterX(cell);
        double y = canvasCenterY(cell);
        spawnSparks(x, y, 24, Palette.GOLD_LIGHT, Palette.ACCENT_LIGHT);
        rings.add(new Ring(x, y, cellSize() * 0.2, cellSize() * 1.3, 0.5, Palette.GOLD));
        scorePops.add(new ScorePop(x, y - cellSize() * 0.25, SnakeGame.POINTS_BIG,
                Palette.GOLD_LIGHT));
        sound.playEatBig();
    }

    private void handleBigSpawned() {
        Cell cell = game.bigFood();
        if (cell == null) {
            return;
        }
        double x = canvasCenterX(cell);
        double y = canvasCenterY(cell);
        rings.add(new Ring(x, y, cellSize() * 0.15, cellSize() * 1.15, 0.55, Palette.GOLD));
        spawnSparks(x, y, 8, Palette.GOLD_LIGHT, Palette.GOLD);
        sound.playBonusSpawn();
    }

    private void handleBigExpired(Cell cell) {
        if (cell != null) {
            double x = canvasCenterX(cell);
            double y = canvasCenterY(cell);
            spawnSparks(x, y, 10, Palette.GOLD, Palette.FAINT);
        }
        sound.playBonusExpire();
    }

    private void handleWrap(TickResult result) {
        wrapFlash = WRAP_FLASH_SECONDS;
        sound.playWrap();
        if (result.head() != null) {
            rings.add(new Ring(canvasCenterX(result.head()), canvasCenterY(result.head()),
                    cellSize() * 0.2, cellSize() * 1.1, 0.38, Palette.ACCENT));
        }
    }

    private void handleDeath(TickResult result) {
        shakeTime = 0.42;
        deathFlash = 1;
        String reason = switch (result.collision()) {
            case WALL -> "A parede não se moveu.";
            case SELF -> "Você mordeu a própria cauda.";
            case OBSTACLE -> "Um bloco bloqueou o caminho.";
            default -> "Fim da linha.";
        };
        if (!currentCells.isEmpty()) {
            spawnSparks(canvasCenterX(currentCells.getFirst()),
                    canvasCenterY(currentCells.getFirst()), 22, Palette.FOOD, Palette.DANGER);
        }
        sound.playGameOver();
        state.set(GameState.GAME_OVER);
        boolean newRecord = game.score() > bestAtRunStart;
        overlay.setActions(this::overlayPrimary, this::leaveToHome);
        scheduleEndOfRun(Duration.millis(DEATH_DELAY_MS), () ->
                overlay.showGameOver(game.score(), best.get(), newRecord, reason));
    }

    private void handleWin() {
        deathFlash = 0;
        shakeTime = 0;
        previousCells = currentCells;
        alpha = 0;
        state.set(GameState.WON);
        sound.playWin();
        if (!currentCells.isEmpty()) {
            double x = canvasCenterX(currentCells.getFirst());
            double y = canvasCenterY(currentCells.getFirst());
            spawnSparks(x, y, 30, Palette.GOLD_LIGHT, Palette.ACCENT_LIGHT);
            rings.add(new Ring(x, y, cellSize() * 0.2, cellSize() * 2.0, 0.7, Palette.ACCENT));
        }
        boolean newRecord = game.score() > bestAtRunStart;
        overlay.setActions(this::overlayPrimary, this::leaveToHome);
        scheduleEndOfRun(Duration.millis(WIN_DELAY_MS), () ->
                overlay.showWon(game.score(), best.get(), newRecord));
    }

    private void scheduleEndOfRun(Duration delay, Runnable action) {
        endOfRunDelay.stop();
        endOfRunDelay.setDuration(delay);
        endOfRunDelay.setOnFinished(event -> action.run());
        endOfRunDelay.playFromStart();
    }

    // ------------------------------------------------------------------
    // Efeitos
    // ------------------------------------------------------------------

    private void spawnSparks(double x, double y, int count, Color a, Color b) {
        double cell = cellSize();
        for (int i = 0; i < count; i++) {
            Spark spark = new Spark();
            spark.x = x;
            spark.y = y;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 26 + random.nextDouble() * 135;
            spark.vx = Math.cos(angle) * speed;
            spark.vy = Math.sin(angle) * speed;
            spark.life = 0.3 + random.nextDouble() * 0.4;
            spark.maxLife = spark.life;
            spark.size = Math.max(1.2, cell * (0.035 + random.nextDouble() * 0.055));
            spark.color = random.nextBoolean() ? a : b;
            sparks.add(spark);
        }
    }

    private void updateEffects(double deltaSeconds) {
        for (Iterator<Spark> iterator = sparks.iterator(); iterator.hasNext(); ) {
            Spark spark = iterator.next();
            spark.life -= deltaSeconds;
            if (spark.life <= 0) {
                iterator.remove();
                continue;
            }
            spark.x += spark.vx * deltaSeconds;
            spark.y += spark.vy * deltaSeconds;
            spark.vx *= 1 - Math.min(1, 2.4 * deltaSeconds);
            spark.vy = spark.vy * (1 - Math.min(1, 2.4 * deltaSeconds)) + 55 * deltaSeconds;
        }
        for (Iterator<ScorePop> iterator = scorePops.iterator(); iterator.hasNext(); ) {
            ScorePop pop = iterator.next();
            pop.life -= deltaSeconds;
            if (pop.life <= 0) {
                iterator.remove();
                continue;
            }
            pop.y -= 24 * deltaSeconds;
        }
        for (Iterator<Ring> iterator = rings.iterator(); iterator.hasNext(); ) {
            Ring ring = iterator.next();
            ring.life -= deltaSeconds;
            if (ring.life <= 0) {
                iterator.remove();
            }
        }
        if (shakeTime > 0) {
            shakeTime = Math.max(0, shakeTime - deltaSeconds);
        }
        if (deathFlash > 0) {
            deathFlash = Math.max(0, deathFlash - deltaSeconds * 2.6);
        }
        if (wrapFlash > 0) {
            wrapFlash = Math.max(0, wrapFlash - deltaSeconds);
        }
        if (mazeFlash > 0) {
            mazeFlash = Math.max(0, mazeFlash - deltaSeconds);
        }
    }

    // ------------------------------------------------------------------
    // Geometria e canvas
    // ------------------------------------------------------------------

    private void updateCanvasSize() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        backgroundLayer.setWidth(width);
        backgroundLayer.setHeight(height);
        surface.setWidth(width);
        surface.setHeight(height);
        if (width != lastBackgroundWidth || height != lastBackgroundHeight) {
            lastBackgroundWidth = width;
            lastBackgroundHeight = height;
            drawBackground();
        }
    }

    private void clipToRoundedCard(Canvas canvas) {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(48);
        clip.setArcHeight(48);
        canvas.setClip(clip);
    }

    private double gridInset() {
        double side = Math.min(getWidth(), getHeight());
        return Math.max(18, side * 0.046);
    }

    private double cellSize() {
        double usable = Math.min(getWidth(), getHeight()) - gridInset() * 2;
        return Math.max(1, usable / Math.max(game.columns(), game.rows()));
    }

    private double gridOriginX() {
        return (getWidth() - cellSize() * game.columns()) / 2;
    }

    private double gridOriginY() {
        return (getHeight() - cellSize() * game.rows()) / 2;
    }

    private double canvasCenterX(Cell cell) {
        return gridOriginX() + (cell.x() + 0.5) * cellSize();
    }

    private double canvasCenterY(Cell cell) {
        return gridOriginY() + (cell.y() + 0.5) * cellSize();
    }

    private double currentBigFraction() {
        if (game.bigFood() == null) {
            return 0;
        }
        double remaining = game.bigFoodRemainingMs();
        if (game.state() == GameState.RUNNING) {
            remaining -= accumulatorMs;
        }
        return clamp(remaining / (double) SnakeGame.BIG_FOOD_LIFETIME_MS, 0, 1);
    }

    // ------------------------------------------------------------------
    // Renderização
    // ------------------------------------------------------------------

    private void drawBackground() {
        double width = backgroundLayer.getWidth();
        double height = backgroundLayer.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        GraphicsContext gc = backgroundLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, 0.5, 0.02, 0.92, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.39, 0.95, 0.75, 0.085)),
                new Stop(0.55, Color.color(0.39, 0.95, 0.75, 0.025)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new LinearGradient(0, 0.68, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(1, Color.color(0, 0, 0, 0.28))));
        gc.fillRect(0, 0, width, height);

        double cell = cellSize();
        double originX = gridOriginX();
        double originY = gridOriginY();
        double radius = Math.max(1.1, cell * 0.06);
        gc.setFill(Palette.GRID_DOT);
        for (int y = 0; y < game.rows(); y++) {
            for (int x = 0; x < game.columns(); x++) {
                double cx = originX + (x + 0.5) * cell;
                double cy = originY + (y + 0.5) * cell;
                gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            }
        }

        drawArenaBorder(gc, originX, originY, cell * game.columns(), cell * game.rows(), cell);
    }

    private void drawArenaBorder(GraphicsContext gc, double x, double y,
                                 double width, double height, double cell) {
        gc.save();
        gc.setLineWidth(1);
        if (game.mode().wraps()) {
            gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.14));
            gc.setLineDashes(3, 7);
            gc.strokeRoundRect(x - 6, y - 6, width + 12, height + 12, 18, 18);
            gc.setLineDashes();
            drawWrapChevrons(gc, x, y, width, height, cell);
        } else {
            gc.setStroke(Color.color(1, 1, 1, 0.05));
            gc.strokeRoundRect(x - 6, y - 6, width + 12, height + 12, 18, 18);
        }
        gc.restore();
    }

    private void drawWrapChevrons(GraphicsContext gc, double x, double y,
                                  double width, double height, double cell) {
        gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.18));
        gc.setLineWidth(1.3);
        double inset = Math.max(9, cell * 0.42);
        double arm = Math.max(2.5, cell * 0.16);
        double midX = x + width / 2;
        double midY = y + height / 2;
        double right = x + width + inset;
        double left = x - inset;
        double top = y - inset;
        double bottom = y + height + inset;

        gc.strokeLine(right - arm, midY - arm, right, midY);
        gc.strokeLine(right, midY, right - arm, midY + arm);
        gc.strokeLine(left + arm, midY - arm, left, midY);
        gc.strokeLine(left, midY, left + arm, midY + arm);
        gc.strokeLine(midX - arm, top + arm, midX, top);
        gc.strokeLine(midX, top, midX + arm, top + arm);
        gc.strokeLine(midX - arm, bottom - arm, midX, bottom);
        gc.strokeLine(midX, bottom, midX + arm, bottom - arm);
    }

    private void render() {
        double width = surface.getWidth();
        double height = surface.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        GraphicsContext gc = surface.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.save();

        if (shakeTime > 0) {
            double strength = (shakeTime / 0.42) * Math.max(2, cellSize() * 0.22);
            gc.translate(Math.sin(elapsedSeconds * 92) * strength * 0.8,
                    Math.cos(elapsedSeconds * 71) * strength * 0.55);
        }

        drawObstacles(gc);
        double cell = cellSize();
        Cell small = game.smallFood();
        if (small != null) {
            drawSmallFood(gc, canvasCenterX(small), canvasCenterY(small), cell);
        }
        Cell big = game.bigFood();
        if (big != null) {
            drawBigFood(gc, canvasCenterX(big), canvasCenterY(big), cell);
        }
        drawSnake(gc, cell);
        drawSparks(gc);
        drawRings(gc);
        drawScorePops(gc);
        gc.restore();

        if (wrapFlash > 0) {
            gc.save();
            gc.setStroke(Color.color(0.39, 0.95, 0.75,
                    0.28 * (wrapFlash / WRAP_FLASH_SECONDS)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(2, 2, width - 4, height - 4, 46, 46);
            gc.restore();
        }
        if (deathFlash > 0) {
            gc.save();
            gc.setStroke(Color.color(1, 0.42, 0.36, 0.55 * deathFlash));
            gc.setLineWidth(2);
            gc.strokeRoundRect(1.5, 1.5, width - 3, height - 3, 46, 46);
            gc.restore();
        }
    }

    private void drawObstacles(GraphicsContext gc) {
        if (game.obstacles().isEmpty()) {
            return;
        }
        double cell = cellSize();
        double inset = Math.max(1.1, cell * 0.045);
        double size = cell - inset * 2;
        double radius = cell * 0.14;
        double flash = clamp(mazeFlash / MAZE_FLASH_SECONDS, 0, 1);
        Color fill = Palette.OBSTACLE.interpolate(Palette.ACCENT, 0.16 * flash);
        Color top = Palette.OBSTACLE_LIGHT.interpolate(Palette.ACCENT, 0.22 * flash);

        for (Cell wall : game.obstacles()) {
            double x = gridOriginX() + wall.x() * cell + inset;
            double y = gridOriginY() + wall.y() * cell + inset;
            gc.setFill(Color.color(0, 0, 0, 0.24));
            gc.fillRoundRect(x + 0.8, y + 1.8, size, size, radius, radius);
            gc.setFill(fill);
            gc.fillRoundRect(x, y, size, size, radius, radius);
            gc.setStroke(top);
            gc.setLineWidth(1);
            gc.strokeLine(x + radius * 0.6, y + 1.2, x + size - radius * 0.6, y + 1.2);
        }
    }

    private void drawSmallFood(GraphicsContext gc, double x, double y, double cell) {
        double pulse = 1 + 0.05 * Math.sin(elapsedSeconds * 4.1);
        double radius = cell * 0.30 * pulse;
        double glowRadius = cell * 1.75;

        gc.setFill(new RadialGradient(0, 0, x, y, glowRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.48, 0.42, 0.26)),
                new Stop(0.45, Color.color(1, 0.48, 0.42, 0.09)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillOval(x - glowRadius, y - glowRadius, glowRadius * 2, glowRadius * 2);

        gc.setFill(new RadialGradient(0, 0, x - radius * 0.3, y - radius * 0.35,
                radius * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Palette.FOOD_LIGHT),
                new Stop(0.55, Palette.FOOD),
                new Stop(1, Color.web("#E25549"))));
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        gc.setFill(Color.color(1, 1, 1, 0.55));
        double highlight = Math.max(1.2, radius * 0.24);
        gc.fillOval(x - radius * 0.42, y - radius * 0.46, highlight, highlight);
    }

    private void drawBigFood(GraphicsContext gc, double x, double y, double cell) {
        double fraction = currentBigFraction();
        boolean urgent = fraction < 0.22;
        double blink = urgent ? 0.75 + 0.25 * Math.sin(elapsedSeconds * 18) : 1;
        double pulse = 1 + 0.035 * Math.sin(elapsedSeconds * 4.5);
        double radius = cell * 0.38 * pulse;
        double glowRadius = cell * 1.9;
        Color ringColor = urgent ? Palette.FOOD : Palette.GOLD;

        gc.setFill(new RadialGradient(0, 0, x, y, glowRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.76, 0.37, 0.26 * blink)),
                new Stop(0.45, Color.color(1, 0.76, 0.37, 0.09 * blink)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillOval(x - glowRadius, y - glowRadius, glowRadius * 2, glowRadius * 2);

        gc.setFill(new RadialGradient(0, 0, x - radius * 0.35, y - radius * 0.4,
                radius * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Palette.GOLD_LIGHT),
                new Stop(0.55, Palette.GOLD),
                new Stop(1, Palette.GOLD_DARK)));
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        double ringRadius = cell * 0.55;
        gc.setStroke(Color.color(1, 1, 1, 0.09));
        gc.setLineWidth(Math.max(1.3, cell * 0.055));
        gc.strokeOval(x - ringRadius, y - ringRadius, ringRadius * 2, ringRadius * 2);
        gc.setStroke(Color.color(ringColor.getRed(), ringColor.getGreen(),
                ringColor.getBlue(), 0.85 * blink));
        gc.setLineWidth(Math.max(1.5, cell * 0.065));
        gc.strokeArc(x - ringRadius, y - ringRadius, ringRadius * 2, ringRadius * 2,
                90, -360 * fraction, ArcType.OPEN);

        gc.setFill(Color.web("#3A2405"));
        gc.setFont(Font.font(Fonts.FAMILY, FontWeight.BOLD,
                Math.max(8, cell * 0.36)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("5", x, y + cell * 0.01);
    }

    private void drawSnake(GraphicsContext gc, double cell) {
        if (currentCells.isEmpty()) {
            return;
        }
        int count = currentCells.size();
        List<Point2D> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Cell to = currentCells.get(i);
            Cell from = previousCells.isEmpty() ? to
                    : previousCells.get(Math.min(i, previousCells.size() - 1));
            double x = canvasCenterX(to);
            double y = canvasCenterY(to);
            if (manhattan(from, to) <= 1) {
                x = lerp(canvasCenterX(from), x, alpha);
                y = lerp(canvasCenterY(from), y, alpha);
            }
            points.add(new Point2D(x, y));
        }

        boolean dead = game.state() == GameState.GAME_OVER;
        double bodyWidth = Math.max(3, cell * 0.70);
        Point2D head = points.get(0);
        Point2D tail = points.get(points.size() - 1);

        Paint bodyPaint;
        if (dead) {
            bodyPaint = new LinearGradient(head.getX(), head.getY(), tail.getX(), tail.getY(),
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, Palette.DEAD_BODY), new Stop(1, Palette.DEAD_TAIL));
        } else if (head.distance(tail) < 2) {
            bodyPaint = Palette.ACCENT;
        } else {
            bodyPaint = new LinearGradient(head.getX(), head.getY(), tail.getX(), tail.getY(),
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#C2FFEA")),
                    new Stop(0.35, Palette.ACCENT),
                    new Stop(1, Color.web("#17805F")));
        }

        List<List<Point2D>> runs = splitRuns(points, cell);
        for (List<Point2D> run : runs) {
            if (run.size() >= 2) {
                gc.beginPath();
                gc.moveTo(run.get(0).getX(), run.get(0).getY());
                for (int i = 1; i < run.size(); i++) {
                    gc.lineTo(run.get(i).getX(), run.get(i).getY());
                }
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.setLineJoin(StrokeLineJoin.ROUND);
                if (!dead) {
                    gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.06));
                    gc.setLineWidth(bodyWidth * 3.4);
                    gc.stroke();
                    gc.setStroke(Color.color(0.39, 0.95, 0.75, 0.09));
                    gc.setLineWidth(bodyWidth * 2.0);
                    gc.stroke();
                }
                gc.setStroke(bodyPaint);
                gc.setLineWidth(bodyWidth);
                gc.stroke();
            } else if (!run.isEmpty()) {
                Point2D point = run.get(0);
                gc.setFill(dead ? Palette.DEAD_BODY : Palette.ACCENT);
                gc.fillOval(point.getX() - bodyWidth / 2, point.getY() - bodyWidth / 2,
                        bodyWidth, bodyWidth);
            }
        }

        drawHead(gc, head.getX(), head.getY(), cell, dead);
    }

    private static List<List<Point2D>> splitRuns(List<Point2D> points, double cell) {
        List<List<Point2D>> runs = new ArrayList<>();
        if (points.isEmpty()) {
            return runs;
        }
        List<Point2D> current = new ArrayList<>();
        current.add(points.get(0));
        double limit = cell * 1.5;
        for (int i = 1; i < points.size(); i++) {
            Point2D previous = points.get(i - 1);
            Point2D point = points.get(i);
            if (previous.distance(point) > limit) {
                runs.add(current);
                current = new ArrayList<>();
            }
            current.add(point);
        }
        runs.add(current);
        return runs;
    }

    private void drawHead(GraphicsContext gc, double x, double y, double cell, boolean dead) {
        double radius = cell * 0.42;
        if (dead) {
            gc.setFill(DEAD_HEAD);
        } else {
            gc.setFill(new RadialGradient(0, 0, x - radius * 0.25, y - radius * 0.3,
                    radius * 1.4, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#D3FFF1")),
                    new Stop(0.55, Palette.ACCENT_LIGHT),
                    new Stop(1, Color.web("#47DFB1"))));
        }
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        if (!dead) {
            gc.setFill(Color.color(1, 1, 1, 0.16));
            gc.fillOval(x - radius * 0.5, y - radius * 0.62, radius * 0.75, radius * 0.55);
        }

        Direction direction = game.direction();
        double forwardX = direction.dx();
        double forwardY = direction.dy();
        double sideX = -direction.dy();
        double sideY = direction.dx();
        double eyeForward = cell * 0.13;
        double eyeSide = cell * 0.16;
        double eyeRadius = Math.max(1.6, cell * 0.085);

        for (int sign = -1; sign <= 1; sign += 2) {
            double eyeX = x + forwardX * eyeForward + sideX * eyeSide * sign;
            double eyeY = y + forwardY * eyeForward + sideY * eyeSide * sign;
            if (dead) {
                double arm = eyeRadius * 1.1;
                gc.setStroke(EYE_DARK);
                gc.setLineWidth(Math.max(1.2, cell * 0.05));
                gc.strokeLine(eyeX - arm, eyeY - arm, eyeX + arm, eyeY + arm);
                gc.strokeLine(eyeX + arm, eyeY - arm, eyeX - arm, eyeY + arm);
            } else {
                gc.setFill(EYE_DARK);
                gc.fillOval(eyeX - eyeRadius, eyeY - eyeRadius,
                        eyeRadius * 2, eyeRadius * 2);
                double glint = Math.max(0.7, eyeRadius * 0.28);
                gc.setFill(Color.color(1, 1, 1, 0.75));
                gc.fillOval(eyeX - eyeRadius * 0.55, eyeY - eyeRadius * 0.55, glint, glint);
            }
        }
    }

    private void drawSparks(GraphicsContext gc) {
        for (Spark spark : sparks) {
            double lifeRatio = clamp(spark.life / spark.maxLife, 0, 1);
            gc.setFill(Color.color(spark.color.getRed(), spark.color.getGreen(),
                    spark.color.getBlue(), lifeRatio * 0.85));
            double size = spark.size * (0.4 + lifeRatio * 0.6);
            gc.fillOval(spark.x - size / 2, spark.y - size / 2, size, size);
        }
    }

    private void drawRings(GraphicsContext gc) {
        for (Ring ring : rings) {
            double progress = 1 - clamp(ring.life / ring.maxLife, 0, 1);
            double radius = ring.startRadius + (ring.endRadius - ring.startRadius) * progress;
            gc.setStroke(Color.color(ring.color.getRed(), ring.color.getGreen(),
                    ring.color.getBlue(), (1 - progress) * 0.7));
            gc.setLineWidth(1.6);
            gc.strokeOval(ring.x - radius, ring.y - radius, radius * 2, radius * 2);
        }
    }

    private void drawScorePops(GraphicsContext gc) {
        if (scorePops.isEmpty()) {
            return;
        }
        gc.save();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        double size = Math.max(10, cellSize() * 0.52);
        gc.setFont(Font.font(Fonts.FAMILY, FontWeight.SEMI_BOLD, size));
        for (ScorePop pop : scorePops) {
            double lifeRatio = clamp(pop.life / 0.8, 0, 1);
            gc.setFill(Color.color(pop.color.getRed(), pop.color.getGreen(),
                    pop.color.getBlue(), lifeRatio * 0.9));
            gc.fillText("+" + pop.value, pop.x, pop.y);
        }
        gc.restore();
    }

    private static long manhattan(Cell a, Cell b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void syncBody() {
        currentCells = game.body();
        previousCells = currentCells;
    }

    // ------------------------------------------------------------------
    // Propriedades observáveis
    // ------------------------------------------------------------------

    public ReadOnlyIntegerProperty scoreProperty() {
        return score.getReadOnlyProperty();
    }

    public int getScore() {
        return score.get();
    }

    public ReadOnlyIntegerProperty bestProperty() {
        return best.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<GameState> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty mutedProperty() {
        return muted.getReadOnlyProperty();
    }

    public GameMode mode() {
        return game.mode();
    }

    private static final class Spark {
        double x;
        double y;
        double vx;
        double vy;
        double life;
        double maxLife;
        double size;
        Color color;
    }

    private static final class ScorePop {
        double x;
        double y;
        double life = 0.8;
        final int value;
        final Color color;

        ScorePop(double x, double y, int value, Color color) {
            this.x = x;
            this.y = y;
            this.value = value;
            this.color = color;
        }
    }

    private static final class Ring {
        final double x;
        final double y;
        final double startRadius;
        final double endRadius;
        final double maxLife;
        double life;
        final Color color;

        Ring(double x, double y, double startRadius, double endRadius,
             double life, Color color) {
            this.x = x;
            this.y = y;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.maxLife = life;
            this.life = life;
            this.color = color;
        }
    }
}
