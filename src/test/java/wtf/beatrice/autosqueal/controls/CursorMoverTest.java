package wtf.beatrice.autosqueal.controls;

import org.junit.jupiter.api.Test;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorMoverTest
{
    private static final Point CURSOR_AT = new Point(100, 100);

    private final RobotMouseTracker robotTracker = new RobotMouseTracker();
    private final AutoSquealConfig config = new AutoSquealConfig();
    private final RecordingExecutor executor = new RecordingExecutor();

    /** Bounds of a pretend multi-screen setup: the main screen is offset. */
    private final Rectangle movementBounds = new Rectangle(100, 50, 800, 600);
    private final Rectangle primaryBounds = new Rectangle(100, 50, 800, 600);

    /** A mover that queues its movements on the recording executor. */
    private CursorMover mover() {
        return new CursorMover(executor, robotTracker, () -> true, config,
                () -> movementBounds, () -> primaryBounds,
                new Random(42), () -> CURSOR_AT);
    }

    private List<SingleStepMovementTask> movements() {
        List<SingleStepMovementTask> movements = new ArrayList<>();
        for (Runnable movement : executor.movements) {
            movements.add((SingleStepMovementTask) movement);
        }
        return movements;
    }

    @Test
    void movementsAreSkippedWhileTheUserIsPresent() {
        // the automation only moves the mouse when the user is away
        CursorMover presentMover = new CursorMover(executor, robotTracker, () -> false, config,
                () -> movementBounds, () -> primaryBounds,
                new Random(42), () -> CURSOR_AT);

        for (int i = 0; i < 10; i++) {
            presentMover.run();
        }

        assertTrue(executor.movements.isEmpty());
    }

    @Test
    void randomMovementsStayWithinTheBounds() {
        config.setClickEnabled(false);

        CursorMover cursorMover = mover();
        for (int i = 0; i < 50; i++) {
            cursorMover.run();
        }

        List<SingleStepMovementTask> movements = movements();
        assertEquals(50, movements.size());
        for (SingleStepMovementTask movement : movements) {
            assertFalse(movement.isClick());
            assertTrue(movement.getDestX() >= 100, "destX below the bounds: " + movement.getDestX());
            assertTrue(movement.getDestX() < 900, "destX above the bounds: " + movement.getDestX());
            assertTrue(movement.getDestY() >= 50, "destY below the bounds: " + movement.getDestY());
            assertTrue(movement.getDestY() < 650, "destY above the bounds: " + movement.getDestY());
        }
    }

    @Test
    void cornerClicksHappenEveryNthMovement() {
        config.setClickEveryNMoves(3);

        CursorMover cursorMover = mover();
        for (int i = 0; i < 6; i++) {
            cursorMover.run();
        }

        List<SingleStepMovementTask> movements = movements();
        assertEquals(6, movements.size());

        // one movement every 3: the 3rd and the 6th are corner clicks
        assertFalse(movements.get(0).isClick());
        assertFalse(movements.get(1).isClick());
        assertTrue(movements.get(2).isClick());
        assertFalse(movements.get(3).isClick());
        assertFalse(movements.get(4).isClick());
        assertTrue(movements.get(5).isClick());
    }

    @Test
    void cornerClicksAimAtTheMainScreensTopRight() {
        // the click must open the notifications, which live on the main
        // screen's corner: wherever the movement bounds are, the click
        // destination follows the main screen
        config.setClickEveryNMoves(3);
        Rectangle elsewhere = new Rectangle(5000, 5000, 200, 200);
        CursorMover cursorMover = new CursorMover(executor, robotTracker, () -> true, config,
                () -> elsewhere, () -> primaryBounds,
                new Random(42), () -> CURSOR_AT);

        cursorMover.run(); // iteration 0: a normal movement
        cursorMover.run(); // iteration 1: a normal movement
        cursorMover.run(); // iteration 2: the corner click

        List<SingleStepMovementTask> movements = movements();
        assertTrue(movements.get(2).isClick());
        // the main screen's top-right corner, 5px inside
        assertEquals(100 + 800 - 5, movements.get(2).getDestX());
        assertEquals(50 + 5, movements.get(2).getDestY());
    }

    @Test
    void clicksCanBeTurnedOff() {
        config.setClickEnabled(false);

        CursorMover cursorMover = mover();
        for (int i = 0; i < 10; i++) {
            cursorMover.run();
        }

        for (SingleStepMovementTask movement : movements()) {
            assertFalse(movement.isClick());
        }
    }

    @Test
    void clickCadenceIsReadLive() {
        config.setClickEveryNMoves(2);

        CursorMover cursorMover = mover();
        cursorMover.run(); // iteration 0
        cursorMover.run(); // iteration 1: click, cadence 2
        assertTrue(movements().get(1).isClick());

        // the user loosens the cadence while the automation runs
        config.setClickEveryNMoves(4);
        cursorMover.run(); // iteration 0
        cursorMover.run(); // iteration 1
        cursorMover.run(); // iteration 2
        cursorMover.run(); // iteration 3: click, cadence 4

        List<SingleStepMovementTask> movements = movements();
        assertEquals(6, movements.size());
        assertFalse(movements.get(2).isClick());
        assertFalse(movements.get(3).isClick());
        assertFalse(movements.get(4).isClick());
        assertTrue(movements.get(5).isClick());
    }

    /** An executor that records the tasks instead of running them. */
    private static class RecordingExecutor extends AbstractExecutorService {

        final List<Runnable> movements = new ArrayList<>();

        public void execute(Runnable command) {
            movements.add(command);
        }

        public void shutdown() { }

        public List<Runnable> shutdownNow() {
            return List.of();
        }

        public boolean isShutdown() {
            return false;
        }

        public boolean isTerminated() {
            return false;
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }
    }
}