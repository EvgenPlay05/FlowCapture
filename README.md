# FlowCapture 🎥✨

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Design-Material%20You%20%2F%20M3-7C4DFF?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)

**FlowCapture** is a high-performance, battery-efficient screen recording utility designed for modern Android devices. Leveraging the beauty of the **Geometric Balance Material 3 design system**, it presents an elegant single-surface, tabbed layout that brings simplicity and professional features to screen recording and game capturing.

---

## 🎨 Visual Identity & Styling: Geometric Balance

FlowCapture uses a **custom Geometric Balance visual palette**:
- **Palette Elements**: Dominated by deep violet and primary indigo accents (`#6750A4`), offset by lovely primary lavenders (`#EADDFF`) and high-contrast glowing indicators.
- **Spacious Design**: Balanced layouts with generous negative space, rounded profiles (up to 28dp), neat borders, and comfortable 16dp outer paddings.
- **Atmospheric Cards**: Interactive sections are compartmentalized within beautifully defined cards with custom outline weights, avoiding generic UI noise.

---

## 🚀 Key Features

### 🖥️ High-Fidelity Applet Capturing
- Capture full phone screens or active games directly with built-in high-performance pipelines.
- Supports smooth, lag-free internal frames with optimized RAM footprint (standby uses `< 20 MB`).

### ⚙️ Interactive Recording Profiles
- Toggle pre-defined encoding presets or define your own coordinates:
  - **Gaming Profile**: `1080p @ 60 fps`
  - **Lecture Profile**: `720p @ 30 fps`
  - **Low Storage**: `720p @ 24 fps`
  - **High Quality**: `1440p @ 60 fps`

### 🎙️ Advanced Audio Selection
- Record **System Audio**, external **Microphone** input, **Combined** streams, or silent video streams (`None`) inside a modern responsive Quick Settings matrix grid.

### 🫧 Control Bubbles & Drawing Instruments
- Active **Overlay Bubble** stays visible on top of other applications during recording.
- Supports **Drawing Pen Overlay tools** to highlight or mark key spots in real-time.
- Customize overlay dimensions, opacity scales, and optionally exclude the bubble itself from final videos during system projection!

### 🎞️ Visual Clips Gallery
- High-fidelity collection showing your **Saved Captures** paired with automatic metadata chips (Clips duration, storage file size, exact resolution).
- Tap-to-play support instantly routing system intent players to show recorded games.
- **Demo Mode**: Includes a "Load Test Clips" option to populate your gallery list with mock samples instantly during setup or testing.

---

## 🛠️ Architecture & Tech Stack

This application is built using modern Android development best-practices:
* **UI Framework**: Modern declarative [Jetpack Compose](https://developer.android.com/jetpack/compose).
* **Architecture Pattern**: MVVM (Model-View-ViewModel) with structured unidirectional state flows.
* **State Management**: Kotlin `StateFlow` and `collectAsStateWithLifecycle()` guaranteeing lifecycle-aware state consistency.
* **Components**: Custom Material 3 Scaffold layout, Navigation-bar routing frameworks, and smooth slide Crossfade transitions.
* **Persistent Settings**: Multi-layered persistent parameters syncing device states seamlessly.

---

## 📦 Build Instructions

Prepare your environment with Android Studio Ladybug (or higher) and JDK 17 to build the project:

1. **Clone the project repository**:
   ```bash
   git clone https://github.com/your-username/FlowCapture.git
   cd FlowCapture
   ```

2. **Open the project in Android Studio**.
3. **Let Gradle sync completed project files**.
4. **Compile and Run**:
   - Run the app via a physical device or virtual image module.
   - Or compile via terminal:
     ```bash
     gradle assembleDebug
     ```

---

## 📝 License

Distributed under the Apache License 2.0. See `LICENSE` for more information.

---

*FlowCapture — capture your screen in professional style.*
