<h1 align="center">🎲 Offline Board Game Hub</h1>

<p align="center">
  A premium, interactive, and beautifully designed collection of classic board games built for Android. <br>
  Play anywhere, anytime — no internet required!
</p>

## 🎮 Included Games
Experience 7 classic games, all carefully crafted with dynamic animations, modern glassmorphism UI, and smart AI opponents.

1. **Ludo**: Roll the glowing dice, capture opponents, and race your pieces home. Play with friends or challenge the smart CPU.
2. **Chess**: A powerful chess engine with deep analysis, move validation, and a beautiful UI. Features puzzle mode and match history.
3. **Air Hockey**: High-octane arcade action with dynamic physics, fluid puck movement, and responsive paddles.
4. **Tic-Tac-Toe**: The classic game reimagined with an unbeatable CPU logic and neon aesthetics.
5. **Connect 4**: Strategic disc-dropping gameplay with smooth animations and gravity physics.
6. **Minesweeper**: Flag mines and reveal tiles with a modern twist, including chord logic and flood-fill mechanics.
7. **Dots & Boxes**: Tactical line-drawing fun! Box out your opponents with clever play.

## ✨ Key Features
- **Offline First**: No Wi-Fi? No problem. All core game functionality operates 100% offline.
- **Smart Bot Opponents**: Every multiplayer game includes robust "vs AI" modes utilizing advanced algorithms (Minimax, heuristics) so you always have a challenger.
- **Multiplayer "Pass & Play"**: Gather your friends and family around a single device for real-time local multiplayer action.
- **Save & Resume automatically**: The engine automatically saves your game state so you can pick up exactly where you left off.
- **Modern Architecture**: Built entirely in Kotlin using **Jetpack Compose** with a custom 2D rendering engine (`SurfaceView`) handling responsive physics and 60fps animations.

## 🛠 Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material3, dynamic theming, custom navigation)
- **Architecture**: MVI (Model-View-Intent) pattern with robust ViewModels and Reducers separating state from rendering.
- **Engine**: Custom `FrameUpdateAction` and `PhysicsWorld` for handling 2D collisions and game loops on a background `GameThread`.
- **Persistence**: Encrypted SharedPreferences via a generic `SaveManager` serialization logic.

## 🚀 How to Build & Run
1. Clone the repository to your local machine.
2. Open the project in **Android Studio** (Koala or later recommended).
3. Connect your Android device or start an emulator.
4. Run the `app` module (`Shift + F10`).

> Note: To test the AdMob integration in release builds, add a `local.properties` file with your actual ad unit credentials. Debug builds use Google test ad IDs by default.

## 📄 Privacy Policy
The game is completely offline but uses Google AdMob (in release builds) to support development. Please view our [Privacy Policy](privacy_policy.html).
