package wtf.beatrice.autosqueal.controls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class SingleStepMovementTaskTest
{
    private static final int MAX_TICKS = 20_000;
    private static final float DELTA = 0.0f;

    @Test
    void noStepWhenAlreadyAtDestination() {
        assertEquals(0.0f, SingleStepMovementTask.computeStepX(0, 0), DELTA);
        assertEquals(0.0f, SingleStepMovementTask.computeStepY(0, 0), DELTA);
    }

    @Test
    void axisAlignedMovementsProduceNoInfiniteSteps() {
        // moving only horizontally: the y step must be 0, not Infinity or NaN
        assertEquals(1.0f, SingleStepMovementTask.computeStepX(100, 0), DELTA);
        assertEquals(0.0f, SingleStepMovementTask.computeStepY(100, 0), DELTA);

        // moving only vertically: the x step must be 0, not Infinity or NaN
        assertEquals(0.0f, SingleStepMovementTask.computeStepX(0, 100), DELTA);
        assertEquals(1.0f, SingleStepMovementTask.computeStepY(0, 100), DELTA);
    }

    @Test
    void diagonalMovementsMoveBothAxes() {
        assertEquals(1.0f, SingleStepMovementTask.computeStepX(100, 100), DELTA);
        assertEquals(1.0f, SingleStepMovementTask.computeStepY(100, 100), DELTA);

        assertEquals(0.5f, SingleStepMovementTask.computeStepX(50, 100), DELTA);
        assertEquals(1.0f, SingleStepMovementTask.computeStepY(50, 100), DELTA);

        assertEquals(1.0f, SingleStepMovementTask.computeStepX(100, 50), DELTA);
        assertEquals(0.5f, SingleStepMovementTask.computeStepY(100, 50), DELTA);
    }

    @Test
    void stepsAreNeverInfiniteOrNaN() {
        for (int lengthX = 0; lengthX <= 400; lengthX += 7) {
            for (int lengthY = 0; lengthY <= 400; lengthY += 7) {
                float stepX = SingleStepMovementTask.computeStepX(lengthX, lengthY);
                float stepY = SingleStepMovementTask.computeStepY(lengthX, lengthY);

                assertFalse(Float.isInfinite(stepX), "infinite stepX for lengths [" + lengthX + ", " + lengthY + "]");
                assertFalse(Float.isNaN(stepX), "NaN stepX for lengths [" + lengthX + ", " + lengthY + "]");
                assertFalse(Float.isInfinite(stepY), "infinite stepY for lengths [" + lengthX + ", " + lengthY + "]");
                assertFalse(Float.isNaN(stepY), "NaN stepY for lengths [" + lengthX + ", " + lengthY + "]");
            }
        }
    }

    @Test
    void advanceMovesTowardTheDestination() {
        assertEquals(5.0f, SingleStepMovementTask.advance(5.0f, 5.0f, 1.0f), DELTA); // already there
        assertEquals(9.0f, SingleStepMovementTask.advance(10.0f, 5.0f, 1.0f), DELTA); // decreasing
        assertEquals(6.0f, SingleStepMovementTask.advance(5.0f, 10.0f, 1.0f), DELTA); // increasing
    }

    @Test
    void adjustedStepUsesExactRemainderWhenClose() {
        assertEquals(0.4f, SingleStepMovementTask.adjustedStep(10.0f, 10.4f, 1.0f), 0.00001f);
        assertEquals(0.4f, SingleStepMovementTask.adjustedStep(10.8f, 10.4f, 1.0f), 0.00001f);
        assertEquals(1.0f, SingleStepMovementTask.adjustedStep(10.0f, 20.0f, 1.0f), DELTA);
    }

    @Test
    void movementConvergesToDestination() {
        record MovementCase(float startX, float startY, int destX, int destY) {}

        MovementCase[] cases = {
                new MovementCase(100, 100, 500, 500),   // plain diagonal
                new MovementCase(500, 500, 100, 100),   // going up-left
                new MovementCase(100, 100, 500, 100),   // horizontal (used to break: infinite step)
                new MovementCase(500, 100, 100, 100),   // horizontal, other direction
                new MovementCase(100, 100, 100, 500),   // vertical (used to break: infinite step)
                new MovementCase(100, 500, 100, 100),   // vertical, other direction
                new MovementCase(105, 105, 105, 105),   // already at destination
                new MovementCase(0, 0, 2000, 1000),     // steep diagonal
                new MovementCase(2000, 1000, 0, 0),     // same, backwards
                new MovementCase(37, 173, 2311, 997),   // random-ish, both directions mixed
        };

        for (MovementCase move : cases) {
            float x = move.startX();
            float y = move.startY();

            int lengthX = Math.round(Math.abs(x - move.destX()));
            int lengthY = Math.round(Math.abs(y - move.destY()));
            float stepX = SingleStepMovementTask.computeStepX(lengthX, lengthY);
            float stepY = SingleStepMovementTask.computeStepY(lengthX, lengthY);

            boolean converged = false;
            for (int tick = 0; tick < MAX_TICKS; tick++) {
                if (Math.round(x) == move.destX() && Math.round(y) == move.destY()) {
                    converged = true;
                    break;
                }

                stepX = SingleStepMovementTask.adjustedStep(x, move.destX(), stepX);
                stepY = SingleStepMovementTask.adjustedStep(y, move.destY(), stepY);
                x = SingleStepMovementTask.advance(x, move.destX(), stepX);
                y = SingleStepMovementTask.advance(y, move.destY(), stepY);
            }

            if (!converged) {
                fail("movement did not converge: stuck at [" + Math.round(x) + ", " + Math.round(y)
                        + "] while heading to [" + move.destX() + ", " + move.destY() + "]");
            }
        }
    }
}