import React from 'react';
import { useAudio } from '../../context/AudioContext';
import { Track } from '../../types';

interface QueueDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export const QueueDrawer: React.FC<QueueDrawerProps> = ({ isOpen, onClose }) => {
  const { currentTrack, userQueue, playlistQueue, removeFromQueue, clearQueue, playTrack } = useAudio();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-md flex justify-end animate-[fadeIn_0.2s_ease]">
      <div className="w-full max-w-md h-full bg-[#121212] border-l border-white/10 flex flex-col p-6 shadow-2xl animate-[slideUp_0.25s_ease]">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <svg className="w-5 h-5 text-brand-accent" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
            </svg>
            Queue
          </h2>
          <div className="flex items-center gap-2">
            {(userQueue.length > 0 || playlistQueue.length > 0) && (
              <button
                onClick={clearQueue}
                className="text-xs text-red-400 hover:text-red-300 px-3 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 transition-all"
              >
                Clear
              </button>
            )}
            <button
              onClick={onClose}
              className="p-1.5 text-gray-400 hover:text-white rounded-full hover:bg-white/10 transition-all"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* Content List */}
        <div className="flex-1 overflow-y-auto py-4 flex flex-col gap-6 scrollbar-thin">
          {/* Now Playing */}
          {currentTrack && (
            <div className="flex flex-col gap-2">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Now Playing</span>
              <div className="flex items-center gap-3 p-2.5 rounded-xl bg-white/10 border border-white/10">
                <img src={currentTrack.thumbnail} alt={currentTrack.title} className="w-11 h-11 rounded-lg object-cover" />
                <div className="flex flex-col min-w-0">
                  <span className="text-sm font-semibold text-white truncate">{currentTrack.title}</span>
                  <span className="text-xs text-gray-400 truncate">{currentTrack.artist}</span>
                </div>
              </div>
            </div>
          )}

          {/* User Priority Queue */}
          {userQueue.length > 0 && (
            <div className="flex flex-col gap-2">
              <span className="text-xs font-bold text-brand-accent uppercase tracking-wider flex items-center justify-between">
                <span>Next in Queue</span>
                <span className="text-[10px] text-gray-400">User priority</span>
              </span>
              <div className="flex flex-col gap-1.5">
                {userQueue.map((track, idx) => (
                  <div
                    key={`${track.id}-${idx}`}
                    className="flex items-center justify-between p-2 rounded-xl bg-white/5 hover:bg-white/10 group transition-all"
                  >
                    <div
                      onClick={() => playTrack(track)}
                      className="flex items-center gap-3 min-w-0 flex-1 cursor-pointer"
                    >
                      <img src={track.thumbnail} alt={track.title} className="w-10 h-10 rounded-lg object-cover" />
                      <div className="flex flex-col min-w-0">
                        <span className="text-xs font-semibold text-white truncate">{track.title}</span>
                        <span className="text-[11px] text-gray-400 truncate">{track.artist}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => removeFromQueue(idx)}
                      className="p-1.5 text-gray-500 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all"
                      title="Remove from queue"
                    >
                      <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M18 6L6 18M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Playlist / Upcoming Context Queue */}
          {playlistQueue.length > 0 && (
            <div className="flex flex-col gap-2">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                Next From Playlist ({playlistQueue.length})
              </span>
              <div className="flex flex-col gap-1.5">
                {playlistQueue.map((track, idx) => (
                  <div
                    key={`${track.id}-${idx}`}
                    onClick={() => playTrack(track, playlistQueue)}
                    className="flex items-center gap-3 p-2 rounded-xl hover:bg-white/5 cursor-pointer transition-all"
                  >
                    <span className="text-xs text-gray-500 w-4 text-center">{idx + 1}</span>
                    <img src={track.thumbnail} alt={track.title} className="w-10 h-10 rounded-lg object-cover" />
                    <div className="flex flex-col min-w-0">
                      <span className="text-xs font-medium text-white truncate">{track.title}</span>
                      <span className="text-[11px] text-gray-400 truncate">{track.artist}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
