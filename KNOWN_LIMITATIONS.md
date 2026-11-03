# IbraMusic Mobile — Known Limitations

1. **Remote Music Providers (Deezer)**:
   - Deezer's public API (`https://api.deezer.com/`) is used for high-fidelity track search and studio artwork discovery.
   - Audio playback from Deezer public API is restricted to 30-second preview streams (`preview` MP3), as full-length audio requires proprietary authenticated DRM streaming. Full-length local playback is supported for user-owned files on device.

2. **YouTube / Piped Stream Resolver**:
   - Web application resolver uses public Piped/Invidious instances for stream extraction. In the native Android app, public Piped endpoints may suffer rate limits or downtime depending on upstream provider availability.

3. **Supabase Cloud Sync**:
   - Supabase sync is optional. Currently operates in Guest Mode locally with SQLite/Room as the source of truth. Remote account creation and row-level cloud synchronization will connect when valid Supabase project credentials (URL & anon key) are configured.

4. **Hardware Specifics**:
   - Bluetooth media buttons and Android Auto controls use standard AndroidX `MediaSession` APIs and require testing on physical hardware or authorized automotive emulators.

