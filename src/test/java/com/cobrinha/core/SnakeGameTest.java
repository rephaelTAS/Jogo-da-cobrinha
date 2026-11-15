package com.cobrinha.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SnakeGameTest {

    private static SnakeGame classic() {
        return new SnakeGame(GameMode.CLASSIC, 8, 8, new Random(7));
    }

    private static SnakeGame wrap() {
        return new SnakeGame(GameMode.WRAP, 8, 8, new Random(1));
    }

    private static SnakeGame maze() {
        return new SnakeGame(GameMode.MAZE, 22, 22, new Random(4));
    }

    private static Cell freeCell(SnakeGame game, Cell alsoAvoid) {
        for (int y = 0; y < game.rows(); y++) {
            for (int x = 0; x < game.columns(); x++) {
                Cell cell = new Cell(x, y);
                if (game.body().contains(cell) || game.obstacles().contains(cell)
                        || cell.equals(game.smallFood()) || cell.equals(game.bigFood())
                        || cell.equals(alsoAvoid)) {
                    continue;
                }
                return cell;
            }
        }
        throw new AssertionError("Sem célula livre para os testes");
    }

    @Test
    void startsReadyWithFourSegments() {
        SnakeGame game = classic();

        assertEquals(GameMode.CLASSIC, game.mode());
        assertEquals(GameState.READY, game.state());
        assertEquals(4, game.length());
        assertEquals(Direction.RIGHT, game.direction());
        assertEquals(0, game.score());
        assertEquals(new Cell(5, 4), game.body().getFirst());
        assertNotNull(game.smallFood());
        assertNull(game.bigFood());
        assertFalse(game.body().contains(game.smallFood()));
        assertTrue(game.obstacles().isEmpty());
    }

    @Test
    void tickOnlyRunsWhilePlaying() {
        SnakeGame game = classic();
        var before = game.body();

        assertFalse(game.tick().moved());
        assertEquals(before, game.body());

        game.start();
        assertTrue(game.tick().moved());
        assertEquals(GameState.RUNNING, game.state());
        assertEquals(new Cell(6, 4), game.body().getFirst());
        assertEquals(4, game.length());
    }

    @Test
    void refusesOppositeDirectionAndBuffersTwoTurns() {
        SnakeGame game = classic();
        game.start();

        assertFalse(game.requestDirection(Direction.LEFT));
        assertFalse(game.requestDirection(Direction.RIGHT));
        assertTrue(game.requestDirection(Direction.UP));
        assertTrue(game.requestDirection(Direction.LEFT));
        assertFalse(game.requestDirection(Direction.DOWN));

        game.tick();
        assertEquals(Direction.UP, game.direction());
        game.tick();
        assertEquals(Direction.LEFT, game.direction());
    }

    @Test
    void eatingSmallFoodGivesOnePointGrowsAndSpeedsUp() {
        SnakeGame game = classic();
        game.start();
        parqueSmall(game);
        Cell target = game.body().getFirst().translate(game.direction());
        game.placeSmallFoodAt(target);
        int intervalBefore = game.tickIntervalMs();

        TickResult result = game.tick();

        assertTrue(result.moved());
        assertTrue(result.ateSmall());
        assertFalse(result.ateBig());
        assertFalse(result.died());
        assertEquals(5, game.length());
        assertEquals(1, game.score());
        assertEquals(1, game.smallsEaten());
        assertEquals(target, game.body().getFirst());
        assertTrue(game.tickIntervalMs() < intervalBefore);
        assertNotNull(game.smallFood());
        assertFalse(game.body().contains(game.smallFood()));
    }

    private static void parqueSmall(SnakeGame game) {
        game.placeSmallFoodAt(freeCell(game, null));
    }

    @Test
    void speedStopsAtTheMinimum() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 60, 8, new Random(3));
        game.start();

        for (int i = 0; i < 20; i++) {
            Cell next = game.body().getFirst().translate(game.direction());
            game.placeSmallFoodAt(next);
            game.tick();
            assertEquals(GameState.RUNNING, game.state());
        }

        assertEquals(72, game.tickIntervalMs());
    }

    @Test
    void wallKillsInClassicMode() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 6, 6, new Random(1));
        game.placeSmallFoodAt(new Cell(0, 0));
        game.start();

        game.tick();
        TickResult result = game.tick();

        assertTrue(result.died());
        assertEquals(TickResult.Collision.WALL, result.collision());
        assertEquals(GameState.GAME_OVER, game.state());
        assertEquals(new Cell(5, 3), game.body().getFirst());
    }

    @Test
    void wrapsAroundTheEdgeAndCanEatOnTheOtherSide() {
        SnakeGame game = wrap();
        game.placeSmallFoodAt(new Cell(0, 4));
        game.start();

        TickResult first = game.tick();
        assertFalse(first.wrapped());
        game.tick();
        TickResult wrapTick = game.tick();

        assertTrue(wrapTick.wrapped());
        assertTrue(wrapTick.moved());
        assertTrue(wrapTick.ateSmall());
        assertEquals(new Cell(0, 4), game.body().getFirst());
        assertEquals(5, game.length());
        assertEquals(1, game.score());
        assertEquals(GameState.RUNNING, game.state());
    }

    @Test
    void chasingTheOwnTailIsAllowed() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 8, 8, new Random(2));
        game.placeSmallFoodAt(new Cell(0, 0));
        game.start();

        game.requestDirection(Direction.UP);
        game.tick();
        game.requestDirection(Direction.LEFT);
        game.tick();
        game.requestDirection(Direction.DOWN);
        TickResult result = game.tick();

        assertFalse(result.died());
        assertEquals(GameState.RUNNING, game.state());
        assertEquals(new Cell(4, 4), game.body().getFirst());
    }

    @Test
    void bitingTheBodyEndsTheGame() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 8, 8, new Random(4));
        game.start();
        game.placeSmallFoodAt(new Cell(6, 4));
        game.tick();
        assertEquals(5, game.length());

        game.placeSmallFoodAt(new Cell(0, 0));
        game.requestDirection(Direction.UP);
        game.tick();
        game.requestDirection(Direction.LEFT);
        game.tick();
        game.requestDirection(Direction.DOWN);
        TickResult result = game.tick();

        assertTrue(result.died());
        assertEquals(TickResult.Collision.SELF, result.collision());
        assertEquals(GameState.GAME_OVER, game.state());
    }

    @Test
    void pauseFreezesTheGameAndResumeContinues() {
        SnakeGame game = classic();
        game.start();
        game.tick();
        var body = game.body();

        game.pause();
        assertEquals(GameState.PAUSED, game.state());
        assertFalse(game.tick().moved());
        assertEquals(body, game.body());
        assertFalse(game.requestDirection(Direction.UP));

        game.resume();
        assertEquals(GameState.RUNNING, game.state());
        assertTrue(game.tick().moved());
    }

    @Test
    void restartResetsBodyScoreAndQueue() {
        SnakeGame game = classic();
        game.start();
        game.placeSmallFoodAt(game.body().getFirst().translate(Direction.RIGHT));
        game.tick();
        game.requestDirection(Direction.UP);

        game.restart();
        game.tick();

        assertEquals(GameState.RUNNING, game.state());
        assertEquals(0, game.score());
        assertEquals(4, game.length());
        assertEquals(Direction.RIGHT, game.direction());
        assertEquals(new Cell(6, 4), game.body().getFirst());
    }

    @Test
    void togglePauseOnlyWorksDuringPlay() {
        SnakeGame game = classic();
        assertEquals(GameState.READY, game.togglePause());
        game.start();
        assertEquals(GameState.PAUSED, game.togglePause());
        assertEquals(GameState.RUNNING, game.togglePause());
    }

    @Test
    void directionIsIgnoredAfterGameOver() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 6, 6, new Random(5));
        game.placeSmallFoodAt(new Cell(0, 0));
        game.start();
        game.tick();
        game.tick();

        assertEquals(GameState.GAME_OVER, game.state());
        assertFalse(game.requestDirection(Direction.UP));
    }

    @Test
    void classicBoardMustBeBigEnough() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnakeGame(GameMode.CLASSIC, 5, 26, new Random(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new SnakeGame(GameMode.CLASSIC, 26, 4, new Random(1)));
    }

    @Test
    void bigFoodAppearsAfterFiveSmallFoods() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 60, 8, new Random(8));
        game.start();

        for (int i = 1; i <= 4; i++) {
            Cell next = game.body().getFirst().translate(game.direction());
            game.placeSmallFoodAt(next);
            TickResult result = game.tick();
            assertTrue(result.ateSmall());
            assertFalse(result.bigSpawned());
            assertNull(game.bigFood());
        }

        Cell next = game.body().getFirst().translate(game.direction());
        game.placeSmallFoodAt(next);
        TickResult result = game.tick();

        assertTrue(result.ateSmall());
        assertTrue(result.bigSpawned());
        assertEquals(5, game.score());
        assertNotNull(game.bigFood());
        assertNotNull(game.smallFood());
        assertNotEquals(game.bigFood(), game.smallFood());
        assertFalse(game.body().contains(game.bigFood()));
        assertTrue(game.bigFoodFraction() > 0.9);
    }

    @Test
    void eatingBigFoodGivesFivePointsAndKeepsSmallFood() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 60, 8, new Random(9));
        game.start();

        for (int i = 0; i < 5; i++) {
            Cell next = game.body().getFirst().translate(game.direction());
            game.placeSmallFoodAt(next);
            game.tick();
        }
        assertEquals(5, game.score());
        assertNotNull(game.bigFood());

        game.placeSmallFoodAt(freeCell(game, null));
        Cell head = game.body().getFirst();
        Cell bigTarget = head.translate(game.direction());
        game.placeBigFoodAt(bigTarget);
        int lengthBefore = game.length();
        Cell smallBefore = game.smallFood();

        TickResult result = game.tick();

        assertTrue(result.ateBig());
        assertFalse(result.ateSmall());
        assertEquals(10, game.score());
        assertNull(game.bigFood());
        assertEquals(lengthBefore + 1, game.length());
        assertEquals(smallBefore, game.smallFood());
    }

    @Test
    void bigFoodExpiresAndSmallFoodStays() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 30, 8, new Random(10));
        game.start();
        Cell small = freeCell(game, null);
        game.placeSmallFoodAt(small);
        game.placeBigFoodAt(freeCell(game, small));
        game.setBigFoodRemainingMs(1);

        TickResult result = game.tick();

        assertTrue(result.bigExpired());
        assertFalse(result.ateBig());
        assertNull(game.bigFood());
        assertEquals(0, game.score());
        assertEquals(small, game.smallFood());
    }

    @Test
    void bigFoodDoesNotSpawnOnTheSnakeOrSmallFood() {
        SnakeGame game = new SnakeGame(GameMode.CLASSIC, 60, 8, new Random(11));
        game.start();
        for (int i = 0; i < 5; i++) {
            game.placeSmallFoodAt(game.body().getFirst().translate(game.direction()));
            game.tick();
        }

        assertNotNull(game.bigFood());
        assertFalse(game.body().contains(game.bigFood()));
        assertNotEquals(game.smallFood(), game.bigFood());
        assertTrue(game.obstacles().isEmpty());
    }

    @Test
    void mazeStartsWithValidObstacles() {
        SnakeGame game = maze();

        assertNotNull(game.smallFood());
        assertFalse(game.obstacles().isEmpty());
        Set<Cell> occupied = new HashSet<>(game.body());
        occupied.add(game.smallFood());
        for (Cell wall : game.obstacles()) {
            assertFalse(occupied.contains(wall), "Obstáculo em cima de cobra/comida");
        }
    }

    @Test
    void mazeObstacleKillsOnNormalMove() {
        SnakeGame game = maze();
        game.clearObstacles();
        game.placeSmallFoodAt(freeCell(game, new Cell(13, 11)));
        game.addObstacle(new Cell(13, 11));
        game.start();

        TickResult result = game.tick();

        assertTrue(result.died());
        assertEquals(TickResult.Collision.OBSTACLE, result.collision());
        assertEquals(GameState.GAME_OVER, game.state());
    }

    @Test
    void mazeShiftsWhenWrapping() {
        SnakeGame game = maze();
        game.clearObstacles();
        game.placeSmallFoodAt(new Cell(0, 0));
        game.start();
        int versionBefore = game.mazeVersion();

        TickResult wrapTick = null;
        for (int i = 0; i < 12; i++) {
            TickResult result = game.tick();
            if (result.wrapped()) {
                wrapTick = result;
                break;
            }
            if (result.died()) {
                break;
            }
        }

        assertNotNull(wrapTick, "A cobra deveria ter atravessado a borda");
        assertTrue(wrapTick.mazeShifted());
        assertTrue(game.mazeVersion() > versionBefore);
        assertFalse(game.obstacles().contains(game.body().getFirst()));
        assertFalse(game.obstacles().isEmpty());
        assertEquals(GameState.RUNNING, game.state());
    }

    @Test
    void mazeFoodNeverOverlapsObstacles() {
        SnakeGame game = maze();
        game.start();
        for (int i = 0; i < 8; i++) {
            Cell next = game.body().getFirst().translate(game.direction());
            if (next.isInside(game.columns(), game.rows())
                    && !game.obstacles().contains(next)) {
                game.placeSmallFoodAt(next);
                TickResult result = game.tick();
                if (result.died() || result.won()) {
                    break;
                }
            } else {
                game.requestDirection(Direction.UP);
                game.tick();
            }
            assertNotNull(game.smallFood());
            assertFalse(game.obstacles().contains(game.smallFood()));
            assertFalse(game.body().contains(game.smallFood()));
        }
    }
}
