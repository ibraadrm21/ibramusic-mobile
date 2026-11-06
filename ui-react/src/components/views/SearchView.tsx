import React, { useState } from 'react';
import { useAudio } from '../../context/AudioContext';
import { Track } from '../../types';

export type SearchFilter = 'all' | 'track' | 'artist' | 'album' | 'community';

interface SearchViewProps {
  onOpenArtist: (artist: { id: string; name: string }) => void;
  onOpenAlbum: (albumId: string) => void;
  onAddToPlaylist: (track: Track) => void;
}

export const SearchView: React.FC<SearchViewProps> = ({ onOpenArtist, onOpenAlbum, onAddToPlaylist }) => {
  const { playTrack } = useAudio();
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<SearchFilter>('all');
  const [isSearching, setIsSearching] = useState(false);
  const [results, setResults] = useState<Track[]>([]);

  // Search History from localStorage
  const [history, setHistory] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('ibrastream_search_history') || '[]');
    } catch {
      return [];
    }
  });

  const handleSearch = async (term: string) => {
    if (!term.trim()) return;
    setIsSearching(true);

    // Save to history
    const nextHistory = Array.from(new Set([term, ...history])).slice(0, 8);
    setHistory(nextHistory);
    localStorage.setItem('ibrastream_search_history', JSON.stringify(nextHistory));

    try {
      // Deezer / Piped search fallback
      const res = await fetch(`https://api.deezer.com/search?q=${encodeURIComponent(term)}`);
      const data = await res.json();
      if (data && data.data) {
        const mapped: Track[] = data.data.map((item: any) => ({
          id: String(item.id),
          title: item.title,
          artist: item.artist?.name || 'Unknown Artist',
          artistId: String(item.artist?.id || ''),
          album: item.album?.title,
          albumId: String(item.album?.id || ''),
          duration: item.duration || 180,
          thumbnail: item.album?.cover_medium || item.album?.cover_big || item.artist?.picture_medium,
        }));
        setResults(mapped);
      }
    } catch {
      setResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="flex flex-col gap-6 pb-32 animate-[fadeIn_0.3s_ease]">
      {/* Search Input Bar */}
      <div className="relative w-full max-w-2xl">
        <svg
          className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>

        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch(query)}
          placeholder="Search songs, artists, albums, or community..."
          className="w-full h-12 pl-12 pr-10 rounded-2xl bg-white/5 border border-white/10 hover:border-white/20 focus:border-white/40 focus:bg-white/10 text-white placeholder-gray-500 text-sm focus:outline-none transition-all shadow-inner"
        />

        {query && (
          <button
            onClick={() => {
              setQuery('');
              setResults([]);
            }}
            className="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 text-gray-400 hover:text-white"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Filter Category Chips (Track, Artist, Album, Community) */}
      <div className="flex items-center gap-2 overflow-x-auto scrollbar-none pb-1">
        {(['all', 'track', 'artist', 'album', 'community'] as SearchFilter[]).map(cat => (
          <button
            key={cat}
            onClick={() => setFilter(cat)}
            className={`px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-wider transition-all whitespace-nowrap ${
              filter === cat
                ? 'bg-white text-black shadow-md'
                : 'bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white border border-white/5'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Recent Search History Chips */}
      {history.length > 0 && results.length === 0 && (
        <div className="flex flex-col gap-3">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Recent Searches</span>
          <div className="flex flex-wrap gap-2">
            {history.map(term => (
              <button
                key={term}
                onClick={() => {
                  setQuery(term);
                  handleSearch(term);
                }}
                className="px-3.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs text-gray-300 hover:text-white transition-all flex items-center gap-2"
              >
                <span>{term}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Results List */}
      {isSearching ? (
        <div className="py-20 flex flex-col items-center justify-center gap-3 text-gray-400">
          <div className="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin" />
          <span className="text-xs">Searching database...</span>
        </div>
      ) : results.length > 0 ? (
        <div className="flex flex-col gap-2">
          {results.map((track, idx) => (
            <div
              key={track.id}
              onClick={() => playTrack(track, results)}
              className="flex items-center justify-between p-3 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/10 cursor-pointer group transition-all"
            >
              <div className="flex items-center gap-3.5 min-w-0 flex-1">
                <span className="text-xs text-gray-500 w-5 text-center">{idx + 1}</span>
                <img src={track.thumbnail} alt={track.title} className="w-11 h-11 rounded-xl object-cover shrink-0" />
                <div className="flex flex-col min-w-0">
                  <span className="text-sm font-semibold text-white truncate group-hover:text-brand-accent transition-colors">
                    {track.title}
                  </span>
                  <span
                    onClick={e => {
                      e.stopPropagation();
                      if (track.artistId) onOpenArtist({ id: track.artistId, name: track.artist });
                    }}
                    className="text-xs text-gray-400 truncate hover:underline hover:text-white"
                  >
                    {track.artist}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-3" onClick={e => e.stopPropagation()}>
                <button
                  onClick={() => onAddToPlaylist(track)}
                  className="p-2 text-gray-400 hover:text-white rounded-full hover:bg-white/10 transition-all opacity-0 group-hover:opacity-100"
                  title="Add to playlist"
                >
                  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 5v14M5 12h14" />
                  </svg>
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
};
