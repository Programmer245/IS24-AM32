package it.polimi.ingsw.am32.client.view.gui;

import it.polimi.ingsw.am32.message.ClientToServer.InboundChatMessage;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;

/**
 * This class represents the chat area in the GUI.
 * It contains the message display area, the message input area, and the submit button.
 * It also contains a combo box that allows the user to select a recipient for the message.
 */
public class ChatArea {
    /**
     * An object representing the chat area in the GUI
     */
    private final VBox chatArea;
    /**
     * An object representing the area where messages are displayed.
     * Contained inside the scroll pane
     */
    private final VBox messageDisplayArea;
    /**
     * An object representing the scroll pane that contains the message display area
     */
    private final ScrollPane messageScrollPane;
    /**
     * An object representing the area where the user can input messages.
     * Contains the text field and the submit button
     */
    private final HBox submissionArea;
    /**
     * An object representing the text field where the user can input messages
     */
    private final TextField inputMessageField;
    /**
     * An object representing the button that submits the message
     */
    private final Button submitButton;
    /**
     * A combo box that allows the user to select a recipient for the message
     */
    private final ComboBox<String> playerList;
    /**
     * A reference to the gui; needed to forward messages to the server
     */
    private final GraphicalUI gui;

    /**
     * Constructor for the ChatArea class
     * @param gui A reference to the GUI
     * @param X X coordinate of the chat area
     * @param Y Y coordinate of the chat area
     * @param width Width of the chat area
     * @param height Height of the chat area
     * @param players List of players in the game; used for the combo box
     */
    public ChatArea(int X, int Y, int width, int height, ArrayList<String> players, GraphicalUI gui) {
        // Initialize empty components

        this.chatArea = new VBox();

        this.messageDisplayArea = new VBox();
        this.messageScrollPane = new ScrollPane(messageDisplayArea);

        this.submissionArea = new HBox();
        this.inputMessageField = new TextField();
        this.submitButton = new Button("Send");
        this.playerList = new ComboBox<>();

        this.gui = gui;

        // Configure components and arrange them

        initializeChatArea(X, Y, width, height, players);
    }

    /**
     * Initializes the chat area with the given dimensions.
     * Generates all components, and configures them.
     *
     * @param X X coordinate of the chat area
     * @param Y Y coordinate of the chat area
     * @param width Width of the chat area
     * @param height Height of the chat area
     */
    private void initializeChatArea(int X, int Y, int width, int height, ArrayList<String> players) {
        // Set the effective size of the chat area
        messageScrollPane.setMinSize(width+80, height);
        messageScrollPane.setMaxSize(width+80, height);

        // Enable only vertical scrolling
        messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Add options to scroll pane for recipient selection in chat
        for (String player : players) {
            playerList.getItems().add(player);
        }
        playerList.getItems().add("All");
        playerList.setValue("All"); // Set default option in scroll pane

        playerList.setStyle("-fx-background-color: #E6DEB3;-fx-text-fill: #3A2111;"+
                "-fx-font-size: 15px;-fx-font-family: 'JejuHallasan';-fx-border-color: #3A2111; -fx-border-width: 1px; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px;");

        // Set style of the chat area
        messageScrollPane.setStyle("-fx-background-color: #E6DEB3;-fx-border-color: #3A2111; -fx-border-width: 1px; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px;");

        submitButton.setStyle("-fx-text-fill: #3A2111;-fx-alignment: center;" +
                "-fx-font-size: 15px;-fx-font-family: 'JejuHallasan'");
        playerList.setMaxSize(80, 30);

        inputMessageField.setPromptText("Type your message......");
        inputMessageField.setStyle("-fx-background-color: #E6DEB359;-fx-text-fill: #3A2111;"+
                "-fx-font-size: 15px;-fx-font-family: 'JejuHallasan';-fx-border-color: #3A2111; -fx-border-width: 1px; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px;");

        inputMessageField.setPrefSize(250, 30);
        chatArea.setPrefSize(width+100, height + inputMessageField.getHeight());

        // Link the button to a handler method
        submitButton.setOnAction(e -> submitChatMessage());
        submitButton.setMaxSize(100, 30);

        inputMessageField.setOnMouseClicked(e -> messageScrollPane.setVisible(true));
        messageScrollPane.setOnMouseClicked(e -> messageScrollPane.setVisible(false));

        // Generate VBox container
        submissionArea.getChildren().addAll(playerList,inputMessageField, submitButton); // Add the text field and submit button to the chat input area
        chatArea.getChildren().addAll(messageScrollPane,submissionArea); // Add the scroll pane and chat input area to the chat area
        // Set the position of the chat area
        chatArea.setTranslateX(X);
        chatArea.setTranslateY(Y);
    }

    /**
     * Adds an incoming message to the chat area send from another player.
     * The message is appended to the end of the message display area.
     * Called by outside classes to add messages to the chat area.
     * @param message The message to be added.
     * @param senderNickname The nickname of the player who sent the message.
     */
    public void addIncomingMessageToChat(String message, String senderNickname) {
        String sender = senderNickname.equals(gui.getThisPlayerNickname())?"Yourself": senderNickname;
        Label newMessage = new Label("(" + sender + ") " + message);
        newMessage.setStyle("-fx-text-fill: #3CA99F;-fx-alignment: center;" +
                "-fx-font-size: 15px;-fx-font-family: 'JejuHallasan';");
        messageDisplayArea.getChildren().add(newMessage);
    }

    /**
     * Submits a message to the chat area, and send it to the appropriate player.
     * The message is taken from the input field, and added to the message display area.
     * Called when the user clicks the submit button.
     */
    private void submitChatMessage() {

        if (inputMessageField.getText().isEmpty()) return; // Do not send empty messages (or messages with only whitespace characters)
        String recipient = playerList.getValue().equals(gui.getThisPlayerNickname())?"Yourself": playerList.getValue();
        // Add message to chat area
        Label newMessage = new Label("> You to "+ recipient+": "+ inputMessageField.getText());
        newMessage.setStyle("-fx-text-fill: #3A2111;-fx-alignment: center;" +
                "-fx-font-size: 15px;-fx-font-family: 'JejuHallasan';");
        messageDisplayArea.getChildren().add(newMessage);

        // Send the message to the server
        gui.notifyAskListener(new InboundChatMessage(gui.getThisPlayerNickname(), playerList.getValue(), playerList.getValue().equals("All"), inputMessageField.getText()));

        inputMessageField.clear(); // Clear the input field after the message is sent
    }

    /**
     * Sets the chat area to active or inactive.
     * @param active True if the chat area should be active, false otherwise
     */
    public void setActive(boolean active) {
        chatArea.setDisable(!active);
    }

    /**
     * Returns the chat area
     *
     * @return The chat area
     */
    public VBox getChatArea() {
        return chatArea;
    }
}
