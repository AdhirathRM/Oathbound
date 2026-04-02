# ⚔ Oathbound: The Ten Trials

> **2D Action-Platformer / Turn-Based RPG Hybrid** · Java · OOP  
> A party of five heroes must conquer Ten Trials to rescue their kidnapped Squire from Lord Malakor, the Vampire.

---

## 📁 Project Structure

```
Oathbound/
├── src/main/java/com/oathbound/
│   ├── Main.java                  # Entry point
│   ├── core/
│   │   ├── GameWindow.java        # JFrame wrapper
│   │   └── GamePanel.java        # 60 FPS game loop + rendering (PB-001, PB-005)
│   └── state/
│       └── GameState.java        # Enum: MENU | PLAY | BOSS (PB-005)
├── res/
│   └── levels/
│       └── level_test.txt        # Tile-map for Sprint 1 testing (PB-007)
└── README.md
```

---

## 🚀 Running the Game (VS Code)

### Prerequisites
- **Java 17+** installed ([download](https://adoptium.net/))
- **VS Code** with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) installed

### Steps
1. Open the `Oathbound/` folder in VS Code (`File → Open Folder`).
2. VS Code will auto-detect the Java sources.
3. Open `src/main/java/com/oathbound/Main.java`.
4. Click the **▶ Run** button (top-right) or press `F5`.
5. A 1280 × 720 window titled **"Oathbound: The Ten Trials"** should appear with the placeholder MENU screen.
6. Watch the terminal for the FPS counter — it should read **~60 FPS**.

### Compiling manually (terminal)
```bash
# From the Oathbound/ root
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.oathbound.Main
```

---

## 🐙 Setting Up on GitHub

### First time (create the repo)
```bash
# 1. Inside the Oathbound/ folder, initialise Git
git init

# 2. Stage everything
git add .

# 3. First commit
git commit -m "chore: initial project scaffold — game loop + GameState enum (pre PB-001)"

# 4. Create a repo on GitHub (github.com → New Repository → name it "Oathbound")
#    Then link it:
git remote add origin https://github.com/<YOUR_USERNAME>/Oathbound.git

# 5. Push
git branch -M main
git push -u origin main
```

### Ongoing workflow (per sprint)
```bash
# Start a feature
git checkout -b feature/PB-001-game-loop

# Work, then commit
git add .
git commit -m "feat(PB-001): implement 60fps fixed-timestep loop"

# Push and open a Pull Request on GitHub
git push origin feature/PB-001-game-loop
```

---

## 🗂 Product Backlog Snapshot

| ID | Sprint | Story | Priority |
|----|--------|-------|----------|
| PB-001 | 1 | 60 FPS game loop | Critical |
| PB-002 | 1 | Gravity system | Critical |
| PB-003 | 1 | Jumping | Critical |
| PB-004 | 1 | Tile-map loader (.txt/.csv) | Critical |
| PB-005 | 1 | GameState enum (MENU/PLAY/BOSS) | Critical |
| PB-006 | 1 | Collision detection (Rectangle.intersects) | Critical |
| PB-007 | 1 | Test map | High |
| … | … | … | … |

Full backlog: `Oathbound_Product_Backlog.xlsx`

---

## 🏗 Architecture (OOP)

- **`GamePanel`** → owns the loop thread, branches on `GameState`
- **`GameWindow`** → JFrame shell, calls `gamePanel.startGameLoop()`
- **`GameState`** → `MENU | PLAY | BOSS` enum (PB-005)
- **`Player` (Sprint 2)** → abstract base class for all 5 heroes (PB-008)
- **`TileMapLoader` (Sprint 1)** → parses `res/levels/*.txt` (PB-004)

---

## 📋 Definition of Done (Sprint 1)

> A block-character can navigate a basic map without falling through the floor.

- [ ] PB-001 — Loop hits 60 FPS, delta-time tracked
- [ ] PB-002 — Gravity + terminal velocity
- [ ] PB-003 — Jump with correct arc
- [ ] PB-004 — Tile-map loads from `level_test.txt`
- [ ] PB-005 — GameState enum branching ✅ *(done in this scaffold)*
- [ ] PB-006 — `Rectangle.intersects()` collision
- [ ] PB-007 — Test map passes full playthrough
