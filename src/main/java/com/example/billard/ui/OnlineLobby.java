package com.example.billard.ui;

import com.example.billard.net.Connector;
import com.example.billard.net.DirectHost;
import com.example.billard.net.DirectJoin;
import com.example.billard.net.OnlineLink;
import com.example.billard.net.PlayerName;
import com.example.billard.net.RelayConnector;
import com.example.billard.net.RelayProtocol;
import com.example.billard.net.ServerAddress;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The menu page for online play. The player picks a name and how to meet, directly by address or through a relay
 * server by room code, then hosts or joins. A waiting page follows until the other player is there, showing what
 * to pass on to them; once both are connected the link is handed over and the match starts.
 */
final class OnlineLobby extends StackPane {

    private final OnlinePreferences preferences;
    private final Consumer<OnlineLink> onConnected;
    private final TextField nameField = new TextField();
    private final RadioButton directChoice = new RadioButton("Directly, by address");
    private final RadioButton relayChoice = new RadioButton("Through a relay server, by room code");
    private final TextField relayField = new TextField();
    private final TextField joinField = new TextField();
    private final Label problemLabel = new Label();
    private final Label waitingHeading = new Label();
    private final Label waitingText = new Label();
    private final Label roomCodeLabel = new Label();
    private final Button cancelButton = new Button("Cancel");
    private final VBox setupPage;
    private final VBox waitingPage;
    private OnlineLink pending;

    OnlineLobby(OnlinePreferences preferences, Consumer<OnlineLink> onConnected, Runnable onBack) {
        this.preferences = preferences;
        this.onConnected = onConnected;
        this.setupPage = buildSetupPage(onBack);
        this.waitingPage = buildWaitingPage();
        getChildren().addAll(setupPage, waitingPage);
        showSetup("");
    }

    void focusFirstControl() {
        nameField.requestFocus();
    }

    /** Gives up any hosting or joining still in progress. */
    void cancel() {
        OnlineLink abandoned = pending;
        pending = null;
        if (abandoned != null) {
            abandoned.close();
        }
        showSetup("");
    }

    private VBox buildSetupPage(Runnable onBack) {
        Label heading = new Label("Play online");
        heading.getStyleClass().add("display-medium");

        nameField.setText(preferences.name());
        nameField.setPromptText("Your name");
        nameField.setAccessibleText("Your name, shown to the other player");

        ToggleGroup connection = new ToggleGroup();
        directChoice.setToggleGroup(connection);
        relayChoice.setToggleGroup(connection);
        (preferences.usesRelay() ? relayChoice : directChoice).setSelected(true);
        relayField.setText(preferences.relayAddress());
        relayField.setPromptText("Relay server, e.g. relay.example.org");
        relayField.setAccessibleText("Relay server address");
        relayField.visibleProperty().bind(relayChoice.selectedProperty());
        relayField.managedProperty().bind(relayChoice.selectedProperty());
        connection.selectedToggleProperty().addListener((observable, before, now) -> updateJoinPrompt());
        updateJoinPrompt();

        Button hostButton = new Button("Host a match");
        hostButton.getStyleClass().add("primary");
        hostButton.setOnAction(event -> host());
        Button joinButton = new Button("Join");
        joinButton.setOnAction(event -> join());
        joinField.setOnAction(event -> join());
        HBox.setHgrow(joinField, Priority.ALWAYS);
        HBox joinRow = new HBox(12, joinField, joinButton);
        joinRow.setAlignment(Pos.CENTER_LEFT);

        problemLabel.getStyleClass().add("muted");
        problemLabel.setWrapText(true);
        problemLabel.managedProperty().bind(problemLabel.visibleProperty());

        Button back = new Button("Back");
        back.setOnAction(event -> onBack.run());

        VBox page = new VBox(18, heading, labelled("Your name", nameField),
                labelled("Connect", new VBox(8, directChoice, relayChoice, relayField)),
                hostButton, labelled("Or join a match", joinRow), problemLabel, back);
        page.setAlignment(Pos.CENTER_LEFT);
        return page;
    }

    private VBox buildWaitingPage() {
        waitingHeading.getStyleClass().add("display-medium");
        waitingHeading.setWrapText(true);
        waitingText.setWrapText(true);
        roomCodeLabel.getStyleClass().add("room-code");
        roomCodeLabel.managedProperty().bind(roomCodeLabel.visibleProperty());
        cancelButton.setOnAction(event -> cancel());
        VBox page = new VBox(22, waitingHeading, roomCodeLabel, waitingText, cancelButton);
        page.setAlignment(Pos.CENTER_LEFT);
        return page;
    }

    private static VBox labelled(String name, Node control) {
        Label label = new Label(name);
        label.getStyleClass().add("rule-name");
        label.setLabelFor(control);
        return new VBox(6, label, control);
    }

    private void updateJoinPrompt() {
        boolean relay = relayChoice.isSelected();
        joinField.setText(relay ? "" : preferences.joinAddress());
        joinField.setPromptText(relay ? "Room code" : "Host address, e.g. 192.168.1.20");
        joinField.setAccessibleText(relay ? "Room code to join" : "Address of the computer hosting the match");
    }

    private void host() {
        Optional<String> name = checkedName();
        if (name.isEmpty()) {
            return;
        }
        if (relayChoice.isSelected()) {
            checkedRelay().ifPresent(relay -> {
                OnlineLink link = OnlineLink.host(RelayConnector.hosting(relay, this::roomOpened), name.get(),
                        Platform::runLater);
                waitFor(link, "Opening a room\u2026", "Asking the relay server at " + relay + " for a room code.");
            });
        } else {
            hostDirectly(name.get());
        }
    }

    private void hostDirectly(String name) {
        DirectHost host;
        try {
            host = new DirectHost(DirectHost.DEFAULT_PORT);
        } catch (IOException e) {
            showSetup("Could not open port " + DirectHost.DEFAULT_PORT + " for hosting. Is another match already "
                    + "hosted on this computer?");
            return;
        }
        OnlineLink link = OnlineLink.host(host, name, Platform::runLater);
        waitFor(link, "Waiting for an opponent", directHostHelp(host.port()));
    }

    private static String directHostHelp(int port) {
        List<String> addresses = DirectHost.localAddresses().stream().map(address -> address + ":" + port).toList();
        String where = addresses.isEmpty()
                ? "this computer's address and port " + port
                : String.join("  or  ", addresses);
        return "Ask your opponent to join " + where + ". That works on the same network; over the internet, "
                + "forward port " + port + " on your router or share a VPN, or use a relay server instead.";
    }

    /** Called from the connecting thread once the relay has opened the room. */
    private void roomOpened(String code) {
        Platform.runLater(() -> {
            if (pending != null) {
                waitingHeading.setText("Waiting for an opponent");
                roomCodeLabel.setText(code);
                roomCodeLabel.setVisible(true);
                waitingText.setText("Tell your opponent this room code. They choose the relay server too and "
                        + "join with it.");
            }
        });
    }

    private void join() {
        Optional<String> name = checkedName();
        if (name.isEmpty()) {
            return;
        }
        String target = joinField.getText();
        if (relayChoice.isSelected()) {
            Optional<String> code = RelayProtocol.roomCode(target);
            if (code.isEmpty()) {
                showSetup("A room code is " + RelayProtocol.CODE_LENGTH + " letters and digits, like K7QM.");
                return;
            }
            checkedRelay().ifPresent(relay -> startJoining(RelayConnector.joining(relay, code.get()), name.get(),
                    "room " + code.get()));
            return;
        }
        Optional<ServerAddress> address = ServerAddress.parse(target, DirectHost.DEFAULT_PORT);
        if (address.isEmpty()) {
            showSetup("That is not an address. Type it like 192.168.1.20 or 192.168.1.20:47474.");
            return;
        }
        preferences.setJoinAddress(target.strip());
        startJoining(new DirectJoin(address.get()), name.get(), address.get().toString());
    }

    private void startJoining(Connector connector, String name, String target) {
        waitFor(OnlineLink.join(connector, name, Platform::runLater), "Joining\u2026", "Connecting to " + target + ".");
    }

    private Optional<String> checkedName() {
        Optional<String> name = PlayerName.clean(nameField.getText());
        if (name.isEmpty()) {
            showSetup("Choose a name of up to " + PlayerName.MAX_LENGTH + " letters, digits and spaces.");
            nameField.requestFocus();
            return name;
        }
        nameField.setText(name.get());
        preferences.setName(name.get());
        preferences.setUsesRelay(relayChoice.isSelected());
        return name;
    }

    private Optional<ServerAddress> checkedRelay() {
        Optional<ServerAddress> relay = ServerAddress.parse(relayField.getText(), RelayProtocol.DEFAULT_PORT);
        if (relay.isEmpty()) {
            showSetup("Type the relay server's address, like relay.example.org or 203.0.113.5:"
                    + RelayProtocol.DEFAULT_PORT + ".");
            relayField.requestFocus();
        } else {
            preferences.setRelayAddress(relayField.getText().strip());
        }
        return relay;
    }

    /** Shows the waiting page until {@code link} connects, fails or is cancelled. */
    private void waitFor(OnlineLink link, String heading, String text) {
        pending = link;
        waitingHeading.setText(heading);
        waitingText.setText(text);
        roomCodeLabel.setVisible(false);
        setupPage.setVisible(false);
        waitingPage.setVisible(true);
        cancelButton.requestFocus();
        link.setStatusListener((state, reason) -> {
            if (link != pending) {
                return;
            }
            switch (state) {
                case CONNECTED -> {
                    pending = null;
                    showSetup("");
                    onConnected.accept(link);
                }
                case CLOSED -> {
                    pending = null;
                    showSetup(reason);
                }
                case CONNECTING, RECONNECTING -> { }
            }
        });
    }

    private void showSetup(String problem) {
        problemLabel.setText(problem);
        problemLabel.setVisible(!problem.isEmpty());
        waitingPage.setVisible(false);
        setupPage.setVisible(true);
    }
}
