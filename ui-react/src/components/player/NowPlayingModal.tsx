import React, { useState } from 'react';
import { useAudio } from '../../context/AudioContext';
import { Track } from '../../types';

interface NowPlayingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenAlbum?: (albumId: string) => void;
  onOpenArtist?: (artist: { id: string; name: string }) => void;
  onOpenLyrics?: () => void;
  onOpenQueue?: () => void;
  isFavorite?: boolean;
  onToggleFavorite?: (track: Track) => void;
}

export const NowPlayingModal: React.FC<NowPlayingModalProps> = ({
  isOpen,
  onClose,
  onOpenAlbum,
  onOpenArtist,
  onOpenLyrics,
  onOpenQueue,
  isFavorite = false,
  onToggleFavorite,
}) => {
  const {
    currentTrack,
    isPlaying,
    isLoading,
    currentTime,
    duration,
    isShuffle,
    isRepeat,
    seamlessTransitions,
    togglePlay,
    nextTrack,
    prevTrack,
    seek,
    toggleShuffle,
    toggleRepeat,
    toggleSeamless,
  } = useAudio();

  const [activeVisualMode, setActiveVisualMode] = useState<'cover' | 'vinyl'>('cover');

  if (!isOpen || !currentTrack) return null;

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#0f0f0f] flex flex-col justify-between p-6 select-none animate-[slideUp_0.25s_cubic-bezier(0.16,1,0.3,1)] overflow-hidden">
      {/* Dynamic Ambient Blur Glow Background */}
      <div
        className="absolute -top-[20%] -left-[20%] w-[140%] h-[140%] opacity-25 blur-3xl pointer-events-none transition-all duration-1000"
        style={{
          backgroundImage: `url(${currentTrack.thumbnail})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      />

      {/* Top Header */}
      <header className="relative flex items-center justify-between z-10">
        <button
          onClick={onClose}
          className="p-2 text-gray-400 hover:text-white rounded-full bg-white/5 active:scale-95 transition-all"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] uppercase tracking-widest text-gray-400 font-bold">Playing From</span>
          <span className="text-xs font-semibold text-white truncate max-w-[200px]">
            {currentTrack.album || 'IbraStream Library'}
          </span>
        </div>

        <button
          onClick={() => setActiveVisualMode(v => (v === 'cover' ? 'vinyl' : 'cover'))}
          className="p-2 text-gray-400 hover:text-white rounded-full bg-white/5 active:scale-95 transition-all text-xs font-semibold"
          title="Toggle Vinyl / Artwork"
        >
          {activeVisualMode === 'cover' ? 'Vinyl' : 'Art'}
        </button>
      </header>

      {/* Center Artwork / Vinyl View */}
      <div className="relative my-auto flex items-center justify-center z-10 py-4">
        {activeVisualMode === 'cover' ? (
          <div className="w-72 h-72 sm:w-80 sm:h-80 rounded-3xl overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.8)] border border-white/10">
            <img src={currentTrack.thumbnail} alt={currentTrack.title} className="w-full h-full object-cover" />
          </div>
        ) : (
          <div
            className={`w-64 h-64 sm:w-72 sm:h-72 rounded-full border-4 border-[#222] bg-black p-3 shadow-2xl relative flex items-center justify-center ${
              isPlaying ? 'animate-spin-slow' : ''
            }`}
          >
            {/* Vinyl Grooves */}
            <div className="absolute inset-4 rounded-full border border-white/5" />
            <div className="absolute inset-8 rounded-full border border-white/5" />
            <div className="absolute inset-12 rounded-full border border-white/5" />
            <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-black shadow-inner">
              <img src={currentTrack.thumbnail} alt={currentTrack.title} className="w-full h-full object-cover" />
            </div>
            <div className="absolute w-4 h-4 rounded-full bg-[#111] border border-white/20" />
          </div>
        )}
      </div>

      {/* Bottom Section: Metadata, Slider & Controls */}
      <div className="relative flex flex-col gap-5 z-10">
        {/* Track Metadata & Favorite */}
        <div className="flex items-center justify-between">
          <div className="flex flex-col min-w-0 pr-4">
            <h1 className="text-xl sm:text-2xl font-bold text-white truncate tracking-tight">
              {currentTrack.title}
            </h1>
            <p
              onClick={() =>
                onOpenArtist &&
                currentTrack.artistId &&
                onOpenArtist({ id: currentTrack.artistId, name: currentTrack.artist })
              }
              className="text-sm text-gray-400 truncate mt-1 hover:text-white cursor-pointer transition-colors"
            >
              {currentTrack.artist}
            </p>
          </div>

          <button
            onClick={() => onToggleFavorite && onToggleFavorite(currentTrack)}
            className={`p-3 rounded-full bg-white/5 active:scale-90 transition-all ${
              isFavorite ? 'text-red-500' : 'text-gray-400 hover:text-white'
            }`}
          >
            <svg
              className="w-6 h-6"
              viewBox="0 0 24 24"
              fill={isFavorite ? 'currentColor' : 'none'}
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
            </svg>
          </button>
        </div>

        {/* Progress Slider */}
        <div className="flex flex-col gap-1.5">
          <input
            type="range"
            min={0}
            max={duration || 100}
            value={currentTime}
            onChange={e => seek(parseFloat(e.target.value))}
            className="w-full h-1.5 bg-white/10 rounded-lg appearance-none cursor-pointer accent-white focus:outline-none"
          />
          <div className="flex items-center justify-between text-xs text-gray-400 font-medium tabular-nums">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Playback Controls Row */}
        <div className="flex items-center justify-between px-2">
          {/* Shuffle */}
          <button
            onClick={toggleShuffle}
            className={`p-2 transition-all ${isShuffle ? 'text-white' : 'text-gray-600'}`}
            title="Shuffle"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5" />
            </svg>
          </button>

          {/* Previous */}
          <button onClick={prevTrack} className="p-2 text-white active:scale-90 transition-all">
            <svg className="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" />
            </svg>
          </button>

          {/* Large Play/Pause */}
          <button
            onClick={togglePlay}
            disabled={isLoading}
            className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center active:scale-95 transition-all shadow-xl shadow-white/20"
          >
            {isLoading ? (
              <div className="w-6 h-6 border-3 border-black border-t-transparent rounded-full animate-spin" />
            ) : isPlaying ? (
              <svg className="w-7 h-7 fill-current" viewBox="0 0 24 24">
                <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
              </svg>
            ) : (
              <svg className="w-7 h-7 fill-current ml-1" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
            )}
          </button>

          {/* Next */}
          <button onClick={nextTrack} className="p-2 text-white active:scale-90 transition-all">
            <svg className="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" />
            </svg>
          </button>

          {/* Repeat */}
          <button
            onClick={toggleRepeat}
            className={`p-2 relative transition-all ${isRepeat === 'none' ? 'text-gray-600' : 'text-white'}`}
            title="Repeat"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 1l4 4-4 4M3 11V9a4 4 0 014-4h14M7 23l-4-4 4-4M21 13v2a4 4 0 01-4 4H3" />
            </svg>
            {isRepeat === 'one' && (
              <span className="absolute 0 top-0 right-0 w-3.5 h-3.5 text-[8px] font-black bg-white text-black rounded-full flex items-center justify-center">
                1
              </span>
            )}
          </button>
        </div>

        {/* Bottom Utility Bar: Lyrics, Queue, Crossfade */}
        <div className="flex items-center justify-between pt-2 border-t border-white/5">
          <button
            onClick={toggleSeamless}
            className={`text-xs px-3 py-1.5 rounded-full border transition-all flex items-center gap-1.5 ${
              seamlessTransitions
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                : 'border-white/10 text-gray-500'
            }`}
          >
            <span className="w-1.5 h-1.5 rounded-full bg-current" />
            Crossfade
          </button>

          <div className="flex items-center gap-3">
            <button
              onClick={onOpenLyrics}
              className="text-xs font-semibold px-3 py-1.5 rounded-full bg-white/10 hover:bg-white/15 text-white active:scale-95 transition-all flex items-center gap-1.5"
            >
              <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 18V5l12-2v13M9 9l12-2M6 18a3 3 0 100-6 3 3 0 000 6zM18 16a3 3 0 100-6 3 3 0 000 6z" />
              </svg>
              Lyrics
            </button>

            <button
              onClick={onOpenQueue}
              className="p-2 rounded-full bg-white/10 text-white active:scale-95 transition-all"
              title="Queue"
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
