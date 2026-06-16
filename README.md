# Hermes Dispatch

<p align="center">
  <img src="assets/hermes-dispatch-icon-512.png" width="140" alt="Hermes Dispatch app icon" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-0.1.3-6200EA?style=for-the-badge" />
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/kotlin-2.0-7C4DFF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-latest-00B0FF?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
</p>

<p align="center">
  <b>Mobile command center for your <a href="https://github.com/nousresearch/hermes-agent">Hermes Agent</a> deployment.</b>
</p>

---

## What is Hermes Dispatch?

Hermes Dispatch is a native Android app that connects to a self-hosted [Hermes Agent](https://hermes-agent.nousresearch.com) gateway and gives you a full-featured mobile interface for your personal AI agent — chat, sessions, cron tasks, channel management, and real-time tool approvals, all from your phone.

## Features

- 💬 **Full chat interface** — streaming responses, markdown rendering, inline code blocks, tool progress indicators
- 📋 **Session management** — browse, resume, and start conversations across all your Hermes sessions
- ⏰ **Cron & task scheduler** — view, create, run, and delete scheduled agent automations
- 📡 **Channel manager** — see all connected platforms (Telegram, Discord, Slack, Matrix, etc.)
- 🔐 **Approval flow** — real-time risk-rated approval dialogs for tool calls that need confirmation
- 🔔 **Push notifications** — alerts for pending approvals and completed tasks
- 👤 **Profile switcher** — switch between Hermes profiles without reconnecting
- 🌙 **Dark / light theme** — Material 3 design with a deep space dark palette

## Screenshots

> Coming soon — install the APK and screenshot your own instance!

## Installation

### Download APK

Grab the latest APK from the [Releases](https://github.com/crazy54/hermes-dispatch/releases) page and sideload it:

```bash
adb install hermes-dispatch-v0.1.0-debug.apk
```

### Build from source

Requirements: JDK 17, Android SDK 34, Gradle 8.6

```bash
git clone https://github.com/crazy54/hermes-dispatch.git
cd hermes-dispatch
./gradlew app:assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Connecting

1. Launch Hermes Dispatch
2. Enter your Hermes gateway URL (e.g. `https://hermes.yourdomain.com`)
3. Enter your API token
4. Select your profile (default: `default`)
5. Tap **Connect**

You need a running [Hermes Agent](https://hermes-agent.nousresearch.com) gateway with the REST/SSE API enabled.

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Networking | Retrofit + OkHttp + SSE streaming |
| Fonts | Inter + JetBrains Mono (Google Fonts) |
| Images | Coil |
| Storage | DataStore |
| Min SDK | Android 8.0 (API 26) |

## Project Structure

```
app/src/main/java/com/nousresearch/hermes/
├── data/
│   ├── api/          # Retrofit service + SSE streaming client
│   ├── model/        # Data models (sessions, messages, cron, channels)
│   └── repository/   # HermesRepository
├── di/               # Hilt modules
├── ui/
│   ├── components/   # MessageBubble, ApprovalDialog, GlassCard
│   ├── screens/      # Chat, Sessions, Connect, Cron, Channels
│   └── theme/        # Colors, Typography (Inter), Shapes
├── util/             # NotificationHelper
└── viewmodel/        # ChatViewModel, SessionViewModel, CronTaskViewModel, etc.
```

## Roadmap

- [ ] Voice input / output
- [ ] File and image upload
- [ ] Widget for quick message dispatch
- [ ] Release signing + Play Store listing

## License

MIT — see [LICENSE](LICENSE)

---

<p align="center">Built with ⚡ by <a href="https://github.com/crazy54">crazy54</a> · Powered by <a href="https://nousresearch.com">Nous Research</a> Hermes Agent</p>
