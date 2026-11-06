# Graph Report - ui-react  (2026-11-06)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 113 nodes · 226 edges · 10 communities
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7944951a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9

## God Nodes (most connected - your core abstractions)
1. `useAudio()` - 17 edges
2. `Track` - 15 edges
3. `App()` - 13 edges
4. `Playlist` - 6 edges
5. `LyricsView()` - 5 edges
6. `AudioContextValue` - 4 edges
7. `ThemeMode` - 4 edges
8. `ThemeSettings` - 4 edges
9. `DesktopPlayerBar()` - 4 edges
10. `MobileMiniPlayer()` - 4 edges

## Surprising Connections (you probably didn't know these)
- `DesktopPlayerBarProps` --references--> `Track`  [EXTRACTED]
  src/components/player/DesktopPlayerBar.tsx → src/types/index.ts
- `MobileMiniPlayerProps` --references--> `Track`  [EXTRACTED]
  src/components/player/MobileMiniPlayer.tsx → src/types/index.ts
- `NowPlayingModalProps` --references--> `Track`  [EXTRACTED]
  src/components/player/NowPlayingModal.tsx → src/types/index.ts
- `SearchViewProps` --references--> `Track`  [EXTRACTED]
  src/components/views/SearchView.tsx → src/types/index.ts
- `HomeViewProps` --references--> `Playlist`  [EXTRACTED]
  src/components/views/HomeView.tsx → src/types/index.ts

## Import Cycles
- None detected.

## Communities (10 total, 0 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.08
Nodes (22): fs, jsCode, jsPath, mainViewsBlock, path, apiUrls, fs, jsCode (+14 more)

### Community 1 - "Community 1"
Cohesion: 0.23
Nodes (16): App(), Navbar(), NavbarProps, NavTab, DesktopPlayerBar(), MobileMiniPlayer(), NowPlayingModal(), QueueDrawer() (+8 more)

### Community 2 - "Community 2"
Cohesion: 0.23
Nodes (11): DesktopPlayerBarProps, MobileMiniPlayerProps, NowPlayingModalProps, SearchFilter, SearchViewProps, AudioContext, AudioContextValue, PlaybackState (+3 more)

### Community 3 - "Community 3"
Cohesion: 0.25
Nodes (10): HomeViewProps, LibrarySubTab, LibraryViewProps, StatsViewProps, Album, Artist, EqPreset, LeaderboardUser (+2 more)

### Community 4 - "Community 4"
Cohesion: 0.25
Nodes (7): cssCode, cssPath, fs, jsCode, jsPath, path, uniqueViews

### Community 5 - "Community 5"
Cohesion: 0.29
Nodes (6): fs, jsCode, jsPath, keywords, path, results

### Community 6 - "Community 6"
Cohesion: 0.29
Nodes (6): fs, jsCode, jsPath, path, playerSnippets, terms

### Community 7 - "Community 7"
Cohesion: 0.50
Nodes (4): LyricsView(), LyricsViewProps, parseLrc(), LyricLine

### Community 8 - "Community 8"
Cohesion: 0.83
Nodes (3): SettingsViewProps, ThemeMode, ThemeSettings

### Community 9 - "Community 9"
Cohesion: 0.50
Nodes (3): animations, colors, glassmorphism

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `Community 2` to `Community 1`, `Community 3`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.07936507936507936 - nodes in this community are weakly interconnected._
- **Why does `useAudio()` connect `Community 1` to `Community 8`, `Community 2`, `Community 3`, `Community 7`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **Why does `Playlist` connect `Community 3` to `Community 1`?**
  _High betweenness centrality (0.007) - this node is a cross-community bridge._