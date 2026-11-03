# IbraMusic Mobile — Technical Decisions

## 1. Architectural Foundation
- **Platform**: Native Android (Min SDK 29 / Android 10, Target SDK 34 / Android 14).
- **Architecture**: Clean Architecture with MVVM, single unidirectional data flow via Kotlin Coroutines and `StateFlow`.
- **UI Toolkit**: 100% Jetpack Compose with Material 3 foundation, customized with IbraMusic's signature dark/noir aesthetic.
- **Dependency Injection**: Google Dagger Hilt with Kotlin Symbol Processing (KSP).

## 2. Playback Architecture (AndroidX Media3)
- **Engine**: `androidx.media3.exoplayer.ExoPlayer` wrapped in a singleton `ExoPlayerWrapper`.
- **Background Service**: `androidx.media3.session.MediaSessionService` implemented via `IbraMusicPlaybackService` with `foregroundServiceType="mediaPlayback"`.
- **Media Controls**: Media3 `MediaSession` integration supporting lock-screen transport controls, Notification Shade MediaStyle controls, Bluetooth AVRCP commands, Android Auto, and audio route switching.
- **Audio Attributes**: `USAGE_MEDIA` with `CONTENT_TYPE_MUSIC`.
- **Interruption & Focus**: `setHandleAudioBecomingNoisy(true)` for automatic headphone disconnect pausing; cubic volume shaping ($v = k^3$) with $0.001f$ floor preventing Chromium/system clock stall on pause/duck.

## 3. Persistent Two-Tier Queue System
- **Tier 1 (User Queue)**: Priority list for user-driven "Play Next" (inserted at index 0) and "Add to Queue" (appended).
- **Tier 2 (Playlist / Context Queue)**: Contextual list originating from albums, search results, or recommendations.
- **History Stack**: Tracks already played to enable backward navigation.
- **Previous Track Threshold**: Seeks to 0:00 if position > 3000ms; navigates to previous track in history if position <= 3000ms.
- **Shuffle & Repeat**: Preserves un-shuffled context order so disabling shuffle restores original track order. Supports Repeat Off, Repeat All, Repeat One.
- **Room Persistence**: Active queue saved to `queue_items` table and restored automatically across app restarts.

## 4. Local-First Philosophy & Multi-Provider Architecture
- **Offline / Local Independence**: Works 100% offline without network or user login.
- **Local Audio Provider**: Native `MediaStore.Audio.Media` scanner with album art resolution and local content URIs.
- **Deezer Provider**: Public metadata search provider delivering studio high-res artwork and audio previews.
- **Unified Resolver**: Decouples player from specific providers by routing track resolution dynamically via `ProviderRegistry`.

