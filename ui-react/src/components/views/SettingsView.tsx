import React, { useState } from 'react';
import { useAudio } from '../../context/AudioContext';
import { ThemeSettings, ThemeMode } from '../../types';

interface SettingsViewProps {
  themeSettings: ThemeSettings;
  onChangeTheme: (mode: ThemeMode) => void;
  onToggleAmbientGlow: () => void;
  userEmail?: string | null;
  onSignOut?: () => void;
  onOpenAuth?: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  themeSettings,
  onChangeTheme,
  onToggleAmbientGlow,
  userEmail,
  onSignOut,
  onOpenAuth,
}) => {
  const {
    seamlessTransitions,
    seamlessDuration,
    toggleSeamless,
    setSeamlessDuration,
  } = useAudio();

  const [audioQuality, setAudioQuality] = useState<'normal' | 'high' | 'lossless'>('high');
  const [filterExplicit, setFilterExplicit] = useState<boolean>(() => {
    return localStorage.getItem('ibrastream_filter_explicit') === 'true';
  });
  const [onlyDownloaded, setOnlyDownloaded] = useState<boolean>(() => {
    return localStorage.getItem('ibrastream_only_downloaded') === 'true';
  });

  const handleToggleExplicit = () => {
    setFilterExplicit(prev => {
      const next = !prev;
      localStorage.setItem('ibrastream_filter_explicit', String(next));
      return next;
    });
  };

  const handleToggleOnlyDownloaded = () => {
    setOnlyDownloaded(prev => {
      const next = !prev;
      localStorage.setItem('ibrastream_only_downloaded', String(next));
      return next;
    });
  };

  return (
    <div className="flex flex-col gap-8 max-w-3xl pb-32 animate-[fadeIn_0.3s_ease] select-none">
      <h1 className="text-2xl font-bold text-white tracking-tight">Settings</h1>

      {/* Audio Playback Section */}
      <section className="flex flex-col gap-5 p-6 rounded-3xl bg-white/5 border border-white/10">
        <h2 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Audio Playback</h2>

        {/* Seamless Transitions (Crossfade) */}
        <div className="flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white">Seamless Transitions (Crossfade)</span>
            <span className="text-xs text-gray-400 mt-0.5">
              Dual-deck simultaneous crossfade between upcoming tracks
            </span>
          </div>
          <button
            onClick={toggleSeamless}
            className={`w-11 h-6 rounded-full p-1 transition-colors duration-200 ${
              seamlessTransitions ? 'bg-white' : 'bg-white/10'
            }`}
          >
            <div
              className={`w-4 h-4 rounded-full bg-black transition-transform duration-200 ${
                seamlessTransitions ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Crossfade Duration Slider */}
        {seamlessTransitions && (
          <div className="flex flex-col gap-2 pt-2 border-t border-white/5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-400">Crossfade Duration</span>
              <span className="text-white font-bold">{seamlessDuration}s</span>
            </div>
            <input
              type="range"
              min={1}
              max={12}
              value={seamlessDuration}
              onChange={e => setSeamlessDuration(Number(e.target.value))}
              className="w-full h-1.5 bg-white/10 rounded-lg appearance-none cursor-pointer accent-white"
            />
          </div>
        )}

        {/* Streaming Audio Quality */}
        <div className="flex items-center justify-between pt-2 border-t border-white/5">
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white">Streaming Audio Quality</span>
            <span className="text-xs text-gray-400 mt-0.5">Bitrate optimization for streaming</span>
          </div>
          <select
            value={audioQuality}
            onChange={e => setAudioQuality(e.target.value as any)}
            className="bg-white/10 border border-white/15 text-white text-xs px-3 py-1.5 rounded-xl cursor-pointer focus:outline-none"
          >
            <option value="normal" className="bg-[#181818] text-white">Normal (128 kbps)</option>
            <option value="high" className="bg-[#181818] text-white">High (256 kbps)</option>
            <option value="lossless" className="bg-[#181818] text-white">Pristine (320 kbps)</option>
          </select>
        </div>

        {/* Offline Mode / Only Downloaded */}
        <div className="flex items-center justify-between pt-2 border-t border-white/5">
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white">Modo Offline (Solo Descargadas)</span>
            <span className="text-xs text-gray-400 mt-0.5">
              Muestra y reproduce exclusivamente música descargada en el dispositivo
            </span>
          </div>
          <button
            onClick={handleToggleOnlyDownloaded}
            className={`w-11 h-6 rounded-full p-1 transition-colors duration-200 ${
              onlyDownloaded ? 'bg-amber-400' : 'bg-white/10'
            }`}
          >
            <div
              className={`w-4 h-4 rounded-full bg-black transition-transform duration-200 ${
                onlyDownloaded ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </section>

      {/* Appearance & Themes Section */}
      <section className="flex flex-col gap-5 p-6 rounded-3xl bg-white/5 border border-white/10">
        <h2 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Appearance</h2>

        {/* Theme Picker (Dark, Noir, Bright) */}
        <div className="flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white">Theme Style</span>
            <span className="text-xs text-gray-400 mt-0.5">Color palette and darkness levels</span>
          </div>
          <div className="flex items-center gap-2">
            {(['noir', 'dark', 'bright'] as ThemeMode[]).map(mode => (
              <button
                key={mode}
                onClick={() => onChangeTheme(mode)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold capitalize transition-all ${
                  themeSettings.mode === mode
                    ? 'bg-white text-black shadow-md'
                    : 'bg-white/5 text-gray-400 hover:text-white border border-white/10'
                }`}
              >
                {mode}
              </button>
            ))}
          </div>
        </div>

        {/* Dynamic Ambient Background Glow */}
        <div className="flex items-center justify-between pt-2 border-t border-white/5">
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white">Ambient Glow Effect</span>
            <span className="text-xs text-gray-400 mt-0.5">
              Soft ambient lighting reacting dynamically to current album colors
            </span>
          </div>
          <button
            onClick={onToggleAmbientGlow}
            className={`w-11 h-6 rounded-full p-1 transition-colors duration-200 ${
              themeSettings.ambientGlow ? 'bg-white' : 'bg-white/10'
            }`}
          >
            <div
              className={`w-4 h-4 rounded-full bg-black transition-transform duration-200 ${
                themeSettings.ambientGlow ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </section>

      {/* Account & Cloud Sync Section */}
      <section className="flex flex-col gap-5 p-6 rounded-3xl bg-white/5 border border-white/10">
        <h2 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Account & Cloud Sync</h2>

        <div className="flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white">
              {userEmail ? 'Signed in as' : 'Offline / Guest Session'}
            </span>
            <span className="text-xs text-gray-400 mt-0.5">
              {userEmail || 'Playlists and statistics stored locally'}
            </span>
          </div>

          {userEmail ? (
            <button
              onClick={onSignOut}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 transition-all"
            >
              Sign Out
            </button>
          ) : (
            <button
              onClick={onOpenAuth}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-black bg-white hover:bg-gray-200 transition-all active:scale-95 shadow-md"
            >
              Login / Register
            </button>
          )}
        </div>
      </section>
    </div>
  );
};
