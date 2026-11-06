import React from 'react';
import { useAudio } from '../../context/AudioContext';
import { Track } from '../../types';

interface DesktopPlayerBarProps {
  onOpenNowPlaying?: () => void;
  onOpenArtist?: (artist: { id: string; name: string }) => void;
  onToggleQueue?: () => void;
  isQueueOpen?: boolean;
  isFavorite?: boolean;
  onToggleFavorite?: (track: Track) => void;
}

export const DesktopPlayerBar: React.FC<DesktopPlayerBarProps> = ({
  onOpenNowPlaying,
  onOpenArtist,
  onToggleQueue,
  isQueueOpen,
  isFavorite = false,
  onToggleFavorite,
}) => {
  const {
    currentTrack,
    isPlaying,
    isLoading,
    currentTime,
    duration,
    volume,
    isMuted,
    isShuffle,
    isRepeat,
    seamlessTransitions,
    togglePlay,
    nextTrack,
    prevTrack,
    seek,
    changeVolume,
    toggleMute,
    toggleShuffle,
    toggleRepeat,
    toggleSeamless,
  } = useAudio();

  if (!currentTrack) return null;

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <footer className="fixed bottom-0 left-0 right-0 h-24 bg-[#0f0f0f]/95 backdrop-blur-xl border-t border-white/10 z-40 px-6 flex items-center justify-between select-none">
      {/* Left: Track Info */}
      <div className="flex items-center gap-4 w-1/3 min-w-[240px]">
        <div
          onClick={onOpenNowPlaying}
          className="relative w-14 h-14 rounded-xl overflow-hidden cursor-pointer group shrink-0 border border-white/10 shadow-lg"
        >
          <img
            src={currentTrack.thumbnail}
            alt={currentTrack.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          {isPlaying && (
            <div className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-brand-accent rounded-full ring-2 ring-black animate-pulse" />
          )}
        </div>

        <div className="flex flex-col min-w-0">
          <span
            onClick={onOpenNowPlaying}
            className="text-sm font-semibold text-white truncate cursor-pointer hover:underline"
          >
            {currentTrack.title}
          </span>
          <div className="text-xs text-gray-400 truncate mt-0.5 flex flex-wrap gap-x-1">
            {currentTrack.artists && currentTrack.artists.length > 0 ? (
              currentTrack.artists.map((artist, idx) => (
                <React.Fragment key={artist.id}>
                  <span
                    onClick={() => onOpenArtist && onOpenArtist(artist)}
                    className="hover:underline hover:text-white cursor-pointer transition-colors"
                  >
                    {artist.name}
                  </span>
                  {idx < currentTrack.artists!.length - 1 && <span>,</span>}
                </React.Fragment>
              ))
            ) : (
              <span
                onClick={() =>
                  onOpenArtist &&
                  currentTrack.artistId &&
                  onOpenArtist({ id: currentTrack.artistId, name: currentTrack.artist })
                }
                className="hover:underline hover:text-white cursor-pointer transition-colors"
              >
                {currentTrack.artist}
              </span>
            )}
          </div>
        </div>

        <button
          onClick={() => onToggleFavorite && onToggleFavorite(currentTrack)}
          className={`p-2 rounded-full hover:bg-white/5 transition-all ${
            isFavorite ? 'text-red-500' : 'text-gray-400 hover:text-white'
          }`}
          title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
        >
          <svg
            className="w-4.5 h-4.5"
            viewBox="0 0 24 24"
            fill={isFavorite ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
          </svg>
        </button>
      </div>

      {/* Center: Playback Controls & Timeline */}
      <div className="flex flex-col items-center gap-2 w-1/3 max-w-[600px]">
        <div className="flex items-center gap-5">
          {/* Shuffle */}
          <button
            onClick={toggleShuffle}
            className={`p-1.5 transition-all ${
              isShuffle ? 'text-white' : 'text-gray-500 hover:text-white'
            }`}
            title="Shuffle"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5" />
            </svg>
          </button>

          {/* Previous */}
          <button
            onClick={prevTrack}
            className="p-1.5 text-gray-400 hover:text-white transition-all active:scale-90"
            title="Previous"
          >
            <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
              <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" />
            </svg>
          </button>

          {/* Play / Pause */}
          <button
            onClick={togglePlay}
            disabled={isLoading}
            className="w-10 h-10 rounded-full bg-white text-black transition-all hover:scale-105 active:scale-95 flex items-center justify-center shadow-md shadow-white/20"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            {isLoading ? (
              <div className="w-4.5 h-4.5 border-2 border-black border-t-transparent rounded-full animate-spin" />
            ) : isPlaying ? (
              <svg className="w-4.5 h-4.5 fill-current" viewBox="0 0 24 24">
                <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
              </svg>
            ) : (
              <svg className="w-4.5 h-4.5 fill-current ml-0.5" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
            )}
          </button>

          {/* Next */}
          <button
            onClick={nextTrack}
            className="p-1.5 text-gray-400 hover:text-white transition-all active:scale-90"
            title="Next"
          >
            <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
              <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" />
            </svg>
          </button>

          {/* Repeat */}
          <button
            onClick={toggleRepeat}
            className={`p-1.5 relative transition-all ${
              isRepeat === 'none' ? 'text-gray-500 hover:text-white' : 'text-white'
            }`}
            title="Repeat"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 1l4 4-4 4M3 11V9a4 4 0 014-4h14M7 23l-4-4 4-4M21 13v2a4 4 0 01-4 4H3" />
            </svg>
            {isRepeat === 'one' && (
              <span className="absolute -top-0.5 -right-0.5 w-3 h-3 text-[7px] font-black bg-white text-black rounded-full flex items-center justify-center">
                1
              </span>
            )}
          </button>
        </div>

        {/* Timeline Slider */}
        <div className="w-full flex items-center gap-3 text-xs text-gray-500 font-medium tabular-nums">
          <span>{formatTime(currentTime)}</span>
          <input
            type="range"
            min={0}
            max={duration || 100}
            value={currentTime}
            onChange={e => seek(parseFloat(e.target.value))}
            className="flex-1 h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-white focus:outline-none"
          />
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      {/* Right: Audio FX & Extras */}
      <div className="flex items-center justify-end gap-3 w-1/3 min-w-[200px]">
        {/* Seamless Transitions (Crossfade) */}
        <button
          onClick={toggleSeamless}
          className={`p-2 rounded-full transition-all relative group ${
            seamlessTransitions
              ? 'text-white bg-white/10 shadow-[0_0_10px_rgba(255,255,255,0.2)]'
              : 'text-gray-500 hover:text-gray-300 hover:bg-white/5'
          }`}
          title={
            seamlessTransitions
              ? 'Seamless Transitions activadas (Crossfade suave)'
              : 'Activar Seamless Transitions'
          }
        >
          <svg className="w-4.5 h-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M2 17h6l4-10h6M18 7l4 4-4 4M2 7h6l2 5M16 17h6M18 13l4 4-4 4" />
          </svg>
          {seamlessTransitions && (
            <span className="absolute -top-1 -right-1 w-2 h-2 bg-emerald-400 rounded-full animate-pulse ring-2 ring-[#0f0f0f]" />
          )}
        </button>

        {/* Queue Drawer Toggle */}
        <button
          onClick={onToggleQueue}
          className={`p-2 rounded-full hover:bg-white/5 transition-all ${
            isQueueOpen ? 'text-white bg-white/10' : 'text-gray-400 hover:text-white'
          }`}
          title="Cola de reproducción"
        >
          <svg className="w-4.5 h-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
          </svg>
        </button>

        {/* Volume */}
        <div className="flex items-center gap-2 bg-white/5 border border-white/5 px-3 py-1.5 rounded-xl">
          <button onClick={toggleMute} className="text-gray-400 hover:text-white transition-all">
            {isMuted || volume === 0 ? (
              <svg className="w-4.5 h-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M11 5L6 9H2v6h4l5 4V5zM23 9l-6 6M17 9l6 6" />
              </svg>
            ) : (
              <svg className="w-4.5 h-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 010 14.14M15.54 8.46a5 5 0 010 7.07" />
              </svg>
            )}
          </button>
          <input
            type="range"
            min={0}
            max={1}
            step={0.01}
            value={isMuted ? 0 : volume}
            onChange={e => changeVolume(parseFloat(e.target.value))}
            className="w-20 h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-white"
          />
        </div>
      </div>
    </footer>
  );
};
