import React, { useState, useEffect } from 'react';
import { AudioProvider } from './context/AudioContext';
import { Navbar, NavTab } from './components/layout/Navbar';
import { DesktopPlayerBar } from './components/player/DesktopPlayerBar';
import { MobileMiniPlayer } from './components/player/MobileMiniPlayer';
import { NowPlayingModal } from './components/player/NowPlayingModal';
import { QueueDrawer } from './components/player/QueueDrawer';
import { LyricsView } from './components/player/LyricsView';
import { HomeView } from './components/views/HomeView';
import { SearchView } from './components/views/SearchView';
import { LibraryView } from './components/views/LibraryView';
import { StatsView } from './components/views/StatsView';
import { SettingsView } from './components/views/SettingsView';
import { Playlist, Track, ThemeSettings, ThemeMode } from './types';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('home');
  const [isMobile, setIsMobile] = useState<boolean>(() => window.innerWidth < 768);
  const [isNowPlayingOpen, setIsNowPlayingOpen] = useState<boolean>(false);
  const [isQueueOpen, setIsQueueOpen] = useState<boolean>(false);
  const [isLyricsOpen, setIsLyricsOpen] = useState<boolean>(false);

  const [themeSettings, setThemeSettings] = useState<ThemeSettings>(() => {
    const saved = localStorage.getItem('ibrastream_theme_settings');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {}
    }
    return {
      mode: 'noir',
      accentColor: '#e8e8e8',
      backgroundColor: '#0f0f0f',
      borderRadius: 'rounded',
      ambientGlow: true,
    };
  });

  const [playlists, setPlaylists] = useState<Playlist[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('ibrastream_playlists') || '[]');
    } catch {
      return [];
    }
  });

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleChangeTheme = (mode: ThemeMode) => {
    setThemeSettings(prev => {
      const next = { ...prev, mode };
      localStorage.setItem('ibrastream_theme_settings', JSON.stringify(next));
      return next;
    });
  };

  const handleToggleAmbientGlow = () => {
    setThemeSettings(prev => {
      const next = { ...prev, ambientGlow: !prev.ambientGlow };
      localStorage.setItem('ibrastream_theme_settings', JSON.stringify(next));
      return next;
    });
  };

  return (
    <AudioProvider>
      <div className="flex h-screen w-screen overflow-hidden bg-[#0f0f0f] text-gray-200 font-sans select-none">
        {/* Navigation Sidebar (Desktop) */}
        {!isMobile && (
          <Navbar
            activeTab={activeTab}
            onSelectTab={setActiveTab}
            onOpenAuth={() => {}}
            isMobile={false}
          />
        )}

        {/* Main Scrollable Content Area */}
        <main className="flex-1 overflow-y-auto px-6 py-8 md:px-10 scrollbar-none">
          {activeTab === 'home' && (
            <HomeView
              playlists={playlists}
              onSelectPlaylist={() => {}}
              onOpenArtist={() => {}}
            />
          )}

          {activeTab === 'search' && (
            <SearchView
              onOpenArtist={() => {}}
              onOpenAlbum={() => {}}
              onAddToPlaylist={() => {}}
            />
          )}

          {activeTab === 'library' && (
            <LibraryView
              playlists={playlists}
              onSelectPlaylist={() => {}}
              onCreatePlaylist={() => {}}
              onImportSpotify={() => {}}
              onOpenArtist={() => {}}
              onOpenAlbum={() => {}}
            />
          )}

          {activeTab === 'stats' && <StatsView />}

          {activeTab === 'settings' && (
            <SettingsView
              themeSettings={themeSettings}
              onChangeTheme={handleChangeTheme}
              onToggleAmbientGlow={handleToggleAmbientGlow}
            />
          )}
        </main>

        {/* Desktop Player Bar (Persistent on desktop) */}
        {!isMobile && (
          <DesktopPlayerBar
            onOpenNowPlaying={() => setIsNowPlayingOpen(true)}
            onToggleQueue={() => setIsQueueOpen(q => !q)}
            isQueueOpen={isQueueOpen}
          />
        )}

        {/* Mobile Mini Player Bar (Floating above mobile bottom nav) */}
        {isMobile && (
          <MobileMiniPlayer
            onClick={() => setIsNowPlayingOpen(true)}
          />
        )}

        {/* Mobile Bottom Navigation Bar */}
        {isMobile && (
          <Navbar
            activeTab={activeTab}
            onSelectTab={setActiveTab}
            onOpenAuth={() => {}}
            isMobile={true}
          />
        )}

        {/* Overlays / Modals */}
        <NowPlayingModal
          isOpen={isNowPlayingOpen}
          onClose={() => setIsNowPlayingOpen(false)}
          onOpenLyrics={() => setIsLyricsOpen(true)}
          onOpenQueue={() => setIsQueueOpen(true)}
        />

        <QueueDrawer
          isOpen={isQueueOpen}
          onClose={() => setIsQueueOpen(false)}
        />

        <LyricsView
          isOpen={isLyricsOpen}
          onClose={() => setIsLyricsOpen(false)}
        />
      </div>
    </AudioProvider>
  );
};
