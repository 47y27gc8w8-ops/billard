package com.example.billard.model;

/**
 * Where the cue tip meets the cue ball, as seen from behind the cue: {@code right} runs from -1 (left edge) to
 * 1 (right edge), {@code up} from -1 (bottom) to 1 (top). The point always lies within the ball's face.
 *
 * <p>Hitting above centre gives follow (the ball pushes on after a collision), below gives draw (it comes back),
 * and either side gives english (the ball kicks sideways off cushions).
 */
public record TipContact(double right, double up) {

    public static final TipContact CENTER = new TipContact(0, 0);

    private static final double ROLL_SPIN_RATIO = 0.6;
    private static final double SIDE_SPIN_RATIO = 0.5;

    /** Pulls a point outside the ball's face back onto its rim. */
    public TipContact {
        double distance = Math.hypot(right, up);
        if (distance > 1) {
            right /= distance;
            up /= distance;
        }
    }

    public boolean isCentered() {
        return right == 0 && up == 0;
    }

    /** Follow (along {@code direction}) or draw (against it), as the extra speed the ball takes on at its next hit. */
    public Vec2 rollSpin(Vec2 direction, double shotSpeed) {
        return direction.times(up * shotSpeed * ROLL_SPIN_RATIO);
    }

    /**
     * Sideways spin, positive when the ball turns clockwise on screen. Striking the right side makes it turn
     * counter-clockwise, hence the sign.
     */
    public double sideSpin(double shotSpeed) {
        return -right * shotSpeed * SIDE_SPIN_RATIO;
    }
}
