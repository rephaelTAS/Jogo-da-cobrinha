package com.cobrinha.core;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Motor do jogo, sem nenhuma dependência de JavaFX. Suporta os três modos
 * clássicos da cobrinha:
 *
 * <ul>
 *   <li>{@link GameMode#WRAP}: atravessa as bordas;</li>
 *   <li>{@link GameMode#CLASSIC}: paredes matam;</li>
 *   <li>{@link GameMode#MAZE}: atravessa as bordas e um labirinto muda de
 *       posição a cada passagem.</li>
 * </ul>
 *
 * <p>Além da fruta pequena, a cada cinco frutas pequenas comidas aparece uma
 * fruta dourada que vale 5 pontos e desaparece depois de um tempo.</p>
 */
public final class SnakeGame {

    public static final int DEFAULT_COLUMNS = 22;
    public static final int DEFAULT_ROWS = 22;

    public static final int POINTS_SMALL = 1;
    public static final int POINTS_BIG = 5;
    public static final long BIG_FOOD_LIFETIME_MS = 9_000;

    private static final int MIN_BOARD_SIZE = 6;
    private static final int MIN_MAZE_BOARD_SIZE = 12;
    private static final int START_LENGTH = 4;
    private static final int BASE_INTERVAL_MS = 150;
    private static final int MIN_INTERVAL_MS = 72;
    private static final int INTERVAL_STEP_MS = 4;
    private static final int MAX_BUFFERED_TURNS = 2;
    private static final int SMALLS_PER_BIG = 5;

    /**
     * Labirinto base desenhado em uma grade 22x22. A cada passagem de borda o
     * padrão inteiro é deslocado e "dá a volta" no tabuleiro, como um toro.
     */
    private static final Cell[] BASE_MAZE = {
        // L superior esquerdo
        new Cell(4, 5), new Cell(5, 5), new Cell(6, 5), new Cell(7, 5),
        new Cell(8, 5), new Cell(8, 6), new Cell(8, 7), new Cell(8, 8),
        // L superior direito
        new Cell(13, 5), new Cell(14, 5), new Cell(15, 5), new Cell(16, 5),
        new Cell(17, 5), new Cell(13, 6), new Cell(13, 7), new Cell(13, 8),
        // L inferior esquerdo
        new Cell(4, 16), new Cell(5, 16), new Cell(6, 16), new Cell(7, 16),
        new Cell(8, 16), new Cell(8, 13), new Cell(8, 14), new Cell(8, 15),
        // L inferior direito
        new Cell(13, 16), new Cell(14, 16), new Cell(15, 16), new Cell(16, 16),
        new Cell(17, 16), new Cell(13, 13), new Cell(13, 14), new Cell(13, 15),
        // portões centrais
        new Cell(10, 9), new Cell(11, 9), new Cell(10, 12), new Cell(11, 12),
        // colunas laterais
        new Cell(3, 9), new Cell(3, 10), new Cell(3, 11), new Cell(3, 12),
        new Cell(18, 9), new Cell(18, 10), new Cell(18, 11), new Cell(18, 12)
    };

    private final GameMode mode;
    private final int columns;
    private final int rows;
    private final Random random;
    private final Deque<Cell> body = new ArrayDeque<>();
    private final Deque<Direction> bufferedTurns = new ArrayDeque<>();
    private final Set<Cell> obstacles = new HashSet<>();

    private Direction direction;
    private Cell smallFood;
    private Cell bigFood;
    private long bigFoodRemainingMs;
    private GameState state;
    private int score;
    private int smallsEaten;
    private int smallsTowardBig;
    private int intervalMs;
    private int mazeShiftX;
    private int mazeShiftY;
    private int mazeVersion;

    public SnakeGame(GameMode mode) {
        this(mode, DEFAULT_COLUMNS, DEFAULT_ROWS, new Random());
    }

    public SnakeGame(GameMode mode, int columns, int rows, Random random) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.random = Objects.requireNonNull(random, "random");
        if (columns < MIN_BOARD_SIZE || rows < MIN_BOARD_SIZE) {
            throw new IllegalArgumentException("O tabuleiro precisa ter pelo menos "
                    + MIN_BOARD_SIZE + "x" + MIN_BOARD_SIZE);
        }
        if (mode == GameMode.MAZE
                && (columns < MIN_MAZE_BOARD_SIZE || rows < MIN_MAZE_BOARD_SIZE)) {
            throw new IllegalArgumentException("O modo labirinto precisa de um tabuleiro "
                    + "de pelo menos " + MIN_MAZE_BOARD_SIZE + "x" + MIN_MAZE_BOARD_SIZE);
        }
        this.columns = columns;
        this.rows = rows;
        reset();
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    public void reset() {
        body.clear();
        bufferedTurns.clear();
        obstacles.clear();
        direction = Direction.RIGHT;
        smallFood = null;
        bigFood = null;
        bigFoodRemainingMs = 0;
        score = 0;
        smallsEaten = 0;
        smallsTowardBig = 0;
        intervalMs = BASE_INTERVAL_MS;
        mazeShiftX = 0;
        mazeShiftY = 0;
        mazeVersion = 0;
        state = GameState.READY;

        int y = rows / 2;
        int headX = Math.min(columns - 2, columns / 2 + 1);
        for (int i = 0; i < START_LENGTH; i++) {
            body.addLast(new Cell(headX - i, y));
        }

        if (mode.hasMaze()) {
            chooseInitialMaze();
        }
        if (!placeSmallFood()) {
            state = GameState.WON;
        }
    }

    public void restart() {
        reset();
        state = GameState.RUNNING;
    }

    public void start() {
        if (state == GameState.READY) {
            state = GameState.RUNNING;
        }
    }

    public void pause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
    }

    public GameState togglePause() {
        if (state == GameState.RUNNING) {
            pause();
        } else if (state == GameState.PAUSED) {
            resume();
        }
        return state;
    }

    /**
     * Enfileira uma curva. O comando é validado contra o último comando da fila
     * (ou a direção atual, se a fila estiver vazia), impedindo meia-volta.
     */
    public boolean requestDirection(Direction requested) {
        if (requested == null || state == GameState.PAUSED
                || state == GameState.GAME_OVER || state == GameState.WON) {
            return false;
        }
        Direction base = bufferedTurns.isEmpty() ? direction : bufferedTurns.peekLast();
        if (requested == base || requested == base.opposite()) {
            return false;
        }
        if (bufferedTurns.size() >= MAX_BUFFERED_TURNS) {
            return false;
        }
        bufferedTurns.addLast(requested);
        return true;
    }

    // ------------------------------------------------------------------
    // Simulação
    // ------------------------------------------------------------------

    public TickResult tick() {
        if (state != GameState.RUNNING) {
            return TickResult.idle();
        }

        Direction nextTurn = bufferedTurns.pollFirst();
        if (nextTurn != null) {
            direction = nextTurn;
        }

        Cell head = body.peekFirst();
        int targetX = head.x() + direction.dx();
        int targetY = head.y() + direction.dy();
        boolean outside = targetX < 0 || targetY < 0 || targetX >= columns || targetY >= rows;
        boolean wrapped = false;

        if (outside) {
            if (!mode.wraps()) {
                state = GameState.GAME_OVER;
                return new TickResult(false, false, false, false, false, false, false,
                        TickResult.Collision.WALL, new Cell(targetX, targetY), false);
            }
            targetX = Math.floorMod(targetX, columns);
            targetY = Math.floorMod(targetY, rows);
            wrapped = true;
        }

        Cell target = new Cell(targetX, targetY);
        boolean ateSmall = target.equals(smallFood);
        boolean ateBig = target.equals(bigFood);
        boolean willGrow = ateSmall || ateBig;

        Cell tail = body.peekLast();
        for (Cell segment : body) {
            // A cauda sai da célula neste mesmo passo quando a cobra não cresce.
            if (!willGrow && segment.equals(tail)) {
                continue;
            }
            if (segment.equals(target)) {
                state = GameState.GAME_OVER;
                return new TickResult(false, false, false, false, false, wrapped,
                        false, TickResult.Collision.SELF, target, false);
            }
        }

        boolean mazeShifted = false;
        if (mode.hasMaze()) {
            if (wrapped) {
                mazeShifted = shiftMazeOnWrap(target);
            }
            if (obstacles.contains(target)) {
                state = GameState.GAME_OVER;
                return new TickResult(false, false, false, false, false, wrapped,
                        false, TickResult.Collision.OBSTACLE, target, false);
            }
        }

        body.addFirst(target);
        if (!willGrow) {
            body.removeLast();
        }

        boolean bigSpawned = false;
        boolean bigExpired = false;
        boolean won = false;

        if (ateSmall) {
            score += POINTS_SMALL;
            smallsEaten++;
            smallsTowardBig++;
            intervalMs = Math.max(MIN_INTERVAL_MS,
                    BASE_INTERVAL_MS - smallsEaten * INTERVAL_STEP_MS);

            boolean smallPlaced = placeSmallFood();
            if (smallPlaced && smallsTowardBig >= SMALLS_PER_BIG) {
                bigSpawned = placeBigFood();
                if (bigSpawned) {
                    smallsTowardBig -= SMALLS_PER_BIG;
                }
            }
            if (!smallPlaced) {
                smallFood = null;
                state = GameState.WON;
                won = true;
            }
        } else if (ateBig) {
            score += POINTS_BIG;
            bigFood = null;
            bigFoodRemainingMs = 0;
        }

        if (bigFood != null && !ateBig && !bigSpawned) {
            bigFoodRemainingMs -= intervalMs;
            if (bigFoodRemainingMs <= 0) {
                bigFood = null;
                bigFoodRemainingMs = 0;
                bigExpired = true;
            }
        }

        return new TickResult(true, ateSmall, ateBig, bigSpawned, bigExpired,
                wrapped, mazeShifted, TickResult.Collision.NONE, target, won);
    }

    // ------------------------------------------------------------------
    // Comida
    // ------------------------------------------------------------------

    private boolean placeSmallFood() {
        List<Cell> free = freeCells(bigFood == null ? Set.of() : Set.of(bigFood));
        if (free.isEmpty()) {
            smallFood = null;
            return false;
        }
        smallFood = free.get(random.nextInt(free.size()));
        return true;
    }

    private boolean placeBigFood() {
        List<Cell> free = freeCells(smallFood == null ? Set.of() : Set.of(smallFood));
        if (free.isEmpty()) {
            return false;
        }
        bigFood = free.get(random.nextInt(free.size()));
        bigFoodRemainingMs = BIG_FOOD_LIFETIME_MS;
        return true;
    }

    private List<Cell> freeCells(Set<Cell> extraExcluded) {
        List<Cell> free = new ArrayList<>(columns * rows);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                Cell cell = new Cell(x, y);
                if (body.contains(cell) || obstacles.contains(cell)
                        || extraExcluded.contains(cell)) {
                    continue;
                }
                free.add(cell);
            }
        }
        return free;
    }

    // ------------------------------------------------------------------
    // Labirinto
    // ------------------------------------------------------------------

    private void chooseInitialMaze() {
        Cell head = body.peekFirst();
        Set<Cell> forbidden = new HashSet<>(body);
        forbidden.addAll(safeZone(head, direction));

        for (int attempt = 0; attempt < 120; attempt++) {
            int shiftX = random.nextInt(columns);
            int shiftY = random.nextInt(rows);
            if (tryApplyMaze(shiftX, shiftY, forbidden, true)) {
                return;
            }
        }
        applyPartialMaze(random.nextInt(columns), random.nextInt(rows), forbidden);
    }

    /**
     * Reconstrói o labirinto quando a cobra atravessa uma borda. O novo padrão
     * nunca pode ocupar a nova cabeça, o corpo, a zona de segurança ou as frutas.
     */
    private boolean shiftMazeOnWrap(Cell newHead) {
        Set<Cell> forbidden = new LinkedHashSet<>(body);
        forbidden.add(newHead);
        forbidden.addAll(safeZone(newHead, direction));
        if (smallFood != null) {
            forbidden.add(smallFood);
        }
        if (bigFood != null) {
            forbidden.add(bigFood);
        }

        for (int attempt = 0; attempt < 150; attempt++) {
            int shiftX = random.nextInt(columns);
            int shiftY = random.nextInt(rows);
            if (shiftX == mazeShiftX && shiftY == mazeShiftY) {
                continue;
            }
            if (tryApplyMaze(shiftX, shiftY, forbidden, false)) {
                return true;
            }
        }

        // Sem posição totalmente livre: mantém o padrão, removendo apenas as
        // células que conflitariam com a cobra/zonas seguras/frutas.
        int shiftX = (mazeShiftX + 1 + random.nextInt(Math.max(1, columns - 1))) % columns;
        int shiftY = (mazeShiftY + 1 + random.nextInt(Math.max(1, rows - 1))) % rows;
        applyPartialMaze(shiftX, shiftY, forbidden);
        return true;
    }

    private boolean tryApplyMaze(int shiftX, int shiftY, Set<Cell> forbidden,
                                 boolean allowSameShift) {
        if (!allowSameShift && shiftX == mazeShiftX && shiftY == mazeShiftY) {
            return false;
        }
        Set<Cell> candidate = new HashSet<>();
        for (Cell wall : BASE_MAZE) {
            Cell shifted = new Cell(Math.floorMod(wall.x() + shiftX, columns),
                    Math.floorMod(wall.y() + shiftY, rows));
            if (forbidden.contains(shifted)) {
                return false;
            }
            candidate.add(shifted);
        }
        if (candidate.isEmpty()) {
            return false;
        }
        if (!allowSameShift && candidate.equals(obstacles)) {
            return false;
        }
        applyMaze(candidate, shiftX, shiftY);
        return true;
    }

    private void applyPartialMaze(int shiftX, int shiftY, Set<Cell> forbidden) {
        Set<Cell> candidate = new HashSet<>();
        for (Cell wall : BASE_MAZE) {
            Cell shifted = new Cell(Math.floorMod(wall.x() + shiftX, columns),
                    Math.floorMod(wall.y() + shiftY, rows));
            if (!forbidden.contains(shifted)) {
                candidate.add(shifted);
            }
        }
        applyMaze(candidate, shiftX, shiftY);
    }

    private void applyMaze(Set<Cell> candidate, int shiftX, int shiftY) {
        obstacles.clear();
        obstacles.addAll(candidate);
        mazeShiftX = shiftX;
        mazeShiftY = shiftY;
        mazeVersion++;
    }

    private Set<Cell> safeZone(Cell head, Direction facing) {
        Set<Cell> zone = new LinkedHashSet<>();
        zone.add(head);
        zone.add(wrap(head.x() + 1, head.y()));
        zone.add(wrap(head.x() - 1, head.y()));
        zone.add(wrap(head.x(), head.y() + 1));
        zone.add(wrap(head.x(), head.y() - 1));
        Cell step = head;
        for (int i = 0; i < 2; i++) {
            step = wrap(step.x() + facing.dx(), step.y() + facing.dy());
            zone.add(step);
        }
        return zone;
    }

    private Cell wrap(int x, int y) {
        return new Cell(Math.floorMod(x, columns), Math.floorMod(y, rows));
    }

    // ------------------------------------------------------------------
    // Acesso ao estado
    // ------------------------------------------------------------------

    public GameMode mode() {
        return mode;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public GameState state() {
        return state;
    }

    public int score() {
        return score;
    }

    public int smallsEaten() {
        return smallsEaten;
    }

    public Direction direction() {
        return direction;
    }

    public Cell smallFood() {
        return smallFood;
    }

    public Cell bigFood() {
        return bigFood;
    }

    /**
     * Fração restante de vida da fruta dourada (0 a 1), útil para o anel de
     * contagem regressiva na interface.
     */
    public double bigFoodFraction() {
        if (bigFood == null) {
            return 0;
        }
        return Math.max(0, Math.min(1, bigFoodRemainingMs / (double) BIG_FOOD_LIFETIME_MS));
    }

    public long bigFoodRemainingMs() {
        return bigFoodRemainingMs;
    }

    public int tickIntervalMs() {
        return intervalMs;
    }

    public int mazeVersion() {
        return mazeVersion;
    }

    public Set<Cell> obstacles() {
        return Collections.unmodifiableSet(obstacles);
    }

    public List<Cell> body() {
        return List.copyOf(body);
    }

    public int length() {
        return body.size();
    }

    // ------------------------------------------------------------------
    // Ganchos usados apenas pelos testes
    // ------------------------------------------------------------------

    void placeSmallFoodAt(Cell cell) {
        Objects.requireNonNull(cell, "cell");
        if (!cell.isInside(columns, rows) || body.contains(cell)
                || obstacles.contains(cell) || cell.equals(bigFood)) {
            throw new IllegalArgumentException("Fruta pequena precisa estar livre: " + cell);
        }
        smallFood = cell;
    }

    void placeBigFoodAt(Cell cell) {
        Objects.requireNonNull(cell, "cell");
        if (!cell.isInside(columns, rows) || body.contains(cell)
                || obstacles.contains(cell) || cell.equals(smallFood)) {
            throw new IllegalArgumentException("Fruta dourada precisa estar livre: " + cell);
        }
        bigFood = cell;
        bigFoodRemainingMs = BIG_FOOD_LIFETIME_MS;
    }

    void setBigFoodRemainingMs(long remaining) {
        bigFoodRemainingMs = Math.max(0, remaining);
    }

    void addObstacle(Cell cell) {
        obstacles.add(Objects.requireNonNull(cell, "cell"));
    }

    void clearObstacles() {
        obstacles.clear();
    }
}
