import React, { useState } from 'react';
import { Playlist, Track, Artist, Album } from '../../types';

export type LibrarySubTab = 'playlists' | 'favorites' | 'albums' | 'artists' | 'history';

interface LibraryViewProps {
  playlists: Playlist[];
  onSelectPlaylist: (playlist: Playlist) => void;
  onCreatePlaylist: () => void;
  onImportSpotify: () => void;
  onOpenArtist: (artist: { id: string; name: string }) => void;
  onOpenAlbum: (albumId: string) => void;
}

export const LibraryView: React.FC<LibraryViewProps> = ({
  playlists,
  onSelectPlaylist,
  onCreatePlaylist,
  onImportSpotify,
  onOpenArtist,
  onOpenAlbum,
}) => {
  const [subTab, setSubTab] = useState<LibrarySubTab>('playlists');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'recents' | 'alphabetical' | 'custom'>('recents');
  const [showHidden, setShowHidden] = useState(false);

  const filteredPlaylists = playlists.filter(pl => {
    if (!showHidden && pl.isHidden) return false;
    return pl.name.toLowerCase().includes(searchQuery.toLowerCase());
  });

  return (
    <div className="flex flex-col gap-6 pb-32 animate-[fadeIn_0.3s_ease]">
      {/* Top Header: Title & Action Buttons */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <svg className="w-6 h-6 text-brand-accent" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20M4 4.5A2.5 2.5 0 016.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15z" />
          </svg>
          Your Library
        </h1>

        <div className="flex items-center gap-2.5">
          <button
            onClick={onImportSpotify}
            className="px-3.5 py-1.5 rounded-full border border-white/10 hover:border-brand-accent hover:text-white text-xs font-semibold text-gray-300 transition-all active:scale-95 flex items-center gap-1.5"
          >
            <svg className="w-3.5 h-3.5 text-emerald-400" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
            </svg>
            Import Spotify
          </button>

          <button
            onClick={onCreatePlaylist}
            className="px-3.5 py-1.5 rounded-full bg-white text-black text-xs font-semibold hover:bg-gray-200 transition-all active:scale-95 flex items-center gap-1.5 shadow-md"
          >
            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Create Playlist
          </button>
        </div>
      </div>

      {/* Sub-Tabs: Playlists, Liked, Albums, Artists, History */}
      <div className="flex items-center gap-2 border-b border-white/10 pb-3 overflow-x-auto scrollbar-none">
        {(
          [
            { id: 'playlists', label: 'Playlists' },
            { id: 'favorites', label: 'Liked Songs' },
            { id: 'albums', label: 'Albums' },
            { id: 'artists', label: 'Artists' },
            { id: 'history', label: 'Listening History' },
          ] as const
        ).map(tab => (
          <button
            key={tab.id}
            onClick={() => setSubTab(tab.id)}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all whitespace-nowrap ${
              subTab === tab.id
                ? 'bg-white/15 text-white shadow-sm'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Search & Sort Controls within Library */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div className="relative flex-1 max-w-sm">
          <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="M21 21l-4.35-4.35" />
          </svg>
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Search in library..."
            className="w-full h-9 pl-9 pr-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 text-xs focus:outline-none focus:border-white/30"
          />
        </div>

        {/* Sort Dropdown */}
        <div className="flex items-center gap-2 text-xs text-gray-400">
          <span>Sort by:</span>
          <select
            value={sortBy}
            onChange={e => setSortBy(e.target.value as any)}
            className="bg-white/5 border border-white/10 text-white text-xs px-2.5 py-1.5 rounded-lg focus:outline-none cursor-pointer"
          >
            <option value="recents" className="bg-[#181818] text-white">Recents</option>
            <option value="alphabetical" className="bg-[#181818] text-white">Alphabetical</option>
            <option value="custom" className="bg-[#181818] text-white">Custom (Drag)</option>
          </select>
        </div>
      </div>

      {/* Playlists Grid */}
      {subTab === 'playlists' && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {filteredPlaylists.map(pl => (
            <div
              key={pl.id}
              onClick={() => onSelectPlaylist(pl)}
              className="flex flex-col gap-3 p-3.5 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/10 cursor-pointer group transition-all"
            >
              <div className="relative aspect-square rounded-xl overflow-hidden bg-[#181818] shadow-md">
                <img
                  src={pl.coverUrl || 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80'}
                  alt={pl.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                />
                {pl.isPinned && (
                  <span className="absolute top-2 left-2 text-[10px] font-bold bg-black/60 px-2 py-0.5 rounded-full text-white backdrop-blur-md">
                    Pinned •
                  </span>
                )}
              </div>
              <div className="flex flex-col min-w-0">
                <span className="text-sm font-semibold text-white truncate">{pl.name}</span>
                <span className="text-xs text-gray-400 truncate mt-0.5">{pl.tracks.length} tracks</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
