package com.example.billard.ui;

import com.example.billard.model.GameSession;
import com.example.billard.model.TipContact;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Full-area overlay with a large cue ball. Click or drag on it, or use the arrow keys, to choose where the cue
 * hits; the choice goes straight to the session. Escape, Done or a click outside the card closes it.
 */
final class SpinPicker extends StackPane {

    private static final double MIN_FACE_SIZE = 140;
    private static final double MAX_FACE_SIZE = 360;
    private static final double CARD_CHROME_HEIGHT = 280;
    private static final double KEY_STEP = 0.1;
    private static final double SPIN_SHOWN_FROM = 0.05;

    private final GameSession session;
    private final Runnable onClosed;
    private final CueBallFace face = new CueBallFace(MAX_FACE_SIZE, true);
    private final Label readout = new Label();
    private final Button doneButton = new Button("Done");

    SpinPicker(GameSession session, Runnable onClosed) {
        this.session = session;
        this.onClosed = onClosed;
        getStyleClass().add("overlay");
        getChildren().add(buildCard());
        setVisible(false);

        heightProperty().addListener((observable, before, height) -> fitFaceTo(height.doubleValue()));
        face.setOnMousePressed(this::choose);
        face.setOnMouseDragged(this::choose);
        setOnMouseClicked(event -> closeWhenBackdropClicked(event));
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
    }

    void open() {
        refresh();
        setVisible(true);
        doneButton.requestFocus();
    }

    void close() {
        setVisible(false);
        onClosed.run();
    }

    private VBox buildCard() {
        Label title = new Label("Where should the cue hit the ball?");
        title.getStyleClass().add("lead");
        readout.getStyleClass().add("status");

        Button reset = new Button("Centre");
        reset.setOnAction(event -> choose(TipContact.CENTER));
        doneButton.getStyleClass().add("primary");
        doneButton.setOnAction(event -> close());

        HBox faceRow = new HBox(18, edgeLabel("Left"), face, edgeLabel("Right"));
        faceRow.setAlignment(Pos.CENTER);
        HBox buttons = new HBox(14, reset, doneButton);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(12, title, edgeLabel("Top: follow"), faceRow, edgeLabel("Bottom: draw"), readout, buttons);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        card.setPadding(new Insets(24, 40, 24, 40));
        card.getStyleClass().add("overlay-card");
        return card;
    }

    private static Label edgeLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private void fitFaceTo(double availableHeight) {
        double size = Math.max(MIN_FACE_SIZE, Math.min(MAX_FACE_SIZE, availableHeight - CARD_CHROME_HEIGHT));
        face.resizeTo(size);
    }

    private void choose(MouseEvent event) {
        choose(face.contactAt(event.getX(), event.getY()));
    }

    private void choose(TipContact contact) {
        session.setTipContact(contact);
        refresh();
    }

    private void refresh() {
        TipContact contact = session.tipContact();
        face.show(contact);
        readout.setText(describe(contact));
        face.setAccessibleText("Cue ball. " + readout.getText());
    }

    private void handleKey(KeyEvent event) {
        TipContact now = session.tipContact();
        switch (event.getCode()) {
            case ESCAPE -> close();
            case LEFT -> choose(new TipContact(now.right() - KEY_STEP, now.up()));
            case RIGHT -> choose(new TipContact(now.right() + KEY_STEP, now.up()));
            case UP -> choose(new TipContact(now.right(), now.up() + KEY_STEP));
            case DOWN -> choose(new TipContact(now.right(), now.up() - KEY_STEP));
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void closeWhenBackdropClicked(MouseEvent event) {
        if (event.getTarget() == this) {
            close();
        }
    }

    static String describe(TipContact contact) {
        if (Math.hypot(contact.right(), contact.up()) < SPIN_SHOWN_FROM) {
            return "Centre hit: no spin";
        }
        StringBuilder text = new StringBuilder();
        if (Math.abs(contact.up()) >= SPIN_SHOWN_FROM) {
            text.append(contact.up() > 0 ? "Follow " : "Draw ").append(percent(contact.up()));
        }
        if (Math.abs(contact.right()) >= SPIN_SHOWN_FROM) {
            text.append(text.isEmpty() ? "" : "   ")
                    .append(contact.right() > 0 ? "Right english " : "Left english ")
                    .append(percent(contact.right()));
        }
        return text.toString();
    }

    private static String percent(double amount) {
        return Math.round(Math.abs(amount) * 100) + "%";
    }
}
