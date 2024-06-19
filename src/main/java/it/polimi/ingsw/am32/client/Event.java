package it.polimi.ingsw.am32.client;

/**
 * Enumeration of the possible events that can occur during the game.
 */
public enum Event {
    LOBBY(0),
    PREPARATION(1),
    PLAYING(2),
    TERMINATING(3),
    LAST_TURN(4),
    TERMINATED(5),
    CHOOSE_CONNECTION (6),
    SELECT_GAME_MODE(7),
    CREATE_GAME(8),
    JOIN_GAME(9),
    RECONNECT_GAME(10),
    GAME_CREATED (11),
    GAME_JOINED (12),
    GAME_RECONNECTED (13),
    NEW_PLAYER_JOIN (14),
    PLAYER_DISCONNECTED (15),
    WAITING_FOR_START (16),
    GAME_START (17),
    SELECT_STARTER_CARD_SIDE(18),
    SELECT_STARTER_CARD_SIDE_FAILURE (19),
    SELECT_SECRET_OBJ_CARD (20),
    SELECT_SECRET_OBJ_CARD_FAILURE (21),
    PLACE_CARD(22),
    PLACE_CARD_FAILURE (23),
    DRAW_CARD(24),
    PLAYER_RECONNECTED(25),
    DRAW_CARD_FAILURE(26),
    WELCOME(27),
    SELECTED_STARTER_CARD_SIDE(28),
    SELECTED_SECRET_OBJ_CARD(29),
    CARD_PLACED(30),
    CARD_DRAWN(31),
    WAITING_FOR_TURN(32),
    CHAT_ERROR(33),
    CREATE_GAME_FAILURE(34),
    JOIN_GAME_FAILURE(35),
    RECONNECT_GAME_FAILURE(36);

    private final int value;

    /**
     * Constructor for the Event enum.
     * @param value The integer value associated with the event.
     */
    Event(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value associated with the event.
     * @return The integer value of the event.
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets the event associated with the given integer value.
     * @param value The integer value of the event.
     * @return The event associated with the given integer value.
     */
    public static Event getEvent(int value) {
        for (Event event : Event.values()) {
            if (event.getValue() == value) {
                return event;
            }
        }
        return null;
    }
}
