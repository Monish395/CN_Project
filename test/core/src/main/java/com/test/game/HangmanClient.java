package com.test.game;

// LibGDX core class for managing multiple screens
import com.badlogic.gdx.Game;

/**
 * HangmanClient is the main entry point of the game client.
 * It extends LibGDX's Game class, which manages the game lifecycle and screen transitions.
 * Extends Game, which gives you screen management via setScreen(...).
 * Starts with the FirstScreen, which leads to GameScreen after clicking “Start Game.”
 */
public class HangmanClient extends Game {

    /**
     * Called when the application is launched.
     * Initializes and sets the first screen shown to the user.
     */
    @Override
    public void create() {
        // Set the initial screen to the main menu (FirstScreen)
        setScreen(new FirstScreen(this));
    }
}
