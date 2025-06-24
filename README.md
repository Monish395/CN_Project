🎮 **2-Player Hangman Game (Java + LibGDX)**

A classic 2-player Hangman game with a graphical UI built using **LibGDX** and a **Java Socket-based server**.  
One player sets the secret word and clue (Chooser), while the other guesses letters (Guesser). The game also supports real-time chat and animated visuals.

---

🚀 Features

- 🎭 **Chooser & Guesser roles** with role-specific interactions
- 💬 **In-game chat** between players
- 💀 **Animated Hangman drawing**
  - Head fade-in
  - Shake effect on wrong guesses
  - Red "X" eyes on losing
- 🧠 Intelligent UI updates based on turn & game stage
- 📡 Java Socket-based server handling multiple players

---

🛠 Technologies Used

- **Java**
- **LibGDX** (for rendering and UI)
- **Java Sockets** (for client-server communication)

---

🖥️ How to Run

1. Start the Server
    - Run the Server.java file

2. Run the Client (LibGDX UI)
    - Make sure you have your LibGDX environment set up.
    - cd client
    - Compile and run using your IDE or Gradle
    - Both clients should connect to the same server (same Wi=Fi). Use two windows or machines(connected to same Wi-Fi).

---

🔄 Gameplay Flow
- Player 1 and Player 2 connect to the server.
- One becomes the Chooser, enters a word and clue.
- The other becomes the Guesser, guesses letters.
- Wrong guesses animate the hangman.
- Game ends with a win/lose message and visual effects.
- Players can choose to play again.

---

📌 Known Issues
- Server currently handles exactly two players.
- No support for mid-game reconnects.

---

📧 Contact

Built by [Monish](https://github.com/Monish395)

Feel free to raise issues or contribute!

---

✅ Future Improvements
- Add AI mode (play vs computer)
- Score tracking
