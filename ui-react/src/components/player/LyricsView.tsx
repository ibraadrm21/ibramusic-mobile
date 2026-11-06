import React, { useState, useEffect, useRef } from 'react';
import { useAudio } from '../../context/AudioContext';
import { LyricLine } from '../../types';

interface LyricsViewProps {
  isOpen: boolean;
  onClose: () => void;
}

export const LyricsView: React.FC<LyricsViewProps> = ({ isOpen, onClose }) => {
  const { currentTrack, currentTime, seek } = useAudio();
  const [lyrics, setLyrics] = useState<LyricLine[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [offsetMs, setOffsetMs] = useState<number>(0); // Sincronizar offset adjuster
  const activeLineRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!currentTrack || !isOpen) return;

    let isMounted = true;
    setIsLoading(true);

    // Fetch synced lyrics from lrclib.net matching ibrastream production
    fetch(
      `https://lrclib.net/api/get?track_name=${encodeURIComponent(
        currentTrack.title
      )}&artist_name=${encodeURIComponent(currentTrack.artist)}&duration=${Math.floor(
        currentTrack.duration || 180
      )}`
    )
      .then(res => (res.ok ? res.json() : null))
      .then(data => {
        if (!isMounted) return;
        if (data && data.syncedLyrics) {
          const lines = parseLrc(data.syncedLyrics);
          setLyrics(lines);
        } else if (data && data.plainLyrics) {
          const lines = data.plainLyrics.split('\n').map((text: string, i: number) => ({
            time: i * 4,
            text,
          }));
          setLyrics(lines);
        } else {
          setLyrics([]);
        }
        setIsLoading(false);
      })
      .catch(() => {
        if (isMounted) {
          setLyrics([]);
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [currentTrack, isOpen]);

  // Auto-scroll active line into view
  useEffect(() => {
    if (activeLineRef.current) {
      activeLineRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [currentTime, offsetMs]);

  if (!isOpen) return null;

  const adjustedTime = currentTime + offsetMs / 1000;
  const activeIndex = lyrics.findIndex((line, i) => {
    const nextLine = lyrics[i + 1];
    return adjustedTime >= line.time && (!nextLine || adjustedTime < nextLine.time);
  });

  return (
    <div className="fixed inset-0 z-50 bg-[#0f0f0f]/95 backdrop-blur-2xl flex flex-col p-6 animate-[fadeIn_0.2s_ease] select-none">
      {/* Top Bar */}
      <header className="flex items-center justify-between pb-4 border-b border-white/10 shrink-0">
        <div className="flex flex-col">
          <span className="text-xs uppercase tracking-widest text-brand-accent font-bold">Lyrics</span>
          <span className="text-sm font-semibold text-white truncate">{currentTrack?.title}</span>
        </div>

        {/* Sync Offset Tool */}
        <div className="flex items-center gap-3 bg-white/5 border border-white/10 px-3 py-1.5 rounded-full text-xs text-gray-300">
          <span>Sincronizar:</span>
          <button
            onClick={() => setOffsetMs(o => o - 500)}
            className="px-1.5 py-0.5 rounded bg-white/10 hover:bg-white/20 active:scale-95 text-white"
          >
            -0.5s
          </button>
          <span className="tabular-nums font-mono">{offsetMs > 0 ? `+${offsetMs}ms` : `${offsetMs}ms`}</span>
          <button
            onClick={() => setOffsetMs(o => o + 500)}
            className="px-1.5 py-0.5 rounded bg-white/10 hover:bg-white/20 active:scale-95 text-white"
          >
            +0.5s
          </button>
        </div>

        <button
          onClick={onClose}
          className="p-2 text-gray-400 hover:text-white rounded-full bg-white/5 active:scale-95 transition-all"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </header>

      {/* Lyrics Scrollable Container */}
      <div className="flex-1 overflow-y-auto py-12 flex flex-col items-center gap-8 text-center scrollbar-none">
        {isLoading ? (
          <div className="my-auto flex flex-col items-center gap-3 text-gray-400">
            <div className="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin" />
            <span className="text-sm">Fetching lyrics...</span>
          </div>
        ) : lyrics.length === 0 ? (
          <div className="my-auto flex flex-col items-center gap-2 text-gray-400">
            <p className="text-lg font-semibold text-white">No lyrics available</p>
            <p className="text-xs">We couldn't find synchronized lyrics for this song.</p>
          </div>
        ) : (
          lyrics.map((line, idx) => {
            const isActive = idx === activeIndex;
            return (
              <div
                key={idx}
                ref={isActive ? activeLineRef : null}
                onClick={() => seek(line.time - offsetMs / 1000)}
                className={`text-xl sm:text-2xl font-bold transition-all duration-300 cursor-pointer max-w-xl ${
                  isActive
                    ? 'text-white scale-110 drop-shadow-[0_0_15px_rgba(255,255,255,0.4)]'
                    : 'text-gray-500 hover:text-gray-300 scale-100'
                }`}
              >
                {line.text}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

function parseLrc(lrc: string): LyricLine[] {
  const lines = lrc.split('\n');
  const result: LyricLine[] = [];
  const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\]/;

  for (const line of lines) {
    const match = timeRegex.exec(line);
    if (match) {
      const min = parseInt(match[1], 10);
      const sec = parseInt(match[2], 10);
      const ms = parseInt(match[3].padEnd(3, '0').slice(0, 3), 10);
      const time = min * 60 + sec + ms / 1000;
      const text = line.replace(timeRegex, '').trim();
      if (text) result.push({ time, text });
    }
  }

  return result.sort((a, b) => a.time - b.time);
}
