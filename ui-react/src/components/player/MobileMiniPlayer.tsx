import React from 'react';
import { useAudio } from '../../context/AudioContext';
import { Track } from '../../types';

interface MobileMiniPlayerProps {
  onClick: () => void;
  isFavorite?: boolean;
  onToggleFavorite?: (track: Track) => void;
}

export const MobileMiniPlayer: React.FC<MobileMiniPlayerProps> = ({
  onClick,
  isFavorite = false,
  onToggleFavorite,
}) => {
  const { currentTrack, isPlaying, isLoading, currentTime, duration, togglePlay, nextTrack } = useAudio();

  if (!currentTrack) return null;

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="fixed bottom-20 left-3 right-3 z-40 select-none animate-[slideUp_0.2s_ease]">
      <div
        onClick={onClick}
        className="relative h-15 rounded-2xl bg-[#0f0f0f]/90 backdrop-blur-2xl border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.5)] p-2.5 flex items-center justify-between cursor-pointer active:scale-[0.99] transition-all overflow-hidden"
      >
        {/* Real-time thin progress line at the very top of mini-player */}
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-white/10">
          <div
            className="h-full bg-white transition-all duration-300"
            style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
          />
        </div>

        {/* Left: Thumbnail & Track info */}
        <div className="flex items-center gap-3 min-w-0 pr-2">
          <div className="relative w-11 h-11 rounded-xl overflow-hidden shrink-0 border border-white/10 bg-[#171717]">
            <img src={currentTrack.thumbnail} alt={currentTrack.title} className="w-full h-full object-cover" />
            {isPlaying && (
              <div className="absolute top-1 right-1 w-2 h-2 bg-brand-accent rounded-full ring-1 ring-black animate-pulse" />
            )}
          </div>

          <div className="flex flex-col min-w-0">
            <span className="text-xs font-semibold text-white truncate">{currentTrack.title}</span>
            <span className="text-[11px] text-gray-400 truncate mt-0.5">{currentTrack.artist}</span>
          </div>
        </div>

        {/* Right: Quick action controls */}
        <div className="flex items-center gap-1 shrink-0" onClick={e => e.stopPropagation()}>
          {/* Favorite button */}
          <button
            onClick={() => onToggleFavorite && onToggleFavorite(currentTrack)}
            className={`p-2 rounded-full transition-all active:scale-90 ${
              isFavorite ? 'text-red-500' : 'text-gray-400 hover:text-white'
            }`}
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

          {/* Play/Pause circular white button */}
          <button
            onClick={togglePlay}
            disabled={isLoading}
            className="w-9 h-9 rounded-full bg-white text-black flex items-center justify-center active:scale-90 transition-all shadow-md shadow-white/20"
          >
            {isLoading ? (
              <div className="w-4 h-4 border-2 border-black border-t-transparent rounded-full animate-spin" />
            ) : isPlaying ? (
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
              </svg>
            ) : (
              <svg className="w-4 h-4 fill-current ml-0.5" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
            )}
          </button>

          {/* Skip Next button */}
          <button
            onClick={nextTrack}
            className="p-2 text-gray-400 hover:text-white transition-all active:scale-90"
          >
            <svg className="w-4.5 h-4.5 fill-current" viewBox="0 0 24 24">
              <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};
