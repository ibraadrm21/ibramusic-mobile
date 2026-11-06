import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { Track, RepeatMode, PlaybackState, QueueState } from '../types';

interface AudioContextValue extends PlaybackState, QueueState {
  playTrack: (track: Track, newQueue?: Track[], playlistId?: string) => Promise<void>;
  togglePlay: () => void;
  pause: () => void;
  resume: () => void;
  nextTrack: () => void;
  prevTrack: () => void;
  seek: (seconds: number) => void;
  changeVolume: (vol: number) => void;
  toggleMute: () => void;
  toggleShuffle: () => void;
  toggleRepeat: () => void;
  toggleSeamless: () => void;
  setSeamlessDuration: (seconds: number) => void;
  toggleAmbientGlow: () => void;
  addToQueue: (track: Track) => void;
  playNext: (track: Track) => void;
  removeFromQueue: (index: number) => void;
  clearQueue: () => void;
}

const AudioContext = createContext<AudioContextValue | null>(null);

export const AudioProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentTrack, setCurrentTrack] = useState<Track | null>(() => {
    try {
      const saved = localStorage.getItem('ibrastream_current_track');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);
  const [volume, setVolume] = useState<number>(() => {
    return Number(localStorage.getItem('ibrastream_volume') ?? 1);
  });
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [isShuffle, setIsShuffle] = useState<boolean>(() => {
    return localStorage.getItem('ibrastream_is_shuffle') === 'true';
  });
  const [isRepeat, setIsRepeat] = useState<RepeatMode>(() => {
    return (localStorage.getItem('ibrastream_is_repeat') as RepeatMode) || 'none';
  });
  const [seamlessTransitions, setSeamlessTransitions] = useState<boolean>(() => {
    return localStorage.getItem('ibrastream_seamless_transitions') !== 'false';
  });
  const [seamlessDuration, setSeamlessDuration] = useState<number>(() => {
    return Number(localStorage.getItem('ibrastream_seamless_duration') ?? 4);
  });
  const [ambientGlow, setAmbientGlow] = useState<boolean>(() => {
    return localStorage.getItem('ibrastream_ambient_glow') !== 'false';
  });
  const [playingPlaylistId, setPlayingPlaylistId] = useState<string | null>(() => {
    return localStorage.getItem('ibrastream_playing_playlist_id');
  });

  const [userQueue, setUserQueue] = useState<Track[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('ibrastream_user_queue') || '[]');
    } catch {
      return [];
    }
  });

  const [playlistQueue, setPlaylistQueue] = useState<Track[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('ibrastream_playlist_queue') || '[]');
    } catch {
      return [];
    }
  });

  const [originalPlaylistQueue, setOriginalPlaylistQueue] = useState<Track[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('ibrastream_original_playlist_queue') || '[]');
    } catch {
      return [];
    }
  });

  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [history, setHistory] = useState<Track[]>([]);

  // Persistent syncing
  useEffect(() => {
    if (currentTrack) {
      localStorage.setItem('ibrastream_current_track', JSON.stringify(currentTrack));
    }
  }, [currentTrack]);

  useEffect(() => {
    localStorage.setItem('ibrastream_user_queue', JSON.stringify(userQueue));
  }, [userQueue]);

  useEffect(() => {
    localStorage.setItem('ibrastream_playlist_queue', JSON.stringify(playlistQueue));
  }, [playlistQueue]);

  useEffect(() => {
    localStorage.setItem('ibrastream_original_playlist_queue', JSON.stringify(originalPlaylistQueue));
  }, [originalPlaylistQueue]);

  useEffect(() => {
    localStorage.setItem('ibrastream_is_shuffle', String(isShuffle));
  }, [isShuffle]);

  useEffect(() => {
    localStorage.setItem('ibrastream_is_repeat', isRepeat);
  }, [isRepeat]);

  useEffect(() => {
    localStorage.setItem('ibrastream_seamless_transitions', String(seamlessTransitions));
  }, [seamlessTransitions]);

  useEffect(() => {
    localStorage.setItem('ibrastream_seamless_duration', String(seamlessDuration));
  }, [seamlessDuration]);

  const playTrack = async (track: Track, newQueue?: Track[], playlistId?: string) => {
    setCurrentTrack(track);
    setIsLoading(true);
    setIsPlaying(true);
    setCurrentTime(0);
    setDuration(track.duration || 180);

    if (playlistId) {
      setPlayingPlaylistId(playlistId);
      localStorage.setItem('ibrastream_playing_playlist_id', playlistId);
    }

    if (newQueue) {
      const idx = newQueue.findIndex(t => t.id === track.id);
      const upcoming = idx !== -1 ? newQueue.slice(idx + 1) : newQueue;
      setOriginalPlaylistQueue(newQueue);
      if (isShuffle) {
        const shuffled = [...upcoming].sort(() => Math.random() - 0.5);
        setPlaylistQueue(shuffled);
      } else {
        setPlaylistQueue(upcoming);
      }
      setCurrentIndex(idx !== -1 ? idx : 0);
    }

    setIsLoading(false);
  };

  const togglePlay = () => setIsPlaying(prev => !prev);
  const pause = () => setIsPlaying(false);
  const resume = () => setIsPlaying(true);

  const nextTrack = () => {
    if (isRepeat === 'one' && currentTrack) {
      setCurrentTime(0);
      return;
    }

    if (userQueue.length > 0) {
      const [next, ...rest] = userQueue;
      setUserQueue(rest);
      if (currentTrack) setHistory(h => [currentTrack, ...h]);
      setCurrentTrack(next);
      setCurrentTime(0);
      return;
    }

    if (playlistQueue.length > 0) {
      const [next, ...rest] = playlistQueue;
      setPlaylistQueue(rest);
      if (currentTrack) setHistory(h => [currentTrack, ...h]);
      setCurrentTrack(next);
      setCurrentTime(0);
      return;
    }

    if (isRepeat === 'all' && originalPlaylistQueue.length > 0) {
      const queueCopy = [...originalPlaylistQueue];
      if (isShuffle) queueCopy.sort(() => Math.random() - 0.5);
      const [first, ...rest] = queueCopy;
      setPlaylistQueue(rest);
      setCurrentTrack(first);
      setCurrentTime(0);
      return;
    }

    setIsPlaying(false);
  };

  const prevTrack = () => {
    if (currentTime > 3) {
      setCurrentTime(0);
      return;
    }

    if (history.length > 0) {
      const [prev, ...rest] = history;
      setHistory(rest);
      if (currentTrack) setPlaylistQueue(q => [currentTrack, ...q]);
      setCurrentTrack(prev);
      setCurrentTime(0);
      return;
    }

    setCurrentTime(0);
  };

  const seek = (seconds: number) => setCurrentTime(seconds);
  const changeVolume = (vol: number) => {
    setVolume(vol);
    localStorage.setItem('ibrastream_volume', String(vol));
  };
  const toggleMute = () => setIsMuted(prev => !prev);

  const toggleShuffle = () => {
    setIsShuffle(prev => {
      const next = !prev;
      if (next) {
        setPlaylistQueue(q => [...q].sort(() => Math.random() - 0.5));
      } else {
        const remainingIds = new Set(playlistQueue.map(t => t.id));
        setPlaylistQueue(originalPlaylistQueue.filter(t => remainingIds.has(t.id)));
      }
      return next;
    });
  };

  const toggleRepeat = () => {
    setIsRepeat(prev => {
      if (prev === 'none') return 'all';
      if (prev === 'all') return 'one';
      return 'none';
    });
  };

  const toggleSeamless = () => setSeamlessTransitions(prev => !prev);
  const toggleAmbientGlow = () => setAmbientGlow(prev => !prev);

  const addToQueue = (track: Track) => setUserQueue(q => [...q, track]);
  const playNext = (track: Track) => setUserQueue(q => [track, ...q]);
  const removeFromQueue = (index: number) => {
    setUserQueue(q => q.filter((_, i) => i !== index));
  };
  const clearQueue = () => {
    setUserQueue([]);
    setPlaylistQueue([]);
  };

  return (
    <AudioContext.Provider
      value={{
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
        seamlessDuration,
        ambientGlow,
        playingPlaylistId,
        userQueue,
        playlistQueue,
        originalPlaylistQueue,
        currentIndex,
        history,
        playTrack,
        togglePlay,
        pause,
        resume,
        nextTrack,
        prevTrack,
        seek,
        changeVolume,
        toggleMute,
        toggleShuffle,
        toggleRepeat,
        toggleSeamless,
        setSeamlessDuration,
        toggleAmbientGlow,
        addToQueue,
        playNext,
        removeFromQueue,
        clearQueue,
      }}
    >
      {children}
    </AudioContext.Provider>
  );
};

export const useAudio = () => {
  const context = useContext(AudioContext);
  if (!context) throw new Error('useAudio must be used within an AudioProvider');
  return context;
};
