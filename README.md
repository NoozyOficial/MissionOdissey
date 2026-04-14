<div align="center">

<img src="https://i.imgur.com/cREQVx4.png" alt="Mission Odyssey Logo" width="480"/>

**A space exploration mod for Minecraft 1.21.1 (NeoForge)**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=for-the-badge&logo=minecraft)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.226-orange?style=for-the-badge)](https://neoforged.net)
[![Version](https://img.shields.io/badge/Version-0.1-blue?style=for-the-badge)](https://github.com/NoozyOficial/MissionOdissey)
[![License](https://img.shields.io/badge/License-MIT-purple?style=for-the-badge)](LICENSE)

*Leave the Overworld behind. The cosmos awaits.*

</div>

---

## 🚀 Overview

**Mission Odyssey** is a Minecraft mod that transforms your game into an interplanetary adventure. Pilot your own spaceship, travel through a procedurally rendered space dimension, and land on alien worlds — each with their own unique atmosphere, gravity, terrain, and visual effects.

Built for **NeoForge 1.21.1**, Mission Odyssey leverages **GeckoLib** for animated 3D models and the **Veil** rendering library for stunning, custom atmospheric shaders.

---

## 🌌 Features

### 🛸 Spaceship
- Fully controllable spaceship entity with custom 3D GeckoLib model
- Dedicated keybindings for flight controls
- Warp effect with cinematic screen distortion when entering/exiting dimensions

### 🌍 Dimensions

| Dimension | Description |
|-----------|-------------|
| 🌑 **Space** | A vast, star-filled void with a custom skybox. Home of the solar system. |
| 🔴 **Mars** | A red desert world with exclusive blocks, lower gravity, and a dusty orange atmosphere. |
| 🌎 **Earth** | A colossal procedurally rendered Earth planet visible from space, with a realistic blue atmospheric glow and morphing cloud shaders. |

### 🪐 Planetary Rendering
- **Saturn** — Rendered with physically accurate rings, a custom ring shadow shader, and atmospheric glow
- **Mars** — Reddish-orange atmospheric scattering with dynamic lighting
- **Earth** — Procedural cloud layer with a pixelated, morphing GLSL shader and blue atmospheric limb glow
- **Space Skybox** — A fully custom starfield skybox renderer

### 🧱 New Blocks

| Block | Description |
|-------|-------------|
| `mars_stone` | Sturdy reddish stone native to Mars. Requires the correct tool. |
| `mars_sand` | Loose Martian sand. Falls like regular sand, with an orange hue. |

### ⚙️ Technical Highlights
- Custom **GLSL shaders** via the [Veil](https://github.com/FoundationGames/Veil) rendering pipeline
- Animated 3D entities with [GeckoLib](https://github.com/bernie-g/geckolib)
- Server-side teleportation triggered by altitude and planetary proximity
- Custom gravity attribute applied per dimension
- Mixin-based client/server event hooks

---

## 🛠️ Installation

### Requirements

- **Minecraft** `1.21.1`
- **NeoForge** `21.1.226` or later
- **GeckoLib** `4.8.4`
- **Veil** `3.4.0`

### Steps

1. Download and install **NeoForge** for Minecraft 1.21.1 from [neoforged.net](https://neoforged.net).
2. Download the required dependencies: **GeckoLib** and **Veil**.
3. Place all `.jar` files into your `.minecraft/mods` folder.
4. Launch Minecraft and enjoy your odyssey!

---

## 🧩 Dependencies

| Mod | Version | Link |
|-----|---------|------|
| NeoForge | `21.1.226` | [neoforged.net](https://neoforged.net) |
| GeckoLib | `4.8.4` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/geckolib) / [Modrinth](https://modrinth.com/mod/geckolib) |
| Veil | `3.4.0` | [GitHub](https://github.com/FoundationGames/Veil) |

---

## 🏗️ Building from Source

> Requires **Java 21** and **Git**.

```bash
git clone https://github.com/NoozyOficial/MissionOdissey.git
cd MissionOdissey
./gradlew build
```

The compiled `.jar` will be in `build/libs/`.

To run in a dev environment:

```bash
./gradlew runClient
```

---

## 📁 Project Structure

```
src/main/java/com/noozy/missionodyssey/
├── MissionOdyssey.java          # Mod entry point
├── client/
│   ├── planet/                  # Planet & atmosphere renderers (Earth, Mars, Saturn)
│   ├── entity/                  # Entity renderers (Spaceship)
│   ├── dimension/               # Dimension-specific client logic
│   ├── WarpEffectHandler.java   # Warp screen effect
│   └── SpaceshipInputHandler.java
├── entity/
│   └── SpaceshipEntity.java     # Spaceship entity logic
├── registry/
│   ├── ModBlocks.java           # Block registrations
│   ├── ModItems.java            # Item registrations
│   ├── ModEntities.java         # Entity type registrations
│   └── ModDimensions.java       # Dimension registrations
├── server/                      # Server-side teleportation & dimension logic
├── network/                     # Packets & networking
└── mixin/                       # Mixin patches
```

---

## 📜 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---

## 👤 Author

Made with ❤️ by **[NoozyOficial](https://github.com/NoozyOficial)**

> *"One small step for a mod, one giant leap for your Minecraft world."*

