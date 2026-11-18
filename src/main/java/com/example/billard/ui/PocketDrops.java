package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.PhysicsListener;
import com.example.billard.model.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;

/**
 * Balls on their way down into pockets. The rules take a ball off the table the moment a pocket catches it; this
 * keeps a copy of it on screen for the fraction of a second it takes to fall in.
 */
final class PocketDrops implements PhysicsListener {

    private record Falling(PocketDrop drop, double elapsed) {
    }

    private final Table table;
    private final RollingBallPainter painter = new RollingBallPainter();
    private final List<Falling> falling = new ArrayList<>();

    PocketDrops(Table table) {
        this.table = table;
    }

    @Override
    public void onPocketed(Ball ball, double speed) {
        Table.Pocket pocket = table.pockets().stream()
                .min(Comparator.comparingDouble(p -> p.center().distanceTo(ball.position())))
                .orElseThrow();
        falling.add(new Falling(new PocketDrop(ball.number(), ball.position(), pocket.center(),
                pocket.captureRadius(), table.ballRadius(), speed, ball.orientation()), 0));
    }

    void advance(double seconds) {
        falling.replaceAll(ball -> new Falling(ball.drop(), ball.elapsed() + seconds));
        falling.removeIf(ball -> ball.drop().isFinished(ball.elapsed()));
    }

    void draw(GraphicsContext gc, double pixelsPerUnit) {
        for (Falling ball : falling) {
            PocketDrop drop = ball.drop();
            PocketDrop.Frame frame = drop.frameAt(ball.elapsed());
            double cx = frame.centre().x();
            double cy = frame.centre().y();
            double r = frame.radius();
            gc.save();
            BallPainter.clipToCircle(gc, drop.pocket().x(), drop.pocket().y(), frame.clipRadius());
            painter.paint(gc, drop.number(), frame.orientation(), cx, cy, r, pixelsPerUnit);
            // Down in the hole the lamp no longer reaches it: it fades into the pocket's own darkness.
            gc.setGlobalAlpha(frame.darkness());
            gc.setFill(Palette.POCKET);
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            gc.restore();
        }
    }
}
